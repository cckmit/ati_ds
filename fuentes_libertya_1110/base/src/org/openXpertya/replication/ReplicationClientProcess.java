package org.openXpertya.replication;

/**
 * Cliente RMI de replicación.  Se encarga de interactuar
 * via RMI con el servidor de Replication, pasándole
 * los datos a replicar, iterando dicha lógica por cada sucursal
 */



import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Level;

import org.openXpertya.interfaces.Replication;
import org.openXpertya.model.MOrg;
import org.openXpertya.model.MProcess;
import org.openXpertya.model.MReplicationHost;
import org.openXpertya.model.PO;
import org.openXpertya.model.X_AD_ReplicationError;
import org.openXpertya.plugin.install.ChangeLogElement;
import org.openXpertya.process.ProcessInfo;
import org.openXpertya.process.ProcessInfoParameter;
import org.openXpertya.process.SvrProcess;
import org.openXpertya.reflection.ClientCall;
import org.openXpertya.reflection.RMICallConfig;
import org.openXpertya.util.DB;
import org.openXpertya.util.Env;
import org.openXpertya.util.Trx;


public class ReplicationClientProcess extends SvrProcess {

	
	/** Flag de replicacion sincrónica */
	boolean sync_replication = true;

	/** Todas las replicaciones dan OK */
	boolean all_orgs_ok = true;
	
	/** ID de esta organización */
	int thisOrgID = -1; 
	
	/** Posicion de esta organización en el array de organización */
	int thisOrgPos = -1; 
		
	/** Si esta variable está seteada, significa que en este momento deberá replicarse unicamente hacia esta sucursal 
	 *  Esto está pensado para los casos de replicateNow, en los cuales hay que priorizar el dato para una org. especifica */
	int p_forceTargetOrgID = -1;
	
	/** ID inicial del changelog a replicar (sujeto a valores del replicationArray) - tabla AD_Changelog_Replication */
	int p_Changelog_Initial_ID = -1; 
	
	/** ID final del changelog a replicar (sujeto a valores del replicationArray) - tabla AD_Changelog_Replication */
	int p_Changelog_Final_ID = -1;

	/** Realizar la replicación sincrónica de manera forzada (salteando replicaciones pendientes en host destino)  */
	String p_Force_Replication = "N";
	
	/** Constante para entrada en el contexto: verificación del origen de la petición de replicación */
	public static final String VALIDATION_PROPERTY = "#Replication_Access_Key";
	
	/** Consante para entrada en el contexto: insercion tardía */ 
	public static final String DELAYEDINSERT_PROPERTY = "#Delayed_Insert_Replication";
	
	/** Si Nombre de host es OFFLINE_HOST, entonces guarda a archivo en lugar de enviar la información a replicar */
	public static final String OFFLINE_REPLICATION_VALUE = "OFFLINE_HOST";
	
	@Override
	protected void prepare() {
		
		/* Setear el ID y Pos de esta organizacion (ignora la Org del login, utiliza la conf. de thisHost) */
		thisOrgID  = DB.getSQLValue(get_TrxName(), "SELECT AD_Org_ID FROM AD_ReplicationHost WHERE thisHost = 'Y' AND AD_Client_ID = " + getAD_Client_ID()); 
		thisOrgPos = MReplicationHost.getReplicationPositionForOrg(thisOrgID, get_TrxName()); 
		
		/* Log en tabla de errores de replicacion - inicio de replicación */
		String info = "INICIO DE REPLICACIÓN";
		saveLog(false, Level.INFO, info, info, null, -1, -1, getReplicationType());

		/* Parametros? */
        ProcessInfoParameter[] para = getParameter();
        for( int i = 0;i < para.length;i++ ) {
            String name = para[ i ].getParameterName();
            if( name.equals( "AD_Org_ID" ))
            	p_forceTargetOrgID = para[ i ].getParameterAsInt();
            else if (name.equals( "Changelog_Initial_ID" ))
            	p_Changelog_Initial_ID = para[i].getParameterAsInt();
            else if (name.equals( "Changelog_Final_ID" ))
            	p_Changelog_Final_ID = para[i].getParameterAsInt();
            else if (name.equals( "Force_Replication" ))
            	p_Force_Replication = (String)para[i].getParameter();
            		
        }

	}
	
	
	@Override
	protected String doIt() throws Exception {
		
		/* Configuración correcta en AD_ReplicationHost? */
		if (thisOrgID == -1)
			throw new Exception (" Sin marca de host.  Debe realizar la configuración correspondiente en la ventana Hosts de Replicación ");
		
		/** Cargar en memoria todas las referencias e información de tablas */
        loadCacheData();
		
		/* Recuperar todas las organizaciones a fin de replicar la informacion para cada una 
		   Contemplando la posibilidad de replicar en este momento unicamente a una sucursal destino */		
		int[] orgs = {p_forceTargetOrgID}; 
		if (p_forceTargetOrgID == -1) 
			orgs = PO.getAllIDs("AD_Org", " isActive = 'Y' AND AD_Client_ID = " + getAD_Client_ID() + " AND AD_Org_ID != " + thisOrgID, get_TrxName());
		
		/* Builder que genenera el XML a partir del changelog */
		ReplicationBuilder builder = null;
		int initialID = -1;
		int finalID = -1;

		/* Iterar por todas las sucursales */
		for (int i=0; i<orgs.length; i++)
		{
			try
			{
				/* Obtener el XML a enviar al host destino, en caso de error continuar con siguiente host */
				int replicationArrayPos = MReplicationHost.getReplicationPositionForOrg(orgs[i], get_TrxName());
				if (replicationArrayPos == -1)
					continue;

				/* Setear la accessKey para validación en el host remoto */
				Env.setContext(getCtx(), VALIDATION_PROPERTY, DB.getSQLValueString(get_TrxName(), getAccessKeySQLQuery(true), replicationArrayPos)); 
				
				/**
				 * Recorrer el changelog para la sucursal paginando de a
				 * un numero de registros, comprimirlos e incorporarlos
				 * al vector que contiene todos los stripes comprimidos,
				 * el cual será enviado al host remoto para su replicación
				 */
				initialID = -1;
				finalID = -1;
				Vector<byte[]> completeCompressedXMLContent = new Vector<byte[]>();
				boolean hasReplicationData = true;
				int iteration = 0;
				
				/* El builder a utilizar procesará la información del changelog
				 * En la primera iteración, buscará el menor changelogReplicationID (por esto se le pasa -1) 
				 * En las siguientes iteraciones, deberá buscar a partir del siguiente registro luego del changelog_final_id 
				 */
				builder = new ReplicationBuilder(replicationArrayPos, p_Changelog_Initial_ID, p_Changelog_Final_ID, get_TrxName());
				while (hasReplicationData)
				{
					/* Generar el XML */
					builder.generateDocument();
					hasReplicationData = builder.hasReplicationData();

					/* Hay informacion a replicar? */
					if (hasReplicationData)
					{
						/* Si es la primera iteración, quedarme con el id inicial */
						if (iteration++ == 0)
							initialID = builder.getGroupList().getM_changelog_initial_id();
						/* Quedarme siempre con el ultimo changelog */
						finalID = builder.getGroupList().getM_changelog_final_id();
	
						/* Comprimir el string a fin de reducir los tiempos de transmision entre hosts e incorporarlo al Vector */
						byte[] compressedXML = builder.getCompressedXML();
						completeCompressedXMLContent.add(compressedXML);
	
						/* Limpiar memoria */
						builder.emptyM_replicationXMLData();
						builder.setInitial_changelog_replication_id(finalID+1);
						compressedXML = null;
						System.gc();
					}
				}
				
				/* Recuperar el hostname para determinar si es una replicación online o no */
				String hostName = MReplicationHost.getHostForOrg(orgs[i], get_TrxName());
				boolean offlineOrg = OFFLINE_REPLICATION_VALUE.equalsIgnoreCase(hostName);
				
				/* Setear en el contexto si estamos realizando una replicación tardía o no */
				setIsDelayedInsertReplication(initialID, replicationArrayPos);
				
				/* Realizar el envio correspondiente */
				transferContent(completeCompressedXMLContent, orgs, i, initialID, finalID, replicationArrayPos, offlineOrg);

			}
			catch (RemoteException e)
			{
				String error = "Error Remoto. ";
				saveLog(true, Level.SEVERE, error, error + e.getMessage(), orgs[i], initialID, finalID, getReplicationType());
			}
			catch (Exception e)
			{
				String error = "Error Local. ";
				saveLog(true, Level.SEVERE, error, error + e.getMessage(), orgs[i], initialID, finalID, getReplicationType());
			}
			finally
			{
				/* Limpiar el contexto */
				Env.setContext(getCtx(), VALIDATION_PROPERTY, (String)null);
				Env.setContext(getCtx(), DELAYEDINSERT_PROPERTY, (String)null);
				
				/* Help garbage collector */
				builder = null;
				System.gc();
			}
		}

		/* Log en tabla de info/errores de replicacion - fin de replicación */
		String info = "FIN DE REPLICACIÓN";
		saveLog(false, Level.INFO, info, info, null, -1, -1, getReplicationType());
		
		/* Informar acordemenete (aunque esto es relativo, ya que este proceso sera una tarea programada) */
		freeCacheData();
		return "FINALIZADO " + (all_orgs_ok?"":"Revise la nomina de errores en replicación");
		
	}
	
	/**
	 * Realiza el envio de los datos de replicación
	 * @param completeCompressedXMLContent contenido
	 * @param orgs organizaciones
	 * @param i organizacion actual
	 * @param initialID changelog inicial
	 * @param finalID changelog final
	 * @param replicationArrayPos posicion en el array de replicacion
	 * @param offlineOrg indica si debe almacenarse en archivo en lugar de ser enviado
	 * @throws Exception
	 */
	protected void transferContent(Vector<byte[]> completeCompressedXMLContent, int[] orgs, int i, int initialID, int finalID, int replicationArrayPos, boolean offlineOrg) throws Exception, RemoteException
	{
		ClientCall clientCall = null;
		
		/* Si no hay nada para replicar, salteamos la org */
		if (completeCompressedXMLContent.size() == 0)
			return;
		
		/* Si es una sucursl offline, generar archivos con la informacion (uno por cada posicion del bytearray) */
		if (offlineOrg)
		{
			MOrg thisOrg = new MOrg(getCtx(), thisOrgID, get_TrxName());
			MOrg anOrg = new MOrg(getCtx(), orgs[i], get_TrxName());
			String baseFileName = "ReplicationInfo_" + Env.getDateTime("yyyyMMdd-HHmmss")
									+ "_" + thisOrgPos + ("(") + thisOrg.getName().replace("(", "").replace(")", "") + ")a" 
									+ replicationArrayPos + "(" + anOrg.getName().replace("(", "").replace(")", "") + ")_" 
									+ initialID + "a" + finalID;
			int size = completeCompressedXMLContent.size();
			for (int k = 0; k < size; k++)
			{
				String fileName = baseFileName + ".rep" + (k+1) + "_" + size;
				DataOutputStream os = new DataOutputStream(new FileOutputStream(fileName));
				os.write(completeCompressedXMLContent.get(k));
				os.close();
			}
			BufferedWriter out = new BufferedWriter(new FileWriter(baseFileName + ".md5"));
			out.write(DB.getSQLValueString(get_TrxName(), getAccessKeySQLQuery(true), replicationArrayPos));
			out.close();
		}
		else
		{
			/* Instanciar la conexión correspondiente y verificar que sea correcta*/
			ReplicationConnection conn = new ReplicationConnection(orgs[i], get_TrxName()); 
			Replication replication = conn.getReplication();
			if (replication==null)
			{
				String error = "Error Local. Imposible conectar a host. ";
				saveLog(true, Level.SEVERE, error, error + conn.getException().getMessage(), orgs[i], initialID, finalID, getReplicationType());
				return;
			}
			// Modified by Matías Cap - Disytel
			// ---------------------------------------------------------
			// Usar las nuevas llamadas a métodos genéricos con timeouts
			
			// Armar la configuración de la llamada con timeout
			RMICallConfig callConfig = new RMICallConfig();
			callConfig.setCtx(getCtx());
			callConfig.setTrxName(get_TrxName());
			callConfig.setCaller(replication);
			callConfig.setMethod("replicate");
			callConfig.setTimerValue("ReplicacionPeriodica");
			callConfig.setParametersTypes(new Class<?>[] {
					Properties.class, Vector.class, boolean.class,
					int.class, int.class, int.class,
					boolean.class });
			callConfig.setParametersValues(new Object[] { getCtx(),
					completeCompressedXMLContent, sync_replication,
					thisOrgPos, initialID, finalID,
					"Y".equals(p_Force_Replication) });
			
			clientCall = new ClientCall(callConfig);
			clientCall.call();
		}
		
		/* Todo bien? Decrementar los indices de replicationArray en la posicion de la organización 
		 * Comitteamos inmediatamente ya que en el host remoto los cambios ya han sido realizados 
		 * (en el caso de replicación offline, se toma la correcta exportación a archivo */
		if(offlineOrg || !clientCall.getResult().isError()){
			decrementReplicationArray(replicationArrayPos, initialID, finalID);
			Trx.getTrx(get_TrxName()).commit();
		}
		else{
			// Hubo un problema en una replicación del lado del server 
			String error = "Error Remoto durante replicación. ";
			saveLog(true, Level.SEVERE, error, error + clientCall.getResult().getMsg(), orgs[i], initialID, initialID, getReplicationType());
		}
		clientCall = null;
		// ---------------------------------------------------------
	}
	
	/**
	 * Almacena el error correspondiente segun sea el caso de invocación
	 * @param logMessage
	 */
	protected void saveLog(boolean setError, Level aLevel, String logMessage, String replicationError, Integer orgID, int changelogInitPos, int changelogFinalPos, String replicationType)
	{
		System.out.println("Replication " + (orgID==null?"":"("+orgID+")") + ":" + logMessage + "-" + replicationError);
		
		if (setError)
			all_orgs_ok = false;

		X_AD_ReplicationError aLog = new X_AD_ReplicationError(getCtx(), 0, get_TrxName());
		aLog.setORG_Target_ID(orgID==null?0:orgID);
		aLog.setInitialChangelog_ID(changelogInitPos);
		aLog.setFinalChangelog_ID(changelogFinalPos);
		aLog.setReplication_Type(replicationType);
		aLog.setReplication_Error(Env.getDateTime("yyyy/MM/dd-HH:mm:ss.SSS") + " - " + replicationError);
		aLog.setClientOrg(getAD_Client_ID(), thisOrgID);
		aLog.save();
		
		log.log(aLevel, logMessage );
	}
	
	
	/**
	 * Retorna el tipo de replicación en función del boolean correspondiente 
	 */
	protected String getReplicationType()
	{
		return sync_replication?X_AD_ReplicationError.REPLICATION_TYPE_Synchronous:X_AD_ReplicationError.REPLICATION_TYPE_Asynchronous;
	}
	
	
	/**
	 * Decrementa a 0 el valor en la columna replicationArray para todas 
	 * las entradas en la bitacora de replicación en la posicion pos
	 * @param pos posicion a decrementar en el array
	 */
	protected void decrementReplicationArray(int pos, int initialID, int finalID)
	{
		DB.executeUpdate(" UPDATE ad_changelog_replication " +
						 " SET replicationarray = OVERLAY(replicationarray placing '0' FROM "+(pos)+" for 1) " +
						 " WHERE substring(replicationarray, "+(pos)+", 1) = '1' " +
						 " AND ad_changelog_replication_id between " + initialID + " AND " + finalID + 
						 " AND AD_Client_ID = " + getAD_Client_ID(), get_TrxName());
	}
	    

    /**
     * Invocaciones a carga de tablas referenciales varias en memoria
     * a fin de reducir los tiempos de acceso a base de datos
     * @throws Exception
     */
    protected void loadCacheData() throws Exception
    {
    	ReplicationBuilder.loadCacheData(get_TrxName());
		ChangeLogElement.loadColumnData(get_TrxName());
    }
    
    /**
     * Liberación de memoria de tablas referenciales
     */
    protected void freeCacheData()
    {
    	ReplicationBuilder.freeCacheData();
    	ChangeLogElement.freeColumnData();
    }
    
    
    /**
     * Genera el query para obtener el key definitivo entre las sucursales origen 
     * y destino a fin de validar la correctitud de la petición de replicación
     * @param host: si es true arma la consulta para el host origen, si es false la arma para el host destino
     * @return el md5 resultante de concatenar ambos hostAccessKey
     */
    public static String getAccessKeySQLQuery(boolean host)
    {
    	String sourceWhere = host ? " thisHost = 'Y' " : " replicationArrayPos = ? ";
    	String targetWhere = host ? " replicationArrayPos = ? " : " thisHost = 'Y' ";
    	return 
			" SELECT md5( " + 
			"			(SELECT hostAccessKey as source FROM AD_REPLICATIONHOST WHERE " + sourceWhere + ") || " +
			"	  	    (SELECT hostAccessKey as target FROM AD_REPLICATIONHOST WHERE " + targetWhere + ")    " +
			"	  	 )    ";
    }

    
    /**
     * Setea en el contexto si la replicación actual es una replicación tardía o no.
     * Para esto, compara el initialID del set a replicar con entradas mas viejas
     * del changelog.  En caso de que todavía existan entradas anteriores a replicar
     * para el host destino en cuestión, seteará en el ctx como replicación tardía
     * En función de este dato, el host destino actualizará o no el ultimo changelog
     * replicado desde el host origen (si es tardío no lo actualiza).  De esta manera
     * al replicar las entradas previas restantes, el host destino no devolverá error 
     * por changelogID menor al ultimo changelog registrado para dicha sucursal origen. 
     * @param initialID
     * @param replicationArrayPos
     * @throws Exception
     */
    protected void setIsDelayedInsertReplication(int initialID, int replicationArrayPos) throws Exception
    {
    	/* Suponer inicialmente que no es una inserción tardía */
    	Env.setContext(getCtx(), DELAYEDINSERT_PROPERTY, "N");
    	
    	/* Si en los parámetros el usuario directamente no seteo un changelogID inicial,
    	 * entonces está queriendo replicar el changelog desde el inicio, por lo tanto
    	 * no estará intentando realizar una replicación tardía */
    	if (p_Changelog_Initial_ID == -1)
    		return;
    	
    	/* En cualquier otro caso, verificar si quedaron "cosas por replicar mas antiguas" */
    	int count = DB.getSQLValue(get_TrxName(), " SELECT count(1) FROM ad_changelog_replication " +
    						" WHERE substring(replicationarray, "+(replicationArrayPos)+", 1) = '1' " +
    						" AND ad_changelog_replication_id < " + initialID +  
    						" AND AD_Client_ID = " + getAD_Client_ID());
    	
    	/* Setear el valor resultante */
    	Env.setContext(getCtx(), DELAYEDINSERT_PROPERTY, count > 0 ? "Y":"N");
    }
    
    /**
     * Invocación al proceso de replicación cliente desde terminal
     * Alternativa de uso del proceso fuera de la aplicación
     * @param args
     */
    public static void main(String args[])
    {
    	// Ejemplo de uso:
    	// java -classpath lib/OXP.jar:lib/OXPLib.jar:lib/OXPXLib.jar org.openXpertya.replication.ReplicationClientProcess -host1010053 -initial1049382 -final1058178 -force
    	// -host 		es el host destino al cual deberá replicar, indicándose el AD_Org_ID correspondiente.  
    	//				si no es especifica ninguno, replicará a todos los hosts según el replicationArray
    	// -initial 	es el changelogID inicial que se quiere replicar
    	// -final 		es el changelogID inicial que se quiere replicar
    	// -force		fuerza la replicación (este parámetro no requiere valor alguno).
    	// ------------ NO DEBEN DEJARSE ESPACIOS ENTRE EL PARAMETRO Y EL VALOR DEL PARAMETRO! ---------------
    	
    	/* Constantes */
    	final String PARAMETER_HOST 		= "-host";
    	final String PARAMETER_INITIAL_CL 	= "-initial"; 
    	final String PARAMETER_FINAL_CL 	= "-final"; 
    	final String PARAMETER_FORCE 		= "-force"; 
    	
    	// Parsear los parametros
    	HashMap<String, Object> params = new HashMap<String, Object>();
    	for (String arg : args)
    	{
    		if (arg.toLowerCase().startsWith(PARAMETER_HOST))
    			params.put("AD_Org_ID", arg.substring(PARAMETER_HOST.length()));
    		else if (arg.toLowerCase().startsWith(PARAMETER_INITIAL_CL))
    			params.put("Changelog_Initial_ID", arg.substring(PARAMETER_INITIAL_CL.length()));
    		else if (arg.toLowerCase().startsWith(PARAMETER_FINAL_CL))
    			params.put("Changelog_Final_ID", arg.substring(PARAMETER_FINAL_CL.length()));
    		else if (arg.toLowerCase().startsWith(PARAMETER_FORCE))
    			params.put("Force_Replication", "Y");
    	}
    		
    	String oxpHomeDir = System.getenv("OXP_HOME"); 
    	if (oxpHomeDir == null)
    	{
    		System.out.println("Error: OXP_HOME environment variable not set");
    		return;
    	}

    	// Cargar el entorno basico
    	System.setProperty("OXP_HOME", oxpHomeDir);
    	if (!org.openXpertya.OpenXpertya.startupEnvironment( false ))
    	{
    		System.err.println("Error al iniciar la configuración de replicacion");
    		return;
    	}
        Env.setContext(Env.getCtx(), "#AD_Client_ID", DB.getSQLValue(null, " SELECT AD_Client_ID FROM AD_REPLICATIONHOST WHERE thisHost = 'Y' "));
        Env.setContext(Env.getCtx(), "#AD_Org_ID", DB.getSQLValue(null, " SELECT AD_Org_ID FROM AD_REPLICATIONHOST WHERE thisHost = 'Y' "));
        
        // Iniciar la transacción
		String m_trxName = Trx.createTrxName();
		Trx.getTrx(m_trxName).start();
		
		// Recuperar el proceso de replicación cliente
		int processId = DB.getSQLValue(m_trxName, " SELECT AD_PROCESS_ID FROM AD_PROCESS WHERE AD_COMPONENTOBJECTUID = 'CORE-AD_Process-1010218' ");
		ProcessInfo pi = MProcess.execute(Env.getCtx(), processId, params, m_trxName);

		// En caso de error, presentar en consola
		if (pi.isError())
			System.err.println("Error en replicacion: " + pi.getSummary());
    }
    
    
    
}

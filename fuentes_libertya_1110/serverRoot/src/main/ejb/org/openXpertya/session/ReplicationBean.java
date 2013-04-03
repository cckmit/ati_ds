package org.openXpertya.session;
 
import java.rmi.RemoteException;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Level;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import org.openXpertya.model.MAsyncReplication;
import org.openXpertya.model.MReplicationHost;
import org.openXpertya.model.X_AD_AsyncReplication;
import org.openXpertya.replication.ReplicationClientProcess;
import org.openXpertya.replication.ReplicationUtils;
import org.openXpertya.replication.ReplicationXMLUpdater;
import org.openXpertya.util.CLogger;
import org.openXpertya.util.DB;
import org.openXpertya.util.Env;
import org.openXpertya.util.Trx;
 
/**
 * 	Bean de Replication de Libertya.
 *
 *  @ejb:bean name="openXpertya/Replication"
 *           display-name="Libertya Replication Bean"
 *           type="Stateless"
 *           transaction-type="Bean"
 *           jndi-name="ejb/openXpertya/Replication"
 *
 *  @ejb:ejb-ref ejb-name="openXpertya/Replication"
 *              ref-name="openXpertya/Replication"
 *
 * @author     Equipo de Desarrollo de Libertya  
 * 
 */
public class ReplicationBean  implements SessionBean {
 
	/**	Context				*/
	private SessionContext 	m_Context;
	/**	Logger				*/
	private static CLogger log = CLogger.getCLogger(ServerBean.class);
	/** TrxName				*/
	private String m_trxName = "";
 
	/**************************************************************************
	 * 	Create the Session Bean - Obligatorio implementar
	 * 	@throws CreateException
	 *  @ejb:create-method view-type="remote"
	 */
	public void ejbCreate() throws CreateException
	{
 
	}	//	ejbCreate
 
 
	// Métodos remotos 
 
	/**
	 * Realiza el impacto del contentXML recibido por parte del  
	 * AD_Org_ID origen, sobre la base de datos del host local
	 * 
	 * @param ctx el contexto
	 * @param completeCompressedXML contenido a replicar, comprimido y separado en varios chunks para reducir requerimientos de memoria
	 * @param sync tipo de replicación
	 * 			En caso de que sync sea verdadero, la replicación es inmedianta
	 * 			En caso de que sync sea falso, la replicación es buffereada sobre AD_AsynReplication,
	 * 			y los datos ahí contenidos serán procesados por el replicador offline
	 * @param initialChangelogID es el ID inicial a replicar de la tabla AD_Changelog_Replication en el host origen
	 * @param finalChangelogID es el ID final de la tabla AD_Changelog_Replication en el host origen
	 * @param force permite "saltear" entradas pendientes en la tabla de replicación asincrónica, forzando el impacto directo (solo util para replicación sincronica)
	 * 
	 * 
	 *  @ejb:interface-method view-type="remote"
	 */
	public String replicate (Properties ctx, Vector<byte[]> completeCompressedXML, boolean sync, int arrayPos, int initialChangelogID, int finalChangelogID, boolean force) throws RemoteException
	{
		try 
		{
			/* Hay contenido a replicar? */
			if (completeCompressedXML == null || completeCompressedXML.size() == 0)
				return "";

			/* Transacción */
			m_trxName = Trx.createTrxName();
			Trx.getTrx(m_trxName).start();
			
			/* Validar pedido de replicación */
			validateReplicationRequest(ctx, arrayPos, m_trxName);
			
			/* Determinar el OrgId de origen en funcion de la posicion en el array origen */
			int AD_Org_ID = MReplicationHost.getReplicationOrgForPosition(arrayPos, m_trxName);
			if (AD_Org_ID == -1)
				throw new RemoteException("No es posible determinar la organización a partir del replicationArrayPos");
			
			/** Iterar por el vector, obteniendo las páginas de replicacion correspondientes */
			for (int i=0; i<completeCompressedXML.size(); i++)
			{
				/* Obtener el primer pack comprimido */
				byte[] compressedXML = completeCompressedXML.get(i);
				
				/* Contenido correcto para su replicación? */
				if (compressedXML == null || compressedXML.length == 0)
					continue;
				
				/* Descomprimir el zip */
				String contentXML = ReplicationUtils.decompressString(compressedXML);
				if (contentXML == null || contentXML.length() == 0)
					continue;

				/* Es una replicación tardía? */
				boolean isDelayedInsert = "Y".equals(Env.getContext(ctx, ReplicationClientProcess.DELAYEDINSERT_PROPERTY));
				
				/* Replicación sincrónica o asincrónica? */
				if (sync)
				{
					// Replicación sincrónica: Debo procesar el XML en este preciso momento 
					// 		Si quedan pendientes replicaciones OFFLINE, en ese momento 
					//		deberá procesar todo el changelog existente hasta el momento
					//		(en caso de que force es true, saltear los pendientes en tabla asincronica)
					boolean allProcessed = force ? true : MAsyncReplication.processPendingContentInAsyncReplication(ctx, AD_Org_ID, m_trxName);
					if (allProcessed)
						ReplicationXMLUpdater.processChangelog(contentXML, m_trxName, AD_Org_ID, initialChangelogID, finalChangelogID, isDelayedInsert);
					else
						addToAsyncTable(ctx, contentXML, sync, arrayPos, initialChangelogID, finalChangelogID, AD_Org_ID, isDelayedInsert);
				}
				else
					addToAsyncTable(ctx, contentXML, sync, arrayPos, initialChangelogID, finalChangelogID, AD_Org_ID, isDelayedInsert);
			}
			
			/* Commitear la transaccion */
			Trx.getTrx(m_trxName).commit();
			
		}
		catch (Exception e)
		{
			/* Excepcion general - Abortar transaccion para evitar bloqueos */
			log.log(Level.SEVERE, " Imposible replicar desde organizacion:" + arrayPos );
			Trx.getTrx(m_trxName).rollback();
			
			/* Excepcion de comunicación o excepcion general */
			if (e instanceof RemoteException)
				throw (RemoteException)e;
			else
				throw new RemoteException(e.getMessage());
		}
		finally
		{
			/* Cerrar la transaccion */
			Trx.getTrx(m_trxName).close();
		}
		
		return "";
	}
	
	/**
	 * Incorpora la entrada a la tabla de replicación offline
	 */
	private void addToAsyncTable(Properties ctx, String contentXML, boolean sync, int arrayPos, int initialChangelogID, int finalChangelogID, int AD_Org_ID, boolean delayedInsert) throws Exception
	{
		/* Replicación asincrónica: Solo guardar el XML, el cual será procesado en otro momento */
		X_AD_AsyncReplication asyncReplication = new X_AD_AsyncReplication(ctx, 0, m_trxName);
		asyncReplication.setasync_action(delayedInsert ? MAsyncReplication.ASYNC_ACTION_DelayedReplicate : X_AD_AsyncReplication.ASYNC_ACTION_Replicate);
		asyncReplication.setasync_content(contentXML);
		asyncReplication.setORG_Source_ID(AD_Org_ID);
		asyncReplication.setInitialChangelog_ID(initialChangelogID);
		asyncReplication.setFinalChangelog_ID(finalChangelogID);
		if (!asyncReplication.save())
			throw new Exception ("Error al persistir en tabla de replicacion asincronica");
	}
	
	
	/**
	 * Verifica que la petición efectivamente sea de la sucursal origen.
	 * @param ctx el contexto de Libertya
	 * @throws Exception en caso de que la validación no sea correcta
	 */
	protected void validateReplicationRequest(Properties ctx, int arrayPos, String trxName) throws Exception
	{
		/* Obtener Key origen y destino */
		String sourceKey = Env.getContext(ctx, ReplicationClientProcess.VALIDATION_PROPERTY);
		String targetKey = DB.getSQLValueString(trxName, ReplicationClientProcess.getAccessKeySQLQuery(false), arrayPos);
		
		/* En caso de diferir, levantar la excepcion correspondiente */
		if (!sourceKey.equals(targetKey))
			throw new Exception(" Error en AccessKey desde sucursal " + arrayPos);
	}
	
	
	// ------------------ Implementación de métodos de SessionBean ------------------ 
 
	@Override
	public void ejbActivate() throws EJBException, RemoteException {
		if (log == null)
			log = CLogger.getCLogger(getClass());
		log.info ("ejbActivate ");
	}
 
	@Override
	public void ejbPassivate() throws EJBException, RemoteException {
		log.info ("ejbPassivate ");
	}
 
	@Override
	public void ejbRemove() throws EJBException, RemoteException {
		log.info ("ejbRemove ");
	}
 
	@Override
	public void setSessionContext(SessionContext aContext) throws EJBException,
			RemoteException {
		m_Context = aContext;
	}
	

}


package org.openXpertya.process;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.openXpertya.model.M_Table;
import org.openXpertya.model.X_AD_TableReplication;
import org.openXpertya.util.DB;
import org.openXpertya.util.Env;

/**
 * Crea el SQL correspondiente al trigger de 
 * replicación según la tabla donde actualmente 
 * se encuentra posicionado el usuario
 * 
 * CONVENCION IMPORTANTE:
 * """"""""""""""""""""""
 * La columna nochangelog define si bitacorear o no un registro ('Y' no lo bitacorea, 'N' si lo hace)
 * Sin embargo, puede suceder que una tabla nunca fue marcada para que sea bitacoreada, pero 
 * posteriormente se descubre que dicha tabla REQUIERE ser bitacoreada a fin de replicarla.
 * 
 * Si en dicha tabla ya existen entradas, las mismas nunca fueron bitacoreadas, ni tampoco
 * poseen el retrieveUID correspondiente.  Una vez marcada dicha tabla como replicable, para poder 
 * "rellenar" la información faltante, se utiliza el parametro nochangelog = 'X'.  Denominamos
 * este caso como "replicación tardía de registros de una tabla"
 * 
 * Haciendo: UPDATE nombre_tabla SET nochangelog = 'X' WHERE condicion
 * Generará:
 * 			1) El retrieveUID correspondiente (si es que no lo tiene seteado) 
 * 			2) Una entrada en AD_Changelog_Replication: Inserción con las columnas del registro/s en cuestion
 * 			3) Otra entrada en AD_Changelog_Replication: Modificación de todas las columnas del registro/s en cuestion
 * 				Esto es por las dudas que el registro ya exista en el host destino, pero deba actualizarse
 *				toda la información del registro con los datos actualmente cargados.  Entonces: el punto 2
 *				garantiza que el registro se insertará si no existe y el punto 3 garantizará que el registro
 *				estará actualizado debido a que es una modificación más nueva que las posibles anteriores
 * Luego dejará nuevamente el campo nochangelog en estado 'N' para futuros bitacoreos.
 *
 */


public class CreateReplicationTriggerProcess extends SvrProcess {
	
	/* Tipo de alcance de aplicación del proceso */
	public static final String SCOPE_ALL_TABLES = "A";
	public static final String SCOPE_CONFIGURED = "C";
	public static final String SCOPE_THIS_RECORD = "R";
	
	// Si el proceso es disparado desde el arbol de menú, se supone que se desea
	// generar los triggers para TODAS las tablas de la base de datos
	// (la insercion masiva es de utilidad para determinar que tablas se bitacorean en cada circuito)
	protected String p_scope = "";
	
	protected M_Table table; 

	// Valor de retorno
	protected StringBuffer retValue = new StringBuffer("");
	
	// Columnas reservadas para replicacion, no deberian insertarse en metadatos directamente 
	protected static final String COLUMN_RETRIEVEUID = "retrieveUID";
	protected static final String COLUMN_NOCHANGELOG = "noChangelog";
	
	// Replication array dummy para relleno unicamente
	protected static final String DUMMY_REPARRAY = "0";
	
	
	@Override
	protected void prepare() {

		// Parametro de alcance de la aplicación (scope)
		ProcessInfoParameter[] para = getParameter();
        for( int i = 0;i < para.length;i++ ) {
            String name = para[ i ].getParameterName();
            if( name.equals( "Scope" ))
                p_scope = (String)para[ i ].getParameter();
        }
		
		retValue = new StringBuffer(" - Resultados de la ejecucion - \n");
		
	}

	
	@Override
	protected String doIt() throws Exception 
	{
		
		// Query según el tipo de alcance
		String scopeClause = "";
		if (SCOPE_ALL_TABLES.equalsIgnoreCase(p_scope))
			scopeClause = " AND substr(lower(lt.tablename), 1, 2) != 'ad' AND substr(lower(lt.tablename), 1, 1) NOT IN ('t', 'i')";
		if (SCOPE_CONFIGURED.equalsIgnoreCase(p_scope))
			scopeClause = " AND lt.AD_Table_ID IN " + getConfiguredTablesQuery();
		if (SCOPE_THIS_RECORD.equalsIgnoreCase(p_scope))
		{
			X_AD_TableReplication tableReplication = new X_AD_TableReplication(getCtx(), getRecord_ID(), get_TrxName());
			table = new M_Table(getCtx(), tableReplication.getAD_Table_ID(), get_TrxName());
			scopeClause = " AND lt.AD_Table_ID = " + table.getAD_Table_ID();
		}
		
		/* Recuperar los IDs de todas las tablas (sin contemplar tablas AD_ ya que las mismas son de metadatos para el caso allTables) */
		PreparedStatement pstmt = DB.prepareStatement( 	" SELECT lt.ad_table_id " +
														" FROM information_schema.tables pt " +
														" INNER JOIN ad_table lt ON lower(pt.table_name) = lower(lt.tablename) " +
														" WHERE pt.table_schema = 'libertya' " +
														" AND pt.table_type = 'BASE TABLE' " + scopeClause, get_TrxName());
		ResultSet rs = pstmt.executeQuery();
		
		while (rs.next())
		{
			// Recuperar el ID de la tabla
			table = new M_Table(getCtx(), rs.getInt("AD_Table_ID"), get_TrxName());
			
			// Si estamos realizando la insercion masiva, entonces tambien generar todas las entradas en AD_TableReplication
			if (SCOPE_ALL_TABLES.equalsIgnoreCase(p_scope))
			{
				// verificar si no existe una entrada previa
				int exists = DB.getSQLValue(get_TrxName(), " SELECT count(1) FROM AD_TableReplication " +
															" WHERE AD_Client_ID = " + Env.getAD_Client_ID(getCtx()) + 
															" AND AD_Table_ID = " + table.getAD_Table_ID());
				
				// si no hay entrada, entonces aplicar a la tabla
				if (exists == 0)
				{
					// Genera una entrada dummy para cada tabla en la tabla de replicación por tabla, 
					// de esta manera, la bitacora sera creada en cada caso 
					// (dado que los triggers validan la existencia en esta tabla)
					System.out.print(".");
					X_AD_TableReplication tr = new X_AD_TableReplication(getCtx(), 0, get_TrxName());
					tr.setClientOrg(getAD_Client_ID(), Env.getAD_Org_ID(getCtx()));
					tr.setAD_Table_ID(table.getAD_Table_ID());
					tr.setReplicationArray(DUMMY_REPARRAY); // <- Un valor base que se deberá cambiar segun sea necesario (deberá ser 1 para las tablas que queremos bitacorear)
					tr.save();
				}
			}
			else
				retValue.append(" [" + table.getTableName() + "] ");
			
			// Query resultante
			StringBuffer sql = new StringBuffer(""); 

			// Campos necesarios para replicación
			appendNewColumns(sql);
			
			// Rellena la columna retrieveUID con valor general para registros existentes (se supone que son registros comunes a todas las sucursales)
			appendSQLFillRetrieveUID(sql);
			
			// Encabezado del procedure para generación del UID
			appendProcedureUID(sql);
			
			// Encabezado del procedure
			appendSQLHeaderFunction(sql);

			// Cada una de las columnas contenidas en la tabla
			appendSQLColumnsFunction(sql);

			// Pie de procedure e invocación a procedure para insertar en AD_Changelog_Replication
			appendSQLFooterFunction(sql);

			// Creación de los triggers que invocan a este procedure
			appendSQLTrigger(sql);
			
			// Impactar en base de datos
			DB.executeUpdate( sql.toString(), false , get_TrxName(), true );	
		}
				
		/* Si se ejecuta sobre todas las tablas, solo mostrar OK */
		return SCOPE_ALL_TABLES.equalsIgnoreCase(p_scope) ? "OK" : retValue.toString();
	}

	
	/**
	 *  Devuelve un String con el query que selecciona el conjunto de tablas que se 
	 *  encuentran configuradas para replicación, dentro de la tabla AD_TableReplication.
	 *  Unicamente las tablas que tienen un replicationArray valido (diferente a 0)
	 */
	protected String getConfiguredTablesQuery() throws Exception
	{
		int count = 0;
		StringBuffer values = new StringBuffer("(");
		PreparedStatement pstmt = DB.prepareStatement(" SELECT AD_Table_ID FROM AD_TableReplication " +
														" WHERE AD_Client_ID = " + getAD_Client_ID() + 
														" AND replicationArray <> '" + DUMMY_REPARRAY + "' ", 
														get_TrxName());
		ResultSet rs = pstmt.executeQuery();
		while (rs.next())
		{
			count++;
			values.append(rs.getInt("AD_Table_ID")).append(",");
		}
		values.replace(values.length()-1, values.length()-1, ")");
		
		return count == 0 ? "()" : values.toString().substring(0, values.toString().length()-1);
	}
	
	protected void appendNewColumns(StringBuffer sql)
	{
		// Columna retrieveUID
		if (!existColumnInTable(COLUMN_RETRIEVEUID, table.getTableName()))
		{
			append (sql, " ALTER TABLE " + table.getTableName() + " ADD COLUMN " + COLUMN_RETRIEVEUID + " varchar(100);" );
			retValue.append(" - Creada columna: " + COLUMN_RETRIEVEUID + " \n");
		}
		else
			retValue.append(" - Columna " + COLUMN_RETRIEVEUID + " ya existe en la tabla" + " \n");
		
		// Columna noChangelog
		if (!existColumnInTable(COLUMN_NOCHANGELOG, table.getTableName()))
		{
			append (sql, " ALTER TABLE " + table.getTableName() + " ADD COLUMN " + COLUMN_NOCHANGELOG + " char(1) DEFAULT 'N';" );
			retValue.append(" - Creada columna: " + COLUMN_NOCHANGELOG + " \n");
		}
		else
			retValue.append(" - Columna " + COLUMN_NOCHANGELOG + " ya existe en la tabla" + " \n");
	}
	
	
	/**
	 * Rellena el campo retrieveUID para las entradas ya existentes en la tabla (se suponen valores comunes a todas las sucursales)
	 * Para determinar esto, nos fijamos si existe en columna AD_ComponentObjectUID.  Todo registro que sea parte de un componente
	 * (o del core) debe contar con la información de esta columna para futuras referencias.  Todo registro generado durante el uso
	 * cotidiano el sistema, no tendrá seteado este dato.  De esta manera podemos distinguir registros del core de los que no lo son.
	 * @param sql
	 */
	protected void appendSQLFillRetrieveUID(StringBuffer sql)
	{
		append (sql, " UPDATE " + table.getTableName() + " SET " + COLUMN_RETRIEVEUID + " = " + generateRecordUID(table, "", true) + 
					 " WHERE " + COLUMN_RETRIEVEUID + " IS NULL AND (AD_Client_ID = " + getAD_Client_ID() + " OR AD_Client_ID = 0) ");

		// existe la column AD_ComponentObjectUID? (ciertas tablas lo tienen, pero otras como C_Invoice no lo tienen)
		String tmpSQL = " select count(1) " +
						" from information_schema.columns " + 
						" where lower(table_name) = lower('" + table.getTableName() + "') " + 
						" and lower(column_name) = lower('AD_ComponentObjectUID') ";	 

		// si existe, concatenar esta condicion
		if (DB.getSQLValue(get_TrxName(), tmpSQL) == 1)
			append (sql, " AND AD_ComponentObjectUID IS NOT NULL ");
		
		append (sql, ";");
	}
	
	
	/**
	 * Procedure para la creación del retrieveUID
	 */
	protected void appendProcedureUID(StringBuffer sql)
	{
		append( sql, " CREATE OR REPLACE FUNCTION replication_generateReplicationUID_" + table.getTableName().toLowerCase() + "()");
		append( sql, " RETURNS trigger AS " );
		append( sql, " $BODY$ " );
		append( sql, " DECLARE " );
		append( sql, " replicationPos integer; " );
		append( sql, " BEGIN " );
		append( sql, "   IF NEW.retrieveUID IS NULL OR NEW.retrieveUID = '' THEN " );
		append( sql, "     SELECT INTO replicationPos replicationArrayPos FROM AD_ReplicationHost WHERE thisHost = 'Y'; ");
		append( sql, "     IF replicationPos IS NULL THEN RAISE EXCEPTION 'Configuracion de Hosts incompleta: Ninguna sucursal tiene marca de Este Host'; END IF; ");
		append( sql, "     NEW.retrieveUID = " + generateRecordUID(table, "NEW.", false) + "; " );
		append( sql, " END IF;" );
		append( sql, " RETURN NEW; ");
		append( sql, " END; ");
		append( sql, " $BODY$ ");
		append( sql, " LANGUAGE 'plpgsql' VOLATILE; ");
	}
	
	
	/**
	 * Encabezado
	 */
	protected void appendSQLHeaderFunction(StringBuffer sql)
	{
		append( sql, " CREATE OR REPLACE FUNCTION replication_log_entry_" + table.getTableName().toLowerCase() + "() " );
		append( sql, " RETURNS trigger AS " );
		append( sql, " $BODY$ " );
		append( sql, " DECLARE " );
		append( sql, " xml varchar; " );
		append( sql, " found integer; " );
		append( sql, " cant integer; " );	
		append( sql, " BEGIN " );
		append( sql, " IF TG_OP = 'INSERT' OR TG_OP = 'UPDATE' THEN " );
		append( sql, "   SELECT INTO found replication_is_valid_table(NEW.AD_Client_ID," + table.getAD_Table_ID()+ ", NEW.noChangelog, NEW.retrieveUID); ");
		append( sql,  " ELSEIF TG_OP = 'DELETE' THEN ");
		append( sql, "   SELECT INTO found replication_is_valid_table(OLD.AD_Client_ID," + table.getAD_Table_ID()+ ", OLD.noChangelog, OLD.retrieveUID); ");
		append( sql, " END IF; " );
		append( sql, " IF found = 0 THEN RETURN NULL; END IF; ");
		append( sql, " cant := 0; " );
		append( sql, " xml := replication_xml_append_header() || replication_xml_append_open_tag(); " );
	}

	
	/**
	 * Columnas
	 */
	protected void appendSQLColumnsFunction(StringBuffer sql) throws Exception
	{
		PreparedStatement stmt = DB.prepareStatement(getColumnForTriggerSQL(), get_TrxName());
		ResultSet rs = stmt.executeQuery();
		appendColumns(sql, rs, "INSERT");

		stmt = DB.prepareStatement(getColumnForTriggerSQL(), get_TrxName());
		rs = stmt.executeQuery();
		appendColumns(sql, rs, "UPDATE");
	}

	
	/**
	 * Insercion de columnas
	 */
	protected void appendColumns(StringBuffer sql, ResultSet rs, String operation) throws Exception
	{
		String columnName = null;
		int columnID = -1;
		
		// Iterar por las columnas a fin de crear el query		
		append( sql,  " IF TG_OP = '" + operation +"' THEN " );
		while (rs.next())
		{
			columnName = rs.getString(1);
			columnID = rs.getInt(2);
			
			if (operation.equals("INSERT"))
			{
				append( sql,  " 	IF NEW." + columnName + " IS NOT NULL THEN ");
				append( sql,  " 		xml := xml || replication_xml_append_column("+columnID+", NEW."+columnName+"::varchar); ");
				append( sql,  "			cant := cant + 1; ");
				append( sql,  " 	END IF;");
			}
			if (operation.equals("UPDATE"))
			{	
				append( sql,  " 	IF NEW.nochangelog = 'X' OR (NEW." + columnName + " != OLD." + columnName + ") OR (NEW." + columnName + " IS NOT NULL AND OLD." + columnName + " IS NULL) THEN ");
				append( sql,  " 		xml := xml || replication_xml_append_column("+columnID+", NEW."+columnName+"::varchar); ");
				append( sql,  "			cant := cant + 1; ");
				append( sql,  " 	END IF;");
			}
		}
		append( sql,  " END IF; " );
	}
	
	
	/**
	 * Pie de procedure
	 */
	protected void appendSQLFooterFunction(StringBuffer sql)
	{
		append( sql,  " xml := xml || replication_xml_append_close_tag(); ");
		append( sql,  " IF (cant > 0 AND TG_OP = 'INSERT') THEN " );
		append( sql,  " 	PERFORM replication_insert_in_changelog(NEW.AD_Client_ID, NEW.AD_Org_ID, NEW.UpdatedBy, TG_ARGV[0]::int, NEW.retrieveUID, TG_ARGV[1]::varchar, xml); ");
		append( sql,  " ELSEIF (cant > 0 AND TG_OP = 'UPDATE') THEN " );
		append( sql,  " 	IF NEW.nochangelog = 'X' THEN " ); // <- caso para replicacion tardia (registros ya existentes (no comunes a todas las organizaciones) que deberan replicarse)
		append( sql,  " 		PERFORM replication_insert_in_changelog(OLD.AD_Client_ID, OLD.AD_Org_ID, OLD.UpdatedBy, TG_ARGV[0]::int, NEW.retrieveUID, 'I'::varchar, xml); ");
		append( sql,  " 		PERFORM replication_insert_in_changelog(OLD.AD_Client_ID, OLD.AD_Org_ID, OLD.UpdatedBy, TG_ARGV[0]::int, NEW.retrieveUID, 'M'::varchar, xml); ");
		append( sql,  " 		UPDATE " + table.getTableName() + " SET nochangelog = 'N' WHERE retrieveUID = NEW.retrieveUID;" );
		append( sql,  " 	ELSE " );
		append( sql,  " 		PERFORM replication_insert_in_changelog(NEW.AD_Client_ID, NEW.AD_Org_ID, NEW.UpdatedBy, TG_ARGV[0]::int, OLD.retrieveUID, TG_ARGV[1]::varchar, xml); ");
		append( sql,  "     END IF; " );
		append( sql,  " ELSEIF (TG_OP = 'DELETE') THEN ");
		append( sql,  " 	PERFORM replication_insert_in_changelog(OLD.AD_Client_ID, OLD.AD_Org_ID, OLD.UpdatedBy, TG_ARGV[0]::int, OLD.retrieveUID, TG_ARGV[1]::varchar, xml); ");
		append( sql,  " END IF; ");
		append( sql,  " RETURN NULL; ");
		append( sql,  " END; ");
		append( sql,  " $BODY$ ");
		append( sql,  " LANGUAGE 'plpgsql' VOLATILE; ");
	}
	
	/**
	 * Triggers para insert, update y delete
	 */
	protected void appendSQLTrigger(StringBuffer sql) throws Exception
	{
		// Trigger para creación del UID
		append( sql,  " DROP TRIGGER IF EXISTS replication_generateReplicationUID ON " + table.getTableName() + ";" );
		append( sql,  " CREATE TRIGGER replication_generateReplicationUID BEFORE INSERT OR UPDATE ON " + table.getTableName() + 
					  " FOR EACH ROW EXECUTE PROCEDURE replication_generateReplicationUID_" + table.getTableName().toLowerCase() + "(); ");
		
		// Triggers para bitacoreo
		appendTrigger(sql, "insert", table.getTableName(), table.getAD_Table_ID(), "I");
		appendTrigger(sql, "update", table.getTableName(), table.getAD_Table_ID(), "M");
		appendTrigger(sql, "delete", table.getTableName(), table.getAD_Table_ID(), "D");
		
		retValue.append(" - Creados los triggers: replication_generateReplicationUID y replication_log_entry \n");
	}

	/**
	 * Trigger
	 */
	protected void appendTrigger(StringBuffer sql, String action, String tableName, int tableID, String opType)
	{
		append( sql,  " DROP TRIGGER IF EXISTS replication_log_" + action + " on " + tableName + "; " );
		append( sql,  " CREATE TRIGGER replication_log_" + action + " AFTER " + action.toUpperCase() + " ON " + tableName + 
					  " FOR EACH ROW EXECUTE PROCEDURE replication_log_entry_" + tableName.toLowerCase() + "(" + tableID + ",'" + opType + "'); ");
	}
	
	/**
	 * Genera el query para la insercion del recordUID, usando las columnas clave de la tabla dada
	 * FORMATO UID: h+ReplicationArrayPos_KEY1-KEY2-...-KEYN_TIMESTAMP
	 * IMPORTANTE: El orden de las key columns es determinado por getKeyColumns().
	 * 
	 * En el caso especial useOrg, no lee la información de la organización, sino directamente utiliza
	 * el AD_Org_ID, esto se supone para los casos en los que ya existe información previa en la tabla
	 * (datos en comun a Libertya para todas las organizaciones).  Para esos casos, el retrieveUID es:
	 * o+AD_Org_IDdelRegistro_KEY1-KEY2-...-KEYN
	 */
	public static String generateRecordUID(M_Table aTable, String prefix, boolean useOrg)
	{
		// Generar el UID
		String uidType = useOrg ? " 'o' || " : " 'h' || ";
		String castVarchar = "::varchar";
		String fieldSeparator = " || '-' || ";
		String orgTimeSeparator = " || '_' || ";
		String retValue = uidType + (useOrg ? "AD_Org_ID" : "replicationPos") + castVarchar + orgTimeSeparator;
		for (String keyColumn : aTable.getKeyColumns())
			retValue += prefix + keyColumn + castVarchar + fieldSeparator; 
		
		// Quitar el ultimo par de || para concatenar a nivel SQL
		retValue = retValue.substring(0, retValue.length()-fieldSeparator.length());
		
		// Concatenar el TimeStamp
		return retValue; 
	}
	
	/**
	 * Query que devuelve las columnas de la tabla
	 * @return
	 */
	protected String getColumnForTriggerSQL()
	{
		return 	" SELECT ColumnName, AD_Column_ID " +
		 		" FROM AD_Column WHERE AD_Table_ID = " + table.getAD_Table_ID() + " " +
		 		" AND isActive = 'Y' AND columnsql IS NULL " +
				" AND lower(columnname) in " +
				" ( " +
				"		  select lower(column_name) from information_schema.columns " +
				"		  where table_name = '" + table.getTableName().toLowerCase() + "' " +
				" ) " +
				" AND lower(columnname) not in ( '" + COLUMN_RETRIEVEUID.toLowerCase() + "', '" + COLUMN_NOCHANGELOG.toLowerCase() + "') ";
	}
	
	/**
	 * Concatena
	 */
	protected void append(StringBuffer sql, String sentence)
	{
		sql.append(sentence + "\n"); // .append(BR);
	}
	

	/**
	 * Retorna 1 si la tabla ya posee la columna correspondientes 
	 */
	protected boolean existColumnInTable(String column, String table)
	{
		int cant = DB.getSQLValue(get_TrxName(), " select COUNT(1) from information_schema.columns " +
												 " where lower(table_name) = '" + table.toLowerCase() + "'" +
												 " and lower(column_name) ='" + column.toLowerCase() + "'");
		
		return cant > 0;
	}
	
	
	/**
	 * Elimina cualquier trigger previo relacionado con replicacion
	 * @throws Exception
	 */
	public static void dropPreviousTriggers(String trxName) throws Exception
	{
		PreparedStatement pstmt = DB.prepareStatement( 	" SELECT lt.tablename " +
														" FROM information_schema.tables pt " +
														" INNER JOIN ad_table lt ON lower(pt.table_name) = lower(lt.tablename) " +
														" WHERE pt.table_schema = 'libertya' " +
														" AND pt.table_type = 'BASE TABLE' ", trxName);
		
		ResultSet rs = pstmt.executeQuery();
		while (rs.next())
		{
			DB.executeUpdate(" DROP TRIGGER IF EXISTS replication_generateReplicationUID ON " + rs.getString("tablename"), trxName);
			DB.executeUpdate(" DROP TRIGGER IF EXISTS replication_log_" + "insert" + " on " + rs.getString("tablename"), trxName);
			DB.executeUpdate(" DROP TRIGGER IF EXISTS replication_log_" + "update" + " on " + rs.getString("tablename"), trxName);
			DB.executeUpdate(" DROP TRIGGER IF EXISTS replication_log_" + "delete" + " on " + rs.getString("tablename"), trxName);			
		}

	}

}


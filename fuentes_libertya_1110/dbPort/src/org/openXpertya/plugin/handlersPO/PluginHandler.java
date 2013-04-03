package org.openXpertya.plugin.handlersPO;

import java.lang.reflect.Constructor;
import java.util.Properties;

import org.openXpertya.model.PO;
import org.openXpertya.plugin.MPluginPO;
import org.openXpertya.plugin.common.PluginConstants;

public class PluginHandler {

	
	/**
	 * Obtiene una copia del PO que está siendo procesado (save, delete, process).
	 * @param po el po que está siendo procesado
	 * @param plugin para el cual crear la instancia
	 * @return Una instancia de LP_ con los datos copiados, o null si ésta no existe 
	 */
	protected PO getLPluginPO(PO po, MPluginPO plugin)
	{
		PO copy = null;
		
		try
		{
			Class<?> clazz = Class.forName(plugin.getPackageName() + "." + PluginConstants.PACKAGE_NAME_MODEL + "." + PluginConstants.LIBERTYA_PLUGIN_PREFIX + po.get_TableName() );
			
			Class<?>[] paramTypes = { Properties.class, int.class, String.class };
			Object[] args = { po.getCtx(), po.getID(), po.get_TrxName() };			
			Constructor<?> cons = clazz.getConstructor(paramTypes);

			copy = (PO)cons.newInstance(args);	

			PO.deepCopyValues(po, copy);
		}
		catch (Exception e)	{ 
			// no existe clase LP_ correspondiente
			}

		return copy;

	}
	
}

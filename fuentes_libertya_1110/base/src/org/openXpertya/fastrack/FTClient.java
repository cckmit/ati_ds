package org.openXpertya.fastrack;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;

import org.openXpertya.model.MAcctProcessor;
import org.openXpertya.model.MBPGroup;
import org.openXpertya.model.MBPartner;
import org.openXpertya.model.MBPartnerLocation;
import org.openXpertya.model.MClient;
import org.openXpertya.model.MDiscountSchema;
import org.openXpertya.model.MDiscountSchemaBreak;
import org.openXpertya.model.MDiscountSchemaLine;
import org.openXpertya.model.MFormAccess;
import org.openXpertya.model.MLocation;
import org.openXpertya.model.MLocator;
import org.openXpertya.model.MOrg;
import org.openXpertya.model.MPaySchedule;
import org.openXpertya.model.MPaymentTerm;
import org.openXpertya.model.MPriceList;
import org.openXpertya.model.MPriceListVersion;
import org.openXpertya.model.MProcessAccess;
import org.openXpertya.model.MProduct;
import org.openXpertya.model.MProductCategory;
import org.openXpertya.model.MRetSchemaConfig;
import org.openXpertya.model.MRetencionProcessor;
import org.openXpertya.model.MRetencionSchema;
import org.openXpertya.model.MRetencionType;
import org.openXpertya.model.MRole;
import org.openXpertya.model.MRoleOrgAccess;
import org.openXpertya.model.MSequence;
import org.openXpertya.model.MTabAccess;
import org.openXpertya.model.MTax;
import org.openXpertya.model.MTaxCategory;
import org.openXpertya.model.MTree;
import org.openXpertya.model.MTreeBar;
import org.openXpertya.model.MTree_NodeMM;
import org.openXpertya.model.MUOM;
import org.openXpertya.model.MUOMConversion;
import org.openXpertya.model.MUOMGroup;
import org.openXpertya.model.MUser;
import org.openXpertya.model.MUserRoles;
import org.openXpertya.model.MWarehouse;
import org.openXpertya.model.MWindowAccess;
import org.openXpertya.model.PO;
import org.openXpertya.model.X_AD_TreeBar;
import org.openXpertya.print.MPrintForm;
import org.openXpertya.util.DB;
import org.openXpertya.util.Env;
import org.openXpertya.wf.MWorkflowAccess;

public class FTClient extends FTModule {

	//Variables de instancia
	
	/** Configuraci??n de la creaci??n del cliente y su configuraci??n */
	
	private FTConfiguration ftConfig;
	private static final int USER_SUPERVISOR_ID = 100;
	
	/** 
	 * Colecci??n clave valor que determina los roles creados con los roles template 
	 *  <ul>
	 *  <li>Clave: id del rol template</li>
	 *  <li>Valor: MRole creado nuevo</li>
	 *  </ul> 
	 */
	
	private HashMap roles = new HashMap(); 
	
	/** 
	 * Colecci??n clave valor que determina las organizaciones creadas con las organizaciones template 
	 *  <ul>
	 *  <li>Clave: id de la org template</li>
	 *  <li>Valor: MOrg creado nuevo</li>
	 *  </ul> 
	 */
	
	
	private HashMap<Integer, MUser> users = new HashMap<Integer, MUser>(); 
	
	/** 
	 * Colecci??n clave valor que almacena los usuarios creados a partir de la compa????a template 
	 *  <ul>
	 *  <li>Clave: id del usuario cia template</li>
	 *  <li>Valor: MUser creado nuevo</li>
	 *  </ul> 
	 */	
	
	private HashMap orgs = new HashMap(); 
	

	/** 
	 * Colecci??n clave valor que determina los MTree creados con las organizaciones template 
	 *  <ul>
	 *  <li>Clave: id del tree template</li>
	 *  <li>Valor: MTree creado nuevo</li>
	 *  </ul> 
	 */
	
	private HashMap<Integer, Integer> trees = new HashMap<Integer, Integer>(); 
	
	
	public HashMap<Integer, Integer> getTrees() {
		return trees;
	}

	public void setTrees(HashMap<Integer, Integer> trees) {
		this.trees = trees;
	}


	/** 
	 * Colecci??n que contiene el listado de accesos directos a las entradas del menu
	 * Las entradas corresponden a las contenidas en la plantilla por el usuario Supervisor  
	 */
	private ArrayList<X_AD_TreeBar> treeBarEntries = new ArrayList();
	
	public ArrayList<X_AD_TreeBar> getTreeBarEntries() {
		return treeBarEntries;
	}

	public void setTreeBarEntries(ArrayList<X_AD_TreeBar> treeBarEntries) {
		this.treeBarEntries = treeBarEntries;
	}


	/** Contexto */
	
	private Properties ctx = Env.getCtx();
	
	
	//Constructores
	
	public FTClient() {
	}

	public FTClient(FTConfiguration ftConfig){
		this.setTrxName(ftConfig.getTrxName());
		this.setFtConfig(ftConfig);
	}
	
	//Getters y Setters
	
	public int getClientTemplate() {
		return this.getFtConfig().getClient_template_id();
	}

	public int getNewClient() {
		return this.getFtConfig().getNew_client_id();
	}
	
	public void setRoles(HashMap roles) {
		this.roles = roles;
	}

	public HashMap getRoles() {
		return roles;
	}

	public void setOrgs(HashMap orgs) {
		this.orgs = orgs;
	}

	public HashMap getOrgs() {
		return orgs;
	}

	public void setCtx(Properties ctx) {
		this.ctx = ctx;
	}

	public Properties getCtx() {
		return ctx;
	}

	public void setFtConfig(FTConfiguration ftConfig) {
		this.ftConfig = ftConfig;
	}

	public FTConfiguration getFtConfig() {
		return ftConfig;
	}
	
	
	//M??todos varios		


	private int getOrgId(int AD_Org_ID){
		int org = 0;
		
		if(this.getOrgs().containsKey(AD_Org_ID)){
			org = ((MOrg)this.getOrgs().get(AD_Org_ID)).getID();
		}
		
		return org;
	}
	
	
	private void setCtx(int AD_Client_ID){
		//Seteo el contexto con la compa????a nueva 
		Env.setContext(this.getCtx(), "#AD_Client_ID", AD_Client_ID);
	}
	
	private void setCtx(int AD_Client_ID,int AD_Org_ID){
		this.setCtx(AD_Client_ID);
		//Seteo el contexto con la organizaci??n 
		Env.setContext(this.getCtx(), "#AD_Org_ID", AD_Org_ID);
	}	
	
	
	
	private void createInitialInfo() throws Exception{
		//Creo las organizaciones de la compa????a
		this.createOrgs();
		
		//Crea los perfiles y los guarda en la colecci??n
		this.createRoles();
		
		//Crea los accesos de los roles a los componentes
		this.createMenuAccess();
		
		//Creo los perfiles de las organizaciones
		this.createRoleOrgAccess();
		
		// Copiar los formatos de impresi??n (Bot??n Imprimir) de System
		this.createPrintForm();
		
		// Generar las entradas de accesos directos del emnu
		this.createTreeBarEntries();
	}
	
	/**
	 * Carga las entradas de accesos directos del men??
	 */
	private void createTreeBarEntries()
	{
		String sql = "";
		try
		{
			sql = "SELECT * FROM AD_TREEBAR WHERE AD_CLIENT_ID = " + getClientTemplate();
			PreparedStatement stmt = DB.prepareStatement(sql, getTrxName());
			ResultSet rs = stmt.executeQuery();
			while (rs.next())
				treeBarEntries.add(new X_AD_TreeBar(getCtx(), rs, getTrxName()));
		}
		catch (Exception e)
		{
			this.getLog().log(Level.SEVERE, sql, e);
		}
		
	}
	
	/**
	 * Realiza la configuraci??n de la compa????a para que sea utilizable 
	 */
	private void configureClient() throws Exception{
		//Seteo el contexto con el cliente nuevo y la org 0
		this.setCtx(this.getNewClient(), 0);
		
		Map<Integer, Integer> taxCategories = new HashMap<Integer, Integer>();
		Map<Integer, Integer> locations = new HashMap<Integer, Integer>();
		
		// Creo la categor??a del producto 
		
		MProductCategory pc = new MProductCategory( this.getCtx(),0,this.getTrxName());

        pc.setValue( "Standard" );
        pc.setName( "Standard" );
        pc.setIsDefault( true );
		
        pc.save();
        
		//Copiar las categor??as de Impuesto
		this.createTaxCategories(taxCategories);
		
		// Copiar las UOM y sus conversiones
		int c_uom_id = this.createUOMs();
		
		//Crear producto
		MProduct product = new MProduct(this.getCtx(),0,this.getTrxName());
		
		product.setValue( "Standard" );
        product.setName( "Standard" );
		product.setM_Product_Category_ID(pc.getID());
		product.setC_TaxCategory_ID(taxCategories.values().iterator().next());
		product.setC_UOM_ID((c_uom_id == 0)?100:c_uom_id);
		
		product.save();
		
		// Crear las tarifas
		this.createDiscountSchemaAndPriceLists();
		
		// Copiar los esquemas de vencimiento
		this.createPaymentTerm();
		
		// Copiar el procesador de contabilidad
		this.createAccountingProcessor();
				
		// Copiar los warehouses
		this.createWarehousesAndLocators(locations);
		
		// Copiar la configuraci??n de retenciones
		this.createInfoRetenciones(taxCategories,c_uom_id,pc.getID(),locations);
	}
	
	
	/**
	 * Crea la info de retenciones a partir de la compa????a template
	 * @param taxCategories categor??as de impuesto para los productos de los tipos de retenci??n
	 * @param c_uom_id uom id para los productos de los tipos de retenci??n
	 * @param productCategory categoria de producto para los productos de los tipos de retenci??n
	 * @throws Exception
	 */
	private void createInfoRetenciones(Map<Integer,Integer> taxCategories,int c_uom_id,int productCategory, Map<Integer,Integer> locations) throws Exception{
		Map<Integer, Integer> retenc_procesadores = new HashMap<Integer, Integer>();
		Map<Integer, Integer> retenc_types = new HashMap<Integer, Integer>();
		Map<Integer, Integer> entes_recaudadores = new HashMap<Integer, Integer>();
		Map<Integer, Integer> grupo_entes = new HashMap<Integer, Integer>();
				
		// Crear tipos de retenci??n
		this.createRetencionTypes(retenc_types,taxCategories,c_uom_id,productCategory);
		
		// Crear procesador de retenciones
		this.createRetencionProcesador(retenc_procesadores);
		
		// Esquemas de retenciones
		
		// Seteo el contexto con la compa????a template para el getOfClient
		this.setCtx(this.getClientTemplate());
		
		// Obtener esquemas de retenciones para luego copiarlos		
		List<MRetencionSchema> esquemas = MRetencionSchema.getOfClient(this.getCtx(), this.getTrxName()); 
		
		// Seteo el contexto con la compa????a nueva 
		this.setCtx(this.getNewClient());
		MRetencionSchema newEsquema;
		int id_ente;
		
		for (MRetencionSchema retencionSchema : esquemas) {
			newEsquema = new MRetencionSchema(this.getCtx(),0,this.getTrxName());
			
			// Si no fue creado antes el ente recaudador, lo creo
			if(!entes_recaudadores.containsKey(retencionSchema.getC_BPartner_Recaudador_ID())){
				// Creo el ente recaudador y lo agrego al map
				id_ente = this.createEnteRecaudador(retencionSchema.getC_BPartner_Recaudador_ID(),grupo_entes,locations);
				entes_recaudadores.put(retencionSchema.getC_BPartner_Recaudador_ID(),id_ente); 
			}
			else{
				id_ente = entes_recaudadores.get(retencionSchema.getC_BPartner_Recaudador_ID());
			}
			
			// Copio los valores
			MRetencionSchema.copyValues(retencionSchema, newEsquema);
			newEsquema.setC_RetencionProcessor_ID(retenc_procesadores.get(retencionSchema.getC_RetencionProcessor_ID()));
			newEsquema.setC_RetencionType_ID(retenc_types.get(retencionSchema.getC_RetencionType_ID()));
			newEsquema.setC_BPartner_Recaudador_ID(id_ente);
			
			newEsquema.save();
			
			this.createEsquemaParams(retencionSchema,newEsquema);
		}
	}
	
	/**
	 * Crea el nuevo bpartner recaudador a partir del template
	 * @param bpartner_template_id bpartner template
	 * @param grupos asociacion entre los grupos template y los nuevos
	 * @param locations asociacion entre las locations template y las nuevas
	 * @return id del bpartner recaudador nuevo
	 * @throws Exception
	 */
	private int createEnteRecaudador(Integer bpartner_template_id, Map<Integer,Integer> grupos,Map<Integer,Integer> locations) throws Exception{
		// Seteo el contexto con la compa????a nueva 
		this.setCtx(this.getNewClient());
		
		MBPartner newPartner = new MBPartner(this.getCtx(),0,this.getTrxName());
		MBPartner partner = new MBPartner(this.getCtx(),bpartner_template_id,this.getTrxName());
		
		int bgroup_id = groupsRecaudador(partner,grupos); 
		
		MBPartner.copyValues(partner, newPartner);
		newPartner.setC_BP_Group_ID(bgroup_id);
		newPartner.save();
		
		// Crear las locations
		this.createPartnerLocations(partner, newPartner, locations);
		return newPartner.getID(); 
	}
	
	/**
	 * Crea las bpartner locations de la template
	 * @param partner partner template
	 * @param newPartner nuevo partner
	 * @param locations asociacion de las locations template con las creadas
	 */
	private void createPartnerLocations(MBPartner partner, MBPartner newPartner, Map<Integer,Integer> locations) throws Exception{
		MBPartnerLocation[] bpLocations = partner.getLocations(true);
		MBPartnerLocation newBPLocation;
		
		for (MBPartnerLocation partnerLocation : bpLocations) {
			newBPLocation = new MBPartnerLocation(this.getCtx(),0,this.getTrxName());
						
			MBPartnerLocation.copyValues(partnerLocation, newBPLocation);
			newBPLocation.setC_Location_ID(this.createLocation(partnerLocation));
			newBPLocation.setC_BPartner_ID(newPartner.getID());
			
			newBPLocation.save();
		}
	}
	
	/**
	 * Crea las locations del bpartner location
	 * @param bpartnerLocation bpartner location template
	 * @return id del location nuevo
	 * @throws Exception
	 */
	private int createLocation(MBPartnerLocation bpartnerLocation) throws Exception{
		MLocation locationTemplate = new MLocation(this.getCtx(),bpartnerLocation.getC_Location_ID(),this.getTrxName());
		MLocation newLocation = new MLocation(this.getCtx(),0,this.getTrxName());
		MLocation.copyValues(locationTemplate, newLocation);
		newLocation.save();
		return newLocation.getID();
	}
	
	/**
	 * Verifica si est?? el grupo del recaudador en grupos y si no est?? lo creo 
	 * @param partner business partner template
	 * @param grupos asociaci??n entre los grupos templates y los nuevos creados
	 * @return id del grupo para asociar al ente recaudador nuevo 
	 * @throws Exception
	 */
	private int groupsRecaudador(MBPartner partner, Map<Integer,Integer> grupos) throws Exception{
		int groupRetorno;
		// Si no lo contiene, lo creo
		if(!grupos.containsKey(partner.getC_BP_Group_ID())){
			groupRetorno = this.createGroupRecaudador(partner);
			grupos.put(partner.getC_BP_Group_ID(),groupRetorno);
		}
		else{
			groupRetorno = grupos.get(partner.getC_BP_Group_ID()).intValue();
		}
		
		return groupRetorno;
	}
	
	/**
	 * Crea el bpartner group a partir del template
	 * @param partner business partner con el grupo template
	 * @return id del grupo nuevo creado
	 * @throws Exception
	 */
	private int createGroupRecaudador(MBPartner partner) throws Exception{
		// Seteo el contexto con la compa????a nueva 
		this.setCtx(this.getNewClient());
		MBPGroup groupTemplate = new MBPGroup(this.getCtx(),partner.getC_BP_Group_ID(),this.getTrxName()); 
		MBPGroup newGroup = new MBPGroup(this.getCtx(),0,this.getTrxName());
		
		MBPGroup.copyValues(groupTemplate, newGroup);
		newGroup.save();
		return newGroup.getID();
	}
	
	/**
	 * Crea los par??metros del esquema de retencion template al esquema nuevo
	 * @param schemaTemplate esquema template
	 * @param newSchema nuevo esquema
	 */
	private void createEsquemaParams(MRetencionSchema schemaTemplate, MRetencionSchema newSchema) throws Exception{
		// Crear los par??metros del esquema a partir del esquema template
		// Seteo el contexto con la compa????a nueva 
		this.setCtx(this.getNewClient());
		// Obtengo los par??metros de la template
		Collection<Object> params = ((Map<String,Object>)schemaTemplate.getParameters()).values();
		MRetSchemaConfig newParam;
		
		// Itero por los par??metros del esquema y los voy copiando
		for (Object paramTemplate : params) {
			newParam = new MRetSchemaConfig(this.getCtx(),0,this.getTrxName());
			MRetSchemaConfig.copyValues((MRetSchemaConfig)paramTemplate, newParam);
			newParam.setC_RetencionSchema_ID(newSchema.getID());
			
			newParam.save();
		}		
	}
	
	/**
	 * Creo los tipos de retenciones
	 * @param types hashmap a inicializar con los tipos de retenci??n y las nuevas creadas
	 * @param taxCategories categorias de impuesto para el producto
	 * @param c_uom_id uom id para el producto
	 * @param productCategory subfamilia para el producto 
	 */
	private void createRetencionTypes(Map<Integer,Integer> types,Map<Integer,Integer> taxCategories,int c_uom_id,int productCategory) throws Exception{
		// Seteo el contexto con la compa????a template para el getOfClient
		this.setCtx(this.getClientTemplate());
		
		Map<Integer,Integer> productos = new HashMap<Integer, Integer>();
		
		// Obtengo los tipos de retenciones
		List<MRetencionType> list_types = MRetencionType.getOfClient(this.getCtx(), this.getTrxName());
		
		// Seteo el contexto con la compa????a nueva 
		this.setCtx(this.getNewClient());
		MRetencionType newType;
		int product_id;
		
		// Itero por los tipos de retenciones y los voy creando
		for (MRetencionType retencionType : list_types) {
			newType = new MRetencionType(this.getCtx(),0,this.getTrxName());

			// Si el producto no fue creado antes
			if(!productos.containsKey(retencionType.getM_Product_ID())){
				// crear el art??culo del tipo de retenci??n
				product_id = this.createRetencionProduct(retencionType.getM_Product_ID(),taxCategories,c_uom_id,productCategory);
				productos.put(retencionType.getM_Product_ID(),product_id);
			}
			else{
				product_id = productos.get(retencionType.getM_Product_ID()).intValue();
			}
			
			// Seteo los valores del nuevo tipo de retencion
			MRetencionType.copyValues(retencionType, newType);
			newType.setM_Product_ID(product_id);
			
			newType.save();
			
			types.put(retencionType.getID(), newType.getID());
		}
	}
	
	/**
	 * Crea el producto para el tipo de retenci??n
	 * @param product_template_id id del producto template
	 * @param taxCategories categor??as de producto para el producto
	 * @param c_uom_id uom id para el producto
	 * @param productCategory categor??a para el producto 
	 * @return id del producto nuevo
	 * @throws Exception
	 */
	private int createRetencionProduct(Integer product_template_id,Map<Integer,Integer> taxCategories,int c_uom_id,int productCategory) throws Exception{
		// Obtengo el producto template y creo el nuevo
		MProduct prod_template = new MProduct(this.getCtx(),product_template_id,this.getTrxName());
		MProduct newProduct = new MProduct(this.getCtx(),0,this.getTrxName());
		
		MProduct.copyValues(prod_template, newProduct);
		newProduct.setC_UOM_ID((c_uom_id == 0)?100:c_uom_id);
		newProduct.setM_Product_Category_ID(productCategory);
		newProduct.setC_TaxCategory_ID(taxCategories.get(prod_template.getC_TaxCategory_ID()));
		
		newProduct.save();
		return newProduct.getID();
	}
	
	/**
	 * Creo los procesadores de retenciones
	 * @param procesadores hashmap a inicializar con los procesadroes de retenci??n y las nuevas creadas
	 */
	private void createRetencionProcesador(Map<Integer,Integer> procesadores) throws Exception{
		// Seteo el contexto con la compa????a template para el getOfClient
		this.setCtx(this.getClientTemplate());
		
		List<MRetencionProcessor> list_processors = MRetencionProcessor.getOfClient(this.getCtx(), this.getTrxName());
				
		// Seteo el contexto con la compa????a nueva 
		this.setCtx(this.getNewClient());
		MRetencionProcessor newProcessor;
		
		for (MRetencionProcessor retencionProcessor : list_processors) {
			newProcessor = new MRetencionProcessor(this.getCtx(),0,this.getTrxName());
			// copiar los valores
			MRetencionProcessor.copyValues(retencionProcessor, newProcessor);
			newProcessor.save();
			
			// Agrego los dos en la map
			procesadores.put(retencionProcessor.getID(), newProcessor.getID());
		}
	}
	
	
	/**
	 * Crea las tarifas a partir de las tarifas template y las retorna en Map con: 
	 * <ul>
	 * <li>Clave = id de tarifa template</li>
	 * <li>Valor = id de la tarifa nueva</li>
	 * </ul> 
	 * @return las asociaciones en un Map
	 * @throws Exception
	 */
	private Map<Integer,Integer> createTarifasTemplateAndNew() throws Exception{
		// Seteo el contexto con la compa????a template para el getOfClient
		this.setCtx(this.getClientTemplate());
		
		Map<Integer,Integer> tarifas = new HashMap<Integer,Integer>();
		
		// Obtengo las tarifas de la compa????a template
		List<MPriceList> list = MPriceList.getOfClient(this.getCtx(), this.getTrxName());
		
		// Seteo el contexto con la compa????a nueva 
		this.setCtx(this.getNewClient());
		
		MPriceList newPriceList;
		
		// Creo y copio las tarifas
		for (MPriceList priceList : list) {
			//Creo la nueva tarifa
			newPriceList = new MPriceList(this.getCtx(),0,this.getTrxName());
			
			// Copio los datos
			MPriceList.copyValues(priceList, newPriceList);
			newPriceList.setC_Currency_ID(this.getFtConfig().getC_currency_id());
			
			// Guardo
			newPriceList.save();
			
			tarifas.put(priceList.getID(), newPriceList.getID());
		}
		
		return tarifas;
	} 
	
	/**
	 * Creo los cortes del esquema de vencimiento template para el esquema nuevo
	 * @param esquema_template
	 * @param id_esquema_nuevo
	 * @throws Exception
	 */
	private void createDiscountsBreaks(MDiscountSchema esquema_template,int id_esquema_nuevo) throws Exception{
		// Seteo el contexto con la compa????a nueva 
		this.setCtx(this.getNewClient());
		
		MDiscountSchemaBreak[] cortes = esquema_template.getBreaks(true);
		
		MDiscountSchemaBreak corte;
		
		// Itero por los cortes de ese esquema
		for (int i = 0; i < cortes.length; i++) {
			//Creo el nuevo corte
			corte = new MDiscountSchemaBreak(this.getCtx(),0,this.getTrxName());
			
			// Seteo los valores
			MDiscountSchemaBreak.copyValues(cortes[i], corte);
			corte.setM_DiscountSchema_ID(id_esquema_nuevo);
			
			// Guardo
			corte.save();
		}
	}	
	
	/**
	 * Copia las l??neas del esquema de descuento pasado como par??metro
	 * @param discountSchema_template
	 * @param newDiscountSchema
	 * @throws Exception
	 */
	private void createDiscountSchemaLines(MDiscountSchema discountSchema_template,int newDiscountSchema) throws Exception{
		// Obtengo las l??neas del esquema de descuento template
		MDiscountSchemaLine[] lines = discountSchema_template.getLines(true);
		
		// Seteo el contexto con la compa????a nueva 
		this.setCtx(this.getNewClient());
		
		MDiscountSchemaLine newLinea;
		
		for (int i = 0; i < lines.length; i++) {
			// Crear la nueva l??nea
			newLinea = new MDiscountSchemaLine(this.getCtx(),0,this.getTrxName());
			
			// Setear los valores de la template
			MDiscountSchemaLine.copyValues(lines[i],newLinea);
			newLinea.setM_DiscountSchema_ID(newDiscountSchema);
			newLinea.setC_BPartner_ID(0);
			newLinea.setM_Product_ID(0);
			newLinea.setM_Product_Family_ID(0);
			newLinea.setM_Product_Gamas_ID(0);
			newLinea.setM_Product_Category_ID(0);
			
			newLinea.save();
		}
		
	}
	
	/**
	 * Crea los esquemas de descuento con sus respectivos cortes y lo devuelve en un Map con:
	 * <ul>
	 * <li>Clave = id de tarifa template</li>
	 * <li>Valor = id de la tarifa nueva</li>
	 * </ul>
	 * @return
	 * @throws Exception
	 */
	private Map<Integer,Integer> createDiscountSchemasTemplateAndNew() throws Exception{
		// Seteo el contexto con la compa????a template para el getOfClient
		this.setCtx(this.getClientTemplate());
		
		Map<Integer,Integer> descuentos = new HashMap<Integer,Integer>();
		
		// Obtengo las tarifas de la compa????a template
		List<MDiscountSchema> list = MDiscountSchema.getOfClient(this.getCtx(), this.getTrxName());
		
		// Seteo el contexto con la compa????a nueva 
		this.setCtx(this.getNewClient());
		
		MDiscountSchema newDiscountSchema;
		
		for (MDiscountSchema discountSchema : list) {
			//Creo el nuevo
			newDiscountSchema = new MDiscountSchema(this.getCtx(),0,this.getTrxName());
			
			//Seteo los valores
			MDiscountSchema.copyValues(discountSchema, newDiscountSchema);
			
			// Guardo
			newDiscountSchema.save();
			
			//Creo los cortes asociados, si los hay
			this.createDiscountsBreaks(discountSchema,newDiscountSchema.getID());
			
			// Creo las l??neas del esquema de vencimiento creado a partir del template
			this.createDiscountSchemaLines(discountSchema,newDiscountSchema.getID());
			
			descuentos.put(discountSchema.getID(), newDiscountSchema.getID());
		}
		
		return descuentos;
	}
	
	/**
     * Crea un impuesto con los datos pasados como par??metro
     * @param name nombre del impuesto
     * @param rate tasa del impuesto
     * @param C_TaxCategory_ID id de la categor??a del impuesto a la cual va a pertenecer
     */
	private void createTaxs(int category_origen,int category_destiny) throws Exception{
		// Seteo el contexto con la compa????a template para el getOfClient
		this.setCtx(this.getClientTemplate());
		
		// Obtengo los impuestos de la categor??a de impuesto origen
		List<MTax> list = MTax.getOfTaxCategory(ctx, category_origen, this.getTrxName());
		
		// Asociaci??n entre las taxs templates y las nuevas creadas (sirve para asociar los parent taxs) 
		HashMap<Integer,Integer> taxs = new HashMap<Integer,Integer>();
		
		// Seteo el contexto con la compa????a nueva 
		this.setCtx(this.getNewClient());
		
		MTax newTax;
		
		for (MTax tax : list) {
			// Creo el nuevo impuesto a partir del impuesto template 
			newTax = new MTax(this.getCtx(),0,this.getTrxName());
			
			// Seteo los valores
			newTax.setIsActive(tax.isActive());
			newTax.setName(tax.getName());
			newTax.setDescription(tax.getDescription());
			newTax.setTaxIndicator(tax.getTaxIndicator());
			newTax.setIsDocumentLevel(tax.isDocumentLevel());
			newTax.setValidFrom(tax.getValidFrom());
			newTax.setIsSummary(tax.isSummary());
			newTax.setRequiresTaxCertificate(tax.isRequiresTaxCertificate());
			newTax.setRate(tax.getRate());
			newTax.setParent_Tax_ID((taxs.get(tax.getID()) == null)?0:(taxs.get(tax.getID())).intValue());
			newTax.setC_TaxCategory_ID(category_destiny);
			newTax.setIsDefault(tax.isDefault());
			newTax.setIsTaxExempt(tax.isTaxExempt());
			newTax.setSOPOType(tax.getSOPOType());
			newTax.setTaxType(tax.getTaxType());
			newTax.setTaxAccusation(tax.getTaxAccusation());
			
			//guardo el nuevo impuesto
			newTax.save();
				
			//Agrego los ids de asociaci??n en la hash
			taxs.put(tax.getID(), newTax.getID());
		}
		
	}
	
    /**
     * Obtiene las traducciones de la tabla pasada como par??metro
     * @param tableName nombre de la tabla padre
     * @param id id del registro origen de las trl 
     * @throws Exception
     */
    private List<List<Object>> getTrlOfTable(String tableName,int id) throws Exception{
    	// script sql
    	String sql = "SELECT * FROM "+tableName+"_trl WHERE "+tableName+"_id = "+id;
    	
    	//Ejecuto y retorno los resultados de la consulta sql
    	return ExecuterSql.executeQueryToList(sql, this.getTrxName());
    }
	
	/**
	 * Copia las tax categories de la compa??ia template
	 * @param categories hashmap a inicializar con las categorias de impuesto y las nuevas creadas 
	 * @return el ??ltimo tax category creado
	 */
	private void createTaxCategories(Map<Integer,Integer> categories) throws Exception{
		//Seteo el contexto con la compa????a template para el getOfClient
		this.setCtx(this.getClientTemplate());
		
		// Obtengo las categor??as de impuesto de la compa????a template
		List<MTaxCategory> list = MTaxCategory.getOfClient(this.getCtx(), this.getTrxName()); 
		
		//Seteo el contexto con la compa????a nueva 
		this.setCtx(this.getNewClient());
		
		MTaxCategory newTaxCategory;
				
		// Itero sobre las categor??as
		for (MTaxCategory taxCategory : list) {
			// Creo una por una para la nueva compa????a
			newTaxCategory = new MTaxCategory(this.getCtx(),0,this.getTrxName());
			
			// Seteo los valores
			newTaxCategory.setName(taxCategory.getName());
			newTaxCategory.setDescription(taxCategory.getDescription());
			newTaxCategory.setCommodityCode(taxCategory.getCommodityCode());
			newTaxCategory.setIsDefault(taxCategory.isDefault());
			newTaxCategory.setIsActive(taxCategory.isActive());
			newTaxCategory.setIsManual(taxCategory.isManual());
			
			// Guardo la categor??a de impuesto
			newTaxCategory.save();
			
			categories.put(taxCategory.getID(), newTaxCategory.getID());
						
			// Copio los impuestos de esa categor??a 
			this.createTaxs(taxCategory.getID(),newTaxCategory.getID());							
		}
	}
	
	/**
	 * Copia las tarifas, sus versiones y esquemas de vencimiento, con sus respectivos breaks  
	 * @throws Exception
	 */
	private void createDiscountSchemaAndPriceLists() throws Exception{
		// Asociaci??n entre las tarifas templates y las nuevas creadas  
		Map<Integer,Integer> tarifas = this.createTarifasTemplateAndNew();
		
		// Asociaci??n entre los descuentos templates y los nuevos creados  
		Map<Integer,Integer> descuentos = this.createDiscountSchemasTemplateAndNew();
		
		// Seteo la compa????a template
		this.setCtx(this.getClientTemplate());
		
		// Obtengo las versiones de la tarifa		
		List<MPriceListVersion> versiones = MPriceListVersion.getOfClient(this.getCtx(), this.getTrxName());
		
		// Seteo la compa????a nueva
		this.setCtx(this.getNewClient());
		
		MPriceListVersion newVersion;
		
		for(MPriceListVersion version : versiones){
			// Creo la nueva versi??n
			newVersion = new MPriceListVersion(this.getCtx(),0,this.getTrxName());
			
			// Seteo los valores
			MPriceListVersion.copyValues(version, newVersion);
			newVersion.setM_PriceList_ID(tarifas.get(version.getM_PriceList_ID()));
			newVersion.setM_DiscountSchema_ID(descuentos.get(version.getM_DiscountSchema_ID()));
			newVersion.setM_Pricelist_Version_Base_ID(tarifas.get(version.getM_Pricelist_Version_Base_ID()) == null?0:tarifas.get(version.getM_Pricelist_Version_Base_ID()));
			
			// Guardo
			newVersion.save();
		}
			  
	}
	
	/**
	 * Copia los pay schedule del esquema de vencimiento template y le asigna el esquema nuevo 
	 * @param paymentTerm_template
	 * @param id_paymentTerm_new
	 * @throws Exception
	 */
	private void createPaySchedules(MPaymentTerm paymentTerm_template,int id_paymentTerm_new) throws Exception{
		// Obtener los pay schedule de el esquema de vencimientos
		MPaySchedule[] paySchedules = paymentTerm_template.getSchedule(true);
		
		// Seteo la compa????a nueva
		this.setCtx(this.getNewClient());
		
		MPaySchedule newPaySchedule;
		
		for (MPaySchedule paySchedule : paySchedules) {
			// Creo el nuevo pay schedule
			newPaySchedule = new MPaySchedule(this.getCtx(),0,this.getTrxName());
			
			// Seteo los valores
			MPaySchedule.copyValues(paySchedule, newPaySchedule);
			newPaySchedule.setC_PaymentTerm_ID(id_paymentTerm_new);
			
			// Guardo
			newPaySchedule.save();
		}
	}
	
	
	/**
	 * Copia los esquemas de vencimiento de la compa????a template
	 * @throws Exception
	 */
	private void createPaymentTerm() throws Exception{
		// Seteo la compa????a template
		this.setCtx(this.getClientTemplate());
		
		// Obtengo los payment terms de la compa????a template
		List<MPaymentTerm> esquemas = MPaymentTerm.getOfClient(this.getCtx(), this.getTrxName());
		
		// Seteo la compa????a nueva
		this.setCtx(this.getNewClient());
		
		MPaymentTerm newPaymentTerm;
		
		for (MPaymentTerm paymentTerm : esquemas) {
			// Creo el payment Term
			newPaymentTerm = new MPaymentTerm(this.getCtx(),0,this.getTrxName());
			
			// Seteo los valores
			MPaymentTerm.copyValues(paymentTerm, newPaymentTerm);
			
			// Guardo
			newPaymentTerm.save();
			
			// Copio los pay schedule de cada esquema de vencimiento
			this.createPaySchedules(paymentTerm,newPaymentTerm.getID());
		}
	}
	
	
	/**
	 * Copia el procesador de contabilidad de la compa????a template
	 * @throws Exception
	 */
	// 
	private void createAccountingProcessor() throws Exception
	{
		// Seteo la compa????a template
		this.setCtx(this.getClientTemplate());
		
		// Recuperar el procesador de la compa????a template
		MAcctProcessor templateAcctProcessor = (MAcctProcessor)PO.findFirst(getCtx(), "C_AcctProcessor", "AD_Client_ID = ?" , new Object[]{getClientTemplate()}, null, getTrxName());
		
		// Seteo la compa????a nueva
		this.setCtx(this.getNewClient());
		
		// crear la copia, copiar valores 
		MAcctProcessor newAcctProcessor = new MAcctProcessor(getCtx(), 0, getTrxName());
		PO.copyValues(templateAcctProcessor, newAcctProcessor);
		
		// modificar los que corresponden y guardar
		newAcctProcessor.setClientOrg( getNewClient() , 0);
		newAcctProcessor.setName("Servidor de Procesos Contable");
		newAcctProcessor.setDescription("Servidor de procesamiento contable");
		newAcctProcessor.setSupervisor_ID( (users.get(templateAcctProcessor.getSupervisor_ID())).getAD_User_ID() );
		newAcctProcessor.save();
	}
	
	
	/**
	 * Creo las categor??as de uom a partir de la compa????a template y lo devuelvo en un map para 
	 * asociar categor??as nuevas con viejas
	 * @return
	 */
	private Map<Integer,Integer> createUOMCategories(){
		// Seteo la compa????a template
		this.setCtx(this.getClientTemplate());
		
		// Obtengo las uoms categorias
		List<MUOMGroup> categorias = MUOMGroup.getOfClient(this.getCtx(), this.getTrxName());
		
		Map<Integer,Integer> mapeo_categories = new HashMap<Integer,Integer>(); 
		
		// Seteo la compa????a nueva
		this.setCtx(this.getNewClient());
		
		MUOMGroup newGroup;
		
		for (MUOMGroup group : categorias) {
			// Creo el nuevo
			newGroup = new MUOMGroup(this.getCtx(),0,this.getTrxName());
			
			//Seteo los valores
			MUOMGroup.copyValues(group, newGroup);
			
			// Guardo
			newGroup.save();
			
			// Guardo en hash lo valores
			mapeo_categories.put(group.getID(), newGroup.getID());
		}
		
		return mapeo_categories;
	}
	
	/**
	 * Creo los uoms de la compa????a template y retorno la ??ltima creada
	 * @return
	 */
	private int createUOMs(){
		// Seteo la compa????a template
		this.setCtx(this.getClientTemplate());
		
		// Asociar las categor??as de UOM template con las nuevas a crear
		Map<Integer,Integer> categories = this.createUOMCategories(); 
		
		//Obtener las UOM de la compa????a template
		List<MUOM> uoms = MUOM.getOfClient(this.getCtx(), this.getTrxName());
			
		// Seteo la compa????a nueva
		this.setCtx(this.getNewClient());
		
		MUOM newUom;
		Map<Integer,Integer> allUoms = new HashMap<Integer,Integer>();
		int uom_retorno = 0;
		
		// Creo las uoms para la compa????a nueva a partir de las de la compa????a template y las guardo
		// en hash para luego crear sus respectivas conversiones
		for (MUOM muom : uoms) {
			// Creo el nuevo uom
			newUom = new MUOM(this.getCtx(),0,this.getTrxName());
			
			// Seteo los valores
			MUOM.copyValues(muom, newUom);
			newUom.setC_UOM_Group_ID(categories.get(muom.getC_UOM_Group_ID()));
			
			// Guardo
			newUom.save();
			
			allUoms.put(muom.getID(), newUom.getID());
			
			uom_retorno = newUom.getID();
		}
		
		// Crear las conversiones de las uoms creadas
		
		// Seteo la compa????a template
		this.setCtx(this.getClientTemplate());
		
		// Obtengo las conversiones de la compa????a template
		List<MUOMConversion> conversiones = MUOMConversion.getOfClient(this.getCtx(), this.getTrxName());
		
		// Seteo la compa????a nueva
		this.setCtx(this.getNewClient());
		
		MUOMConversion newConversion;
		
		// Creo las conversiones a partir de la compa????a template y seteo las uom de cada conversi??n 
		// mediante la hash de uoms creada mas arriba
		for (MUOMConversion conversion : conversiones) {
			// Creo el nuevo
			newConversion = new MUOMConversion(this.getCtx(),0,this.getTrxName());
			
			//Seteo los valores
			MUOMConversion.copyValues(conversion, newConversion);
			newConversion.setC_UOM_ID(allUoms.get(conversion.getC_UOM_ID()));
			newConversion.setC_UOM_To_ID(allUoms.get(conversion.getC_UOM_To_ID()));
			newConversion.setM_Product_ID(0);
			
			// Guardo
			newConversion.save();
		}
		
		return uom_retorno;
	}
	
	/**
	 * Copio las locations de la compa????a template
	 * @return
	 * @throws Exception
	 */
	private Map<Integer,Integer> createLocations() throws Exception{
		// Seteo la compa????a template
		this.setCtx(this.getClientTemplate());
		
		// Obtengo las locations de la compa????a template
		List<MLocation> locations = MLocation.getOfClient(this.getCtx(), this.getTrxName());
		
		Map<Integer,Integer> allLocations = new HashMap<Integer, Integer>();
		
		MLocation newLocation;
		
		for (MLocation location : locations) {
			// Seteo la compa????a nueva y la organizaci??n asociada
			this.setCtx(this.getNewClient(),this.getOrgId(location.getAD_Org_ID()));
			
			// Creo la nueva
			newLocation = new MLocation(this.getCtx(),0,this.getTrxName());
			
			// Seteo los valores
			MLocation.copyValues(location, newLocation);
			
			newLocation.save();
			
			allLocations.put(location.getID(), newLocation.getID());
		}
		
		return allLocations;
	}
	
	/**
	 * Crea las locators del warehouse pasado como par??metro
	 * @param warehouse_template
	 * @param new_warehouse_id
	 * @throws Exception
	 */
	private void createLocators(MWarehouse warehouse_template,int new_warehouse_id) throws Exception{
		// Obtengo las locators del almac??n template
		MLocator[] locators =  warehouse_template.getLocators(true);
		
		MLocator newLocator;
		
		for (int i = 0; i < locators.length; i++) {
			// Seteo la compa????a nueva y la organizaci??n asociada
			this.setCtx(this.getNewClient(),this.getOrgId(locators[i].getAD_Org_ID()));
			
			// Creo el nuevo
			newLocator = new MLocator(this.getCtx(),0,this.getTrxName());
			
			MLocator.copyValues(locators[i], newLocator);
			newLocator.setM_Warehouse_ID(new_warehouse_id);
			
			newLocator.save();
		}
	}
	
	
	/**
	 * Creo los almacenes y ubicaciones a partir de la compa????a y la organizaci??n template
	 * @param ad_org_id
	 * @throws Exception
	 */
	private void createWarehousesAndLocators(Map<Integer,Integer> locations) throws Exception{
		// Obtengo las asociaciones entre las locations template y las creadas nuevas
		locations = this.createLocations();
		
		// Seteo la compa????a template
		this.setCtx(this.getClientTemplate());
		
		// Obtengo las locations de la compa????a template
		List<MWarehouse> almacenes = MWarehouse.getOfClient(this.getCtx(), this.getTrxName());
		
		MWarehouse newAlmacen;
		
		for (MWarehouse almacen : almacenes) {
			// Seteo la compa????a nueva y la organizaci??n asociada
			this.setCtx(this.getNewClient(),this.getOrgId(almacen.getAD_Org_ID()));
			
			// Creo la nueva
			newAlmacen = new MWarehouse(this.getCtx(),0,this.getTrxName());
			
			// Seteo los valores
			MLocation.copyValues(almacen, newAlmacen);
			newAlmacen.setC_Location_ID(locations.get(almacen.getC_Location_ID()));
			
			newAlmacen.save();
			
			// Copiar las locators del almacen
			this.createLocators(almacen,newAlmacen.getID());
		}
		
	}
	
	
	/**
	 * Obtiene el grupo de las entidades comerciales si es que existe, sino la crea
	 * @return id del grupo
	 */
	private int createBPGroup() throws Exception{
		//Saco el grupo creado en el contexto
		int idBGP = Env.getContextAsInt(this.getCtx(), "#C_BP_Group_ID");
		
		//Si no existe el grupo, lo creo
		if(idBGP == 0){
			
			this.setCtx(this.getNewClient(),0);			
			
			//Creo el grupo de los usuario a crear
			MBPGroup bpg = new MBPGroup(this.getCtx(),0,this.getTrxName());
			
			idBGP = bpg.getID();
			
			//Seteo los valores
			bpg.setValue("Empleados");
			bpg.setName("Empleados");
			
			//Si se pudo guardar, registro el log
			if(bpg.save()){
				idBGP = bpg.getID();
				this.getLog().info("BPartnerGroup creado con id "+idBGP);
			}
			else{
				throw new Exception("No se pudo crear el grupo de la entidad comercial");
			}
		}
				
		return idBGP;
	}
	
	/**
	 * Crea las organizaciones para la compa????a a partir de la compa????a template
	 */
	private void createOrgs() throws Exception{
		
		this.setCtx(this.getClientTemplate());
				
		//Obtengo las orgs de la compa????a template
		MOrg[] orgs = MOrg.getOfClient(this.getCtx(), this.getTrxName());
		
		this.setCtx(this.getNewClient());
						
		int total = orgs.length;
		MOrg org = null;
		MOrg orgAux = null;
		
		for(int i = 0; i < total ; i++){

			orgAux = orgs[i];
			
			//Creo la nueva org
			org = new MOrg(ctx,0,this.getTrxName());
			
			//Seteo los valores
			org.setValue(orgAux.getName());
			org.setName(orgAux.getName());
			
			//Guardo la org
			if(org.save()){
				this.getLog().info("Organizacion copiada de la template. Nombre de la nueva org: "+org.getName());
				//Guardo en la colecci??n de organizaciones
				this.getOrgs().put(orgAux.getID(), org);
			}
			else{
				throw new Exception("No se pudieron crear las organizaciones");
			}
		}
		
	}
	
	/**
	 * Elimina los formatos de impresi??n del bot??n imprimir para poder agregarlos nuevos, si es que hay algunos
	 * @throws Exception
	 */
	private void delPrintForms() throws Exception{
		// sql
		String sql = "DELETE FROM ad_printform WHERE ad_client_id = ?";
		
		ExecuterSql.executeUpdate(sql, this.getTrxName(),new Object[]{this.getClientTemplate()});
	}
	
	
	/**
	 * Copia los formatos de impresi??n del bot??n Imprimir de System
	 */
	private void createPrintForm() throws Exception{
		// Seteo el contexto con la compa????a
		this.setCtx(0);
		
		// Obtengo el print form de la compa????a
		List<MPrintForm> list = MPrintForm.getOfClient(this.getCtx(), this.getTrxName());
		
		// Elimino todos los de la nueva template que se hayan creado anteriormente
		this.delPrintForms();
		
		// Seteo el ctx con la nueva
		this.setCtx(this.getNewClient());
		
		MPrintForm newPrintForm;
	
		for (MPrintForm printForm : list) {
			// Seteo el contexto con el cliente template y la org espec??fica
			this.setCtx(this.getNewClient(), this.getOrgId(printForm.getAD_Org_ID()));
			// Creo el nuevo print form
			newPrintForm = new MPrintForm(this.getCtx(),0,this.getTrxName());
			
			// Seteo los valores
			MPrintForm.copyValues(printForm, newPrintForm);
			newPrintForm.setIsActive(printForm.isActive());
			
			// Guardo
			if(!newPrintForm.save()){
				throw new Exception("No se pudieron crear los formatos de impresion del boton Imprimir");
			}
		}				
	}
	
	/**
	 * Creo la location para BPartner 
	 * @param C_Location_ID id de la location template
	 * @return el id de la nueva location
	 */
	private int createBPLocation(int C_Location_ID) throws Exception{		
		//Obtengo la location pasada como par??metro
		MLocation location = new MLocation(this.getCtx(),C_Location_ID,this.getTrxName());
		
		//Seteo el contexto con la nueva compa????a
		this.setCtx(this.getNewClient());
		
		//Creo la location copia de la anterior
		MLocation newLocation = new MLocation(this.getCtx(),0,this.getTrxName());
		
		newLocation.setAddress1(location.getAddress1());
		newLocation.setCity(location.getCity());
		newLocation.setC_Country_ID(location.getC_Country_ID());
		newLocation.setC_Region_ID(location.getC_Region_ID());
		
		//Guardar la nueva location de BPartner
		if(!newLocation.save()){
			throw new Exception("No se pudo crear la localizaci??n para el usuario");
		}
		
		//Obtengo el id de la nueva location creada
		int id = newLocation.getID();
		
		return id;
	}	
	
	/**
	 * Crea el BPartner para el usuario a agregar
	 * @param C_BPartner_Group_ID grupo del bpartner
	 * @param name value y nombre del bpartner
	 * @return el id del bpartner creado
	 */
	private int createBPartner(int C_BPartner_Group_ID, int C_BPartner_ID, String name) throws Exception{
		//Obtengo el BPartner pasado como par??metro
		MBPartner bpTemplate = new MBPartner(this.getCtx(),C_BPartner_ID,this.getTrxName());
		
		//Obtengo la compa????a para colocarle al nombre de la bpartner
		MClient client = new MClient(this.getCtx(), this.getNewClient(),this.getTrxName());
		
		//Obtengo la location del bpartner template 
		int location = this.createBPLocation(bpTemplate.getC_Location_ID());
		
		//Creo el BPartner para el usuario 
		MBPartner bp = new MBPartner(this.getCtx(),0,this.getTrxName());		
		
		bp.setValue(client.getName()+" "+name);
		bp.setName(client.getName()+" "+name);
		bp.setC_BP_Group_ID(C_BPartner_Group_ID);
		bp.setIsEmployee(true);
		bp.setIsSalesRep(true);
		bp.setC_Location_ID(location);
		
		int id = 0;
		
		//Si se pudo guardar, registro el log
		if(bp.save()){
			this.getLog().info("BPartner creado con nombre "+bp.getName());
			id = bp.getID(); 
		}
		else{
			throw new Exception("No se pudo crear la entidad comercial para el usuario");
		}
		
		return id;
	}
	
	/**
	 * Crea un usuario a partir del usuario template
	 * @param userTemplate usuario template
	 * @param bpGroup id del grupo de BPartner
	 * @return id del usuario creado
	 */
	private int createUser(MUser userTemplate, int bpGroup) throws Exception{
		//Creo los BPartners para cada usuario
		int idBp = this.createBPartner(bpGroup, userTemplate.getC_BPartner_ID(),userTemplate.getName());
		
		//Seteo el contexto con la organizaci??n del usuario template
		this.setCtx(this.getNewClient(), this.getOrgId(userTemplate.getAD_Org_ID()));
		
		//Obtengo el cliente nuevo para poner un nombre a la org 
		MClient client = new MClient(this.getCtx(), this.getNewClient(),this.getTrxName()); 

		//Creo el usuario nuevo
		MUser user = new MUser(this.getCtx(),0,this.getTrxName());
		
		//Formateo el nombre de la compa????a y el del nuevo usuario
		String companyName = client.getName().replaceAll(" ", "");
		String userName = userTemplate.getName().replaceAll("WideFast-Track", companyName);
		
		//Seteo los valores
		user.setName(userName);
		user.setDescription(userTemplate.getDescription());
		user.setPassword(userName);
		user.setC_BPartner_ID(idBp);
		
		int idUser = 0;
		
		//Si se pudo guardar, registro el log
		if(user.save()){
			this.getLog().info("Usuario creado con nombre: "+user.getName());
			idUser = user.getID();
		}
		else{
			throw new Exception("No se pudo crear el usuario a partir del template");
		}
		
		// incorporar el usuario al hashmap de usuarios
		users.put(userTemplate.getAD_User_ID(), user);
		
		return idUser;
	}
	
	/**
	 * Crea el nuevo tree a partir del tree template
	 * @param AD_Tree_ID tree template
	 * @return id del nuevo tree
	 */
	private int createTree(int AD_Tree_ID) throws Exception{
		//Seteo el contexto con la compa????a template
		this.setCtx(this.getClientTemplate());
		
		//Obtengo el tree pasado como par??metro
		MTree treeAux = new MTree(this.getCtx(),AD_Tree_ID,this.getTrxName());
		
		//Seteo el contexto con la organizaci??n del usuario template
		this.setCtx(this.getNewClient(), 0);
		
		//Creo el nuevo tree
		MTree newTree = new MTree(this.getCtx(),0,this.getTrxName());
		
		//Seteo los valores
		newTree.setName(treeAux.getName());
		newTree.setTreeType(treeAux.getTreeType());
		newTree.setIsAllNodes(false);
		newTree.setProcessing(false);
		
		//Guardo el nuevo tree
		if(!newTree.save()){
			throw new Exception("No se pudo crear el arbol de menu");
		}
		
		
		//Retorno el id del tree nuevo
		return newTree.getID();
	}
	
	/**
	 * Crea los accesos a ventanas
	 */
	private void createWindowsAccess_old(){
		//Seteo el contexto con la organizaci??n del usuario template
		this.setCtx(this.getNewClient(), 0);
		
		//Para todos los roles template
		Set conjRoles = this.getRoles().keySet();
		
		Iterator iteraKeys = conjRoles.iterator();
		Integer key = null;
		String sql = null;
		PreparedStatement psmt = null;
		MWindowAccess mwa = null;
		ResultSet rs = null;
		
		try{
		
			while(iteraKeys.hasNext()){
				key = (Integer)iteraKeys.next();
							
				//Script sql para los windows access
				sql = "SELECT * FROM ad_window_access WHERE ad_role_id = ? AND ad_client_id = ?";
							
				psmt = DB.prepareStatement(sql, this.getTrxName());
				psmt.setInt(1, key.intValue());
				psmt.setInt(2, this.getClientTemplate());
							
				rs = psmt.executeQuery();
				 
				while(rs.next()){
					 //Creo el nuevo
					 mwa = new MWindowAccess(this.getCtx(),0,this.getTrxName());
					 
					 //Seteo los valores
					 mwa.setAD_Role_ID(((MRole)this.getRoles().get(key)).getID());
					 mwa.setAD_Window_ID(rs.getInt("ad_window_id"));
					 
					 //Guardo 
					 if(mwa.save()){
						 //Registrar el log
					 }
				}

			}
		} catch(Exception e){
			this.getLog().log(Level.SEVERE, sql, e);
		}
		
		
	}
	
	/**
	 * Crea los accesos a procesos
	 */
	private void createProcessAccess(){
		//Seteo el contexto con la organizaci??n del usuario template
		this.setCtx(this.getNewClient(), 0);
		
		//Para todos los roles template
		Set conjRoles = this.getRoles().keySet();
		
		Iterator iteraKeys = conjRoles.iterator();
		Integer key = null;
		String sql = null;
		PreparedStatement psmt = null;
		MProcessAccess mpa = null;
		ResultSet rs = null;
		
		try{
		
			while(iteraKeys.hasNext()){
				key = (Integer)iteraKeys.next();
							
				//Script sql para los windows access
				sql = "SELECT * FROM ad_process_access WHERE ad_role_id = ? AND ad_client_id = ?";
				
				psmt = DB.prepareStatement(sql, this.getTrxName());
				psmt.setInt(1, key.intValue());
				psmt.setInt(2, this.getClientTemplate());
											
				rs = psmt.executeQuery();
				 
				while(rs.next()){
					 //Creo el nuevo
					 mpa = new MProcessAccess(this.getCtx(),0,this.getTrxName());
					 
					 //Seteo los valores
					 mpa.setAD_Role_ID(((MRole)this.getRoles().get(key)).getID());
					 mpa.setAD_Process_ID(rs.getInt("ad_process_id"));
					 
					 //Guardo 
					 if(mpa.save()){
						 //Registrar el log
					 }
				}

			}
		} catch(Exception e){
			this.getLog().log(Level.SEVERE, sql, e);
		} finally{
			try{
				rs.close ();
				psmt.close ();
				psmt = null;
			}catch (Exception e){
				psmt = null;
			}
		}
		
	}
	
	/**
	 * Crea los accesos de forms
	 */
	private void createFormAccess(){
		//Seteo el contexto con la organizaci??n del usuario template
		this.setCtx(this.getNewClient(), 0);
		
		//Para todos los roles template
		Set conjRoles = this.getRoles().keySet();
		
		Iterator iteraKeys = conjRoles.iterator();
		Integer key = null;
		String sql = null;
		PreparedStatement psmt = null;
		MFormAccess mfa = null;
		ResultSet rs = null;
		
		try{
		
			while(iteraKeys.hasNext()){
				key = (Integer)iteraKeys.next();
							
				//Script sql para los windows access
				sql = "SELECT * FROM ad_form_access WHERE ad_role_id = ? AND ad_client_id = ?";
				
				psmt = DB.prepareStatement(sql, this.getTrxName());
				psmt.setInt(1, key.intValue());
				psmt.setInt(2, this.getClientTemplate());
				rs = psmt.executeQuery();
				 
				while(rs.next()){
					 //Creo el nuevo
					 mfa = new MFormAccess(this.getCtx(),0,this.getTrxName());
					 
					 //Seteo los valores
					 mfa.setAD_Role_ID(((MRole)this.getRoles().get(key)).getID());
					 mfa.setAD_Form_ID(rs.getInt("ad_form_id"));
					 
					 //Guardo 
					 if(mfa.save()){
						 //Registrar el log
					 }
				}
			}
		} catch(Exception e){
			this.getLog().log(Level.SEVERE, sql, e);
		} finally{
			try{
				rs.close ();
				psmt.close ();
				psmt = null;
			}catch (Exception e){
				psmt = null;
			}
		}
	}
	
	/**
	 * Crea los accesos de los workflows
	 */
	private void createWorkflowAccess(){
		//Seteo el contexto con la organizaci??n del usuario template
		this.setCtx(this.getNewClient(), 0);
		
		//Para todos los roles template
		Set conjRoles = this.getRoles().keySet();
		
		Iterator iteraKeys = conjRoles.iterator();
		Integer key = null;
		String sql = null;
		PreparedStatement psmt = null;
		MWorkflowAccess mwa = null;
		ResultSet rs = null;
		
		try{
		
			while(iteraKeys.hasNext()){
				key = (Integer)iteraKeys.next();
							
				//Script sql para los windows access
				sql = "SELECT * FROM ad_workflow_access WHERE ad_role_id = ? AND ad_client_id = ?";
				
				psmt = DB.prepareStatement(sql, this.getTrxName());
				psmt.setInt(1, key.intValue());
				psmt.setInt(2, this.getClientTemplate());
				rs = psmt.executeQuery();
				 
				while(rs.next()){
					 //Creo el nuevo
					 mwa = new MWorkflowAccess(this.getCtx(),0,this.getTrxName());
					 
					 //Seteo los valores
					 mwa.setAD_Role_ID(((MRole)this.getRoles().get(key)).getID());
					 mwa.setAD_Workflow_ID(rs.getInt("ad_workflow_id"));
					 
					 //Guardo 
					 if(mwa.save()){
						 //Registrar el log
					 }
				}
			}
		} catch(Exception e){
			this.getLog().log(Level.SEVERE, sql, e);
		} finally{
			try{
				rs.close ();
				psmt.close ();
				psmt = null;
			}catch (Exception e){
				psmt = null;
			}
		}
	}
	
	/**
	 * Copia los accessos a las pesta??as de los roles
	 * @throws Exception
	 */
	private void createTabAccess() throws Exception{
		//Seteo el contexto con la organizaci??n del usuario template
		this.setCtx(this.getNewClient(), 0);
		
		//Para todos los roles template
		Set conjRoles = this.getRoles().keySet();
		
		Iterator iteraKeys = conjRoles.iterator();
		Integer key = null;
		MRole valueRol;
		PreparedStatement psmt = null;
		ResultSet rs = null;
		String sql;
		int nro;
		
		try{
			//Itero por todos los roles
			while(iteraKeys.hasNext()){
				key = (Integer)iteraKeys.next();
				valueRol = (MRole)this.getRoles().get(key);
				
				//Obtengo las tab access del rol actual
				List<MTabAccess> tabRoles = MTabAccess.getOfRoleInList(key.intValue(),this.getTrxName());
				
				
				//Itero por todas las tab access del rol y creo las nuevas
				for (MTabAccess auxMta : tabRoles) {
					
					/*
					 * Agrega el tab access para ese rol 
					 * -------------------------------------------------------
					 * INSERT INTO ad_tab_access (ad_tab_access_id,ad_tab_id,ad_window_id,ad_role_id,ad_client_id,ad_org_id,isactive,createdby,updatedby,isreadwrite) 
					 * VALUES (?,?,?,?,?,?,?,?,?,?)
					 * -------------------------------------------------------
					 */
					
					sql = "INSERT INTO ad_tab_access (ad_tab_access_id,ad_tab_id,ad_window_id,ad_role_id,ad_client_id,ad_org_id,isactive,createdby,updatedby,isreadwrite) VALUES (?,?,?,?,?,?,?,?,?,?)";
					
					nro = MSequence.getNextID(this.getNewClient(), "AD_Tab_Access", this.getTrxName());
					
					psmt = DB.prepareStatement(sql, this.getTrxName());
					
					//Seteo los valores de la consulta
					psmt.setInt(1, nro);
					psmt.setInt(2, auxMta.getAD_Tab_ID());
					psmt.setInt(3, auxMta.getAD_Window_ID());
					psmt.setInt(4, valueRol.getID());
					psmt.setInt(5, this.getNewClient());
					psmt.setInt(6, 0);
					psmt.setString(7, (auxMta.isActive())?"Y":"N");
					psmt.setInt(8, 100);
					psmt.setInt(9, 100);
					psmt.setString(10, (auxMta.isReadWrite())?"Y":"N");
					
					psmt.executeUpdate();
				}			
			}			
		}catch(Exception e){
			throw new Exception(e.getMessage());
		}finally{
			try{
				rs.close ();
				psmt.close ();
				psmt = null;
			}catch (Exception e){
				psmt = null;
			}
		}
	}
	
	
	/**
	 * Copia los accesos a las pesta??as de cada uno de los perfiles (OLD)
	 * @throws Exception
	 */
	private void createTabAccess_old() throws Exception{
		//Seteo el contexto con la organizaci??n del usuario template
		this.setCtx(this.getNewClient(), 0);
		
		//Para todos los roles template
		Set conjRoles = this.getRoles().keySet();
		
		Iterator iteraKeys = conjRoles.iterator();
		Integer key = null;
		MRole valueRol;
		PreparedStatement psmt = null;
		MTabAccess mta = null;
		ResultSet rs = null;
		
		try{
			//Itero por todos los roles
			while(iteraKeys.hasNext()){
				key = (Integer)iteraKeys.next();
				valueRol = (MRole)this.getRoles().get(key);
				
				//Obtengo las tab access del rol actual
				List<MTabAccess> tabRoles = MTabAccess.getOfRoleInList(key.intValue(),this.getTrxName());
				
				
				//Itero por todas las tab access del rol y creo las nuevas
				for (MTabAccess auxMta : tabRoles) {
					//Obtengo el tab access
					mta = new MTabAccess(this.getCtx(),0,this.getTrxName());
										
					//Seteo los nuevos valores
					mta.setAD_Role_ID(valueRol.getID());
					mta.setAD_Tab_ID(auxMta.getAD_Tab_ID());
					mta.setAD_Window_ID(auxMta.getAD_Window_ID());
					mta.setIsReadWrite(auxMta.isReadWrite());
					
					if(!mta.save()){
						throw new Exception("No se pudieron crear los accesos a las pesta??as");
					}					
				}			
			}			
		}catch(Exception e){
			throw new Exception(e.getMessage());
		}finally{
			try{
				rs.close ();
				psmt.close ();
				psmt = null;
			}catch (Exception e){
				psmt = null;
			}
		}
	}

	/**
	 * Crea los accesos a las ventanas en base a las ventanas del men??
	 */
	private void createWindowsAccess() throws Exception{
		//Seteo el contexto con la organizaci??n del usuario template
		this.setCtx(this.getNewClient(), 0);
		
		//Para todos los roles template
		Set conjRoles = this.getRoles().keySet();
		
		Iterator iteraKeys = conjRoles.iterator();
		Integer key = null;
		MRole valueRol; 
		String sql = null;
		PreparedStatement psmt = null;
		MWindowAccess mwa = null;
		MWindowAccess aux = null;
		ResultSet rs = null;
				
		try{
		
			//Itero por todos los perfiles
			while(iteraKeys.hasNext()){
				key = (Integer)iteraKeys.next();
				valueRol = (MRole)this.getRoles().get(key);
				
				//Si tiene un ??rbol de men?? asociado realizo todas las actualizaciones a la base
				if(valueRol.getAD_Tree_Menu_ID() != 0){
									
					/*
					 * Selecciono los nodos ventana que tiene el men?? del perfil actual
					 * ---------------------------------------------------------------------------------------
					 * SELECT ad_window_id 
					 * FROM ad_treenodemm as tn	
					 * INNER JOIN ad_role as r	
					 * ON (ad_role_id = ?) AND (tn.ad_tree_id = r.ad_tree_menu_id)	
					 * INNER JOIN ad_menu as m	
					 * ON (m.ad_menu_id = node_id)
					 * ---------------------------------------------------------------------------------------
					 */
					
					sql = "SELECT ad_window_id FROM ad_treenodemm as tn	INNER JOIN ad_role as r	ON (ad_role_id = ?) AND (tn.ad_tree_id = r.ad_tree_menu_id)	INNER JOIN ad_menu as m	ON (m.ad_menu_id = node_id) WHERE ACTION = 'W'";
					
					psmt = DB.prepareStatement(sql, this.getTrxName());
					psmt.setInt(1, valueRol.getID());
					rs = psmt.executeQuery();
					
					// Mientras haya nodos en el men??
					while(rs.next()){
						
						// Obtengo el window access del rol nuevo para la ventana actual de men??
						mwa = MWindowAccess.getOfRoleAndWindow(this.getCtx(), valueRol.getID(), rs.getInt("ad_window_id"), this.getTrxName());
						
						// Obtengo el acceso del rol template para esa ventana
						aux = MWindowAccess.getOfRoleAndWindow(this.getCtx(), key.intValue(), rs.getInt("ad_window_id"), this.getTrxName());
						
						// Si no existe el acceso de ese perfil nuevo y la ventana, creo uno nuevo
						if(mwa == null){
							//creo la window access 
							mwa = new MWindowAccess(this.getCtx(),0,this.getTrxName());
							
							//Seteo los valores
							mwa.setAD_Window_ID(rs.getInt("ad_window_id"));
							mwa.setAD_Role_ID(valueRol.getID());
						}
						
						// Si no tiene un ad_window_access creado para esa ventana y ese perfil, pongo los valores por defecto
						mwa.setIsActive((aux != null)?aux.isActive():true);
						mwa.setIsReadWrite((aux != null)?aux.isReadWrite():true);
							
						//Guardo
						if(!mwa.save()){
							throw new Exception("No se pudieron crear los accesos para las ventanas");
						}
						
					}									
				}
					
				// Eliminar los windows access ajenos a las ventas del men??
				
				sql = "DELETE FROM ad_window_access WHERE (ad_window_id IN (SELECT ad_window_id FROM ad_window_access WHERE (ad_role_id = ?) EXCEPT SELECT ad_window_id FROM ad_treenodemm as tn INNER JOIN ad_role as r ON (ad_role_id = ?) AND (tn.ad_tree_id = r.ad_tree_menu_id) INNER JOIN ad_menu as m ON (m.ad_menu_id = node_id) WHERE ACTION = 'W')) and (ad_role_id = ?)";
				psmt = DB.prepareStatement(sql, this.getTrxName());
				psmt.setInt(1, valueRol.getID());
				psmt.setInt(2, valueRol.getID());
				psmt.setInt(3, valueRol.getID());
				psmt.executeUpdate();
			}			
		} catch(Exception e){
			this.getLog().log(Level.SEVERE, sql, e);
			throw new Exception(e.getMessage());
		} finally{
			try{
				rs.close ();
				psmt.close ();
				psmt = null;
			}catch (Exception e){
				psmt = null;
			}
	}
		
}
	
	/**
	 * Crea los accesos a componentes a partir de los roles template 
	 */
	private void createMenuAccess() throws Exception{
		//Accesos a Windows
		this.createWindowsAccess();
		
		//Accesos a Tabs
		this.createTabAccess();
		
		//Accesos a Process
		//this.createProcessAccess();
		
		//Accesos a Forms
		//this.createFormAccess();
		
		//Accesos a workflows
		//this.createWorkflowAccess();				
	}
	
	/**
	 * Creo el ??rbol de men?? nuevo con todos sus nodos para el nuevo perfil a partir del ??rbol template
	 * @param AD_Tree_ID ??rbol template
	 * @return id del ??rbol creado
	 */
	private int createTreeMenu(int AD_Tree_ID) throws Exception{
		int treeRetorno = 0;
		
		//Si existe un ??rbol de men?? dentro del rol, lo copio a la compa????a template 
		if(AD_Tree_ID != 0){
			//Seteo el contexto con la compa????a template
			this.setCtx(this.getClientTemplate());
			
			//Obtengo los nodos del menu
			MTree_NodeMM[] nodosMenu = MTree_NodeMM.getOfTree(this.getCtx(), AD_Tree_ID, this.getTrxName());
			
			int total = nodosMenu.length;
			
			//Creo el ??rbol nuevo a partir del template pasado como par??metro
			treeRetorno = this.createTree(AD_Tree_ID);
			
			//Guardar el map entre viejo y nuevo
			this.getTrees().put(AD_Tree_ID, treeRetorno);

			//Seteo el contexto con la organizaci??n del usuario template
			this.setCtx(this.getNewClient(), 0);
			
			//Nodos del ??rbol men??
			MTree_NodeMM treeNodeMMAux = null;
			MTree_NodeMM treeNodeMM = null;	
			
			for(int i = 0; i < total ; i++){
				
				treeNodeMMAux = nodosMenu[i];
				
				if(treeNodeMMAux.getNode_ID() != 0){
					//Creo el nuevo nodo del menu
					treeNodeMM = new MTree_NodeMM(this.getCtx(),0,this.getTrxName());
					
					//Seteo los valores
					treeNodeMM.setAD_Tree_ID(treeRetorno);
					treeNodeMM.setNode_ID(treeNodeMMAux.getNode_ID());
					treeNodeMM.setParent_ID(treeNodeMMAux.getParent_ID());
					treeNodeMM.setSeqNo(treeNodeMMAux.getSeqNo());
					
					
					
					//Guardo el nuevo 
					if(!treeNodeMM.save()){
						throw new Exception("No se pudieron crear los nodos para el arbol de menu");
					}
				}				
			}			
		}
		
		return treeRetorno;
	} 
	
	/**
	 * Crea los perfiles a partir de los perfiles template de la compa????a template
	 */
	
	private void createRoles() throws Exception{
		
		this.setCtx(this.getClientTemplate());
		
		//Obtengo los roles a partir de la compa????a template
		MRole[] roles = MRole.getOfClient(this.getCtx());
		
		this.setCtx(this.getNewClient(), 0);
		
		int total = roles.length;
		MRole roleAux = null;
		MRole newRole = null;
				
		for(int i = 0; i < total ; i++){
			//Obtengo el rol
			roleAux = roles[i];
			
			//Creo el rol nuevo
			newRole = new MRole(this.getCtx(),0,this.getTrxName());
			
			//Seteo los valores del rol template
			newRole.setName(roleAux.getName());
			newRole.setIsActive(roleAux.isActive());
			newRole.setUserLevel(roleAux.getUserLevel());
			newRole.setIsShowAcct(roleAux.isShowAcct());
			newRole.setPreferenceType(roleAux.getPreferenceType());
			newRole.setAD_Tree_Menu_ID(this.createTreeMenu(roleAux.getAD_Tree_Menu_ID()));			
					
			
			//Guardo el rol nuevo
			if(!newRole.save()){
				throw new Exception("No se pudo crear los perfiles");
			}
			
			//Lo agrego a la colecci??n
			this.getRoles().put(roleAux.getID(), newRole);
		}
	}
	
	/**
	 * Ddetermina los roles de usuario dependiendo que tipo de usuario sea
	 * @param idUserTemplate usuario template
	 * @return array de roles de ese usuario template
	 */
	
	private MUserRoles[] getUserRoles(int idUserTemplate){
		MUserRoles[] retorno = null;
		
		if(idUserTemplate != 100){
			retorno = MUserRoles.getOfUser(this.getCtx(), idUserTemplate);
		}
		else{
			retorno = MUserRoles.getOfUserAndClient(this.getCtx(), idUserTemplate,this.getClientTemplate(),this.getTrxName());
		}
		
		return retorno;
	}
	
	/**
	 * Crea los perfiles de los usuarios  
	 * @param idUserTemplate id de usuario template
	 * @param idUser id de usuario nuevo
	 */
	
	private void createUserProfiles(int idUserTemplate, int idUser) throws Exception{
		//Seteo el contexto con la compa????a vieja
		this.setCtx(this.getClientTemplate());
		
		//Obtengo los roles para ese usuario
		MUserRoles[] userRoles = this.getUserRoles(idUserTemplate);
		
		int total = userRoles.length;
		MUserRoles userRole = null;
		MUserRoles userRoleAux = null;
		
		//Itero por todos los roles del usuario template y creo los roles para el nuevo usuario  
		for(int i = 0; i < total ; i++){
			
			//Obtengo el rol template del usuario template
			userRoleAux = userRoles[i];
			
			//Seteo el contexto para crear los perfiles de usuario con la compa????a y organizaci??n nueva
			//que se relaciona con la organizaci??n template dentro de la colecci??n 
			this.setCtx(this.getNewClient(), this.getOrgId(userRoleAux.getAD_Org_ID()));
			
			//Creo el nuevo rol
			userRole = new MUserRoles(this.getCtx(),0,this.getTrxName());
			
			//Seteo los valores
			
			//Seteo el rol con el rol de la colecci??n que se relaciona con el rol template 
			userRole.setAD_Role_ID(((MRole)this.getRoles().get(userRoleAux.getAD_Role_ID())).getID());
			
			//Seteo el usuario con el usuario creado
			userRole.setAD_User_ID(idUser);
			userRole.setIsActive(userRoleAux.isActive());
			
			//Guardo el nuevo rol de usuario
			if(!userRole.save()){
				throw new Exception("No se pudo crear los roles del usuario");
			}
		}
		
	}
	
	/**
	 * Crea los accesos a las organizaciones de los perfiles a partir de la compa????a template
	 */
	
	private void createRoleOrgAccess() throws Exception{
		//Obtengo los roles en cuanto a los accesos a las organizaciones de la compa????a template
		MRoleOrgAccess[] orgRoles = MRoleOrgAccess.getOfClient(this.getCtx(),this.getClientTemplate());
				
		int total = orgRoles.length;
		MRoleOrgAccess orgRole = null;
		MRoleOrgAccess orgRoleAux = null;
		
		for(int i = 0; i < total ; i++){
			
			orgRoleAux = orgRoles[i];
			
			//Seteo el contexto con la compa????a y la organizaci??n nueva
			this.setCtx(this.getNewClient(),this.getOrgId(orgRoleAux.getAD_Org_ID()));
			
			//Creo el nuevo registro para la organizaci??n y el rol que se relacionan con la template
			orgRole = new MRoleOrgAccess(this.getCtx(),0,this.getTrxName());
			
			//Seteo el rol
			orgRole.setAD_Role_ID(((MRole)this.getRoles().get(orgRoleAux.getAD_Role_ID())).getAD_Role_ID());
			
			//Guardo el nuevo role org access
			if(!orgRole.save()){
				throw new Exception("No se pudo crear los roles para las organizaciones");
			}
		}
	}
	
	/**
	 * Crea los usuarios a partir de los usuarios de la compa????a template
	 */
	private void createAllUsers() throws Exception{
		//Creo el grupo de la entidad comercial para los usuarios 
		int bpGroup = this.createBPGroup();	
		
		this.setCtx(this.getClientTemplate());
		
		//Obtengo los usuarios del cliente template
		MUser[] users = MUser.getOfClient(this.getCtx(), this.getTrxName());
		
		int total = users.length;
		MUser userAux = null;
				
		//Itero por todos los usuarios de la compa????a template, creando los nuevos usuarios, 
		//bpartners relacionados y perfiles
		for(int i = 0; i < total ; i++){

			userAux = users[i];
			
			//Creo los usuarios a partir de los usuarios template
			int idUser = this.createUser(userAux,bpGroup);
						
			//Creo los perfiles y los menues para los nuevos usuarios a partir de los usuarios template
			this.createUserProfiles(userAux.getID(),idUser);		
			
			//Copiar los accesos directos del menu (segun usuario y arbol)
			for (X_AD_TreeBar treeBarEntry : getTreeBarEntries())
			{
				//si es una entrada del usuario en la template...
				if (treeBarEntry.getAD_User_ID() == userAux.getAD_User_ID())
				{
					//generar las entrada para el nuevo ususario y ad_tree
					copyTreeBarEntry(treeBarEntry, idUser);
				}
			}
		}
		
		/** Reasignar los accesos directos del menu del usuario Supervisor a la nueva compa????a */
		//si es una entrada del usuario Supervisor, generar las entrada del Supervisor para la nueva compa??ia y org		
		for (X_AD_TreeBar treeBarEntry : getTreeBarEntries())
			if (treeBarEntry.getAD_User_ID() == USER_SUPERVISOR_ID)
				copyTreeBarEntry(treeBarEntry, USER_SUPERVISOR_ID);

	}
	
	/**
	 * Genera una copia de una instancia de X_AD_TreeBar segun la informacion de los HashMaps existentes  
	 * @param sourceTreeBar entrada original a copiar
	 * @param newUserID nuevo usuario a asignar la entrada
	 */
	private void copyTreeBarEntry(X_AD_TreeBar sourceTreeBar, int newUserID)
	{
		MTreeBar newTreeBar = new MTreeBar(getCtx(), 0, getTrxName());
		newTreeBar.setAD_Tree_ID( getTrees().get(sourceTreeBar.getAD_Tree_ID() ) );
		newTreeBar.setAD_User_ID( newUserID );
		newTreeBar.setNode_ID( sourceTreeBar.getNode_ID() );
		newTreeBar.setClientOrg( getNewClient() , ((MOrg)(getOrgs().get(sourceTreeBar.getAD_Org_ID()))).getAD_Org_ID() );
		newTreeBar.setIsActive(true);
		newTreeBar.save();
	}
	
	/**
	 * Copia toda la informaci??n de la compa????a template a la nueva compa????a
	 */
	public void ejecutar() throws Exception{
		
		//Creo la informaci??n inicial, las organizaciones y los perfiles, de la compa????a nueva 
		//a partir de la compa????a template
		this.createInitialInfo();
		
		//Crear todos los datos por usuario cada vez
		this.createAllUsers();
		
		// Realiza la configuraci??n de la compa????a nueva en base a datos de la compa????a template
		this.configureClient();
	}
	
	
	public void deshacer() {
		// TODO: implementar!
	}

}

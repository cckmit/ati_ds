package org.openXpertya.apps.form;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;

import org.openXpertya.apps.ProcessCtl;
import org.openXpertya.apps.form.VModelHelper.ResultItem;
import org.openXpertya.cc.CurrentAccountManager;
import org.openXpertya.model.CalloutInvoiceExt;
import org.openXpertya.model.MAllocationHdr;
import org.openXpertya.model.MCashLine;
import org.openXpertya.model.MDiscountConfig;
import org.openXpertya.model.MDiscountSchema;
import org.openXpertya.model.MDocType;
import org.openXpertya.model.MEntidadFinancieraPlan;
import org.openXpertya.model.MInvoice;
import org.openXpertya.model.MInvoiceLine;
import org.openXpertya.model.MInvoicePaySchedule;
import org.openXpertya.model.MLetraComprobante;
import org.openXpertya.model.MOrg;
import org.openXpertya.model.MOrgInfo;
import org.openXpertya.model.MPInstance;
import org.openXpertya.model.MPOS;
import org.openXpertya.model.MPOSJournal;
import org.openXpertya.model.MPOSPaymentMedium;
import org.openXpertya.model.MPayment;
import org.openXpertya.model.MPaymentTerm;
import org.openXpertya.model.MRefList;
import org.openXpertya.model.MTax;
import org.openXpertya.model.RetencionProcessor;
import org.openXpertya.model.X_C_AllocationHdr;
import org.openXpertya.process.ProcessInfo;
import org.openXpertya.rc.Invoice;
import org.openXpertya.rc.ReciboDeCliente;
import org.openXpertya.util.ASyncProcess;
import org.openXpertya.util.CLogger;
import org.openXpertya.util.CPreparedStatement;
import org.openXpertya.util.DB;
import org.openXpertya.util.DisplayType;
import org.openXpertya.util.Env;
import org.openXpertya.util.KeyNamePair;
import org.openXpertya.util.Msg;
import org.openXpertya.util.TimeUtil;
import org.openXpertya.util.Util;
import org.openXpertya.util.ValueNamePair;

public class VOrdenCobroModel extends VOrdenPagoModel {

	/** Locale AR activo? */
	public final boolean LOCALE_AR_ACTIVE = CalloutInvoiceExt.ComprobantesFiscalesActivos();
	
	/** Error por defecto de punto de venta */
	public static final String DEFAULT_POS_ERROR_MSG = "CanGetPOSNumberButEmptyCR";
	
	/** Mensaje de error de punto de venta */
	public static String POS_ERROR_MSG = DEFAULT_POS_ERROR_MSG;
	/**
	 * Medios de pago disponibles para selecci??n. Asociaci??n por tipo de medio
	 * de pago.
	 */
	protected Map<String, List<MPOSPaymentMedium>> paymentMediums;
	
	/** Entidades Financieras y sus planes */
	protected Map<Integer, List<MEntidadFinancieraPlan>> entidadFinancieraPlans;
	
	/** Lista de Bancos */
	protected Map<String, String> banks;
	
	/** Recibo de Cliente con el matching de facturas y discount calculators */
	private ReciboDeCliente reciboDeCliente; 
	
	/** Punto de Venta */
	private Integer POS;

	/** Facturas creadas como d??bito y/o cr??dito en base a descuentos/recargos */
	private List<MInvoice> customInvoices = new ArrayList<MInvoice>();
	
	/** Info de la organizaci??n actual */
	private MOrgInfo orgInfo;
	
	/** Existe alguna factura vencida? */
	private boolean existsOverdueInvoices = false;
			
	public VOrdenCobroModel() {
		super();
		getMsgMap().put("TenderType", "CustomerTenderType");
		getMsgMap().put("AdvancedPayment", "AdvancedCustomerPayment");
		getMsgMap().put("Payment","CustomerPayment");
		reciboDeCliente = new ReciboDeCliente(getCtx(),getTrxName());
	}

	@Override
	protected OpenInvoicesTableModel getInvoicesTableModel(){
		return new OpenInvoicesCustomerReceiptsTableModel();
	}
	
	@Override
	protected String getIsSOTrx() {
		return "Y";
	}

	@Override
	protected int getSignoIsSOTrx() {
		return 1;
	}

	@Override
	protected void calculateRetencions() {
		// Para los recibos de clientes, las retenciones no se calculan, 
		// se ingresan manualmente. Aqu?? no debe realizarse ninguna operaci??n de c??lculo.
	}

	@Override
	public String getChequeChequeraSqlValidation() {
		return " C_BankAccount.BankAccountType = 'C'";
	}
	
	public String getRetencionSqlValidation() {
		// Solo esquemas de retenciones sufridas.
		return " C_RetencionSchema.RetencionApplication = 'S' ";
	}

	
	/**
	 * Agrega una retenci??n como medio de cobro.
	 * @param retencionSchemaID Esquema de retenci??n
	 * @param retencionNumber Nro. de la retenci??n
	 * @param amount Monto de la retenci??n
	 * @param retencionDate Fecha de la retenci??n
	 * @throws Exception En caso de que se produzca un error al intentar cargar los datos
	 * del esquema de retenci??n.
	 */
	public void addRetencion(Integer retencionSchemaID, String retencionNumber, BigDecimal amount, Timestamp retencionDate) throws Exception {
		if (retencionSchemaID == null || retencionSchemaID == 0)
			throw new Exception("@FillMandatory@ @C_Withholding_ID@");
		if (retencionNumber == null || retencionNumber.length() == 0)
			throw new Exception("@FillMandatory@ @RetencionNumber@");
		if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
			throw new Exception("@NoAmountError@");
		if (retencionDate == null) 
			throw new Exception("@FillMandatory@ @Date@");
		
		RetencionProcessor retProcessor = getGeneratorRetenciones().addRetencion(retencionSchemaID);
		if (retProcessor == null)
			throw new Exception("@SaveRetencionError@");
		
		retProcessor.setDateTrx(retencionDate);
		retProcessor.setAmount(amount);
		retProcessor.setRetencionNumber(retencionNumber);
		
		addRetencion(retProcessor);
	}
	
	/**
	 * Quita una retenci??n manual de la lista de retenciones.
	 */
	public void removeRetencion(RetencionProcessor processor) {
		getRetenciones().remove(processor);
		updateRemovingRetencion(processor);
		updateTreeModel();
	}
	
	
	public void addRetencion(RetencionProcessor processor) {
//		getRetenciones().add(processor);
		updateAddingRetencion(processor);
		updateTreeModel();
	}

	/**
	 * Quita un pago adelantado de la lista de retenciones.
	 */
	public void removePagoAdelantado(MedioPagoAdelantado mpa) {
		getMediosPago().remove(mpa);
		updateTreeModel();
	}	

	@Override
	protected String getAllocHdrDescriptionMsg() {
		return "@CustomerReceipt@";   
	}

	@Override
	protected String getHdrAllocationType() {
		if (isNormalPayment())
			return MAllocationHdr.ALLOCATIONTYPE_CustomerReceipt;
		else
			return MAllocationHdr.ALLOCATIONTYPE_AdvancedCustomerReceipt;
	}

	@Override
	protected String getCashType() {
		return MCashLine.CASHTYPE_GeneralReceipts;
	}

	@Override
	protected String getReportValue() {
		return "CustomerReceipt";
	}

	/**
	 * Inicializa los medios de pago disponibles por tipo de medio de pago
	 */
	public void initPaymentMediums() {
		// Obtengo los medios de pago
		List<MPOSPaymentMedium> posPaymentMediums = MPOSPaymentMedium
				.getAvailablePaymentMediums(getCtx(), null,
						MPOSPaymentMedium.CONTEXT_POSOnly, true, getTrxName());
		paymentMediums = new HashMap<String, List<MPOSPaymentMedium>>();
		List<MPOSPaymentMedium> mediums = null;
		for (MPOSPaymentMedium mposPaymentMedium : posPaymentMediums) {
			mediums = paymentMediums.get(mposPaymentMedium.getTenderType());
			if(mediums == null){
				mediums = new ArrayList<MPOSPaymentMedium>();
			}
			mediums.add(mposPaymentMedium);
			paymentMediums.put(mposPaymentMedium.getTenderType(), mediums);
		}
	}

	/**
	 * Inicializa los planes de entidades financieras existentes
	 */
	public void initEntidadFinancieraPlans(){
		// Obtengo los planes
		List<MEntidadFinancieraPlan> eFPlans = MEntidadFinancieraPlan
				.getPlansAvailables(getCtx(), null, getTrxName());
		entidadFinancieraPlans = new HashMap<Integer, List<MEntidadFinancieraPlan>>();
		List<MEntidadFinancieraPlan> plans = null;
		for (MEntidadFinancieraPlan mEntidadFinancieraPlan : eFPlans) {
			plans = entidadFinancieraPlans.get(mEntidadFinancieraPlan.getID());
			if(plans == null){
				plans = new ArrayList<MEntidadFinancieraPlan>();
			}
			plans.add(mEntidadFinancieraPlan);
			entidadFinancieraPlans.put(mEntidadFinancieraPlan
					.getM_EntidadFinanciera_ID(), plans);
		}		
	}
	
	/**
	 * Obtengo los medios de pago de un tipo de medio de pago par??metro
	 * 
	 * @param tenderType
	 *            tipo de medio de pago
	 * @return lista de medios de pago del tipo de medio de pago par??metro
	 */
	public List<MPOSPaymentMedium> getPaymentMediums(String tenderType){
		if(paymentMediums == null){
			initPaymentMediums();
		}
		return paymentMediums.get(tenderType);
	}

	/**
	 * Obtengo los planes de la entidad financiera par??metro
	 * 
	 * @param entidadFinancieraID
	 *            id de la entidad financiera
	 * @return lista de los planes disponibles de la entidad financiera
	 *         par??metro
	 */
	public List<MEntidadFinancieraPlan> getPlans(Integer entidadFinancieraID){
		if(entidadFinancieraPlans == null){
			initEntidadFinancieraPlans();
		}
		return entidadFinancieraPlans.get(entidadFinancieraID);
	}
	
	/**
	 * Inicializo los bancos
	 */
	public void initBanks(){
		ValueNamePair[] bankList = MRefList.getList(MPOSPaymentMedium.BANK_AD_Reference_ID, false);
		banks = new HashMap<String, String>();
		for (int i = 0; i < bankList.length; i++) {
			banks.put(bankList[i].getValue(), bankList[i].getName());
		}
	}
	
	/**
	 * @param value value de la lista de bancos
	 * @return nombre del valor de la lista pasado como par??metro
	 */
	public String getBankName(String value){
		if(banks == null){
			initBanks();
		}
		return banks.get(value);
	}
	
	
	/**
	 * Agrego la tarjeta de cr??dito como medio de pago
	 * @param paymentMedium 
	 * @param plan
	 * @param creditCardNo
	 * @param couponNo
	 * @param amt
	 * @param bank
	 */
	public void addCreditCard(MPOSPaymentMedium paymentMedium,
			MEntidadFinancieraPlan plan, String creditCardNo, String couponNo,
			BigDecimal amt, String bank, int cuotasCount, BigDecimal cuotaAmt,
			Integer campaignID, Integer projectID) throws Exception {
		// Validaciones iniciales
		if (amt == null || amt.compareTo(BigDecimal.ZERO) <= 0)
			throw new Exception("@NoAmountError@");
		if(paymentMedium == null)
			throw new Exception("@FillMandatory@ @ReceiptMedium@");
		if(plan == null)
			throw new Exception("@FillMandatory@ @M_EntidadFinancieraPlan_ID@");
		if(bank == null)
			throw new Exception("@FillMandatory@ @C_Bank_ID@");
		MedioPagoTarjetaCredito tarjetaCredito = new MedioPagoTarjetaCredito();
		tarjetaCredito.setCouponNo(couponNo);
		tarjetaCredito.setCreditCardNo(creditCardNo);
		tarjetaCredito.setPaymentMedium(paymentMedium);
		tarjetaCredito.setEntidadFinancieraPlan(plan);
		tarjetaCredito.setImporte(amt);
		tarjetaCredito.setCuotasCount(cuotasCount);
		tarjetaCredito.setCuotaAmt(cuotaAmt);
		tarjetaCredito.setCampaign(campaignID);
		tarjetaCredito.setProject(projectID);
		tarjetaCredito.setSOTrx(true);
		tarjetaCredito.setBank(bank);
		tarjetaCredito.setDiscountSchemaToApply(getCurrentGeneralDiscount());
		addMedioPago(tarjetaCredito);
	}

	/**
	 * Agrega un cheque como medio de pago al ??rbol
	 * 
	 * @param paymentMedium
	 * @param chequeraID
	 * @param checkNo
	 * @param amount
	 * @param fechaEmi
	 * @param fechaPago
	 * @param chequeALaOrden
	 * @param bankName
	 * @param CUITLibrador
	 * @param checkDescription
	 * @throws Exception
	 */
	public void addCheck(MPOSPaymentMedium paymentMedium, Integer chequeraID,
			String checkNo, BigDecimal amount, Timestamp fechaEmi,
			Timestamp fechaPago, String chequeALaOrden, String bankName,
			String CUITLibrador, String checkDescription, Integer campaignID, 
			Integer projectID) throws Exception {
		// Validaciones iniciales
		if(Util.isEmpty(chequeraID, true))
			throw new Exception("@FillMandatory@ @C_BankAccount_ID@");
		if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
			throw new Exception("@NoAmountError@");
		if(paymentMedium == null)
			throw new Exception("@FillMandatory@ @ReceiptMedium@");
		if (fechaEmi == null)
			throw new Exception("@FillMandatory@ @EmittingDate@");
		if (fechaPago == null)
			throw new Exception("@FillMandatory@ @PayDate@");
		if (fechaPago.compareTo(fechaEmi) < 0) 
			throw new Exception(getMsg("InvalidCheckDueDate"));
		if (fechaPago.compareTo(getFechaPagoCheque(fechaEmi, paymentMedium)) > 0){
			Date maxDueDate = getFechaPagoCheque(fechaEmi, paymentMedium);
			throw new Exception(getMsg("InvalidCheckDueDateError")
					+ ": "
					+ DisplayType.getDateFormat(DisplayType.Date).format(
							maxDueDate));
		}
		if (checkNo.trim().equals(""))
			throw new Exception("@FillMandatory@ @CheckNo@");
		MedioPagoCheque cheque = new MedioPagoCheque(chequeraID, checkNo,
				amount, fechaEmi, fechaPago, chequeALaOrden);
		cheque.setPaymentMedium(paymentMedium);
		cheque.banco = bankName;
		cheque.cuitLibrador = CUITLibrador;
		cheque.descripcion = checkDescription;
		cheque.setCampaign(campaignID);
		cheque.setProject(projectID);
		cheque.setSOTrx(true);
		cheque.setDiscountSchemaToApply(getCurrentGeneralDiscount());
		addMedioPago(cheque);
	}
	
	
	@Override
	protected void addCustomPaymentInfo(MPayment pay, MedioPago mp){
		// Si es tarjeta de cr??dito agrego la info necesaria
		if(mp.getTipoMP().equals(MedioPago.TIPOMEDIOPAGO_TARJETACREDITO)){
			MedioPagoTarjetaCredito tarjeta = (MedioPagoTarjetaCredito)mp;
			pay.setTenderType(MPayment.TENDERTYPE_CreditCard);
			pay.setTrxType(MPayment.TRXTYPE_CreditPayment);
			pay.setCouponNumber(tarjeta.getCouponNo());
			pay.setCreditCardNumber(tarjeta.getCreditCardNo());
			pay.setCreditCardType(tarjeta.getCreditCardType());
			pay.setA_Bank(tarjeta.getBank());
			pay.setM_EntidadFinancieraPlan_ID(tarjeta.getEntidadFinancieraPlan().getID());
			tarjeta.setPayment(pay);
		}
	}
	
	
	/** Actualiza el modelo de la tabla de facturas
	 * 
	 * Tambien setea en cero los montos ingresados de cada factura. 
	 *
	 */
	public void actualizarFacturas() {
		
		int i = 1;
		
		if (m_facturas == null) {
			m_facturas = new Vector<ResultItem>();
			m_facturasTableModel.setResultItem(m_facturas);
		}
		m_facturas.clear();
		
				
		if ((!m_esPagoNormal) || (m_fechaFacturas == null || C_BPartner_ID == 0)) {
			
			// Si es pago adelantado, no muestra ninguna factura.
			
			m_facturasTableModel.fireChanged(false);
			return;
		}
		

		// paymenttermduedate
		
		StringBuffer sql = new StringBuffer();
		
		sql.append(" SELECT c_invoice_id, c_invoicepayschedule_id, orgname, documentno, dateinvoiced, duedate, convertedamt, openamt FROM ");
		sql.append("  (SELECT i.C_Invoice_ID, i.C_InvoicePaySchedule_ID, org.name as orgname, i.DocumentNo, dateinvoiced, coalesce(duedate,dateinvoiced) as DueDate, "); // ips.duedate
		sql.append("    abs(currencyConvert( i.GrandTotal, i.C_Currency_ID, ?, i.DateAcct, null, i.AD_Client_ID, i.AD_Org_ID)) as ConvertedAmt, ");
		sql.append("    currencyConvert( invoiceOpen(i.C_Invoice_ID, COALESCE(i.C_InvoicePaySchedule_ID, 0)), i.C_Currency_ID, ?, i.DateAcct, null, i.AD_Client_ID, i.AD_Org_ID) AS openAmt ");
		sql.append("  FROM c_invoice_v AS i ");
		sql.append("  LEFT JOIN ad_org org ON (org.ad_org_id=i.ad_org_id) ");
		sql.append("  LEFT JOIN c_invoicepayschedule AS ips ON (i.c_invoicepayschedule_id=ips.c_invoicepayschedule_id) ");
		sql.append("  INNER JOIN C_DocType AS dt ON (dt.C_DocType_ID=i.C_DocType_ID) ");
		sql.append("  WHERE i.IsActive = 'Y' AND i.DocStatus IN ('CO', 'CL') ");
		sql.append("    AND i.IsSOTRx = '" + getIsSOTrx() + "' AND GrandTotal <> 0.0 AND C_BPartner_ID = ? ");
		sql.append("    AND dt.signo_issotrx = " + getSignoIsSOTrx());
		
		if (AD_Org_ID != 0) 
			sql.append("  AND i.ad_org_id = ?  ");
		
		sql.append("  ) as openInvoices ");
		sql.append(" GROUP BY c_invoice_id, c_invoicepayschedule_id, orgname, documentno, dateinvoiced, duedate, convertedamt, openamt  ");
		sql.append(" HAVING openamt > 0.0 ");
		// Si agrupa no se puede filtrar por fecha de vencimiento, se deben traer todas
		if (!m_allInvoices && !getBPartner().isGroupInvoices())
			sql.append("  AND ( duedate IS NULL OR duedate <= ? ) ");
	
		sql.append(" ORDER BY DueDate");
		
		try {
			
			CPreparedStatement ps = DB.prepareStatement(sql.toString(), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE, getTrxName());
			
			ps.setInt(i++, C_Currency_ID);
			ps.setInt(i++, C_Currency_ID);
			ps.setInt(i++, C_BPartner_ID);
			
			if (AD_Org_ID != 0)
				ps.setInt(i++, AD_Org_ID);
			
			if (!m_allInvoices && !getBPartner().isGroupInvoices())
				ps.setTimestamp(i++, m_fechaFacturas);

			ResultSet rs = ps.executeQuery();
			//int ultimaFactura = -1;
			while (rs.next()) {
				ResultItemFactura rif = new ResultItemFactura(rs);
				// Actualizar el descuento/recargo del esquema de vencimientos
				updatePaymentTermInfo(rif);
				m_facturas.add(rif);
			}
			
		} catch (Exception e) {
			log.log(Level.SEVERE, "", e);
		}
		
		applyBPartnerDiscount();
		m_facturasTableModel.fireChanged(false);
		
	}

	/**
	 * Actualizaci??n del descuento/recargo de las facturas
	 * 
	 * @param rif
	 *            ??tem de factura de la tabla, factura o cuota
	 */
	protected void updatePaymentTermInfo(ResultItemFactura rif){
		// ID de factura
		int invoiceID = (Integer) rif.getItem(m_facturasTableModel
				.getIdColIdx());
		// ID de esquema de pago de factura
		Integer invoicePayScheduleID = (Integer) rif.getItem(m_facturasTableModel
				.getInvoicePayScheduleColIdx());
		// Fecha de Facturaci??n 
		Timestamp dateInvoiced = (Timestamp) rif
				.getItem(((OpenInvoicesCustomerReceiptsTableModel) m_facturasTableModel)
						.getDateInvoicedColIdx());
		// Fecha de vencimiento
		Timestamp dueDate = (Timestamp) rif.getItem(m_facturasTableModel
				.getDueDateColIdx());
		// Open Amt que servir?? de base para el c??lculo de descuento/recargo a
		// aplicar
		BigDecimal openAmt = (BigDecimal) rif.getItem(m_facturasTableModel
				.getOpenAmtColIdx()); 
		// ID de esquema de vencimientos de la factura
		int paymentTermID = DB
				.getSQLValue(
						getTrxName(),
						"SELECT c_paymentterm_id FROM c_invoice WHERE c_invoice_id = ?",
						invoiceID);
		// Obtener el esquema de vencimientos
		MPaymentTerm paymentTerm = new MPaymentTerm(getCtx(), paymentTermID,
				getTrxName());
		// Obtengo el esquema de pago de la factura
		MInvoicePaySchedule ips = null;
		if(!Util.isEmpty(invoicePayScheduleID, true)){
			ips = new MInvoicePaySchedule(getCtx(), invoicePayScheduleID,
					getTrxName());
		}
		// Obtengo el descuento y lo seteo dentro del Result Item
		BigDecimal discount = paymentTerm.getDiscount(ips, dateInvoiced,
				dueDate, new Timestamp(System.currentTimeMillis()), openAmt);
		rif.setPaymentTermDiscount(discount);
	}
	
	@Override
	public void updateBPartner(Integer bPartnerID){
		super.updateBPartner(bPartnerID);
		// Actualizo el descuento
		reciboDeCliente.updateBPartner(bPartnerID);
	}

	/**
	 * Determino si existen facturas vencidas a partir de la fecha de
	 * vencimiento par??metro
	 * 
	 * @param dueDate
	 *            fecha de vencimiento de una factura o cuota
	 */
	protected void setIsOverdueInvoice(Timestamp dueDate){
		if(dueDate == null)return;
		// Si la fecha de vencimiento de la factura se encuentra despu??s de la
		// fecha de login, entonces existen facturas vencidas del cliente
		if(TimeUtil.getDiffDays(dueDate,Env.getDate()) > 0){
			existsOverdueInvoices = true;
		}
	}
		
	/**
	 * Aplica el esquema de descuento a las facturas existentes si pueden ser
	 * aplicables seg??n cada calculator discount propio
	 */
	public void applyBPartnerDiscount(){
		reciboDeCliente.applyBPartnerDiscount();
	}
	
	
	public MDiscountSchema getDiscountFrom(MEntidadFinancieraPlan plan){
		return reciboDeCliente.getDiscountFrom(plan);
	}
	
	public MDiscountSchema getDiscountFrom(MPOSPaymentMedium paymentMedium){
		return reciboDeCliente.getDiscountFrom(paymentMedium);
	}
	
	public MDiscountSchema getDiscount(MPOSPaymentMedium paymentMedium, MEntidadFinancieraPlan plan){
		return reciboDeCliente.getDiscount(paymentMedium, plan);
	}
	
	/**
	 * @param discountSchema
	 * @param baseAmt
	 */
	public void updateGeneralDiscount(MDiscountSchema discountSchema){
		reciboDeCliente.setCurrentGeneralDiscountSchema(discountSchema);
	}

	public MDiscountSchema getCurrentGeneralDiscount(){
		return reciboDeCliente.getCurrentGeneralDiscountSchema();
	}
	
	/**
	 * @param verifyApplication
	 *            true si se debe verificar si es posible aplicar el descuento
	 *            de entidad comercial en base a configuraci??n de descuento,
	 *            false si solo se debe retornar el descuento de entidad
	 *            comercial configurado
	 * @return esquema de descuentos de la entidad comercial o null caso que no
	 *         exista ninguno configurado o no se pueda aplicar (si es true el
	 *         par??metro)
	 */
	public MDiscountSchema getbPartnerDiscountSchema(boolean verifyApplication){
		MDiscountSchema discountSchema = reciboDeCliente.getbPartnerDiscountSchema();
		if(verifyApplication){
			if(discountSchema != null){
				if (!reciboDeCliente.isBPartnerDiscountApplicable(
						discountSchema.getDiscountContextType(), getBPartner()
								.getDiscountContext())) {
					discountSchema = null;
				}
			}
		}
		return discountSchema;
	}
	
	@Override
	protected void updateInvoicesModel(Vector<Integer> facturasProcesar, Vector<BigDecimal> manualAmounts, Vector<ResultItemFactura> resultsProcesar){
		reciboDeCliente.clear();
		int totalInvoice = facturasProcesar.size();
		BigDecimal paymentTermDiscount;
		for (int i = 0; i < totalInvoice ;i++) {
//			reciboDeCliente.add(AD_Org_ID,facturasProcesar.get(i),manualAmounts.get(i), true);
			paymentTermDiscount = resultsProcesar.get(i).getPaymentTermDiscount();
			reciboDeCliente.add(facturasProcesar.get(i),manualAmounts.get(i), paymentTermDiscount, true);
		}
		
		// Creo la factura global
		reciboDeCliente.makeGlobalInvoice();
	}
	
	
	public BigDecimal getToPayAmount(MDiscountSchema discountSchema){
		BigDecimal toPay = reciboDeCliente.getToPayAmt(discountSchema);
		return toPay;
	}
	
	/**
	 * @param discountContextType
	 *            tipo de contexto de descuento
	 * @return Indica si son aplicables los descuentos por medios de pagos en el
	 *         contexto pasado como par??metro
	 */
	public boolean isPaymentMediumDiscountApplicable(String discountContextType) {
		return reciboDeCliente
				.isGeneralDocumentDiscountApplicable(discountContextType);
	}
	
	@Override
	protected void updateAditionalInfo(){
		// Actualizo el descuento de entidad comercial
		reciboDeCliente.updateBPDiscountSchema();
	}

	/**
	 * Suma los descuentos realizados al recibo de cliente (a todas las
	 * facturas)
	 * 
	 * @return suma de los descuentos aplicados hasta el momento
	 */
	public BigDecimal getSumaDescuentos(){
		return reciboDeCliente.getTotalDiscountAmt();
	}
	
	@Override
	protected void updateAddingMedioPago(MedioPago mp){
		if(getToPayAmount(null).compareTo(BigDecimal.ZERO) != 0){
			// Agregar el medio de pago en el recibo
			reciboDeCliente.addMedioPago(mp);
		}
	}

	@Override
	protected void updateRemovingMedioPago(MedioPago mp){
		// Eliminar el medio de pago en el recibo
		reciboDeCliente.removeMedioPago(mp);
	}
	
	@Override
	protected void updateAddingRetencion(RetencionProcessor processor){
		if(getToPayAmount(null).compareTo(BigDecimal.ZERO) != 0){
			// Agregar la retenci??n en el recibo
			reciboDeCliente.addRetencion(processor);
		}
	}

	@Override
	protected void updateRemovingRetencion(RetencionProcessor processor){
		// Eliminar la retencion en el recibo
		reciboDeCliente.removeRetencion(processor);
	}
	
	/**
	 * Actualiza todos los descuentos
	 */
	public void updateDiscounts(){
		// Actualizar descuentos
		reciboDeCliente.updateDiscounts();
	}

	@Override
	public void addDebit(MInvoice invoice){
		reciboDeCliente.addDebit(invoice);
	}
	
	protected List<Invoice> ordenarFacturas(List<Invoice> invoicesToSort) {
		Comparator<Invoice> cmp = new Comparator<Invoice>() {
			public int compare(Invoice arg0, Invoice arg1) {
				BigDecimal b0 = arg0.getManualAmt();
				BigDecimal b1 = arg1.getManualAmt();
				return b0.compareTo(b1);
			}
		};
		Invoice[] invoicesSorted = new Invoice[invoicesToSort.size()];
		invoicesToSort.toArray(invoicesSorted);		
		Arrays.sort(invoicesSorted, cmp);
		return Arrays.asList(invoicesSorted);
	}
	
	@Override
	public void updateOrg(int orgID){
		super.updateOrg(orgID);
		orgInfo = MOrgInfo.get(getCtx(), orgID);
		// Tomar la configuraci??n de descuentos de la organizaci??n seleccionada
		reciboDeCliente.updateOrg(orgID);
		// Obtener el punto de venta de cajas diarias
		setPOS(getPOSNumber());
	}

	/**
	 * @return true si es posible obtener el punto de venta 
	 */
	public boolean autoGettingPOSValidation(){
		return LOCALE_AR_ACTIVE && getPOSNumber() != null;
	}

	/**
	 * @return true si existe la posibilidad de obtener el punto de venta
	 *         autom??ticamente, false caso contrario
	 */
	public boolean mustGettingPOSNumber(){
		return LOCALE_AR_ACTIVE;
	}

	/**
	 * Obtengo el nro de punto de venta para el usuario actual. Primero verifico
	 * la caja diaria activa, sino obtengo de la configuraci??n del TPV para ese
	 * usuario y organizaci??n. Si existe m??s de una configuraci??n no se coloca
	 * ninguna para que el usuario inserte la adecuada.
	 * 
	 * @return nro de punto de venta para el usuario y organizaci??n actual, null
	 *         si no existe ninguna o m??s de una
	 */
	public Integer getPOSNumber(){
		// Obtenerlo de la caja diaria
		Integer posNro = MPOSJournal.getCurrentPOSNumber();
		boolean error = true;
		// Si no existe, directamente el pto de venta del usuario
		if(Util.isEmpty(posNro, true)){
			// Verificar los nros de los puntos de venta, si hay mas de uno
			// dejar vac??o para que lo complete el usuario
			List<MPOS> poss = MPOS.get(getCtx(), AD_Org_ID, null, getTrxName());
			if(poss.size() == 1){
				posNro = poss.get(0).getPOSNumber();
			}
			else if(poss.size() > 1){
				POS_ERROR_MSG = "CanGetPOSNumberButMoreThanOne";
				error = false;
			}
		}
		if(Util.isEmpty(posNro, true) && error){
			posNro = null;
			POS_ERROR_MSG = DEFAULT_POS_ERROR_MSG;
		}
		return posNro;
	}
	
	/**
	 * @return el cargo de cobro de facturas vencidas de la organizaci??n
	 */
	public BigDecimal getOrgCharge(){
		BigDecimal charge = BigDecimal.ZERO;
		if (existsOverdueInvoices && orgInfo != null) {
			charge = orgInfo.getOverdueInvoicesCharge();
		}
		return charge;
	}
	
	public Integer getPOS(){
		return POS;
	}
	
	public void setPOS(Integer pOS) {
		POS = pOS;
	}

	@Override
	public void updateOnlineAllocationLines(Vector<MedioPago> pays){
		// Eliminar todos los allocation anteriores
		onlineAllocationLines = null;
		onlineAllocationLines = new ArrayList<AllocationLine>();
		BigDecimal saldoMediosPago = getSaldoMediosPago(true);		
		// Ordenar los cr??ditos
		Vector<MedioPago> credits = new Vector<MedioPago>( pays );
		// Ordenar los d??bitos
		Vector<Invoice> debits = new Vector<Invoice>(
				ordenarFacturas(reciboDeCliente.getRealInvoices()));
		
		BigDecimal sobraDelPago = null;
		int payIdx = 0;
		int totalFacturas = debits.size();
		int totalPagos = credits.size();
		Invoice invoice;
		int C_Invoice_ID;
		BigDecimal deboPagar;
		BigDecimal sumaPagos;
		ArrayList<MedioPago> subPagos;
		ArrayList<BigDecimal> montosSubPagos;
		for (int i = 0; i < totalFacturas && payIdx < totalPagos;i++) {
			invoice = debits.get(i);
			// Recorro todas las facturas para pagarlas 
			C_Invoice_ID = invoice.getInvoiceID();
			
			deboPagar = invoice.getManualAmt().add(
					invoice.getTotalPaymentTermDiscount()); // Esto es lo que
															// tengo que cubrir
			sumaPagos = (sobraDelPago != null) ? sobraDelPago : credits.get(payIdx).getImporte(); // Inicializar lo que cubro
			subPagos = new ArrayList<MedioPago>();
			montosSubPagos = new ArrayList<BigDecimal>();
			
			// Puede haber mas de un pago por factura: busco cuales son.
			
			subPagos.add(credits.get(payIdx));
			montosSubPagos.add(sumaPagos);
			
			// Precondicion: Se asume que en este punto la cantidad de plata alcanza.
			
			// TODO: Redondeos de centavos ?? [(-0,01, 0.01)]
			boolean follow = true;
			while (compararMontos(deboPagar, sumaPagos) > 0 && follow) { // Mientras no haya cubrido
				payIdx++;
				follow = payIdx < totalPagos;
				if(follow){
					BigDecimal importe = credits.get(payIdx).getImporte();
					
					subPagos.add(credits.get(payIdx));
					montosSubPagos.add(importe);
					sumaPagos = sumaPagos.add(importe);
				}
			}

			if (compararMontos(deboPagar, sumaPagos) < 0) {
				// Si sobra ...
				int x = montosSubPagos.size() - 1;
				
				sobraDelPago = sumaPagos.subtract(deboPagar);
				
				// Si sobra plata, en el ultimo monto hay plata de m??s.
				montosSubPagos.set( x, montosSubPagos.get(x).subtract(sobraDelPago) );
			} else {
				payIdx++;
				sobraDelPago = null;
			}
			
			// Aca estoy seguro de que deboPagar.compareTo(sumaPagos) == 0			
			
			for (int subpayIdx = 0; subpayIdx < subPagos.size(); subpayIdx++) {
				
				MedioPago mp = subPagos.get(subpayIdx);
				BigDecimal AppliedAmt = montosSubPagos.get(subpayIdx);
				
				//
				// Si se cambian estos valores, verificar las monedas !!
				//
				
				BigDecimal DiscountAmt = Env.ZERO;
				BigDecimal WriteoffAmt = Env.ZERO;
				BigDecimal OverunderAmt = Env.ZERO;

				if (saldoMediosPago.signum() != 0) {
					WriteoffAmt = saldoMediosPago; // Redondeo de minusculos centavitos.
					saldoMediosPago = BigDecimal.ZERO;
				}
				
				AllocationLine allocLine =	new AllocationLine();
				allocLine.setAmt(AppliedAmt);
				allocLine.setDiscountAmt(DiscountAmt);
				allocLine.setWriteOffAmt(WriteoffAmt);
				allocLine.setOverUnderAmt(OverunderAmt);
				allocLine.setDebitDocumentID(C_Invoice_ID);
				allocLine.setCreditPaymentMedium(mp);
				onlineAllocationLines.add(allocLine);
			}
		}
	}
	
	
	@Override
	protected void makeCustomDebitsCredits(Vector<MedioPago> pays) throws Exception{
		// Crear los d??bitos para recargos
		// Crear medios de pago de cr??dito para descuentos
		// Obtener la suma de descuentos agrupada por tasa de impuesto y crear las
		// l??neas de factura en la factura que corresponda
//		BigDecimal discountSum = getSumaDescuentos();
//		// FIXME qu?? pasa cuando se aplicaron descuentos y recargos a la vez
//		// donde el total da 0?
//		if (discountSum.compareTo(BigDecimal.ZERO) == 0
//				&& getTotalPaymentTermDiscount().compareTo(BigDecimal.ZERO) == 0) {
//			return;
//		}
		// Obtener la suma de descuentos/recargos por tipo de descuento
		Map<String, BigDecimal> discountsPerKind = reciboDeCliente.getDiscountsSumPerKind();
		Set<String> kinds = discountsPerKind.keySet();
		// Si no existe descuento o recargo 
		MInvoice credit = null;
		MInvoice debit = null;
		MInvoice inv = null; 
		MInvoiceLine invoiceLine = null; 
		BigDecimal amt;
		boolean isCredit;
		boolean createInvoice;
		MTax tax = MTax.getTaxExemptRate(getCtx(),getTrxName());
		for (String discountKind : kinds) {
			amt = discountsPerKind.get(discountKind);
			if(amt.compareTo(BigDecimal.ZERO) != 0){
				isCredit = amt.compareTo(BigDecimal.ZERO) > 0;
				// Si es cargo por organizaci??n entonces el monto de cargo viene
				// positivo, no es descuento sino recargo por eso hay que
				// intercambiar el valor 
				if(discountKind.equals(ReciboDeCliente.CHARGE_DISCOUNT)){
					isCredit = !isCredit;
				}
				createInvoice = isCredit?credit == null:debit==null;
				if(createInvoice){
					// Crear la factura
					inv = createCreditDebitInvoice(isCredit);
					inv.setBPartner(getBPartner());
					if(!inv.save()){
						throw new Exception("Can't create " + (isCredit ? "credit" : "debit")
								+ " document for discounts. Original Error: "+CLogger.retrieveErrorAsString());
					}
					if(isCredit){
						credit = inv;
					}
					else{
						debit = inv;
					}
				}
				// Si es cr??dito 
				inv = isCredit?credit:debit;
				// Creo la l??nea de la factura
				invoiceLine = createInvoiceLine(inv,isCredit,amt,tax,discountKind);				
				if(!invoiceLine.save()){
					throw new Exception("Can't create " + (isCredit ? "credit" : "debit")
							+ " document line for discounts. Original Error: "+CLogger.retrieveErrorAsString());  
				}
			}
		}
		// - Si es hay cr??dito lo guardo como un medio de pago
		// - Si es d??bito lo guardo donde se encuentran las facturas
		if(credit != null){			
			// Completar el cr??dito en el caso que no requiera impresi??n fiscal,
			// ya que si requieren se realiza al final del procesamiento
			if(!needFiscalPrint(credit)){
				processDocument(credit, MInvoice.DOCACTION_Complete);
			}
			// Asociar como medio de pago
			MedioPagoCredito credito = new MedioPagoCredito(true);
			credito.setC_invoice_ID(credit.getID());
			credito.setImporte(credit.getGrandTotal());
			pays.add(credito);
			// Agregarla a la lista local para despu??s jugar con ellas para la
			// cuenta corriente del cliente
			customInvoices.add(credit);
		}
		if(debit != null){
			// Completar el cr??dito en el caso que no requiera impresi??n fiscal,
			// ya que si requieren se realiza al final del procesamiento
			if(!needFiscalPrint(debit)){
				processDocument(debit, MInvoice.DOCACTION_Complete);
			}
			// Agregarlo como nuevo d??bito
			addDebit(debit);
			// Agregarla a la lista local para despu??s jugar con ellas para la
			// cuenta corriente del cliente
			customInvoices.add(debit);
		}
	}

	/**
	 * Creo una factura como cr??dito o d??bito, dependiendo configuraci??n.
	 * 
	 * @param credit
	 *            true si se debe crear un cr??dito o false si es d??bito
	 * @return factura creada
	 * @throws Exception en caso de error
	 */
	protected MInvoice createCreditDebitInvoice(boolean credit) throws Exception{
		MInvoice invoice = new MInvoice(getCtx(), 0, getTrxName());
		invoice.setBPartner(getBPartner());
		// Setear el tipo de documento
		invoice = setDocType(invoice, credit);
		
		if(LOCALE_AR_ACTIVE){
			invoice = addLocaleARData(invoice, credit);
		}
		
		// Se indica que no se debe crear una l??nea de caja al completar la factura ya
		// que es el propio TPV el que se encarga de crear los pagos e imputarlos con
		// la factura (esto soluciona el problema de l??neas de caja duplicadas que 
		// se hab??a detectado).
		invoice.setCreateCashLine(false);
		
		invoice.setDocAction(MInvoice.DOCACTION_Complete);
		invoice.setDocStatus(MInvoice.DOCSTATUS_Drafted);
		// Seteo el bypass de la factura para que no chequee el saldo del
		// cliente porque ya lo chequea el tpv
		invoice.setCurrentAccountVerified(true);
		// Seteo el bypass para que no actualice el cr??dito del cliente ya
		// que se realiza luego al finalizar las operaciones
		invoice.setUpdateBPBalance(false);
		return invoice;
	}

	/**
	 * Setea el tipo de documento a la factura par??metro
	 * 
	 * @param invoice
	 *            factura a modificar
	 * @param isCredit
	 *            booleano que determina si lo que estoy creando es un d??bito o
	 *            un cr??dito
	 * @return factura con el tipo de doc seteada
	 */
	protected MInvoice setDocType(MInvoice invoice, boolean isCredit) throws Exception{
		// Obtengo el doctype de configuraci??n
		Integer docType = null;
		String generalDocType = null;
		if(isCredit){
			generalDocType = reciboDeCliente.getGeneralCreditDocType();
			if(generalDocType.equals(MDiscountConfig.CREDITDOCUMENTTYPE_Other)){
				docType = reciboDeCliente.getCreditDocType();
			}
		}
		else{
			generalDocType = reciboDeCliente.getGeneralDebitDocType();
			if(generalDocType.equals(MDiscountConfig.DEBITDOCUMENTTYPE_Other)){
				docType = reciboDeCliente.getDebitDocType();
			}
		}
		MDocType documentType = null;
		// Si tengo un tipo de documento real configurado, entonces obtengo ese
		// tipo de doc
		if(docType != null){
			documentType = new MDocType(getCtx(), docType, getTrxName());
		}
		// Si todav??a no encontr?? el tipo de doc, lo busco en base al general y
		// si estamos sobre Locale_Ar activo
		if(documentType == null){
			// Obtener la clave del tipo de documento a general
			String docTypeKey = getRealDocTypeKey(generalDocType, isCredit);
			// Si est?? activo locale_ar entonces se debe obtener en base al pto de venta y la letra
			if(LOCALE_AR_ACTIVE){
				MLetraComprobante letra = getLetraComprobante();
				if(Util.isEmpty(getPOS(),true)) throw new Exception(getMsg("NotExistPOSNumber"));
				Integer posNumber = Integer.valueOf(getPOS());
				// Se obtiene el tipo de documento para la factura.
				documentType = MDocType.getDocType(getCtx(), docTypeKey,
						letra.getLetra(), posNumber, getTrxName());
				if (documentType == null) {
					throw new Exception(Msg.getMsg(getCtx(),
							"NonexistentPOSDocType", new Object[] { letra,
									posNumber }));
				}
				if(!Util.isEmpty(posNumber,true)){
					invoice.setPuntoDeVenta(posNumber);
				}
			}
			else{
				// Tipo de documento en base a la key
				documentType = MDocType.getDocType(getCtx(), docTypeKey, getTrxName()); 
			}
		}
		invoice.setC_DocTypeTarget_ID(documentType.getID());
		return invoice;
	}

	/**
	 * Obtener la letra del comprobante
	 * 
	 * @return letra del comprobante
	 * @throws Exception
	 *             si la compa????a o el cliente no tienen configurado una
	 *             categor??a de IVA y si no existe una Letra que los corresponda
	 */
	protected MLetraComprobante getLetraComprobante() throws Exception{
		Integer categoriaIVAclient = CalloutInvoiceExt.darCategoriaIvaClient();
		Integer categoriaIVACustomer = getBPartner().getC_Categoria_Iva_ID();
		// Se validan las categorias de IVA de la compa??ia y el cliente.
		if (categoriaIVAclient == null || categoriaIVAclient == 0) {
			throw new Exception(getMsg("ClientWithoutIVAError"));
		} else if (categoriaIVACustomer == null || categoriaIVACustomer == 0) {
			throw new Exception(getMsg("BPartnerWithoutIVAError"));
		}
		// Se obtiene el ID de la letra del comprobante a partir de las categorias de IVA.
		Integer letraID = CalloutInvoiceExt.darLetraComprobante(categoriaIVACustomer, categoriaIVAclient);
		if (letraID == null || letraID == 0){
			throw new Exception(getMsg("LetraCalculationError"));
		}
		// Se obtiene el PO de letra del comprobante.
		return new MLetraComprobante(getCtx(), letraID, getTrxName());
	}

	/**
	 * Obtener la clave del tipo de documento real en base al general y si el
	 * comprobante que estoy creando es un cr??dito o un d??bito
	 * 
	 * @param generalDocType
	 *            tipo de documento general
	 * @param isCredit
	 *            true si estamos ante un cr??dito, false caso contrario
	 * @return
	 */
	protected String getRealDocTypeKey(String generalDocType, boolean isCredit){
		// La lista de tipos de documento generales tiene como value los doc
		// type keys de los tipos de documento
		String docTypeKey = generalDocType;
		// Para Locale AR, Abono de Cliente es Nota de Cr??dito o Nota de D??bito
		// dependiendo si estamos creando un cr??dito o un d??bito respectivamente 
		if(LOCALE_AR_ACTIVE){
			// Nota de Cr??dito
			if (isCredit
					&& generalDocType
							.equals(MDiscountConfig.CREDITDOCUMENTTYPE_CreditNote)) {
				docTypeKey = MDocType.DOCTYPE_CustomerCreditNote;
			}
			// Nota de D??bito
			if (!isCredit
					&& generalDocType
							.equals(MDiscountConfig.DEBITDOCUMENTTYPE_DebitNote)) {
				docTypeKey = MDocType.DOCTYPE_CustomerDebitNote;
			}
		}
		return docTypeKey;
	}
	
	/**
	 * Agregar la info de locale ar necesaria a la factura con localizaci??n
	 * argentina.
	 * 
	 * @param invoice
	 *            factura
	 * @param credit true si estamos ante un cr??dito, false si es d??bito
	 * @return factura par??metro con toda la info localeAr necesaria cargada
	 * @throws Exception en caso de error
	 */
	protected MInvoice addLocaleARData(MInvoice invoice, boolean credit) throws Exception{
		MLetraComprobante letraCbte = getLetraComprobante();
		// Se asigna la letra de comprobante, punto de venta y n??mero de comprobante
		// a la factura creada.
		invoice.setC_Letra_Comprobante_ID(letraCbte.getID());
		// Nro de comprobante.
		Integer nroComprobante = CalloutInvoiceExt
				.getNextNroComprobante(invoice.getC_DocTypeTarget_ID());
		if (nroComprobante != null)
			invoice.setNumeroComprobante(nroComprobante);
		
		// Asignaci??n de CUIT en caso de que se requiera.
		String cuit = getBPartner().getTaxID();
		invoice.setCUIT(cuit);
		
		// Setear una factura original al cr??dito que estamos creando
		if(credit && LOCALE_AR_ACTIVE){
			// Obtengo la primer factura como random (la impresora fiscal puede tirar un error si no existe una factura original seteada)
			Invoice firstInvoice = reciboDeCliente.getRealInvoices().get(0);
			if(firstInvoice != null){
				invoice.setC_Invoice_Orig_ID(firstInvoice.getInvoiceID());
			}
		}
		
		return invoice;
	}

	/**
	 * Crea una l??nea de factura de la factura y datos par??metro.
	 * 
	 * @param invoice
	 *            factura
	 * @param isCredit
	 *            true si estamos creando un cr??dito, false caso contrario
	 * @param amt
	 *            monto de la l??nea
	 * @param tax
	 *            impuesto para la l??nea
	 * @param discountKind
	 *            tipo de descuento para obtener el art??culo correspondiente de
	 *            la configuraci??n de descuentos
	 * @return l??nea de la factura creada
	 * @throws Excepci??n
	 *             en caso de error
	 */
	public MInvoiceLine createInvoiceLine(MInvoice invoice, boolean isCredit, BigDecimal amt, MTax tax, String discountKind) throws Exception{
		MInvoiceLine invoiceLine = new MInvoiceLine(invoice);
		invoiceLine.setQty(1);
		// Setear el precio con el monto del descuento
		amt = amt.abs();
		Integer sign = reciboDeCliente.getGeneralDocTypeSign(isCredit);
		amt = amt.multiply(new BigDecimal(sign));
		invoiceLine.setPriceEntered(amt);
		invoiceLine.setPriceActual(amt);
		invoiceLine.setPriceList(amt);
		invoiceLine.setC_Tax_ID(tax.getID());
		invoiceLine.setLineNetAmt();
		// Setear el art??culo
		// Buscar el art??culo en la config
		Integer productID = reciboDeCliente.getConfigProductID(isCredit, discountKind);
		if(Util.isEmpty(productID,true)){
			throw new Exception(
					"Falta configuracion de articulos para crear cr??ditos/d??bitos para descuentos/recargos");
		}
		invoiceLine.setM_Product_ID(productID);
		return invoiceLine;
	}

	/**
	 * Indica si la factura debe ser emitida mediante un controlador fiscal.
	 * @param invoice Factura a evaluar.
	 */
	private boolean needFiscalPrint(MInvoice invoice) {
		return MDocType.isFiscalDocType(invoice.getC_DocTypeTarget_ID())
				&& LOCALE_AR_ACTIVE;
	}
	
	/**
	 * Procesa la factura en base a un docaction par??metro
	 * 
	 * @param invoice
	 *            factura
	 * @param docAction
	 *            acci??n
	 * @throws Exception
	 *             si hubo error al realizar el procesamiento o al guardar
	 */
	public void processDocument(MInvoice invoice, String docAction) throws Exception{
		// Procesar el documento
		if(!invoice.processIt(docAction)){
			throw new Exception(invoice.getProcessMsg());
		}
		// Guardar
		if(!invoice.save()){
			throw new Exception(CLogger.retrieveErrorAsString());
		}
	}
	
	@Override
	public BigDecimal getSaldoMediosPago() {
		BigDecimal saldo = super.getSaldoMediosPago();
		saldo = saldo.subtract(getSumaDescuentos());
		return saldo;
	}
		
	@Override
	public BigDecimal getSumaTotalPagarFacturas() {
		BigDecimal suma = new BigDecimal(0);
		
		if (m_esPagoNormal) {
			suma = super.getSumaTotalPagarFacturas();
			suma = suma.add(existsOverdueInvoices ? getOrgCharge()
					: BigDecimal.ZERO); 
		} else {
			suma = m_montoPagoAnticipado;
		}
		
		return suma;
	}
	
	@Override
	public BigDecimal getAllocationHdrTotalAmt(){
		return reciboDeCliente.getTotalAPagar();
	}
	
	@Override
	protected void performAditionalCustomCurrentAccountWork(MOrg org, CurrentAccountManager manager) throws Exception{
		// Itero por las facturas creadas y realizo las tareas adicionales
		for (MInvoice invoice : customInvoices) {
			performAditionalCurrentAccountWork(org, getBPartner(), manager, invoice, true);
		}
	}
	
	@Override
	public void reset(){
		super.reset();
		reciboDeCliente = new ReciboDeCliente(getCtx(),getTrxName());
		customInvoices = new ArrayList<MInvoice>();
		existsOverdueInvoices = false;
	}
	
	public BigDecimal getSaldoMediosPago(boolean finishing){
		if(!finishing)return getSaldoMediosPago();
		BigDecimal saldo = super.getSaldoMediosPago();
		if (saldo.compareTo(BigDecimal.ZERO) != 0
				&& saldo.abs().compareTo(ReciboDeCliente.ROUND_TOLERANCE) > 0) {
			saldo = saldo.subtract(getSumaDescuentos());
		}
		return saldo;
	}
	
	/**
	 * Verificar si estamos cobrando facturas vencidas
	 */
	protected void updateOverdueInvoicesCharge(){
		Timestamp dueDate = null;
		existsOverdueInvoices = false;
		if(m_facturas != null){
			for (ResultItem f : m_facturas) {
				ResultItemFactura fac = (ResultItemFactura) f;
				if (fac.getManualAmount().signum() > 0) {
					dueDate = (Timestamp) fac.getItem(m_facturasTableModel
							.getDueDateColIdx());
					setIsOverdueInvoice(dueDate);
				}
			}
		}
		// Actualizo el recibo con el monto de cargo de la organizaci??n
		reciboDeCliente.setOrgCharge(getOrgCharge());
	}

	/**
	 * @return monto equivalente al total abierto de las facturas a fecha de
	 *         vencimiento menor o igual a la actual o del mismo mes + cargo si
	 *         hubiere
	 */
	public BigDecimal getDefaultGroupingValue(){
		BigDecimal defaultValue = BigDecimal.ZERO;
		int totalInvoices = m_facturas.size();
    	ResultItemFactura fac;
    	boolean charged = false;
    	boolean stop = false;
    	Timestamp dueDate;
    	Timestamp loginDate = Env.getDate();
    	Calendar loginCalendar = Calendar.getInstance();
    	loginCalendar.setTimeInMillis(loginDate.getTime());
    	Calendar dueCalendar = Calendar.getInstance();
		// Itero por las facturas y voy asignandoles el monto de agrupaci??n
		// desde la mas antigua a la mas nueva hasta llegar a que la fecha de
		// vencimiento sea mayor a la actual o que se encuentre en el mismo mes 
		for (int i = 0; i < totalInvoices && !stop; i++) {
			fac = (ResultItemFactura) m_facturas.get(i);
			dueDate = (Timestamp)fac.getItem(m_facturasTableModel.getDueDateColIdx());
			if(dueDate != null){
				dueCalendar.setTimeInMillis(dueDate.getTime());
			}
			// Paro cuando encuentro una fecha de vencimiento mayor a la fecha
			// actual o se encuentra en el mismo mes 
			stop = dueDate != null
					&& (TimeUtil.getDiffDays(loginDate, dueDate) > 0 
							||	((loginCalendar
									.get(Calendar.YEAR) == dueCalendar
									.get(Calendar.YEAR)) && (loginCalendar
									.get(Calendar.MONTH) == dueCalendar
									.get(Calendar.MONTH))));
			if(!stop){
				// Verifico la fecha de vencimiento ya que debo incrementar con el
				// cargo de la organizaci??n
				if(!charged){
					setIsOverdueInvoice(dueDate);
					if (getOrgCharge().compareTo(BigDecimal.ZERO) > 0) {
						defaultValue = defaultValue.add(getOrgCharge());
						charged = true;
					}
				}
				defaultValue = defaultValue.add(fac.getToPayAmt(true));
			}
		}
		return defaultValue;
	}
	
	
	/**
	 * Actualizar los montos manuales de las facturas prorateando el monto
	 * grupal par??metro
	 * 
	 * @param groupingAmt
	 *            monto de agrupaci??n
	 */
	public void updateGroupingAmtInvoices(BigDecimal groupingAmt){
		// Actualizar los montos manuales
    	BigDecimal amt = groupingAmt;
    	int totalInvoices = m_facturas.size();
    	ResultItemFactura fac;
    	BigDecimal currentManualAmt, toPay;
    	boolean chargeDecremented = false;
		// Itero por las facturas y voy asignandoles el monto de agrupaci??n
		// desde la mas antigua a la mas nueva
    	int i = 0;
		for (; i < totalInvoices && amt.compareTo(BigDecimal.ZERO) > 0; i++) {
			fac = (ResultItemFactura) m_facturas.get(i);
			// Verifico la fecha de vencimiento ya que debo decrementar primero
			// el cargo de la organizaci??n
			// CUIDADO: La lista de facturas debe ir ordenada de forma
			// ascendente por la fecha de vencimiento
			if(!chargeDecremented){
				setIsOverdueInvoice((Timestamp) fac.getItem(m_facturasTableModel
						.getDueDateColIdx()));
				if (getOrgCharge().compareTo(BigDecimal.ZERO) > 0) {
					amt = amt.subtract(getOrgCharge());
					chargeDecremented = true;
				}
			}
			// Si el monto es mayor a 0 sigo. Quiz??s con el decremento del cargo
			// se volvi?? a 0 o menor. Si se volvi?? menor habr?? un problema con
			// la factura o cuota anterior por eso debe ir ordenado por fecha de
			// vencimiento ascendente 
			if(amt.compareTo(BigDecimal.ZERO) > 0){
				// A pagar (open amt - descuento de payment term)
				toPay = fac.getToPayAmt(true);
				// Si el monto a pagar es mayor a 
				if(toPay.compareTo(amt) > 0){
					currentManualAmt = amt;
				}
				else{
					currentManualAmt = toPay;
				}
				fac.setManualAmount(currentManualAmt);
				amt = amt.subtract(currentManualAmt);
			}
		}
		// Dejar en 0 las facturas restantes
		for (; i < totalInvoices; i++) {
			fac = (ResultItemFactura) m_facturas.get(i);
			fac.setManualAmount(BigDecimal.ZERO);
		}
	}

	/**
	 * Calcula la fecha de pago del cheque a partir del plazo de pago
	 * configurado del medio de pago
	 * 
	 * @param fechaEmi
	 *            fecha de emisi??n
	 * @param paymentMedium
	 *            medio de pago
	 * @return fecha de pago del cheque
	 */
	public Date getFechaPagoCheque(Date fechaEmi, MPOSPaymentMedium paymentMedium){
		Calendar dueCalendar = Calendar.getInstance();
    	dueCalendar.setTimeInMillis(fechaEmi.getTime());
    	String checkDeadLine = paymentMedium.getCheckDeadLine();
    	int days = 0;
    	if(checkDeadLine.equals(MPOSPaymentMedium.CHECKDEADLINE_30)){
    		days = 30;
    	}
    	else if(checkDeadLine.equals(MPOSPaymentMedium.CHECKDEADLINE_60)){
    		days = 60;
    	}
    	else if(checkDeadLine.equals(MPOSPaymentMedium.CHECKDEADLINE_90)){
    		days = 90;
    	}
    	dueCalendar.add(Calendar.DATE, days);
    	return dueCalendar.getTime();
	}

	/**
	 * @return el total de descuento/recargo de esquema de vencimientos. Si es
	 *         negativo es recargo, positivo descuento
	 */
	public BigDecimal getTotalPaymentTermDiscount(){
		return reciboDeCliente.getGlobalInvoice().getTotalPaymentTermDiscount();
	}

	/**
	 * @return value del proceso de impresi??n de facturas
	 */
	protected String getInvoiceReportValue(){
		return "Factura (Impresion)";
	}
	
	@Override
	public void printCustomDocuments(ASyncProcess asyncProcess){
		if(customInvoices == null || customInvoices.size() == 0)return;
		// Traer el id del proceso que se encarga de imprimir los comprobantes
		// facturas
		int defaultProcessID = DB.getSQLValue( null, "SELECT AD_Process_ID FROM AD_Process WHERE value='" + getInvoiceReportValue()+ "' " );
		if(defaultProcessID <= 0)return;
		int tableID = DB.getSQLValue( null, "SELECT ad_table_id FROM AD_Table WHERE tablename = 'C_Invoice'" );
		Integer processID = defaultProcessID;
		MDocType docType;
		ProcessInfo pi;
		MPInstance instance;
		// Imprimir los d??bitos y cr??ditos creados
		for (MInvoice invoice : customInvoices) {
			// Imprimir la factura actual
			// Si necesita impresi??n fiscal significa que no se complet??
			// anteriormente la factura, por lo que se debe completar y as??
			// imprimir
			if (!needFiscalPrint(invoice)) {
	        	docType = new MDocType(m_ctx, invoice.getC_DocTypeTarget_ID(), null);
				processID = Util.isEmpty(docType.getAD_Process_ID(), true) ? defaultProcessID
						: docType.getAD_Process_ID();
				// Crear la instancia del proceso
	        	instance = new MPInstance(Env.getCtx(), processID, 0, null);
	            if( !instance.save()) {
	            	log.log(Level.SEVERE, "Error at mostrarInforme: instance.save()");
	                return;
	            }
	            // Crear el processinfo
	            pi = new ProcessInfo( "Factura",processID );
	            pi.setAD_PInstance_ID( instance.getAD_PInstance_ID());
	            pi.setRecord_ID(invoice.getID());
	            pi.setAD_User_ID(instance.getAD_User_ID());
	            pi.setTable_ID(tableID);
	           
	            ProcessCtl worker = new ProcessCtl( asyncProcess, pi, null );
	            worker.start();
			}
		}
	}

	@Override
	public void doPostProcesarNormalCustom() throws Exception{
		// Completar los documentos custom realizados en el caso que requieran
		// impresi??n fiscal
		for (MInvoice invoice : customInvoices) {
			if (needFiscalPrint(invoice)) {
				// Completar la factura
				processDocument(invoice, MInvoice.DOCACTION_Complete);
			}
		}
	}
	
	public void setAssumeGeneralDiscountAdded(boolean value) {
		reciboDeCliente.setAssumeGeneralDiscountAdded(value);
	}
	
	/**
	 * Subclasificaci??n del tablemodel de ordenes de pago para adaptar nueva
	 * l??gica de descuentos/recargos de esquemas de vencimiento
	 * 
	 * @author Equipo de Desarrollo Libertya - Mat??as Cap
	 */
	protected class OpenInvoicesCustomerReceiptsTableModel extends OpenInvoicesTableModel {
		private boolean allowManualAmtEditable = true;
		public OpenInvoicesCustomerReceiptsTableModel() {
			super();
			
    		columnNames = new Vector<String>();

            columnNames.add( "#$#" + Msg.getElement( Env.getCtx(),"C_Invoice_ID" ));
            columnNames.add( "#$#" + Msg.getElement( Env.getCtx(),"C_InvoicePaySchedule_ID" ));
            columnNames.add( Msg.translate( Env.getCtx(),"AD_Org_ID" ));
            columnNames.add( Msg.getElement( Env.getCtx(),"DocumentNo" ));
            columnNames.add( Msg.getElement( Env.getCtx(),"DateInvoiced" ));
            columnNames.add( Msg.translate( Env.getCtx(),"DueDate" ));
            columnNames.add( Msg.translate( Env.getCtx(),"GrandTotal" ));
            columnNames.add( Msg.translate( Env.getCtx(),"openAmt" ));
            columnNames.add( Msg.translate( Env.getCtx(),"DiscountSurchargeDue" ));
            columnNames.add( Msg.translate( Env.getCtx(),"ToPay" ));
		}
		
		public int getOpenAmtColIdx() {
			return 7;
		}
		
		public int getIdColIdx() {
			return 0;
		}
		
		public int getInvoicePayScheduleColIdx() {
			return 1;
		}
		
		public int getDueDateColIdx() {
			return 5;
		}
		
		public int getDateInvoicedColIdx() {
			return 4;
		}
		
		@Override
		public boolean isCellEditable(int row, int column) {
			if (column < getColumnCount() - 2)
				return super.isCellEditable(row, column);
			
			boolean editable = false;
			// Si es toPay
			if (column == getColumnCount() - 1 && isAllowManualAmtEditable()) {
				editable = true;
			}
			return editable;
		}
		
		@Override
		public Object getValueAt(int row, int column) {
			if (column < getColumnCount() - 2)
				return super.getValueAt(row, column);
			
			BigDecimal value = null;
			// Si es toPay
			if(column == getColumnCount()-1){
				value = ((ResultItemFactura) item.get(row)).getManualAmount();
			}
			// HACK: Para descuento/recargo de esquema de vencimientos
			else if(column == columnNames.size()-2){
				value = ((ResultItemFactura) item.get(row)).getPaymentTermDiscount();
				if(value.compareTo(BigDecimal.ZERO) != 0){
					value = value.negate();
				}
			}
			
			return value; 
		}
		
		@Override
		public void setValueAt(Object arg0, int row, int column) {
			if (column < getColumnCount() - 2)
				super.setValueAt(arg0, row, column);
			
			// Si es toPay
			if(column == getColumnCount()-1){
				((ResultItemFactura) item.get(row)).setManualAmount((BigDecimal)arg0);				
			}
			// HACK: Para descuento/recargo de esquema de vencimientos
			else if(column == columnNames.size()-2){
				((ResultItemFactura) item.get(row)).setPaymentTermDiscount((BigDecimal)arg0);
			}
			
			fireTableCellUpdated(row, column);
		}
		
		@Override
		public Class<?> getColumnClass(int columnIndex) {
			if (columnIndex != getColumnCount() - 1)
				return super.getColumnClass(columnIndex);
			
			return BigDecimal.class;
		}

		public void setAllowManualAmtEditable(boolean allowManualAmtEditable) {
			this.allowManualAmtEditable = allowManualAmtEditable;
		}

		public boolean isAllowManualAmtEditable() {
			return allowManualAmtEditable;
		}
	}

	protected String getAllocTypes()
	{
		return
			"(" +
			"'" + X_C_AllocationHdr.ALLOCATIONTYPE_CustomerReceipt + "'," + 
			"'" + X_C_AllocationHdr.ALLOCATIONTYPE_AdvancedCustomerReceipt + "'," +
			"'" + X_C_AllocationHdr.ALLOCATIONTYPE_SalesTransaction + "'" +
			")";
	}
	
}

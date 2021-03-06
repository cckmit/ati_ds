package org.openXpertya.pos.ctrl;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;

import org.openXpertya.apps.ProcessCtl;
import org.openXpertya.apps.form.VModelHelper;
import org.openXpertya.cc.CurrentAccountBalanceStrategy;
import org.openXpertya.cc.CurrentAccountManager;
import org.openXpertya.cc.CurrentAccountManagerFactory;
import org.openXpertya.model.CalloutInvoiceExt;
import org.openXpertya.model.DiscountCalculator;
import org.openXpertya.model.FiscalDocumentPrint;
import org.openXpertya.model.MAllocationHdr;
import org.openXpertya.model.MAllocationLine;
import org.openXpertya.model.MAttributeSet;
import org.openXpertya.model.MBPartner;
import org.openXpertya.model.MBPartnerLocation;
import org.openXpertya.model.MCash;
import org.openXpertya.model.MCashLine;
import org.openXpertya.model.MCategoriaIva;
import org.openXpertya.model.MConversionRate;
import org.openXpertya.model.MDocType;
import org.openXpertya.model.MEntidadFinanciera;
import org.openXpertya.model.MEntidadFinancieraPlan;
import org.openXpertya.model.MInOut;
import org.openXpertya.model.MInOutLine;
import org.openXpertya.model.MInvoice;
import org.openXpertya.model.MInvoiceLine;
import org.openXpertya.model.MLetraComprobante;
import org.openXpertya.model.MLocation;
import org.openXpertya.model.MOrder;
import org.openXpertya.model.MOrderLine;
import org.openXpertya.model.MOrg;
import org.openXpertya.model.MOrgInfo;
import org.openXpertya.model.MPOS;
import org.openXpertya.model.MPOSJournal;
import org.openXpertya.model.MPOSPaymentMedium;
import org.openXpertya.model.MPayment;
import org.openXpertya.model.MPriceList;
import org.openXpertya.model.MPriceListVersion;
import org.openXpertya.model.MProduct;
import org.openXpertya.model.MProductPO;
import org.openXpertya.model.MProductPrice;
import org.openXpertya.model.MQuery;
import org.openXpertya.model.MRefList;
import org.openXpertya.model.MRole;
import org.openXpertya.model.MStorage;
import org.openXpertya.model.MTax;
import org.openXpertya.model.MUser;
import org.openXpertya.model.M_Tab;
import org.openXpertya.model.PO;
import org.openXpertya.model.PrintInfo;
import org.openXpertya.pos.exceptions.FiscalPrintException;
import org.openXpertya.pos.exceptions.InsufficientBalanceException;
import org.openXpertya.pos.exceptions.InsufficientCreditException;
import org.openXpertya.pos.exceptions.InvalidOrderException;
import org.openXpertya.pos.exceptions.InvalidPaymentException;
import org.openXpertya.pos.exceptions.InvalidProductException;
import org.openXpertya.pos.exceptions.InvoiceCreateException;
import org.openXpertya.pos.exceptions.PosException;
import org.openXpertya.pos.exceptions.UserException;
import org.openXpertya.pos.model.BankTransferPayment;
import org.openXpertya.pos.model.BusinessPartner;
import org.openXpertya.pos.model.CashPayment;
import org.openXpertya.pos.model.CheckPayment;
import org.openXpertya.pos.model.CreditCardPayment;
import org.openXpertya.pos.model.CreditNotePayment;
import org.openXpertya.pos.model.CreditPayment;
import org.openXpertya.pos.model.DiscountSchema;
import org.openXpertya.pos.model.EntidadFinanciera;
import org.openXpertya.pos.model.EntidadFinancieraPlan;
import org.openXpertya.pos.model.Location;
import org.openXpertya.pos.model.Order;
import org.openXpertya.pos.model.OrderProduct;
import org.openXpertya.pos.model.Payment;
import org.openXpertya.pos.model.PaymentMedium;
import org.openXpertya.pos.model.PaymentTerm;
import org.openXpertya.pos.model.PriceList;
import org.openXpertya.pos.model.PriceListVersion;
import org.openXpertya.pos.model.Product;
import org.openXpertya.pos.model.ProductList;
import org.openXpertya.pos.model.Tax;
import org.openXpertya.pos.model.User;
import org.openXpertya.print.MPrintFormat;
import org.openXpertya.print.ReportEngine;
import org.openXpertya.print.View;
import org.openXpertya.process.DocAction;
import org.openXpertya.process.DocumentEngine;
import org.openXpertya.process.InvoiceGlobalVoiding;
import org.openXpertya.process.ProcessInfo;
import org.openXpertya.reflection.CallResult;
import org.openXpertya.util.AccumulableTask;
import org.openXpertya.util.CLogger;
import org.openXpertya.util.DB;
import org.openXpertya.util.Env;
import org.openXpertya.util.MProductCache;
import org.openXpertya.util.MeasurableTask;
import org.openXpertya.util.Msg;
import org.openXpertya.util.TimeStatsAccumulator;
import org.openXpertya.util.TimeStatsLogger;
import org.openXpertya.util.TimeUtil;
import org.openXpertya.util.Trx;
import org.openXpertya.util.Util;
import org.openXpertya.util.ValueNamePair;

public class PoSOnline extends PoSConnectionState {

	private final boolean LOCAL_AR_ACTIVE = CalloutInvoiceExt.ComprobantesFiscalesActivos();
	
	// ID de pesta??as utilizadas para obtener los reportes de impresi??n.
	private final int ORDER_TAB_ID = 186;
	private final int INVOICE_TAB_ID = 263;
	
	private Properties ctx = Env.getCtx();
	private String trxName = null;
	//private Trx trx = null;
	private MBPartner partner = null;
	private Timestamp invoiceDate = null;
	//private int C_Currency_ID ;
	private MOrder morder = null;
	private MInvoice invoice = null;
	private MInOut shipment = null;
	private MAllocationHdr allocHdr = null;
	
	private HashMap<Integer, MPayment> mpayments = new HashMap<Integer, MPayment>();
	// private Vector<MPayment> mpayments = new Vector<MPayment>();
	private Vector<MAllocationLine> allocLines = new Vector<MAllocationLine>();
	
	private BigDecimal sumaPagos = null;
	private BigDecimal sumaProductos = null;
	
	private BigDecimal sumaCashPayments = null;
	private BigDecimal sumaCheckPayments = null;
	private BigDecimal sumaCreditCardPayments = null;
	private BigDecimal sumaCreditPayments = null;
	private BigDecimal sumaCreditNotePayments = null;
	private BigDecimal sumaBankTransferPayments = null;
	
	private Vector<CashPayment> cashPayments = new Vector<CashPayment>();
	private Vector<CheckPayment> checkPayments = new Vector<CheckPayment>();
	private Vector<CreditCardPayment> creditCardPayments = new Vector<CreditCardPayment>();
	private Vector<CreditPayment> creditPayments = new Vector<CreditPayment>();
	private Vector<CreditNotePayment> creditNotePayments = new Vector<CreditNotePayment>();
	private Vector<BankTransferPayment> bankTransferPayments = new Vector<BankTransferPayment>();
	
	private BigDecimal sobraPorCheques = null;
	private BigDecimal faltantePorRedondeo = null;
	
	private boolean shouldCreateInvoice;
	private boolean shouldCreateInout;
	private boolean shouldUpdateBPBalance;
	
	private Map<Integer, MCashLine> mCashLines = new HashMap<Integer, MCashLine>();
	private Map<PO, Object> aditionalWorkResults = new HashMap<PO, Object>();
	
	private Map<Integer, PaymentTerm> paymentTerms = new HashMap<Integer, PaymentTerm>();
	
	public PoSOnline() {
		super();
	}
	
	private static void throwIfFalse(boolean b, DocAction sourceDocActionPO, Class posExceptionClass) throws PosException {
		if (!b) 
		{
			ValueNamePair np = CLogger.retrieveError();
			String msg = null;
			// Se intenta obtener el mensaje a partir del Logger.
			if (np != null) {

				String name = (np.getName() != null) ? Msg.translate(Env.getCtx(), np.getName()) : "";
				String value = (np.getValue() != null) ? Msg.translate(Env.getCtx(), np.getValue()) : "";
				if (name.length() > 0 && value.length() > 0)
					msg = value + ": " + name;
				else if (name.length() > 0)
					msg = name;
				else if (value.length() > 0)
					msg = value;
				else
					msg = "";
			
			// Se intenta obtener un mensaje a partir del mensaje de los POs que implementan DocAction.
			} else if (sourceDocActionPO != null && sourceDocActionPO.getProcessMsg() != null &&
					 sourceDocActionPO.getProcessMsg().length() > 0) {
			
				msg = Msg.parseTranslation(Env.getCtx(), sourceDocActionPO.getProcessMsg());
			
			}
		
			PosException e;
			try {
				e =(PosException)posExceptionClass.newInstance();
			} catch (Exception e2) {
				e2.printStackTrace();
				e = new PosException(); 
			}
			if (msg != null)
				e.setMessage(msg);
			
			throw e;
		}
	}
	
	private static void throwIfFalse(boolean b) throws PosException {
		throwIfFalse(b, null, PosException.class);
	}
	
	private static void throwIfFalse(boolean b, Class posExceptionClass) throws PosException {
		throwIfFalse(b, null, posExceptionClass);
	}

	private static void throwIfFalse(boolean b, DocAction sourceDocActionPO) throws PosException {
		throwIfFalse(b, sourceDocActionPO, PosException.class);
	}
	
	/**
	 * El metodo completeOrder:
	 * 
	 * <ol>
	 * <li>Valida que el saldo sea cero (si no lo es, dispara una InsufficientBalanceException). 
	 * 
	 * <li>Valida si tiene credito (si no lo tiene, dispara una InsufficientCreditException).  
	 * 
	 * <li>Crea el pedido (El pedido es un MOrder). 
	 * 
	 * <li>Crea la factura (MInvoice). 
	 * 
	 * <li>Crea el albar??n (MInOut, solo si est?? configurado para tal fin). 
	 * 
	 * <li>Crea los pagos (MPayment, MCashLine). 
	 * 
	 * <li>Crea el allocation (MAllocationHdr). 
	 * </ol>
	 * 
	 * @param order la orden que se desea completar
	 * @throws PosException
	 * @throws InsufficientCreditException
	 */
	@Override
	public void completeOrder(Order order) throws PosException, InsufficientCreditException, InsufficientBalanceException, InvalidPaymentException, InvalidProductException {
		Trx trx = null; // LOCAL. Solo para hacer rollback o commit
		try {
		
			trxName = createTrxName();
			trx = Trx.get(trxName, true);
			//ADER, para que aparezca en el log de postgres
			//DB.getSQLObject(getTrxName(), "select 'Comenzando completeOrder'", null);		
	
			//ADER, iniciazliacion de caches
			initCachesFromOrder(order);
			
			// clearState(order);
			debug("Chequeando saldo y cr??dito");
			checkSaldo(order);
			
			// Se controla el cr??dito s??lo si hay que crear factura
			if(getShouldCreateInvoice()){
				checkCredit(order);
			}
			
			// MOrder
			debug("Creando Pedido (MOrder)");
			morder = createOxpOrder(order);
			
			// MInvoice 
			
			if (getShouldCreateInvoice()) {
				debug("Creando Factura (MInvoice)");
				invoice = createOxpInvoice(order);
				
				invoice.addDocActionStatusListener(getDocActionStatusListener());
				debug("Chequeando Factura");
				checkInvoice();
			}
			
			debug("Guardando los descuentos");
			// Guarda los descuentos
			saveDiscounts(order);
			
			// TODO: Crear TICKET ?
			
			// MInOut: Albar??n, Remito.
			
			if (getShouldCreateInout()) {
				debug("Creando Remito (MInOut)");
				shipment = createOxpInOut(order); 
			}
			
			// Se crea el allocation y los pagos en el caso que se cree la
			// factura sino no tiene sentido
			
			if (getShouldCreateInvoice()) {
				// Allocation Header
				debug("Creando Allocation");
				allocHdr = createOxpAllocation();
				
				adjustPayments(order);
				
				// Crear los MPayments & MAllocationLine, o MCashLine
				debug("Creando los pagos (MPayment & MCashLine)");
				createOxpPayments(order);
				debug("Completando el allocation");
				doCompleteAllocation();
			}

			// Realizar las tareas de cuenta corriente antes de finalizar
			if (shouldUpdateBPBalance) {
				debug("Acciones de cuenta corriente");
				performAditionalCurrentAccountWork(order);
			}
//
//			FB - Comentado a partir del quitado de la impresi??n fiscal de 
//			la transacci??n principal
//			
//			/*
//			 * IMPORTANTE: Completado de Factura.
//			 * Aqu?? se completa la factura solo si debe ser emitida mediante un controlador
//			 * fiscal. Esta operaci??n debe ser la ??ltima del conjunto dado que una vez
//			 * emitido el comrpobante impreso, la transacci??n debe confirmarse para que
//			 * los datos queden consistentes. (papel impreso con factura creada en la BD).
//			 * Si luego de esta operaci??n se agrega otra (por ejemplo la creaci??n de otro
//			 * documento) y esto falla, la transacci??n se anular??a dejando como resultado
//			 * un comprobante impreso sin su factura correspondiente en la base de datos.
//			 */
//			if (invoice != null && needFiscalPrint(invoice)){
//				debug("Completando la factura para Impresi??n Fiscal");
//				throwIfFalse(invoice.processIt(DocAction.ACTION_Complete), invoice, InvoiceCreateException.class);
//			}
			
			// Actualizar el cr??dito de la entidad comercial
			if(shouldUpdateBPBalance){
				debug("Actualizando cr??dito de la Entidad Comercial");
				afterProcessDocuments(order);
			}
			//ADER, para que aparezca en el log de postgres
			DB.getSQLObject(getTrxName(), "select 'Finalizando completeOrder'", null);		
			
			debug("Commit de Transaccion");
			throwIfFalse(trx.commit());
			TimeStatsLogger.endTask(MeasurableTask.POS_SAVE_DOCUMENTS);
			
			trxName = null;
		} catch (PosException e) {
			/*
			try {
				if (morder != null && morder.getID() > 0) {
					morder.processIt(DocAction.ACTION_Void);
					morder.save();
				}
			} catch (Exception e2) {}

			try {
				if (invoice != null && invoice.getID() > 0) {
					morder.processIt(DocAction.ACTION_Void);
					morder.save();
				}
			} catch (Exception e2) {}
			
			try {
				if (shipment != null && shipment.getID() > 0) {
					shipment.processIt(DocAction.ACTION_Void);
					shipment.save();
				}
			} catch (Exception e2) {}
			*/
			try {
				trx.rollback();
			} catch (Exception e2) {
				
			}
			
			throw e;
		} catch (Exception e) {
			try {
				trx.rollback();
			} catch (Exception e2) {}
			throw new PosException(e);
		} finally {
			
			try {
				trx.close();
			} catch (Exception e2) {
				
			}
		}
		
		debug("Impresion de venta");
		
		// Aqu?? estamos fuera de la transacci??n. Ahora s?? emitimos la factura
		// por el controlador fiscal en caso de ser necesario.
		if (getShouldCreateInvoice() && invoice.requireFiscalPrint()) {
			debug("Imprimiendo ticket fiscal");
			// Recargamos la factura con TRX NULL. Si usamos la MInvoice con un
			// nombre de transacci??n entonces obtendr??amos los mismos bloqueos que
			// cuando la emisi??n fiscal se hac??a dentro de la transacci??n principal.
			MInvoice tmpInvoice = new MInvoice(getCtx(), invoice.getC_Invoice_ID(), null);
			tmpInvoice.addDocActionStatusListener(getDocActionStatusListener());
			// Lanza la impresi??n fiscal
			String errorMsg = tmpInvoice.doFiscalPrint();
			if (errorMsg != null) {
				throw new FiscalPrintException();
			}
		}
		
		try {
			// Impresi??n del ticket convencional (solo si no fue emitido por
			// controlador fiscal)
			printTicket();
			// Impresi??n del documento de art??culos a retirar por almac??n
			printWarehouseDeliveryTicket(order);
		} catch (Exception e) {
			
		}
	}
	
	private boolean getShouldCreateInvoice() {
		return shouldCreateInvoice;
	}
	
	private boolean getShouldCreateInout() {
		return shouldCreateInout;
	}
	
	/**
	 * Este m??todo ajusta los pagos en efectivo, rest??ndole lo que se le di?? de vuelto 
	 * al cliente. 
	 * 
	 * Eso es, si el cliente tiene que pagar una orden por 85 y paga con 100, el pago 
	 * en efectivo llega en 100 y ac?? se ajusta a 85, quit??ndole los 15 que se le 
	 * devolvieron. 
	 * 
	 * Solo para pagos en Efectivo.
	 * 
	 * @param order
	 */
	private void adjustPayments(Order order) {
		
		BigDecimal change = order.getChangeAmount();
		
		for (int i = 0; i<cashPayments.size(); i++) {
			CashPayment p = cashPayments.get(i);
			BigDecimal amt = currencyConvert(p.getAmount(), p.getCurrencyId());
			
			if (amt.compareTo(change) > 0) 
			{
				amt = amt.subtract(change);
				p.setAmount(currencyConvert(amt, getPoSCOnfig().getCurrencyID(), p.getCurrencyId()));
				change = BigDecimal.ZERO;
			} 
			else 
			{
				change = change.subtract(amt);
				cashPayments.remove(i);
				--i;
			}
		}
		
	}
	
	public boolean balanceValidate(Order order) {
		try {
			checkSaldo(order);
			return true;
		} catch (PosException e) {
			return false;
		}
	}
	
	private String getTrxName() {
		//return trx != null ? trx.getTrxName() : null;
		return trxName;
	}
	
	private void clearState(Order order) {
		
		ctx = Env.getCtx();
		invoiceDate = new Timestamp(System.currentTimeMillis());
		
//		if (trx != null) {
//			try {
//				trx.close();
//			} catch (Exception e) {}
//			trx = null;
//		}
//		
//		trx = Trx.get(this.toString() + invoiceDate.toString() + Thread.currentThread().getId(), true);
//		trx.start();
		
		if (order.getBusinessPartner() != null)
			partner = new MBPartner(ctx, order.getBusinessPartner().getId(), getTrxName());
		else
			partner = null;
		
		
		boolean isPoSOrder = (MDocType.get(ctx, getPoSCOnfig().getOrderDocTypeID()).getDocSubTypeSO().equals(MDocType.DOCSUBTYPESO_POSOrder));
		// Al completar el TicketTPV se crean la factura y el remito, con lo cual en este caso
		// el TPV NO debe crear ninguno de estos documentos.
		shouldCreateInvoice = getPoSCOnfig().isCreateInvoice() && !isPoSOrder;
		shouldCreateInout = getPoSCOnfig().isCreateInOut() &&  !isPoSOrder;
		shouldUpdateBPBalance = false;
		
		morder = null;
		invoice = null;
		shipment = null;
		allocHdr = null;
		
		mpayments.clear();
		allocLines.clear();
		mCashLines.clear();
		
		aditionalWorkResults.clear();
		
		sobraPorCheques = null;
		faltantePorRedondeo = null;
		
		sumaPagos = BigDecimal.ZERO;
		sumaProductos = BigDecimal.ZERO;
		
		sumaCashPayments = BigDecimal.ZERO;
		sumaCheckPayments = BigDecimal.ZERO;
		sumaCreditCardPayments = BigDecimal.ZERO;
		sumaCreditPayments = BigDecimal.ZERO;
		sumaCreditNotePayments = BigDecimal.ZERO;
		sumaBankTransferPayments = BigDecimal.ZERO;
		
		cashPayments.clear();
		checkPayments.clear();
		creditCardPayments.clear();
		creditPayments.clear();
		creditNotePayments.clear();
		bankTransferPayments.clear();
		
		
		for (Payment p : order.getPayments()) {
			BigDecimal amount = currencyConvert(p.getAmount(), p.getCurrencyId());
			
			if (p.isCashPayment()) {
				sumaCashPayments = sumaCashPayments.add(amount);
				cashPayments.add((CashPayment)p);
			} else if (p.isCheckPayment()) {
				sumaCheckPayments = sumaCheckPayments.add(amount);
				checkPayments.add((CheckPayment)p);
			} else if (p.isCreditCardPayment()) {
				sumaCreditCardPayments = sumaCreditCardPayments.add(amount);
				creditCardPayments.add((CreditCardPayment)p);
			} else if (p.isCreditPayment()) {
				sumaCreditPayments = sumaCreditPayments.add(amount);
				creditPayments.add((CreditPayment)p);
			} else if (p.isCreditNotePayment()) {
				sumaCreditNotePayments = sumaCreditNotePayments.add(amount);
				creditNotePayments.add((CreditNotePayment)p);
			} else if (p.isBankTransferPayment()) {
				sumaBankTransferPayments = sumaBankTransferPayments.add(amount);
				bankTransferPayments.add((BankTransferPayment)p);
			}
		}
		
	}
	
	private boolean VerificarSaldo(BigDecimal sumaProductos, BigDecimal sumaPagos) {

		if (sumaPagos.compareTo(sumaProductos) >= 0)
			return true;
		
		BigDecimal diff = sumaPagos.subtract(sumaProductos).abs();
		
		if (diff.compareTo(VModelHelper.GetRedondeoMoneda(ctx, getClientCurrencyID() )) < 0)
			return true;
		
		return false;
		
	}
	
	public BigDecimal currencyConvert(BigDecimal amount, int fromCurrency) { 
		return currencyConvert(amount, fromCurrency, getPoSCOnfig().getCurrencyID());
	}
	
	private BigDecimal currencyConvert(BigDecimal amount, int fromCurrency, int toCurrency) {
		return VModelHelper.currencyConvert(amount, fromCurrency, toCurrency, invoiceDate);
	}
	
	private void checkInvoice() throws PosException {

		BigDecimal totalPagar = invoice.getGrandTotal().subtract(sumaCreditPayments);
		BigDecimal sumaPagos = 
			sumaCashPayments.
			add(sumaCheckPayments).
			add(sumaCreditCardPayments).
			add(sumaCreditNotePayments).
			add(sumaBankTransferPayments);
		
		BigDecimal redondeo = VModelHelper.GetRedondeoMoneda(ctx, getClientCurrencyID());

		// Diff es la cantidad exacta que FALTA PAGAR para que la diferencia sea PRECISAMENTE CERO. 
		BigDecimal diff = totalPagar.subtract(sumaPagos);

		// Hay una diferencia menor o igual al redodeo est??ndar ? 
		
		if (diff.abs().compareTo(redondeo) <= 0) 
		{
			faltantePorRedondeo = diff;
			sumaPagos.add(diff);
		}
		else if (diff.compareTo(redondeo) >= 0) 
		{
			// Falta dinero 
			throw new InsufficientBalanceException();
		}
		
		// En este punto sumaPagos es >= totalPagar
		
		// Sobra dinero ?
		
		if (sumaPagos.compareTo(totalPagar) > 0) 
		{
			sobraPorCheques = sumaPagos.subtract(totalPagar);
		}
	}
	
	/**
	 * 
	 * Verifica que los pagos est??n bien hechos; que no falte ni sobre dinero. 
	 * 
	 * @param order
	 * @throws PosException
	 */
	private void checkSaldo(Order order) throws PosException {
		
		BigDecimal cashChange = BigDecimal.ZERO;
		boolean invalidPayment = false;
		
		order.setChangeAmount(cashChange);
		
		clearState(order);
		
		sumaProductos = BigDecimal.ZERO;
		sumaPagos = BigDecimal.ZERO;
		
		// Si no hay pagos, no deja procesar.
		
		if (order.getPayments().size() == 0)
			throw new InvalidPaymentException();
		
		// Si no hay pedidos, no deja procesar.
		
		if (order.getOrderProducts().size() == 0)
			throw new InvalidProductException();
		
		MPriceList priceList = new MPriceList(ctx, getPoSCOnfig().getPriceListID(), null);
		int priceListCurrencyID = priceList.getC_Currency_ID();
		
		// Suma productos se calcula seg??n el algoritmo del Order, que tiene en
		// cuenta los redondeos en el c??lculo general de impuestos
		sumaProductos = currencyConvert(order.getTotalAmount(), priceListCurrencyID);
		
		for (Payment p : order.getPayments()) {
			int fromCurrency = p.getCurrencyId();
			
			BigDecimal amt = currencyConvert(p.getAmount(), fromCurrency);
			
			sumaPagos = sumaPagos.add( amt );
		}
		
		// Scalado de importes finales. Si hay descuento, puede suceder que
		// sumaProductos tenga una escala mayor a la utilizada por la moneda, y
		// la comparaci??n puede fallar err??neamente
		int stdScale = priceList.getStandardPrecision();
		sumaProductos = sumaProductos.setScale(stdScale, BigDecimal.ROUND_HALF_UP);
		sumaPagos = sumaPagos.setScale(stdScale, BigDecimal.ROUND_HALF_UP);
		
		// Si no alcanzan los pagos para pagar los productos, no deja procesar.
		
		if (!VerificarSaldo(sumaProductos, sumaPagos))
			throw new InsufficientBalanceException();
		
		// Si hay algun pago que no sea cheque, y est??n pagando de m??s, no permito que se efectue la operacion
		
		BigDecimal redondeo = VModelHelper.GetRedondeoMoneda(ctx, getClientCurrencyID());
		boolean sobraPlata = sumaPagos.subtract(sumaProductos).compareTo(redondeo) >= 0;  
		
		
		if (sobraPlata) { // order.getOrderProducts().size() != checkPayments.size()) {
			
			// if (sobraPlata)
			//	throw new InvalidPaymentException();
			
			BigDecimal x = 
				(sumaCreditPayments).
				add(sumaCreditCardPayments).
				add(sumaCreditNotePayments).
				add(sumaBankTransferPayments);
			
			// si x > sumaProductos, sobra plata -> ERROR
			
			if (x.subtract(sumaProductos).compareTo(redondeo) > 0)
				invalidPayment = true;
		
			x = x.add(sumaCheckPayments);
			
			// si x > sumaProductos , sobra por cheque -> OK
			
			if (x.subtract(sumaProductos).compareTo(redondeo) > 0)
			{
				// Sobra plata -> Todo el efectivo se manda de vuelta
				
				cashChange = sumaCashPayments;
			}
			else
			{
				// Falta plata -> de lo que tengo en efectivo, saco lo que me falta pagar
				
				cashChange = sumaCashPayments.subtract(sumaProductos.subtract(x).abs());
			}
		}

		order.setChangeAmount(cashChange);
		
		if (invalidPayment)
			throw new InvalidPaymentException();
	}
	
	public ProductList searchProduct(String code) {
		List<Integer> vendors;
		//ArrayList<Product> productList = new ArrayList<Product>();
		ProductList productList = new ProductList();
		int m_PriceList_ID = getPoSCOnfig().getPriceListID(); 
		// Obtengo la tarifa, y pido la version valida.
		MPriceList mPriceList = new MPriceList(Env.getCtx(),m_PriceList_ID,null);
		int m_PriceList_Version_ID = mPriceList.getPriceListVersion(null).getID();
		Product product = null;
		
		StringBuffer sql = new StringBuffer();
		// Obtiene el id, nombre y el precio en la tarifa del producto.
		sql
		  .append("SELECT DISTINCT ")
		  .append(   "u.M_Product_ID, ")
		  .append(   "bomPriceStd(u.M_Product_ID, M_PriceList_Version_ID, u.M_AttributeSetInstance_ID), ")
		  .append(	 "p.Name, ")
		  .append(   "bomPriceLimit(u.M_Product_ID, M_PriceList_Version_ID, u.M_AttributeSetInstance_ID), ")
		  .append(   "u.M_AttributeSetInstance_ID, ")
		  .append(   "masi.description, ")
		  .append(   "u.MatchType, ")
		  .append(   "s.MandatoryType, ")
		  .append(   "p.m_product_category_id, ")
		  .append(   "p.CheckoutPlace, ")
		  .append(   "p.IsSold ")
		  .append("FROM ( "); 
		
		boolean needUnion = false;  // Indicador de concatenaci??n de UNION a la consulta
		int codeParameterCount = 0; // Cantidad de veces que hay que agregar 'code' como 
		                            // par??metro de la consulta (var??a seg??n la conf del TPV) 
		String query = null;
		// Se agrega la sub-consulta de b??squeda por UPC en caso de que la configuraci??n
		// del TPV as?? lo indique.
		if (getPoSCOnfig().isSearchByUPCConfigured()) {
			query = getSearchByUPCQuery();
			sql.append(query);
			codeParameterCount += parametersCount(query);
			needUnion = true;
		}
		// Se agrega la sub-consulta de b??squeda por Clave de B??squeda en caso de que la 
		// configuraci??n del TPV as?? lo indique.
		if (getPoSCOnfig().isSearchByValueConfigured()) {
			query = getSearchByValueQuery();
			sql.append(needUnion?" UNION ":"");
			sql.append(query);
			codeParameterCount += parametersCount(query);
			needUnion = true;
		}
		// Se agrega la sub-consulta de b??squeda por Nombre en caso de que la configuraci??n
		// del TPV as?? lo indique.
		if (getPoSCOnfig().isSearchByNameConfigured()) {
			query = getSearchByNameQuery();
			sql.append(needUnion?" UNION ":"");
			sql.append(query);
			codeParameterCount += parametersCount(query);
			needUnion = true;
		}
		
		sql
		  .append(") u ")
		  .append("INNER JOIN M_Product p ON (p.M_Product_id = u.M_Product_id) ")
		  .append("INNER JOIN M_ProductPrice pp ON (pp.M_PriceList_Version_ID = ?) ")
		  .append("LEFT JOIN M_AttributeSet s ON (p.M_AttributeSet_ID = s.M_AttributeSet_ID) ") 
		  .append("LEFT JOIN M_AttributeSetInstance masi ON (u.M_AttributeSetInstance_ID = masi.M_AttributeSetInstance_ID) ")
          .append("WHERE u.M_Product_ID = pp.M_Product_ID ")
          .append(  "AND u.M_Product_ID = p.M_Product_ID ")
          .append(  "AND p.IsActive = 'Y' ")
          .append("ORDER BY u.MatchType ASC ");

		try {
			PreparedStatement pstmt = DB.prepareStatement(sql.toString());
			int i = 1;
			// Se carga el c??digo del art??culo como par??metro las veces que la consulta
			// lo requiera
			while(i <= codeParameterCount) {
				pstmt.setString(i++, code);
			}
			pstmt.setInt(i++,m_PriceList_Version_ID);
			
			int prevMasiId = -1;
			
			ResultSet rs = pstmt.executeQuery();
			Map<Integer, Integer> productMatch = new HashMap<Integer, Integer>();
			while(rs.next()) {
				int m_Product_Id = rs.getInt(1);
				int matchType = rs.getInt("MatchType");
				
				// Verifica si el art??culo ya fue agregado a la lista (para
				// evitar agregar dos veces el mismo art??culo con diferente
				// Matching). Si ya est?? agregado, se queda con el matching de
				// mayor prioridad.
				if (productMatch.containsKey(m_Product_Id)
						&& productMatch.get(m_Product_Id) <= matchType) {
					continue;
				}
				
				BigDecimal productPrice = rs.getBigDecimal(2);
				String productName = rs.getString(3);
				BigDecimal productLimitPrice = rs.getBigDecimal(4); 
				
				int M_AttributeSetInstance_ID = rs.getInt(5);
				String masiDescription = rs.getString(6);
				boolean masiMandatory = MAttributeSet.MANDATORYTYPE_AlwaysMandatory.equals(rs.getString("MandatoryType"));
				String checkoutPlace = rs.getString("CheckoutPlace");
				boolean sold = "Y".equals(rs.getString("IsSold"));
				
				// Me quedo solo con el primer Product si el MASI es > 0, 
				// o con todos en el resto de los casos (todos los masi = 0).
				
				if (prevMasiId > 0)
					break;
				
				prevMasiId = M_AttributeSetInstance_ID;
				
				// Creo el producto.
				vendors = getVendors(m_Product_Id);
				product = new Product(
						m_Product_Id, 
						code, 
						productName, 
						productPrice, 
						productLimitPrice, 
						M_AttributeSetInstance_ID, 
						masiDescription, 
						getPoSCOnfig().isPriceListWithTax(), 
						masiMandatory, 
						rs.getInt("m_product_category_id"), 
						vendors,
						checkoutPlace,
						sold);
				
				productList.addProduct(product, matchType);
				productMatch.put(product.getId(), matchType);
			}
			
			rs.close();
			pstmt.close();
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return productList;
	}
	
	/**
	 * Realizar las validaciones de cr??dito de la entidad comercial 
	 * @param order
	 * @throws InsufficientCreditException
	 */
	private void checkCredit(Order order) throws InsufficientCreditException, Exception {
		MBPartner bp = new MBPartner(getCtx(), order.getBusinessPartner().getId(), getTrxName());
		MOrg org = new MOrg(getCtx(), Env.getAD_Org_ID(getCtx()), getTrxName());
		// Obtengo el manager actual
		CurrentAccountManager manager = CurrentAccountManagerFactory
				.getManager();
		// Seteo el estado actual del cliente y lo obtengo
		CallResult result = manager.setCurrentAccountStatus(getCtx(), bp, org,
				null);
		// Si hubo error, obtengo el mensaje y tiro la excepci??n
		if (result.isError()) {
			throw new InsufficientCreditException(result.getMsg());
		}
		// Me guardo el estado de la entidad comercial
		String creditStatus = (String)result.getResult(); 
		// Determino los tipos de pago a verificar el estado de cr??dito
		result = manager.getTenderTypesToControlStatus(getCtx(), org, bp,
				getTrxName());
		// Si hubo error, obtengo el mensaje y tiro la excepci??n
		if (result.isError()) {
			throw new Exception(result.getMsg());
		}
		// Me guardo la lista de tipos de pago a verificar
		Set<String> tenderTypesAllowed = (Set<String>)result.getResult();
		// Armo la lista de payments por tipo de medio de pago con los montos
		// convertidos correspondientes
		Map<String, BigDecimal> pays = new HashMap<String, BigDecimal>();
		BigDecimal convertedPayAmt;
		for (Payment pay : order.getPayments()) {
			// Verificar por el manager de cuentas corrientes si debo verificar
			// el estado de cr??dito en base a los tipos de pago obtenidos
			if (tenderTypesAllowed != null
					&& tenderTypesAllowed.contains(pay.getTenderType())) {
				shouldUpdateBPBalance = true;
				// Verificar la situaci??n de cr??dito de la entidad comercial
				result = manager.validateCurrentAccountStatus(getCtx(),
						creditStatus, getTrxName());
				// Si hubo error, obtengo el mensaje y tiro la excepci??n
				if (result.isError()) {
					throw new InsufficientCreditException(result.getMsg());
				}
			}
			// Convierto el monto del pago a partir de su moneda
			convertedPayAmt = MConversionRate.convertBase(getCtx(), pay
					.getAmount(), pay.getCurrencyId(), order.getDate(), 0, Env
					.getAD_Client_ID(getCtx()), Env.getAD_Org_ID(getCtx()));
			pays.put(CurrentAccountBalanceStrategy.getPaymentRuleEquivalent(pay
					.getTenderType()), convertedPayAmt);
		}		
		// Verificar el cr??dito con la factura
		result = manager.checkInvoicePaymentRulesBalance(getCtx(), bp, org,
				pays, getTrxName());
		// Si hubo error, obtengo el mensaje y tiro la excepci??n
		if (result.isError()) {
			throw new InsufficientCreditException(result.getMsg());
		}
	}

	/**
	 * Realizar tareas adicionales para la gesti??n de cr??dito de clientes
	 * @param order pedido
	 * @throws Exception en caso de error
	 */
	private void performAditionalCurrentAccountWork(Order order) throws Exception{
		MBPartner bp = new MBPartner(getCtx(), order.getBusinessPartner()
				.getId(), getTrxName());
		MOrg org = new MOrg(getCtx(), Env.getAD_Org_ID(getCtx()), getTrxName());
		// Obtengo el manager actual
		CurrentAccountManager manager = CurrentAccountManagerFactory
				.getManager();
		// Realizo las tareas adicionales necesarias
		// Factura
		if(invoice != null){
			performAditionalCurrentAccountWork(org, bp, manager, invoice, true);
		}
		// Payments
		for (MPayment pay : mpayments.values()) {
			performAditionalCurrentAccountWork(org, bp, manager, pay, true);
		}
		// Cashlines
		for (MCashLine cashLine : mCashLines.values()) {
			performAditionalCurrentAccountWork(org, bp, manager, cashLine, true);
		}
	}

	/**
	 * Realiza las tareas adicionales en base a los param??tros.
	 * 
	 * @param org
	 *            organizaci??n actual
	 * @param bp
	 *            entidad comercial de la venta actual
	 * @param manager
	 *            manager de cuentas corrientes actual
	 * @param po
	 *            documento o transacci??n involucrada, puede ser Invoice,
	 *            Payment o Cashline.
	 * @param addToWorkResults
	 *            true si el resultado de estas tareas se debe colocar dentro de
	 *            la map de resultados (variable de instancia
	 *            aditionalWorkResults)
	 * @return el resultado de las tareas adicionales si es que existe
	 * @throws Exception
	 *             si hubo alg??n error dentro de la ejecuci??n de esas tareas
	 */
	private Object performAditionalCurrentAccountWork(MOrg org, MBPartner bp,
			CurrentAccountManager manager, PO po, boolean addToWorkResults) throws Exception {
		// Realizo las tareas adicionales
		CallResult result = manager.performAditionalWork(getCtx(), org, bp, po,
				true, getTrxName());
		// Si es error devuelvo una exception
		if(result.isError()){
			throw new Exception(result.getMsg());
		}
		// Lo agrego a la map si me lo permite el par??metro y tengo un resultado
		if(addToWorkResults && result != null){
			getAditionalWorkResults().put(po, result.getResult());
		}
		return result.getResult();
	}

	/**
	 * Realizar tareas de gesti??n de cr??dito luego de procesar los documentos,
	 * como por ejemplo actualizaci??n del cr??dito de la entidad comercial, etc.
	 * 
	 * @param order
	 *            pedido actual
	 */
	private void afterProcessDocuments(Order order){
		MBPartner bp = new MBPartner(getCtx(), order.getBusinessPartner()
				.getId(), getTrxName());
		MOrg org = new MOrg(getCtx(), Env.getAD_Org_ID(getCtx()), getTrxName());
		// Obtengo el manager actual
		CurrentAccountManager manager = CurrentAccountManagerFactory
				.getManager();
		// Actualizo el cr??dito
		CallResult result = manager.afterProcessDocument(getCtx(), org, bp,
				getAditionalWorkResults(), getTrxName());
		if(result.isError()){
			log.severe(result.getMsg());
		}
	}
	

	private MOrder createOxpOrder(Order order) throws PosException {
		
		
		List<OrderProduct> products = order.getOrderProducts();
		
		MOrder mo = new MOrder(ctx, 0, getTrxName());

		setCaches(mo); //Ader: caches
		
		mo.setIsSOTrx( true );
		// mo.setC_DocTypeTarget_ID();
		
		// TODO: Cual de los dos ?
		
		mo.setC_DocType_ID(getPoSCOnfig().getOrderDocTypeID());
		mo.setC_DocTypeTarget_ID(getPoSCOnfig().getOrderDocTypeID());
		
		mo.setDocAction(MOrder.DOCACTION_Complete);
		mo.setDocStatus(MOrder.DOCSTATUS_Drafted);
		
		mo.setBPartner(partner);
		mo.setC_BPartner_Location_ID(order.getBusinessPartner().getLocationId());
		mo.setAD_User_ID(Env.getAD_User_ID(ctx));
		mo.setM_PriceList_ID(getPoSCOnfig().getPriceListID());
		mo.setM_Warehouse_ID(getPoSCOnfig().getWarehouseID());
		mo.setSalesRep_ID(Env.getContextAsInt(ctx, "#SalesRep_ID"));
        
		// Si el pedido tiene asociado un esquema de vencimientos entonces le
		// seteo ese al original, sino busco el de la config
		int paymentTermID = order.getPaymentTerm() != null ? order
				.getPaymentTerm().getId() : getPoSCOnfig().getPaymentTermID(); 
		mo.setC_PaymentTerm_ID(paymentTermID);
		
		debug("Guardando el Pedido (Encabezado, sin l??neas a??n)");
		throwIfFalse(mo.save(), mo);
		
		// obtener el total lineas a generar 
		int currentProduct = 0;
		int productCount = products.size();
		int numOrderLine = 0; //ADER: mejora de performance (y arreglo de bug), al setearse explicitamente 
		
		for (OrderProduct op : products) {
			currentProduct++;
			
			//MProduct product = new MProduct(ctx, op.getProduct().getId(), getTrxName());
			//Ader : caches; si esto es null bueno....
			MProduct product = getProductFromCache(op.getProduct().getId());
			
			MOrderLine line = new MOrderLine(mo);

			line.setDirectInsert(true);
			line.setProduct( product );
			line.setM_AttributeSetInstance_ID( op.getProduct().getAttributeSetInstanceID() );
	        line.setQty( new BigDecimal(op.getCount()) );
	        line.setPrice(getPoSCOnfig().getPriceListID());    // sets List/limit

	        numOrderLine +=10;
	        line.setLine(numOrderLine); //ADER: mejora de seteo explicito
	        line.setPrice(op.getPrice());
	        line.setPriceList(op.getPriceList());

	        line.setC_Tax_ID(op.getTax().getId());
	        line.setDiscount();
	        line.setLineNetAmt();
	        line.setLineBonusAmt(op.getLineBonusAmt());
	        line.setLineDiscountAmt(op.getLineDiscountAmt());
	        
	        // unicamente la ultima linea actualizar?? el encabezado con informaci??n de impuestos
	        line.setShouldUpdateHeader(currentProduct==productCount);
	        debug("Guardando l??nea #" + currentProduct);
	        throwIfFalse(line.save());
	        
	        op.setOrderLineID(line.getC_OrderLine_ID());
		}
		debug("Guardando el Pedido (Encabezado, con l??neas ya creadas)");
		throwIfFalse(mo.save(), mo);
		
		// Descuentos: leer las l??neas del pedido para ya tenerlas cacheadas y setear 
		// 				el shouldUpdateHeader a false en todas menos la ??ltima
		currentProduct = 0;
		for (MOrderLine orderLine : mo.getLines()) 
		{ 
			currentProduct++;
			orderLine.setShouldUpdateHeader(currentProduct==productCount);
		}
		
		// Crea un calculador de descuentos a partir del calculador de
		// descuentos asociado al pedido de TPV, asociando al nuevo calculador
		// el pedido MOrder creado (wrapper). Luego aplica los descuentos.
		DiscountCalculator discountCalculator = DiscountCalculator.create(mo.getDiscountableWrapper(), order.getDiscountCalculator());
		debug("Aplicando descuentos al Pedido (DiscountCalculator)");
		discountCalculator.applyDiscounts();
		debug("Guardando el Pedido nuevamente (luego de aplicar descuentos)");
		throwIfFalse(mo.save(), mo);
		
		// Reload Order
		
		mo = new MOrder(ctx, mo.getID(), getTrxName());
		setCaches(mo); //caches
		
		// Completar Orden
		debug("Completando el pedido");
		throwIfFalse(mo.processIt(DocAction.ACTION_Complete), mo);
		debug("Guardando el pedido (luego de completar)");
		throwIfFalse(mo.save(), mo);
		
		order.setGeneratedOrderID(mo.getC_Order_ID());
		return mo;
	}
	
	private MInvoice createOxpInvoice(Order order) throws PosException {

		MInvoice inv;
	
		// Se crea una factura de Argentina.
		if (LOCAL_AR_ACTIVE)
			inv = createLocaleInvoice(order);
		// Se crea una factura est??ndar.
		else
			inv = new MInvoice(morder, getPoSCOnfig().getInvoiceDocTypeID(), invoiceDate);
		
		// Se indica que no se debe crear una l??nea de caja al completar la factura ya
		// que es el propio TPV el que se encarga de crear los pagos e imputarlos con
		// la factura (esto soluciona el problema de l??neas de caja duplicadas que 
		// se hab??a detectado).
		inv.setCreateCashLine(false);
		
		// Se skippea la actualizaci??n del descuento manual general de la
		// cabecera para la ventana de facturas 
		inv.setSkipManualGeneralDiscount(true);
		
		inv.setDocAction(MInvoice.DOCACTION_Complete);
		inv.setDocStatus(MInvoice.DOCSTATUS_Drafted);
		
		// Esquema de vencimientos
		inv.setC_PaymentTerm_ID(morder.getC_PaymentTerm_ID());
		
		// Se copia el importe de descuento/recargo
		inv.setChargeAmt(morder.getChargeAmt());
		inv.setC_Charge_ID(morder.getC_Charge_ID());
		
		throwIfFalse(inv.save(), inv, InvoiceCreateException.class);
		
		MOrderLine[] moLines = morder.getLines(true);
		int lineNumber = 10;
		
		// obtener el total lineas a generar 
		int currentLine = 0;
		int lineCount = moLines.length;
		
		TimeStatsAccumulator.getAccumulator(AccumulableTask.NEW_INVOICE_LINE).resetAccumulator();
		for (MOrderLine line : moLines) {
			currentLine++;
			MInvoiceLine invLine = new MInvoiceLine(inv);
			
			invLine.setDirectInsert(true);
			invLine.setOrderLine(line);
			invLine.setQty(line.getQtyOrdered());
			invLine.setLine(lineNumber);
			invLine.setDocumentDiscountAmt(line.getDocumentDiscountAmt());
			invLine.setLineBonusAmt(line.getLineBonusAmt());
			invLine.setLineDiscountAmt(line.getLineDiscountAmt());
			invLine.setC_Project_ID(inv.getC_Project_ID());
			
			// Se asigna el impuesto. Aqu?? se recalcula la tasa a partir de los datos
			// de la factura.
			// IMPORTANTE: la nueva tasa puede variar de la que se calcul?? previamente
			// en el ingreso de productos al pedido de TPV, dado que la entidad comercial
			// puede diferir.
			invLine.setC_Tax_ID(line.getC_Tax_ID());

			// la ultima linea unicamente se encarg?? de setear los impuestos correspondientes
			invLine.setShouldUpdateHeader(currentLine==lineCount);
			
			// Se skippea la actualizaci??n del descuento manual general de la
			// cabecera para la ventana de facturas 
			invLine.setSkipManualGeneralDiscount(true);
			
			debug("Guardando l??nea #" + invLine.getLine());
			throwIfFalse(invLine.save(), InvoiceCreateException.class);
			
			lineNumber += 10;
		}
		// Recargar la factura
		
		inv = new MInvoice(ctx, inv.getID(), getTrxName());
		// Seteo el bypass de la factura para que no chequee el saldo del
		// cliente porque ya lo chequea el tpv
		inv.setCurrentAccountVerified(true);
		// Seteo el bypass para que no actualice el cr??dito del cliente ya
		// que se realiza luego al finalizar las operaciones
		inv.setUpdateBPBalance(false);
		
		// Se skippea la actualizaci??n del descuento manual general de la
		// cabecera para la ventana de facturas 
		inv.setSkipManualGeneralDiscount(true);
		
		//
		// FB - Comentado a partir del quitado de la impresi??n fiscal de 
		// la transacci??n principal
		//
		// Completar Factura. 
		// IMPORTANTE: Si la factura debe ser emitida mediante un controlador fiscal, no
		// se completa aqu?? dado que esto disparar??a la impresi??n fiscal, y luego, en caso
		// de producirse alg??n error por ejemplo al crear los pagos, quedar??a inconsistente
		// la informaci??n emitida con la guardada, dado que en caso de error no se
		// guarda la factura OXP pero si podr??a emitirse el comprobante por la impresora
		// fiscal. El completado y emisi??n de comprobante se deben hacer al final del proceso
		// de creaci??n del pedido.
		//if (!needFiscalPrint(inv)) {
		//	throwIfFalse(inv.processIt(DocAction.ACTION_Complete), inv, InvoiceCreateException.class);
		//}
		
		// Ignora la impresi??n fiscal al completar. Se hace luego fuera de la transacci??n. 
		inv.setIgnoreFiscalPrint(true);
		inv.skipAfterAndBeforeSave = true;
		throwIfFalse(inv.processIt(DocAction.ACTION_Complete), inv, InvoiceCreateException.class);
		throwIfFalse(inv.save(), inv, InvoiceCreateException.class);
		
		order.setGeneratedInvoiceID(inv.getC_Invoice_ID());
		
		return inv;
	}
	
	private MInvoice createLocaleInvoice(Order order) throws PosException {
		Integer categoriaIVAclient = CalloutInvoiceExt.darCategoriaIvaClient();
		Integer categoriaIVACustomer = partner.getC_Categoria_Iva_ID();
		BusinessPartner bPartner = order.getBusinessPartner();
		
		// Se validan las categorias de IVA de la compa??ia y el cliente.
		if (categoriaIVAclient == null || categoriaIVAclient == 0) {
			throw new InvoiceCreateException("ClientWithoutIVAError");
		} else if (categoriaIVACustomer == null || categoriaIVACustomer == 0) {
			throw new InvoiceCreateException("BPartnerWithoutIVAError");
		}
		
		// Se obtiene el ID de la letra del comprobante a partir de las categorias de IVA.
		Integer letraID = CalloutInvoiceExt.darLetraComprobante(categoriaIVACustomer, categoriaIVAclient);
		if (letraID == null || letraID == 0)
			throw new InvoiceCreateException("LetraCalculationError");
		
		// Se obtiene el PO de letra del comprobante.
		MLetraComprobante mLetraComprobante = new MLetraComprobante(ctx, letraID, getTrxName());
		
		// Se obtiene la letra y el nro de punto de venta para determinar el tipo
		// de documento de la factura.
		String letra = mLetraComprobante.getLetra();
		int posNumber = getPoSCOnfig().getPosNumber();
		
		// Se obtiene el tipo de documento para la factura.
		MDocType mDocType = MDocType.getDocType(ctx, MDocType.DOCTYPE_CustomerInvoice, letra, posNumber, getTrxName());
		if (mDocType == null) 
			throw new InvoiceCreateException(Msg.getMsg(ctx, "NonexistentPOSDocType", new Object[] {letra, posNumber}));
		
		MInvoice inv = new MInvoice(morder, mDocType.getC_DocType_ID(), invoiceDate);
		
		// Se asigna la letra de comprobante, punto de venta y n??mero de comprobante
		// a la factura creada.
		inv.setC_Letra_Comprobante_ID(letraID);
		inv.setPuntoDeVenta(posNumber);
		// Nro de comprobante.
		//String documentNo = MSequence.getDocumentNo(mDocType.getID(), getTrxName());
		Integer nroComprobante = CalloutInvoiceExt.getNextNroComprobante(mDocType.getID());
		if (nroComprobante != null)
			inv.setNumeroComprobante(nroComprobante);
		
		// Asignaci??n de CUIT en caso de que se requiera.
		MCategoriaIva mCategoriaIvaCus = new MCategoriaIva(ctx, categoriaIVACustomer, getTrxName());
		String cuit = partner.getTaxID();
		inv.setCUIT(cuit);

		// Se asignan los datos de consumidor final.
		// Nombre, direcci??n e identificaci??n para los casos en que el monto de la factura
		// sea mayor que el permitido a consumidor final.
		if (mCategoriaIvaCus.getCodigo() == MCategoriaIva.CONSUMIDOR_FINAL) {
			inv.setNombreCli(bPartner.getCustomerName());
			inv.setInvoice_Adress(bPartner.getCustomerAddress());
			inv.setNroIdentificCliente(bPartner.getCustomerIdentification());
		}
		
		return inv;
	}
	
	/**
	 * Indica si la factura debe ser emitida mediante un controlador fiscal.
	 * @param invoice Factura a evaluar.
	 */
	private boolean needFiscalPrint(MInvoice invoice) {
		return MDocType.isFiscalDocType(invoice.getC_DocTypeTarget_ID()) && LOCAL_AR_ACTIVE;
	}
	
	private MInOut createOxpInOut(Order order) throws PosException {
		/*
		 * Ampliado y corregido por Franco Bonafine - Disytel - 2010-04-07
		 * 
		 * El remito ahora no se crea con la totalidad de los art??culos que se agregaron
		 * al pedido del TPV  sino que solo se agregan aquellos art??culos cuyo lugar de 
		 * retiro sea el TPV. Los art??culos cuyo retiro sea por almac??n no ser??n 
		 * incluidos en este remito, quedando sus cantidades pendientes en el pedido 
		 * previamente creado.
		 */
		shipment = null;
		// El remito se realiza solo si la configuraci??n del TPV as?? lo indica y adem??s
		// si el pedido de TPV contiene al menos 1 art??culo cuyo retiro sea por TPV (si
		// todos los art??culos del pedido se retiran por almac??n entonces el remito no
		// se debe hacer ya que no contendr??a l??neas).
		if (getPoSCOnfig().isCreateInOut() && order.getPOSCheckoutProductsCount() > 0) { 
			// Crea el encabezado del remito a partir del pedido, y lo guarda
			shipment = new MInOut(morder, 0, morder.getDateOrdered());
			shipment.setDocAction(MInOut.DOCACTION_Complete);
			shipment.setDocStatus(MInOut.DOCSTATUS_Drafted);
			shipment.setTPVInstance(true);
			
			throwIfFalse(shipment.save(), shipment);
			
			//Ader: evitar que se reelean las MOrderLInes; esto evita 20 accesos
			//y otros tantos potencialemten (al evitar acceer a la cache tradcionale
			//de productos
			HashMap<Integer,MOrderLine> mapMo = new HashMap<Integer,MOrderLine>();
			//no deberia ser necesario getLines(true); si deberia, que se resete
			//antes de llamar a este metodo
			MOrderLine[] moLines = morder.getLines();
			for (int i = 0; i < moLines.length; i++)
			{
				MOrderLine moLine = moLines[i];
				mapMo.put(moLine.getC_OrderLine_ID(),moLine);
			}
			
			// FIXME: Ader: notar que lo siguiente rompe la logica BOM...
			
			// Se recorren todos los art??culos del pedido TPV para ir agreg??ndolos
			// al remito. Es necesario iterar sobre esta colecci??n (y no sobre los 
			// MOrderLines del mOrder creado previamente) ya que en la colecci??n
			// se encuentra el lugar de retiro del art??culo, y de este dato depende
			// si el art??culo se agrega o no al remito (a diferencia del MOrder y MInvoice
			// que se crean con todas las l??neas cargadas al TPV).
			for (OrderProduct orderProduct : order.getOrderProducts()) {
				// El remito solo incluye los art??culos que se retiren por el TPV
				// Aquellos art??culos cuyo retiro sea por almac??n quedan fuera de este
				// remito, quedando pendientes de entrega.
				if (orderProduct.isPOSCheckout()) {
					// Obtiene la instancia de la l??nea de pedido creada.
					/*MOrderLine orderLine = new MOrderLine(
							morder.getCtx(), 
							orderProduct.getOrderLineID(), 
							getTrxName());
					*/
					//evitar accesos a DB; de paso, esta MorderLine va a tener
					//la cahe de productos seteada
					MOrderLine orderLine = mapMo.get(orderProduct.getOrderLineID());
					
					// Crea la l??nea del remito a partir de la l??nea del pedido
					MInOutLine shipmentLine = new MInOutLine(shipment); 
					shipmentLine.setDirectInsert(true);
					shipmentLine.setOrderLine(orderLine, 0, orderLine.getQtyEntered());
					shipmentLine.setQty(orderLine.getQtyEntered());
					shipmentLine.setTPVInstance(true);
					// Guarda los cambios.
					debug("Guardando l??nea #" + shipmentLine.getLine());
					throwIfFalse(shipmentLine.save());
				}
			}
			// Completa el remito
			throwIfFalse(shipment.processIt(DocAction.ACTION_Complete), shipment);
			throwIfFalse(shipment.save(), shipment);
		}
		return shipment;
	}
	
	private MPayment createOxpMPayment(String TenderType, int C_Currency_ID, BigDecimal Amt, String documentNo) {
		
		MPayment pay = new MPayment(ctx, 0, getTrxName());
		
		if(documentNo != null){
			pay.setDocumentNo(documentNo);
		}
		pay.setTenderType(TenderType);
		
		pay.setC_DocType_ID(true);
		pay.setC_BPartner_ID(partner.getC_BPartner_ID());
		
		pay.setDateTrx(this.invoiceDate);
		pay.setDateAcct(this.invoiceDate);
		
		pay.setC_BankAccount_ID(getPoSCOnfig().getCheckBankAccountID()); 
		
		pay.setC_Order_ID(morder.getC_Order_ID());
		
		// Esta asignaci??n de RoutingNo y AccountNo es incorrecta!
		// Aqu?? se est?? asignando los datos de la cuenta bancario destino del cobro, mientras
		// que esos campos existen para ingresar la informaci??n relacionada con el ORIGEN
		// del cobro. Con lo cual se est?? mezclando la informaci??n y esto confunde al usuario
		// En la ventana de Cobros estos campos son visibles solo para Cheques y Transferencias
		// con lo cual, se deber??an cargar con la identificaci??n del Banco y la Cuenta origen
		// del Cheque o Transferencia.
		// Por el momento se comentan estas l??nas para no seguir cargando err??neamente
		// esta informaci??n.
		// -->
		//String RoutingNo = VModelHelper.getSQLValueString(null, " select routingno from c_bank inner join c_bankaccount on (c_bank.c_bank_id=c_bankaccount.c_bank_id) where c_bankaccount.c_bankaccount_id = ? ", pay.getC_BankAccount_ID() );
		//String AccountNo = VModelHelper.getSQLValueString(null, " select AccountNo from c_bankaccount where c_bankaccount.c_bankaccount_id = ? ", pay.getC_BankAccount_ID() );

		//pay.setRoutingNo(RoutingNo);
		//pay.setAccountNo(AccountNo);
		// <-- Fin comentario
	
		pay.setAmount(C_Currency_ID, Amt);
		pay.setDiscountAmt(BigDecimal.ZERO);
		pay.setWriteOffAmt(BigDecimal.ZERO);
		pay.setOverUnderAmt(BigDecimal.ZERO);
		
		return pay;
		
	}
	
	private MAllocationLine createOxpMAllocationLine(Payment p, MPayment pay) throws PosException {
		return createOxpMAllocationLine(p, pay, null, null);
	}
	
	private MAllocationLine createOxpMAllocationLine(Payment p, MCashLine cashLine) throws PosException {
		return createOxpMAllocationLine(p, null, cashLine, null);
	}
	
	private MAllocationLine createOxpMAllocationLine(Payment p, Integer creditInvoiceID) throws PosException {
		return createOxpMAllocationLine(p, null, null, creditInvoiceID);
	}
	
	/**
	 * @param p
	 * @param pay
	 * @param cashLine
	 * @param creditInvoiceID
	 * @return
	 * @throws PosException
	 */
	private MAllocationLine createOxpMAllocationLine(Payment p, MPayment pay, MCashLine cashLine, Integer creditInvoiceID) throws PosException {
		
		BigDecimal allocLineAmt = currencyConvert(p.getAmount(), p.getCurrencyId(), allocHdr.getC_Currency_ID());
		BigDecimal writeOffAmt = BigDecimal.ZERO;
		
		if (faltantePorRedondeo != null) {
			writeOffAmt = faltantePorRedondeo;
			faltantePorRedondeo = null;
		}
		
		MAllocationLine allocLine = new MAllocationLine(allocHdr, allocLineAmt, BigDecimal.ZERO, writeOffAmt, BigDecimal.ZERO);
		
		if (getShouldCreateInvoice())
			allocLine.setC_Invoice_ID(invoice.getC_Invoice_ID());

		allocLine.setC_Order_ID(morder.getC_Order_ID());
		allocLine.setC_BPartner_ID(morder.getC_BPartner_ID());
		
		if (pay != null) {
			allocLine.setC_Payment_ID(pay.getC_Payment_ID());
		} else if (cashLine != null) {
			allocLine.setC_CashLine_ID(cashLine.getC_CashLine_ID());
		} else if (creditInvoiceID != null) {
			allocLine.setC_Invoice_Credit_ID(creditInvoiceID);
		}
		
		throwIfFalse(allocLine.save());
		
		allocLines.add(allocLine);
		
		return allocLine;
	}
	
	private void createOxpPayments(Order order) throws PosException {
		for (CashPayment p : cashPayments)
			createOxpCashPayment(p);
		
		for (CheckPayment p : checkPayments)
			createOxpCheckPayment(p);

		for (CreditCardPayment p : creditCardPayments)
			createOxpCreditCardPayment(p);

		for (CreditPayment p : creditPayments)
			createOxpCreditPayment(p);
		
		for (CreditNotePayment p : creditNotePayments)
			createOxpCreditNotePayment(p);
		
		for (BankTransferPayment p : bankTransferPayments)
			createOxpBankTransferPayment(p);

		// Completar Pagos. En este punto ya est??n creadas y guardadas las lineas de imputacion (MAllocationLine),
		// por lo que puedo completarlos sin que se generen conflictos con la imputacion autom??tica.
		// Para m??s informacion mirar completeIt() (allocateIt()) de MPayment.
		
		for (MPayment p : mpayments.values()) {
			// Seteo el bypass para que no actualice el cr??dito del cliente ya
			// que se realiza luego al finalizar las operaciones
			p.setUpdateBPBalance(false);
			throwIfFalse(p.processIt(DocAction.ACTION_Complete), p);
			throwIfFalse(p.save(), p);
		}
		
	}
	
	private void createOxpCashPayment(CashPayment p) throws PosException {
		// MCashBook cashBook = new MCashBook(ctx, posConfig.getC_CashBook_ID(), trxName);
		MCash cash = null;
		// Si el config tiene asociado el Cash entonces se usa ese (Cajas Diarias)
		if (getPoSCOnfig().getCashID() > 0) {
			cash = new MCash(ctx, getPoSCOnfig().getCashID(), getTrxName());
		// Sino se obtiene uno para la fecha el Libro indicado en el config
		} else {
			cash = MCash.get(ctx, getPoSCOnfig().getCashBookID(), invoiceDate, getTrxName());
		}
		
		throwIfFalse(cash.getC_Cash_ID() > 0);
		MCashLine cashLine = new MCashLine(cash);
		
		// Verificar que el CurrencyID sea valido con respecto al Amount 
		
		BigDecimal convertedAmt = p.getAmount();
		
		cashLine.setDescription("");
		
		if (getShouldCreateInvoice())
		{
			cashLine.setCashType(MCashLine.CASHTYPE_Invoice);
			cashLine.setC_Invoice_ID(invoice.getC_Invoice_ID());
		}
		else
			cashLine.setCashType(MCashLine.CASHTYPE_GeneralReceipts);
		
		cashLine.setUpdateBPBalance(false);
		cashLine.setC_Currency_ID(p.getCurrencyId());
		cashLine.setAmount(convertedAmt);
		cashLine.setDiscountAmt(BigDecimal.ZERO);
		cashLine.setWriteOffAmt(BigDecimal.ZERO);
		cashLine.setIsGenerated(true);
		cashLine.setIgnoreAllocCreate(true);

		throwIfFalse(cashLine.save()); // Necesario para que se asigne el C_CashLine_ID
		throwIfFalse(cashLine.processIt(MCashLine.ACTION_Complete));
		throwIfFalse(cashLine.save());

		// Agrego el cashline para llevar su registro
		getMCashLines().put(cashLine.getID(), cashLine);
		
		MAllocationLine allocLine = createOxpMAllocationLine(p, cashLine);
		
	}
	
	private void createOxpCheckPayment(CheckPayment p) throws PosException {
		
		MPayment pay = createOxpMPayment(MPayment.TENDERTYPE_Check, getClientCurrencyID(), p.getAmount(), null);
		String sucursal = VModelHelper.getSQLValueString(null, " select AccountNo from c_bankaccount where c_bankaccount.c_bankaccount_id = ? ", p.getBankAccountID() );
		pay.setDateAcct(p.getAcctDate());
		pay.setDateTrx(p.getEmissionDate());
		
		pay.setC_BankAccount_ID(p.getBankAccountID());
		pay.setCheckNo(p.getCheckNumber()); // Numero de cheque
		pay.setMicr(sucursal + ";" + p.getBankAccountID() + ";" + p.getCheckNumber()); // Sucursal; cta; No. cheque
		pay.setA_Name(""); // Nombre
		pay.setA_Bank(p.getBankName());
		pay.setA_CUIT(p.getCuitLibrador());
		pay.setDueDate(p.getAcctDate());
		
		throwIfFalse(pay.save(), pay);
		mpayments.put(pay.getC_Payment_ID(), pay);
		MAllocationLine allocLine = createOxpMAllocationLine(p, pay);
		
		if (sobraPorCheques != null && p.getAmount().compareTo(sobraPorCheques) > 0) {
			allocLine.setAmount(allocLine.getAmount().subtract(sobraPorCheques));
			
			sobraPorCheques = null;
		}
	}
	
	private void createOxpCreditCardPayment(CreditCardPayment p) throws PosException {
		
		MPayment pay = createOxpMPayment(MPayment.TENDERTYPE_CreditCard, getClientCurrencyID(), p.getAmount(), p.getCouponNumber());
		MEntidadFinanciera entidadFinanciera = new MEntidadFinanciera(ctx, p.getEntidadFinancieraID(), null);  
		
		String CreditCardType = entidadFinanciera.getCreditCardType();
		
		pay.setCreditCard(MPayment.TRXTYPE_Sales, CreditCardType, p.getCreditCardNumber(), "", 0, 0 );
		pay.setC_BankAccount_ID(entidadFinanciera.getC_BankAccount_ID());
		pay.setM_EntidadFinancieraPlan_ID(p.getPlan().getEntidadFinancieraPlanID());
		pay.setCouponNumber(p.getCouponNumber());
		pay.setA_Bank(p.getBankName());
		
		throwIfFalse(pay.save(), pay);
		mpayments.put(pay.getC_Payment_ID(), pay);
		createOxpMAllocationLine(p, pay);
	}
	
	private void createOxpCreditPayment(CreditPayment p) throws PosException {
		// NULL. Nothing to be done. Please, move along.
	}

	private void createOxpCreditNotePayment(CreditNotePayment p) throws PosException {
		// No se debe crear ning??n documento de pago ya que la nota de cr??dito ya
		// existe en el sistema. Solo se hace la imputaci??n contra la factura del pedido
		createOxpMAllocationLine(p, p.getInvoiceID());
	}

	private void createOxpBankTransferPayment(BankTransferPayment p) throws PosException {
		MPayment pay = createOxpMPayment(MPayment.TENDERTYPE_DirectDeposit, p.getCurrencyId(), p.getAmount(), null);
		
		pay.setDateTrx(p.getTransferDate());
		pay.setDateAcct(p.getTransferDate());
		pay.setC_BankAccount_ID(p.getBankAccountID());
		pay.setCheckNo(p.getTransferNumber());
		throwIfFalse(pay.save(), pay);
		mpayments.put(pay.getC_Payment_ID(), pay);
		createOxpMAllocationLine(p, pay);
	}
	
	private MAllocationHdr createOxpAllocation() throws PosException {
		
		MAllocationHdr hdr = new MAllocationHdr(ctx, 0, getTrxName());
		
		hdr.setAllocationType(MAllocationHdr.ALLOCATIONTYPE_SalesTransaction);

		BigDecimal approvalAmt = sumaPagos;
		
		hdr.setApprovalAmt(approvalAmt);
		hdr.setGrandTotal(approvalAmt); // GrandTotal = approvalAmt - Retenciones
		hdr.setRetencion_Amt(BigDecimal.ZERO);

		hdr.setC_BPartner_ID(partner.getC_BPartner_ID());
		hdr.setC_Currency_ID(getClientCurrencyID());
		
		hdr.setDateAcct(this.invoiceDate);
		hdr.setDateTrx(this.invoiceDate);
		
		hdr.setDescription("TPV: ");
		hdr.setIsManual(false);
		
		throwIfFalse(hdr.save(), hdr);
		
		return hdr;
	}

	private void doCompleteAllocation() throws PosException {
		
		
		for (MAllocationLine al : allocLines) 
		{
			throwIfFalse(al.save());
		}
		
		// Completar Allocation
		
		// Seteo el bypass para que no actualice el cr??dito del cliente ya
		// que se realiza luego al finalizar las operaciones
		allocHdr.setUpdateBPBalance(false);
		
		if (allocLines.size() > 0) {
			throwIfFalse(allocHdr.processIt(DocAction.ACTION_Complete), allocHdr);
			throwIfFalse(allocHdr.save(), allocHdr);
		} else if (creditPayments.size() > 0) {
			/*
			 * Si el medio de pago es cr??dito no se crea ning??n elemento de pago ya 
			 * que la factura creada quedar?? con un saldo a pagar.
			 * 
			 * En este caso, no hay que crear ningun allocation.
			 * 
			 */
			throwIfFalse(allocHdr.processIt(DocAction.ACTION_Void), allocHdr);
			throwIfFalse(allocHdr.save(), allocHdr);
		} else {
			throw new PosException("doCompleteAllocation: allocLines.size() == 0 && creditPayments.size() == 0");
		}
		
	}

	@Override
	public User searchUser(String name, String password) throws UserException {
		MUser mUser = MUser.get(Env.getCtx(),name,password);
		// El usuario o clave son incorrectos;
		if(mUser == null)
			throw new UserException("InvalidUserPassError");
		
		return getUser(mUser.getID());
	}
	
	@Override
	public BusinessPartner getBPartner(int bPartnerID) {
		MBPartner mBPartner = new MBPartner(Env.getCtx(),bPartnerID,null);
		// Se toma siempre el M_DiscountSchema_ID de la EC ya que es transacci??n de ventas.
		DiscountSchema discountSchema = getDiscountSchema(mBPartner.getM_DiscountSchema_ID());
		// Esquema de vencimientos de la entidad comercial
		PaymentTerm paymentTerm = getPaymentTerm(mBPartner.getC_PaymentTerm_ID());
		// Medio de Pago de la entidad comercial
		PaymentMedium paymentMedium = getPaymentMedium(mBPartner.getC_POSPaymentMedium_ID());
		BusinessPartner rBPartner = new BusinessPartner(bPartnerID, 0,
				mBPartner.getTaxID(), mBPartner.getName(), mBPartner
						.getM_PriceList_ID(), discountSchema, mBPartner
						.getFlatDiscount(), paymentTerm, paymentMedium);
		rBPartner.setDiscountSchemaContext(mBPartner.getDiscountContext());
		int codigoIVA = MCategoriaIva.getCodigo(mBPartner.getC_Categoria_Iva_ID(), null);
		rBPartner.setIVACategory(codigoIVA);
		
		rBPartner.setCustomerName(mBPartner.getName());
		// Si no es la misma EC que la por defecto en la config, se cargan los datos
		// de la EC como datos del comprador (DNI y Direcci??n).
		if (bPartnerID != getPoSCOnfig().getBPartnerCashTrxID()) {
			rBPartner.setCustomerAddress(getBPartnerLocations(bPartnerID).get(0).toString());
			rBPartner.setCustomerIdentification(mBPartner.getTaxID());
			// Indica que los datos del comprador se deben mantener sincronizados
			// con los datos de la EC.
			rBPartner.setCustomerSynchronized(true);
		}
		return rBPartner;
	}

	@Override
	public List<Location> getBPartnerLocations(int bPartnerID) {
		MBPartnerLocation[] bpLocations = MBPartnerLocation.getForBPartner(Env.getCtx(),bPartnerID);
		List<Location> locations = new ArrayList<Location>();
		
		for (int i = 0; i < bpLocations.length; i++) {
			if (bpLocations[i].isActive()) {
				MLocation mLocation = bpLocations[i].getLocation(false);
				Location location = new Location(bpLocations[i].getID(), mLocation.toStringShort());
				locations.add(location);
			}
		}
		return locations;
	}

	@Override
	public boolean productStockValidate(int productId, int count, int attributeSetInstanceID) {
		MProduct mProduct = new MProduct(ctx,productId,null);
		boolean stockAvailable;
        if( mProduct.isStocked()) {
            int M_Warehouse_ID = getPoSCOnfig().getWarehouseID(); 
            BigDecimal availableCount;
            // Se consulta el stock para el articulo con instancia de atributo si la misma
            // existe.
            if (attributeSetInstanceID > 0)
            	availableCount = MStorage.getQtyAvailable( M_Warehouse_ID, productId, attributeSetInstanceID, null );
            else
            	availableCount = MStorage.getQtyAvailable( M_Warehouse_ID, productId);
            
            if( availableCount == null ) {
            	stockAvailable = false;
            } else { 
            	stockAvailable =  
            		(availableCount.compareTo(BigDecimal.valueOf(count)) >= 0 );
                
            }
        } else
        	stockAvailable = true;

		return stockAvailable;
	}

	@Override
	public int getOrgCityId() {
		MOrgInfo orgInfo = MOrgInfo.get(ctx,Env.getAD_Org_ID(ctx));
		int orgLocId = orgInfo.getC_Location_ID();
		MLocation orgLoc = MLocation.get(ctx,orgLocId,null);
		return orgLoc.getC_City_ID();
	}

	@Override
	public List<EntidadFinanciera> getEntidadesFinancieras() {
		List<EntidadFinanciera> entidades = new ArrayList<EntidadFinanciera>();
		String sql = "SELECT M_EntidadFinanciera_ID, Name " +
			 	     "FROM M_EntidadFinanciera " + 
			 	     "WHERE ((C_City_ID IS NULL) OR (C_City_ID = ?)) AND IsActive = 'Y' ";
		
		sql = MRole.getDefault().addAccessSQL( sql, "M_EntidadFinanciera", false, true );
		
		try {
			PreparedStatement pstmt = DB.prepareStatement(sql);
			// Seteo parametros
			pstmt.setInt(1,getOrgCityId());
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				int c_EntidadFinanciera_ID = rs.getInt(1);
				String name = rs.getString(2);
				
				EntidadFinanciera entidad = new EntidadFinanciera(c_EntidadFinanciera_ID,name);
				entidades.add(entidad);
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return entidades;
	}

	@Override
	public List<PoSConfig> getPoSConfigs() {
		int AD_Client_ID = Env.getAD_Client_ID(ctx);
		int AD_Org_ID = Env.getAD_Org_ID(ctx);
		int AD_User_ID = Env.getAD_User_ID(ctx);
		Timestamp today = Env.getDate();
		List<PoSConfig> posConfigs;
		
		// Cajas Diarias Activas. Verifica si hay al menos una caja diaria
		// abierta para este usuario, fecha y org y obtiene las configs a partir
		// de esas cajas diarias.
		if (MPOSJournal.isActivated()){
			posConfigs = getJournalPOSConfigs(AD_Org_ID, AD_User_ID, today);
		
		// Cajas Diarias NO Activas. Busca configuraciones de TPV con modo
		// de operaci??n simple (??nico usuario - Modo Antiguo) para el
		// usuario y organizaci??n actual. (este es el comportamiento
		// original antes del agregado de Cajas Diarias)
		} else {
			String sqlWhere = " AD_Client_ID = " + AD_Client_ID + 
				              " AND AD_Org_ID = " + AD_Org_ID +
				              " AND SalesRep_ID = " + AD_User_ID +
				              " AND IsActive = 'Y' " +
				              " AND OperationMode = '" + MPOS.OPERATIONMODE_POSSimple + "'"; 
			
			int[] posIds = MPOS.getAllIDs("C_Pos",sqlWhere,null);
			posConfigs = new ArrayList<PoSConfig>();
			
			for (int i = 0; i < posIds.length; i++) {
				int C_Pos_ID = posIds[i];
				MPOS mPos = new MPOS(ctx, C_Pos_ID, null);
				posConfigs.add(new PoSConfig(mPos));
			}
		}
		return posConfigs;
	}
	
	/**
	 * 
	 */
	public void printTicket() {
		// Salimos si ya se imprimi?? mediante controlador fiscal
		if (invoice != null && invoice.requireFiscalPrint()) {
			return;
		}

		TimeStatsLogger.beginTask(MeasurableTask.POS_PRINT_TICKET);
		
		int processID = 0;
		int tableID = 0;
		int recordID = 0;
		// Por defecto se lanza el informe configurado en la pesta??a de encabezado
		// del pedido.
		processID = new M_Tab(ctx, ORDER_TAB_ID, getTrxName()).getAD_Process_ID();
		tableID = MOrder.Table_ID;
		recordID = morder.getC_Order_ID();
		
		// En caso de que se haya emitido una factura, entonces se lanza el informe
		// de la factura.
		if (getShouldCreateInvoice()) {
			// Primero se busca el informe en el Tipo de Documento de la Factura
			MDocType docType = MDocType.get(getCtx(), invoice.getC_DocType_ID());
			if (docType.getAD_Process_ID() > 0) {
				processID = docType.getAD_Process_ID();
			// Si el tipo de documento no tiene un informe asociado, se utiliza el
			// informe configurado para la ventana de Facturas de Clientes.
			} else {
				processID = new M_Tab(ctx, INVOICE_TAB_ID, getTrxName())
						.getAD_Process_ID();
			}
			tableID = MInvoice.Table_ID;
			recordID = invoice.getC_Invoice_ID();
		}

		// Lanza el informe determinado en caso de existir.
        if( processID > 0 ) {
	        ProcessInfo pi = new ProcessInfo("TPV", processID, tableID, recordID);	            
	        ProcessCtl.process(getProcessListener(), 0, pi, null );    // calls lockUI, unlockUI
        } else {
			// M??todo antoguo. En caso de que no se haya podido determinar un
			// Informe (AD_Process) se intenta imprimir un informe a partir de
			// los formatos de impresi??n.
        	printTicketFromPrintFormat();
        }

        //
		TimeStatsLogger.endTask(MeasurableTask.POS_PRINT_TICKET);
	}

	private void printTicketFromPrintFormat() {
		int order_ID = morder.getC_Order_ID();
		MPrintFormat pf = getTicketPrintFormat();
		if (pf != null) {
			// Se configura la consulta para la impresi??n.
			MQuery query = new MQuery( "C_Order" );
			query.addRestriction( "C_Order_ID", MQuery.EQUAL, new Integer(order_ID), "", "" );
			// Se crea la informaci??n para la impresi??n.
			PrintInfo info = new PrintInfo( pf.getName(), pf.getAD_Table_ID(), order_ID );
			info.setDescription( query.getInfo());
			// Se instancia el motor de reportes.
			ReportEngine re = new ReportEngine( Env.getCtx(), pf, query, info );
			View vv = re.getView();
			// TODO: printer name ?
			re.getPrintInfo().setWithDialog( true );
			re.print();
		}
	}
	
	private MPrintFormat getTicketPrintFormat() {
		MPrintFormat pf = null;
		// Consulta de formato de impresi??n. 
		// 1. Primero d?? prioridad a la configuraci??n realizada
		// en la ventana de formatos, filtrando por compa??ia y organizaci??n.
		// 2. Si no encuentra un formato espec??fico para la organizaci??n, obtiene
		// uno asignado para la compa??ia sin filtrar por organizaci??n.
		// 3. Finalmente, si tampoco hay resultados en 2, consulta el formato
		// de impresi??n asignado al tipo de documento, en la tabla de DocTypes.
		String sql =
			" SELECT AD_PrintFormat_ID "+
			" FROM ( "+
			"	(SELECT Order_Printformat_ID AS AD_PrintFormat_ID "+
			"	FROM AD_PrintForm "+
			"	WHERE AD_Client_ID = ? AND AD_Org_ID = ?) "+
	
			"	UNION "+
	
			" 	(SELECT Order_Printformat_ID AS AD_PrintFormat_ID "+
			"	FROM AD_PrintForm "+
			"	WHERE AD_Client_ID = ?) "+
	
			"	UNION "+
	
			"	(SELECT AD_PrintFormat_ID "+
			"	FROM C_DocType "+
			"	WHERE C_DocType_ID = ?) "+
			" ) f "+
			" WHERE AD_PrintFormat_ID IS NOT NULL ";
		
		// Se obtiene la compa??ia y organizaci??n para consultar
		// el formato de impresi??n asignado.
		int AD_Client_ID = Env.getAD_Client_ID(ctx);
		int AD_Org_ID = Env.getAD_Org_ID(ctx);

		try {
			PreparedStatement pstmt = DB.prepareStatement(sql);
			// Se asignan los par??metros.
			int i = 1;
			pstmt.setInt(i++, AD_Client_ID);
			pstmt.setInt(i++, AD_Org_ID);
			pstmt.setInt(i++, AD_Client_ID);
			pstmt.setInt(i++, getPoSCOnfig().getOrderDocTypeID());
			// Se consultan los datos, si no se encuentra ning??n formato retorna
			// un resulset vacio.
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				int printFormatID = rs.getInt("AD_PrintFormat_ID");
				pf = MPrintFormat.get( Env.getCtx(), printFormatID, false);
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return pf;
	}

	@Override
	public Product getProduct(int productId, int attributeSetInstanceId) {
		List<Integer> vendorsIDs = null;
		int m_PriceList_ID = getPoSCOnfig().getPriceListID(); 
		// Obtengo la tarifa, y pido la version valida.
		MPriceList mPriceList = new MPriceList(Env.getCtx(),m_PriceList_ID,null);
		int m_PriceList_Version_ID = mPriceList.getPriceListVersion(null).getID();
		Product product = null;
		
		String sql;
		
		// Obtiene el id, nombre y el precio en la tarifa del producto.
		/*
		sql = "SELECT p.M_Product_ID, pp.PriceStd, p.Name, pp.PriceLimit, p.upc " +
			  "FROM M_ProductPrice pp, M_Product p " +
              "WHERE p.M_Product_ID = pp.M_Product_ID " +
              "	 AND p.M_Product_ID = ? " +
              "  AND M_PriceList_Version_ID = ? " +
              "  AND p.IsActive = 'Y' " +
              "  AND pp.IsActive = 'Y' ";
		 */
		
		sql = " SELECT p.M_Product_ID, bomPriceStd(p.M_Product_ID, ?, ?), p.name, bomPriceLimit(p.M_Product_ID, ?, ?), p.upc, s.MandatoryType, p.m_product_category_id, p.CheckoutPlace, p.IsSold, p.Value " +    
			  "	FROM M_Product p " +
			  "	LEFT JOIN M_AttributeSet s ON (p.M_AttributeSet_ID = s.M_AttributeSet_ID)     " +
			  " WHERE p.M_Product_ID = ? " +
			  "   AND p.IsActive = 'Y'";

		
		try {
			PreparedStatement pstmt = DB.prepareStatement(sql);
			int i = 1;
			pstmt.setInt(i++,m_PriceList_Version_ID);
			pstmt.setInt(i++,attributeSetInstanceId);

			pstmt.setInt(i++,m_PriceList_Version_ID);
			pstmt.setInt(i++,attributeSetInstanceId);

			pstmt.setInt(i++,productId);
			
			ResultSet rs = pstmt.executeQuery();
			if(rs.next()) {
				int m_Product_Id = rs.getInt(1);
				BigDecimal productPrice = rs.getBigDecimal(2);
				String productName = rs.getString(3);
				BigDecimal productLimitPrice = rs.getBigDecimal(4);
				String code = rs.getString(5);
				if (code == null || code.trim().isEmpty() || code.trim().equals("0")) {
					code = rs.getString("Value");
				}
				boolean masiMandatory = MAttributeSet.MANDATORYTYPE_AlwaysMandatory.equals(rs.getString("MandatoryType"));
				String masiDescription = (String)DB.getSQLObject(null, "SELECT Description FROM m_attributesetinstance WHERE m_attributesetinstance_id = ?", new Object[]{attributeSetInstanceId});
				vendorsIDs = getVendors(m_Product_Id);
				String checkoutPlace = rs.getString("CheckoutPlace");
				boolean sold = "Y".equals(rs.getString("IsSold"));
				// Creo el producto.
				product = new Product(
								m_Product_Id, 
								code, 
								productName, 
								productPrice, 
								productLimitPrice, 
								attributeSetInstanceId, 
								masiDescription, 
								getPoSCOnfig().isPriceListWithTax(), 
								masiMandatory, 
								rs.getInt("m_product_category_id"), 
								vendorsIDs,
								checkoutPlace,
								sold);
			}
			
			rs.close();
			pstmt.close();
			
			return product;
		
		} catch (SQLException e) {
			//TODO Relanzar una excepcion definida.
			return null;
		}
	}

	@Override
	public void reloadPoSConfig(int windowNo) {
		Properties ctx = Env.getCtx();
		Env.setContext(ctx, windowNo,"M_PriceList_ID", getPoSCOnfig().getPriceListID());
		Env.setContext(ctx, windowNo,"M_Warehouse_ID", getPoSCOnfig().getWarehouseID());
	}

	@Override
	public Tax getProductTax(int productId, int locationID) {
		Timestamp now = new Timestamp(System.currentTimeMillis());
		BigDecimal taxRate;
		
		// Se obtiene el Tax del producto.
		int taxId = org.openXpertya.model.Tax.get( ctx,productId, 0, now, now, Env.getAD_Org_ID(ctx),getPoSCOnfig().getWarehouseID(), locationID,locationID,true);
		MTax mTax = MTax.get(ctx,taxId,null);
		if(mTax != null)
			taxRate = mTax.getRate();
		else
			taxRate = BigDecimal.ZERO;
		return new org.openXpertya.pos.model.Tax(taxId, taxRate);
	}
	
	public Tax getProductTax(int productID) {
		// Se obtiene la EC para efectivo para obtener la direcion para
		// calcular el impuesto del producto.
		MBPartner bPartner = new MBPartner(ctx,getPoSCOnfig().getBPartnerCashTrxID(),null);
		int locID = 0;
		if(bPartner.getID() > 0) {
			MBPartnerLocation[] locs = bPartner.getLocations(false);
			locID = (locs.length > 0 ? locs[0].getID() : 0);
		}
		
		return getProductTax(productID, locID);
	}

	@Override
	public Order loadOrder(int orderId, boolean loadLines) throws InvalidOrderException, PosException {
		if (orderId == 0)
			throw new InvalidOrderException();
		
		Order order = new Order();
		MOrder mOrder = new MOrder(ctx, orderId, getTrxName());
		if (!"CO".equals(mOrder.getDocStatus()) && ! "CL".equals(mOrder.getDocStatus()))
			throw new InvalidOrderException(Msg.translate(ctx, "POSOrderStatusError"));
		
		BusinessPartner bPartner = getBPartner(mOrder.getC_BPartner_ID());
		
		order.setId(orderId);
		order.setBusinessPartner(bPartner);
		order.setDate(mOrder.getDateOrdered());
		
		// Carga las l??neas si as?? se indic?? 
		if (loadLines) {
			loadOrderLines(order);
		}
		
		return order;
	}

	@Override
	public void loadOrderLines(Order order) {
		
		// Nada que cargar...
		if (order.getId() == 0) {
			return;
		}
		
		MOrder mOrder = new MOrder(ctx, order.getId(), getTrxName());
		MOrderLine[] oLines = mOrder.getLines();
		for (int i = 0; i < oLines.length; i++) {
			MOrderLine line = oLines[i];
			if (line.getM_Product_ID() == 0)
				continue;

			order.addOrderProduct(createOrderProduct(line));
		}
	}

	/**
	 * Crea un <code>OrderProduct</code> a partir de una l??nea de un pedido
	 * del sistema (C_OrderLine)
	 * @param line L??nea del pedido.
	 * @return La l??nea del pedido de TPV.
	 */
	private OrderProduct createOrderProduct(MOrderLine line) {
		int m_Product_ID = line.getM_Product_ID();
		OrderProduct orderProduct = null;
		Product product = null; 
		
		product = getProduct(m_Product_ID, line.getM_AttributeSetInstance_ID());
		product.setStdPrice(line.getPriceList());
		
		MTax mTax = MTax.get(ctx, line.getC_Tax_ID(), null);
		// Lugar de retiro del art??culo.
		String checkoutPlace;
		// Si el TPV no crea remito siempre el retiro es por almac??n. Adem??s, si crea remito
		// y est?? configurado para que los pedidos pre-creados agregados al pedido TPV sean
		// entregados por Almac??n, entonces el retiro tambi??n es por almac??n.
		if (!getPoSCOnfig().isCreateInOut()
				|| getPoSCOnfig().isDeliverOrderInWarehouse()) {
			checkoutPlace = MProduct.CHECKOUTPLACE_Warehouse;
		// Aqu??, se crean remitos y los pedidos pre-creados agregados se incluyen en
		// el remito del TPV, por consiguiente el retiro es por TPV.
		} else {
			checkoutPlace = MProduct.CHECKOUTPLACE_PointOfSale;
		}
					
		orderProduct = 
			new OrderProduct(line.getQtyEntered().intValue(), 
					         line.getDiscount(),
					         new Tax(mTax.getID(), mTax.getRate()),
					         product, checkoutPlace);
		
		orderProduct.setPrice(line.getPriceActual());
		return orderProduct;
	}

	@Override
	public void validatePoSConfig() throws PosException {
		getPoSCOnfig().validateOnline();
	}

	@Override
	public User getCurrentUser() {
		return getUser(getPoSCOnfig().getCurrentUserID());
	}
	
	private User getUser(int userID) {
		int supervisorRoleID = getPoSCOnfig().getSupervisorRoleID();
		MUser mUser = MUser.get(ctx, userID);
		// Validaci??n de usuario.
		if (mUser == null)
			return null;
		
		User user = new User(mUser.getName(), mUser.getPassword());
		// Se chequea si el los perfiles del usuario, alguno tiene permiso
		// para sobreescribir el precio limite de los productos.
		boolean overwritePriceLimit = false;
		MRole[] roles = mUser.getRoles(Env.getAD_Org_ID(ctx));
		for (int i = 0; i < roles.length && !overwritePriceLimit; i++) {
			MRole userRole = roles[i];
			if(userRole.isOverwritePriceLimit()) {
				overwritePriceLimit = true;
			}
		}
		user.setOverwriteLimitPrice(overwritePriceLimit);

		// Verifico si el usuario tiene el perfil de supervisor del TPV
		// para poder realizar modificaciones de productos en el pedido.
		user.setPoSSupervisor(false);
		MRole[] userRoles = mUser.getRoles(Env.getAD_Org_ID(Env.getCtx()));
		for (int i = 0; i < userRoles.length && !user.isPoSSupervisor(); i++) {
			MRole userRole = userRoles[i];
			if(userRole.getAD_Role_ID() == supervisorRoleID)
				user.setPoSSupervisor(true);
		}
		
		return user;
	}

	@Override
	public List<PriceList> getPriceLists() {
		// Buscar las listas de precios de ventas
		List<PO> priceLists = PO.find(Env.getCtx(), "m_pricelist", "ad_client_id = ? and isactive = 'Y' and issopricelist = 'Y'", new Object[]{Env.getAD_Client_ID(Env.getCtx())}, null, null);  
		List<PriceList> lists = new ArrayList<PriceList>();
		PriceList newPriceList;
		MPriceList mPriceList;
		for (PO priceList : priceLists) {
			mPriceList = (MPriceList)priceList; 
			newPriceList = new PriceList(mPriceList.getID(),mPriceList.getName(),mPriceList.getDescription(),mPriceList.getC_Currency_ID(),mPriceList.isTaxIncluded(),mPriceList.isSOPriceList(),mPriceList.isDefault(),mPriceList.getPricePrecision());
			lists.add(newPriceList);
		}
		return lists;
	}

	@Override
	public PriceList getCurrentPriceList(int windowNo) {
		MPriceList priceList = new MPriceList(Env.getCtx(), Env.getContextAsInt(Env.getCtx(), windowNo, "M_PriceList_ID"), null);
		PriceList newPriceList = new PriceList(priceList.getID(),priceList.getName(), priceList.getDescription(),priceList.getC_Currency_ID(),priceList.isTaxIncluded(),priceList.isSOPriceList(),priceList.isDefault(),priceList.getPricePrecision());
		return newPriceList;
	}

	@Override
	public void updatePriceList(PriceList newPriceList, int windowNo) {
		// Seteo la nueva tarifa dentro del contexto
		Env.setContext(ctx, windowNo,"M_PriceList_ID", newPriceList.getId());
	}
	
	@Override
	public PriceListVersion getCurrentPriceListVersion(PriceList priceList, int windowNo) {
		Timestamp priceDate = null;
        // Sales Order Date
        String dateStr = Env.getContext( Env.getCtx(),windowNo,"DateOrdered" );
        if( (dateStr != null) && (dateStr.length() > 0) ) {
            priceDate = Env.getContextAsDate( Env.getCtx(),windowNo,"DateOrdered" );
        } 
        else {// Invoice Date
            dateStr = Env.getContext( Env.getCtx(),windowNo,"DateInvoiced" );
            if( (dateStr != null) && (dateStr.length() > 0) ) {
                priceDate = Env.getContextAsDate( Env.getCtx(),windowNo,"DateInvoiced" );
            }
        }
        // Today
        if( priceDate == null ) {
            priceDate = new Timestamp( System.currentTimeMillis());
        }
        int versionID = 0;
        PriceListVersion version = null;
        String SQL = "SELECT plv.M_PriceList_Version_ID, plv.ValidFrom " + "FROM M_PriceList pl, M_PriceList_Version plv " + "WHERE pl.M_PriceList_ID=plv.M_PriceList_ID" + " AND plv.IsActive='Y'" + " AND pl.M_PriceList_ID=? "    // 1
                          + "ORDER BY plv.ValidFrom DESC";
        // find newest one
        try {
            PreparedStatement pstmt = DB.prepareStatement( SQL );
            pstmt.setInt( 1,priceList.getId());
            ResultSet rs = pstmt.executeQuery();
            while( rs.next() && (versionID == 0) ) {
                Timestamp plDate = rs.getTimestamp( 2 );
                if( !priceDate.before( plDate )) {
                	versionID = rs.getInt( 1 );
                }
            }
            if(versionID != 0){
            	MPriceListVersion pLVersion = new MPriceListVersion(Env.getCtx(), versionID, null);
            	version = new PriceListVersion(pLVersion.getID(), pLVersion.getName(), pLVersion.getDescription(), pLVersion.getM_DiscountSchema_ID(), pLVersion.getValidFrom());
            }
            rs.close();
            pstmt.close();
        } catch( SQLException e ) {
            e.printStackTrace();
        }
//        Env.setContext( Env.getCtx(),windowNo,"M_PriceList_Version_ID",retValue );
        return version;
	}

	@Override
	public BigDecimal getProductPrice(Product product, PriceListVersion priceListVersion) {
		BigDecimal price = null;
		if(priceListVersion != null && product != null){
			PO productPrice = PO.findFirst(Env.getCtx(), "m_productprice", "m_product_id = ? AND m_pricelist_version_id = ?", new Object[]{product.getId(),priceListVersion.getId()}, new String[]{"created desc"}, null);
			if(productPrice != null){
				// En el TPV el precio de lista es el precio std
				price = ((MProductPrice)productPrice).getPriceStd();
			}
		}		
		return price;
	}

	@Override
	public void updateBPartner(BusinessPartner bpartner, int windowNo) {
		Env.setContext(ctx, windowNo,"C_BPartner_ID", bpartner == null?0:bpartner.getId());
	}

	@Override
	public DiscountSchema getDiscountSchema(int discountSchemaID) {
		DiscountSchema discountSchema = null;
		if(discountSchemaID > 0){
			discountSchema = new DiscountSchema(discountSchemaID, this);
			// Se ignoran los esquemas que no son v??lidos para la fecha actual.
			if (!discountSchema.isValid()) {
				discountSchema = null;
			}
		}
		return discountSchema;
	}

	@Override
	public List<Integer> getVendors(int productID) {
		List<PO> vendors = PO.find(ctx, "M_Product_PO", "m_product_id= ? AND iscurrentvendor='Y'", new Object[]{productID}, null, null);
		List<Integer> vendorsID = new ArrayList<Integer>();
		MProductPO productPO;
		for (PO mProductPO : vendors) {
			productPO = (MProductPO)mProductPO;
			vendorsID.add(productPO.getC_BPartner_ID());
		}
		return vendorsID;
	}
	
	/**
	 * @return Devuelve la sub-consulta para b??squeda exacta y/o parcial de art??culos
	 * por UPC
	 */
	private String getSearchByUPCQuery() {
		StringBuffer query = new StringBuffer();
		// La consulta base contiene partes sin completar encerradas entre <>. Estos tags
		// son luego reemplazados por valores concretos seg??n la necesidad de hacer una 
		// b??squeda exacta y/o parcial. De esta forma la consulta se esribe una ??nica vez
		// y se reutiliza en ambas b??squedas.
		StringBuffer baseQuery = new StringBuffer();
	    baseQuery
	      // 1. Busca en los UPC de instancias. Estos son los que tiene mayor prioridad
	      .append("(SELECT M_Product_ID, M_AttributeSetInstance_ID, <MT_M_Product_Upc_Instance> As MatchType ")
	      .append( "FROM M_Product_Upc_Instance ")
	      .append( "WHERE (UPC <COMPARATOR> <VALUE>) AND IsActive = 'Y') ")
	      .append("UNION ")
		  // 2. Busca en los UPC asociados al art??culo. Primero se lista el UPC predeterminado.
	      .append("(SELECT M_Product_ID, 0, <MT_M_ProductUPC> ") 
		  .append( "FROM M_ProductUPC ")
		  .append( "WHERE (UPC <COMPARATOR> <VALUE>) AND IsActive = 'Y' ")
		  .append( "ORDER BY IsDefault DESC) ")
		  .append("UNION ")
		  // 3. Buscan por UPC en los art??culos asociados al proveedor
		  .append("(SELECT M_Product_ID, 0, <MT_M_Product_PO> ")
		  .append( "FROM M_Product_PO ")
		  .append( "WHERE (UPC <COMPARATOR> <VALUE>) AND IsActive = 'Y') ") 
		  .append("UNION ")
		  // 4. Busca por VendorProductNo en los art??culos asociados a entidades comerciales 
		  .append("(SELECT M_Product_ID, 0, <MT_C_BPartner_Product> ")
		  .append( "FROM C_BPartner_Product ")
		  .append( "WHERE (VendorProductNo <COMPARATOR> <VALUE>) AND isActive = 'Y') ");

	    String exactQuery = null;
	    String partialQuery = null;
	    
	    // La configuraci??n de TPV indica que hay que realizar una b??squeda exacta por UPC
	    if (getPoSCOnfig().isSearchByUPC()) {
	    	exactQuery = baseQuery.toString();
	    	// Reemplazada el comparador y el valor. Aqu?? se compara por igualdad exacta
	    	exactQuery = exactQuery.replaceAll("<COMPARATOR>", "=");
	    	exactQuery = exactQuery.replaceAll("<VALUE>", "?");
	    	// Reemplaza los Tipos de Matching
	    	exactQuery = exactQuery.replaceAll("<MT_M_Product_Upc_Instance>", String.valueOf(ProductList.MASI_UPC_EXACT_MATCH));
	    	exactQuery = exactQuery.replaceAll("<MT_M_ProductUPC>", String.valueOf(ProductList.UPC_EXACT_MATCH));
	    	exactQuery = exactQuery.replaceAll("<MT_M_Product_PO>", String.valueOf(ProductList.PO_UPC_EXACT_MATCH));
	    	exactQuery = exactQuery.replaceAll("<MT_C_BPartner_Product>", String.valueOf(ProductList.BP_CODE_EXACT_MATCH));
	    	query.append(exactQuery);
	    }
	    
	    // La configuraci??n de TPV indica que hay que realizar una b??squeda parcial por UPC
	    if (getPoSCOnfig().isSearchByUPCLike()) {
	    	partialQuery = baseQuery.toString();
	    	// Reemplazada el comparador y el valor. Aqu?? se compara por ILIKE para no tener 
	    	// en cuenta may??sculas y min??sculas, y solo se agrega un comod??n al final de la
	    	// cadena. Debido a que los UPC son com??nmente num??ricos, si se agregara un
	    	// comod??n al inicio tambi??n producir??a demasiados resultados desechables.
	    	partialQuery = partialQuery.replaceAll("<COMPARATOR>", "ILIKE");
	    	partialQuery = partialQuery.replaceAll("<VALUE>", "(? || '%')");
	    	// Reemplaza los Tipos de Matching
	    	partialQuery = partialQuery.replaceAll("<MT_M_Product_Upc_Instance>", String.valueOf(ProductList.MASI_UPC_PARTIAL_MATCH));
	    	partialQuery = partialQuery.replaceAll("<MT_M_ProductUPC>", String.valueOf(ProductList.UPC_PARTIAL_MATCH));
	    	partialQuery = partialQuery.replaceAll("<MT_M_Product_PO>", String.valueOf(ProductList.PO_UPC_PARTIAL_MATCH));
	    	partialQuery = partialQuery.replaceAll("<MT_C_BPartner_Product>", String.valueOf(ProductList.BP_CODE_PARTIAL_MATCH));
	    	// Si se requiri?? la b??squeda exacta es necesario unir los resultados con esta
	    	// b??squeda.
	    	if (exactQuery != null) {
	    		query.append("UNION ");
	    	}
	    	query.append(partialQuery);
	    }
		
		return query.toString();
	}
	
	/**
	 * @return Devuelve la sub-consulta para b??squeda exacta y/o parcial de art??culos
	 * por Clave de B??squeda
	 */
	private String getSearchByValueQuery() {
		StringBuffer query = new StringBuffer();
		// La consulta base contiene partes sin completar encerradas entre <>. Estos tags
		// son luego reemplazados por valores concretos seg??n la necesidad de hacer una 
		// b??squeda exacta y/o parcial. De esta forma la consulta se esribe una ??nica vez
		// y se reutiliza en ambas b??squedas.
		StringBuffer baseQuery = new StringBuffer();
	    baseQuery
		  // 1. Buscan por Value en la tabla de art??culos
		  .append("(SELECT M_Product_ID, 0 AS M_AttributeSetInstance_ID, <MT_M_Product> AS MatchType ")
		  .append( "FROM M_Product ")
		  .append( "WHERE (Value <COMPARATOR> <VALUE>) AND IsActive = 'Y') "); 

	    String exactQuery = null;
	    String partialQuery = null;
	    
	    // La configuraci??n de TPV indica que hay que realizar una b??squeda exacta por Clave
	    // de B??squeda
	    if (getPoSCOnfig().isSearchByValue()) {
	    	exactQuery = baseQuery.toString();
	    	// Reemplazada el comparador y el valor. Aqu?? se compara por igualdad exacta
	    	exactQuery = exactQuery.replaceAll("<COMPARATOR>", "=");
	    	exactQuery = exactQuery.replaceAll("<VALUE>", "?");
	    	// Reemplaza los Tipos de Matching
	    	exactQuery = exactQuery.replaceAll("<MT_M_Product>", String.valueOf(ProductList.VALUE_EXACT_MATCH));
	    	query.append(exactQuery);
	    }
	    
	    // La configuraci??n de TPV indica que hay que realizar una b??squeda parcial por UPC
	    if (getPoSCOnfig().isSearchByValueLike()) {
	    	partialQuery = baseQuery.toString();
	    	// Reemplazada el comparador y el valor. Aqu?? se compara por ILIKE para no tener 
	    	// en cuenta may??sculas y min??sculas, y solo se agrega un comod??n al final de la
	    	// cadena.
	    	partialQuery = partialQuery.replaceAll("<COMPARATOR>", "ILIKE");
	    	partialQuery = partialQuery.replaceAll("<VALUE>", "(? || '%')");
	    	// Reemplaza los Tipos de Matching
	    	partialQuery = partialQuery.replaceAll("<MT_M_Product>", String.valueOf(ProductList.VALUE_PARTIAL_MATCH));
	    	// Si se requiri?? la b??squeda exacta es necesario unir los resultados con esta
	    	// b??squeda.
	    	if (exactQuery != null) {
	    		query.append("UNION ");
	    	}
	    	query.append(partialQuery);
	    }
		
		return query.toString();
	}

	/**
	 * @return Devuelve la sub-consulta para b??squeda exacta y/o parcial de art??culos
	 * por Nombre
	 */
	private String getSearchByNameQuery() {
		StringBuffer query = new StringBuffer();
		// La consulta base contiene partes sin completar encerradas entre <>. Estos tags
		// son luego reemplazados por valores concretos seg??n la necesidad de hacer una 
		// b??squeda exacta y/o parcial. De esta forma la consulta se esribe una ??nica vez
		// y se reutiliza en ambas b??squedas.
		StringBuffer baseQuery = new StringBuffer();
	    baseQuery
		  // 1. Buscan por Value en la tabla de art??culos
		  .append("(SELECT M_Product_ID, 0 AS M_AttributeSetInstance_ID, <MT_M_Product> AS MatchType ")
		  .append( "FROM M_Product ")
		  .append( "WHERE (Name <COMPARATOR> <VALUE>) AND IsActive = 'Y') "); 

	    String exactQuery = null;
	    String partialQuery = null;
	    
	    // La configuraci??n de TPV indica que hay que realizar una b??squeda exacta por Clave
	    // de B??squeda
	    if (getPoSCOnfig().isSearchByName()) {
	    	exactQuery = baseQuery.toString();
	    	// Reemplazada el comparador y el valor. Aqu?? se compara por igualdad de strings
	    	// sin case sentsitive para generar mejores resultados debido a que los nombres
	    	// de art??culos no se diferencian por may??sculas o min??sculas sino por la propia
	    	// cadena de nombres.
	    	exactQuery = exactQuery.replaceAll("<COMPARATOR>", "ILIKE");
	    	exactQuery = exactQuery.replaceAll("<VALUE>", "?");
	    	// Reemplaza los Tipos de Matching
	    	exactQuery = exactQuery.replaceAll("<MT_M_Product>", String.valueOf(ProductList.NAME_EXACT_MATCH));
	    	query.append(exactQuery);
	    }
	    
	    // La configuraci??n de TPV indica que hay que realizar una b??squeda parcial por UPC
	    if (getPoSCOnfig().isSearchByNameLike()) {
	    	partialQuery = baseQuery.toString();
	    	// Reemplazada el comparador y el valor. Aqu?? se compara por ILIKE para no tener 
	    	// en cuenta may??sculas y min??sculas, y se agregan un comodines al inicio y 
	    	// final de la cadena.
	    	partialQuery = partialQuery.replaceAll("<COMPARATOR>", "ILIKE");
	    	partialQuery = partialQuery.replaceAll("<VALUE>", "('%' || ? || '%')");
	    	// Reemplaza los Tipos de Matching
	    	partialQuery = partialQuery.replaceAll("<MT_M_Product>", String.valueOf(ProductList.NAME_PARTIAL_MATCH));
	    	// Si se requiri?? la b??squeda exacta es necesario unir los resultados con esta
	    	// b??squeda.
	    	if (exactQuery != null) {
	    		query.append("UNION ");
	    	}
	    	query.append(partialQuery);
	    }
		
		return query.toString();
	}

	/**
	 * Devuelve la cantidad de par??metros que requiere una consulta SQL para se utilizada
	 * como una {@link PreparedStatement}. Simplemente cuenta la cantidad de caracteres 
	 * <code>?</code> que existen dentro del String.
	 * @param sql Consulta SQL origen
	 * @return Cantidad de parametros requeridos.
	 */
	private int parametersCount(String sql) {
		int count = 0;
		char[] chars = sql.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			char c = chars[i];
			if (c == '?') {
				count++;
			}
		}
		return count;
	}
	
	/**
	 * Imprime el comprobante para retiro de art??culos por almac??n en caso de estar
	 * indicada esta opci??n en la configuraci??n del TPV.
	 */
	private void printWarehouseDeliveryTicket(Order order) {
		/*
		 * Por el momento SOLO se imprime el comprobante mediante una Impresora Fiscal, con
		 * lo cual se deben cumplir estas condiciones:
		 * 1) Est?? activada la localizaci??n Argentina
		 * 2) El TPV est?? configurado para realizar la factura
		 * 3) El tipo de documento de la factura est?? configurado para que se emita
		 *    el comprobante mediante un controlador fiscal, teniendo este tipo asociada
		 *    la impresora fiscal a utilizar
		 * 
		 * Al cumplirse estas 3 condiciones, aqu?? se obtiene la impresora fiscal asociada
		 * al tipo de documento de la factura creada, y se emite el ticket para retiro x
		 * almac??n en esa impresora fiscal.   
		 */
		
		// El pedido tiene al menos un art??culo que se retira por almac??n, 
		// adem??s se cre?? la factura y el TPV est?? configurado para emitir el 
		// documento de retiro 		
		if (order.getWarehouseCheckoutProductsCount() > 0 
				&& invoice != null 
				&& getPoSCOnfig().isPrintWarehouseDeliverDocument()) {
			// El tipo de documento de la factura debe ser fiscal y tener asociado
			// un controlador fiscal.
			MDocType docType = MDocType.get(ctx, invoice.getC_DocType_ID());
			if (docType.isFiscal() && docType.getC_Controlador_Fiscal_ID() > 0) {
				// Impresor de comprobantes.
				FiscalDocumentPrint fdp = new FiscalDocumentPrint();
				fdp.addDocumentPrintListener(getFiscalDocumentPrintListener());
				fdp.setPrinterEventListener(getFiscalPrinterEventListener());

				if(!fdp.printDeliveryDocument(docType.getC_Controlador_Fiscal_ID(), morder)) {
					
				}

			}
		}
		
		// TODO: Codificar la impresi??n de comprobante mediante un informe com??n
		// es decir, un informe jasper o Libertya que muestre los art??culos que 
		// se deben retirar por almac??n. En este caso no es requerido que est??
		// habilitada la localizaci??n Argentina.
	}

	@Override
	public List<PaymentMedium> getPaymentMediums() {
		List<PaymentMedium> paymentMediums = new ArrayList<PaymentMedium>();
		// Se buscan los medios de pago que sean v??lidos para la fecha actual y
		// que su contexto de uso no sea Recibos de Cliente
		List<MPOSPaymentMedium> mPaymentMediums = MPOSPaymentMedium
				.getAvailablePaymentMediums(getCtx(), null,
						MPOSPaymentMedium.CONTEXT_CustomerReceiptsOnly, true,
						getTrxName());
		PaymentMedium paymentMedium = null;
		int discountSchemaID = 0;
		for (MPOSPaymentMedium mposPaymentMedium : mPaymentMediums) {
			// Crea la instancia del medio de pago con los datos del C_POSPaymentMedium
			paymentMedium = new PaymentMedium(mposPaymentMedium.getID(),
					mposPaymentMedium.getName(), mposPaymentMedium
							.getTenderType(), mposPaymentMedium
							.getC_Currency_ID());
			// Si tiene esquema de descuento se obtiene la instancia y se asocia al
			// medio de pago.
			discountSchemaID = mposPaymentMedium.getM_DiscountSchema_ID();
			if (discountSchemaID > 0) {
				paymentMedium.setDiscountSchema(getDiscountSchema(discountSchemaID));
			}
			// Si es un medio de pago de tipo Tarjeta de Cr??dito se guarda el ID de 
			// la entidad financiera y se cargan los planes de tarjeta disponibles
			// para la misma
			if (paymentMedium.isCreditCard()) {
				paymentMedium.setEntidadFinancieraID(mposPaymentMedium.getM_EntidadFinanciera_ID());
				paymentMedium.setCreditCardPlans(getCreditCardPlans(paymentMedium.getEntidadFinancieraID()));
			}
			
			// Si es un medio de pago de tipo Cheque se guarda el plazo de cobro
			if (paymentMedium.isCheck()) {
				paymentMedium.setCheckDeadLine(Integer
						.parseInt(mposPaymentMedium.getCheckDeadLine()));
			}
			
			// Cheques y Tarjetas pueden contener un banco (lista de referencia)
			// Se guarda el value en el objeto.
			if (paymentMedium.isCreditCard() || paymentMedium.isCheck()) {
				paymentMedium.setBank(mposPaymentMedium.getBank());
			}
			
			// Se asigna el nombre del tipo de pago a partir de la lista de referencia
			paymentMedium.setTenderTypeName(
				MRefList.getListName(
						ctx, 
						MPOSPaymentMedium.TENDERTYPE_AD_Reference_ID, 
						paymentMedium.getTenderType()
				)
			);
			
			paymentMediums.add(paymentMedium);
		}
		
		return paymentMediums;
	}

	/**
	 * Devuelve una lista con todos los planes v??lidos para la fecha actual de una
	 * Entidad Financiera.
	 * @param entidadFinancieraID ID de Entidad Financiera
	 * @return {@link List} de {@link EntidadFinancieraPlan}
	 */
	protected List<EntidadFinancieraPlan> getCreditCardPlans(int entidadFinancieraID) {
		List<EntidadFinancieraPlan> creditCardPlans = new ArrayList<EntidadFinancieraPlan>();
		// Se buscan los planes de la entidad financiera cuyo rango de validez
		// contenga la fecha actual.
		List<MEntidadFinancieraPlan> mCreditCardPlans = MEntidadFinancieraPlan
				.getPlansAvailables(getCtx(), entidadFinancieraID, getTrxName());
		EntidadFinancieraPlan plan = null;
		int discountSchemaID;
		for (MEntidadFinancieraPlan mEntidadFinancieraPlan : mCreditCardPlans) {
			// Crea la instancia del plan
			plan = new EntidadFinancieraPlan(
				mEntidadFinancieraPlan.getID(),
				entidadFinancieraID,
				mEntidadFinancieraPlan.getName(),
				mEntidadFinancieraPlan.getCuotasPago()
			);
			// Asocia el esquema de descuento si tiene
			discountSchemaID = mEntidadFinancieraPlan.getM_DiscountSchema_ID();
			if (discountSchemaID > 0) {
				plan.setDiscountSchema(getDiscountSchema(discountSchemaID));
			}
			creditCardPlans.add(plan);
		}	
		return creditCardPlans;
	}

	@Override
	public BigDecimal getCreditAvailableAmount(int invoiceID) {
		try {
			BigDecimal AvailableamountToConvert = 
				(BigDecimal)DB.getSQLObject(null, "SELECT invoiceOpen(?, 0)", new Object[] { invoiceID });
			return currencyConvert(AvailableamountToConvert, DB.getSQLValue(null, "SELECT C_Currency_ID From C_Invoice where C_Invoice_ID = " + invoiceID));
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * @return the ctx
	 */
	public Properties getCtx() {
		return ctx;
	}
	
	/**
	 * Efectua el guardado a la BD de los descuentos aplicados al pedido TPV
	 * @param order Pedido TPV.
	 */
	private void saveDiscounts(Order order) throws PosException {
		throwIfFalse(order.getDiscountCalculator().saveDiscounts(getTrxName()));
	}
	
	/**
	 * Busca la lista de Cajas Diarias abiertas para un usuario en una organizaci??n 
	 * determinada, indicando adem??s la fecha de validez de la misma.
	 * @param orgID ID de organizaci??n
	 * @param userID ID de usuario
	 * @param date Fecha de la caja
	 * @return Lista de Configs encontrados.
	 */
	private List<PoSConfig> getJournalPOSConfigs(int orgID, int userID, Timestamp date) {
		List<PoSConfig> configs = new ArrayList<PoSConfig>();
		
		String sql =
			"SELECT * " +
			"FROM C_POSJournal " +
			"WHERE AD_Org_ID = ? " +
			  "AND AD_User_ID = ? " +
			  "AND DateTrx = ? " +
			  "AND DocStatus = ? ";
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			pstmt = DB.prepareStatement(sql);
			int i = 1;
			pstmt.setInt(i++, orgID);
			pstmt.setInt(i++, userID);
			pstmt.setTimestamp(i++, date);
			pstmt.setString(i++, MPOSJournal.DOCSTATUS_Opened);
			
			rs = pstmt.executeQuery();
			MPOSJournal journal = null;
			while (rs.next()) {
				journal = new MPOSJournal(getCtx(), rs, null);
				if (journal.getC_POS_ID() > 0) {
					configs.add(new PoSConfig(journal));
				}
			}
			
		} catch (SQLException e) {
			log.log(Level.SEVERE, "POS: Error getting POS Journals", e);
		}
		
		
		return configs;
	}

	public void setMCashLines(Map<Integer, MCashLine> cashLines) {
		this.mCashLines = cashLines;
	}

	public Map<Integer, MCashLine> getMCashLines() {
		return mCashLines;
	}

	public void setAditionalWorkResults(Map<PO, Object> aditionalWorkResults) {
		this.aditionalWorkResults = aditionalWorkResults;
	}

	public Map<PO, Object> getAditionalWorkResults() {
		return aditionalWorkResults;
	}
	
	@Override
	public List<PaymentTerm> getPaymentTerms() {
		String sql = "SELECT c_paymentterm_id, name, c_pospaymentmedium_id FROM c_paymentterm WHERE ad_client_id = ? AND isactive = 'Y'";
		PreparedStatement ps = null;
		ResultSet rs = null;
		List<PaymentTerm> pts = new ArrayList<PaymentTerm>();
		PaymentTerm pt = null;
		try {
			ps = DB.prepareStatement(sql, getTrxName());
			ps.setInt(1, Env.getAD_Client_ID(getCtx()));
			rs = ps.executeQuery();
			while(rs.next()){
				pt = new PaymentTerm(rs.getInt("c_paymentterm_id"), rs
						.getString("name"), rs.getInt("c_pospaymentmedium_id"));
				pts.add(pt);
				paymentTerms.put(pt.getId(), pt);
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "POS: Error getting Payment Terms", e);
		} finally{
			try {
				if(ps != null)ps.close();
				if(rs != null)rs.close();	
			} catch (Exception e2) {
				log.log(Level.SEVERE, "POS: Error getting Payment Terms", e2);
			}			
		}
		return pts;
	}

	@Override
	public PaymentTerm getPaymentTerm(int paymentTermID) {
		// Si no hay esquemas de vencimientos cargados entonces los cargo
		if(paymentTerms == null || paymentTerms.size() == 0){
			getPaymentTerms();
		}
		return paymentTerms.get(paymentTermID);
	}
	
	@Override
	public PaymentMedium getPaymentMedium(Integer paymentMediumID) {
		PaymentMedium pm = null;
		if(!Util.isEmpty(paymentMediumID, true)){
			MPOSPaymentMedium posPM = new MPOSPaymentMedium(getCtx(),
					paymentMediumID, getTrxName());
			pm = new PaymentMedium(posPM.getID(), posPM.getName(), posPM
					.getTenderType(), posPM.getC_Currency_ID());
		}
		return pm;
	}
	
	private void debug(String text) {
		System.out.println("TPV DEBUG ==> "
				+ DB.getSQLValueTimestamp(null, "select now()") + " - " + text);
	}
	
	
	/**
	 * @return Crea un nuevo nombre de transacci??n
	 */
	protected String createTrxName() {
		return Trx.createTrxName(this.toString() + invoiceDate.toString()
				+ Thread.currentThread().getId());
	}

	@Override
	public void voidDocuments() throws PosException {
		/*
		 * PRECONDICION: se asume que el TPV ha generado la factura ya que una
		 * anulaci??n de documentos solo es realizable en caso de no haber podido
		 * emitir la factura mediante el controlador fiscal.
		 */
		String trxName = createTrxName();

		try {
//			// Primero anulamos la imputaci??n
//			if (allocHdr != null && allocHdr.getC_AllocationHdr_ID() > 0) {
//				MAllocationHdr vAlloc = new MAllocationHdr(getCtx(), allocHdr.getC_AllocationHdr_ID(), trxName);
//				vAlloc.setAllocationAction(MAllocationHdr.ALLOCATIONACTION_VoidPayments);
//				if (!DocumentEngine.processAndSave(vAlloc, MAllocationHdr.ACTION_Void, false)) {
//					throw new PosException(vAlloc.getProcessMsg());
//				}
//			}
//			
//			// Luego anulamos el remito (si existe)
//			if (shipment != null && shipment.getM_InOut_ID() > 0) {
//				MInOut vShipment = new MInOut(getCtx(), shipment.getM_InOut_ID(), trxName);
//				if (!DocumentEngine.processAndSave(vShipment, MInOut.ACTION_Void, false)) {
//					throw new PosException(vShipment.getProcessMsg());
//				}
//			}
//			
//			// Luego la factura
//			if (invoice != null && invoice.getC_Invoice_ID() > 0) {
//				MInvoice vInvoice = new MInvoice(getCtx(), invoice.getC_Invoice_ID(), trxName);
//				if (!DocumentEngine.processAndSave(vInvoice, MInvoice.ACTION_Void, false)) {
//					throw new PosException(vInvoice.getProcessMsg());
//				}
//			}
//			
//			// Finalmente el Pedido
//			if (morder != null && morder.getC_Order_ID() > 0) {
//				MOrder vOrder = new MOrder(getCtx(), morder.getC_Order_ID(), trxName);
//				if (!DocumentEngine.processAndSave(vOrder, MOrder.ACTION_Void, false)) {
//					throw new PosException(vOrder.getProcessMsg());
//				}
//			}
			
			
			InvoiceGlobalVoiding voidingProcess = new InvoiceGlobalVoiding(
					invoice.getC_Invoice_ID(), getCtx(), trxName);
			
			voidingProcess.start();
			
			Trx.getTrx(trxName).commit();
			
		} catch (Exception e) {
			Trx.getTrx(trxName).rollback();
			throw new PosException(Msg.parseTranslation(getCtx(), e.getMessage()));
		}
	}
	
	//Ader: manejo de caches multi-documento
	private MProductCache m_prodCache;
	private MProductCache getProdCache()
	{
		return m_prodCache;
	}
	private void initCachesFromOrder(Order o)
	{
		//por ahora solo productos
		initCacheProdFromOrder(o);

    	
	}
	
	private void setCaches(MOrder mo)
	{
		//por ahora solo productos
		mo.setProductCache(m_prodCache);
	}
	private boolean initCacheProdFromOrder(Order o)
	{
		m_prodCache = new MProductCache(ctx,trxName);
    	List<Integer> newIds = new ArrayList<Integer>();
    	for (OrderProduct op: o.getOrderProducts())
    	{
    		int M_Product_ID = op.getProduct().getId();
    		if (M_Product_ID <= 0)
    			continue;
    		if (m_prodCache.contains(M_Product_ID))
    			continue;
    		if (newIds.contains(M_Product_ID))
    		    continue;
    		newIds.add(M_Product_ID);
    	}
    	
    	if (newIds.size() <= 0)
    		return true; 
    	
    	//carga masiva en cache; un solo acceso a DB
    	int qtyCached = m_prodCache.loadMasive(newIds);
    	
    	if (qtyCached != newIds.size())
    		return false; //algunos no se cargaron...
    	return true;
	}
	private MProduct getProductFromCache(int M_Product_ID)
	{
	   	if (m_prodCache == null)
     		m_prodCache = new MProductCache(getCtx(),trxName);
    	if (M_Product_ID <=0)
    		return null;
    	MProduct p = m_prodCache.get(M_Product_ID);
    	return p;
	}
}

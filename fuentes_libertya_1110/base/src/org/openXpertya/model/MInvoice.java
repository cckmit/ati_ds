/*
 *    El contenido de este fichero está sujeto a la  Licencia Pública openXpertya versión 1.1 (LPO)
 * en tanto en cuanto forme parte íntegra del total del producto denominado:  openXpertya, solución 
 * empresarial global , y siempre según los términos de dicha licencia LPO.
 *    Una copia  íntegra de dicha  licencia está incluida con todas  las fuentes del producto.
 *    Partes del código son CopyRight (c) 2002-2007 de Ingeniería Informática Integrada S.L., otras 
 * partes son  CopyRight (c) 2002-2007 de  Consultoría y  Soporte en  Redes y  Tecnologías  de  la
 * Información S.L.,  otras partes son  adaptadas, ampliadas,  traducidas, revisadas  y/o mejoradas
 * a partir de código original de  terceros, recogidos en el  ADDENDUM  A, sección 3 (A.3) de dicha
 * licencia  LPO,  y si dicho código es extraido como parte del total del producto, estará sujeto a
 * su respectiva licencia original.  
 *     Más información en http://www.openxpertya.org/ayuda/Licencia.html
 */

package org.openXpertya.model;

import java.io.File;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

import org.openXpertya.cc.CurrentAccountBalanceStrategy;
import org.openXpertya.cc.CurrentAccountManager;
import org.openXpertya.cc.CurrentAccountManagerFactory;
import org.openXpertya.print.ReportEngine;
import org.openXpertya.process.DocAction;
import org.openXpertya.process.DocumentEngine;
import org.openXpertya.process.ProcessorWSFE;
import org.openXpertya.reflection.CallResult;
import org.openXpertya.util.AuxiliarDTO;
import org.openXpertya.util.CCache;
import org.openXpertya.util.CLogger;
import org.openXpertya.util.DB;
import org.openXpertya.util.Env;
import org.openXpertya.util.MProductCache;
import org.openXpertya.util.MeasurableTask;
import org.openXpertya.util.Msg;
import org.openXpertya.util.StringUtil;
import org.openXpertya.util.TimeStatsLogger;
import org.openXpertya.util.Util;

/**
 * Descripción de Clase
 * 
 * 
 * @version 2.1, 02.07.07
 * @author Equipo de Desarrollo de openXpertya
 */

public class MInvoice extends X_C_Invoice implements DocAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// Adder: POSSimple - Omision de pre y before Save
	public boolean skipAfterAndBeforeSave = false;

	/**
	 * Bypass para actualización del descuento manual general de la factura
	 * (Sólo para Facturas de Cliente, no TPV)
	 */
	private boolean skipManualGeneralDiscount = false;

	/**
	 * Bypass para aplicación del esquema de vencimientos
	 */
	private boolean skipApplyPaymentTerm = false;
	
	/**
	 * Descripción de Método
	 * 
	 * 
	 * @param ctx
	 * @param C_BPartner_ID
	 * @param trxName
	 * 
	 * @return
	 */

	public static MInvoice[] getOfBPartner(Properties ctx, int C_BPartner_ID,
			String trxName) {
		ArrayList list = new ArrayList();
		String sql = "SELECT * FROM C_Invoice WHERE C_BPartner_ID=?";
		PreparedStatement pstmt = null;

		try {
			pstmt = DB.prepareStatement(sql, trxName);
			pstmt.setInt(1, C_BPartner_ID);

			ResultSet rs = pstmt.executeQuery();

			while (rs.next()) {
				list.add(new MInvoice(ctx, rs, trxName));
			}

			rs.close();
			pstmt.close();
			pstmt = null;
		} catch (Exception e) {
			s_log.log(Level.SEVERE, "getOfBPartner", e);
		}

		try {
			if (pstmt != null) {
				pstmt.close();
			}

			pstmt = null;
		} catch (Exception e) {
			pstmt = null;
		}

		//

		MInvoice[] retValue = new MInvoice[list.size()];

		list.toArray(retValue);

		return retValue;
	} // getOfBPartner

	/**
	 * Descripción de Método
	 * 
	 * 
	 * @param from
	 * @param dateDoc
	 * @param C_DocTypeTarget_ID
	 * @param isSOTrx
	 * @param counter
	 * @param trxName
	 * @param setOrder
	 * 
	 * @return
	 */

	public static MInvoice copyFrom(MInvoice from, Timestamp dateDoc,
			int C_DocTypeTarget_ID, boolean isSOTrx, boolean counter,
			String trxName, boolean setOrder) {
		MInvoice to = new MInvoice(from.getCtx(), 0, null);

		to.set_TrxName(trxName);
		PO.copyValues(from, to, from.getAD_Client_ID(), from.getAD_Org_ID());
		to.setC_Invoice_ID(0);
		to.set_ValueNoCheck("DocumentNo", null);
		to.setIsCopy(true);

		//

		to.setDocStatus(DOCSTATUS_Drafted); // Draft
		to.setDocAction(DOCACTION_Complete);

		//

		to.setC_DocType_ID(0);
		to.setC_DocTypeTarget_ID(C_DocTypeTarget_ID);
		to.setIsSOTrx(isSOTrx);

		//

		to.setDateInvoiced(dateDoc);
		to.setDateAcct(dateDoc);
		to.setDatePrinted(null);
		to.setIsPrinted(false);

		//

		to.setIsApproved(false);
		to.setC_Payment_ID(0);
		to.setC_CashLine_ID(0);
		to.setIsPaid(false);
		to.setIsInDispute(false);

		//
		// Amounts are updated by trigger when adding lines

		to.setGrandTotal(Env.ZERO);
		to.setTotalLines(Env.ZERO);

		//

		to.setIsTransferred(false);
		to.setPosted(false);
		to.setProcessed(false);

		// delete references

		to.setIsSelfService(false);

		if (!setOrder) {
			to.setC_Order_ID(0);
		}

		if (counter) {
			to.setRef_Invoice_ID(from.getC_Invoice_ID());

			// Try to find Order link

			if (from.getC_Order_ID() != 0) {
				MOrder peer = new MOrder(from.getCtx(), from.getC_Order_ID(),
						from.get_TrxName());

				if (peer.getRef_Order_ID() != 0) {
					to.setC_Order_ID(peer.getRef_Order_ID());
				}
			}
		} else {
			to.setRef_Invoice_ID(0);
		}

		if (!to.save(trxName)) {
			throw new IllegalStateException("Could not create Invoice");
		}

		if (counter) {
			from.setRef_Invoice_ID(to.getC_Invoice_ID());
		}

		// Lines

		if (to.copyLinesFrom(from, counter, setOrder) == 0) {
			throw new IllegalStateException("Could not create Invoice Lines");
		}

		return to;
	} // copyFrom

	/**
	 * Descripción de Método
	 * 
	 * 
	 * @param documentDir
	 * @param C_Invoice_ID
	 * 
	 * @return
	 */

	public static String getPDFFileName(String documentDir, int C_Invoice_ID) {
		StringBuffer sb = new StringBuffer(documentDir);

		if (sb.length() == 0) {
			sb.append(".");
		}

		if (!sb.toString().endsWith(File.separator)) {
			sb.append(File.separator);
		}

		sb.append("C_Invoice_ID_").append(C_Invoice_ID).append(".pdf");

		return sb.toString();
	} // getPDFFileName

	/**
	 * Descripción de Método
	 * 
	 * 
	 * @param ctx
	 * @param C_Invoice_ID
	 * @param trxName
	 * 
	 * @return
	 */

	public static MInvoice get(Properties ctx, int C_Invoice_ID, String trxName) {
		Integer key = new Integer(C_Invoice_ID);
		MInvoice retValue = (MInvoice) s_cache.get(key);

		if (retValue != null) {
			return retValue;
		}

		retValue = new MInvoice(ctx, C_Invoice_ID, trxName);

		if (retValue.getID() != 0) {
			s_cache.put(key, retValue);
		}

		return retValue;
	} // get

	/** Descripción de Campos */

	private static CCache s_cache = new CCache("C_Invoice", 20, 2); // 2 minutes

	/**
	 * Constructor de la clase ...
	 * 
	 * 
	 * @param ctx
	 * @param C_Invoice_ID
	 * @param trxName
	 */

	public MInvoice(Properties ctx, int C_Invoice_ID, String trxName) {
		super(ctx, C_Invoice_ID, trxName);

		if (C_Invoice_ID == 0) {
			setDocStatus(DOCSTATUS_Drafted); // Draft
			setDocAction(DOCACTION_Complete);

			//

			setPaymentRule(PAYMENTRULE_OnCredit); // Payment Terms
			setDateInvoiced(new Timestamp(System.currentTimeMillis()));
			setDateAcct(new Timestamp(System.currentTimeMillis()));

			//

			setChargeAmt(Env.ZERO);
			setTotalLines(Env.ZERO);
			setGrandTotal(Env.ZERO);

			//

			setIsSOTrx(true);
			setIsTaxIncluded(false);
			setIsApproved(false);
			setIsDiscountPrinted(false);
			setIsPaid(false);
			setSendEMail(false);
			setIsPrinted(false);
			setIsTransferred(false);
			setIsSelfService(false);
			setIsPayScheduleValid(false);
			setIsInDispute(false);
			setPosted(false);
			super.setProcessed(false);
			setProcessing(false);
		}
	} // MInvoice

	/**
	 * Constructor de la clase ...
	 * 
	 * 
	 * @param ctx
	 * @param rs
	 * @param trxName
	 */

	public MInvoice(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	} // MInvoice

	/**
	 * Constructor de la clase ...
	 * 
	 * 
	 * @param order
	 * @param C_DocTypeTarget_ID
	 * @param invoiceDate
	 */

	public MInvoice(MOrder order, int C_DocTypeTarget_ID, Timestamp invoiceDate) {
		this(order.getCtx(), 0, order.get_TrxName());
		setClientOrg(order);
		setOrder(order); // set base settings

		//

		if (C_DocTypeTarget_ID == 0) {
			C_DocTypeTarget_ID = DB
					.getSQLValue(
							null,
							"SELECT C_DocTypeInvoice_ID FROM C_DocType WHERE C_DocType_ID=?",
							order.getC_DocType_ID());
		}

		setC_DocTypeTarget_ID(C_DocTypeTarget_ID);

		if (invoiceDate != null) {
			setDateInvoiced(invoiceDate);
		}

		setDateAcct(getDateInvoiced());

		//

		setSalesRep_ID(order.getSalesRep_ID());

		//

		setC_BPartner_ID(order.getBill_BPartner_ID());
		setC_BPartner_Location_ID(order.getBill_Location_ID());
		setAD_User_ID(order.getBill_User_ID());
	} // MInvoice

	/**
	 * Constructor de la clase ...
	 * 
	 * 
	 * @param ship
	 * @param invoiceDate
	 */

	public MInvoice(MInOut ship, Timestamp invoiceDate) {
		this(ship.getCtx(), 0, ship.get_TrxName());
		setClientOrg(ship);
		setShipment(ship); // set base settings

		//

		setC_DocTypeTarget_ID();

		if (invoiceDate != null) {
			setDateInvoiced(invoiceDate);
		}

		setDateAcct(getDateInvoiced());

		//

		setSalesRep_ID(ship.getSalesRep_ID());
		setAD_User_ID(ship.getAD_User_ID());
	} // MInvoice

	/**
	 * Constructor de la clase ...
	 * 
	 * 
	 * @param batch
	 * @param line
	 */

	public MInvoice(MInvoiceBatch batch, MInvoiceBatchLine line) {
		this(line.getCtx(), 0, line.get_TrxName());
		setClientOrg(line);
		setDocumentNo(line.getDocumentNo());

		//

		setIsSOTrx(batch.isSOTrx());

		MBPartner bp = new MBPartner(line.getCtx(), line.getC_BPartner_ID(),
				line.get_TrxName());

		setBPartner(bp); // defaults

		//

		setIsTaxIncluded(line.isTaxIncluded());

		// May conflict with default price list

		setC_Currency_ID(batch.getC_Currency_ID());
		setC_ConversionType_ID(batch.getC_ConversionType_ID());

		//
		// setPaymentRule(order.getPaymentRule());
		// setC_PaymentTerm_ID(order.getC_PaymentTerm_ID());
		// setPOReference("");

		setDescription(batch.getDescription());

		// setDateOrdered(order.getDateOrdered());
		//

		setAD_OrgTrx_ID(line.getAD_OrgTrx_ID());
		setC_Project_ID(line.getC_Project_ID());

		// setC_Campaign_ID(line.getC_Campaign_ID());

		setC_Activity_ID(line.getC_Activity_ID());
		setUser1_ID(line.getUser1_ID());
		setUser2_ID(line.getUser2_ID());

		//

		setC_DocTypeTarget_ID(line.getC_DocType_ID());
		setDateInvoiced(line.getDateInvoiced());
		setDateAcct(line.getDateAcct());

		//

		setSalesRep_ID(batch.getSalesRep_ID());

		//

		setC_BPartner_ID(line.getC_BPartner_ID());
		setC_BPartner_Location_ID(line.getC_BPartner_Location_ID());
		setAD_User_ID(line.getAD_User_ID());
	} // MInvoice

	/** Descripción de Campos */

	private BigDecimal m_openAmt = null;

	/** Descripción de Campos */

	private MInvoiceLine[] m_lines;

	/** Descripción de Campos */

	private MInvoiceTax[] m_taxes;

	/** Descripción de Campos */

	private static CLogger s_log = CLogger.getCLogger(MInvoice.class);

	private static Map<String, String> reverseDocTypes;

	/**
	 * Booleano que determina si esta factura ya fue verificada a nivel cuenta
	 * corriente de la entidad comercial
	 */
	private boolean isCurrentAccountVerified = false;

	/**
	 * Booleano que determina si al completar esta factura se debe actualizar el
	 * saldo de cuenta corriente del cliente
	 */
	private boolean updateBPBalance = true;

	/**
	 * Resultado de la llamada de cuenta corriente que realiza trabajo adicional
	 * al procesar un documento. Al anular un invoice y crearse un documento
	 * reverso, se debe guardar dentro de esta map también.
	 */	
	private Map<PO, Object> aditionalWorkResult;

	/**
	 * Booleano que determina si se debe confimar el trabajo adicional de cuenta
	 * corriente al procesar el/los documento/s
	 */
	private boolean confirmAditionalWorks = true;
	
	
	
	static {
		// Se inicializan los tabla de tipos de documentos para la cancelación
		// de documentos en la localización argentina.
		reverseDocTypes = new HashMap<String, String>();
		reverseDocTypes.put(MDocType.DOCTYPE_CustomerInvoice,
				MDocType.DOCTYPE_CustomerCreditNote);
		reverseDocTypes.put(MDocType.DOCTYPE_CustomerDebitNote,
				MDocType.DOCTYPE_CustomerCreditNote);
		reverseDocTypes.put(MDocType.DOCTYPE_CustomerCreditNote,
				MDocType.DOCTYPE_CustomerDebitNote);
	}

	/**
	 * Descripción de Método
	 * 
	 * 
	 * @param AD_Client_ID
	 * @param AD_Org_ID
	 */

	public void setClientOrg(int AD_Client_ID, int AD_Org_ID) {
		super.setClientOrg(AD_Client_ID, AD_Org_ID);
	} // setClientOrg

	/**
	 * Descripción de Método
	 * 
	 * 
	 * @param bp
	 */

	public void setBPartner(MBPartner bp) {
		if (bp == null) {
			return;
		}

		setC_BPartner_ID(bp.getC_BPartner_ID());

		// Set Defaults

		int ii = 0;

		if (isSOTrx()) {
			ii = bp.getC_PaymentTerm_ID();
		} else {
			ii = bp.getPO_PaymentTerm_ID();
		}

		if (ii != 0) {
			setC_PaymentTerm_ID(ii);
		}

		//

		if (isSOTrx()) {
			ii = bp.getM_PriceList_ID();
		} else {
			ii = bp.getPO_PriceList_ID();
		}

		if (ii != 0) {
			setM_PriceList_ID(ii);
		}

		//

		String ss = bp.getPaymentRule();

		if (ss != null) {
			setPaymentRule(ss);
		}

		// Set Locations

		MBPartnerLocation[] locs = bp.getLocations(false);

		if (locs != null) {
			for (int i = 0; i < locs.length; i++) {
				if ((locs[i].isBillTo() && isSOTrx())
						|| (locs[i].isPayFrom() && !isSOTrx())) {
					setC_BPartner_Location_ID(locs[i]
							.getC_BPartner_Location_ID());
				}
			}

			// set to first

			if ((getC_BPartner_Location_ID() == 0) && (locs.length > 0)) {
				setC_BPartner_Location_ID(locs[0].getC_BPartner_Location_ID());
			}
		}

		if (getC_BPartner_Location_ID() == 0) {
			log.log(Level.SEVERE, "Has no To Address: " + bp);
		}

		// Set Contact

		MUser[] contacts = bp.getContacts(false);

		if ((contacts != null) && (contacts.length > 0)) { // get first User
			setAD_User_ID(contacts[0].getAD_User_ID());
		}
	} // setBPartner

	/**
	 * Descripción de Método
	 * 
	 * 
	 * @param order
	 */

	public void setOrder(MOrder order) {
		if (order == null) {
			return;
		}

		setC_Order_ID(order.getC_Order_ID());
		setIsSOTrx(order.isSOTrx());
		setIsDiscountPrinted(order.isDiscountPrinted());
		setIsSelfService(order.isSelfService());
		setSendEMail(order.isSendEMail());

		//

		setM_PriceList_ID(order.getM_PriceList_ID());
		setIsTaxIncluded(order.isTaxIncluded());
		setC_Currency_ID(order.getC_Currency_ID());
		setC_ConversionType_ID(order.getC_ConversionType_ID());

		//

		setPaymentRule(order.getPaymentRule());
		setC_PaymentTerm_ID(order.getC_PaymentTerm_ID());
		setPOReference(order.getPOReference());
		setDescription(order.getDescription());
		setDateOrdered(order.getDateOrdered());

		//

		setAD_OrgTrx_ID(order.getAD_OrgTrx_ID());
		setC_Project_ID(order.getC_Project_ID());
		setC_Campaign_ID(order.getC_Campaign_ID());
		setC_Activity_ID(order.getC_Activity_ID());
		setUser1_ID(order.getUser1_ID());
		setUser2_ID(order.getUser2_ID());
	} // setOrder

	/**
	 * Descripción de Método
	 * 
	 * 
	 * @param ship
	 */

	public void setShipment(MInOut ship) {
		if (ship == null) {
			return;
		}

		setIsSOTrx(ship.isSOTrx());

		//

		MBPartner bp = new MBPartner(getCtx(), ship.getC_BPartner_ID(), null);

		setBPartner(bp);

		//

		setSendEMail(ship.isSendEMail());

		//

		setPOReference(ship.getPOReference());
		setDescription(ship.getDescription());
		setDateOrdered(ship.getDateOrdered());

		//

		setAD_OrgTrx_ID(ship.getAD_OrgTrx_ID());
		setC_Project_ID(ship.getC_Project_ID());
		setC_Campaign_ID(ship.getC_Campaign_ID());
		setC_Activity_ID(ship.getC_Activity_ID());
		setUser1_ID(ship.getUser1_ID());
		setUser2_ID(ship.getUser2_ID());

		//

		if (ship.getC_Order_ID() != 0) {
			setC_Order_ID(ship.getC_Order_ID());

			MOrder order = new MOrder(getCtx(), ship.getC_Order_ID(),
					get_TrxName());

			setIsDiscountPrinted(order.isDiscountPrinted());
			setM_PriceList_ID(order.getM_PriceList_ID());
			setIsTaxIncluded(order.isTaxIncluded());
			setC_Currency_ID(order.getC_Currency_ID());
			setC_ConversionType_ID(order.getC_ConversionType_ID());
			setPaymentRule(order.getPaymentRule());
			setC_PaymentTerm_ID(order.getC_PaymentTerm_ID());

			//

			MDocType dt = MDocType.get(getCtx(), order.getC_DocType_ID());

			setC_DocTypeTarget_ID(dt.getC_DocTypeInvoice_ID());

			// Overwrite Invoice Address

			setC_BPartner_Location_ID(order.getBill_Location_ID());
		}
	} // setOrder

	/**
	 * Descripción de Método
	 * 
	 * 
	 * @param DocBaseType
	 */

	public void setC_DocTypeTarget_ID(String DocBaseType) {
		String sql = "SELECT C_DocType_ID FROM C_DocType "
				+ "WHERE AD_Client_ID=? AND DocBaseType=? "
				+ "ORDER BY IsDefault DESC";
		int C_DocType_ID = DB.getSQLValue(null, sql, getAD_Client_ID(),
				DocBaseType);

		if (C_DocType_ID <= 0) {
			log.log(Level.SEVERE,
					"setC_DocTypeTarget_ID - Not found for AC_Client_ID="
							+ getAD_Client_ID() + " - " + DocBaseType);
		} else {
			log.fine("setC_DocTypeTarget_ID - " + DocBaseType);
			setC_DocTypeTarget_ID(C_DocType_ID);

			boolean isSOTrx = MDocType.DOCBASETYPE_ARInvoice
					.equals(DocBaseType)
					|| MDocType.DOCBASETYPE_ARCreditMemo.equals(DocBaseType);

			setIsSOTrx(isSOTrx);
		}
	} // setC_DocTypeTarget_ID

	/**
	 * Descripción de Método
	 * 
	 */

	public void setC_DocTypeTarget_ID() {
		if (getC_DocTypeTarget_ID() > 0) {
			return;
		}

		if (isSOTrx()) {
			setC_DocTypeTarget_ID(MDocType.DOCBASETYPE_ARInvoice);
		} else {
			setC_DocTypeTarget_ID(MDocType.DOCBASETYPE_APInvoice);
		}
	} // setC_DocTypeTarget_ID

	/**
	 * Descripción de Método
	 * 
	 * 
	 * @param creditMemoAdjusted
	 * 
	 * @return
	 */

	public BigDecimal getGrandTotal(boolean creditMemoAdjusted) {
		if (!creditMemoAdjusted) {
			return super.getGrandTotal();
		}

		//

		BigDecimal amt = getGrandTotal();

		if (isCreditMemo()) {
			return amt.negate();
		}

		return amt;
	} // getGrandTotal

	/**
	 * Descripción de Método
	 * 
	 * 
	 * @param whereClause
	 * 
	 * @return
	 */

	private MInvoiceLine[] getLines(String whereClause) {
		ArrayList list = new ArrayList();
		String sql = "SELECT * FROM C_InvoiceLine WHERE C_Invoice_ID=? ";

		if (whereClause != null) {
			sql += whereClause;
		}

		sql += " ORDER BY Line";

		PreparedStatement pstmt = null;

		try {
			pstmt = DB.prepareStatement(sql, get_TrxName());
			pstmt.setInt(1, getC_Invoice_ID());

			ResultSet rs = pstmt.executeQuery();

			while (rs.next()) {
				MInvoiceLine il = new MInvoiceLine(getCtx(), rs, get_TrxName());

				il.setInvoice(this);
				list.add(il);
			}

			rs.close();
			pstmt.close();
			pstmt = null;
		} catch (Exception e) {
			log.log(Level.SEVERE, "getLines", e);
		} finally {
			try {
				if (pstmt != null) {
					pstmt.close();
				}
			} catch (Exception e) {
			}

			pstmt = null;
		}

		//

		MInvoiceLine[] lines = new MInvoiceLine[list.size()];

		list.toArray(lines);

		return lines;
	} // getLines

	/**
	 * Descripción de Método
	 * 
	 * 
	 * @param requery
	 * 
	 * @return
	 */

	public MInvoiceLine[] getLines(boolean requery) {
		if ((m_lines == null) || (m_lines.length == 0) || requery) {
			m_lines = getLines(null);
		}

		return m_lines;
	} // getLines

	/**
	 * Descripción de Método
	 * 
	 * 
	 * @return
	 */

	public MInvoiceLine[] getLines() {
		return getLines(false);
	} // getLines

	/**
	 * Descripción de Método
	 * 
	 * 
	 * @param step
	 */

	public void renumberLines(int step) {
		int number = step;
		MInvoiceLine[] lines = getLines(false);

		for (int i = 0; i < lines.length; i++) {
			MInvoiceLine line = lines[i];

			line.setLine(number);
			line.save();
			number += step;
		}

		m_lines = null;
	} // renumberLines

	/**
	 * Descripción de Método
	 * 
	 * 
	 * @param otherInvoice
	 * @param counter
	 * @param setOrder
	 * 
	 * @return
	 */

	public int copyLinesFrom(MInvoice otherInvoice, boolean counter,
			boolean setOrder) {
		if (isProcessed() || isPosted() || (otherInvoice == null)) {
			return 0;
		}

		MInvoiceLine[] fromLines = otherInvoice.getLines(false);
		int count = 0;

		for (int i = 0; i < fromLines.length; i++) {
			MInvoiceLine line = new MInvoiceLine(getCtx(), 0, get_TrxName());

			PO
					.copyValues(fromLines[i], line, getAD_Client_ID(),
							getAD_Org_ID());
			line.setC_Invoice_ID(getC_Invoice_ID());
			line.setInvoice(this);
			line.setC_InvoiceLine_ID(0); // new

			// Reset

			if (!setOrder) {
				line.setC_OrderLine_ID(0);
			}

			line.setRef_InvoiceLine_ID(0);
			line.setM_InOutLine_ID(0);
			line.setA_Asset_ID(0);
			line.setM_AttributeSetInstance_ID(0);
			line.setS_ResourceAssignment_ID(0);

			// New Tax

			if (getC_BPartner_ID() != otherInvoice.getC_BPartner_ID()) {
				line.setTax(); // recalculate
			}

			//

			if (counter) {
				line.setRef_InvoiceLine_ID(fromLines[i].getC_InvoiceLine_ID());

				if (fromLines[i].getC_OrderLine_ID() != 0) {
					MOrderLine peer = new MOrderLine(getCtx(), fromLines[i]
							.getC_OrderLine_ID(), get_TrxName());

					if (peer.getRef_OrderLine_ID() != 0) {
						line.setC_OrderLine_ID(peer.getRef_OrderLine_ID());
					}
				}

				line.setM_InOutLine_ID(0);

				if (fromLines[i].getM_InOutLine_ID() != 0) {
					MInOutLine peer = new MInOutLine(getCtx(), fromLines[i]
							.getM_InOutLine_ID(), get_TrxName());

					if (peer.getRef_InOutLine_ID() != 0) {
						line.setM_InOutLine_ID(peer.getRef_InOutLine_ID());
					}
				}
			}

			//

			line.setProcessed(false);

			if (line.save(get_TrxName())) {
				count++;
			}

			// Cross Link

			if (counter) {
				fromLines[i].setRef_InvoiceLine_ID(line.getC_InvoiceLine_ID());
				fromLines[i].save(get_TrxName());
			}
		}

		if (fromLines.length != count) {
			log.log(Level.SEVERE, "copyLinesFrom - Line difference - From="
					+ fromLines.length + " <> Saved=" + count);
		}

		return count;
	} // copyLinesFrom

	/**
	 * Descripción de Método
	 * 
	 * 
	 * @param requery
	 * 
	 * @return
	 */

	public MInvoiceTax[] getTaxes(boolean requery) {
		if ((m_taxes != null) && !requery) {
			return m_taxes;
		}

		String sql = "SELECT * FROM C_InvoiceTax WHERE C_Invoice_ID=?";
		ArrayList list = new ArrayList();
		PreparedStatement pstmt = null;

		try {
			pstmt = DB.prepareStatement(sql, get_TrxName());
			pstmt.setInt(1, getC_Invoice_ID());

			ResultSet rs = pstmt.executeQuery();

			while (rs.next()) {
				list.add(new MInvoiceTax(getCtx(), rs, get_TrxName()));
			}

			rs.close();
			pstmt.close();
			pstmt = null;
		} catch (Exception e) {
			log.log(Level.SEVERE, "getTaxes", e);
		}

		try {
			if (pstmt != null) {
				pstmt.close();
			}

			pstmt = null;
		} catch (Exception e) {
			pstmt = null;
		}

		m_taxes = new MInvoiceTax[list.size()];
		list.toArray(m_taxes);

		return m_taxes;
	} // getTaxes

	/**
	 * Descripción de Método
	 * 
	 * 
	 * @param description
	 */

	public void addDescription(String description) {
		String desc = getDescription();

		if (desc == null) {
			setDescription(description);
		} else {
			setDescription(desc + " | " + description);
		}
	} // addDescription

	/**
	 * Descripción de Método
	 * 
	 * 
	 * @return
	 */

	public boolean isCreditMemo() {
		MDocType dt = MDocType.get(getCtx(),
				(getC_DocType_ID() == 0) ? getC_DocTypeTarget_ID()
						: getC_DocType_ID());

		return MDocType.DOCBASETYPE_APCreditMemo.equals(dt.getDocBaseType())
				|| MDocType.DOCBASETYPE_ARCreditMemo
						.equals(dt.getDocBaseType());
	} // isCreditMemo

	/**
	 * Descripción de Método
	 * 
	 * 
	 * @param processed
	 */

	public void setProcessed(boolean processed) {
		super.setProcessed(processed);

		if (getID() == 0) {
			return;
		}

		String set = "SET Processed='" + (processed ? "Y" : "N")
				+ "' WHERE C_Invoice_ID=" + getC_Invoice_ID();
		int noLine = DB.executeUpdate("UPDATE C_InvoiceLine " + set,
				get_TrxName());
		int noTax = DB.executeUpdate("UPDATE C_InvoiceTax " + set,
				get_TrxName());

		m_lines = null;
		m_taxes = null;
		log.fine(processed + " - Lines=" + noLine + ", Tax=" + noTax);
	} // setProcessed

	/**
	 * Descripción de Método
	 * 
	 * 
	 * @return
	 */

	public boolean validatePaySchedule() {
		MInvoicePaySchedule[] schedule = MInvoicePaySchedule
				.getInvoicePaySchedule(getCtx(), getC_Invoice_ID(), 0,
						get_TrxName());

		log.fine("#" + schedule.length);

		if (schedule.length == 0) {
			setIsPayScheduleValid(false);

			return false;
		}

		// Add up due amounts

		BigDecimal total = Env.ZERO;

		for (int i = 0; i < schedule.length; i++) {
			schedule[i].setParent(this);

			BigDecimal due = schedule[i].getDueAmt();

			if (due != null) {
				total = total.add(due);
			}
		}

		boolean valid = getGrandTotal().compareTo(total) == 0;

		setIsPayScheduleValid(valid);

		// Update Schedule Lines

		for (int i = 0; i < schedule.length; i++) {
			if (schedule[i].isValid() != valid) {
				schedule[i].setIsValid(valid);
				schedule[i].save(get_TrxName());
			}
		}

		return valid;
	} // validatePaySchedule

	private boolean completarPuntoLetraNumeroDoc() {
		HashMap<String, Object> hm = CalloutInvoiceExt
				.DividirDocumentNo(getDocumentNo());

		if (is_ValueChanged("NumeroComprobante"))
			hm.put("NumeroComprobante", getNumeroComprobante());

		// Si hay una letra de comprobante elegida es gracias al callout (ya que
		// el campo es de solo
		// lectura para el usuario), o un valor especificado manualmente.
		// Verificar si se corresponde
		// el valor elegido con el que tiene asociada la secuencia del tipo de
		// documento.

		if (getC_Letra_Comprobante_ID() != 0
				&& (Integer) hm.get("C_Letra_Comprobante_ID") != getC_Letra_Comprobante_ID()) {
			log.saveError("SaveError", Msg.translate(Env.getCtx(),
					"DiferentDocTypeLetraComprobanteError"));
			return false;
		}

		for (String k : hm.keySet()) {
			Object v = hm.get(k);

			if (v == null) {
				log.saveError("SaveError", Msg.translate(Env.getCtx(),
						"InvalidDocTypeFormatError"));
				return false;
			}

			set_Value(k, v);
		}

		return true;
	}

	/**
	 * Descripción de Método
	 * 
	 * 
	 * @param newRecord
	 * 
	 * @return
	 */

	protected boolean beforeSave(boolean newRecord) {
		//POSSimple:
		if (this.skipAfterAndBeforeSave)
			return true;
		
		log.fine("");

		// Disytel: Si ya se incorporaron lineas, no permitir el cambio de la
		// tarifa
		if (is_ValueChanged("M_PriceList_ID") && getLines(true).length > 0) {
			log.saveError("Error", Msg.getMsg(getCtx(),
					"PriceListChangedLinesAlreadyLoaded"));
			return false;
		}

		// Disytel: Si ya se incorporaron lineas, no permitir el cambio de la
		// moneda destino
		if (is_ValueChanged("C_Currency_ID") && getLines(true).length > 0) {
			log.saveError("Error", Msg.getMsg(getCtx(),
					"CurrencyChangedLinesAlreadyLoaded"));
			return false;
		}

		// No Partner Info - set Template

		if (getC_BPartner_ID() == 0) {
			setBPartner(MBPartner.getTemplate(getCtx(), getAD_Client_ID()));
		}

		MBPartner partner = new MBPartner(getCtx(), getC_BPartner_ID(),
				get_TrxName());

		if (getC_BPartner_Location_ID() == 0) {
			setBPartner(partner);
		}

		// Price List

		if (getM_PriceList_ID() == 0) {
			int ii = Env.getContextAsInt(getCtx(), "#M_PriceList_ID");

			if (ii != 0) {
				setM_PriceList_ID(ii);
			} else {
				String sql = "SELECT M_PriceList_ID FROM M_PriceList WHERE AD_Client_ID=? AND IsDefault='Y'";

				ii = DB.getSQLValue(null, sql, getAD_Client_ID());

				if (ii != 0) {
					setM_PriceList_ID(ii);
				}
			}
		}

		// Currency

		if (getC_Currency_ID() == 0) {
			String sql = "SELECT C_Currency_ID FROM M_PriceList WHERE M_PriceList_ID=?";
			int ii = DB.getSQLValue(null, sql, getM_PriceList_ID());

			if (ii != 0) {
				setC_Currency_ID(ii);
			} else {
				setC_Currency_ID(Env
						.getContextAsInt(getCtx(), "#C_Currency_ID"));
			}
		}

		// Sales Rep

		if (getSalesRep_ID() == 0) {
			int ii = Env.getContextAsInt(getCtx(), "#SalesRep_ID");

			if (ii != 0) {
				setSalesRep_ID(ii);
			}
		}

		// Document Type

		if (getC_DocType_ID() == 0) {
			setC_DocType_ID(0); // make sure it's set to 0
		}

		if (getC_DocTypeTarget_ID() == 0) {
			setC_DocTypeTarget_ID(isSOTrx() ? MDocType.DOCBASETYPE_ARInvoice
					: MDocType.DOCBASETYPE_APInvoice);
		}

		// Payment Term

		if (getC_PaymentTerm_ID() == 0) {
			int ii = Env.getContextAsInt(getCtx(), "#C_PaymentTerm_ID");

			if (ii != 0) {
				setC_PaymentTerm_ID(ii);
			} else {
				String sql = "SELECT C_PaymentTerm_ID FROM C_PaymentTerm WHERE AD_Client_ID=? AND IsDefault='Y'";

				ii = DB.getSQLValue(get_TrxName(), sql, getAD_Client_ID());

				if (ii != 0) {
					setC_PaymentTerm_ID(ii);
				}
			}
		}

		// Disytel: Si no hay conversion, no permitir seleccionar moneda destino
		int priceListCurrency = new MPriceList(getCtx(), getM_PriceList_ID(),
				null).getC_Currency_ID();
		if (priceListCurrency != getC_Currency_ID()
				&& MCurrency.currencyConvert(new BigDecimal(1),
						priceListCurrency, getC_Currency_ID(),
						getDateInvoiced(), 0, getCtx()) == null) {
			log
					.saveError("Error", Msg.getMsg(getCtx(),
							"NoCurrencyConversion"));
			return false;
		}

		// Se obtiene el tipo de documento para determinar si es fiscal o no.
		MDocType docType = MDocType.get(getCtx(), getC_DocTypeTarget_ID());
		/*
		 * // Indicador de documento fiscal. boolean fiscalDocType = // Factura
		 * de Retención no requiere validaciones fiscales
		 * !docType.getDocTypeKey().equals(MDocType.DOCTYPE_Retencion_Invoice)
		 * && // Recibo de retención no requiere validaciones fiscales.
		 * !docType.getDocTypeKey().equals(MDocType.DOCTYPE_Retencion_Receipt);
		 */
		if (CalloutInvoiceExt.ComprobantesFiscalesActivos()
				&& docType.isFiscalDocument()) {
			boolean IsSOTrx = isSOTrx();

			// CUIT - si no está seteado, setearlo a partir del BPartner
			
			String cuit = getCUIT();
			if (Util.isEmpty(cuit))
			{
				cuit = partner.getTaxID();
				setCUIT(cuit);
			}
			MCategoriaIva bpCategoriaIva = new MCategoriaIva(getCtx(), partner
					.getC_Categoria_Iva_ID(), get_TrxName());

			if (bpCategoriaIva.isRequiereCUIT()
					&& !CalloutInvoiceExt.ValidarCUIT(cuit)) {
				log.saveError("InvalidCUIT", "");
				return false;
			}

			// Nombre, Identificacion y Domicilio de cliente

			try {
				MBPartnerLocation loc = new MBPartnerLocation(getCtx(),
						getC_BPartner_Location_ID(), get_TrxName());
				if (loc.getID() < 1)
					return false;

				if (IsSOTrx) {

					// Solo se setea el nombre y el domicilio si no es
					// CONSUMIDOR FINAL
					if (bpCategoriaIva.getCodigo() != MCategoriaIva.CONSUMIDOR_FINAL) {
						setNombreCli(partner.getName());
						setInvoice_Adress(loc.getLocation(true).toString());
					}
				}

			} catch (Exception e) {
				log.saveError("SaveError", e);
				e.printStackTrace();
				return false;
			}

			// Definir la Letra del documento automáticamente

			try {
				if (getDocumentNo() == null && getC_Letra_Comprobante_ID() == 0) {
					Integer letraId;

					Integer categoriaIvaClient = CalloutInvoiceExt
							.darCategoriaIvaClient();
					categoriaIvaClient = categoriaIvaClient == null ? 0
							: categoriaIvaClient;
					int categoriaIvaPartner = partner.getC_Categoria_Iva_ID();

					// Algunas de las categorias de iva no esta asignada
					if (categoriaIvaClient == 0 || categoriaIvaPartner == 0) {
						String errorDesc = (categoriaIvaClient == 0 ? "@ClientWithoutIVAError@"
								: "@BPartnerWithoutIVAError@");
						log
								.saveError(
										"InvalidInvoiceLetraSaveError",
										Msg
												.parseTranslation(
														getCtx(),
														errorDesc
																+ ". @CompleteBPandClientCateoriaIVA@"));
						return false;
					}

					if (IsSOTrx) { // partner -> customer, empresa -> vendor
						letraId = CalloutInvoiceExt.darLetraComprobante(
								categoriaIvaPartner, categoriaIvaClient);
					} else { // empresa -> customer, partner -> vendor
						letraId = CalloutInvoiceExt.darLetraComprobante(
								categoriaIvaClient, categoriaIvaPartner);
					}

					// No fué posible calcular la letra de comprobante a partir
					// de las categorías de
					// IVA de la entidad comercial y la compañía.
					if (letraId == null) {
						log.saveError("InvalidInvoiceLetraSaveError", Msg
								.translate(getCtx(), "LetraCalculationError"));
						return false;
					}

					setC_Letra_Comprobante_ID(letraId);
				}
			} catch (Exception e) {
				log.saveError("SaveError", e);
				e.printStackTrace();
				return false;
			}

			// Numero de documento

			try {
				if (isSOTrx()) {
					if (getDocumentNo() == null)
						// Se calcula el nro de documento a partir de Pto.Vta,
						// letra y
						// siguiente Comprobante del Tipo de Documento.
						setDocumentNo();

					if (!completarPuntoLetraNumeroDoc())
						return false;

					/**
					 * Aceptar la modificacion manual del nro de comprobante
					 * para generar el numero de documento Las facturas de
					 * cliente deben poder modificarse en caso que el usuario
					 * indique un numero de comprobante diferente al de la
					 * secuencia. Se debe contemplar el uso de <> para
					 * posteriores usos de la secuencia, en los casos en que el
					 * usuario no indique un valor diferente al sugerido
					 * */
					if (newRecord) {
						// Recuperar el valor sugerido y compararlo con el
						// indicado por el usuario
						String nroCompr = getDocumentNo().replace("<", "")
								.replace(">", "");
						int nroC = Integer.parseInt(nroCompr.substring(5,
								nroCompr.length()));
						setDocumentNo(CalloutInvoiceExt
								.GenerarNumeroDeDocumento(
										getPuntoDeVenta(),
										getNumeroComprobante(),
										getLetra(),
										IsSOTrx,
										isCopy()
												|| getNumeroComprobante() == nroC));
					} else
						// Si no es un nuevo registro, siempre usar el indicado
						// (no usar secuencia)
						setDocumentNo(CalloutInvoiceExt
								.GenerarNumeroDeDocumento(getPuntoDeVenta(),
										getNumeroComprobante(), getLetra(),
										IsSOTrx, false));

				} else {
					setDocumentNo(CalloutInvoiceExt.GenerarNumeroDeDocumento(
							getPuntoDeVenta(), getNumeroComprobante(),
							getLetra(), IsSOTrx));
				}

				setNumeroDeDocumento(getDocumentNo());

			} catch (Exception e) {
				log.saveError("SaveError", e);
				e.printStackTrace();
				return false;
			}

			// Letra de comprobante = categoria iva
			if ((this.isSOTrx()) && (newRecord)) {
				/*
				 * valido que la categoria de impuesto del cliente este correcta
				 * con la letra que tiene la factura
				 */
				if (!this.validarLetraComprobante()) {
					// log.saveError("Error",
					// "No es correcta la letra de comprobante, para el cliente");
					return false;
				}
			}

			// Fecha del CAI
			if (getCAI() != null && !getCAI().equals("")
					&& getDateCAI() == null) {
				log.saveError("InvalidCAIDate", "");
				return false;
			}

			// Fecha del CAI > que fecha de facturacion
			if (getDateCAI() != null
					&& getDateInvoiced().compareTo(getDateCAI()) > 0) {
				log.saveError("InvoicedDateAfterCAIDate", "");
				return false;
			}

			// Punto de Venta y Numero de comprobante - Validacion de rango.
			// Dado que los rangos que se pueden configurar en los metadatos
			// de la columna no producen error (solo agregando una nota al log),
			// se hace dicha validación manualmente aquí.
			if (!(getPuntoDeVenta() > 0 && getPuntoDeVenta() < 10000)) {
				log.saveError("SaveError", Msg.getMsg(getCtx(),
						"FieldValueOutOfRange", new Object[] {
								Msg.translate(getCtx(), "PuntoDeVenta"), 1,
								9999 }));
				return false;
			}

			if (!(getNumeroComprobante() > 0 && getNumeroComprobante() < 1000000000)) {
				log.saveError("SaveError", Msg.getMsg(getCtx(),
						"FieldValueOutOfRange", new Object[] {
								Msg.translate(getCtx(), "NumeroComprobante"),
								1, 99999999 }));
				return false;
			}

		}

		/*
		 * Comprobar si el documento base denota crédito (generalmente una nota
		 * de crédito). Si es, verificar que la factura o comprobante son de las
		 * mismas entidades comerciales
		 */

		// Si el documento base denota un crédito (generalmente una nota de
		// crédito) para el cliente

		if (docType.getDocBaseType().equals("ARC")) {
			// Si tiene una factura o comprobante original
			if (this.getC_Invoice_Orig_ID() != 0) {

				// Obtengo la factura o comprobante original
				MInvoice origin = MInvoice.get(this.getCtx(), this
						.getC_Invoice_Orig_ID(), this.get_TrxName());

				// Si las entidades comerciales del documento y de la factura
				// original no coinciden ----> Error
				if (this.getC_BPartner_ID() != origin.getC_BPartner_ID()) {
					log.saveError("BPartnerInvoiceCustomerCreditNotSame", "");
					return false;
				}
			}
		}
		
		
		// Actualización de las líneas en base al descuento de la cabecera
		// cuando cambia ese dato (No para TPV)
		if(is_ValueChanged("ManualGeneralDiscount") && !updateManualGeneralDiscount()){
			log.saveError("", CLogger.retrieveErrorAsString());
			return false;
		}
		
		return true;
	} // beforeSave

	/**
	 * Actualiza el descuento manual general
	 * 
	 * @return true si fue posible la actualización, false caso contrario
	 */
	public boolean updateManualGeneralDiscount(){
		if (isSkipManualGeneralDiscount())
			return true;
		
        int stdPrecision = MPriceList.getStandardPrecision( getCtx(),getM_PriceList_ID());
		try{
			// Actualización del descuento de líneas 
			updateManualGeneralDiscountToLines(stdPrecision);
		} catch(Exception e){
			log.saveError("", !Util.isEmpty(e.getMessage()) ? e.getMessage()
					: e.getCause() != null ? e.getCause().getMessage() : "");
			return false;
		}
		return true;
	}

	/**
	 * Actualización de líneas en base al descuento cargado en la cabecera
	 * 
	 * @param scale
	 * @throws Exception
	 */
	public void updateManualGeneralDiscountToLines(int scale) throws Exception{
		for (MInvoiceLine invoiceLine : getLines()) {
			invoiceLine.updateGeneralManualDiscount(getManualGeneralDiscount(), scale);
			invoiceLine.setSkipManualGeneralDiscount(true);
			if(!invoiceLine.save()){
				throw new Exception(CLogger.retrieveErrorAsString());
			}
		}
	}

	/**
	 * Actualizo el porcentaje de descuento de la cabecera con la suma de los
	 * descuentos de las líneas
	 * 
	 * @param scale
	 */
	public void updateManualGeneralDiscountByLines(int scale) {
		if(getGrandTotal().compareTo(BigDecimal.ZERO) == 0) return;
		BigDecimal totalLineDiscountAmt = getSumColumnLines("LineDiscountAmt");
		// Obtengo el porcentaje de descuento en base al grandtotal y a la suma
		// de los descuentos
		BigDecimal discountManualPerc = totalLineDiscountAmt.multiply(
				new BigDecimal(100)).divide(getGrandTotal(), scale,
				BigDecimal.ROUND_HALF_UP); 
		setManualGeneralDiscount(discountManualPerc);
	}

	/**
	 * Obtengo la suma de una columna numérica de las líneas de esta factura
	 * 
	 * @param numericColumnName
	 *            nombre de la columna numérica de la línea
	 * @return la suma de esa columna numérica de las líneas de la factura
	 */
	protected BigDecimal getSumColumnLines(String numericColumnName){
		// Obtengo la suma de los descuentos de las líneas
		String sql = "SELECT sum(" + numericColumnName
				+ ") FROM c_invoiceline WHERE c_invoice_id = ?";
		BigDecimal totalLineAmt = DB.getSQLValueBD(get_TrxName(), sql, getID());
		totalLineAmt = totalLineAmt == null?BigDecimal.ZERO:totalLineAmt;
		return totalLineAmt;
	}
	
	/**
	 * Descripción de Método
	 * 
	 * 
	 * @return
	 */

	private boolean validarLetraComprobante() {
		// Parametros

		MClient vCompania = new MClient(getCtx(),
				Env.getAD_Client_ID(getCtx()), get_TrxName());
		Integer vCategoriaIva = vCompania.getCategoriaIva();
		MBPartner vCliente = new MBPartner(getCtx(), this.getC_BPartner_ID(),
				get_TrxName());
		boolean value = false;

		// 
		if (vCategoriaIva == 0) {
			// no existen alguno de los datos a validar, devuelvo verdadero
			log.saveError("SaveError", Msg.translate(Env.getCtx(),
					"ClientWithoutIVAError"));
			value = false;
		} else if (vCliente.getC_Categoria_Iva_ID() == 0) {
			log.saveError("SaveError", Msg.translate(Env.getCtx(),
					"BPartnerWithoutIVAError"));
			value = false;
		} else {
			// todos los parametros de la busqueda existe, busco a ver si es
			// correcta la clasificacion del iva
			StringBuffer sql = new StringBuffer("SELECT * "
					+ "	FROM C_Letra_Acepta_IVA "
					+ "   WHERE categoria_vendor = ? "
					+ "         AND categoria_customer = ? "
					+ "         AND c_letra_comprobante_Id = ? ");

			try {
				PreparedStatement pstmt = DB.prepareStatement(sql.toString());
				pstmt.setInt(1, vCategoriaIva);
				pstmt.setInt(2, vCliente.getC_Categoria_Iva_ID());
				pstmt.setInt(3, this.getC_Letra_Comprobante_ID());
				ResultSet rs = pstmt.executeQuery();

				value = rs.next();

				if (!value) {
					log.saveError("SaveError", Msg.translate(Env.getCtx(),
							"InvalidLetraComprobanteError"));
				}

				rs.close();
				pstmt.close();
			} catch (SQLException e) {
				log.log(Level.SEVERE, sql.toString(), e);
			}
		}
		return value;
	}

	/**
	 * Verifica si la factura se encuentra registrada en el sistema, lo que
	 * provoca facturas repetidas. El criterio para verificar unicidad se
	 * realiza en base a los siguientes campos:
	 * <ul>
	 * <li>CUIT del bpartner relacionado con la factura</li>
	 * <li>Punto de Venta</li>
	 * <li>Número de Factura</li>
	 * <li>Letra de la factura</li>
	 * </ul>
	 * 
	 * @return true si existe una factura con los mismos datos, false cc
	 */
	private boolean isRepeatInvoice() {
		/*
		 * Si la factura posee monto negativo -> es una contrafactura por
		 * anulación. Permitir repetido
		 */
		if (getGrandTotal().compareTo(Env.ZERO) < 0)
			return false;

		/* Si la factura posee contra-documento, omitir validacion */
		if (getRef_Invoice_ID() > 0)
			return false;

		/* Si la factura es una copia de un original, omitir validación */
		if (isCopy())
			return false;
		
		// Para facturas de venta, si está activo locale ar y el tipo de
		// documento requiere impresión fiscal entonces no debo controlar
		// factura repetida
		if(isSOTrx() && requireFiscalPrint()){
			return false;
		}
		
		// Condiciones comunes entre issotrx=Y y issotrx=N
		StringBuffer whereClause = new StringBuffer();
		whereClause.append("docStatus in ('CO', 'CL') AND (c_invoice_id != ?) AND (issotrx = ?) AND (documentno = ?) AND (c_doctypetarget_id = ?) ");
		List<Object> whereParams = new ArrayList<Object>();
		whereParams.add(getID());
		whereParams.add(isSOTrx()?"Y":"N");
		whereParams.add(getDocumentNo());
		whereParams.add(getC_DocTypeTarget_ID());
		if(!isSOTrx()){
			// Si locale_ar, entonces validamos por cuit
			if(CalloutInvoiceExt.ComprobantesFiscalesActivos()){
				whereClause.append(" AND (cuit = ?) ");
				// TODO: Sacar la instanciación de la entidad para obtener el
				// cuit, se dieron casos en que no se seteaba el cuit de la
				// factura, verificar si eso no pasa mas, por las dudas la
				// obtenemos de la entidad comercial
				MBPartner bpartner = new MBPartner(getCtx(), getC_BPartner_ID(), get_TrxName());
				whereParams.add(bpartner.getTaxID());
			}
			else{
				whereClause.append(" AND (c_bpartner_id = ?) ");
				whereParams.add(getC_BPartner_ID());
			}			
		}
		// Armar el sql
		String sql = "SELECT c_invoice_id FROM c_invoice WHERE "+whereClause.toString();

		Object res = DB.getSQLObject(this.get_TrxName(), sql, whereParams.toArray());

		// true si existe una factura
		return res != null;
	}

	/**
	 * Descripción de Método
	 * 
	 * 
	 * @return
	 */

	public String toString() {
		StringBuffer sb = new StringBuffer("MInvoice[").append(getID()).append(
				"-").append(getDocumentNo()).append(",GrandTotal=").append(
				getGrandTotal());

		if (m_lines != null) {
			sb.append(" (#").append(m_lines.length).append(")");
		}

		sb.append("]");

		return sb.toString();
	} // toString

	/**
	 * Descripción de Método
	 * 
	 * 
	 * @param newRecord
	 * @param success
	 * 
	 * @return
	 */

	protected boolean afterSave(boolean newRecord, boolean success) {
		//POSSImple:
		if (this.skipAfterAndBeforeSave)
			return success;
		
		if (!success || newRecord) {
			return success;
		}

		if (is_ValueChanged("AD_Org_ID")) {
			String sql = "UPDATE C_InvoiceLine ol"
					+ " SET AD_Org_ID ="
					+ "(SELECT AD_Org_ID"
					+ " FROM C_Invoice o WHERE ol.C_Invoice_ID=o.C_Invoice_ID) "
					+ "WHERE C_Invoice_ID=" + getC_Order_ID();
			int no = DB.executeUpdate(sql, get_TrxName());

			log.fine("Lines -> #" + no);
		}
		
		// Esquemas de pagos
		// Si se modificó el campo del esquema de vencimientos entonces
		// actualizo el esquema de pagos de la factura
		if (!isSkipApplyPaymentTerm() && is_ValueChanged("C_PaymentTerm_ID")) {
			MPaymentTerm pt = new MPaymentTerm(getCtx(), getC_PaymentTerm_ID(),
					get_TrxName());
			return pt.apply(this);
		}
		
		return true;
	} // afterSave

	/**
	 * Descripción de Método
	 * 
	 * 
	 * @param M_PriceList_ID
	 */

	public void setM_PriceList_ID(int M_PriceList_ID) {
		String sql = "SELECT M_PriceList_ID, C_Currency_ID "
				+ "FROM M_PriceList WHERE M_PriceList_ID=?";
		PreparedStatement pstmt = null;

		try {
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, M_PriceList_ID);

			ResultSet rs = pstmt.executeQuery();

			if (rs.next()) {
				super.setM_PriceList_ID(rs.getInt(1));
				setC_Currency_ID(rs.getInt(2));
			}

			rs.close();
			pstmt.close();
			pstmt = null;
		} catch (Exception e) {
			log.log(Level.SEVERE, "setM_PriceList_ID", e);
		} finally {
			try {
				if (pstmt != null) {
					pstmt.close();
				}
			} catch (Exception e) {
			}

			pstmt = null;
		}
	} // setM_PriceList_ID

	/**
	 * Descripción de Método
	 * 
	 * 
	 * @return
	 */

	public BigDecimal getAllocatedAmt() {
		BigDecimal retValue = null;
		String sql = "SELECT SUM(currencyConvert(al.Amount+al.DiscountAmt+al.WriteOffAmt,"
				+ "ah.C_Currency_ID, i.C_Currency_ID,ah.DateTrx,i.C_ConversionType_ID, al.AD_Client_ID,al.AD_Org_ID)) "
				+ "FROM C_AllocationLine al"
				+ " INNER JOIN C_AllocationHdr ah ON (al.C_AllocationHdr_ID=ah.C_AllocationHdr_ID)"
				+ " INNER JOIN C_Invoice i ON (al.C_Invoice_ID=i.C_Invoice_ID) "
				+ "WHERE al.C_Invoice_ID=?"
				+ " AND ah.IsActive='Y' AND al.IsActive='Y'";
		PreparedStatement pstmt = null;

		try {
			pstmt = DB.prepareStatement(sql, get_TrxName());
			pstmt.setInt(1, getC_Invoice_ID());

			ResultSet rs = pstmt.executeQuery();

			if (rs.next()) {
				retValue = rs.getBigDecimal(1);
			}

			rs.close();
			pstmt.close();
			pstmt = null;
		} catch (Exception e) {
			log.log(Level.SEVERE, "getAllocatedAmt", e);
		}

		try {
			if (pstmt != null) {
				pstmt.close();
			}

			pstmt = null;
		} catch (Exception e) {
			pstmt = null;
		}

		// log.fine("getAllocatedAmt - " + retValue);
		// ? ROUND(NVL(v_AllocatedAmt,0), 2);

		return retValue;
	} // getAllocatedAmt

	/**
	 * Descripción de Método
	 * 
	 * 
	 * @return
	 */

	public boolean testAllocation() {
		BigDecimal alloc = getAllocatedAmt(); // absolute

		if (alloc == null) {
			alloc = Env.ZERO;
		}

		BigDecimal total = getGrandTotal();

		if (!isSOTrx()) {
			total = total.negate();
		}

		if (isCreditMemo()) {
			total = total.negate();
		}

		boolean test = total.compareTo(alloc) == 0;
		boolean change = test != isPaid();

		if (change) {
			setIsPaid(test);
		}

		log.fine("testAllocation - Paid=" + test + " (" + alloc + "=" + total
				+ ")");

		return change;
	} // testAllocation

	/**
	 * Descripción de Método
	 * 
	 * 
	 * @return
	 */

	public BigDecimal getOpenAmt() {
		return getOpenAmt(true, null);
	} // getOpenAmt

	/**
	 * Descripción de Método
	 * 
	 * 
	 * @param creditMemoAdjusted
	 * @param paymentDate
	 * 
	 * @return
	 */

	public BigDecimal getOpenAmt(boolean creditMemoAdjusted,
			Timestamp paymentDate) {
		if (isPaid()) {
			return Env.ZERO;
		}

		//

		if (m_openAmt == null) {
			m_openAmt = getGrandTotal();

			if (paymentDate != null) {

				// Payment Discount
				// Payment Schedule

			}

			BigDecimal allocated = getAllocatedAmt();

			if (allocated != null) {
				allocated = allocated.abs(); // is absolute
				m_openAmt = m_openAmt.subtract(allocated);
			}
		}

		//

		if (!creditMemoAdjusted) {
			return m_openAmt;
		}

		if (isCreditMemo()) {
			return m_openAmt.negate();
		}

		return m_openAmt;
	} // getOpenAmt

	/**
	 * Descripción de Método
	 * 
	 * 
	 * @return
	 */

	public String getDocStatusName() {
		return MRefList.getListName(getCtx(), MInvoice.DOCSTATUS_AD_Reference_ID, getDocStatus());
	} // getDocStatusName

	/**
	 * Descripción de Método
	 * 
	 * 
	 * @return
	 */

	public File createPDF() {
		return createPDF(null);
	} // getPDF

	/**
	 * Descripción de Método
	 * 
	 * 
	 * @param file
	 * 
	 * @return
	 */

	public File createPDF(File file) {
		ReportEngine re = ReportEngine.get(getCtx(), ReportEngine.INVOICE,
				getC_Invoice_ID());

		if (re == null) {
			return null;
		}

		return re.getPDF(file);
	} // getPDF

	/**
	 * Descripción de Método
	 * 
	 * 
	 * @param documentDir
	 * 
	 * @return
	 */

	public String getPDFFileName(String documentDir) {
		return getPDFFileName(documentDir, getC_Invoice_ID());
	} // getPDFFileName

	/**
	 * Descripción de Método
	 * 
	 * 
	 * @return
	 */

	public String getCurrencyISO() {
		return MCurrency.getISO_Code(getCtx(), getC_Currency_ID());
	} // getCurrencyISO

	/**
	 * Descripción de Método
	 * 
	 * 
	 * @return
	 */

	public int getPrecision() {
		return MCurrency.getStdPrecision(getCtx(), getC_Currency_ID());
	} // getPrecision

	/**
	 * Descripción de Método
	 * 
	 * 
	 * @param processAction
	 * 
	 * @return
	 */

	public boolean processIt(String processAction) {
		m_processMsg = null;

		DocumentEngine engine = new DocumentEngine(this, getDocStatus());

		boolean status = engine.processIt(processAction, getDocAction(), log);

		status = this.afterProcessDocument(engine.getDocAction(), status) && status;

		return status;
	} // process

	/** Descripción de Campos */

	private boolean m_justPrepared = false;

	/**
	 * Descripción de Método
	 * 
	 * 
	 * @return
	 */

	public boolean unlockIt() {
		log.info("unlockIt - " + toString());
		setProcessing(false);

		return true;
	} // unlockIt

	/**
	 * Descripción de Método
	 * 
	 * 
	 * @return
	 */

	public boolean invalidateIt() {
		log.info("invalidateIt - " + toString());
		setDocAction(DOCACTION_Prepare);

		return true;
	} // invalidateIt

	/**
	 * Descripción de Método
	 * 
	 * 
	 * @return
	 */

	public String prepareIt() {
		log.info(toString());
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,
				ModelValidator.TIMING_BEFORE_PREPARE);

		if (m_processMsg != null) {
			return DocAction.STATUS_Invalid;
		}

		MDocType dt = MDocType.get(getCtx(), getC_DocTypeTarget_ID());

		// Std Period open?

		if (!MPeriod.isOpen(getCtx(), getDateAcct(), dt.getDocBaseType())) {
			m_processMsg = "@PeriodClosed@";

			return DocAction.STATUS_Invalid;
		}

		// Lines

		MInvoiceLine[] lines = getLines(true);

		if (lines.length == 0) {
			m_processMsg = "@NoLines@";

			return DocAction.STATUS_Invalid;
		}

		if (PAYMENTRULE_Cash.equals(getPaymentRule())
				&& (MCashBook.get(getCtx(), getAD_Org_ID(), getC_Currency_ID(),
						null) == null)) {
			m_processMsg = "@NoCashBook@";

			return DocAction.STATUS_Invalid;
		}

		// Convert/Check DocType

		if (getC_DocType_ID() != getC_DocTypeTarget_ID()) {
			setC_DocType_ID(getC_DocTypeTarget_ID());
		}

		if (getC_DocType_ID() == 0) {
			m_processMsg = "No Document Type";

			return DocAction.STATUS_Invalid;
		}

		explodeBOM();

		if (!calculateTaxTotal()) // setTotals
		{
			m_processMsg = "Error calculating Tax";

			return DocAction.STATUS_Invalid;
		}

		createPaySchedule();

		// Modified by Matías Cap - Disytel
		// ---------------------------------------------------------------
		// Las validaciones de crédito se realizan en las nuevas clases
		// encargadas de eso.
		// Obtener el cliente
		MBPartner bp = new MBPartner(getCtx(), getC_BPartner_ID(),
				get_TrxName());
		// Obtener la organización
		MOrg org = new MOrg(getCtx(), Env.getAD_Org_ID(getCtx()), get_TrxName());
		if (!isCurrentAccountVerified && isSOTrx()
				&& getPaymentRule().equals(MInvoice.PAYMENTRULE_OnCredit)) {
			// Obtengo el manager actual
			CurrentAccountManager manager = CurrentAccountManagerFactory
					.getManager();
			// Seteo el estado actual del cliente y lo obtengo
			CallResult result = manager.setCurrentAccountStatus(getCtx(), bp, org, null);
			// Si hubo error, obtengo el mensaje y retorno inválido
			if (result.isError()) {
				m_processMsg = result.getMsg();
				return DocAction.STATUS_Invalid;
			}
			// Verificar la situación de crédito de la entidad comercial
			result = manager.validateCurrentAccountStatus(getCtx(),
					(String) result.getResult(), get_TrxName());
			// Si hubo error, obtengo el mensaje y retorno inválido
			if (result.isError()) {
				m_processMsg = result.getMsg();
				return DocAction.STATUS_Invalid;
			}
		}
		// ---------------------------------------------------------------
		
		// Valida si el documento ya fue impreso mediante un controlador
		// fiscal.
		if (isFiscalAlreadyPrinted()) {
			m_processMsg = "@FiscalAlreadyPrintedError@";
			return DocAction.STATUS_Invalid;
		}

		// - Validaciones generales (AR)
		if (!validateInvoice(bp)) {
			m_processMsg = CLogger.retrieveErrorAsString();
			return DocAction.STATUS_Invalid;
		}
		// -

		// Add up Amounts

		m_justPrepared = true;

		if (!DOCACTION_Complete.equals(getDocAction())) {
			setDocAction(DOCACTION_Complete);
		}
		
		// Verificar si las líneas están relacionadas con un pedido y
		// dependiendo el tipo de documento se deben controlar las cantidades
		// que no se excedan. Para débitos se debe controlar la cantidad
		// facturada con la pedida, para créditos la cantidad reservada con la
		// pedida 
		MInvoiceLine line;
		MOrderLine orderLine;
		MDocType docType = new MDocType(getCtx(), getC_DocTypeTarget_ID(), get_TrxName());
		boolean isDebit = !docType.getDocBaseType().equals(
				MDocType.DOCBASETYPE_ARCreditMemo)
				&& !docType.getDocBaseType().equals(
						MDocType.DOCBASETYPE_APCreditMemo);
    	for (int i = 0; i < lines.length; i++) {
    		line = lines[i];
			if(line.getC_OrderLine_ID() != 0){
				//Ader: NO LEER cosas de manera innecesria...
				//orderLine = new MOrderLine(getCtx(), line.getC_OrderLine_ID(), get_TrxName());
				if(!isDebit){
					//ADER: fix temporal hacer la lectura de las linea aca; igual
					//en general no tiene sentido hacer este chequeo con N accesos...
					//se tiene que poder hacer con un solo select
					//Mas alla de esto hay un error conceptual al usar qtyEntered en vez
					//de QtyInvoiced. Otro error esta en NO todas las lineas
					//de un pedido son afectadas en el campo QtyReserved... ver MOrder.reserverStock
					//o MOrder.reserveStockII
					orderLine = new MOrderLine(getCtx(), line.getC_OrderLine_ID(), get_TrxName());
					
					// Si es crédito se debe verificar que la cantidad ingresada
					// debe ser menor o igual a la cantidad reservada o
					// pendiente 
					if(line.getQtyEntered().compareTo(orderLine.getQtyReserved()) > 0){
						m_processMsg = "@InvoiceLineExceedsQtyReserved@";
			    		return DocAction.STATUS_Invalid;
					}					
				}
				// Si es débito se debe verificar la suma de la cantidad
				// ingresada con la cantidad facturada. 
				// FIXME Por ahora comentado, será necesario?? Es mucha
				// restricción colocar esta validación?
				/*else{
				    orderLine = new MOrderLine(getCtx(), line.getC_OrderLine_ID(), get_TrxName());
					qty = orderLine.getQtyInvoiced().add(line.getQtyEntered());
					if(qty.compareTo(orderLine.getQtyOrdered()) > 0){
						m_processMsg = "@InvoiceLineExceedsQtyOrdered@";
			    		return DocAction.STATUS_Invalid;
					}
				}*/
			}
		}
    	
		// Verificar factura repetida
		if (isRepeatInvoice()) {
			StringBuffer msgParams = new StringBuffer(" \n\n ");
			msgParams.append(" "+Msg.translate(getCtx(), "DocumentNo")+" ").append("\n");
			msgParams.append(" "+Msg.translate(getCtx(), "C_DocType_ID")+" ");
			if(!isSOTrx()){
				if(CalloutInvoiceExt.ComprobantesFiscalesActivos()){
					msgParams.append("\n").append(" CUIT ");
				}
				else{
					msgParams.append("\n").append(" "+Msg.translate(getCtx(), "C_BPartner_ID")+" ");
				}
			}
			m_processMsg = "@RepeatInvoice@"+msgParams.toString();
			return DocAction.STATUS_Invalid;
		}

		return DocAction.STATUS_InProgress;
	} // prepareIt

	/**
	 * Descripción de Método
	 * 
	 */

	private void explodeBOM() {
		String where = "AND IsActive='Y' AND EXISTS "
				+ "(SELECT * FROM M_Product p WHERE C_InvoiceLine.M_Product_ID=p.M_Product_ID"
				+ " AND p.IsBOM='Y' AND p.IsVerified='Y' AND p.IsStocked='N')";

		//

		String sql = "SELECT COUNT(*) FROM C_InvoiceLine "
				+ "WHERE C_Invoice_ID=? " + where;
		int count = DB.getSQLValue(get_TrxName(), sql, getC_Invoice_ID());

		while (count != 0) {
			renumberLines(100);

			// Order Lines with non-stocked BOMs

			MInvoiceLine[] lines = getLines(where);

			for (int i = 0; i < lines.length; i++) {
				MInvoiceLine line = lines[i];
				MProduct product = MProduct.get(getCtx(), line
						.getM_Product_ID());

				log.fine(product.getName());

				// New Lines

				int lineNo = line.getLine();
				MProductBOM[] boms = MProductBOM.getBOMLines(product);

				for (int j = 0; j < boms.length; j++) {
					MProductBOM bom = boms[j];
					MInvoiceLine newLine = new MInvoiceLine(this);

					newLine.setLine(++lineNo);
					newLine.setM_Product_ID(bom.getProduct().getM_Product_ID(),
							bom.getProduct().getC_UOM_ID());
					newLine.setQty(line.getQtyInvoiced().multiply(
							bom.getBOMQty())); // Invoiced/Entered

					if (bom.getDescription() != null) {
						newLine.setDescription(bom.getDescription());
					}

					//

					newLine.setPrice();
					newLine.save(get_TrxName());
				}

				// Convert into Comment Line

				line.setM_Product_ID(0);
				line.setM_AttributeSetInstance_ID(0);
				line.setPriceEntered(Env.ZERO);
				line.setPriceActual(Env.ZERO);
				line.setPriceLimit(Env.ZERO);
				line.setPriceList(Env.ZERO);
				line.setLineNetAmt(Env.ZERO);

				//

				String description = product.getName();

				if (product.getDescription() != null) {
					description += " " + product.getDescription();
				}

				if (line.getDescription() != null) {
					description += " " + line.getDescription();
				}

				line.setDescription(description);
				line.save(get_TrxName());
			} // for all lines with BOM

			m_lines = null;
			count = DB.getSQLValue(get_TrxName(), sql, getC_Invoice_ID());
			renumberLines(10);
		} // while count != 0
	} // explodeBOM

	/**
	 * Descripción de Método
	 * 
	 * 
	 * @return
	 */

	private boolean calculateTaxTotal() {
		log.fine("");

		// Delete Taxes

		DB
				.executeUpdate(
						"DELETE FROM C_InvoiceTax WHERE "
								+ " C_Tax_ID IN (SELECT c_tax_id FROM C_Tax ct left join C_TaxCategory ctc on ct.c_taxcategory_id = ctc.c_taxcategory_Id WHERE ctc.isManual = 'N') AND"
								+ " C_Invoice_ID=" + getC_Invoice_ID(),
						get_TrxName());
		m_taxes = null;

		// Lines

		BigDecimal totalLines = Env.ZERO;
		ArrayList taxList = new ArrayList();
		MInvoiceLine[] lines = getLines(false);

		for (int i = 0; i < lines.length; i++) {
			MInvoiceLine line = lines[i];

			// Sync ownership for SO

			if (isSOTrx() && (line.getAD_Org_ID() != getAD_Org_ID())) {
				line.setAD_Org_ID(getAD_Org_ID());
				line.save(get_TrxName());
			}

			Integer taxID = new Integer(line.getC_Tax_ID());

			if (!taxList.contains(taxID)) {

				MInvoiceTax iTax = MInvoiceTax.get(line, getPrecision(), false,
						get_TrxName()); // current Tax
				MTax cTax = new MTax(getCtx(), taxID, get_TrxName());

				if ((iTax != null) && (!cTax.isCategoriaManual())) {
					iTax.setIsTaxIncluded(isTaxIncluded());

					if (!iTax.calculateTaxFromLines()) {
						return false;
					}

					if (!iTax.save()) {
						return false;
					}

					taxList.add(taxID);
				}
			}

			totalLines = totalLines.add(line.getLineNetAmt());
		}

		// Taxes

		BigDecimal grandTotal = totalLines;
		MInvoiceTax[] taxes = getTaxes(true);

		for (int i = 0; i < taxes.length; i++) {
			MInvoiceTax iTax = taxes[i];
			MTax tax = iTax.getTax();

			if (tax.isSummary()) {
				MTax[] cTaxes = tax.getChildTaxes(false); // Multiple taxes

				for (int j = 0; j < cTaxes.length; j++) {
					MTax cTax = cTaxes[j];
					BigDecimal taxAmt = cTax.calculateTax(iTax.getTaxBaseAmt(),
							isTaxIncluded(), getPrecision());

					// aca tambien cambio por

					if (!cTax.isCategoriaManual()) {

						MInvoiceTax newITax = new MInvoiceTax(getCtx(), 0,
								get_TrxName());
						// aca tambien cambio por

						newITax.setClientOrg(this);
						newITax.setC_Invoice_ID(getC_Invoice_ID());
						newITax.setC_Tax_ID(cTax.getC_Tax_ID());
						newITax.setPrecision(getPrecision());
						newITax.setIsTaxIncluded(isTaxIncluded());
						newITax.setTaxBaseAmt(iTax.getTaxBaseAmt());
						newITax.setTaxAmt(taxAmt);

						if (!newITax.save(get_TrxName())) {
							return false;
						}
					}
					//

					if (!isTaxIncluded()) {
						grandTotal = grandTotal.add(taxAmt);
					}
				}

				if (!iTax.delete(true, get_TrxName())) {
					return false;
				}
			} else {
				if (!isTaxIncluded()) {
					grandTotal = grandTotal.add(iTax.getTaxAmt());
				}
			}
		}

		// Recalculo el total a partir del importe del cargo
		grandTotal = grandTotal.add(getChargeAmt());

		setTotalLines(totalLines);
		setGrandTotal(grandTotal);
		// setGrandTotal(totalLines.add(totalTax()));
		return true;
	} // calculateTaxTotal

	/**
	 * Descripción de Método
	 * 
	 * 
	 * @return
	 */

	private BigDecimal totalTax() {

		BigDecimal amount = new BigDecimal(0);
		String id = String.valueOf(getC_Invoice_ID());
		String sql = "SELECT taxamt FROM C_InvoiceTax "
				+ "WHERE isActive = 'Y' AND C_Invoice_ID = " + id;

		try {
			PreparedStatement pstmt = DB.prepareStatement(sql);
			ResultSet rs = pstmt.executeQuery();

			while (rs.next()) {
				BigDecimal value = rs.getBigDecimal(1);
				amount = amount.add(value);
			}
			rs.close();
			pstmt.close();
		} catch (SQLException e) {
			log.log(Level.SEVERE, "totalTax", e);
		}
		return amount;
	} // totalTax

	/**
	 * Descripción de Método
	 * 
	 * 
	 * @return
	 */

	private boolean createPaySchedule() {
		if (getC_PaymentTerm_ID() == 0) {
			return false;
		}

		MPaymentTerm pt = new MPaymentTerm(getCtx(), getC_PaymentTerm_ID(),
				null);

		log.fine(pt.toString());

		return pt.apply(this); // calls validate pay schedule
	} // createPaySchedule

	/**
	 * Operaciones luego de procesar el documento
	 */
	public boolean afterProcessDocument(String processAction, boolean status) {

		// Setear el crédito

		if ((processAction.equals(MInvoice.DOCACTION_Complete)
				|| processAction.equals(MInvoice.DOCACTION_Reverse_Correct) || processAction
				.equals(MInvoice.DOCACTION_Void))
				&& status) {

			// Guardar la factura con el nuevo estado a fin de recalcular
			// correctamente el credito disponible
			this.save();

			// Si es pedido de ventas y se paga a crédito, setear el crédito
			// recalculado
			if (isUpdateBPBalance() && isConfirmAditionalWorks()) {				
				MBPartner bp = new MBPartner(getCtx(), getC_BPartner_ID(), get_TrxName());
				// Obtengo el manager actual
				CurrentAccountManager manager = CurrentAccountManagerFactory.getManager();
				// Actualizo el balance
				CallResult result = manager.afterProcessDocument(getCtx(),
						new MOrg(getCtx(), getAD_Org_ID(), get_TrxName()), bp,
						getAditionalWorkResult(), get_TrxName());
				// Si hubo error, obtengo el mensaje y retorno inválido
				if (result.isError()) {
					log.severe(result.getMsg());
				}
			}
		}

		return true;

	}

	/**
	 * Descripción de Método
	 * 
	 * 
	 * @return
	 */

	public boolean approveIt() {
		log.info(toString());
		setIsApproved(true);

		return true;
	} // approveIt

	/**
	 * Descripción de Método
	 * 
	 * 
	 * @return
	 */

	public boolean rejectIt() {
		log.info(toString());
		setIsApproved(false);

		return true;
	} // rejectIt

	/**
	 * Descripción de Método
	 * 
	 * 
	 * @return
	 */

	public String completeIt() {
		setAditionalWorkResult(new HashMap<PO, Object>());
		boolean localeARActive = CalloutInvoiceExt
				.ComprobantesFiscalesActivos();
		// Re-Check

		if (!m_justPrepared) {
			String status = prepareIt();

			if (!DocAction.STATUS_InProgress.equals(status)) {
				return status;
			}
		}

		// Implicit Approval

		if (!isApproved()) {
			approveIt();
		}

		log.info(toString());

		StringBuffer info = new StringBuffer();

		// Create Cash

		if (PAYMENTRULE_Cash.equals(getPaymentRule()) && isCreateCashLine()) {
			MCash cash = MCash.get(getCtx(), getAD_Org_ID(), getDateInvoiced(),
					getC_Currency_ID(), get_TrxName());

			if ((cash == null) || (cash.getID() == 0)) {
				m_processMsg = "@NoCashBook@";

				return DocAction.STATUS_Invalid;
			}

			MCashLine cl = new MCashLine(cash);

			cl.setInvoice(this);
			// 1. Crea la línea en la BD
			if (!cl.save()) {
				m_processMsg = "@CashLineCreateError@: " + CLogger.retrieveErrorAsString();
			// 2. Completa la línea
			} else if (!cl.processIt(MCashLine.ACTION_Complete)) {
				m_processMsg = "@CashLineCreateError@: " + cl.getProcessMsg();
			// 3. Guarda los cambios
			} else if (!cl.save(get_TrxName())) {
				m_processMsg = "@CashLineCreateError@: " + CLogger.retrieveErrorAsString();
			}
			if (m_processMsg != null) {
				return STATUS_Invalid;
			}

			info.append("@C_Cash_ID@: " + cash.getName() + " #" + cl.getLine());
			setC_CashLine_ID(cl.getC_CashLine_ID());
		} // CashBook

		// Update Order & Match

		int matchInv = 0;
		int matchPO = 0;
		MInvoiceLine[] lines = getLines(false);
		MDocType docType = new MDocType(getCtx(), getC_DocTypeTarget_ID(), get_TrxName());
		boolean isDebit = !docType.getDocBaseType().equals(
				MDocType.DOCBASETYPE_ARCreditMemo)
				&& !docType.getDocBaseType().equals(
						MDocType.DOCBASETYPE_APCreditMemo);
		MOrderLine orderLine;
		MOrder order = null;
		
		//Ader: mejoras de logica de documentos; por ahora solo se trata
		//el caso de facturas de clientes normales; el codigo siguiente al else
		//trata los siguiente casos como antes. Esta optimización reemplaza, para 
		//facturas creadas a partir de peidods N*6 accesos por solo 1 (N siendo
		//la cantidad de lineas).
		if (isSOTrx()&& isDebit)
		{
			boolean ok = updateOrderIsSOTrxDebit(lines);
			if (!ok)
			{
				m_processMsg = "Could not update Order Line";		
				return DocAction.STATUS_Invalid;
			}
		}
		else
		{ 	//se deja el siguiente codigo tal como estaba, para tratar los demas casos
			for (int i = 0; i < lines.length; i++) {
				MInvoiceLine line = lines[i];
	
				// Update Order Line
	
				// performance: no instanciar los M, ejecutar UPDATE directo
				//MOrderLine ol = null;
	
				if (line.getC_OrderLine_ID() != 0) {
					// Si es débito verificar issotrx y realizar las operaciones
					// necesarias para incrementar la cantidad facturada
					orderLine = new MOrderLine(getCtx(), line.getC_OrderLine_ID(),
							get_TrxName());
					if(order == null || orderLine.getC_Order_ID() != order.getC_Order_ID()){
						order = new MOrder(getCtx(), orderLine.getC_Order_ID(),	get_TrxName());
					}
					if (isSOTrx() || (line.getM_Product_ID() == 0)) {
						if(isDebit){
							if (line.getQtyInvoiced() != null) {
								orderLine.setQtyInvoiced(orderLine.getQtyInvoiced().add(line.getQtyInvoiced()));
								if(!orderLine.save()){
									m_processMsg = "Could not update Order Line";		
									return DocAction.STATUS_Invalid;
								}
							}
						}
						else{
							// Si es crédito verificar issotrx y realizar las
							// operaciones necesarias para incrementar la cantidad
							// entregada y decrementar la cantidad reservada o pendiente
							orderLine.setQtyDelivered(orderLine.getQtyDelivered().add(line.getQtyInvoiced()));
							orderLine.setQtyReserved(orderLine.getQtyReserved().subtract(line.getQtyInvoiced()));
							if(!orderLine.save()){
								m_processMsg = "Could not update Order Line";		
								return DocAction.STATUS_Invalid;
							}
							// Actualizar el stock y el pendiente de entrega para ese producto
							if (!MStorage.add(getCtx(), order.getM_Warehouse_ID(),
									MStorage.getM_Locator_ID(
											order.getM_Warehouse_ID(),
											line.getM_Product_ID(),
											line.getM_AttributeSetInstance_ID(),
											line.getQtyInvoiced(), get_TrxName()),
									line.getM_Product_ID(), line
											.getM_AttributeSetInstance_ID(), line
											.getM_AttributeSetInstance_ID(),
									BigDecimal.ZERO,
									line.getQtyInvoiced().negate(),
									BigDecimal.ZERO, get_TrxName())) {
		                        m_processMsg = "Cannot correct Inventory";
		                        return DocAction.STATUS_Invalid;
		                    }
						}
	
					}
					
					if (!isSOTrx() && (line.getM_Product_ID() != 0)) {
						// MatchPO is created also from MInOut when Invoice exists
						// before Shipment
						BigDecimal matchQty = line.getQtyInvoiced();
						MMatchPO po = new MMatchPO(line, getDateInvoiced(),
								matchQty);
						if (!po.save(get_TrxName())) {
							m_processMsg = "Could not create PO Matching";
							return DocAction.STATUS_Invalid;
						} else {
							matchPO++;
						}
					}
				}
	
				// Matching - Inv-Shipment
	
				if (!isSOTrx() && (line.getM_InOutLine_ID() != 0)
						&& (line.getM_Product_ID() != 0)) {
					BigDecimal matchQty = line.getQtyInvoiced();
					MMatchInv inv = new MMatchInv(line, getDateInvoiced(), matchQty);
	
					if (!inv.save(get_TrxName())) {
						m_processMsg = "Could not create Invoice Matching";
	
						return DocAction.STATUS_Invalid;
					} else {
						matchInv++;
					}
				}
			} // for all lines
		
		}// fin else

		// Update BP Statistics

		MBPartner bp = new MBPartner(getCtx(), getC_BPartner_ID(),
				get_TrxName());

		// Update total revenue and balance / credit limit (reversed on
		// AllocationLine.processIt)

		BigDecimal invAmt = MConversionRate.convertBase(
				getCtx(),
				getGrandTotal(true), // CM adjusted
				getC_Currency_ID(), getDateAcct(), 0, getAD_Client_ID(),
				getAD_Org_ID());

		// Modified by Matías Cap
		// Las consultas y validaciones de cuenta corriente se deben manejar por
		// las nuevas clases que tienen esta implementación centralizada.
		// Verifico estado de crédito con la información de la factura 
		if (!isCurrentAccountVerified && isSOTrx()
				&& getPaymentRule().equals(PAYMENTRULE_OnCredit)) {
			// Obtengo el manager actual
			CurrentAccountManager manager = CurrentAccountManagerFactory
					.getManager();
			// Verificar el crédito con la factura y pedido asociado
			CallResult result = manager.invoiceWithinCreditLimit(getCtx(),
					new MOrg(getCtx(), Env.getAD_Org_ID(getCtx()),
							get_TrxName()), bp, invAmt, get_TrxName());
			// Si hubo error, obtengo el mensaje y retorno inválido
			if (result.isError()) {
				m_processMsg = result.getMsg();
				return DocAction.STATUS_Invalid;
			}
		}

		// User - Last Result/Contact

		if (getAD_User_ID() != 0) {
			MUser user = new MUser(getCtx(), getAD_User_ID(), get_TrxName());

			user.setLastContact(new Timestamp(System.currentTimeMillis()));
			user.setLastResult(Msg.translate(getCtx(), "C_Invoice_ID") + ": "
					+ getDocumentNo());

			if (!user.save(get_TrxName())) {
				m_processMsg = "Could not update Business Partner User";

				return DocAction.STATUS_Invalid;
			}
		} // user

		// Update Project

		if (isSOTrx() && (getC_Project_ID() != 0)) {
			MProject project = new MProject(getCtx(), getC_Project_ID(),
					get_TrxName());
			BigDecimal amt = getGrandTotal(true);
			int C_CurrencyTo_ID = project.getC_Currency_ID();

			if (C_CurrencyTo_ID != getC_Currency_ID()) {
				amt = MConversionRate.convert(getCtx(), amt,
						getC_Currency_ID(), C_CurrencyTo_ID, getDateAcct(), 0,
						getAD_Client_ID(), getAD_Org_ID());
			}

			BigDecimal newAmt = project.getInvoicedAmt();

			if (newAmt == null) {
				newAmt = amt;
			} else {
				newAmt = newAmt.add(amt);
			}

			log.fine("GrandTotal=" + getGrandTotal(true) + "(" + amt
					+ ") Project " + project.getName() + " - Invoiced="
					+ project.getInvoicedAmt() + "->" + newAmt);
			project.setInvoicedAmt(newAmt);

			if (!project.save(get_TrxName())) {
				m_processMsg = "Could not update Project";

				return DocAction.STATUS_Invalid;
			}
		} // project

		// User Validation

		String valid = ModelValidationEngine.get().fireDocValidate(this,
				ModelValidator.TIMING_AFTER_COMPLETE);

		if (valid != null) {
			m_processMsg = valid;

			return DocAction.STATUS_Invalid;
		}

		// Crear el document discount a partir del descuento manual general de
		// la cabecera
		if (isSOTrx()
				&& getManualGeneralDiscount().compareTo(BigDecimal.ZERO) != 0) {
			// Obtener la suma de los descuentos de la línea
			BigDecimal totalLineDiscountAmt = getSumColumnLines("LineDiscountAmt");
			BigDecimal totalPriceListLines = getSumColumnLines("PriceList * QtyInvoiced");
			MDocumentDiscount documentDiscount = new MDocumentDiscount(getCtx(), 0, get_TrxName());
			// Asigna las referencias al documento
			documentDiscount.setC_Invoice_ID(getID());
			// Asigna los importes y demás datos del descuento
			documentDiscount.setDiscountBaseAmt(totalPriceListLines);
			documentDiscount.setDiscountAmt(totalLineDiscountAmt);
			documentDiscount
					.setCumulativeLevel(MDocumentDiscount.CUMULATIVELEVEL_Document);
			documentDiscount
					.setDiscountApplication(MDocumentDiscount.DISCOUNTAPPLICATION_DiscountToPrice);
			documentDiscount.setTaxRate(null);
			documentDiscount
					.setDiscountKind(MDocumentDiscount.DISCOUNTKIND_ManualGeneralDiscount);
			
			// Si no se puede guardar aborta la operación
			if (!documentDiscount.save()) {
				m_processMsg = CLogger.retrieveErrorAsString();
				return DocAction.STATUS_Invalid;
			}
		}
		
		// Counter Documents

		MInvoice counter = createCounterDoc();

		if (counter != null) {
			info.append(" - @CounterDoc@: @C_Invoice_ID@=").append(
					counter.getDocumentNo());
		}
		
		// Caja Diaria. Intenta registrar la factura
		if (!MPOSJournal.registerDocument(this)) {
			m_processMsg = MPOSJournal.DOCUMENT_COMPLETE_ERROR_MSG;
			return STATUS_Invalid;
		}
		
		// Verifico si el gestor de cuentas corrientes debe realizar operaciones
		// antes de completar y eventualmente disparar la impresión fiscal
		// Obtengo el manager actual
		if(isUpdateBPBalance()){
			CurrentAccountManager manager = CurrentAccountManagerFactory.getManager();
			// Actualizo el balance
			CallResult result = manager.performAditionalWork(getCtx(), new MOrg(
					getCtx(), Env.getAD_Org_ID(getCtx()), get_TrxName()), bp, this,
					false, get_TrxName());
			// Si hubo error, obtengo el mensaje y retorno inválido
			if (result.isError()) {
				m_processMsg = result.getMsg();
				return DocAction.STATUS_Invalid;
			}
			// Me guardo el resultado en la variable de instancia para luego
			// utilizarla en afterProcessDocument
			getAditionalWorkResult().put(this, result.getResult());
		}
		

		// LOCALIZACION ARGENTINA
		// Emisión de la factura por controlador fiscal
		if (requireFiscalPrint() && !isIgnoreFiscalPrint()) {
			String errorMsg = doFiscalPrint();
			if (errorMsg != null) {
				m_processMsg = errorMsg;
				return STATUS_Invalid;
			}
		}
		
		/**
		 * @agregado: Horacio Alvarez - Servicios Digitales S.A.
		 * @fecha: 2009-06-16
		 * @fecha: 2011-06-25 modificado para soportar WSFEv1.0
		 * 
		 */
		if (localeARActive
				& MDocType.isElectronicDocType(getC_DocTypeTarget_ID())) {
			if (getcaecbte() != getNumeroComprobante()) {
				ProcessorWSFE processor = new ProcessorWSFE(this);
				String errorMsg = processor.generateCAE();
				if (errorMsg != null) {
					setcaeerror(errorMsg);
					m_processMsg = errorMsg;
					log.log(Level.SEVERE, "CAE Error: " + errorMsg);
					return DocAction.STATUS_Invalid;
				} else {
					setcae(processor.getCAE());
					setvtocae(processor.getDateCae());
					setcaeerror(null);
					int nroCbte = Integer.parseInt(processor.getNroCbte());
					this.setNumeroComprobante(nroCbte);
					log.log(Level.SEVERE, "CAE: " + processor.getCAE());
					log.log(Level.SEVERE, "DATE CAE: " + processor.getDateCae());
				}
			}
		}

		m_processMsg = info.toString().trim();
		setProcessed(true);
		setDocAction(DOCACTION_Close);

		return DocAction.STATUS_Completed;
	} // completeIt

	/**
	 * Descripción de Método
	 * 
	 * 
	 * @return
	 */

	private MInvoice createCounterDoc() {

		// Is this a counter doc ?

		if (getRef_Invoice_ID() != 0) {
			return null;
		}

		// Org Must be linked to BPartner

		MOrg org = MOrg.get(getCtx(), getAD_Org_ID());
		int counterC_BPartner_ID = org.getLinkedC_BPartner_ID();

		if (counterC_BPartner_ID == 0) {
			return null;
		}

		// Business Partner needs to be linked to Org

		MBPartner bp = new MBPartner(getCtx(), getC_BPartner_ID(), null);
		int counterAD_Org_ID = bp.getAD_OrgBP_ID_Int();

		if (counterAD_Org_ID == 0) {
			return null;
		}

		MBPartner counterBP = new MBPartner(getCtx(), counterC_BPartner_ID,
				null);
		MOrgInfo counterOrgInfo = MOrgInfo.get(getCtx(), counterAD_Org_ID);

		log.info("Counter BP=" + counterBP.getName());

		// Document Type

		int C_DocTypeTarget_ID = 0;
		MDocTypeCounter counterDT = MDocTypeCounter.getCounterDocType(getCtx(),
				getC_DocType_ID());

		if (counterDT != null) {
			log.fine(counterDT.toString());

			if (!counterDT.isCreateCounter() || !counterDT.isValid()) {
				return null;
			}

			C_DocTypeTarget_ID = counterDT.getCounter_C_DocType_ID();
		} else // indirect
		{
			C_DocTypeTarget_ID = MDocTypeCounter.getCounterDocType_ID(getCtx(),
					getC_DocType_ID());
			log.fine("Indirect C_DocTypeTarget_ID=" + C_DocTypeTarget_ID);

			if (C_DocTypeTarget_ID <= 0) {
				return null;
			}
		}

		// Deep Copy

		MInvoice counter = copyFrom(this, getDateInvoiced(),
				C_DocTypeTarget_ID, !isSOTrx(), true, get_TrxName(), true);

		//

		counter.setAD_Org_ID(counterAD_Org_ID);

		// counter.setM_Warehouse_ID(counterOrgInfo.getM_Warehouse_ID());
		//

		counter.setBPartner(counterBP);

		// Refernces (Should not be required

		counter.setSalesRep_ID(getSalesRep_ID());
		counter.save(get_TrxName());

		// Update copied lines

		MInvoiceLine[] counterLines = counter.getLines(true);

		for (int i = 0; i < counterLines.length; i++) {
			MInvoiceLine counterLine = counterLines[i];

			counterLine.setInvoice(counter); // copies header values (BP,
			// etc.)
			counterLine.setPrice();
			counterLine.setTax();

			//

			counterLine.save(get_TrxName());
		}

		log.fine(counter.toString());

		// Document Action

		if (counterDT != null) {
			if (counterDT.getDocAction() != null) {
				// Bypass para validaciones de crédito de entidad comercial
				counter.setCurrentAccountVerified(true);
				// Bypass para actualización de crédito de entidad comercial
				counter.setUpdateBPBalance(false);
				counter.setDocAction(counterDT.getDocAction());
				counter.processIt(counterDT.getDocAction());
				counter.save(get_TrxName());
			}
		}

		return counter;
	} // createCounterDoc

	/**
	 * Descripción de Método
	 * 
	 * 
	 * @return
	 */

	public boolean postIt() {
		log.info(toString());

		return false;
	} // postIt

	/**
	 * Descripción de Método
	 * 
	 * 
	 * @return
	 */

	public boolean voidIt() {
		log.info(toString());

		if (DOCSTATUS_Closed.equals(getDocStatus())
				|| DOCSTATUS_Reversed.equals(getDocStatus())
				|| DOCSTATUS_Voided.equals(getDocStatus())) {
			m_processMsg = "Document Closed: " + getDocStatus();
			setDocAction(DOCACTION_None);

			return false;
		}

		// Not Processed

		if (DOCSTATUS_Drafted.equals(getDocStatus())
				|| DOCSTATUS_Invalid.equals(getDocStatus())
				|| DOCSTATUS_InProgress.equals(getDocStatus())
				|| DOCSTATUS_Approved.equals(getDocStatus())
				|| DOCSTATUS_NotApproved.equals(getDocStatus())) {

			// Set lines to 0

			MInvoiceLine[] lines = getLines(false);

			for (int i = 0; i < lines.length; i++) {
				MInvoiceLine line = lines[i];
				BigDecimal old = line.getQtyInvoiced();

				if (old.compareTo(Env.ZERO) != 0) {
					line.setQty(Env.ZERO);
					line.setTaxAmt(Env.ZERO);
					line.setLineNetAmt(Env.ZERO);
					line.setLineTotalAmt(Env.ZERO);
					line.addDescription(Msg.getMsg(getCtx(), "Voided") + " ("
							+ old + ")");

					// Unlink Shipment

					if (line.getM_InOutLine_ID() != 0) {
						MInOutLine ioLine = new MInOutLine(getCtx(), line
								.getM_InOutLine_ID(), get_TrxName());

						ioLine.setIsInvoiced(false);
						ioLine.save(get_TrxName());
						line.setM_InOutLine_ID(0);
					}

					line.save(get_TrxName());
				}
			}

			addDescription(Msg.getMsg(getCtx(), "Voided"));
			setIsPaid(true);
			setC_Payment_ID(0);
		} else {
			return reverseCorrectIt();
		}
		
		setProcessed(true);
		setDocAction(DOCACTION_None);

		return true;
	} // voidIt

	/**
	 * Descripción de Método
	 * 
	 * 
	 * @return
	 */

	public boolean closeIt() {
		log.info(toString());
		setProcessed(true);
		setDocAction(DOCACTION_None);

		return true;
	} // closeIt

	/**
	 * Descripción de Método
	 * 
	 * 
	 * @return
	 */

	public boolean reverseCorrectIt() {
		setAditionalWorkResult(new HashMap<PO, Object>());
		log.info(toString());

		// Disytel - Franco Bonafine
		// No es posible anular o revertir facturas que se encuentran en alguna
		// asignación.
		// Primero se deben revertir las asignaciones y luego anular la factura.
		// En caso de que exista, se ignora la asignación que causó la anulación
		// de este pago
		if (isInAllocation(getVoiderAllocationID())) {
			m_processMsg = "@FreeInvoiceNeededError@";
			return false;
		}

		boolean localeARActive = CalloutInvoiceExt
				.ComprobantesFiscalesActivos();
		MDocType docType = MDocType.get(getCtx(), getC_DocType_ID());
		MDocType reversalDocType = null;

		// Deep Copy

		MInvoice reversal = copyFrom(this, getDateInvoiced(),
				getC_DocType_ID(), isSOTrx(), false, get_TrxName(), true);

		if (reversal == null) {
			m_processMsg = "Could not create Invoice Reversal";

			return false;
		}
		// Se agregan los listeners de DocAction que tiene esta Invoice
		// a la reversal.
		reversal.copyDocActionStatusListeners(this);

		// ////////////////////////////////////////////////////////////////
		// LOCALIZACIÓN ARGENTINA
		// Para la localización argentina es necesario contemplar el tipo
		// de documento a anular a fin de determinar el tipo de documento
		// del documento generado (reversal).
		if (localeARActive & isSOTrx()) {
			// Se obtiene la clave base del tipo de documento.
			String reversalDocTypeBaseKey = reverseDocTypes.get(docType
					.getBaseKey());
			// Si el tipo de documento de esta MInvoice tiene inverso, se cambia
			// el tipo de documento del contramovimiento por el indicado.
			if (reversalDocTypeBaseKey != null) {
				// Se obtiene el tipo de documento del contramovimiento.
				reversalDocType = MDocType.getDocType(getCtx(),
						reversalDocTypeBaseKey, getLetra(), getPuntoDeVenta(),
						get_TrxName());
				// Se asigna el tipo de documento nuevo.
				reversal.setC_DocTypeTarget_ID(reversalDocType
						.getC_DocType_ID());
				reversal.setC_DocType_ID(reversalDocType.getC_DocType_ID());

				if (reversalDocType
						.isDocType(MDocType.DOCTYPE_CustomerCreditNote))
					reversal.setC_Invoice_Orig_ID(getC_Invoice_ID());
			}
			
			reversal.setFiscalAlreadyPrinted(false);
			// Se ignora la impresión fiscal del reversal si esta factura no ha
			// sido emitida fiscalmente.
			reversal.setIgnoreFiscalPrint(!isFiscalAlreadyPrinted());
		}

		// Para la localización argentina no hay que invertir las cantidades ni
		// los montos.
		if (!localeARActive || !isSOTrx()) {

			// Reverse Line Qty
			MInvoiceLine[] rLines = reversal.getLines(false);

			for (int i = 0; i < rLines.length; i++) {
				MInvoiceLine rLine = rLines[i];

				rLine.setQtyEntered(rLine.getQtyEntered().negate());
				rLine.setQtyInvoiced(rLine.getQtyInvoiced().negate());
				rLine.setLineNetAmt(rLine.getLineNetAmt().negate());

				if ((rLine.getTaxAmt() != null)
						&& (rLine.getTaxAmt().compareTo(Env.ZERO) != 0)) {
					rLine.setTaxAmt(rLine.getTaxAmt().negate());
				}

				if ((rLine.getLineTotalAmt() != null)
						&& (rLine.getLineTotalAmt().compareTo(Env.ZERO) != 0)) {
					rLine.setLineTotalAmt(rLine.getLineTotalAmt().negate());
				}

				if (!rLine.save(get_TrxName())) {
					m_processMsg = "Could not correct Invoice Reversal Line";

					return false;
				}
			}
		} // !localeARActive

		reversal.setC_Order_ID(getC_Order_ID());
		reversal.addDescription("{->" + getDocumentNo() + ")");
		reversal.setFiscalAlreadyPrinted(false);
		// No confirmo el trabajo adicional de cuentas corrientes porque se debe
		// realizar luego de anular la factura
		reversal.setConfirmAditionalWorks(false);
		reversal.setCurrentAccountVerified(true);
		//

		if (!reversal.processIt(DocAction.ACTION_Complete)) {
			m_processMsg = "Reversal ERROR: " + reversal.getProcessMsg();

			return false;
		}
		// Me traigo el trabajo adicional de cuentas corrientes y lo confirmo
		// después 
		getAditionalWorkResult().put(reversal,
				reversal.getAditionalWorkResult().get(reversal));
		
		reversal.setC_Payment_ID(0);
		reversal.setIsPaid(true);
		reversal.closeIt();		
		// Disytel - FB
		// Dejamos como Revertido el documento inverso a fin de mantener la
		// consistencia
		// de visibilidad con el documento revertido, de modo que ambos
		// documentos aparezcan
		// en el mismo lugar
		// reversal.setDocStatus( DOCSTATUS_Closed );
		reversal.setDocStatus(DOCSTATUS_Reversed);

		reversal.setDocAction(DOCACTION_None);
		reversal.save(get_TrxName());

		//

		addDescription("(" + reversal.getDocumentNo() + "<-)");

		// Clean up Reversed (this) [thanks Victor!]

		MInvoiceLine[] iLines = getLines(false);

		for (int i = 0; i < iLines.length; i++) {
			MInvoiceLine iLine = iLines[i];

			if (iLine.getM_InOutLine_ID() != 0) {
				MInOutLine ioLine = new MInOutLine(getCtx(), iLine
						.getM_InOutLine_ID(), get_TrxName());

				ioLine.setIsInvoiced(false);
				ioLine.save(get_TrxName());

				// Reconsiliation

				iLine.setM_InOutLine_ID(0);
				iLine.save(get_TrxName());
			}
		}
		
		// Si se debe actualizar el saldo de la entidad comercial entonces
		// realizar el trabajo adicional para esta factura
		if(isUpdateBPBalance()){
			MBPartner bp = new MBPartner(getCtx(), getC_BPartner_ID(),
					get_TrxName());
			CurrentAccountManager manager = CurrentAccountManagerFactory.getManager();
			// Actualizo el balance
			CallResult result = manager.performAditionalWork(getCtx(), new MOrg(
					getCtx(), Env.getAD_Org_ID(getCtx()), get_TrxName()), bp, this,
					false, get_TrxName());
			// Si hubo error, obtengo el mensaje y retorno inválido
			if (result.isError()) {
				m_processMsg = result.getMsg();
				return false;
			}
			// Me guardo el resultado en la variable de instancia para luego
			// utilizarla en afterProcessDocument
			getAditionalWorkResult().put(this, result.getResult());
		}

		setProcessed(true);
		setDocStatus(DOCSTATUS_Reversed);
		setDocAction(DOCACTION_None);

		StringBuffer info = new StringBuffer(reversal.getDocumentNo());
		
		// Reverse existing Allocations

		save(); // for allocation reversal

		// Disytel FB - Ya no se desasignan automáticamente los pagos. Se debe
		// revertir la asignación manualmente.
		/*
		 * --------------> MAllocationHdr[] allocations =
		 * MAllocationHdr.getOfInvoice(getCtx(), getC_Invoice_ID(),
		 * get_TrxName());
		 * 
		 * for (int i = 0; i < allocations.length; i++) {
		 * allocations[i].setDocAction(DocAction.ACTION_Reverse_Correct);
		 * allocations[i].reverseCorrectIt();
		 * allocations[i].save(get_TrxName()); }
		 * 
		 * load(get_TrxName()); // reload allocation reversal info
		 * <-----------------
		 */
		//

		m_processMsg = info.toString();
		reversal.setC_Payment_ID(0);
		reversal.setIsPaid(true);

		// ////////////////////////////////////////////////////////////////
		// LOCALIZACIÓN ARGENTINA
		// Se crea una imputación entre el documento anulado y el generado.
		if (localeARActive && isSOTrx()) {
			// Se crea el la imputación con la fecha de facturación.
			MAllocationHdr allocHdr = new MAllocationHdr(getCtx(), false,
					getDateInvoiced(), getC_Currency_ID(), "Anulación de "
							+ docType.getPrintName() + " número "
							+ getDocumentNo(), get_TrxName());

			if (!allocHdr.save()) {
				m_processMsg = "Could not create reversal allocation header";
				return false;
			}
			// Se crea la línea de imputación.
			MAllocationLine allocLine = new MAllocationLine(allocHdr);
			allocLine.setC_Invoice_ID(getC_Invoice_ID());
			allocLine.setC_Invoice_Credit_ID(reversal.getC_Invoice_ID());
			allocLine.setAmount(getGrandTotal());
			allocLine.setWriteOffAmt(BigDecimal.ZERO);
			allocLine.setDiscountAmt(BigDecimal.ZERO);
			allocLine.setOverUnderAmt(BigDecimal.ZERO);
			allocLine.setC_BPartner_ID(getC_BPartner_ID());

			if (!allocLine.save()) {
				m_processMsg = "Could not create reversal allocation line";
				return false;
			}
			allocHdr.setUpdateBPBalance(false);
			// Se completa la imputación.
			allocHdr.processIt(MAllocationHdr.ACTION_Complete);
			allocHdr.save();
		}

		return true;
	} // reverseCorrectIt

	/**
	 * Descripción de Método
	 * 
	 * 
	 * @return
	 */

	public boolean reverseAccrualIt() {
		log.info(toString());

		return false;
	} // reverseAccrualIt

	/**
	 * Descripción de Método
	 * 
	 * 
	 * @return
	 */

	public boolean reActivateIt() {
		log.info(toString());
		calculateTaxTotal();
		return false;
	} // reActivateIt

	/**
	 * Descripción de Método
	 * 
	 * 
	 * @return
	 */

	public String getSummary() {
		StringBuffer sb = new StringBuffer();

		sb.append(getDocumentNo());

		// : Grand Total = 123.00 (#1)

		sb.append(": ").append(Msg.translate(getCtx(), "GrandTotal")).append(
				"=").append(getGrandTotal()).append(" (#").append(
				getLines(false).length).append(")");

		// - Description

		if ((getDescription() != null) && (getDescription().length() > 0)) {
			sb.append(" - ").append(getDescription());
		}

		return sb.toString();
	} // getSummary

	/**
	 * Descripción de Método
	 * 
	 * 
	 * @return
	 */

	public int getDoc_User_ID() {
		return getSalesRep_ID();
	} // getDoc_User_ID

	/**
	 * Descripción de Método
	 * 
	 * 
	 * @return
	 */

	public BigDecimal getApprovalAmt() {
		return getGrandTotal();
	} // getApprovalAmt

	public void calculateTotal() {
		setGrandTotal(getTotalLines().add(totalTax()));
	}

	public String getLetra() {
		String letra = null;
		if (getC_Letra_Comprobante_ID() != 0) {
			MLetraComprobante mLetraComprobante = new MLetraComprobante(Env
					.getCtx(), getC_Letra_Comprobante_ID(), null);
			letra = mLetraComprobante.getLetra();
		}
		return letra;
	}

	private boolean validateInvoice(MBPartner bp) {
		try{
			// Valida el límite de consumidor final
			FiscalDocumentPrint.validateInvoiceCFLimit(getCtx(), bp, this,
					get_TrxName());
		} catch(Exception e){
			log.saveError("", e.getMessage());
			return false;
		}
		return true;
	}

	private void setDocumentNo() {
		MDocType docType = MDocType.get(getCtx(), getC_DocTypeTarget_ID());
		int posNumber = docType.getPosNumber();
		String letra = docType.getLetter();
		int nroComprobante = CalloutInvoiceExt.getNextNroComprobante(docType
				.getC_DocType_ID());
		setDocumentNo(CalloutInvoiceExt.GenerarNumeroDeDocumento(posNumber,
				nroComprobante, letra, isSOTrx()));
	}

	/**
	 * Verifica si la factura se encuentra en alguna asignación válida del
	 * sistema.
	 * 
	 * @param exceptAllocIDs
	 *            IDs de asignaciones que se deben ignorar para determinar la
	 *            condición de existencia.
	 * @return Verdadero en caso de que exista al menos una asignación en estado
	 *         CO o CL que contenga una línea activa cuya factura de débito o
	 *         crédito es esta factura.
	 */
	protected boolean isInAllocation(Integer[] exceptAllocIDs) {
		StringBuffer sql = new StringBuffer();
		sql.append(" SELECT COUNT(*) ");
		sql.append(" FROM C_AllocationLine al ");
		sql
				.append(" INNER JOIN C_AllocationHdr ah ON (al.C_AllocationHdr_ID = ah.C_AllocationHdr_ID) ");
		sql.append(" WHERE al.IsActive = 'Y' AND ah.DocStatus IN ('CO','CL') ");
		sql
				.append("   AND (al.C_Invoice_ID = ? OR al.C_Invoice_Credit_ID = ?) ");
		if (exceptAllocIDs.length > 0) {
			sql.append(" AND ah.C_AllocationHdr_ID NOT IN (");
			for (int i = 0; i < exceptAllocIDs.length; i++) {
				Integer allocID = exceptAllocIDs[i];
				sql.append(allocID);
				sql.append(i == exceptAllocIDs.length - 1 ? ")" : ",");
			}
		}
		long allocCount = (Long) DB.getSQLObject(get_TrxName(), sql.toString(),
				new Object[] { getC_Invoice_ID(), getC_Invoice_ID() });
		return allocCount > 0;
	}

	/**
	 * Verifica si la factura se encuentra en alguna asignación válida del
	 * sistema.
	 * 
	 * @param exceptThisAllocID
	 *            ID de asignacion que se deben ignorar para determinar la
	 *            condición de existencia. Si el parámetro es NULL entonces no
	 *            se ignora ninguna asignación.
	 * @return Verdadero en caso de que exista al menos una asignación en estado
	 *         CO o CL que contenga una línea activa cuya factura de débito o
	 *         crédito es esta factura.
	 */
	protected boolean isInAllocation(Integer exceptThisAllocID) {
		Integer[] exceptAllocs;
		if (exceptThisAllocID != null)
			exceptAllocs = new Integer[] { exceptThisAllocID };
		else
			exceptAllocs = new Integer[] {};
		return isInAllocation(exceptAllocs);
	}

	/**
	 * Verifica si la factura se encuentra en alguna asignación válida del
	 * sistema.
	 * 
	 * @return Verdadero en caso de que exista al menos una asignación en estado
	 *         CO o CL que contenga una línea activa cuya factura de débito o
	 *         crédito es esta factura.
	 */
	protected boolean isInAllocation() {
		return isInAllocation(new Integer[] {});
	}

	/**
	 * ID de la asignación que intenta anular esta factura. En el caso de que
	 * desde una asignación se quiera anular una factura, es necesario que este
	 * factura sepa cual es la asignación que la está anulando para evitar la
	 * validación de asignaciones de facturas, de modo que la asignación
	 * anuladora no se tenga en cuenta en la validación.
	 */
	private Integer voiderAllocationID = null;

	/**
	 * @return the voiderAllocationID
	 */
	public Integer getVoiderAllocationID() {
		return voiderAllocationID;
	}

	/**
	 * @param voiderAllocationID
	 *            the voiderAllocationID to set
	 */
	public void setVoiderAllocationID(Integer voiderAllocationID) {
		this.voiderAllocationID = voiderAllocationID;
	}

	public BigDecimal getTaxesAmt() {
		MInvoiceTax[] taxes = getTaxes(false);
		BigDecimal total = Env.ZERO;
		for (int i = 0; i < taxes.length; i++) {
			total = total.add(taxes[i].getTaxAmt());
		}
		return total;
	}
	
	public BigDecimal getTotalLinesNet() {
		BigDecimal total = Env.ZERO;
		for (MInvoiceLine invoiceLine : getLines()) {
			// Total de líneas sin impuestos
			total = total.add(invoiceLine.getTotalPriceEnteredNet());
		}
		return total;
	}

	@Override
	public BigDecimal getChargeAmt() {
		return super.getChargeAmt() == null ? BigDecimal.ZERO : super
				.getChargeAmt();
	}

	private List<MDocumentDiscount> discounts = null;

	/**
	 * @return Los descuentos calculados para esta factura. Si no tiene
	 *         descuentos devuelve una lista vacía.
	 */
	public List<MDocumentDiscount> getDiscounts() {
		if (discounts == null) {
			discounts = MDocumentDiscount.getOfInvoice(getC_Invoice_ID(),
					getCtx(), get_TrxName());
		}
		return discounts;
	}

	/**
	 * @return la suma de todos los descuentos aplicados. Los descuentos se
	 *         toman del método {@link MInvoice#getDiscounts()}.
	 */
	public BigDecimal getDiscountsAmt(){
		BigDecimal discountAmt = BigDecimal.ZERO;
		for (MDocumentDiscount discount : getDiscounts()) {
			discountAmt = discountAmt.add(discount.getDiscountAmt());
		}
		return discountAmt;
	}

	public MBPartnerLocation getBPartnerLocation() {
		return new MBPartnerLocation(getCtx(), getC_BPartner_Location_ID(),
				get_TrxName());
	}

	public void setCurrentAccountVerified(boolean isCurrentAccountVerified) {
		this.isCurrentAccountVerified = isCurrentAccountVerified;
	}

	public boolean isCurrentAccountVerified() {
		return isCurrentAccountVerified;
	}

	public void setUpdateBPBalance(boolean updateBPBalance) {
		this.updateBPBalance = updateBPBalance;
	}

	public boolean isUpdateBPBalance() {
		return updateBPBalance;
	}

	/**
	 * Determina si la factura parámetro en la fecha parámetro está vencida e
	 * impaga (si el parámetro así lo requiera). La factura está vencida e
	 * impaga cuando:
	 * <ul>
	 * <li>Los esquemas de pago de la factura vencidos (a fecha parámetro)
	 * tienen monto pendiente.</li>
	 * <li>Si no tiene esquema de pago, determino la configuración adicional del
	 * esquema de vencimiento relacionado. Esta configuración puede ser:
	 * Siguiente fecha hábil, Días Neto, Fecha de vencimiento fija. Este es el
	 * orden por el cual se determina el vencimiento.</li>
	 * </ul>
	 * 
	 * @param ctx
	 *            contexto
	 * @param invoice
	 *            factura
	 * @param compareDate
	 *            fecha de comparación de vencimiento
	 * @param alsoUnpaided
	 *            verifica también además que este impago
	 * @param trxName
	 *            nombre de la transacción
	 * @return la diferencia de días entre la fecha de comparación y la fecha de
	 *         vencimiento, mayor a 0 está vencida e impaga dependiendo, 0 estamos en la fecha de vencimiento y menor a 0 
	 */
	public static Integer isPastDue(Properties ctx, MInvoice invoice, Date compareDate, boolean alsoUnpaided, String trxName){
		boolean pastDue = false;
		Integer diffDays = 0;
		// Si tiene un esquema de vencimientos asignado la factura parámetro lo
		// verifico 
		if(invoice.getC_PaymentTerm_ID() != 0){
			MInvoicePaySchedule[] invPaySchedules = MInvoicePaySchedule
					.getInvoicePaySchedule(ctx, invoice.getID(), 0, trxName);
			// Si hay esquema pago de factura verifico con la fecha de
			// vencimiento y acumulo el monto para luego verificar que esté
			// saldada completamente
			if (invPaySchedules != null && invPaySchedules.length > 0) {
				// Itero por el esquema de pagos de la factura
				for (int i = 0; i < invPaySchedules.length && !pastDue; i++) {
					// Determino la cantidad de días de diferencia entre la
					// fecha de comparación parámetro y la fecha de vencimiento
					// del pay schedule
					diffDays = invPaySchedules[i].diffDueDays(compareDate);
					// Si la diferencia es mayor a 0 significa que está vencida
					pastDue = diffDays > 0;
					// Si el parámetro alsoUnpaided es true, entonces verifico
					// también que no estén pagos los esquemas de pagos de
					// facturas
					if(pastDue && alsoUnpaided){
						try{
							// Obtengo el invoiceopen
							BigDecimal invoiceopen = (BigDecimal) DB
									.getSQLObject(
											trxName,
											"SELECT currencyBase(invoiceopen(?,?),?,?,?,?)",
											new Object[] { invoice.getID(),
													invPaySchedules[i].getID(),
													invoice.getC_Currency_ID(), 
													invoice.getDateInvoiced(),
													invoice.getAD_Client_ID(),
													invoice.getAD_Org_ID()});
							// Pendiente del esquema de pago actual 
							boolean unpaid = invoiceopen.compareTo(BigDecimal.ZERO) > 0;
							if(!unpaid){
								diffDays = 0;
							}
							pastDue = pastDue && unpaid;
						} catch(Exception e){
							s_log.severe("Error in isPastDue method from MInvoice");
							e.printStackTrace();
						}
					}					
				}
			}
		}
		return diffDays;
	}
	
	@Override
	public void setAuxiliarInfo(AuxiliarDTO auxDTO, boolean processed){
		auxDTO.setAuthCode(getAuthCode());
		// Monto convertido
		BigDecimal invAmt = MConversionRate.convertBase(
				getCtx(),
				getGrandTotal(true), // CM adjusted
				getC_Currency_ID(), getDateAcct(), 0, getAD_Client_ID(),
				getAD_Org_ID());
		auxDTO.setAmt(invAmt);
		auxDTO.setDateTrx(getDateInvoiced());
		auxDTO.setDocType(MCentralAux.DOCTYPE_Invoice);
		auxDTO.setDocumentNo(getDocumentNo());
		auxDTO.setPaymentRule(getPaymentRule());
		auxDTO.setTenderType(CurrentAccountBalanceStrategy
				.getTenderTypeEquivalent(getPaymentRule()));
		// Signo en base al tipo de doc
		MDocType docType = new MDocType(getCtx(), getC_DocTypeTarget_ID(), get_TrxName());
		auxDTO.setDocTypeKey(docType.getDocTypeKey());
		auxDTO.setSign(Integer.parseInt(docType.getsigno_issotrx()));
		auxDTO
				.setTransactionType(isSOTrx() ? MCentralAux.TRANSACTIONTYPE_Customer
						: MCentralAux.TRANSACTIONTYPE_Vendor);
		auxDTO.setDocStatus(processed ? getDocStatus() : getDocAction());
		// HACK: EL matching de autorización se setea falso porque después se
		// realiza en la eliminación de transacciones
		setAuthMatch(false);
		// Fecha de vencimiento (Si tiene varias cual paso?)
		//auxDTO.setDueDate();
	}

	public void setAditionalWorkResult(Map<PO, Object> aditionalWorkResult) {
		this.aditionalWorkResult = aditionalWorkResult;
	}

	public Map<PO, Object> getAditionalWorkResult() {
		return aditionalWorkResult;
	}

	public void setConfirmAditionalWorks(boolean confirmAditionalWorks) {
		this.confirmAditionalWorks = confirmAditionalWorks;
	}

	public boolean isConfirmAditionalWorks() {
		return confirmAditionalWorks;
	}
	
	/**
	 * @return Indica si esta factura requiere ser emitida por un controlador fiscal
	 */
	public boolean requireFiscalPrint() {
		return CalloutInvoiceExt.ComprobantesFiscalesActivos()
				&& MDocType.isFiscalDocType(getC_DocTypeTarget_ID());
	}

	/**
	 * Realiza la emisión fiscal de la factura mediante el controlador fiscal
	 * configurado en su tipo de documento
	 * 
	 * @return <code>null</code> si la impresión se realizó correctamente o el
	 *         mensaje de error si hubo algún error.
	 */
	public String doFiscalPrint() {
		// ////////////////////////////////////////////////////////////////
		// LOCALIZACIÓN ARGENTINA
		// Para la localización Argentina, si el tipo de documento está
		// configurado para imprimirse mediante un controlador fiscal,
		// se manda a emitir el comprobante a la impresora.
		if (requireFiscalPrint()) {
			// Aquí finaliza el guardado de documentos para TPV dado que a
			// partir de aquí se emite el comprobante mediante el controlador
			// fiscal. Si esta factura no está siendo completada por el TPV la
			// siguiente sentencia no produce ningún efecto.
			TimeStatsLogger.endTask(MeasurableTask.POS_SAVE_DOCUMENTS);

			TimeStatsLogger.beginTask(MeasurableTask.PRINT_FISCAL_INVOICE);

			// Impresor de comprobantes.
			CallResult printResult = FiscalPrintManager.printDocument(getCtx(),
					this, true, get_TrxName());
			if (printResult.isError()) {
				return !Util.isEmpty(printResult.getMsg()) ? printResult
						.getMsg() : Msg.getMsg(getCtx(),
						"PrintFiscalDocumentError");
			}
			
			// Impresor de comprobantes.
//			FiscalDocumentPrint fdp = new FiscalDocumentPrint();
//			fdp.setTrx(Trx.get(get_TrxName(), false));
//			fireDocActionStatusChanged(new DocActionStatusEvent(this,
//					DocActionStatusEvent.ST_FISCAL_PRINT_DOCUMENT,
//					new Object[] { fdp }));
//			if (!fdp.printDocument(this)) {
//				m_processMsg = fdp.getErrorMsg();
//				return DocAction.STATUS_Invalid;
//			}
			
			TimeStatsLogger.endTask(MeasurableTask.PRINT_FISCAL_INVOICE);
		}
		return null;
	}
	
	private boolean ignoreFiscalPrint = false;

	/**
	 * @return el valor de ignoreFiscalPrint
	 */
	public boolean isIgnoreFiscalPrint() {
		return ignoreFiscalPrint;
	}

	/**
	 * @param ignoreFiscalPrint el valor de ignoreFiscalPrint a asignar
	 */
	public void setIgnoreFiscalPrint(boolean ignoreFiscalPrint) {
		this.ignoreFiscalPrint = ignoreFiscalPrint;
	}

	/**
	 * @return true si la lista de precios asociada tiene el impuesto incluído
	 *         en el precio, false caso contrario
	 */
	public boolean isTaxIncluded() {
		MPriceList pl = MPriceList.get( getCtx(),getM_PriceList_ID(),get_TrxName());
        return pl.isTaxIncluded();
    }	
	
	/**
	 * Método para tratar la actulización de MOrderLines asociadas en el caso
	 * de que la factura sea de "debito" (el caso normal) y pertenezca al
	 * circuito de ventas (esto es, es una factura a cliente). La función de este
	 * método es incrementar la cantidad facturada (C_OrderLine.QtyInvoiced)
	 * en las lineas de pedido asociadas a las lineas de factura.
	 * Genera un solo acceso a DB si es necesario.
	 * @param lines lienas de factura a partir de las cuales
	 * @return false si no la actualizacion fallo por algun motivo
	 */
	private boolean updateOrderIsSOTrxDebit(MInvoiceLine[] lines)
	{
		//Ok, teoricamente no deberia poder haber 2 MInvoiceLIne de la misma
		//factura refiriendo a la misma MOrderLine; auqneu no parece
		//ser un restricción muy importante, se va a permitir esto (el codigo
		//orinal tambien los permitira
		
		//C_OrderLine_ID -> incremeto en QtyInvoiced
		HashMap<Integer,BigDecimal> molQtyInvoiced = new HashMap<Integer,BigDecimal>();
		
		for (int i = 0; i < lines.length; i++)
		{
			MInvoiceLine il = lines[i];
			if (il.getC_OrderLine_ID() <= 0)
				continue;
			int C_OrderLine_ID = il.getC_OrderLine_ID();
			BigDecimal qtyInvoiced = il.getQtyInvoiced();
			if (molQtyInvoiced.containsKey(C_OrderLine_ID))
			{//en general no deberia pasar; sig que dos lineas de la misma factura a la misma linea de pedido
				//se suman las cantidades
				qtyInvoiced = qtyInvoiced.add(molQtyInvoiced.get(C_OrderLine_ID));
				molQtyInvoiced.put(C_OrderLine_ID,qtyInvoiced);
			}else
			{
				molQtyInvoiced.put(C_OrderLine_ID,qtyInvoiced);
			}
			
		}
		
		//se crea la sentencia update
		
    	StringBuffer sb = new StringBuffer();
    	List<Integer> listIds = new ArrayList<Integer>();
    	
    	sb.append("UPDATE C_OrderLine SET QtyInvoiced  = QtyInvoiced + ( ");
    	sb.append(" CASE C_OrderLine_ID ");
    	
    	for (Integer id: molQtyInvoiced.keySet())
    	{
    		//este es el incremento para esta MOrderLine
    		BigDecimal qtyInvoiced = molQtyInvoiced.get(id);
    		if (qtyInvoiced.compareTo(BigDecimal.ZERO)==0)
    			continue; //0 incremento
    		listIds.add(id);
    		
    		sb.append(" WHEN ").append(id).append(" THEN ").append(qtyInvoiced);
    		   
    	}
       	sb.append(" END ) WHERE C_OrderLine_ID IN ");
        
    	if (listIds.size() <= 0) //no hay ninguna c_orderLine que actualizar
    		return true;
    	
    	sb.append(StringUtil.implodeForUnion(listIds)); //(12,2,4,89)
    	String queryUpdate = sb.toString();
    	
      	int qtyUpdated = 
       		DB.executeUpdate(queryUpdate,get_TrxName());
      	
      	if (qtyUpdated != listIds.size())
      		return false; //algo raro paso...
      	
		return true;
	}

	
	//Ader: Manjejo de caches mutidocumentos, tambien de utilidad por ej, para que reportes
	//no tarden tanto
	private MProductCache m_prodCache;
	public void initCaches()
	{
		MInvoiceLine[] lines = getLines();
		initCacheProdFromLines(lines);
		for (MInvoiceLine il: lines)
		{
			//propaga las caches a las lineas
			//esto probablemente se podria hacer tambine en getLines()
			//pero habria que hacerlo nuevametne aca 
			il.setProductCache(m_prodCache);
		}
	}
    /**
     * Carga en la cache de productos aquellos en la lineas (lines) que no se
     * encuentran en la misma. Un solo acceso a DB como máximo y solo si hay al menos un producto
     * no actualmete cacheado.
     * 
     * @param lines MInvoiceLines's a partir de la cual obtener los ids de los productos a cargar
     * @return false si al menos un producto no se pudo cargar en cache (en genral no deberia pasar)
     */
    private boolean initCacheProdFromLines(MInvoiceLine[] lines)
    {
    	if (m_prodCache == null)
    		m_prodCache = new MProductCache(getCtx(),get_TrxName());
    	
    	List<Integer> newIds = new ArrayList<Integer>();
    	
    	for (int i = 0; i < lines.length; i++)
    	{
    		MInvoiceLine il = lines[i];
    		int M_Product_ID = il.getM_Product_ID();
    		if (M_Product_ID <= 0)
    			continue;
    		if (m_prodCache.contains(M_Product_ID))
    			continue;
    		if (newIds.contains(M_Product_ID))
    		    continue;
    		newIds.add(M_Product_ID);
    	}
    	
    	if (newIds.size() <= 0)
    		return true; //nada para cargar, todos ya cacheados
    	
    	//carga masiva en cache; un solo acceso a DB
    	int qtyCached = m_prodCache.loadMasive(newIds);
    	
    	if (qtyCached != newIds.size())
    		return false; //algunos no se cargaron....
    	
    	return true;
    }

	public void setSkipManualGeneralDiscount(boolean skipManualGeneralDiscount) {
		this.skipManualGeneralDiscount = skipManualGeneralDiscount;
	}

	public boolean isSkipManualGeneralDiscount() {
		return skipManualGeneralDiscount;
	}

	public void setSkipApplyPaymentTerm(boolean skipApplyPaymentTerm) {
		this.skipApplyPaymentTerm = skipApplyPaymentTerm;
	}

	public boolean isSkipApplyPaymentTerm() {
		return skipApplyPaymentTerm;
	}
} // MInvoice

/*
 * @(#)MInvoice.java 02.07.07
 * 
 * Fin del fichero MInvoice.java
 * 
 * Versión 2.1
 */

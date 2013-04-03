package org.openXpertya.pos.model;

import java.math.BigDecimal;

public class CreditNotePayment extends Payment {

	private int invoiceID = 0;
	private BigDecimal availableAmt = null;

	/**
	 * Constructor de la clase
	 * @param invoiceID
	 * @param availableAmt
	 */
	public CreditNotePayment(int invoiceID, BigDecimal availableAmt) {
		super();
		this.invoiceID = invoiceID;
		this.availableAmt = availableAmt;
	}

	/**
	 * @return the invoiceID
	 */
	public int getInvoiceID() {
		return invoiceID;
	}
	
	/**
	 * @param invoiceID the invoiceID to set
	 */
	public void setInvoiceID(int invoiceID) {
		this.invoiceID = invoiceID;
	}
	
	/**
	 * @return the availableAmt
	 */
	public BigDecimal getAvailableAmt() {
		return availableAmt;
	}
	
	/**
	 * @param availableAmt the availableAmt to set
	 */
	public void setAvailableAmt(BigDecimal availableAmt) {
		this.availableAmt = availableAmt;
	}

	@Override
	public boolean isCreditNotePayment() {
		return true;
	}
	
}

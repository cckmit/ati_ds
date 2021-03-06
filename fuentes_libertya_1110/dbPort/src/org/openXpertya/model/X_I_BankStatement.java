/** Modelo Generado - NO CAMBIAR MANUALMENTE - Copyright (C) 2006 FUNDESLE */
package org.openXpertya.model;
import java.util.*;
import java.sql.*;
import java.math.*;
import org.openXpertya.util.*;
/** Modelo Generado por I_BankStatement
 *  @author Comunidad de Desarrollo openXpertya*         *Basado en Codigo Original Modificado, Revisado y Optimizado de:*         * Jorg Janke 
 *  @version  - 2008-01-03 10:26:34.484 */
public class X_I_BankStatement extends PO
{
/** Constructor estándar */
public X_I_BankStatement (Properties ctx, int I_BankStatement_ID, String trxName)
{
super (ctx, I_BankStatement_ID, trxName);
/** if (I_BankStatement_ID == 0)
{
setI_BankStatement_ID (0);
setI_IsImported (false);
}
 */
}
/** Load Constructor */
public X_I_BankStatement (Properties ctx, ResultSet rs, String trxName)
{
super (ctx, rs, trxName);
}
/** AD_Table_ID=600 */
public static final int Table_ID=600;

/** TableName=I_BankStatement */
public static final String Table_Name="I_BankStatement";

protected static KeyNamePair Model = new KeyNamePair(600,"I_BankStatement");
protected static BigDecimal AccessLevel = new BigDecimal(2);

/** Load Meta Data */
protected POInfo initPO (Properties ctx)
{
POInfo poi = POInfo.getPOInfo (ctx, Table_ID);
return poi;
}
public String toString()
{
StringBuffer sb = new StringBuffer ("X_I_BankStatement[").append(getID()).append("]");
return sb.toString();
}
/** Set Business Partner Key.
Key of the Business Partner */
public void setBPartnerValue (String BPartnerValue)
{
if (BPartnerValue != null && BPartnerValue.length() > 40)
{
log.warning("Length > 40 - truncated");
BPartnerValue = BPartnerValue.substring(0,39);
}
set_Value ("BPartnerValue", BPartnerValue);
}
/** Get Business Partner Key.
Key of the Business Partner */
public String getBPartnerValue() 
{
return (String)get_Value("BPartnerValue");
}
/** Set Bank Account No.
Bank Account Number */
public void setBankAccountNo (String BankAccountNo)
{
if (BankAccountNo != null && BankAccountNo.length() > 20)
{
log.warning("Length > 20 - truncated");
BankAccountNo = BankAccountNo.substring(0,19);
}
set_Value ("BankAccountNo", BankAccountNo);
}
/** Get Bank Account No.
Bank Account Number */
public String getBankAccountNo() 
{
return (String)get_Value("BankAccountNo");
}
/** Set Business Partner .
Identifies a Business Partner */
public void setC_BPartner_ID (int C_BPartner_ID)
{
if (C_BPartner_ID <= 0) set_Value ("C_BPartner_ID", null);
 else 
set_Value ("C_BPartner_ID", new Integer(C_BPartner_ID));
}
/** Get Business Partner .
Identifies a Business Partner */
public int getC_BPartner_ID() 
{
Integer ii = (Integer)get_Value("C_BPartner_ID");
if (ii == null) return 0;
return ii.intValue();
}
/** Set Bank Account.
Account at the Bank */
public void setC_BankAccount_ID (int C_BankAccount_ID)
{
if (C_BankAccount_ID <= 0) set_Value ("C_BankAccount_ID", null);
 else 
set_Value ("C_BankAccount_ID", new Integer(C_BankAccount_ID));
}
/** Get Bank Account.
Account at the Bank */
public int getC_BankAccount_ID() 
{
Integer ii = (Integer)get_Value("C_BankAccount_ID");
if (ii == null) return 0;
return ii.intValue();
}
/** Set Bank statement line.
Line on a statement from this Bank */
public void setC_BankStatementLine_ID (int C_BankStatementLine_ID)
{
if (C_BankStatementLine_ID <= 0) set_Value ("C_BankStatementLine_ID", null);
 else 
set_Value ("C_BankStatementLine_ID", new Integer(C_BankStatementLine_ID));
}
/** Get Bank statement line.
Line on a statement from this Bank */
public int getC_BankStatementLine_ID() 
{
Integer ii = (Integer)get_Value("C_BankStatementLine_ID");
if (ii == null) return 0;
return ii.intValue();
}
/** Set Bank Statement.
Bank Statement of account */
public void setC_BankStatement_ID (int C_BankStatement_ID)
{
if (C_BankStatement_ID <= 0) set_Value ("C_BankStatement_ID", null);
 else 
set_Value ("C_BankStatement_ID", new Integer(C_BankStatement_ID));
}
/** Get Bank Statement.
Bank Statement of account */
public int getC_BankStatement_ID() 
{
Integer ii = (Integer)get_Value("C_BankStatement_ID");
if (ii == null) return 0;
return ii.intValue();
}
/** Set Charge.
Additional document charges */
public void setC_Charge_ID (int C_Charge_ID)
{
if (C_Charge_ID <= 0) set_Value ("C_Charge_ID", null);
 else 
set_Value ("C_Charge_ID", new Integer(C_Charge_ID));
}
/** Get Charge.
Additional document charges */
public int getC_Charge_ID() 
{
Integer ii = (Integer)get_Value("C_Charge_ID");
if (ii == null) return 0;
return ii.intValue();
}
/** Set Currency.
The Currency for this record */
public void setC_Currency_ID (int C_Currency_ID)
{
if (C_Currency_ID <= 0) set_Value ("C_Currency_ID", null);
 else 
set_Value ("C_Currency_ID", new Integer(C_Currency_ID));
}
/** Get Currency.
The Currency for this record */
public int getC_Currency_ID() 
{
Integer ii = (Integer)get_Value("C_Currency_ID");
if (ii == null) return 0;
return ii.intValue();
}
/** Set Invoice.
Invoice Identifier */
public void setC_Invoice_ID (int C_Invoice_ID)
{
if (C_Invoice_ID <= 0) set_Value ("C_Invoice_ID", null);
 else 
set_Value ("C_Invoice_ID", new Integer(C_Invoice_ID));
}
/** Get Invoice.
Invoice Identifier */
public int getC_Invoice_ID() 
{
Integer ii = (Integer)get_Value("C_Invoice_ID");
if (ii == null) return 0;
return ii.intValue();
}
/** Set Payment.
Payment identifier */
public void setC_Payment_ID (int C_Payment_ID)
{
if (C_Payment_ID <= 0) set_Value ("C_Payment_ID", null);
 else 
set_Value ("C_Payment_ID", new Integer(C_Payment_ID));
}
/** Get Payment.
Payment identifier */
public int getC_Payment_ID() 
{
Integer ii = (Integer)get_Value("C_Payment_ID");
if (ii == null) return 0;
return ii.intValue();
}
/** Set Charge amount.
Charge Amount */
public void setChargeAmt (BigDecimal ChargeAmt)
{
set_Value ("ChargeAmt", ChargeAmt);
}
/** Get Charge amount.
Charge Amount */
public BigDecimal getChargeAmt() 
{
BigDecimal bd = (BigDecimal)get_Value("ChargeAmt");
if (bd == null) return Env.ZERO;
return bd;
}
/** Set Charge Name.
Name of the Charge */
public void setChargeName (String ChargeName)
{
if (ChargeName != null && ChargeName.length() > 60)
{
log.warning("Length > 60 - truncated");
ChargeName = ChargeName.substring(0,59);
}
set_Value ("ChargeName", ChargeName);
}
/** Get Charge Name.
Name of the Charge */
public String getChargeName() 
{
return (String)get_Value("ChargeName");
}
/** Set Create Payment */
public void setCreatePayment (String CreatePayment)
{
if (CreatePayment != null && CreatePayment.length() > 1)
{
log.warning("Length > 1 - truncated");
CreatePayment = CreatePayment.substring(0,0);
}
set_Value ("CreatePayment", CreatePayment);
}
/** Get Create Payment */
public String getCreatePayment() 
{
return (String)get_Value("CreatePayment");
}
/** Set Account Date.
Accounting Date */
public void setDateAcct (Timestamp DateAcct)
{
set_Value ("DateAcct", DateAcct);
}
/** Get Account Date.
Accounting Date */
public Timestamp getDateAcct() 
{
return (Timestamp)get_Value("DateAcct");
}
/** Set Description.
Optional short description of the record */
public void setDescription (String Description)
{
if (Description != null && Description.length() > 255)
{
log.warning("Length > 255 - truncated");
Description = Description.substring(0,254);
}
set_Value ("Description", Description);
}
/** Get Description.
Optional short description of the record */
public String getDescription() 
{
return (String)get_Value("Description");
}
/** Set EFT Amount.
Electronic Funds Transfer Amount */
public void setEftAmt (BigDecimal EftAmt)
{
set_Value ("EftAmt", EftAmt);
}
/** Get EFT Amount.
Electronic Funds Transfer Amount */
public BigDecimal getEftAmt() 
{
BigDecimal bd = (BigDecimal)get_Value("EftAmt");
if (bd == null) return Env.ZERO;
return bd;
}
/** Set EFT Check No.
Electronic Funds Transfer Check No */
public void setEftCheckNo (String EftCheckNo)
{
if (EftCheckNo != null && EftCheckNo.length() > 20)
{
log.warning("Length > 20 - truncated");
EftCheckNo = EftCheckNo.substring(0,19);
}
set_Value ("EftCheckNo", EftCheckNo);
}
/** Get EFT Check No.
Electronic Funds Transfer Check No */
public String getEftCheckNo() 
{
return (String)get_Value("EftCheckNo");
}
/** Set EFT Currency.
Electronic Funds Transfer Currency */
public void setEftCurrency (String EftCurrency)
{
if (EftCurrency != null && EftCurrency.length() > 20)
{
log.warning("Length > 20 - truncated");
EftCurrency = EftCurrency.substring(0,19);
}
set_Value ("EftCurrency", EftCurrency);
}
/** Get EFT Currency.
Electronic Funds Transfer Currency */
public String getEftCurrency() 
{
return (String)get_Value("EftCurrency");
}
/** Set EFT Memo.
Electronic Funds Transfer Memo */
public void setEftMemo (String EftMemo)
{
if (EftMemo != null && EftMemo.length() > 2000)
{
log.warning("Length > 2000 - truncated");
EftMemo = EftMemo.substring(0,1999);
}
set_Value ("EftMemo", EftMemo);
}
/** Get EFT Memo.
Electronic Funds Transfer Memo */
public String getEftMemo() 
{
return (String)get_Value("EftMemo");
}
/** Set EFT Payee.
Electronic Funds Transfer Payee information */
public void setEftPayee (String EftPayee)
{
if (EftPayee != null && EftPayee.length() > 255)
{
log.warning("Length > 255 - truncated");
EftPayee = EftPayee.substring(0,254);
}
set_Value ("EftPayee", EftPayee);
}
/** Get EFT Payee.
Electronic Funds Transfer Payee information */
public String getEftPayee() 
{
return (String)get_Value("EftPayee");
}
/** Set EFT Payee Account.
Electronic Funds Transfer Payyee Account Information */
public void setEftPayeeAccount (String EftPayeeAccount)
{
if (EftPayeeAccount != null && EftPayeeAccount.length() > 40)
{
log.warning("Length > 40 - truncated");
EftPayeeAccount = EftPayeeAccount.substring(0,39);
}
set_Value ("EftPayeeAccount", EftPayeeAccount);
}
/** Get EFT Payee Account.
Electronic Funds Transfer Payyee Account Information */
public String getEftPayeeAccount() 
{
return (String)get_Value("EftPayeeAccount");
}
/** Set EFT Reference.
Electronic Funds Transfer Reference */
public void setEftReference (String EftReference)
{
if (EftReference != null && EftReference.length() > 60)
{
log.warning("Length > 60 - truncated");
EftReference = EftReference.substring(0,59);
}
set_Value ("EftReference", EftReference);
}
/** Get EFT Reference.
Electronic Funds Transfer Reference */
public String getEftReference() 
{
return (String)get_Value("EftReference");
}
/** Set EFT Statement Date.
Electronic Funds Transfer Statement Date */
public void setEftStatementDate (Timestamp EftStatementDate)
{
set_Value ("EftStatementDate", EftStatementDate);
}
/** Get EFT Statement Date.
Electronic Funds Transfer Statement Date */
public Timestamp getEftStatementDate() 
{
return (Timestamp)get_Value("EftStatementDate");
}
/** Set EFT Statement Line Date.
Electronic Funds Transfer Statement Line Date */
public void setEftStatementLineDate (Timestamp EftStatementLineDate)
{
set_Value ("EftStatementLineDate", EftStatementLineDate);
}
/** Get EFT Statement Line Date.
Electronic Funds Transfer Statement Line Date */
public Timestamp getEftStatementLineDate() 
{
return (Timestamp)get_Value("EftStatementLineDate");
}
/** Set EFT Statement Reference.
Electronic Funds Transfer Statement Reference */
public void setEftStatementReference (String EftStatementReference)
{
if (EftStatementReference != null && EftStatementReference.length() > 60)
{
log.warning("Length > 60 - truncated");
EftStatementReference = EftStatementReference.substring(0,59);
}
set_Value ("EftStatementReference", EftStatementReference);
}
/** Get EFT Statement Reference.
Electronic Funds Transfer Statement Reference */
public String getEftStatementReference() 
{
return (String)get_Value("EftStatementReference");
}
/** Set EFT Trx ID.
Electronic Funds Transfer Transaction ID */
public void setEftTrxID (String EftTrxID)
{
if (EftTrxID != null && EftTrxID.length() > 40)
{
log.warning("Length > 40 - truncated");
EftTrxID = EftTrxID.substring(0,39);
}
set_Value ("EftTrxID", EftTrxID);
}
/** Get EFT Trx ID.
Electronic Funds Transfer Transaction ID */
public String getEftTrxID() 
{
return (String)get_Value("EftTrxID");
}
/** Set EFT Trx Type.
Electronic Funds Transfer Transaction Type */
public void setEftTrxType (String EftTrxType)
{
if (EftTrxType != null && EftTrxType.length() > 20)
{
log.warning("Length > 20 - truncated");
EftTrxType = EftTrxType.substring(0,19);
}
set_Value ("EftTrxType", EftTrxType);
}
/** Get EFT Trx Type.
Electronic Funds Transfer Transaction Type */
public String getEftTrxType() 
{
return (String)get_Value("EftTrxType");
}
/** Set EFT Effective Date.
Electronic Funds Transfer Valuta (effective) Date */
public void setEftValutaDate (Timestamp EftValutaDate)
{
set_Value ("EftValutaDate", EftValutaDate);
}
/** Get EFT Effective Date.
Electronic Funds Transfer Valuta (effective) Date */
public Timestamp getEftValutaDate() 
{
return (Timestamp)get_Value("EftValutaDate");
}
/** Set ISO Currency Code.
Three letter ISO 4217 Code of the Currency */
public void setISO_Code (String ISO_Code)
{
if (ISO_Code != null && ISO_Code.length() > 3)
{
log.warning("Length > 3 - truncated");
ISO_Code = ISO_Code.substring(0,2);
}
set_Value ("ISO_Code", ISO_Code);
}
/** Get ISO Currency Code.
Three letter ISO 4217 Code of the Currency */
public String getISO_Code() 
{
return (String)get_Value("ISO_Code");
}
/** Set Import Bank Statement.
Import of the Bank Statement */
public void setI_BankStatement_ID (int I_BankStatement_ID)
{
set_ValueNoCheck ("I_BankStatement_ID", new Integer(I_BankStatement_ID));
}
/** Get Import Bank Statement.
Import of the Bank Statement */
public int getI_BankStatement_ID() 
{
Integer ii = (Integer)get_Value("I_BankStatement_ID");
if (ii == null) return 0;
return ii.intValue();
}
/** Set Import Error Message.
Messages generated from import process */
public void setI_ErrorMsg (String I_ErrorMsg)
{
if (I_ErrorMsg != null && I_ErrorMsg.length() > 2000)
{
log.warning("Length > 2000 - truncated");
I_ErrorMsg = I_ErrorMsg.substring(0,1999);
}
set_Value ("I_ErrorMsg", I_ErrorMsg);
}
/** Get Import Error Message.
Messages generated from import process */
public String getI_ErrorMsg() 
{
return (String)get_Value("I_ErrorMsg");
}
/** Set Imported.
Has this import been processed */
public void setI_IsImported (boolean I_IsImported)
{
set_Value ("I_IsImported", new Boolean(I_IsImported));
}
/** Get Imported.
Has this import been processed */
public boolean isI_IsImported() 
{
Object oo = get_Value("I_IsImported");
if (oo != null) 
{
 if (oo instanceof Boolean) return ((Boolean)oo).booleanValue();
 return "Y".equals(oo);
}
return false;
}
/** Set Interest Amount.
Interest Amount */
public void setInterestAmt (BigDecimal InterestAmt)
{
set_Value ("InterestAmt", InterestAmt);
}
/** Get Interest Amount.
Interest Amount */
public BigDecimal getInterestAmt() 
{
BigDecimal bd = (BigDecimal)get_Value("InterestAmt");
if (bd == null) return Env.ZERO;
return bd;
}
/** Set Invoice Document No.
Document Number of the Invoice */
public void setInvoiceDocumentNo (String InvoiceDocumentNo)
{
if (InvoiceDocumentNo != null && InvoiceDocumentNo.length() > 30)
{
log.warning("Length > 30 - truncated");
InvoiceDocumentNo = InvoiceDocumentNo.substring(0,29);
}
set_Value ("InvoiceDocumentNo", InvoiceDocumentNo);
}
/** Get Invoice Document No.
Document Number of the Invoice */
public String getInvoiceDocumentNo() 
{
return (String)get_Value("InvoiceDocumentNo");
}
/** Set Reversal.
This is a reversing transaction */
public void setIsReversal (boolean IsReversal)
{
set_Value ("IsReversal", new Boolean(IsReversal));
}
/** Get Reversal.
This is a reversing transaction */
public boolean isReversal() 
{
Object oo = get_Value("IsReversal");
if (oo != null) 
{
 if (oo instanceof Boolean) return ((Boolean)oo).booleanValue();
 return "Y".equals(oo);
}
return false;
}
/** Set Line No.
Unique line for this document */
public void setLine (int Line)
{
set_Value ("Line", new Integer(Line));
}
/** Get Line No.
Unique line for this document */
public int getLine() 
{
Integer ii = (Integer)get_Value("Line");
if (ii == null) return 0;
return ii.intValue();
}
/** Set Line Description.
Description of the Line */
public void setLineDescription (String LineDescription)
{
if (LineDescription != null && LineDescription.length() > 255)
{
log.warning("Length > 255 - truncated");
LineDescription = LineDescription.substring(0,254);
}
set_Value ("LineDescription", LineDescription);
}
/** Get Line Description.
Description of the Line */
public String getLineDescription() 
{
return (String)get_Value("LineDescription");
}
/** Set Match Statement */
public void setMatchStatement (String MatchStatement)
{
if (MatchStatement != null && MatchStatement.length() > 1)
{
log.warning("Length > 1 - truncated");
MatchStatement = MatchStatement.substring(0,0);
}
set_Value ("MatchStatement", MatchStatement);
}
/** Get Match Statement */
public String getMatchStatement() 
{
return (String)get_Value("MatchStatement");
}
/** Set Memo.
Memo Text */
public void setMemo (String Memo)
{
if (Memo != null && Memo.length() > 255)
{
log.warning("Length > 255 - truncated");
Memo = Memo.substring(0,254);
}
set_Value ("Memo", Memo);
}
/** Get Memo.
Memo Text */
public String getMemo() 
{
return (String)get_Value("Memo");
}
/** Set Name.
Alphanumeric identifier of the entity */
public void setName (String Name)
{
if (Name != null && Name.length() > 60)
{
log.warning("Length > 60 - truncated");
Name = Name.substring(0,59);
}
set_Value ("Name", Name);
}
/** Get Name.
Alphanumeric identifier of the entity */
public String getName() 
{
return (String)get_Value("Name");
}
/** Set Payment Document No.
Document number of the Payment */
public void setPaymentDocumentNo (String PaymentDocumentNo)
{
if (PaymentDocumentNo != null && PaymentDocumentNo.length() > 30)
{
log.warning("Length > 30 - truncated");
PaymentDocumentNo = PaymentDocumentNo.substring(0,29);
}
set_Value ("PaymentDocumentNo", PaymentDocumentNo);
}
/** Get Payment Document No.
Document number of the Payment */
public String getPaymentDocumentNo() 
{
return (String)get_Value("PaymentDocumentNo");
}
/** Set Processed.
The document has been processed */
public void setProcessed (boolean Processed)
{
set_Value ("Processed", new Boolean(Processed));
}
/** Get Processed.
The document has been processed */
public boolean isProcessed() 
{
Object oo = get_Value("Processed");
if (oo != null) 
{
 if (oo instanceof Boolean) return ((Boolean)oo).booleanValue();
 return "Y".equals(oo);
}
return false;
}
/** Set Process Now */
public void setProcessing (boolean Processing)
{
set_Value ("Processing", new Boolean(Processing));
}
/** Get Process Now */
public boolean isProcessing() 
{
Object oo = get_Value("Processing");
if (oo != null) 
{
 if (oo instanceof Boolean) return ((Boolean)oo).booleanValue();
 return "Y".equals(oo);
}
return false;
}
/** Set Reference No.
Your customer or vendor number at the Business Partner's site */
public void setReferenceNo (String ReferenceNo)
{
if (ReferenceNo != null && ReferenceNo.length() > 40)
{
log.warning("Length > 40 - truncated");
ReferenceNo = ReferenceNo.substring(0,39);
}
set_Value ("ReferenceNo", ReferenceNo);
}
/** Get Reference No.
Your customer or vendor number at the Business Partner's site */
public String getReferenceNo() 
{
return (String)get_Value("ReferenceNo");
}
/** Set Routing No.
Bank Routing Number */
public void setRoutingNo (String RoutingNo)
{
if (RoutingNo != null && RoutingNo.length() > 20)
{
log.warning("Length > 20 - truncated");
RoutingNo = RoutingNo.substring(0,19);
}
set_Value ("RoutingNo", RoutingNo);
}
/** Get Routing No.
Bank Routing Number */
public String getRoutingNo() 
{
return (String)get_Value("RoutingNo");
}
/** Set Statement date.
Date of the statement */
public void setStatementDate (Timestamp StatementDate)
{
set_Value ("StatementDate", StatementDate);
}
/** Get Statement date.
Date of the statement */
public Timestamp getStatementDate() 
{
return (Timestamp)get_Value("StatementDate");
}
/** Set Statement Line Date.
Date of the Statement Line */
public void setStatementLineDate (Timestamp StatementLineDate)
{
set_Value ("StatementLineDate", StatementLineDate);
}
/** Get Statement Line Date.
Date of the Statement Line */
public Timestamp getStatementLineDate() 
{
return (Timestamp)get_Value("StatementLineDate");
}
/** Set Statement amount.
Statement Amount */
public void setStmtAmt (BigDecimal StmtAmt)
{
set_Value ("StmtAmt", StmtAmt);
}
/** Get Statement amount.
Statement Amount */
public BigDecimal getStmtAmt() 
{
BigDecimal bd = (BigDecimal)get_Value("StmtAmt");
if (bd == null) return Env.ZERO;
return bd;
}
/** Set Transaction Amount.
Amount of a transaction */
public void setTrxAmt (BigDecimal TrxAmt)
{
set_Value ("TrxAmt", TrxAmt);
}
/** Get Transaction Amount.
Amount of a transaction */
public BigDecimal getTrxAmt() 
{
BigDecimal bd = (BigDecimal)get_Value("TrxAmt");
if (bd == null) return Env.ZERO;
return bd;
}
public static final int TRXTYPE_AD_Reference_ID=215;
/** Sales = S */
public static final String TRXTYPE_Sales = "S";
/** Delayed Capture = D */
public static final String TRXTYPE_DelayedCapture = "D";
/** Credit (Payment) = C */
public static final String TRXTYPE_CreditPayment = "C";
/** Voice Authorization = F */
public static final String TRXTYPE_VoiceAuthorization = "F";
/** Authorization = A */
public static final String TRXTYPE_Authorization = "A";
/** Void = V */
public static final String TRXTYPE_Void = "V";
/** Set Transaction Type.
Type of credit card transaction */
public void setTrxType (String TrxType)
{
if (TrxType == null || TrxType.equals("S") || TrxType.equals("D") || TrxType.equals("C") || TrxType.equals("F") || TrxType.equals("A") || TrxType.equals("V"));
 else throw new IllegalArgumentException ("TrxType Invalid value - Reference_ID=215 - S - D - C - F - A - V");
if (TrxType != null && TrxType.length() > 20)
{
log.warning("Length > 20 - truncated");
TrxType = TrxType.substring(0,19);
}
set_Value ("TrxType", TrxType);
}
/** Get Transaction Type.
Type of credit card transaction */
public String getTrxType() 
{
return (String)get_Value("TrxType");
}
/** Set Effective date.
Date when money is available */
public void setValutaDate (Timestamp ValutaDate)
{
set_Value ("ValutaDate", ValutaDate);
}
/** Get Effective date.
Date when money is available */
public Timestamp getValutaDate() 
{
return (Timestamp)get_Value("ValutaDate");
}
}

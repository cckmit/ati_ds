/** Modelo Generado - NO CAMBIAR MANUALMENTE - Copyright (C) 2006 FUNDESLE */
package org.openXpertya.model;
import java.util.*;
import java.sql.*;
import java.math.*;
import org.openXpertya.util.*;
/** Modelo Generado por I_GLJournal
 *  @author Comunidad de Desarrollo openXpertya*         *Basado en Codigo Original Modificado, Revisado y Optimizado de:*         * Jorg Janke 
 *  @version  - 2008-01-03 10:26:34.687 */
public class X_I_GLJournal extends PO
{
/** Constructor estándar */
public X_I_GLJournal (Properties ctx, int I_GLJournal_ID, String trxName)
{
super (ctx, I_GLJournal_ID, trxName);
/** if (I_GLJournal_ID == 0)
{
setI_GLJournal_ID (0);
setI_IsImported (false);
}
 */
}
/** Load Constructor */
public X_I_GLJournal (Properties ctx, ResultSet rs, String trxName)
{
super (ctx, rs, trxName);
}
/** AD_Table_ID=599 */
public static final int Table_ID=599;

/** TableName=I_GLJournal */
public static final String Table_Name="I_GLJournal";

protected static KeyNamePair Model = new KeyNamePair(599,"I_GLJournal");
protected static BigDecimal AccessLevel = new BigDecimal(2);

/** Load Meta Data */
protected POInfo initPO (Properties ctx)
{
POInfo poi = POInfo.getPOInfo (ctx, Table_ID);
return poi;
}
public String toString()
{
StringBuffer sb = new StringBuffer ("X_I_GLJournal[").append(getID()).append("]");
return sb.toString();
}
public static final int AD_ORGDOC_ID_AD_Reference_ID=130;
/** Set Document Org.
Document Organization (independent from account organization) */
public void setAD_OrgDoc_ID (int AD_OrgDoc_ID)
{
if (AD_OrgDoc_ID <= 0) set_Value ("AD_OrgDoc_ID", null);
 else 
set_Value ("AD_OrgDoc_ID", new Integer(AD_OrgDoc_ID));
}
/** Get Document Org.
Document Organization (independent from account organization) */
public int getAD_OrgDoc_ID() 
{
Integer ii = (Integer)get_Value("AD_OrgDoc_ID");
if (ii == null) return 0;
return ii.intValue();
}
public static final int AD_ORGTRX_ID_AD_Reference_ID=130;
/** Set Trx Organization.
Performing or initiating organization */
public void setAD_OrgTrx_ID (int AD_OrgTrx_ID)
{
if (AD_OrgTrx_ID <= 0) set_Value ("AD_OrgTrx_ID", null);
 else 
set_Value ("AD_OrgTrx_ID", new Integer(AD_OrgTrx_ID));
}
/** Get Trx Organization.
Performing or initiating organization */
public int getAD_OrgTrx_ID() 
{
Integer ii = (Integer)get_Value("AD_OrgTrx_ID");
if (ii == null) return 0;
return ii.intValue();
}
/** Set Account Key.
Key of Account Element */
public void setAccountValue (String AccountValue)
{
if (AccountValue != null && AccountValue.length() > 40)
{
log.warning("Length > 40 - truncated");
AccountValue = AccountValue.substring(0,39);
}
set_Value ("AccountValue", AccountValue);
}
/** Get Account Key.
Key of Account Element */
public String getAccountValue() 
{
return (String)get_Value("AccountValue");
}
public static final int ACCOUNT_ID_AD_Reference_ID=132;
/** Set Account.
Account used */
public void setAccount_ID (int Account_ID)
{
if (Account_ID <= 0) set_Value ("Account_ID", null);
 else 
set_Value ("Account_ID", new Integer(Account_ID));
}
/** Get Account.
Account used */
public int getAccount_ID() 
{
Integer ii = (Integer)get_Value("Account_ID");
if (ii == null) return 0;
return ii.intValue();
}
/** Set Account Schema Name.
Name of the Accounting Schema */
public void setAcctSchemaName (String AcctSchemaName)
{
if (AcctSchemaName != null && AcctSchemaName.length() > 60)
{
log.warning("Length > 60 - truncated");
AcctSchemaName = AcctSchemaName.substring(0,59);
}
set_Value ("AcctSchemaName", AcctSchemaName);
}
/** Get Account Schema Name.
Name of the Accounting Schema */
public String getAcctSchemaName() 
{
return (String)get_Value("AcctSchemaName");
}
/** Set Accounted Credit.
Accounted Credit Amount */
public void setAmtAcctCr (BigDecimal AmtAcctCr)
{
set_Value ("AmtAcctCr", AmtAcctCr);
}
/** Get Accounted Credit.
Accounted Credit Amount */
public BigDecimal getAmtAcctCr() 
{
BigDecimal bd = (BigDecimal)get_Value("AmtAcctCr");
if (bd == null) return Env.ZERO;
return bd;
}
/** Set Accounted Debit.
Accounted Debit Amount */
public void setAmtAcctDr (BigDecimal AmtAcctDr)
{
set_Value ("AmtAcctDr", AmtAcctDr);
}
/** Get Accounted Debit.
Accounted Debit Amount */
public BigDecimal getAmtAcctDr() 
{
BigDecimal bd = (BigDecimal)get_Value("AmtAcctDr");
if (bd == null) return Env.ZERO;
return bd;
}
/** Set Source Credit.
Source Credit Amount */
public void setAmtSourceCr (BigDecimal AmtSourceCr)
{
set_Value ("AmtSourceCr", AmtSourceCr);
}
/** Get Source Credit.
Source Credit Amount */
public BigDecimal getAmtSourceCr() 
{
BigDecimal bd = (BigDecimal)get_Value("AmtSourceCr");
if (bd == null) return Env.ZERO;
return bd;
}
/** Set Source Debit.
Source Debit Amount */
public void setAmtSourceDr (BigDecimal AmtSourceDr)
{
set_Value ("AmtSourceDr", AmtSourceDr);
}
/** Get Source Debit.
Source Debit Amount */
public BigDecimal getAmtSourceDr() 
{
BigDecimal bd = (BigDecimal)get_Value("AmtSourceDr");
if (bd == null) return Env.ZERO;
return bd;
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
/** Set Batch Description.
Description of the Batch */
public void setBatchDescription (String BatchDescription)
{
if (BatchDescription != null && BatchDescription.length() > 255)
{
log.warning("Length > 255 - truncated");
BatchDescription = BatchDescription.substring(0,254);
}
set_Value ("BatchDescription", BatchDescription);
}
/** Get Batch Description.
Description of the Batch */
public String getBatchDescription() 
{
return (String)get_Value("BatchDescription");
}
/** Set Batch Document No.
Document Number of the Batch */
public void setBatchDocumentNo (String BatchDocumentNo)
{
if (BatchDocumentNo != null && BatchDocumentNo.length() > 30)
{
log.warning("Length > 30 - truncated");
BatchDocumentNo = BatchDocumentNo.substring(0,29);
}
set_Value ("BatchDocumentNo", BatchDocumentNo);
}
/** Get Batch Document No.
Document Number of the Batch */
public String getBatchDocumentNo() 
{
return (String)get_Value("BatchDocumentNo");
}
/** Set Accounting Schema.
Rules for accounting */
public void setC_AcctSchema_ID (int C_AcctSchema_ID)
{
if (C_AcctSchema_ID <= 0) set_Value ("C_AcctSchema_ID", null);
 else 
set_Value ("C_AcctSchema_ID", new Integer(C_AcctSchema_ID));
}
/** Get Accounting Schema.
Rules for accounting */
public int getC_AcctSchema_ID() 
{
Integer ii = (Integer)get_Value("C_AcctSchema_ID");
if (ii == null) return 0;
return ii.intValue();
}
/** Set Activity.
Business Activity */
public void setC_Activity_ID (int C_Activity_ID)
{
if (C_Activity_ID <= 0) set_Value ("C_Activity_ID", null);
 else 
set_Value ("C_Activity_ID", new Integer(C_Activity_ID));
}
/** Get Activity.
Business Activity */
public int getC_Activity_ID() 
{
Integer ii = (Integer)get_Value("C_Activity_ID");
if (ii == null) return 0;
return ii.intValue();
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
/** Set Campaign.
Marketing Campaign */
public void setC_Campaign_ID (int C_Campaign_ID)
{
if (C_Campaign_ID <= 0) set_Value ("C_Campaign_ID", null);
 else 
set_Value ("C_Campaign_ID", new Integer(C_Campaign_ID));
}
/** Get Campaign.
Marketing Campaign */
public int getC_Campaign_ID() 
{
Integer ii = (Integer)get_Value("C_Campaign_ID");
if (ii == null) return 0;
return ii.intValue();
}
/** Set Currency Type.
Currency Conversion Rate Type */
public void setC_ConversionType_ID (int C_ConversionType_ID)
{
if (C_ConversionType_ID <= 0) set_Value ("C_ConversionType_ID", null);
 else 
set_Value ("C_ConversionType_ID", new Integer(C_ConversionType_ID));
}
/** Get Currency Type.
Currency Conversion Rate Type */
public int getC_ConversionType_ID() 
{
Integer ii = (Integer)get_Value("C_ConversionType_ID");
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
/** Set Document Type.
Document type or rules */
public void setC_DocType_ID (int C_DocType_ID)
{
if (C_DocType_ID <= 0) set_Value ("C_DocType_ID", null);
 else 
set_Value ("C_DocType_ID", new Integer(C_DocType_ID));
}
/** Get Document Type.
Document type or rules */
public int getC_DocType_ID() 
{
Integer ii = (Integer)get_Value("C_DocType_ID");
if (ii == null) return 0;
return ii.intValue();
}
public static final int C_LOCFROM_ID_AD_Reference_ID=133;
/** Set Location From.
Location that inventory was moved from */
public void setC_LocFrom_ID (int C_LocFrom_ID)
{
if (C_LocFrom_ID <= 0) set_Value ("C_LocFrom_ID", null);
 else 
set_Value ("C_LocFrom_ID", new Integer(C_LocFrom_ID));
}
/** Get Location From.
Location that inventory was moved from */
public int getC_LocFrom_ID() 
{
Integer ii = (Integer)get_Value("C_LocFrom_ID");
if (ii == null) return 0;
return ii.intValue();
}
public static final int C_LOCTO_ID_AD_Reference_ID=133;
/** Set Location To.
Location that inventory was moved to */
public void setC_LocTo_ID (int C_LocTo_ID)
{
if (C_LocTo_ID <= 0) set_Value ("C_LocTo_ID", null);
 else 
set_Value ("C_LocTo_ID", new Integer(C_LocTo_ID));
}
/** Get Location To.
Location that inventory was moved to */
public int getC_LocTo_ID() 
{
Integer ii = (Integer)get_Value("C_LocTo_ID");
if (ii == null) return 0;
return ii.intValue();
}
/** Set Period.
Period of the Calendar */
public void setC_Period_ID (int C_Period_ID)
{
if (C_Period_ID <= 0) set_Value ("C_Period_ID", null);
 else 
set_Value ("C_Period_ID", new Integer(C_Period_ID));
}
/** Get Period.
Period of the Calendar */
public int getC_Period_ID() 
{
Integer ii = (Integer)get_Value("C_Period_ID");
if (ii == null) return 0;
return ii.intValue();
}
/** Set Project.
Financial Project */
public void setC_Project_ID (int C_Project_ID)
{
if (C_Project_ID <= 0) set_Value ("C_Project_ID", null);
 else 
set_Value ("C_Project_ID", new Integer(C_Project_ID));
}
/** Get Project.
Financial Project */
public int getC_Project_ID() 
{
Integer ii = (Integer)get_Value("C_Project_ID");
if (ii == null) return 0;
return ii.intValue();
}
/** Set Sales Region.
Sales coverage region */
public void setC_SalesRegion_ID (int C_SalesRegion_ID)
{
if (C_SalesRegion_ID <= 0) set_Value ("C_SalesRegion_ID", null);
 else 
set_Value ("C_SalesRegion_ID", new Integer(C_SalesRegion_ID));
}
/** Get Sales Region.
Sales coverage region */
public int getC_SalesRegion_ID() 
{
Integer ii = (Integer)get_Value("C_SalesRegion_ID");
if (ii == null) return 0;
return ii.intValue();
}
/** Set UOM.
Unit of Measure */
public void setC_UOM_ID (int C_UOM_ID)
{
if (C_UOM_ID <= 0) set_Value ("C_UOM_ID", null);
 else 
set_Value ("C_UOM_ID", new Integer(C_UOM_ID));
}
/** Get UOM.
Unit of Measure */
public int getC_UOM_ID() 
{
Integer ii = (Integer)get_Value("C_UOM_ID");
if (ii == null) return 0;
return ii.intValue();
}
/** Set Combination.
Valid Account Combination */
public void setC_ValidCombination_ID (int C_ValidCombination_ID)
{
if (C_ValidCombination_ID <= 0) set_Value ("C_ValidCombination_ID", null);
 else 
set_Value ("C_ValidCombination_ID", new Integer(C_ValidCombination_ID));
}
/** Get Combination.
Valid Account Combination */
public int getC_ValidCombination_ID() 
{
Integer ii = (Integer)get_Value("C_ValidCombination_ID");
if (ii == null) return 0;
return ii.intValue();
}
/** Set Category Name.
Name of the Category */
public void setCategoryName (String CategoryName)
{
if (CategoryName != null && CategoryName.length() > 60)
{
log.warning("Length > 60 - truncated");
CategoryName = CategoryName.substring(0,59);
}
set_Value ("CategoryName", CategoryName);
}
/** Get Category Name.
Name of the Category */
public String getCategoryName() 
{
return (String)get_Value("CategoryName");
}
/** Set Client Key.
Key of the Client */
public void setClientValue (String ClientValue)
{
if (ClientValue != null && ClientValue.length() > 40)
{
log.warning("Length > 40 - truncated");
ClientValue = ClientValue.substring(0,39);
}
set_Value ("ClientValue", ClientValue);
}
/** Get Client Key.
Key of the Client */
public String getClientValue() 
{
return (String)get_Value("ClientValue");
}
/** Set Currency Type Key.
Key value for the Currency Conversion Rate Type */
public void setConversionTypeValue (String ConversionTypeValue)
{
if (ConversionTypeValue != null && ConversionTypeValue.length() > 40)
{
log.warning("Length > 40 - truncated");
ConversionTypeValue = ConversionTypeValue.substring(0,39);
}
set_Value ("ConversionTypeValue", ConversionTypeValue);
}
/** Get Currency Type Key.
Key value for the Currency Conversion Rate Type */
public String getConversionTypeValue() 
{
return (String)get_Value("ConversionTypeValue");
}
/** Set Rate.
Currency Conversion Rate */
public void setCurrencyRate (BigDecimal CurrencyRate)
{
set_Value ("CurrencyRate", CurrencyRate);
}
/** Get Rate.
Currency Conversion Rate */
public BigDecimal getCurrencyRate() 
{
BigDecimal bd = (BigDecimal)get_Value("CurrencyRate");
if (bd == null) return Env.ZERO;
return bd;
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
/** Set Document Type Name.
Name of the Document Type */
public void setDocTypeName (String DocTypeName)
{
if (DocTypeName != null && DocTypeName.length() > 60)
{
log.warning("Length > 60 - truncated");
DocTypeName = DocTypeName.substring(0,59);
}
set_Value ("DocTypeName", DocTypeName);
}
/** Get Document Type Name.
Name of the Document Type */
public String getDocTypeName() 
{
return (String)get_Value("DocTypeName");
}
/** Set Budget.
General Ledger Budget */
public void setGL_Budget_ID (int GL_Budget_ID)
{
if (GL_Budget_ID <= 0) set_Value ("GL_Budget_ID", null);
 else 
set_Value ("GL_Budget_ID", new Integer(GL_Budget_ID));
}
/** Get Budget.
General Ledger Budget */
public int getGL_Budget_ID() 
{
Integer ii = (Integer)get_Value("GL_Budget_ID");
if (ii == null) return 0;
return ii.intValue();
}
/** Set GL Category.
General Ledger Category */
public void setGL_Category_ID (int GL_Category_ID)
{
if (GL_Category_ID <= 0) set_Value ("GL_Category_ID", null);
 else 
set_Value ("GL_Category_ID", new Integer(GL_Category_ID));
}
/** Get GL Category.
General Ledger Category */
public int getGL_Category_ID() 
{
Integer ii = (Integer)get_Value("GL_Category_ID");
if (ii == null) return 0;
return ii.intValue();
}
/** Set Journal Batch.
General Ledger Journal Batch */
public void setGL_JournalBatch_ID (int GL_JournalBatch_ID)
{
if (GL_JournalBatch_ID <= 0) set_Value ("GL_JournalBatch_ID", null);
 else 
set_Value ("GL_JournalBatch_ID", new Integer(GL_JournalBatch_ID));
}
/** Get Journal Batch.
General Ledger Journal Batch */
public int getGL_JournalBatch_ID() 
{
Integer ii = (Integer)get_Value("GL_JournalBatch_ID");
if (ii == null) return 0;
return ii.intValue();
}
/** Set Journal Line.
General Ledger Journal Line */
public void setGL_JournalLine_ID (int GL_JournalLine_ID)
{
if (GL_JournalLine_ID <= 0) set_Value ("GL_JournalLine_ID", null);
 else 
set_Value ("GL_JournalLine_ID", new Integer(GL_JournalLine_ID));
}
/** Get Journal Line.
General Ledger Journal Line */
public int getGL_JournalLine_ID() 
{
Integer ii = (Integer)get_Value("GL_JournalLine_ID");
if (ii == null) return 0;
return ii.intValue();
}
/** Set Journal.
General Ledger Journal */
public void setGL_Journal_ID (int GL_Journal_ID)
{
if (GL_Journal_ID <= 0) set_Value ("GL_Journal_ID", null);
 else 
set_Value ("GL_Journal_ID", new Integer(GL_Journal_ID));
}
/** Get Journal.
General Ledger Journal */
public int getGL_Journal_ID() 
{
Integer ii = (Integer)get_Value("GL_Journal_ID");
if (ii == null) return 0;
return ii.intValue();
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
/** Set Import GL Journal.
Import General Ledger Journal */
public void setI_GLJournal_ID (int I_GLJournal_ID)
{
set_ValueNoCheck ("I_GLJournal_ID", new Integer(I_GLJournal_ID));
}
/** Get Import GL Journal.
Import General Ledger Journal */
public int getI_GLJournal_ID() 
{
Integer ii = (Integer)get_Value("I_GLJournal_ID");
if (ii == null) return 0;
return ii.intValue();
}
public KeyNamePair getKeyNamePair() 
{
return new KeyNamePair(getID(), String.valueOf(getI_GLJournal_ID()));
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
/** Set Journal Document No.
Document number of the Journal */
public void setJournalDocumentNo (String JournalDocumentNo)
{
if (JournalDocumentNo != null && JournalDocumentNo.length() > 30)
{
log.warning("Length > 30 - truncated");
JournalDocumentNo = JournalDocumentNo.substring(0,29);
}
set_Value ("JournalDocumentNo", JournalDocumentNo);
}
/** Get Journal Document No.
Document number of the Journal */
public String getJournalDocumentNo() 
{
return (String)get_Value("JournalDocumentNo");
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
/** Set Product.
Product, Service, Item */
public void setM_Product_ID (int M_Product_ID)
{
if (M_Product_ID <= 0) set_Value ("M_Product_ID", null);
 else 
set_Value ("M_Product_ID", new Integer(M_Product_ID));
}
/** Get Product.
Product, Service, Item */
public int getM_Product_ID() 
{
Integer ii = (Integer)get_Value("M_Product_ID");
if (ii == null) return 0;
return ii.intValue();
}
/** Set Trx Org Key.
Key of the Transaction Organization */
public void setOrgTrxValue (String OrgTrxValue)
{
if (OrgTrxValue != null && OrgTrxValue.length() > 40)
{
log.warning("Length > 40 - truncated");
OrgTrxValue = OrgTrxValue.substring(0,39);
}
set_Value ("OrgTrxValue", OrgTrxValue);
}
/** Get Trx Org Key.
Key of the Transaction Organization */
public String getOrgTrxValue() 
{
return (String)get_Value("OrgTrxValue");
}
/** Set Org Key.
Key of the Organization */
public void setOrgValue (String OrgValue)
{
if (OrgValue != null && OrgValue.length() > 40)
{
log.warning("Length > 40 - truncated");
OrgValue = OrgValue.substring(0,39);
}
set_Value ("OrgValue", OrgValue);
}
/** Get Org Key.
Key of the Organization */
public String getOrgValue() 
{
return (String)get_Value("OrgValue");
}
public static final int POSTINGTYPE_AD_Reference_ID=125;
/** Actual = A */
public static final String POSTINGTYPE_Actual = "A";
/** Presupuestaria = B */
public static final String POSTINGTYPE_Presupuestaria = "B";
/** Pendientes = E */
public static final String POSTINGTYPE_Pendientes = "E";
/** Estadisticos = S */
public static final String POSTINGTYPE_Estadisticos = "S";
/** Set PostingType.
The type of amount that this journal updated */
public void setPostingType (String PostingType)
{
if (PostingType == null || PostingType.equals("A") || PostingType.equals("B") || PostingType.equals("E") || PostingType.equals("S"));
 else throw new IllegalArgumentException ("PostingType Invalid value - Reference_ID=125 - A - B - E - S");
if (PostingType != null && PostingType.length() > 1)
{
log.warning("Length > 1 - truncated");
PostingType = PostingType.substring(0,0);
}
set_Value ("PostingType", PostingType);
}
/** Get PostingType.
The type of amount that this journal updated */
public String getPostingType() 
{
return (String)get_Value("PostingType");
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
/** Set Product Key.
Key of the Product */
public void setProductValue (String ProductValue)
{
if (ProductValue != null && ProductValue.length() > 40)
{
log.warning("Length > 40 - truncated");
ProductValue = ProductValue.substring(0,39);
}
set_Value ("ProductValue", ProductValue);
}
/** Get Product Key.
Key of the Product */
public String getProductValue() 
{
return (String)get_Value("ProductValue");
}
/** Set Project Key.
Key of the Project */
public void setProjectValue (String ProjectValue)
{
if (ProjectValue != null && ProjectValue.length() > 40)
{
log.warning("Length > 40 - truncated");
ProjectValue = ProjectValue.substring(0,39);
}
set_Value ("ProjectValue", ProjectValue);
}
/** Get Project Key.
Key of the Project */
public String getProjectValue() 
{
return (String)get_Value("ProjectValue");
}
/** Set Quantity.
Quantity */
public void setQty (BigDecimal Qty)
{
set_Value ("Qty", Qty);
}
/** Get Quantity.
Quantity */
public BigDecimal getQty() 
{
BigDecimal bd = (BigDecimal)get_Value("Qty");
if (bd == null) return Env.ZERO;
return bd;
}
/** Set SKU.
Stock Keeping Unit */
public void setSKU (String SKU)
{
if (SKU != null && SKU.length() > 30)
{
log.warning("Length > 30 - truncated");
SKU = SKU.substring(0,29);
}
set_Value ("SKU", SKU);
}
/** Get SKU.
Stock Keeping Unit */
public String getSKU() 
{
return (String)get_Value("SKU");
}
/** Set UPC/EAN.
Bar Code (Universal Product Code or its superset European Article Number) */
public void setUPC (String UPC)
{
if (UPC != null && UPC.length() > 30)
{
log.warning("Length > 30 - truncated");
UPC = UPC.substring(0,29);
}
set_Value ("UPC", UPC);
}
/** Get UPC/EAN.
Bar Code (Universal Product Code or its superset European Article Number) */
public String getUPC() 
{
return (String)get_Value("UPC");
}
public static final int USER1_ID_AD_Reference_ID=134;
/** Set User1.
User defined element #1 */
public void setUser1_ID (int User1_ID)
{
if (User1_ID <= 0) set_Value ("User1_ID", null);
 else 
set_Value ("User1_ID", new Integer(User1_ID));
}
/** Get User1.
User defined element #1 */
public int getUser1_ID() 
{
Integer ii = (Integer)get_Value("User1_ID");
if (ii == null) return 0;
return ii.intValue();
}
public static final int USER2_ID_AD_Reference_ID=137;
/** Set User2.
User defined element #2 */
public void setUser2_ID (int User2_ID)
{
if (User2_ID <= 0) set_Value ("User2_ID", null);
 else 
set_Value ("User2_ID", new Integer(User2_ID));
}
/** Get User2.
User defined element #2 */
public int getUser2_ID() 
{
Integer ii = (Integer)get_Value("User2_ID");
if (ii == null) return 0;
return ii.intValue();
}
}

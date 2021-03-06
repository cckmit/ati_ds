/** Modelo Generado - NO CAMBIAR MANUALMENTE - Disytel */
package org.openXpertya.model;
import java.util.*;
import java.sql.*;
import java.math.*;
import org.openXpertya.util.*;
/** Modelo Generado por C_POS
 *  @author Comunidad de Desarrollo Libertya*         *Basado en Codigo Original Modificado, Revisado y Optimizado de:*         * Jorg Janke 
 *  @version  - 2010-10-21 11:31:42.83 */
public class X_C_POS extends org.openXpertya.model.PO
{
/** Constructor estándar */
public X_C_POS (Properties ctx, int C_POS_ID, String trxName)
{
super (ctx, C_POS_ID, trxName);
/** if (C_POS_ID == 0)
{
setAD_Role_ID (0);
setC_BankAccount_ID (0);
setC_BPartnerCashTrx_ID (0);
setC_CashBook_ID (0);
setC_InvoiceDocType_ID (0);
setC_OrderDocType_ID (0);
setC_POS_ID (0);
setIsCreateInvoice (false);
setIsDeliverOrderInWarehouse (false);	// N
setIsModifyPrice (false);	// N
setIsPrintWarehouseDeliverDocument (false);	// N
setIsSaleWithoutStock (false);
setIsSearchByName (false);
setIsSearchByNameLike (false);
setIsSearchByUPC (false);
setIsSearchByUPCLike (false);
setIsSearchByValue (true);	// Y
setIsSearchByValueLike (false);
setM_PriceList_ID (0);
setM_Warehouse_ID (0);
setName (null);
setOperationMode (null);	// S
setPOSNumber (0);
setSalesRep_ID (0);
}
 */
}
/** Load Constructor */
public X_C_POS (Properties ctx, ResultSet rs, String trxName)
{
super (ctx, rs, trxName);
}
/** AD_Table_ID */
public static final int Table_ID = M_Table.getTableID("C_POS");

/** TableName=C_POS */
public static final String Table_Name="C_POS";

protected static KeyNamePair Model = new KeyNamePair(Table_ID,"C_POS");
protected static BigDecimal AccessLevel = new BigDecimal(2);

/** Load Meta Data */
protected POInfo initPO (Properties ctx)
{
POInfo poi = POInfo.getPOInfo (ctx, Table_ID);
return poi;
}
public String toString()
{
StringBuffer sb = new StringBuffer ("X_C_POS[").append(getID()).append("]");
return sb.toString();
}
/** Set Role.
Responsibility Role */
public void setAD_Role_ID (int AD_Role_ID)
{
set_Value ("AD_Role_ID", new Integer(AD_Role_ID));
}
/** Get Role.
Responsibility Role */
public int getAD_Role_ID() 
{
Integer ii = (Integer)get_Value("AD_Role_ID");
if (ii == null) return 0;
return ii.intValue();
}
/** Set Bank Account.
Account at the Bank */
public void setC_BankAccount_ID (int C_BankAccount_ID)
{
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
public static final int C_BPARTNERCASHTRX_ID_AD_Reference_ID = MReference.getReferenceID("C_BPartner Customers");
/** Set Template B.Partner.
Business Partner used for creating new Business Partners on the fly */
public void setC_BPartnerCashTrx_ID (int C_BPartnerCashTrx_ID)
{
set_Value ("C_BPartnerCashTrx_ID", new Integer(C_BPartnerCashTrx_ID));
}
/** Get Template B.Partner.
Business Partner used for creating new Business Partners on the fly */
public int getC_BPartnerCashTrx_ID() 
{
Integer ii = (Integer)get_Value("C_BPartnerCashTrx_ID");
if (ii == null) return 0;
return ii.intValue();
}
/** Set Cash Book.
Cash Book for recording petty cash transactions */
public void setC_CashBook_ID (int C_CashBook_ID)
{
set_Value ("C_CashBook_ID", new Integer(C_CashBook_ID));
}
/** Get Cash Book.
Cash Book for recording petty cash transactions */
public int getC_CashBook_ID() 
{
Integer ii = (Integer)get_Value("C_CashBook_ID");
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
public static final int C_INOUTDOCTYPE_ID_AD_Reference_ID = MReference.getReferenceID("C_DocType");
/** Set InOut Document Type */
public void setC_InoutDocType_ID (int C_InoutDocType_ID)
{
if (C_InoutDocType_ID <= 0) set_Value ("C_InoutDocType_ID", null);
 else 
set_Value ("C_InoutDocType_ID", new Integer(C_InoutDocType_ID));
}
/** Get InOut Document Type */
public int getC_InoutDocType_ID() 
{
Integer ii = (Integer)get_Value("C_InoutDocType_ID");
if (ii == null) return 0;
return ii.intValue();
}
public static final int C_INVOICEDOCTYPE_ID_AD_Reference_ID = MReference.getReferenceID("C_DocType");
/** Set Invoice Document Type */
public void setC_InvoiceDocType_ID (int C_InvoiceDocType_ID)
{
set_Value ("C_InvoiceDocType_ID", new Integer(C_InvoiceDocType_ID));
}
/** Get Invoice Document Type */
public int getC_InvoiceDocType_ID() 
{
Integer ii = (Integer)get_Value("C_InvoiceDocType_ID");
if (ii == null) return 0;
return ii.intValue();
}
public static final int C_ORDERDOCTYPE_ID_AD_Reference_ID = MReference.getReferenceID("C_DocType");
/** Set Order Document Type */
public void setC_OrderDocType_ID (int C_OrderDocType_ID)
{
set_Value ("C_OrderDocType_ID", new Integer(C_OrderDocType_ID));
}
/** Get Order Document Type */
public int getC_OrderDocType_ID() 
{
Integer ii = (Integer)get_Value("C_OrderDocType_ID");
if (ii == null) return 0;
return ii.intValue();
}
/** Set POS Terminal.
Point of Sales Terminal */
public void setC_POS_ID (int C_POS_ID)
{
set_ValueNoCheck ("C_POS_ID", new Integer(C_POS_ID));
}
/** Get POS Terminal.
Point of Sales Terminal */
public int getC_POS_ID() 
{
Integer ii = (Integer)get_Value("C_POS_ID");
if (ii == null) return 0;
return ii.intValue();
}
/** Set POS Key Layout.
POS Function Key Layout */
public void setC_POSKeyLayout_ID (int C_POSKeyLayout_ID)
{
if (C_POSKeyLayout_ID <= 0) set_Value ("C_POSKeyLayout_ID", null);
 else 
set_Value ("C_POSKeyLayout_ID", new Integer(C_POSKeyLayout_ID));
}
/** Get POS Key Layout.
POS Function Key Layout */
public int getC_POSKeyLayout_ID() 
{
Integer ii = (Integer)get_Value("C_POSKeyLayout_ID");
if (ii == null) return 0;
return ii.intValue();
}
/** Set Description.
Optional short description of the record */
public void setDescription (String Description)
{
if (Description != null && Description.length() > 255)
{
log.warning("Length > 255 - truncated");
Description = Description.substring(0,255);
}
set_Value ("Description", Description);
}
/** Get Description.
Optional short description of the record */
public String getDescription() 
{
return (String)get_Value("Description");
}
/** Set Comment/Help.
Comment or Hint */
public void setHelp (String Help)
{
if (Help != null && Help.length() > 2000)
{
log.warning("Length > 2000 - truncated");
Help = Help.substring(0,2000);
}
set_Value ("Help", Help);
}
/** Get Comment/Help.
Comment or Hint */
public String getHelp() 
{
return (String)get_Value("Help");
}
/** Set Create Invoice */
public void setIsCreateInvoice (boolean IsCreateInvoice)
{
set_Value ("IsCreateInvoice", new Boolean(IsCreateInvoice));
}
/** Get Create Invoice */
public boolean isCreateInvoice() 
{
Object oo = get_Value("IsCreateInvoice");
if (oo != null) 
{
 if (oo instanceof Boolean) return ((Boolean)oo).booleanValue();
 return "Y".equals(oo);
}
return false;
}
/** Set Deliver Added Orders In Warehouse.
Deliver Added Orders In Warehouse */
public void setIsDeliverOrderInWarehouse (boolean IsDeliverOrderInWarehouse)
{
set_Value ("IsDeliverOrderInWarehouse", new Boolean(IsDeliverOrderInWarehouse));
}
/** Get Deliver Added Orders In Warehouse.
Deliver Added Orders In Warehouse */
public boolean isDeliverOrderInWarehouse() 
{
Object oo = get_Value("IsDeliverOrderInWarehouse");
if (oo != null) 
{
 if (oo instanceof Boolean) return ((Boolean)oo).booleanValue();
 return "Y".equals(oo);
}
return false;
}
/** Set Modify Price.
Allow modifying the price */
public void setIsModifyPrice (boolean IsModifyPrice)
{
set_Value ("IsModifyPrice", new Boolean(IsModifyPrice));
}
/** Get Modify Price.
Allow modifying the price */
public boolean isModifyPrice() 
{
Object oo = get_Value("IsModifyPrice");
if (oo != null) 
{
 if (oo instanceof Boolean) return ((Boolean)oo).booleanValue();
 return "Y".equals(oo);
}
return false;
}
/** Set Print Warehouse Deliver Document.
Print Warehouse Deliver Document */
public void setIsPrintWarehouseDeliverDocument (boolean IsPrintWarehouseDeliverDocument)
{
set_Value ("IsPrintWarehouseDeliverDocument", new Boolean(IsPrintWarehouseDeliverDocument));
}
/** Get Print Warehouse Deliver Document.
Print Warehouse Deliver Document */
public boolean isPrintWarehouseDeliverDocument() 
{
Object oo = get_Value("IsPrintWarehouseDeliverDocument");
if (oo != null) 
{
 if (oo instanceof Boolean) return ((Boolean)oo).booleanValue();
 return "Y".equals(oo);
}
return false;
}
/** Set Sale Without Stock */
public void setIsSaleWithoutStock (boolean IsSaleWithoutStock)
{
set_Value ("IsSaleWithoutStock", new Boolean(IsSaleWithoutStock));
}
/** Get Sale Without Stock */
public boolean isSaleWithoutStock() 
{
Object oo = get_Value("IsSaleWithoutStock");
if (oo != null) 
{
 if (oo instanceof Boolean) return ((Boolean)oo).booleanValue();
 return "Y".equals(oo);
}
return false;
}
/** Set Search by Name.
Search by Name */
public void setIsSearchByName (boolean IsSearchByName)
{
set_Value ("IsSearchByName", new Boolean(IsSearchByName));
}
/** Get Search by Name.
Search by Name */
public boolean isSearchByName() 
{
Object oo = get_Value("IsSearchByName");
if (oo != null) 
{
 if (oo instanceof Boolean) return ((Boolean)oo).booleanValue();
 return "Y".equals(oo);
}
return false;
}
/** Set Search by Name (Partial).
Search by Name (Partial) */
public void setIsSearchByNameLike (boolean IsSearchByNameLike)
{
set_Value ("IsSearchByNameLike", new Boolean(IsSearchByNameLike));
}
/** Get Search by Name (Partial).
Search by Name (Partial) */
public boolean isSearchByNameLike() 
{
Object oo = get_Value("IsSearchByNameLike");
if (oo != null) 
{
 if (oo instanceof Boolean) return ((Boolean)oo).booleanValue();
 return "Y".equals(oo);
}
return false;
}
/** Set Search by UPC.
Search by UPC */
public void setIsSearchByUPC (boolean IsSearchByUPC)
{
set_Value ("IsSearchByUPC", new Boolean(IsSearchByUPC));
}
/** Get Search by UPC.
Search by UPC */
public boolean isSearchByUPC() 
{
Object oo = get_Value("IsSearchByUPC");
if (oo != null) 
{
 if (oo instanceof Boolean) return ((Boolean)oo).booleanValue();
 return "Y".equals(oo);
}
return false;
}
/** Set Search by UPC (Partial).
Search by UPC (Partial) */
public void setIsSearchByUPCLike (boolean IsSearchByUPCLike)
{
set_Value ("IsSearchByUPCLike", new Boolean(IsSearchByUPCLike));
}
/** Get Search by UPC (Partial).
Search by UPC (Partial) */
public boolean isSearchByUPCLike() 
{
Object oo = get_Value("IsSearchByUPCLike");
if (oo != null) 
{
 if (oo instanceof Boolean) return ((Boolean)oo).booleanValue();
 return "Y".equals(oo);
}
return false;
}
/** Set Search by Value.
Search by Value */
public void setIsSearchByValue (boolean IsSearchByValue)
{
set_Value ("IsSearchByValue", new Boolean(IsSearchByValue));
}
/** Get Search by Value.
Search by Value */
public boolean isSearchByValue() 
{
Object oo = get_Value("IsSearchByValue");
if (oo != null) 
{
 if (oo instanceof Boolean) return ((Boolean)oo).booleanValue();
 return "Y".equals(oo);
}
return false;
}
/** Set Search by Value (Partial).
Search by Value (Partial) */
public void setIsSearchByValueLike (boolean IsSearchByValueLike)
{
set_Value ("IsSearchByValueLike", new Boolean(IsSearchByValueLike));
}
/** Get Search by Value (Partial).
Search by Value (Partial) */
public boolean isSearchByValueLike() 
{
Object oo = get_Value("IsSearchByValueLike");
if (oo != null) 
{
 if (oo instanceof Boolean) return ((Boolean)oo).booleanValue();
 return "Y".equals(oo);
}
return false;
}
/** Set Price List.
Unique identifier of a Price List */
public void setM_PriceList_ID (int M_PriceList_ID)
{
set_Value ("M_PriceList_ID", new Integer(M_PriceList_ID));
}
/** Get Price List.
Unique identifier of a Price List */
public int getM_PriceList_ID() 
{
Integer ii = (Integer)get_Value("M_PriceList_ID");
if (ii == null) return 0;
return ii.intValue();
}
/** Set Warehouse.
Storage Warehouse and Service Point */
public void setM_Warehouse_ID (int M_Warehouse_ID)
{
set_Value ("M_Warehouse_ID", new Integer(M_Warehouse_ID));
}
/** Get Warehouse.
Storage Warehouse and Service Point */
public int getM_Warehouse_ID() 
{
Integer ii = (Integer)get_Value("M_Warehouse_ID");
if (ii == null) return 0;
return ii.intValue();
}
/** Set Name.
Alphanumeric identifier of the entity */
public void setName (String Name)
{
if (Name == null) throw new IllegalArgumentException ("Name is mandatory");
if (Name.length() > 60)
{
log.warning("Length > 60 - truncated");
Name = Name.substring(0,60);
}
set_Value ("Name", Name);
}
/** Get Name.
Alphanumeric identifier of the entity */
public String getName() 
{
return (String)get_Value("Name");
}
public KeyNamePair getKeyNamePair() 
{
return new KeyNamePair(getID(), getName());
}
public static final int OPERATIONMODE_AD_Reference_ID = MReference.getReferenceID("C_POS Operation Mode");
/** POS Simple = S */
public static final String OPERATIONMODE_POSSimple = "S";
/** POS Journal = J */
public static final String OPERATIONMODE_POSJournal = "J";
/** Set Operation Mode.
Operation Mode */
public void setOperationMode (String OperationMode)
{
if (OperationMode.equals("S") || OperationMode.equals("J"));
 else throw new IllegalArgumentException ("OperationMode Invalid value - Reference = OPERATIONMODE_AD_Reference_ID - S - J");
if (OperationMode == null) throw new IllegalArgumentException ("OperationMode is mandatory");
if (OperationMode.length() > 1)
{
log.warning("Length > 1 - truncated");
OperationMode = OperationMode.substring(0,1);
}
set_Value ("OperationMode", OperationMode);
}
/** Get Operation Mode.
Operation Mode */
public String getOperationMode() 
{
return (String)get_Value("OperationMode");
}
/** Set POS Number */
public void setPOSNumber (int POSNumber)
{
set_Value ("POSNumber", new Integer(POSNumber));
}
/** Get POS Number */
public int getPOSNumber() 
{
Integer ii = (Integer)get_Value("POSNumber");
if (ii == null) return 0;
return ii.intValue();
}
/** Set Printer Name.
Name of the Printer */
public void setPrinterName (String PrinterName)
{
if (PrinterName != null && PrinterName.length() > 60)
{
log.warning("Length > 60 - truncated");
PrinterName = PrinterName.substring(0,60);
}
set_Value ("PrinterName", PrinterName);
}
/** Get Printer Name.
Name of the Printer */
public String getPrinterName() 
{
return (String)get_Value("PrinterName");
}
public static final int SALESREP_ID_AD_Reference_ID = MReference.getReferenceID("AD_User - SalesRep");
/** Set Sales Representative.
Sales Representative or Company Agent */
public void setSalesRep_ID (int SalesRep_ID)
{
set_Value ("SalesRep_ID", new Integer(SalesRep_ID));
}
/** Get Sales Representative.
Sales Representative or Company Agent */
public int getSalesRep_ID() 
{
Integer ii = (Integer)get_Value("SalesRep_ID");
if (ii == null) return 0;
return ii.intValue();
}
}

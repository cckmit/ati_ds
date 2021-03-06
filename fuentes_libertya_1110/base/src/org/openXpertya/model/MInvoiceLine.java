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

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;
import java.util.logging.Level;

import org.openXpertya.util.CLogger;
import org.openXpertya.util.DB;
import org.openXpertya.util.Env;
import org.openXpertya.util.MProductCache;
import org.openXpertya.util.Util;

/**
 * Descripción de Clase
 *
 *
 * @version    2.2, 12.10.07
 * @author     Equipo de Desarrollo de openXpertya    
 */

public class MInvoiceLine extends X_C_InvoiceLine {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/** Especifica si la línea debe actualizar los impuestos 
	 *  en la cabecera (en caso que esto sea posible) */
	boolean shouldUpdateHeader = true;
	
	/**
	 * Bypass para actualización del descuento manual general de la factura
	 * (Sólo para Facturas de Cliente, no TPV)
	 */
	private boolean skipManualGeneralDiscount = false;
	
    /**
     * Descripción de Método
     *
     *
     * @param sLine
     *
     * @return
     */

    public static MInvoiceLine getOfInOutLine( MInOutLine sLine ) {
        if( sLine == null ) {
            return null;
        }

        MInvoiceLine      retValue = null;
        String            sql      = "SELECT * FROM C_InvoiceLine WHERE M_InOutLine_ID=?";
        PreparedStatement pstmt    = null;

        try {
            pstmt = DB.prepareStatement( sql,sLine.get_TableName());
            pstmt.setInt( 1,sLine.getM_InOutLine_ID());

            ResultSet rs = pstmt.executeQuery();

            if( rs.next()) {
                retValue = new MInvoiceLine( sLine.getCtx(),rs,sLine.get_TrxName());

                if( rs.next()) {
                    s_log.warning( "More than one C_InvoiceLine of " + sLine );
                }
            }

            rs.close();
            pstmt.close();
            pstmt = null;
        } catch( Exception e ) {
            s_log.log( Level.SEVERE,sql,e );
        }

        try {
            if( pstmt != null ) {
                pstmt.close();
            }

            pstmt = null;
        } catch( Exception e ) {
            pstmt = null;
        }

        return retValue;
    }    // getOfInOutLine

    /** Descripción de Campos */

    private static CLogger s_log = CLogger.getCLogger( MInvoiceLine.class );

    /**
     * Constructor de la clase ...
     *
     *
     * @param ctx
     * @param C_InvoiceLine_ID
     * @param trxName
     */

    public MInvoiceLine( Properties ctx,int C_InvoiceLine_ID,String trxName ) {
        super( ctx,C_InvoiceLine_ID,trxName );

        if( C_InvoiceLine_ID == 0 ) {
            setIsDescription( false );
            setIsPrinted( true );
            setLineNetAmt( Env.ZERO );
            setPriceEntered( Env.ZERO );
            setPriceActual( Env.ZERO );
            setPriceLimit( Env.ZERO );
            setPriceList( Env.ZERO );
            setM_AttributeSetInstance_ID( 0 );
            setTaxAmt( Env.ZERO );

            //

            setQtyEntered( Env.ZERO );
            setQtyInvoiced( Env.ZERO );
        }
    }    // MInvoiceLine

    /**
     * Constructor de la clase ...
     *
     *
     * @param invoice
     */

    public MInvoiceLine( MInvoice invoice ) {
        this( invoice.getCtx(),0,invoice.get_TrxName());

        if( invoice.getID() == 0 ) {
            throw new IllegalArgumentException( "Header not saved" );
        }

        setClientOrg( invoice.getAD_Client_ID(),invoice.getAD_Org_ID());
        setC_Invoice_ID( invoice.getC_Invoice_ID());
        setInvoice( invoice );
    }    // MInvoiceLine

    /**
     * Constructor de la clase ...
     *
     *
     * @param ctx
     * @param rs
     * @param trxName
     */

    public MInvoiceLine( Properties ctx,ResultSet rs,String trxName ) {
        super( ctx,rs,trxName );
    }    // MInvoiceLine

    /** Descripción de Campos */

    private int m_M_PriceList_ID = 0;

    /** Descripción de Campos */

    private Timestamp m_DateInvoiced = null;

    /** Descripción de Campos */

    private int m_C_BPartner_ID = 0;

    /** Descripción de Campos */

    private int m_C_BPartner_Location_ID = 0;

    /** Descripción de Campos */

    private boolean m_IsSOTrx = true;

    /** Descripción de Campos */

    private boolean m_priceSet = false;

    /** Descripción de Campos */

    private MProduct m_product = null;

    /** Descripción de Campos */

    private String m_name = null;

    /** Descripción de Campos */

    private Integer m_precision = null;

    /** Descripción de Campos */

    private MProductPricing m_productPricing = null;

    /**
     * Descripción de Método
     *
     *
     * @param invoice
     */

    public void setInvoice( MInvoice invoice ) {
        setClientOrg( invoice );
        m_M_PriceList_ID         = invoice.getM_PriceList_ID();
        m_DateInvoiced           = invoice.getDateInvoiced();
        m_C_BPartner_ID          = invoice.getC_BPartner_ID();
        m_C_BPartner_Location_ID = invoice.getC_BPartner_Location_ID();
        m_IsSOTrx                = invoice.isSOTrx();
        m_precision              = new Integer( invoice.getPrecision());
    }    // setOrder

    /**
     * Descripción de Método
     *
     *
     * @param oLine
     */

    public void setOrderLine( MOrderLine oLine ) {
        setC_OrderLine_ID( oLine.getC_OrderLine_ID());

        //

        setLine( oLine.getLine());
        setIsDescription( oLine.isDescription());
        setDescription( oLine.getDescription());

        //
        setC_Project_ID(oLine.getC_Project_ID());
        setC_Charge_ID( oLine.getC_Charge_ID());

        //

        setM_Product_ID( oLine.getM_Product_ID());
        setM_AttributeSetInstance_ID( oLine.getM_AttributeSetInstance_ID());
        setS_ResourceAssignment_ID( oLine.getS_ResourceAssignment_ID());
        setC_UOM_ID( oLine.getC_UOM_ID());

        //

        setPriceEntered( oLine.getPriceEntered());
        setPriceActual( oLine.getPriceActual());
        setPriceLimit( oLine.getPriceLimit());
        setPriceList( oLine.getPriceList());

        //

        setC_Tax_ID( oLine.getC_Tax_ID());
        setLineNetAmt( oLine.getLineNetAmt());
        
        // Descuentos
        setDocumentDiscountAmt(oLine.getDocumentDiscountAmt());
        setLineBonusAmt(oLine.getLineBonusAmt());
        setLineDiscountAmt(oLine.getLineDiscountAmt());
        
        setTaxAmt();
        setLineTotalAmt(getLineNetAmt().add(getTaxAmt()));
    }    // setOrderLine

    /**
     * Descripción de Método
     *
     *
     * @param sLine
     */

    public void setShipLine( MInOutLine sLine ) {
        setM_InOutLine_ID( sLine.getM_InOutLine_ID());
        setC_OrderLine_ID( sLine.getC_OrderLine_ID());

        //

        setLine( sLine.getLine());
        setIsDescription( sLine.isDescription());
        setDescription( sLine.getDescription());

        //
        setC_Project_ID(sLine.getC_Project_ID());
        setM_Product_ID( sLine.getM_Product_ID());
        setC_UOM_ID( sLine.getC_UOM_ID());
        setM_AttributeSetInstance_ID( sLine.getM_AttributeSetInstance_ID());

        // setS_ResourceAssignment_ID(sLine.getS_ResourceAssignment_ID());

        setC_Charge_ID( sLine.getC_Charge_ID());

        //

        int C_OrderLine_ID = sLine.getC_OrderLine_ID();

        if( C_OrderLine_ID != 0 ) {
            MOrderLine oLine = new MOrderLine( getCtx(),C_OrderLine_ID,get_TrxName());

            setS_ResourceAssignment_ID( oLine.getS_ResourceAssignment_ID());

            //

            setPriceEntered( oLine.getPriceEntered());
            setPriceActual( oLine.getPriceActual());
            setPriceLimit( oLine.getPriceLimit());
            setPriceList( oLine.getPriceList());

            //

            setC_Tax_ID( oLine.getC_Tax_ID());
            setLineNetAmt( oLine.getLineNetAmt());
        } else {
            setPrice();
            setTax();
        }
    }    // setOrderLine

    /**
     * Descripción de Método
     *
     *
     * @param description
     */

    public void addDescription( String description ) {
        String desc = getDescription();

        if( desc == null ) {
            setDescription( description );
        } else {
            setDescription( desc + " | " + description );
        }
    }    // addDescription

    /**
     * Descripción de Método
     *
     *
     * @param M_AttributeSetInstance_ID
     */

    public void setM_AttributeSetInstance_ID( int M_AttributeSetInstance_ID ) {
        if( M_AttributeSetInstance_ID == 0 ) {    // 0 is valid ID
            set_Value( "M_AttributeSetInstance_ID",new Integer( 0 ));
        } else {
            super.setM_AttributeSetInstance_ID( M_AttributeSetInstance_ID );
        }
    }                                             // setM_AttributeSetInstance_ID

    /**
     * Descripción de Método
     *
     */

    public void setPrice() {
        if( (getM_Product_ID() == 0) || isDescription()) {
            return;
        }

        if( (m_M_PriceList_ID == 0) || (m_C_BPartner_ID == 0) ) {
            MInvoice invoice = new MInvoice( getCtx(),getC_Invoice_ID(),get_TrxName());

            setInvoice( invoice );
        }

        if( (m_M_PriceList_ID == 0) || (m_C_BPartner_ID == 0) ) {
            throw new IllegalStateException( "setPrice - PriceList unknown!" );
        }

        setPrice( m_M_PriceList_ID,m_C_BPartner_ID );
    }    // setPrice

    /**
     * Descripción de Método
     *
     *
     * @param M_PriceList_ID
     * @param C_BPartner_ID
     */

    public void setPrice( int M_PriceList_ID,int C_BPartner_ID ) {
        if( (getM_Product_ID() == 0) || isDescription()) {
            return;
        }

        //

        log.fine( "M_PriceList_ID=" + M_PriceList_ID );
        m_productPricing = new MProductPricing( getM_Product_ID(),C_BPartner_ID,getQtyInvoiced(),m_IsSOTrx );
        m_productPricing.setM_PriceList_ID( M_PriceList_ID );
        m_productPricing.setPriceDate( m_DateInvoiced );

        //

        setPriceActual( m_productPricing.getPriceStd());
        setPriceList( m_productPricing.getPriceList());
        setPriceLimit( m_productPricing.getPriceLimit());

        //

        if( getQtyEntered().compareTo( getQtyInvoiced()) == 0 ) {
            setPriceEntered( getPriceActual());
        } else {
            setPriceEntered( getPriceActual().multiply( getQtyInvoiced().divide( getQtyEntered(),BigDecimal.ROUND_HALF_UP )));    // no precision
        }

        //

        if( getC_UOM_ID() == 0 ) {
            setC_UOM_ID( m_productPricing.getC_UOM_ID());
        }

        //

        m_priceSet = true;
    }    // setPrice

    /**
     * Descripción de Método
     *
     *
     * @param PriceActual
     */

    public void setPrice( BigDecimal PriceActual ) {
        setPriceEntered( PriceActual );
        setPriceActual( PriceActual );
    }    // setPrice

    /**
     * Descripción de Método
     *
     *
     * @param PriceActual
     */

    public void setPriceActual( BigDecimal PriceActual ) {
        if( PriceActual == null ) {
            throw new IllegalArgumentException( "PriceActual is mandatory" );
        }

        set_ValueNoCheck( "PriceActual",PriceActual );
    }    // setPriceActual

    /**
     * Descripción de Método
     *
     *
     * @return
     */

    public boolean setTax() {
        if( isDescription()) {
            return true;
        }

        //

        int M_Warehouse_ID = Env.getContextAsInt( getCtx(),"#M_Warehouse_ID" );

        //

        int C_Tax_ID = Tax.get( getCtx(),getM_Product_ID(),getC_Charge_ID(),m_DateInvoiced,m_DateInvoiced,getAD_Org_ID(),M_Warehouse_ID,m_C_BPartner_Location_ID,    // should be bill to
                                m_C_BPartner_Location_ID,m_IsSOTrx );

        if( C_Tax_ID == 0 ) {
            log.log( Level.SEVERE,"No Tax found" );

            return false;
        }

        setC_Tax_ID( C_Tax_ID );

        if( m_IsSOTrx ) {}

        return true;
    }    // setTax

    /**
     * Descripción de Método
     *
     */

    public void setTaxAmt() {
        BigDecimal TaxAmt = Env.ZERO;

        if( getC_Tax_ID() != 0 ) {

            // setLineNetAmt();

            MTax tax = new MTax( getCtx(),getC_Tax_ID(),get_TrxName());

            if((getTaxAmt() == null || (getTaxAmt() != null && getTaxAmt().compareTo(BigDecimal.ZERO) == 0))
            		&& tax.getRate().compareTo(BigDecimal.ZERO) > 0){
            	TaxAmt = tax.calculateTax( getLineNetAmt().subtract(getDocumentDiscountAmt()),isTaxIncluded(),getPrecision());
            }
            else{
            	TaxAmt = getTaxAmt();
            }
        }

        super.setTaxAmt( TaxAmt );
    }    // setTaxAmt

    /**
     * Descripción de Método
     *
     */

    public void setLineNetAmt() {

        // Calculations & Rounding

        BigDecimal net = getPriceActual().multiply( getQtyInvoiced());

        if( net.scale() > getPrecision()) {
            net = net.setScale( getPrecision(),BigDecimal.ROUND_HALF_UP );
        }

        super.setLineNetAmt( net );
    }    // setLineNetAmt

    /**
     * Descripción de Método
     *
     *
     * @param Qty
     */

    public void setQty( int Qty ) {
        setQty( new BigDecimal( Qty ));
    }    // setQtyInvoiced

    /**
     * Descripción de Método
     *
     *
     * @param Qty
     */

    public void setQty( BigDecimal Qty ) {
        setQtyEntered( Qty );
        setQtyInvoiced( Qty );
    }    // setQtyInvoiced

    /**
     * Descripción de Método
     *
     *
     * @param product
     */

    public void setProduct( MProduct product ) {
        m_product = product;

        if( m_product != null ) {
            setM_Product_ID( m_product.getM_Product_ID());
            setC_UOM_ID( m_product.getC_UOM_ID());
        } else {
            setM_Product_ID( 0 );
            setC_UOM_ID( 0 );
        }

        setM_AttributeSetInstance_ID( 0 );
    }    // setProduct

    /**
     * Descripción de Método
     *
     *
     * @param M_Product_ID
     * @param setUOM
     */

    public void setM_Product_ID( int M_Product_ID,boolean setUOM ) {
        if( setUOM ) {
            setProduct( MProduct.get( getCtx(),M_Product_ID ));
        } else {
            super.setM_Product_ID( M_Product_ID );
        }

        setM_AttributeSetInstance_ID( 0 );
    }    // setM_Product_ID

    /**
     * Descripción de Método
     *
     *
     * @param M_Product_ID
     * @param C_UOM_ID
     */

    public void setM_Product_ID( int M_Product_ID,int C_UOM_ID ) {
        super.setM_Product_ID( M_Product_ID );
        super.setC_UOM_ID( C_UOM_ID );
        setM_AttributeSetInstance_ID( 0 );
    }    // setM_Product_ID

    /**
     * Descripción de Método
     *
     *
     * @return
     */

    public String toString() {
        StringBuffer sb = new StringBuffer( "MInvoiceLine[" ).append( getID()).append( "," ).append( getLine()).append( ",QtyInvoiced=" ).append( getQtyInvoiced()).append( ",LineNetAmt=" ).append( getLineNetAmt()).append( "]" );

        return sb.toString();
    }    // toString

    /**
     * Descripción de Método
     *
     *
     * @return
     */

    public String getName() {
        if( m_name == null ) {
            String sql = "SELECT COALESCE (p.Name, c.Name) " + "FROM C_InvoiceLine il" + " LEFT OUTER JOIN M_Product p ON (il.M_Product_ID=p.M_Product_ID)" + " LEFT OUTER JOIN C_Charge C ON (il.C_Charge_ID=c.C_Charge_ID) " + "WHERE C_InvoiceLine_ID=?";
            PreparedStatement pstmt = null;

            try {
                pstmt = DB.prepareStatement( sql,get_TrxName());
                pstmt.setInt( 1,getC_InvoiceLine_ID());

                ResultSet rs = pstmt.executeQuery();

                if( rs.next()) {
                    m_name = rs.getString( 1 );
                }

                rs.close();
                pstmt.close();
                pstmt = null;

                if( m_name == null ) {
                    m_name = "??";
                }
            } catch( Exception e ) {
                log.log( Level.SEVERE,"getName",e );
            } finally {
                try {
                    if( pstmt != null ) {
                        pstmt.close();
                    }
                } catch( Exception e ) {
                }

                pstmt = null;
            }
        }

        return m_name;
    }    // getName

    /**
     * Descripción de Método
     *
     *
     * @param tempName
     */

    public void setName( String tempName ) {
        m_name = tempName;
    }    // setName

    /**
     * Descripción de Método
     *
     *
     * @return
     */

    public String getDescriptionText() {
        return super.getDescription();
    }    // getDescriptionText

    /**
     * Descripción de Método
     *
     *
     * @return
     */

    public int getPrecision() {
        if( m_precision != null ) {
            return m_precision.intValue();
        }

        String sql = "SELECT c.StdPrecision " + "FROM C_Currency c INNER JOIN C_Invoice x ON (x.C_Currency_ID=c.C_Currency_ID) " + "WHERE x.C_Invoice_ID=?";
        int i = DB.getSQLValue( get_TrxName(),sql,getC_Invoice_ID());

        if( i < 0 ) {
            log.warning( "getPrecision = " + i + " - set to 2" );
            i = 2;
        }

        m_precision = new Integer( i );

        return m_precision.intValue();
    }    // getPrecision

    /**
     * Descripción de Método
     *
     *
     * @return
     */

    public boolean isTaxIncluded() {
		m_M_PriceList_ID = DB.getSQLValue(get_TrxName(),
				"SELECT M_PriceList_ID FROM C_Invoice WHERE C_Invoice_ID=?",
				getC_Invoice_ID());
        MPriceList pl = MPriceList.get( getCtx(),m_M_PriceList_ID,get_TrxName());
        return pl.isTaxIncluded();
    }    // isTaxIncluded

    /**
     * Descripción de Método
     *
     *
     * @param newRecord
     *
     * @return
     */

    protected boolean beforeSave( boolean newRecord ) {
        log.fine( "New=" + newRecord );

        // Charge

        if( getC_Charge_ID() != 0 ) {
            if( getM_Product_ID() != 0 ) {
                setM_Product_ID( 0 );
            }
        } else    // Set Product Price
        {
            if( !m_priceSet && (Env.ZERO.compareTo( getPriceActual()) == 0) && (Env.ZERO.compareTo( getPriceList()) == 0) ) {
                setPrice();
            }
        }

        // Set Tax

        if( getC_Tax_ID() == 0 ) {
            setTax();
        }

        // Get Line No

        if( getLine() == 0 ) {
            String sql = "SELECT COALESCE(MAX(Line),0)+10 FROM C_InvoiceLine WHERE C_Invoice_ID=?";
            int ii = DB.getSQLValue( get_TrxName(),sql,getC_Invoice_ID());

            setLine( ii );
        }

        // UOM

        if( getC_UOM_ID() == 0 ) {
            int C_UOM_ID = MUOM.getDefault_UOM_ID( getCtx());

            if( C_UOM_ID > 0 ) {
                setC_UOM_ID( C_UOM_ID );
            }
        }

		// Actualización de precio en base al descuento manual general
        // Esto es importante dejar antes de actualizar el total de la línea
        if(!isSkipManualGeneralDiscount()){
        	// Descuento manual general
    		BigDecimal generalDiscountManual = DB
    				.getSQLValueBD(
    						get_TrxName(),
    						"SELECT ManualGeneralDiscount FROM c_invoice WHERE c_invoice_id = ?",
    						getC_Invoice_ID());
			if (generalDiscountManual.compareTo(BigDecimal.ZERO) != 0) {
        		int M_PriceList_ID = Env.getContextAsInt( getCtx(),"M_PriceList_ID" );
                int stdPrecision = MPriceList.getStandardPrecision( getCtx(),M_PriceList_ID );
        		updateGeneralManualDiscount(generalDiscountManual, stdPrecision);	
    		}
        }
        
        // Calculations & Rounding

        setLineNetAmt();
        
        // Comentado para poder calcular TaxAmt y LineTotalAmt en Facturas de Cliente
        /*
        if( !m_IsSOTrx    // AP Inv Tax Amt
                && (getTaxAmt().compareTo( Env.ZERO ) == 0) ) {
            setTaxAmt();
        }
		*/
        //

        /* Si el project no está seteado, tomar el de la cabecera */
        if (getC_Project_ID() == 0)
        	setC_Project_ID(DB.getSQLValue(get_TrxName(), " SELECT C_Project_ID FROM C_Invoice WHERE C_Invoice_ID = " + getC_Invoice_ID()));
        
        // Calculo TaxAmt y LineTotalAmt
        // Recupero el impuesto aplicado a la línea
        setTaxAmt();
        setLineTotalAmt(getLineNetAmt().add(getTaxAmt()));
        
        // Setear el proveedor del artículo actual y el precio de costo
        if(!Util.isEmpty(getM_Product_ID(), true)){    		
	        MInvoice invoice = new MInvoice(getCtx(), getC_Invoice_ID(), get_TrxName());
	        MProductPO po = null;
	        if(invoice.isSOTrx()){
	        	// Obtener el proveedor actual del artículo
				po = MProductPO.getOfOneProduct(getCtx(), getM_Product_ID(), get_TrxName());
				setC_BPartner_Vendor_ID(po != null?po.getC_BPartner_ID():0);
	        }
	        else{
	        	setC_BPartner_Vendor_ID(invoice.getC_BPartner_ID());
	        }
	        // Setear el precio de costo
			BigDecimal costPrice = BigDecimal.ZERO;
			boolean decrementTaxAmt = false;
			int costCurrency = Env.getContextAsInt(getCtx(), "$C_Currency_ID");
			// 1) Tarifas de costo del proveedor
			if(!Util.isEmpty(getC_BPartner_Vendor_ID(), true)){
				MBPartner vendor = new MBPartner(getCtx(), getC_BPartner_Vendor_ID(), get_TrxName());
				if(!Util.isEmpty(vendor.getPO_PriceList_ID(), true)){
					MProductPrice pp = getProductPrice(getM_Product_ID(),
							vendor.getPO_PriceList_ID(), false);
					if(pp != null){
						costPrice = pp.getPriceStd();
						// Determino si tengo que decrementar el impuesto y la moneda
						MPriceList priceList = MPriceList.get(getCtx(),
								vendor.getPO_PriceList_ID(),
								get_TrxName());
						decrementTaxAmt = priceList.isTaxIncluded();
						costCurrency = priceList.getC_Currency_ID();
					}
				}
				
			}
			// 2) Tarifas de costo (todas)
			if(costPrice.compareTo(BigDecimal.ZERO) == 0){
				MProductPrice pp = getProductPrice(getM_Product_ID(), null,
						false);
				if(pp != null){
					costPrice = pp.getPriceStd();
					// Determino si tengo que decrementar el impuesto y la moneda
					MPriceListVersion priceListVersion = new MPriceListVersion(
							getCtx(), pp.getM_PriceList_Version_ID(),
							get_TrxName());
					MPriceList priceList = MPriceList.get(getCtx(),
							priceListVersion.getM_PriceList_ID(),
							get_TrxName());
					decrementTaxAmt = priceList.isTaxIncluded();
					costCurrency = priceList.getC_Currency_ID();
				}
			}
			
			// 3) m_producto_po con ese proveedor
			if(costPrice.compareTo(BigDecimal.ZERO) == 0){
				// Si no tengo a priori el PO, entonces lo busco
				if(po == null && !Util.isEmpty(getC_BPartner_Vendor_ID(), true)){
					// Obtención del po
					po = MProductPO.get(getCtx(), getM_Product_ID(),
							getC_BPartner_Vendor_ID(), get_TrxName());
				}
				// Si puedo obtener el precio de ahí entonces lo obtengo
				if(po != null){
					costPrice = po.getPriceList();
					// Verificar la moneda si hay que convertir
					costCurrency = po.getC_Currency_ID();
				}
			}
			// Seteo el precio de costo
			if(costPrice.compareTo(BigDecimal.ZERO) > 0){
				BigDecimal costPriceConverted = MConversionRate.convertBase(getCtx(), costPrice,
						costCurrency, invoice.getDateInvoiced(), 0,
						getAD_Client_ID(), getAD_Org_ID());
				costPrice = costPriceConverted != null
						&& costPriceConverted.compareTo(BigDecimal.ZERO) != 0? costPriceConverted
						: costPrice;
			}
			else{
				decrementTaxAmt = false;
			}
			setCostPrice(costPrice);
			// Decrementar el monto de impuesto al precio de costo? esto pasa el
			// impuesto está incluído en la tarifa
			if(decrementTaxAmt){
				BigDecimal taxAmtConverted = MConversionRate.convertBase(
						getCtx(), getTaxAmt(), invoice.getC_Currency_ID(),
						invoice.getDateInvoiced(), 0, getAD_Client_ID(),
						getAD_Org_ID());
				taxAmtConverted = taxAmtConverted != null?taxAmtConverted:getTaxAmt();
				setCostPrice(getCostPrice().subtract(taxAmtConverted));
			}
        }
        /*
        String sql = "select rate from c_tax where c_tax_id = " + getC_Tax_ID();
        PreparedStatement pstmt = DB.prepareStatement(sql, get_TrxName());
		try {
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()){
				BigDecimal vRate = rs.getBigDecimal("rate").divide(new BigDecimal(100));
				BigDecimal vTaxAmt = getLineNetAmt().multiply(vRate);
				
				// Seteo TaxAmt y LineTotalAmt
				setTaxAmt(vTaxAmt);
				setLineTotalAmt(getLineNetAmt().add(vTaxAmt));
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        */
        return true;
    }    // beforeSave

	/**
	 * Actualiza el descuento de la línea en base del descuento manual general
	 * 
	 * @param generalManualDiscount
	 * @param scale
	 */
    public void updateGeneralManualDiscount(BigDecimal generalManualDiscount, int scale){
		BigDecimal priceList = getPriceList().compareTo(BigDecimal.ZERO) != 0 ? getPriceList()
				: getPriceActual();
		BigDecimal lineDiscountAmtUnit = priceList.multiply(
				generalManualDiscount).divide(HUNDRED, scale,
				BigDecimal.ROUND_HALF_UP);
		// Seteo el precio ingresado con el precio de lista - monto de
		// descuento
		setPriceEntered(priceList.subtract(lineDiscountAmtUnit));
		setPriceActual(getPriceEntered());
		setLineDiscountAmt(lineDiscountAmtUnit.multiply(getQtyEntered()));
    }
    
    /**
     * Descripción de Método
     *
     *
     * @param newRecord
     * @param success
     *
     * @return
     */

    protected boolean afterSave( boolean newRecord,boolean success ) {
        if( !success ) {
            return success;
        }

        if( !newRecord && is_ValueChanged( "C_Tax_ID" )) {

            // Recalculate Tax for old Tax

            MInvoiceTax tax = MInvoiceTax.get( this,getPrecision(),true,get_TrxName());    // old Tax

            if( tax != null ) {
                if( !tax.calculateTaxFromLines()) {
                    return false;
                }

                if( !tax.save( get_TrxName())) {
                    return true;
                }
            }
        }
        
        if(shouldUpdateHeader){
        	if(!updateHeaderTax()){
        		return false;
        	}
        	
        	// Esquema de vencimientos
	        MInvoice invoice = new MInvoice(getCtx(), getC_Invoice_ID(), get_TrxName());
			MPaymentTerm pt = new MPaymentTerm(getCtx(), invoice.getC_PaymentTerm_ID(), get_TrxName());
			if (!pt.apply(invoice.getID()))
				return false;
        }
		
        return true;
    }    // afterSave

    /**
     * Descripción de Método
     *
     *
     * @param success
     *
     * @return
     */

    protected boolean afterDelete( boolean success ) {
        if( !success ) {
            return success;
        }

        return !shouldUpdateHeader || updateHeaderTax();
    }    // afterDelete

	/**
	 * Obtener el precio del producto parámetro, en la lista de precios
	 * parámetro y si debe ser de ventas o compras el precio. El precio que se
	 * obtiene está dado por el siguiente órden: 1) Precio de lista default; 2)
	 * Versión con campo "Válido desde" más nuevo; 3) Versión con campo created
	 * más nuevo. estos 3 criterios determinan qué lista de precios tomar.
	 * 
	 * @param productID
	 *            id de producto
	 * @param priceListID
	 *            id de la lista de precios, null si no se debe filtrar por ella
	 * @param isSoPriceList
	 *            true si es precio de venta, false si es precio de compra y
	 *            null si no se debe colocar la condición
	 * @return precio más nuevo del producto parámetro, null si no existe
	 *         ninguno con los parámetros dados
	 */
    private MProductPrice getProductPrice(int productID, Integer priceListID, Boolean isSoPriceList){
    	StringBuffer sql = new StringBuffer(
		"select pp.m_product_id, pp.m_pricelist_version_id " +
		"from m_pricelist_version as plv " +
		"inner join m_pricelist as pl on pl.m_pricelist_id = plv.m_pricelist_id " +
		"inner join m_productprice as pp on pp.m_pricelist_version_id = plv.m_pricelist_version_id " +
		"where m_product_id = ? ");
		if(!Util.isEmpty(priceListID, true)){
			sql.append(" AND pl.m_pricelist_id = ").append(priceListID);
		}
		if(isSoPriceList != null){
			sql.append(" AND pl.issopricelist = '").append(isSoPriceList?"Y":"N").append("' ");
		}
		sql.append(" order by pl.isdefault desc, plv.validfrom desc, plv.created desc ");
		MProductPrice price = null;
    	PreparedStatement ps = null;
    	ResultSet rs = null;
    	try {
			ps = DB.prepareStatement(sql.toString(), get_TrxName());
			ps.setInt(1, productID);
			rs = ps.executeQuery();
			if(rs.next()){
				price = MProductPrice.get(getCtx(),
						rs.getInt("m_pricelist_version_id"),
						productID, get_TrxName());
			}
		} catch (Exception e) {
			log.severe("Error finding product price for product " + productID
					+ ". Error: " + e.getMessage());
		} finally{
			try {
				if(ps != null)ps.close();
				if(rs != null)rs.close();
			} catch (Exception e2) {
				log.severe("Error finding product price for product " + productID
						+ ". Error: " + e2.getMessage());
			}
		}
		return price;
    }
    
    
    /**
     * Descripción de Método
     *
     *
     * @return
     */

    private boolean updateHeaderTax() {

        // Recalculate Tax for this Tax

        MInvoiceTax tax = MInvoiceTax.get( this,getPrecision(),false,get_TrxName());    // current Tax

        if( tax != null ) {
            if( !tax.calculateTaxFromLines()) {
                return false;
            }

            if( !tax.save( get_TrxName())) {
                return false;
            }
        }

        // Update Invoice Header

        String sql = "UPDATE C_Invoice i" + " SET TotalLines=" + "(SELECT COALESCE(SUM(LineNetAmt),0) FROM C_InvoiceLine il WHERE i.C_Invoice_ID=il.C_Invoice_ID) " + "WHERE C_Invoice_ID=" + getC_Invoice_ID();
        int no = DB.executeUpdate( sql,get_TrxName());

        if( no != 1 ) {
            log.warning( "updateHeaderTax (1) #" + no );
        }

        if( isTaxIncluded()) {
            sql = "UPDATE C_Invoice i " + " SET GrandTotal=TotalLines + ChargeAmt " + "WHERE C_Invoice_ID=" + getC_Invoice_ID();
        } else {
            sql = "UPDATE C_Invoice i " + " SET GrandTotal=TotalLines+" + "(SELECT COALESCE(SUM(TaxAmt),0) FROM C_InvoiceTax it WHERE i.C_Invoice_ID=it.C_Invoice_ID) + ChargeAmt " + "WHERE C_Invoice_ID=" + getC_Invoice_ID();
        }

        no = DB.executeUpdate( sql,get_TrxName());

        if( no != 1 ) {
            log.warning( "updateHeaderTax (2) #" + no );
        }

        return no == 1;
    }    // updateHeaderTax
    
    /** Devuelve la descripcion del producto asociado a la línea */
    public String getProductName()
    {
    	if (getM_Product_ID() > 0){
    		//MProduct prod = new MProduct(p_ctx, getM_Product_ID(), null);
    		//soporte para caches multi-documento
    		MProduct prod = getProduct();
    		//puede ser null... aunque no deberia
    		String prodName = prod == null? "" : prod.getName();
    		
    		//return getDescription() == null ? prod.getName() : (prod.getName() + " - " + getDescription());
       		return getDescription() == null ? prodName : (prodName + " - " + getDescription());
    	}
    	return getDescription();
    }

    /** Devuelve la descripcion del producto asociado a la línea */
    public String getProductValue()
    {
    	if (getM_Product_ID() > 0)
    	{
    		//soprote para caches multi-documento
    		MProduct prod = getProduct();
    		//puede ser null... aunque no deberia
    		String prodValue = prod == null? "": prod.getValue();
    		
    		//return (new MProduct(p_ctx, getM_Product_ID(), null)).getValue();
    		return prodValue;
    	}
    	return "";
    }    
    
    /** Devuelve la descripcion del producto asociado a la línea */
    public String getUOMName()
    {
    	if (getM_Product_ID() > 0 && getC_UOM_ID() > 0)
    		return (new MUOM(p_ctx, getC_UOM_ID(), null)).getName();
    	return "";
    }
    
    public String getLineStr()
    {
    	return "" + getLine();
    }
    
    public BigDecimal getTotalLineNoDsc()
    {
    	return getPriceEntered().multiply(getQtyEntered());
    }
    
	public boolean isShouldUpdateHeader() {
		return shouldUpdateHeader;
	}

	public void setShouldUpdateHeader(boolean shouldUpdateHeader) {
		this.shouldUpdateHeader = shouldUpdateHeader;
	}
		
    /**
     * @return la taza de impuesto configurada en esta línea 
     */
    public BigDecimal getTaxRate() {
		BigDecimal rate = BigDecimal.ZERO;
		if (getC_Tax_ID() > 0) {
			MTax tax = MTax.get(getCtx(), getC_Tax_ID(), get_TrxName());
			rate = tax !=  null ? tax.getRate() : rate;
		}
		return rate;
	}

	/**
	 * @return Indica si esta línea ha sufrido bonificaciones de su precio
	 *         original.
	 */
    public boolean hasBonus() {
    	return getLineBonusAmt().compareTo(BigDecimal.ZERO) != 0;
    }

	/**
	 * @return nombre de la entidad comercial configurada como proveedor
	 * NO MODIFICAR FIRMA, SE USA EN LA IMPRESIÓN DE LA FACTURA
	 */
    public String getBPartnerVendorName(){
    	String vendorName = "";
    	if(Util.isEmpty(getC_BPartner_Vendor_ID(), true)){
			MBPartner bpartner = new MBPartner(getCtx(),
					getC_BPartner_Vendor_ID(), get_TrxName());
			vendorName = bpartner.getName();
		}
    	return vendorName;
    }
    
    /**
     * @return nombre del cargo relacionado con esta línea
     * NO MODIFICAR FIRMA, SE USA EN LA IMPRESIÓN DE LA FACTURA
     */
    public String getChargeName(){
    	String changeName = "";
    	if(Util.isEmpty(getC_Charge_ID(), true)){
    		MCharge charge = new MCharge(getCtx(), getC_Charge_ID(), get_TrxName());
    		changeName = charge.getName();
    	}
    	return changeName;
    }
    
    /**
     * @return nombre del proyecto relacionado con esta línea
     * NO MODIFICAR FIRMA, SE USA EN LA IMPRESIÓN DE LA FACTURA
     */
    public String getProjectName(){
    	String projectName = "";
    	if(Util.isEmpty(getC_Project_ID(), true)){
    		MProject project = new MProject(getCtx(), getC_Project_ID(), get_TrxName());
    		projectName = project.getName();
    	}
    	return projectName;
    }

    /**
     * @return precio ingresado con impuestos
     * NO MODIFICAR FIRMA, SE USA EN LA IMPRESIÓN DE LA FACTURA
     */
    public BigDecimal getPriceEnteredWithTax(){
    	return amtByTax(getPriceEntered(), getTaxAmt(getPriceEntered()), isTaxIncluded(), true);
    }
    
    /**
     * @return precio ingresado sin impuestos
     *  NO MODIFICAR FIRMA, SE USA EN LA IMPRESIÓN DE LA FACTURA
     */
    public BigDecimal getPriceEnteredNet(){
    	return amtByTax(getPriceEntered(), getTaxAmt(getPriceEntered()), isTaxIncluded(), false);
    }
    
    /**
     * @return precio ingresado con impuestos * cantidad ingresada
     *  NO MODIFICAR FIRMA, SE USA EN LA IMPRESIÓN DE LA FACTURA
     */
    public BigDecimal getTotalPriceEnteredWithTax(){
    	return getPriceEnteredWithTax().multiply(getQtyEntered());
    }
    
    /**
     * @return precio ingresado sin impuestos * cantidad ingresada
     *  NO MODIFICAR FIRMA, SE USA EN LA IMPRESIÓN DE LA FACTURA
     */
    public BigDecimal getTotalPriceEnteredNet(){
    	return getPriceEnteredNet().multiply(getQtyEntered());
    }
    
    /**
     * @return precio de lista con impuestos
     *  NO MODIFICAR FIRMA, SE USA EN LA IMPRESIÓN DE LA FACTURA
     */
    public BigDecimal getPriceListWithTax(){
    	return amtByTax(getPriceList(),	getTaxAmt(getPriceList()), isTaxIncluded(), true);
    }
    
    /**
     * @return precio de lista sin impuestos
     *  NO MODIFICAR FIRMA, SE USA EN LA IMPRESIÓN DE LA FACTURA
     */
    public BigDecimal getPriceListNet(){
    	return amtByTax(getPriceList(),	getTaxAmt(getPriceList()), isTaxIncluded(), false);
    }
    
    /**
     * @return precio de lista con impuestos * cantidad ingresada
     *  NO MODIFICAR FIRMA, SE USA EN LA IMPRESIÓN DE LA FACTURA
     */
    public BigDecimal getTotalPriceListWithTax(){
		return getPriceListWithTax().multiply(getQtyEntered());
    }
    
    /**
     * @return precio de lista sin impuestos * cantidad ingresada
     *  NO MODIFICAR FIRMA, SE USA EN LA IMPRESIÓN DE LA FACTURA
     */
    public BigDecimal getTotalPriceListNet(){
    	return getPriceListNet().multiply(getQtyEntered());
    }
    
    /**
     * @return precio actual con impuestos
     *  NO MODIFICAR FIRMA, SE USA EN LA IMPRESIÓN DE LA FACTURA
     */
    public BigDecimal getPriceActualWithTax(){
    	return amtByTax(getPriceActual(), getTaxAmt(getPriceActual()), isTaxIncluded(), true);
    }
    
    /**
     * @return precio actual sin impuestos
     *  NO MODIFICAR FIRMA, SE USA EN LA IMPRESIÓN DE LA FACTURA
     */
    public BigDecimal getPriceActualNet(){
    	return amtByTax(getPriceActual(), getTaxAmt(getPriceActual()), isTaxIncluded(), false);
    }
    
    /**
     * @return precio actual con impuestos * cantiada ingresada
     *  NO MODIFICAR FIRMA, SE USA EN LA IMPRESIÓN DE LA FACTURA
     */
    public BigDecimal getTotalPriceActualWithTax(){
    	return getPriceActualWithTax().multiply(getQtyEntered());
    }
    
    /**
     * @return precio actual sin impuestos * cantiada ingresada
     *  NO MODIFICAR FIRMA, SE USA EN LA IMPRESIÓN DE LA FACTURA
     */
    public BigDecimal getTotalPriceActualNet(){
    	return getPriceActualNet().multiply(getQtyEntered());
    }

	/**
	 * @return bonificación con impuestos por unidad, o sea, bonificación con
	 *         impuesto / cantidad ingresada. NO MODIFICAR FIRMA, SE USA EN LA
	 *         IMPRESIÓN DE LA FACTURA
	 */
    public BigDecimal getBonusUnityAmtWithTax(){
    	BigDecimal unityAmt = getUnityAmt(getLineBonusAmt());
		return amtByTax(unityAmt, getTaxAmt(unityAmt), isTaxIncluded(), true);
    }
    
    /**
	 * @return bonificación sin impuestos por unidad, o sea, bonificación sin
	 *         impuesto / cantidad ingresada. NO MODIFICAR FIRMA, SE USA EN LA
	 *         IMPRESIÓN DE LA FACTURA
	 */
    public BigDecimal getBonusUnityAmtNet(){
    	BigDecimal unityAmt = getUnityAmt(getLineBonusAmt());
		return amtByTax(unityAmt, getTaxAmt(unityAmt), isTaxIncluded(), false);
    }
    
    /**
	 * @return bonificación con impuestos. NO MODIFICAR FIRMA, SE USA EN LA
	 *         IMPRESIÓN DE LA FACTURA
	 */
    public BigDecimal getTotalBonusUnityAmtWithTax(){
		return amtByTax(getLineBonusAmt(), getTaxAmt(getLineBonusAmt()), isTaxIncluded(), true);
    }
    
    /**
	 * @return bonificación sin impuestos. NO MODIFICAR FIRMA, SE USA EN LA
	 *         IMPRESIÓN DE LA FACTURA
	 */
    public BigDecimal getTotalBonusUnityAmtNet(){
    	return amtByTax(getLineBonusAmt(), getTaxAmt(getLineBonusAmt()), isTaxIncluded(), false);
    }

	/**
	 * @return descuento de línea con impuestos por unidad, o sea, descuento de
	 *         línea con impuestos / cantidad ingresada. NO MODIFICAR FIRMA, SE
	 *         USA EN LA IMPRESIÓN DE LA FACTURA
	 */
    public BigDecimal getLineDiscountUnityAmtWithTax(){
    	BigDecimal unityAmt = getUnityAmt(getLineDiscountAmt());
		return amtByTax(unityAmt, getTaxAmt(unityAmt), isTaxIncluded(), true);
    }
    
    /**
	 * @return descuento de línea sin impuestos por unidad, o sea, descuento de
	 *         línea sin impuestos / cantidad ingresada. NO MODIFICAR FIRMA, SE
	 *         USA EN LA IMPRESIÓN DE LA FACTURA
	 */
    public BigDecimal getLineDiscountUnityAmtNet(){
    	BigDecimal unityAmt = getUnityAmt(getLineDiscountAmt());
		return amtByTax(unityAmt, getTaxAmt(unityAmt), isTaxIncluded(), false);
    }
    
    /**
	 * @return descuento de línea con impuestos. NO MODIFICAR FIRMA, SE USA EN LA
	 *         IMPRESIÓN DE LA FACTURA
	 */
    public BigDecimal getTotalLineDiscountUnityAmtWithTax(){
		return amtByTax(getLineDiscountAmt(), getTaxAmt(getLineDiscountAmt()),
				isTaxIncluded(), true);
    }
    
    /**
	 * @return descuento de línea sin impuestos. NO MODIFICAR FIRMA, SE USA EN LA
	 *         IMPRESIÓN DE LA FACTURA
	 */
    public BigDecimal getTotalLineDiscountUnityAmtNet(){
		return amtByTax(getLineDiscountAmt(), getTaxAmt(getLineDiscountAmt()),
				isTaxIncluded(), false);
    }

	/**
	 * @return descuento de documento con impuestos por unidad, o sea, descuento
	 *         de documento con impuestos / cantidad ingresada. NO MODIFICAR
	 *         FIRMA, SE USA EN LA IMPRESIÓN DE LA FACTURA
	 */
    public BigDecimal getDocumentDiscountUnityAmtWithTax(){
    	BigDecimal unityAmt = getUnityAmt(getDocumentDiscountAmt());
		return amtByTax(unityAmt, getTaxAmt(unityAmt), isTaxIncluded(), true);
    }
    
    /**
	 * @return descuento de documento sin impuestos por unidad, o sea, descuento
	 *         de documento sin impuestos / cantidad ingresada. NO MODIFICAR
	 *         FIRMA, SE USA EN LA IMPRESIÓN DE LA FACTURA
	 */
    public BigDecimal getDocumentDiscountUnityAmtNet(){
    	BigDecimal unityAmt = getUnityAmt(getDocumentDiscountAmt());
		return amtByTax(unityAmt, getTaxAmt(unityAmt), isTaxIncluded(), false);
    }
    
    /**
	 * @return descuento de documento con impuestos. NO MODIFICAR FIRMA, SE USA EN LA
	 *         IMPRESIÓN DE LA FACTURA
	 */
    public BigDecimal getTotalDocumentDiscountUnityAmtWithTax(){
		return amtByTax(getDocumentDiscountAmt(), getTaxAmt(getDocumentDiscountAmt()),
				isTaxIncluded(), true);
    }
    
    /**
	 * @return descuento de documento sin impuestos. NO MODIFICAR FIRMA, SE USA EN LA
	 *         IMPRESIÓN DE LA FACTURA
	 */
    public BigDecimal getTotalDocumentDiscountUnityAmtNet(){
		return amtByTax(getDocumentDiscountAmt(), getTaxAmt(getDocumentDiscountAmt()),
				isTaxIncluded(), false);
    }

	/**
	 * Obtengo el monto por unidad, o sea, se toma el monto parámetro y se
	 * divide por la cantidad ingresada
	 * 
	 * @param amt
	 *            monto a dividir
	 * @return monto por unidad
	 */
    public BigDecimal getUnityAmt(BigDecimal amt){
		return amt.divide(getQtyEntered(), amt.scale(),	BigDecimal.ROUND_HALF_EVEN);
    }

	/**
	 * Obtengo el monto de impuesto para un importe parámetro, verificando si
	 * ese importe tiene impuesto incluído o no. Se determina el importe base y
	 * se retorna el monto del impuesto configurado en la línea, a su vez se
	 * determina si el impuesto está incluído en el precio a partir de la tarifa
	 * de la cabecera de la factura.
	 * 
	 * @param amt
	 *            importe con o sin impuestos
	 * @return monto de impuesto a partir del monto parámetro, determinando su
	 *         importe base
	 */
    public BigDecimal getTaxAmt(BigDecimal amt){
    	return MTax.calculateTax(amt, isTaxIncluded(), getTaxRate(), amt.scale());
    }

	/**
	 * Extraigo o agrego el monto de impuesto parámetro al importe parámetro,
	 * dependiento si la tasa está incluída en el precio y si se debe obtener el
	 * precio con impuesto o no.
	 * 
	 * @param amt
	 *            importe
	 * @param taxAmt
	 *            monto de impuesto
	 * @param taxIncluded
	 *            impuesto incluído en el precio
	 * @param withTax
	 *            true si se debe obtener el importe con impuestos, false si el
	 *            neto
	 * @return monto neto o con impuestos dependiendo del parámetro withTax
	 */
    public BigDecimal amtByTax(BigDecimal amt, BigDecimal taxAmt, boolean taxIncluded, boolean withTax){
		BigDecimal amtResult = amt;
		if(taxIncluded){
			if(!withTax){
				amtResult = amtResult.subtract(taxAmt);
			}
		}
		else{
			if(withTax){
				amtResult = amtResult.add(taxAmt);
			}
		}
		return amtResult;
	}
    
    
    //ADER soporte para caches multi-documnto
    private MProductCache m_prodCache;
    public void setProductCache(MProductCache c)
    {
    	m_prodCache = c;
    }
    //por ahroa privado; se usa por lo pronto para dar soporte a los metodos
    //que usan los reportes jasper
    private MProduct getProduct()
    {
    	int id = getM_Product_ID();
    	if (id <= 0)
    		return null;
    	//si se tiene cache multidocuemtntos, se usa esta
    	if (m_prodCache != null)
    		return m_prodCache.get(id);
    	
    	//si no se la mismo que se estaba usando hasta ahora.... (en realidad
    	//se podria usar MProduct.get.... pero bueno
    	
    	return new MProduct(p_ctx, id, null);
   }

	public void setSkipManualGeneralDiscount(boolean skipManualGeneralDiscount) {
		this.skipManualGeneralDiscount = skipManualGeneralDiscount;
	}

	public boolean isSkipManualGeneralDiscount() {
		return skipManualGeneralDiscount;
	}
    
}    // MInvoiceLine



/*
 *  @(#)MInvoiceLine.java   02.07.07
 * 
 *  Fin del fichero MInvoiceLine.java
 *  
 *  Versión 2.2
 *
 */

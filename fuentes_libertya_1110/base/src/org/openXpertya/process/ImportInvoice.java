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



package org.openXpertya.process;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.logging.Level;

import org.openXpertya.model.MBPartner;
import org.openXpertya.model.MBPartnerLocation;
import org.openXpertya.model.MInvoice;
import org.openXpertya.model.MInvoiceLine;
import org.openXpertya.model.MLocation;
import org.openXpertya.model.MUser;
import org.openXpertya.model.X_I_Invoice;
import org.openXpertya.util.DB;
import org.openXpertya.util.Env;

/**
 * Descripción de Clase
 *
 *
 * @version    2.2, 12.10.07
 * @author     Equipo de Desarrollo de openXpertya    
 */

public class ImportInvoice extends SvrProcess {

    /** Descripción de Campos */

    private int m_AD_Client_ID = 0;

    /** Descripción de Campos */

    private int m_AD_Org_ID = 0;

    /** Descripción de Campos */

    private boolean m_deleteOldImported = false;

    /** Descripción de Campos */

    private String m_docAction = MInvoice.DOCACTION_Prepare;

    /** Descripción de Campos */

    private Timestamp m_DateValue = null;

    /**
     * Descripción de Método
     *
     */

    protected void prepare() {
        ProcessInfoParameter[] para = getParameter();

        for( int i = 0;i < para.length;i++ ) {
            String name = para[ i ].getParameterName();

            if( name.equals( "AD_Client_ID" )) {
                m_AD_Client_ID = (( BigDecimal )para[ i ].getParameter()).intValue();
            } else if( name.equals( "AD_Org_ID" )) {
                m_AD_Org_ID = (( BigDecimal )para[ i ].getParameter()).intValue();
            } else if( name.equals( "DeleteOldImported" )) {
                m_deleteOldImported = "Y".equals( para[ i ].getParameter());
            } else if( name.equals( "DocAction" )) {
                m_docAction = ( String )para[ i ].getParameter();
            } else {
                log.log( Level.SEVERE,"prepare - Unknown Parameter: " + name );
            }
        }

        if( m_DateValue == null ) {
            m_DateValue = new Timestamp( System.currentTimeMillis());
        }
    }    // prepare

    /**
     * Descripción de Método
     *
     *
     * @return
     *
     * @throws java.lang.Exception
     */

    protected String doIt() throws java.lang.Exception {
        StringBuffer sql         = null;
        int          no          = 0;
        String       clientCheck = " AND AD_Client_ID=" + m_AD_Client_ID;

        // ****    Prepare ****

        // Delete Old Imported

        if( m_deleteOldImported ) {
            sql = new StringBuffer( "DELETE I_Invoice " + "WHERE I_IsImported='Y'" ).append( clientCheck );
            no = DB.executeUpdate( sql.toString());
            log.fine( "doIt - Delete Old Impored =" + no );
        }

        // Set Client, Org, IsActive, Created/Updated

        sql = new StringBuffer( "UPDATE I_Invoice " + "SET AD_Client_ID = COALESCE (AD_Client_ID," ).append( m_AD_Client_ID ).append( ")," + " AD_Org_ID = COALESCE (AD_Org_ID," ).append( m_AD_Org_ID ).append( ")," + " IsActive = COALESCE (IsActive, 'Y')," + " Created = COALESCE (Created, SysDate)," + " CreatedBy = COALESCE (CreatedBy, 0)," + " Updated = COALESCE (Updated, SysDate)," + " UpdatedBy = COALESCE (UpdatedBy, 0)," + " I_ErrorMsg = NULL," + " I_IsImported = 'N' " + "WHERE I_IsImported<>'Y' OR I_IsImported IS NULL" );
        no = DB.executeUpdate( sql.toString());
        log.info( "doIt - Reset=" + no );
        sql = new StringBuffer( "UPDATE I_Invoice o " + "SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Org, '" + "WHERE (AD_Org_ID IS NULL OR AD_Org_ID=0" + " OR EXISTS (SELECT * FROM AD_Org oo WHERE o.AD_Org_ID=oo.AD_Org_ID AND (oo.IsSummary='Y' OR oo.IsActive='N')))" + " AND I_IsImported<>'Y'" ).append( clientCheck );
        no = DB.executeUpdate( sql.toString());

        if( no != 0 ) {
            log.warning( "doIt - Invalid Org=" + no );
        }

        // Document Type - PO - SO

        sql = new StringBuffer( "UPDATE I_Invoice o " + "SET C_DocType_ID=(SELECT C_DocType_ID FROM C_DocType d WHERE d.Name=o.DocTypeName" + " AND d.DocBaseType='API' AND o.AD_Client_ID=d.AD_Client_ID) " + "WHERE C_DocType_ID IS NULL AND IsSOTrx='N' AND DocTypeName IS NOT NULL AND I_IsImported<>'Y'" ).append( clientCheck );
        no = DB.executeUpdate( sql.toString());
        log.fine( "doIt - Set PO DocType=" + no );
        sql = new StringBuffer( "UPDATE I_Invoice o " + "SET C_DocType_ID=(SELECT C_DocType_ID FROM C_DocType d WHERE d.Name=o.DocTypeName" + " AND d.DocBaseType='ARI' AND o.AD_Client_ID=d.AD_Client_ID) " + "WHERE C_DocType_ID IS NULL AND IsSOTrx='Y' AND DocTypeName IS NOT NULL AND I_IsImported<>'Y'" ).append( clientCheck );
        no = DB.executeUpdate( sql.toString());
        log.fine( "doIt - Set SO DocType=" + no );
        sql = new StringBuffer( "UPDATE I_Invoice o " + "SET C_DocType_ID=(SELECT C_DocType_ID FROM C_DocType d WHERE d.Name=o.DocTypeName" + " AND d.DocBaseType IN ('API','ARI') AND o.AD_Client_ID=d.AD_Client_ID) "

        // + "WHERE C_DocType_ID IS NULL AND IsSOTrx IS NULL AND DocTypeName IS NOT NULL AND I_IsImported<>'Y'").append (clientCheck);

        + "WHERE C_DocType_ID IS NULL AND DocTypeName IS NOT NULL AND I_IsImported<>'Y'" ).append( clientCheck );
        no = DB.executeUpdate( sql.toString());
        log.fine( "doIt - Set DocType=" + no );
        sql = new StringBuffer( "UPDATE I_Invoice " + "SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid DocTypeName, ' " + "WHERE C_DocType_ID IS NULL AND DocTypeName IS NOT NULL" + " AND I_IsImported<>'Y'" ).append( clientCheck );
        no = DB.executeUpdate( sql.toString());

        if( no != 0 ) {
            log.warning( "doIt - Invalid DocTypeName=" + no );
        }

        // DocType Default

        sql = new StringBuffer( "UPDATE I_Invoice o " + "SET C_DocType_ID=(SELECT C_DocType_ID FROM C_DocType d WHERE d.IsDefault='Y'" + " AND d.DocBaseType='API' AND ROWNUM=1 AND o.AD_Client_ID=d.AD_Client_ID) " + "WHERE C_DocType_ID IS NULL AND IsSOTrx='N' AND I_IsImported<>'Y'" ).append( clientCheck );
        no = DB.executeUpdate( sql.toString());
        log.fine( "doIt - Set PO Default DocType=" + no );
        sql = new StringBuffer( "UPDATE I_Invoice o " + "SET C_DocType_ID=(SELECT C_DocType_ID FROM C_DocType d WHERE d.IsDefault='Y'" + " AND d.DocBaseType='ARI' AND ROWNUM=1 AND o.AD_Client_ID=d.AD_Client_ID) " + "WHERE C_DocType_ID IS NULL AND IsSOTrx='Y' AND I_IsImported<>'Y'" ).append( clientCheck );
        no = DB.executeUpdate( sql.toString());
        log.fine( "doIt - Set SO Default DocType=" + no );
        sql = new StringBuffer( "UPDATE I_Invoice o " + "SET C_DocType_ID=(SELECT C_DocType_ID FROM C_DocType d WHERE d.IsDefault='Y'" + " AND d.DocBaseType IN('ARI','API') AND ROWNUM=1 AND o.AD_Client_ID=d.AD_Client_ID) " + "WHERE C_DocType_ID IS NULL AND IsSOTrx IS NULL AND I_IsImported<>'Y'" ).append( clientCheck );
        no = DB.executeUpdate( sql.toString());
        log.fine( "doIt - Set Default DocType=" + no );
        sql = new StringBuffer( "UPDATE I_Invoice " + "SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=No DocType, ' " + "WHERE C_DocType_ID IS NULL" + " AND I_IsImported<>'Y'" ).append( clientCheck );
        no = DB.executeUpdate( sql.toString());

        if( no != 0 ) {
            log.warning( "doIt - No DocType=" + no );
        }

        // Set IsSOTrx

        sql = new StringBuffer( "UPDATE I_Invoice o SET IsSOTrx='Y' " + "WHERE EXISTS (SELECT * FROM C_DocType d WHERE o.C_DocType_ID=d.C_DocType_ID AND d.DocBaseType='ARI' AND o.AD_Client_ID=d.AD_Client_ID)" + " AND C_DocType_ID IS NOT NULL" + " AND I_IsImported<>'Y'" ).append( clientCheck );
        no = DB.executeUpdate( sql.toString());
        log.fine( "doIt - Set IsSOTrx=Y=" + no );
        sql = new StringBuffer( "UPDATE I_Invoice o SET IsSOTrx='N' " + "WHERE EXISTS (SELECT * FROM C_DocType d WHERE o.C_DocType_ID=d.C_DocType_ID AND d.DocBaseType='API' AND o.AD_Client_ID=d.AD_Client_ID)" + " AND C_DocType_ID IS NOT NULL" + " AND I_IsImported<>'Y'" ).append( clientCheck );
        no = DB.executeUpdate( sql.toString());
        log.fine( "doIt - Set IsSOTrx=N=" + no );

        // Price List

        sql = new StringBuffer( "UPDATE I_Invoice o " + "SET M_PriceList_ID=(SELECT M_PriceList_ID FROM M_PriceList p WHERE p.IsDefault='Y'" + " AND p.C_Currency_ID=o.C_Currency_ID AND p.IsSOPriceList=o.IsSOTrx AND ROWNUM=1 AND o.AD_Client_ID=p.AD_Client_ID) " + "WHERE M_PriceList_ID IS NULL AND I_IsImported<>'Y'" ).append( clientCheck );
        no = DB.executeUpdate( sql.toString());
        log.fine( "doIt - Set Default Currency PriceList=" + no );
        sql = new StringBuffer( "UPDATE I_Invoice o " + "SET M_PriceList_ID=(SELECT M_PriceList_ID FROM M_PriceList p WHERE p.IsDefault='Y'" + " AND p.IsSOPriceList=o.IsSOTrx AND ROWNUM=1 AND o.AD_Client_ID=p.AD_Client_ID) " + "WHERE M_PriceList_ID IS NULL AND C_Currency_ID IS NULL AND I_IsImported<>'Y'" ).append( clientCheck );
        no = DB.executeUpdate( sql.toString());
        log.fine( "doIt - Set Default PriceList=" + no );
        sql = new StringBuffer( "UPDATE I_Invoice o " + "SET M_PriceList_ID=(SELECT M_PriceList_ID FROM M_PriceList p " + " WHERE p.C_Currency_ID=o.C_Currency_ID AND p.IsSOPriceList=o.IsSOTrx AND ROWNUM=1 AND o.AD_Client_ID=p.AD_Client_ID) " + "WHERE M_PriceList_ID IS NULL AND I_IsImported<>'Y'" ).append( clientCheck );
        no = DB.executeUpdate( sql.toString());
        log.fine( "doIt - Set Currency PriceList=" + no );
        sql = new StringBuffer( "UPDATE I_Invoice o " + "SET M_PriceList_ID=(SELECT M_PriceList_ID FROM M_PriceList p " + " WHERE p.IsSOPriceList=o.IsSOTrx AND ROWNUM=1 AND o.AD_Client_ID=p.AD_Client_ID) " + "WHERE M_PriceList_ID IS NULL AND C_Currency_ID IS NULL AND I_IsImported<>'Y'" ).append( clientCheck );
        no = DB.executeUpdate( sql.toString());
        log.fine( "doIt - Set PriceList=" + no );

        //

        sql = new StringBuffer( "UPDATE I_Invoice " + "SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=No PriceList, ' " + "WHERE M_PriceList_ID IS NULL" + " AND I_IsImported<>'Y'" ).append( clientCheck );
        no = DB.executeUpdate( sql.toString());

        if( no != 0 ) {
            log.warning( "doIt - No PriceList=" + no );
        }

        // Payment Term

        sql = new StringBuffer( "UPDATE I_Invoice o " + "SET C_PaymentTerm_ID=(SELECT C_PaymentTerm_ID FROM C_PaymentTerm p" + " WHERE o.PaymentTermValue=p.Value AND o.AD_Client_ID=p.AD_Client_ID) " + "WHERE C_PaymentTerm_ID IS NULL AND PaymentTermValue IS NOT NULL AND I_IsImported<>'Y'" ).append( clientCheck );
        no = DB.executeUpdate( sql.toString());
        log.fine( "doIt - Set PaymentTerm=" + no );
        sql = new StringBuffer( "UPDATE I_Invoice o " + "SET C_PaymentTerm_ID=(SELECT C_PaymentTerm_ID FROM C_PaymentTerm p" + " WHERE p.IsDefault='Y' AND ROWNUM=1 AND o.AD_Client_ID=p.AD_Client_ID) " + "WHERE C_PaymentTerm_ID IS NULL AND o.PaymentTermValue IS NULL AND I_IsImported<>'Y'" ).append( clientCheck );
        no = DB.executeUpdate( sql.toString());
        log.fine( "doIt - Set Default PaymentTerm=" + no );

        //

        sql = new StringBuffer( "UPDATE I_Invoice " + "SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=No PaymentTerm, ' " + "WHERE C_PaymentTerm_ID IS NULL" + " AND I_IsImported<>'Y'" ).append( clientCheck );
        no = DB.executeUpdate( sql.toString());

        if( no != 0 ) {
            log.warning( "doIt - No PaymentTerm=" + no );
        }

        // BP from EMail

        sql = new StringBuffer( "UPDATE I_Invoice o " + "SET (C_BPartner_ID,AD_User_ID)=(SELECT C_BPartner_ID,AD_User_ID FROM AD_User u" + " WHERE o.EMail=u.EMail AND o.AD_Client_ID=u.AD_Client_ID AND u.C_BPartner_ID IS NOT NULL) " + "WHERE C_BPartner_ID IS NULL AND EMail IS NOT NULL" + " AND I_IsImported<>'Y'" ).append( clientCheck );
        no = DB.executeUpdate( sql.toString());
        log.fine( "doIt - Set BP from EMail=" + no );

        // BP from ContactName

        sql = new StringBuffer( "UPDATE I_Invoice o " + "SET (C_BPartner_ID,AD_User_ID)=(SELECT C_BPartner_ID,AD_User_ID FROM AD_User u" + " WHERE o.ContactName=u.Name AND o.AD_Client_ID=u.AD_Client_ID AND u.C_BPartner_ID IS NOT NULL) " + "WHERE C_BPartner_ID IS NULL AND ContactName IS NOT NULL" + " AND EXISTS (SELECT Name FROM AD_User u WHERE o.ContactName=u.Name AND o.AD_Client_ID=u.AD_Client_ID AND u.C_BPartner_ID IS NOT NULL GROUP BY Name HAVING COUNT(*)=1)" + " AND I_IsImported<>'Y'" ).append( clientCheck );
        no = DB.executeUpdate( sql.toString());
        log.fine( "doIt - Set BP from ContactName=" + no );

        // BP from Value

        sql = new StringBuffer( "UPDATE I_Invoice o " + "SET C_BPartner_ID=(SELECT C_BPartner_ID FROM C_BPartner bp" + " WHERE o.BPartnerValue=bp.Value AND o.AD_Client_ID=bp.AD_Client_ID AND ROWNUM=1) " + "WHERE C_BPartner_ID IS NULL AND BPartnerValue IS NOT NULL" + " AND I_IsImported<>'Y'" ).append( clientCheck );
        no = DB.executeUpdate( sql.toString());
        log.fine( "doIt - Set BP from Value=" + no );

        // Default BP

        sql = new StringBuffer( "UPDATE I_Invoice o " + "SET C_BPartner_ID=(SELECT C_BPartnerCashTrx_ID FROM AD_ClientInfo c" + " WHERE o.AD_Client_ID=c.AD_Client_ID) " + "WHERE C_BPartner_ID IS NULL AND BPartnerValue IS NULL AND Name IS NULL" + " AND I_IsImported<>'Y'" ).append( clientCheck );
        no = DB.executeUpdate( sql.toString());
        log.fine( "doIt - Set Default BP=" + no );

        // Existing Location ? Exact Match

        sql = new StringBuffer( "UPDATE I_Invoice o " + "SET C_BPartner_Location_ID=(SELECT C_BPartner_Location_ID" + " FROM C_BPartner_Location bpl INNER JOIN C_Location l ON (bpl.C_Location_ID=l.C_Location_ID)" + " WHERE o.C_BPartner_ID=bpl.C_BPartner_ID AND bpl.AD_Client_ID=o.AD_Client_ID" + " AND DUMP(o.Address1)=DUMP(l.Address1) AND DUMP(o.Address2)=DUMP(l.Address2)" + " AND DUMP(o.City)=DUMP(l.City) AND DUMP(o.Postal)=DUMP(l.Postal)" + " AND DUMP(o.C_Region_ID)=DUMP(l.C_Region_ID) AND DUMP(o.C_Country_ID)=DUMP(l.C_Country_ID)) " + "WHERE C_BPartner_ID IS NOT NULL AND C_BPartner_Location_ID IS NULL" + " AND I_IsImported='N'" ).append( clientCheck );
        no = DB.executeUpdate( sql.toString());
        log.fine( "doIt - Found Location=" + no );

        // Set Location from BPartner

        sql = new StringBuffer( "UPDATE I_Invoice o " + "SET C_BPartner_Location_ID=(SELECT C_BPartner_Location_ID FROM C_BPartner_Location l" + " WHERE l.C_BPartner_ID=o.C_BPartner_ID AND o.AD_Client_ID=l.AD_Client_ID" + " AND ((l.IsBillTo='Y' AND o.IsSOTrx='Y') OR o.IsSOTrx='N')" + " AND ROWNUM=1) " + "WHERE C_BPartner_ID IS NOT NULL AND C_BPartner_Location_ID IS NULL" + " AND I_IsImported<>'Y'" ).append( clientCheck );
        no = DB.executeUpdate( sql.toString());
        log.fine( "doIt - Set BP Location from BP=" + no );

        //

        sql = new StringBuffer( "UPDATE I_Invoice " + "SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=No BP Location, ' " + "WHERE C_BPartner_ID IS NOT NULL AND C_BPartner_Location_ID IS NULL" + " AND I_IsImported<>'Y'" ).append( clientCheck );
        no = DB.executeUpdate( sql.toString());

        if( no != 0 ) {
            log.warning( "doIt - No BP Location=" + no );
        }

        // Set Country

        sql = new StringBuffer( "UPDATE I_Invoice o " + "SET CountryCode=(SELECT CountryCode FROM C_Country c WHERE c.IsDefault='Y'" + " AND c.AD_Client_ID IN (0, o.AD_Client_ID) AND ROWNUM=1) " + "WHERE C_BPartner_ID IS NULL AND CountryCode IS NULL AND C_Country_ID IS NULL" + " AND I_IsImported<>'Y'" ).append( clientCheck );
        no = DB.executeUpdate( sql.toString());
        log.fine( "doIt - Set Country Default=" + no );
        sql = new StringBuffer( "UPDATE I_Invoice o " + "SET C_Country_ID=(SELECT C_Country_ID FROM C_Country c" + " WHERE o.CountryCode=c.CountryCode AND c.AD_Client_ID IN (0, o.AD_Client_ID)) " + "WHERE C_BPartner_ID IS NULL AND C_Country_ID IS NULL AND CountryCode IS NOT NULL" + " AND I_IsImported<>'Y'" ).append( clientCheck );
        no = DB.executeUpdate( sql.toString());
        log.fine( "doIt - Set Country=" + no );

        //

        sql = new StringBuffer( "UPDATE I_Invoice " + "SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Country, ' " + "WHERE C_BPartner_ID IS NULL AND C_Country_ID IS NULL" + " AND I_IsImported<>'Y'" ).append( clientCheck );
        no = DB.executeUpdate( sql.toString());

        if( no != 0 ) {
            log.warning( "doIt - Invalid Country=" + no );
        }

        // Set Region

        sql = new StringBuffer( "UPDATE I_Invoice o " + "Set RegionName=(SELECT Name FROM C_Region r" + " WHERE r.IsDefault='Y' AND r.C_Country_ID=o.C_Country_ID" + " AND r.AD_Client_ID IN (0, o.AD_Client_ID) AND ROWNUM=1) " + "WHERE C_BPartner_ID IS NULL AND C_Region_ID IS NULL AND RegionName IS NULL" + " AND I_IsImported<>'Y'" ).append( clientCheck );
        no = DB.executeUpdate( sql.toString());
        log.fine( "doIt - Set Region Default=" + no );

        //

        sql = new StringBuffer( "UPDATE I_Invoice o " + "Set C_Region_ID=(SELECT C_Region_ID FROM C_Region r" + " WHERE r.Name=o.RegionName AND r.C_Country_ID=o.C_Country_ID" + " AND r.AD_Client_ID IN (0, o.AD_Client_ID)) " + "WHERE C_BPartner_ID IS NULL AND C_Region_ID IS NULL AND RegionName IS NOT NULL" + " AND I_IsImported<>'Y'" ).append( clientCheck );
        no = DB.executeUpdate( sql.toString());
        log.fine( "doIt - Set Region=" + no );

        //

        sql = new StringBuffer( "UPDATE I_Invoice o " + "SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Region, ' " + "WHERE C_BPartner_ID IS NULL AND C_Region_ID IS NULL " + " AND EXISTS (SELECT * FROM C_Country c" + " WHERE c.C_Country_ID=o.C_Country_ID AND c.HasRegion='Y')" + " AND I_IsImported<>'Y'" ).append( clientCheck );
        no = DB.executeUpdate( sql.toString());

        if( no != 0 ) {
            log.warning( "doIt - Invalid Region=" + no );
        }

        // Product

        sql = new StringBuffer( "UPDATE I_Invoice o " + "SET M_Product_ID=(SELECT M_Product_ID FROM M_Product p" + " WHERE o.ProductValue=p.Value AND o.AD_Client_ID=p.AD_Client_ID AND ROWNUM=1) " + "WHERE M_Product_ID IS NULL AND ProductValue IS NOT NULL" + " AND I_IsImported<>'Y'" ).append( clientCheck );
        no = DB.executeUpdate( sql.toString());
        log.fine( "doIt - Set Product from Value=" + no );
        sql = new StringBuffer( "UPDATE I_Invoice o " + "SET M_Product_ID=(SELECT M_Product_ID FROM M_Product p" + " WHERE o.UPC=p.UPC AND o.AD_Client_ID=p.AD_Client_ID AND ROWNUM=1) " + "WHERE M_Product_ID IS NULL AND UPC IS NOT NULL" + " AND I_IsImported<>'Y'" ).append( clientCheck );
        no = DB.executeUpdate( sql.toString());
        log.fine( "doIt - Set Product from UPC=" + no );
        sql = new StringBuffer( "UPDATE I_Invoice o " + "SET M_Product_ID=(SELECT M_Product_ID FROM M_Product p" + " WHERE o.SKU=p.SKU AND o.AD_Client_ID=p.AD_Client_ID AND ROWNUM=1) " + "WHERE M_Product_ID IS NULL AND SKU IS NOT NULL" + " AND I_IsImported<>'Y'" ).append( clientCheck );
        no = DB.executeUpdate( sql.toString());
        log.fine( "doIt - Set Product fom SKU=" + no );
        sql = new StringBuffer( "UPDATE I_Invoice " + "SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Product, ' " + "WHERE M_Product_ID IS NULL AND (ProductValue IS NOT NULL OR UPC IS NOT NULL OR SKU IS NOT NULL)" + " AND I_IsImported<>'Y'" ).append( clientCheck );
        no = DB.executeUpdate( sql.toString());

        if( no != 0 ) {
            log.warning( "doIt - Invalid Product=" + no );
        }

        // Tax

        sql = new StringBuffer( "UPDATE I_Invoice o " + "SET C_Tax_ID=(SELECT C_Tax_ID FROM C_Tax t" + " WHERE o.TaxIndicator=t.TaxIndicator AND o.AD_Client_ID=t.AD_Client_ID AND ROWNUM=1) " + "WHERE C_Tax_ID IS NULL AND TaxIndicator IS NOT NULL" + " AND I_IsImported<>'Y'" ).append( clientCheck );
        no = DB.executeUpdate( sql.toString());
        log.fine( "doIt - Set Tax=" + no );
        sql = new StringBuffer( "UPDATE I_Invoice " + "SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid Tax, ' " + "WHERE C_Tax_ID IS NULL AND TaxIndicator IS NOT NULL" + " AND I_IsImported<>'Y'" ).append( clientCheck );
        no = DB.executeUpdate( sql.toString());

        if( no != 0 ) {
            log.warning( "doIt - Invalid Tax=" + no );
        }

        // -- New BPartner ---------------------------------------------------

        // Go through Invoice Records w/o C_BPartner_ID

        sql = new StringBuffer( "SELECT * FROM I_Invoice " + "WHERE I_IsImported='N' AND C_BPartner_ID IS NULL" ).append( clientCheck );

        try {
            PreparedStatement pstmt = DB.prepareStatement( sql.toString());
            ResultSet         rs    = pstmt.executeQuery();

            while( rs.next()) {
                X_I_Invoice imp = new X_I_Invoice( getCtx(),rs,null );

                if( imp.getBPartnerValue() == null ) {
                    if( imp.getEMail() != null ) {
                        imp.setBPartnerValue( imp.getEMail());
                    } else if( imp.getName() != null ) {
                        imp.setBPartnerValue( imp.getName());
                    } else {
                        continue;
                    }
                }

                if( imp.getName() == null ) {
                    if( imp.getContactName() != null ) {
                        imp.setName( imp.getContactName());
                    } else {
                        imp.setName( imp.getBPartnerValue());
                    }
                }

                // BPartner

                MBPartner bp = MBPartner.get( getCtx(),imp.getBPartnerValue());

                if( bp == null ) {
                    bp = new MBPartner( getCtx(),-1,null );
                    bp.setClientOrg( imp.getAD_Client_ID(),imp.getAD_Org_ID());
                    bp.setValue( imp.getBPartnerValue());
                    bp.setName( imp.getName());

                    if( !bp.save()) {
                        continue;
                    }
                }

                imp.setC_BPartner_ID( bp.getC_BPartner_ID());

                // BP Location

                MBPartnerLocation   bpl  = null;
                MBPartnerLocation[] bpls = bp.getLocations( true );

                for( int i = 0;(bpl == null) && (i < bpls.length);i++ ) {
                    if( imp.getC_BPartner_Location_ID() == bpls[ i ].getC_BPartner_Location_ID()) {
                        bpl = bpls[ i ];

                        // Same Location ID

                    } else if( imp.getC_Location_ID() == bpls[ i ].getC_Location_ID()) {
                        bpl = bpls[ i ];

                        // Same Location Info

                    } else if( imp.getC_Location_ID() == 0 ) {
                        MLocation loc = bpl.getLocation( false );

                        if( loc.equals( imp.getC_Country_ID(),imp.getC_Region_ID(),imp.getPostal(),"",imp.getCity(),imp.getAddress1(),imp.getAddress2())) {
                            bpl = bpls[ i ];
                        }
                    }
                }

                if( bpl == null ) {

                    // New Location

                    MLocation loc = new MLocation( getCtx(),0,null );

                    loc.setAddress1( imp.getAddress1());
                    loc.setAddress2( imp.getAddress2());
                    loc.setCity( imp.getCity());
                    loc.setPostal( imp.getPostal());

                    if( imp.getC_Region_ID() != 0 ) {
                        loc.setC_Region_ID( imp.getC_Region_ID());
                    }

                    loc.setC_Country_ID( imp.getC_Country_ID());

                    if( !loc.save()) {
                        continue;
                    }

                    //

                    bpl = new MBPartnerLocation( bp );
                    bpl.setC_Location_ID( imp.getC_Location_ID());

                    if( !bpl.save()) {
                        continue;
                    }
                }

                imp.setC_Location_ID( bpl.getC_Location_ID());
                imp.setC_BPartner_Location_ID( bpl.getC_BPartner_Location_ID());

                // User/Contact

                if( (imp.getContactName() != null) || (imp.getEMail() != null) || (imp.getPhone() != null) ) {
                    MUser[] users = bp.getContacts( true );
                    MUser   user  = null;

                    for( int i = 0;(user == null) && (i < users.length);i++ ) {
                        String name = users[ i ].getName();

                        if( name.equals( imp.getContactName()) || name.equals( imp.getName())) {
                            user = users[ i ];
                            imp.setAD_User_ID( user.getAD_User_ID());
                        }
                    }

                    if( user == null ) {
                        user = new MUser( bp );

                        if( imp.getContactName() == null ) {
                            user.setName( imp.getName());
                        } else {
                            user.setName( imp.getContactName());
                        }

                        user.setEMail( imp.getEMail());
                        user.setPhone( imp.getPhone());

                        if( user.save()) {
                            imp.setAD_User_ID( user.getAD_User_ID());
                        }
                    }
                }

                imp.save();
            }    // for all new BPartners

            rs.close();
            pstmt.close();

            //

        } catch( SQLException e ) {
            log.log( Level.SEVERE,"doIt - CreateBP",e );
        }

        sql = new StringBuffer( "UPDATE I_Invoice " + "SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=No BPartner, ' " + "WHERE C_BPartner_ID IS NULL" + " AND I_IsImported<>'Y'" ).append( clientCheck );
        no = DB.executeUpdate( sql.toString());

        if( no != 0 ) {
            log.warning( "doIt - No BPartner=" + no );
        }

        // -- New Invoices -----------------------------------------------------

        int noInsert     = 0;
        int noInsertLine = 0;

        // Go through Invoice Records w/o

        sql = new StringBuffer( "SELECT * FROM I_Invoice " + "WHERE I_IsImported='N'" ).append( clientCheck ).append( " ORDER BY C_BPartner_ID, C_BPartner_Location_ID, I_Invoice_ID" );

        try {
            PreparedStatement pstmt = DB.prepareStatement( sql.toString());
            ResultSet         rs    = pstmt.executeQuery();

            // Group Change

            int    oldC_BPartner_ID          = 0;
            int    oldC_BPartner_Location_ID = 0;
            String oldDocumentNo             = "";

            //

            MInvoice invoice = null;
            int      lineNo  = 0;

            while( rs.next()) {
                X_I_Invoice imp           = new X_I_Invoice( getCtx(),rs,null );
                String      cmpDocumentNo = imp.getDocumentNo();

                if( cmpDocumentNo == null ) {
                    cmpDocumentNo = "";
                }

                // New Invoice

                if( (oldC_BPartner_ID != imp.getC_BPartner_ID()) || (oldC_BPartner_Location_ID != imp.getC_BPartner_Location_ID()) ||!oldDocumentNo.equals( cmpDocumentNo )) {
                    if( invoice != null ) {
                        invoice.processIt( m_docAction );
                        invoice.save();
                    }

                    // Group Change

                    oldC_BPartner_ID          = imp.getC_BPartner_ID();
                    oldC_BPartner_Location_ID = imp.getC_BPartner_Location_ID();
                    oldDocumentNo = imp.getDocumentNo();

                    if( oldDocumentNo == null ) {
                        oldDocumentNo = "";
                    }

                    //

                    invoice = new MInvoice( getCtx(),0,null );
                    invoice.setClientOrg( imp.getAD_Client_ID(),imp.getAD_Org_ID());
                    invoice.setC_DocTypeTarget_ID( imp.getC_DocType_ID());
                    invoice.setIsSOTrx( imp.isSOTrx());

                    if( imp.getDocumentNo() != null ) {
                        invoice.setDocumentNo( imp.getDocumentNo());
                    }

                    //

                    invoice.setC_BPartner_ID( imp.getC_BPartner_ID());
                    invoice.setC_BPartner_Location_ID( imp.getC_BPartner_Location_ID());

                    if( imp.getAD_User_ID() != 0 ) {
                        invoice.setAD_User_ID( imp.getAD_User_ID());
                    }

                    //

                    if( imp.getDescription() != null ) {
                        invoice.setDescription( imp.getDescription());
                    }

                    invoice.setC_PaymentTerm_ID( imp.getC_PaymentTerm_ID());
                    invoice.setM_PriceList_ID( imp.getM_PriceList_ID());

                    // SalesRep from Import or the person running the import

                    if( imp.getSalesRep_ID() != 0 ) {
                        invoice.setSalesRep_ID( imp.getSalesRep_ID());
                    }

                    if( invoice.getSalesRep_ID() == 0 ) {
                        invoice.setSalesRep_ID( getAD_User_ID());
                    }

                    //

                    if( imp.getAD_OrgTrx_ID() != 0 ) {
                        invoice.setAD_OrgTrx_ID( imp.getAD_OrgTrx_ID());
                    }

                    if( imp.getC_Activity_ID() != 0 ) {
                        invoice.setC_Activity_ID( imp.getC_Activity_ID());
                    }

                    if( imp.getC_Campaign_ID() != 0 ) {
                        invoice.setC_Campaign_ID( imp.getC_Campaign_ID());
                    }

                    if( imp.getC_Project_ID() != 0 ) {
                        invoice.setC_Project_ID( imp.getC_Project_ID());
                    }

                    //

                    if( imp.getDateInvoiced() != null ) {
                        invoice.setDateInvoiced( imp.getDateInvoiced());
                    }

                    if( imp.getDateAcct() != null ) {
                        invoice.setDateAcct( imp.getDateAcct());
                    }

                    //

                    invoice.save();
                    noInsert++;
                    lineNo = 10;
                }

                imp.setC_Invoice_ID( invoice.getC_Invoice_ID());

                // New InvoiceLine

                MInvoiceLine line = new MInvoiceLine( invoice );

                if( imp.getLineDescription() != null ) {
                    line.setDescription( imp.getLineDescription());
                }

                line.setLine( lineNo );
                lineNo += 10;

                if( imp.getM_Product_ID() != 0 ) {
                    line.setM_Product_ID( imp.getM_Product_ID(),true );
                }

                line.setQty( imp.getQtyOrdered());
                line.setPrice();

                BigDecimal price = imp.getPriceActual();

                if( (price != null) && (Env.ZERO.compareTo( price ) != 0) ) {
                    line.setPrice( price );
                }

                if( imp.getC_Tax_ID() != 0 ) {
                    line.setC_Tax_ID( imp.getC_Tax_ID());
                } else {
                    line.setTax();
                    imp.setC_Tax_ID( line.getC_Tax_ID());
                }

                BigDecimal taxAmt = imp.getTaxAmt();

                if( (taxAmt != null) && (Env.ZERO.compareTo( taxAmt ) != 0) ) {
                    line.setTaxAmt( taxAmt );
                }

                line.save();

                //

                imp.setC_InvoiceLine_ID( line.getC_InvoiceLine_ID());
                imp.setI_IsImported( true );
                imp.setProcessed( true );

                //

                if( imp.save()) {
                    noInsertLine++;
                }
            }

            if( invoice != null ) {
                invoice.processIt( m_docAction );
                invoice.save();
            }

            rs.close();
            pstmt.close();
        } catch( Exception e ) {
            log.log( Level.SEVERE,"doIt - CreateInvoice",e );
        }

        // Set Error to indicator to not imported

        sql = new StringBuffer( "UPDATE I_Invoice " + "SET I_IsImported='N', Updated=SysDate " + "WHERE I_IsImported<>'Y'" ).append( clientCheck );
        no = DB.executeUpdate( sql.toString());
        addLog( 0,null,new BigDecimal( no ),"@Errors@" );

        //

        addLog( 0,null,new BigDecimal( noInsert ),"@C_Invoice_ID@: @Inserted@" );
        addLog( 0,null,new BigDecimal( noInsertLine ),"@C_InvoiceLine_ID@: @Inserted@" );

        return "";
    }    // doIt
}    // ImportInvoice



/*
 *  @(#)ImportInvoice.java   02.07.07
 * 
 *  Fin del fichero ImportInvoice.java
 *  
 *  Versión 2.2
 *
 */

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.altaik.priceoffers;

import com.altaik.bo.po.CertificateStoreDescription;
import com.altaik.bo.po.CertificateStorage;
import com.altaik.bo.po.CompanySettings;
import com.altaik.bo.po.OfferedPurchase;
//import com.abs.keystore.pkcs12.PKCS12KeyStoreUtil;
import com.altaik.parser.gos.GosAuthCrawler;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * @author Aset
 */
public class GosRobot {
    private static final Logger logger = Logger.getLogger(GosRobot.class.getName());

    File fileForSign;
    File keyStoreForSign;
    boolean isSignDownloadedFile = true;
    CertificateStorage certStore;
    String loginPassword;
    
    private GosAuthCrawler gosCrawler;
    
    public boolean isLabor = true;
    public boolean isTechSpecChoose = false;
    public String[] prices;
    public int timeout = 120000;
    public int nLots = 1000000;
    
    //for multiple singing
    public String[][] muprices;
    
    
    public GosRobot(){
        this.certStore = new CertificateStorage("D:\\aset\\ecp\\igor\\GOSTKZ_1c034a8e924bb7609d43b36076826f4a5a5c2a5b.p12", "123qaz", "D:\\aset\\ecp\\igor\\AUTH_RSA_bad9918830b3b5faf6945a4896a39eb4d40bb300.p12", "123qaz", "pkcs12", "D:\\aset\\ecp\\oebs.gos.jks", "3075341");
        this.loginPassword = "!Q@W3e4r5t6y";
        this.prices = new String[]{"1540000"}; 
        this.gosCrawler = new GosAuthCrawler();
        this.gosCrawler.setTimeout(timeout);
    }
    
    /**
     *Conctructor
     * @param certificateStorage
     * @param password - password of goszakup login
     * @param prices - lots prices
     */
    public GosRobot(CertificateStorage certificateStorage, String password, String[] prices){
        this.certStore =certificateStorage;
        this.loginPassword = password;
        this.prices = prices;
        //this.timeout = timeout;
        
        this.gosCrawler = new GosAuthCrawler();
        this.gosCrawler.setTimeout(timeout);
    }
    
    public void Atach3SignProcess(){
        HashMap<String,String> appletParams = GetAppletParamsAttach3();
//        Signer(gosCrawler.baseUrl, appletParams.get("downloadURL"), appletParams.get("uploadURL"), appletParams.get("fileAttributes"), appletParams.get("userName"));
        ReadyBtnClickAction();
    }
    
    
    public void MainSignProcess(){
        Properties certProps = new Properties();
        certProps.setProperty("keyStore", certStore.getAuthkey().Store);
        certProps.setProperty("keyStoreType", certStore.getAuthkey().getType());
        certProps.setProperty("keyStorePassword", certStore.getAuthkey().Password);
        certProps.setProperty("trustStore", certStore.getTrust().Store);
        certProps.setProperty("trustStorePassword", certStore.getTrust().Password);
        getGosCrawler().AuthInit(certProps);

        if(!getGosCrawler().authGosAuction(loginPassword)) return;
        Document purchaseHomeDoc = gosCrawler.getCurrentDoc();
        
        Elements elements = purchaseHomeDoc.select("span#OpenInvTable a[id*=\"NotPaused\"]");
        
        while(elements.isEmpty()){
            Elements checkEles = purchaseHomeDoc.select("span#welcomeText");
            for(Element checkEle: checkEles){
                String welcom = checkEle.text();
                logger.log(Level.INFO, welcom);
            }
            
            logger.log(Level.WARNING, "OpenInvTable empty. Try next ...");
            try {
                Thread.sleep(550);
            } catch (InterruptedException ex) {
                Logger.getLogger(GosRobot.class.getName()).log(Level.SEVERE, null, ex);
                break;
            }
            purchaseHomeDoc = gosCrawler.getDoc();
            elements = purchaseHomeDoc.select("span#OpenInvTable a[id*=\"NotPaused\"]");
        }
        
        //giving delay
            try {
                Thread.sleep(2500);
            } catch (InterruptedException ex) {
                Logger.getLogger(GosRobot.class.getName()).log(Level.SEVERE, null, ex);
            }
        
        
        for (Element purchaseElem : elements) {
            String title = purchaseElem.attr("title");
            if(!title.endsWith("-2")){
                logger.log(Level.WARNING, "Something wrong with Number of Purchase. Not equals -2.");
                continue;
            }
            
            String purchaseHref = purchaseElem.attr("href");
            if(purchaseHref.isEmpty()){
                logger.log(Level.WARNING,"Something wrong with getting purchase href.");
            }
            
            purchaseHref = getGosCrawler().fullOA_HTML + purchaseHref;
          
            getGosCrawler().setMethod(Connection.Method.GET);
            getGosCrawler().setUrl(purchaseHref);
            Document purchaseDoc = getGosCrawler().getDoc();
            
            Document offerCreatedDoc = gosCrawler.SubmitAction(GetOfferCreateData(purchaseDoc));
            
            Map<String, String> offerCreatedData;
            Document stringsDoc;
            Elements priceEntryElems;
            

            if (isTechSpecChoose) {
                stringsDoc = offerCreatedDoc;

                int nTechSpecs = stringsDoc.select("span#BidItemPricesTableVO a[id*=\":XXBestDocRN:\"]").size();

                for (int i = 0; i < nTechSpecs; i++) {
                    Elements techSpecElems = stringsDoc.select("span#BidItemPricesTableVO a[id$=\":XXBestDocRN:" + i + "\"]");
                    if (!techSpecElems.isEmpty()) {
                        Element techSpecElem = techSpecElems.get(0);

                        //gosCrawler.setData(gosCrawler.getFormData(offerCreatedDoc),true);
                        gosCrawler.setUrl(gosCrawler.fullOA_HTML + techSpecElem.attr("href"));
                        gosCrawler.setMethod(Connection.Method.GET);
                        Document techSpecDoc = gosCrawler.getDoc();

                        Map<String, String> data = gosCrawler.getFormData(techSpecDoc, "button#SaveButton");
                        data.put("Documents:selected", "0");
                        stringsDoc = gosCrawler.SubmitAction(data);
                    }
                }
                
                if(nTechSpecs==0){
                    stringsDoc = gosCrawler.SubmitAction(gosCrawler.getFormData(offerCreatedDoc, "a[accesskey=\".\"]"));
                    offerCreatedData = gosCrawler.getFormData(stringsDoc, "td[background=\"/OA_HTML/cabo/images/swan/subDimTabBg.gif\"] > a");
                } else {
                    offerCreatedData = gosCrawler.getFormData(stringsDoc, "td[background=\"/OA_HTML/cabo/images/swan/subDimTabBg.gif\"] > a");
                }
                
            } else {
                stringsDoc = gosCrawler.SubmitAction(gosCrawler.getFormData(offerCreatedDoc, "a[accesskey=\".\"]"));
                offerCreatedData = gosCrawler.getFormData(stringsDoc, "td[background=\"/OA_HTML/cabo/images/swan/subDimTabBg.gif\"] > a");
            }
            

            if (isLabor) {
                priceEntryElems = stringsDoc.select("span#BidItemPricesTableVO input[id*=\":LinePriceEntry:\"]");
                if(priceEntryElems == null || priceEntryElems.isEmpty()){
                    priceEntryElems = stringsDoc.select("span#BidItemPricesTableVO input[id*=\":BidCurrencyPriceEntry:\"]");
                }
            } else {
                priceEntryElems = stringsDoc.select("span#BidItemPricesTableVO input[id*=\":BidCurrencyPriceEntry:\"]");
            }

            for (int i = 0; i < priceEntryElems.size() && i < nLots; i++) {
                Element elem = priceEntryElems.get(i);
                offerCreatedData.put(elem.attr("name"), prices[i]);
            }


            //Document headingDoc = gosCrawler.SubmitAction(offerCreatedData);
            Document offerGenerateDoc = gosCrawler.SubmitAction(gosCrawler.getFormData(gosCrawler.SubmitAction(offerCreatedData), "span#RenderReportsRN > a#ShowCD10"));
            Document updatePencilDoc = gosCrawler.SubmitAction(gosCrawler.getFormData(offerGenerateDoc, "a[id*=\":UpdateYes:\"]"));
            
            //Crawler uploadCrawler = new Crawler();
            //uploadCrawler.setUrl(gosCrawler.baseUrl + updatePencilDoc.select("a#FileListRNEx:CHECKED:0").attr("href"));
            //uploadCrawler.setBodySize(20000);
            //uploadCrawler.setMethod(Connection.Method.GET);
            //uploadCrawler.getDoc();
            
            //Storage storage = new Storage("PriceOffersFiles");
            //storage.LoadFile(updatePencilDoc.select("a#FileListRNEx:CHECKED:0").attr("title"), uploadCrawler.getResponse().bodyAsBytes());
            
            Document signAppletDoc = gosCrawler.SubmitAction(gosCrawler.getFormData(updatePencilDoc,"a[id=\"FileListRNEx:SignItem:0\"]"));
            
            Map<String,String> appletParams = GetAppletParams(signAppletDoc);
//            Signer(gosCrawler.baseUrl, appletParams.get("downloadURL"), appletParams.get("uploadURL"), appletParams.get("fileAttributes"), appletParams.get("userName"));
            Map<String,String> signAppletData = gosCrawler.getFormData(signAppletDoc, "button#DoneBtn");
            signAppletData.put("newStatus", signAppletDoc.select("select#newStatus option[value^=\"SUBMITTED\"]").attr("value"));
            Document offerContinueDoc = gosCrawler.SubmitAction(signAppletData);
            
            Document offerFinishDoc = gosCrawler.SubmitAction(gosCrawler.getFormData(offerContinueDoc, "button#ContinueBtn"));
            Document offerLastFinishDoc = gosCrawler.SubmitAction(gosCrawler.getFormData(offerFinishDoc, "button#SubmitBtn"));

            offerCreatedData.clear();
            stringsDoc = null;
            priceEntryElems.clear();
            offerLastFinishDoc = null;
            offerFinishDoc = null;
            offerContinueDoc = null;
            signAppletData.clear();
            appletParams.clear();
            signAppletDoc = null;
            //storage = null;
            //uploadCrawler.close();
            updatePencilDoc = null;
            offerGenerateDoc = null;
            
            
            logger.log(Level.INFO, "FINISH.");
        }
        
        elements.clear();
        purchaseHomeDoc = null;
        gosCrawler.close();
    }
    
    
    public static void MonitorExecute() {
//        String commonEmails = "a.barakbayev@altatender.kz,i.miloshenko@altatender.kz,i.akhmetkaliev@altatender.kz,a.borambayeva@altatender.kz,a.nurbosynova@altatender.kz";
        String commonEmails = "islam.a@inbox.ru,asetbn@gmail.com";
//        String commonEmails = "a.barakbayev@altatender.kz,i.miloshenko@altatender.kz,i.akhmetkaliev@altatender.kz,a.borambayeva@altatender.kz,islam.a@inbox.ru,aset.b@rambler.ru,brobas@inbox.ru";
        ArrayList<CompanySettings> companies = new ArrayList();

//          ukstroy
//        CompanySettings ukstroy = new CompanySettings();
//        ukstroy.certStorage = new CertificateStorage("C:\\data\\ecp\\ukstroy\\GOSTKZ_738a4c1b6f267f293201d0f7a764315aab1f9402.p12", "123456", "C:\\data\\ecp\\ukstroy\\AUTH_RSA_0113941a597d03394533a967288f6048b85242c8.p12", "123456", "pkcs12", "C:\\data\\ecp\\ecp\\oebs.gos.jks", "3075341");
////        ukstroy.certStorage = new CertificateStorage("/home/administrator/app/priceoffersmonitor/ecp/ukstroy/GOSTKZ_738a4c1b6f267f293201d0f7a764315aab1f9402.p12", "123456", "/home/administrator/app/priceoffersmonitor/ecp/ukstroy/AUTH_RSA_0113941a597d03394533a967288f6048b85242c8.p12", "123456", "pkcs12", "/home/administrator/app/priceoffersmonitor/ecp/altaik/oebs.gos.jks", "3075341");
//        ukstroy.companyName = "UKstroy";
//        ukstroy.loginPassword = "456852manat";
//        ukstroy.emails = commonEmails;
//        ukstroy.purchases = new OfferedPurchase[1];
//        OfferedPurchase ukstroyPurchase2 = new OfferedPurchase();
//        ukstroyPurchase2.number = "833468";
//        ukstroy.purchases[0] = ukstroyPurchase2;
//        
//        companies.add(ukstroy);
//          hold
//        CompanySettings hold = new CompanySettings();
////        hold.certStorage = new CertificateStorage("C:\\data\\ecp\\hold\\GOSTKZ_87db32cdcea8ec77ab0d058ff618e7ecc53f3d2c.p12", "123456", "C:\\data\\ecp\\hold\\AUTH_RSA_9581951da44d4e3bb0cd883055c6d2d5ebf5a767.p12", "123456", "pkcs12", "C:\\data\\ecp\\ecp\\oebs.gos.jks", "3075341");
//        hold.certStorage = new CertificateStorage("/home/administrator/app/priceoffersmonitor/ecp/hold/GOSTKZ_87db32cdcea8ec77ab0d058ff618e7ecc53f3d2c.p12", "123456", "/home/administrator/app/priceoffersmonitor/ecp/hold/AUTH_RSA_9581951da44d4e3bb0cd883055c6d2d5ebf5a767.p12", "123456", "pkcs12", "/home/administrator/app/priceoffersmonitor/ecp/altaik/oebs.gos.jks", "3075341");
//        hold.companyName = "Hold";
//        hold.loginPassword = "19081978t";
//        hold.emails = commonEmails;
//        hold.purchases = new OfferedPurchase[2];
//        OfferedPurchase holdPurchase1 = new OfferedPurchase();
//        holdPurchase1.number = "834071";
//        hold.purchases[0] = holdPurchase1;
//        OfferedPurchase holdPurchase2 = new OfferedPurchase();
//        holdPurchase2.number = "834071-2.1";
//        hold.purchases[1] = holdPurchase2;
//        companies.add(hold);
        //kazygurt
//        CompanySettings kazygurt = new CompanySettings();
////        kazygurt.certStorage = new CertificateStorage("C:\\data\\ecp\\kazygurt\\GOSTKZ_50d74657fdbf39e23c4985dd175c06741818c224.p12", "123456", "C:\\data\\ecp\\kazygurt\\AUTH_RSA_7692fd12f8d0eef0e7ce832a409d93ad88a13eb2.p12", "123456", "pkcs12", "C:\\data\\ecp\\ecp\\oebs.gos.jks", "3075341");
//        kazygurt.certStorage = new CertificateStorage("/home/administrator/app/priceoffersmonitor/ecp/kazygurt/GOSTKZ_50d74657fdbf39e23c4985dd175c06741818c224.p12", "123456", "/home/administrator/app/priceoffersmonitor/ecp/kazygurt/AUTH_RSA_7692fd12f8d0eef0e7ce832a409d93ad88a13eb2.p12", "123456", "pkcs12", "/home/administrator/app/priceoffersmonitor/ecp/altaik/oebs.gos.jks", "3075341");
//        kazygurt.companyName = "Kazygurt";
//        kazygurt.loginPassword = "project1234";
//        kazygurt.emails = commonEmails;
//        kazygurt.purchases = new OfferedPurchase[1];
//        OfferedPurchase kazygurtPurchase = new OfferedPurchase();
//        kazygurtPurchase.number = "833610";
//        kazygurt.purchases[0] = kazygurtPurchase;
//        companies.add(kazygurt);
//          zvezda
//        CompanySettings zvezda = new CompanySettings();
////        zvezda.certStorage = new CertificateStorage("C:\\data\\ecp\\zvezda\\GOSTKZ_e949f842b04f9e92d7e74d216f3e5415016e147d.p12", "123456", "C:\\data\\ecp\\zvezda\\AUTH_RSA_c0ed5bdbd1269679022dfb0882bd9c1ca4c3bd86.p12", "123456", "pkcs12", "C:\\data\\ecp\\ecp\\oebs.gos.jks", "3075341");
//        zvezda.certStorage = new CertificateStorage("/home/administrator/app/priceoffersmonitor/ecp/zvezda/GOSTKZ_e949f842b04f9e92d7e74d216f3e5415016e147d.p12", "123456", "/home/administrator/app/priceoffersmonitor/ecp/zvezda/AUTH_RSA_c0ed5bdbd1269679022dfb0882bd9c1ca4c3bd86.p12", "123456", "pkcs12", "/home/administrator/app/priceoffersmonitor/ecp/altaik/oebs.gos.jks", "3075341");
//        zvezda.companyName = "Zvezda";
//        zvezda.loginPassword = "zvezda12345";
//        zvezda.emails = commonEmails;
//        zvezda.purchases = new OfferedPurchase[1];
//        OfferedPurchase zvezdaPurchase = new OfferedPurchase();
//        zvezdaPurchase.number = "828337,1";
//        zvezda.purchases[0] = zvezdaPurchase;
//        
//        companies.add(zvezda);
//          vehi
//        CompanySettings kaskad = new CompanySettings();
////        kaskad.certStorage = new CertificateStorage("C:\\data\\ecp\\kaskad\\GOSTKZ_41771e843ef14c15f35c27ea06b2e39709fbc35f.p12", "123456", "C:\\data\\ecp\\kaskad\\AUTH_RSA_2f010f12af3ef07ecadc4ac2bd44946389d44c46.p12", "123456", "pkcs12", "C:\\data\\ecp\\ecp\\oebs.gos.jks", "3075341");
//        kaskad.certStorage = new CertificateStorage("/home/administrator/app/priceoffersmonitor/ecp/kaskad/GOSTKZ_41771e843ef14c15f35c27ea06b2e39709fbc35f.p12", "123456", "/home/administrator/app/priceoffersmonitor/ecp/kaskad/AUTH_RSA_2f010f12af3ef07ecadc4ac2bd44946389d44c46.p12", "123456", "pkcs12", "/home/administrator/app/priceoffersmonitor/ecp/altaik/oebs.gos.jks", "3075341");
//        kaskad.companyName = "Vehi";
//        kaskad.loginPassword = "87019606065v";
//        kaskad.emails = commonEmails;
//        kaskad.purchases = new OfferedPurchase[1];
//        OfferedPurchase kaskadPurchase0 = new OfferedPurchase();
//        kaskadPurchase0.number = "828112";
//        kaskad.purchases[0] = kaskadPurchase0;
//        
//        companies.add(kaskad);
//          kazstroymontazh
//        CompanySettings kazstroymontazh = new CompanySettings();
////        kazstroymontazh.certStorage = new CertificateStorage("D:\\aset\\data\\ecp\\kazstroymontazh\\GOSTKZ_55b4c3b021c3bcc5cae240233fc2223013bb2d98.p12", "123456", "D:\\aset\\data\\ecp\\kazstroymontazh\\AUTH_RSA_c2f48e97c696233c3895d6b856c1d72b2d3a42b7.p12", "123456", "pkcs12", "D:\\aset\\data\\ecp\\ecp\\oebs.gos.jks", "3075341");
//        kazstroymontazh.certStorage = new CertificateStorage("/home/admin/app/priceoffersmonitor/ecp/kazstroymontazh/GOSTKZ_55b4c3b021c3bcc5cae240233fc2223013bb2d98.p12", "123456", "/home/admin/app/priceoffersmonitor/ecp/kazstroymontazh/AUTH_RSA_c2f48e97c696233c3895d6b856c1d72b2d3a42b7.p12", "123456", "pkcs12", "/home/admin/app/priceoffersmonitor/ecp/altaik/oebs.gos.jks", "3075341");
//        kazstroymontazh.companyName = "KazStroyMontazh";
//        kazstroymontazh.loginPassword = "baimadiev198610";
//        kazstroymontazh.emails = commonEmails;
//        kazstroymontazh.purchases = new OfferedPurchase[1];
//        OfferedPurchase kazstroymontazhPurchase0 = new OfferedPurchase();
//        kazstroymontazhPurchase0.number = "826460";
//        kazstroymontazh.purchases[0] = kazstroymontazhPurchase0;
//        
//        companies.add(kazstroymontazh);
//          akzhar
//        CompanySettings akzhar = new CompanySettings();
////        akzhar.certStorage = new CertificateStorage("D:\\aset\\data\\ecp\\akzhar\\GOSTKZ_444823fb9c5c13f2163a3506bb9c23dc14b3e0c7.p12", "123456", "D:\\aset\\data\\ecp\\akzhar\\AUTH_RSA_6720615efb90a8b546b3d5ccc5496d41f6d3dd77.p12", "123456", "pkcs12", "D:\\aset\\data\\ecp\\ecp\\oebs.gos.jks", "3075341");
//        akzhar.certStorage = new CertificateStorage("/home/admin/app/priceoffersmonitor/ecp/akzhar/GOSTKZ_444823fb9c5c13f2163a3506bb9c23dc14b3e0c7.p12", "123456", "/home/admin/app/priceoffersmonitor/ecp/akzhar/AUTH_RSA_6720615efb90a8b546b3d5ccc5496d41f6d3dd77.p12", "123456", "pkcs12", "/home/admin/app/priceoffersmonitor/ecp/altaik/oebs.gos.jks", "3075341");
//        akzhar.companyName = "Akzhar";
//        akzhar.loginPassword = "m87018948255";
//        akzhar.emails = commonEmails;
//        akzhar.purchases = new OfferedPurchase[1];
//        OfferedPurchase akzharPurchase1 = new OfferedPurchase();
//        akzharPurchase1.number = "828245";
//        akzhar.purchases[0] = akzharPurchase1;
//        
//        companies.add(akzhar);
        //servis nc
//        CompanySettings serviceNC = new CompanySettings();
////        serviceNC.certStorage = new CertificateStorage("/home/admin/app/priceoffersmonitor/ecp/serviceNC/GOSTKZ_7c230bf2194837b3ba754e06c37ab552bbc02883.p12", "123456", "/home/admin/app/priceoffersmonitor/ecp/serviceNC/AUTH_RSA_cc3b3cda5775c3adf15287a0bde70b390f48491b.p12", "123456", "pkcs12", "/home/admin/app/priceoffersmonitor/ecp/altaik/oebs.gos.jks", "3075341");
//        serviceNC.certStorage = new CertificateStorage("D:\\aset\\data\\ecp\\serviceNC\\GOSTKZ_7c230bf2194837b3ba754e06c37ab552bbc02883.p12", "123456", "D:\\aset\\data\\ecp\\serviceNC\\AUTH_RSA_cc3b3cda5775c3adf15287a0bde70b390f48491b.p12", "123456", "pkcs12", "D:\\aset\\data\\ecp\\ecp\\oebs.gos.jks", "3075341");
//        serviceNC.companyName = "Сервис NC";
//        serviceNC.loginPassword = "ns4428114";
//        serviceNC.emails = commonEmails;
//        
//        OfferedPurchase serviceNCpurchase1 = new OfferedPurchase();
//        serviceNCpurchase1.number = "827653";
//
//        serviceNC.purchases = new OfferedPurchase[1];
//        serviceNC.purchases[0] = serviceNCpurchase1;
//        
//        companies.add(serviceNC);
//          музтау
//        CompanySettings muztau = new CompanySettings();
//        muztau.certStorage = new CertificateStorage("D:\\aset\\data\\ecp\\mustay\\GOSTKZ_b07166b51d56ccc4171f337a0a28021995cfbbbe.p12", "123456", "D:\\aset\\data\\ecp\\mustay\\AUTH_RSA_e4c9b9621a3cb92f914e6c1a1f6ac243f6d2c71f.p12", "123456", "pkcs12", "D:\\aset\\data\\ecp\\ecp\\oebs.gos.jks", "3075341");
//        muztau.companyName = "Музтау";
//        muztau.loginPassword = "tehno2002";
//        muztau.emails = commonEmails;
//        muztau.purchases = new OfferedPurchase[1];
//        OfferedPurchase muztauPurchase = new OfferedPurchase();
//        muztauPurchase.number = "824262,1";
//        muztau.purchases[0] = muztauPurchase;
        //companies.add(muztau);
//          yuteks
        CompanySettings ertis = new CompanySettings();
//        ertis.certStorage = new CertificateStorage("C:\\data\\ecp\\ertis\\GOSTKZ_68d9835297bc02b90a11e6862e47d0053a63b3c2.p12", "123456", "C:\\data\\ecp\\ertis\\AUTH_RSA_ab06eb46d16bb9425dbbf8b95596a3770d7af92d.p12", "123456", "pkcs12", "C:\\data\\ecp\\ecp\\oebs.gos.jks", "3075341");
        ertis.certStorage = new CertificateStorage("/home/administrator/app/priceoffersmonitor/ecp/ertis/GOSTKZ_68d9835297bc02b90a11e6862e47d0053a63b3c2.p12", "123456", "/home/administrator/app/priceoffersmonitor/ecp/ertis/AUTH_RSA_ab06eb46d16bb9425dbbf8b95596a3770d7af92d.p12", "123456", "pkcs12", "/home/administrator/app/priceoffersmonitor/ecp/altaik/oebs.gos.jks", "3075341");
//        ertis.certStorage = new CertificateStorage("/home/admin/app/priceoffersmonitor/ecp/ertis/GOSTKZ_68d9835297bc02b90a11e6862e47d0053a63b3c2.p12", "123456", "/home/admin/app/priceoffersmonitor/ecp/ertis/AUTH_RSA_ab06eb46d16bb9425dbbf8b95596a3770d7af92d.p12", "123456", "pkcs12", "/home/administrator/app/priceoffersmonitor/ecp/altaik/oebs.gos.jks", "3075341");
        ertis.companyName = "YutEks";
        ertis.loginPassword = "123456789ca";
        ertis.emails = commonEmails;
        ertis.purchases = new OfferedPurchase[1];
        OfferedPurchase ertisPurchase = new OfferedPurchase();
        ertisPurchase.number = "834059";
        ertis.purchases[0] = ertisPurchase;
        companies.add(ertis);

//          kig
//        CompanySettings kig = new CompanySettings();
//        kig.certStorage = new CertificateStorage("/home/admin/app/priceoffersmonitor/ecp/kurylys/GOSTKZ_8e0bf3ceca7b18e4e5e040e491ba74f1339d6c66.p12", "123456", "/home/admin/app/priceoffersmonitor/ecp/kurylys/AUTH_RSA_9dd319b6ba2ae2f2059bf9b4de7c8aa2261f2d68.p12", "123456", "pkcs12", "/home/admin/app/priceoffersmonitor/ecp/altaik/oebs.gos.jks", "3075341");
////        kig.certStorage = new CertificateStorage("D:\\aset\\data\\ecp\\kurylys\\GOSTKZ_8e0bf3ceca7b18e4e5e040e491ba74f1339d6c66.p12", "123456", "D:\\aset\\data\\ecp\\kurylys\\AUTH_RSA_9dd319b6ba2ae2f2059bf9b4de7c8aa2261f2d68.p12", "123456", "pkcs12", "D:\\aset\\data\\ecp\\ecp\\oebs.gos.jks", "3075341");
//        kig.companyName = "Курылыс Инвест Групп";
//        kig.loginPassword = "kig123456789";
//        kig.emails = commonEmails;
//        kig.purchases = new OfferedPurchase[1];
//        OfferedPurchase kigPurchase = new OfferedPurchase();
//        kigPurchase.number = "810484";
//        kig.purchases[0] = kigPurchase;
//        
//        companies.add(kig);
//          акжар тест
//        CompanySettings company = new CompanySettings();
//        company.certStorage = new CertificateStorage("D:\\aset\\data\\ecp\\akzhar\\GOSTKZ_444823fb9c5c13f2163a3506bb9c23dc14b3e0c7.p12", "123456", "D:\\aset\\data\\ecp\\akzhar\\AUTH_RSA_6720615efb90a8b546b3d5ccc5496d41f6d3dd77.p12", "123456", "pkcs12", "D:\\aset\\data\\ecp\\ecp\\oebs.gos.jks", "3075341");
//        company.companyName = "Акжар";
//        company.loginPassword = "m87018948255";
//        company.emails = "a.barakbayev@altatender.kz,i.akhmetkaliev@altatender.kz,i.miloshenko@altatender.kz";
//        company.purchases = new OfferedPurchase[4];
//        CompanySettings company = new CompanySettings();
////        company.certStorage = new CertificateStorage("C:\\data\\ecp\\akzhar\\GOSTKZ_444823fb9c5c13f2163a3506bb9c23dc14b3e0c7.p12", "123456", "C:\\data\\ecp\\akzhar\\AUTH_RSA_6720615efb90a8b546b3d5ccc5496d41f6d3dd77.p12", "123456", "pkcs12", "C:\\data\\ecp\\ecp\\oebs.gos.jks", "3075341");
//        company.certStorage = new CertificateStorage("/home/administrator/app/priceoffersmonitor/ecp/akzhar/GOSTKZ_444823fb9c5c13f2163a3506bb9c23dc14b3e0c7.p12", "123456", "/home/administrator/app/priceoffersmonitor/ecp/akzhar/AUTH_RSA_6720615efb90a8b546b3d5ccc5496d41f6d3dd77.p12", "123456", "pkcs12", "/home/administrator/app/priceoffersmonitor/ecp/altaik/oebs.gos.jks", "3075341");
//        company.companyName = "Akzhar";
//        company.loginPassword = "m87018948255";
//        company.emails = "a.barakbayev@altatender.kz,i.akhmetkaliev@altatender.kz";
//        company.purchases = new OfferedPurchase[1];
//        OfferedPurchase purchase = new OfferedPurchase();
//        purchase.number = "831533";
//        company.purchases[0] = purchase;
//        companies.add(company);
//          siko
//        CompanySettings company = new CompanySettings();
////        company.certStorage = new CertificateStorage("C:\\data\\ecp\\siko\\GOSTKZ_3a41fd79f1a5f33a76f747e096d0d077e339f781.p12", "123456", "C:\\data\\ecp\\siko\\AUTH_RSA_671b93bba27202641a3dd9c6af8d7f293193f8ed.p12", "123456", "pkcs12", "C:\\data\\ecp\\ecp\\oebs.gos.jks", "3075341");
////        company.certStorage = new CertificateStorage("/home/administrator/app/priceoffersmonitor/ecp/siko/GOSTKZ_3a41fd79f1a5f33a76f747e096d0d077e339f781.p12", "123456", "/home/administrator/app/priceoffersmonitor/ecp/siko/AUTH_RSA_671b93bba27202641a3dd9c6af8d7f293193f8ed.p12", "123456", "pkcs12", "/home/administrator/app/priceoffersmonitor/ecp/altaik/oebs.gos.jks", "3075341");
//        company.certStorage = new CertificateStorage("/home/admin/app/priceoffersmonitor/ecp/siko/GOSTKZ_3a41fd79f1a5f33a76f747e096d0d077e339f781.p12", "123456", "/home/admin/app/priceoffersmonitor/ecp/siko/AUTH_RSA_671b93bba27202641a3dd9c6af8d7f293193f8ed.p12", "123456", "pkcs12", "/home/admin/app/priceoffersmonitor/ecp/altaik/oebs.gos.jks", "3075341");
//        company.companyName = "Siko";
//        company.loginPassword = "parol2015";
//        company.emails = commonEmails;
//        company.purchases = new OfferedPurchase[1];
//        OfferedPurchase purchase = new OfferedPurchase();
//        purchase.number = "828337,1";
//        company.purchases[0] = purchase;
//        companies.add(company);
//
//        OfferedPurchase purchase1 = new OfferedPurchase();
//        purchase1.number = "800012-2";
//        purchase1.isLabor = true;
//        purchase1.isNoticed = false;
//        purchase1.isTechSpecChoosing = false;
//        purchase1.nLots = 1;
//        purchase1.lotNumbers = new String[]{"1"};
//        purchase1.lotPrices = new String[]{"12796842"};
//        company.purchases[0] = purchase1;
//        
//        OfferedPurchase purchase2 = new OfferedPurchase();
//        purchase2.number = "809333-2";
//        company.purchases[1] = purchase2;
//        OfferedPurchase purchase = new OfferedPurchase();
//        purchase.number = "810433-2";
//        company.purchases[2] = purchase;
//        OfferedPurchase purchase3 = new OfferedPurchase();
//        purchase3.number = "814134-2";
//        company.purchases[3] = purchase3;
        //shanyrak
//        CompanySettings company = new CompanySettings();
////        company.certStorage = new CertificateStorage("C:\\data\\ecp\\shanyrak\\GOSTKNCA_0eb37a6276562f4c827cca58b974e25ade80f733.p12", "123456", "C:\\data\\ecp\\shanyrak\\AUTH_RSA256_efa32fcefbe4743377406cdfed1b8f120e21a010.p12", "123456", "pkcs12", "C:\\data\\ecp\\ecp\\oebs.gos.jks", "3075341");
//        company.certStorage = new CertificateStorage("/home/administrator/app/priceoffersmonitor/ecp/shanyrak/GOSTKNCA_0eb37a6276562f4c827cca58b974e25ade80f733.p12", "123456", "/home/administrator/app/priceoffersmonitor/ecp/shanyrak/AUTH_RSA256_efa32fcefbe4743377406cdfed1b8f120e21a010.p12", "123456", "pkcs12", "/home/administrator/app/priceoffersmonitor/ecp/altaik/oebs.gos.jks", "3075341");
////        company.certStorage = new CertificateStorage("/home/admin/app/priceoffersmonitor/ecp/shanyrak/GOSTKNCA_0eb37a6276562f4c827cca58b974e25ade80f733.p12", "123456", "/home/admin/app/priceoffersmonitor/ecp/shanyrak/AUTH_RSA256_efa32fcefbe4743377406cdfed1b8f120e21a010.p12", "123456", "pkcs12", "/home/admin/app/priceoffersmonitor/ecp/altaik/oebs.gos.jks", "3075341");
//        company.companyName = "Astana Shanyrak";
//        company.loginPassword = "asd123fgh";
//        company.emails = commonEmails;
//        company.purchases = new OfferedPurchase[1];
//        OfferedPurchase purchase = new OfferedPurchase();
//        purchase.number = "833613";
//        company.purchases[0] = purchase;
//        companies.add(company);
//        //shanyrak2 yuteks
//        CompanySettings company = new CompanySettings();
////        company.certStorage = new CertificateStorage("C:\\data\\ecp\\shanyrak2\\GOSTKZ_4bd9b503276c4dab5dbd279d77cb428212ba6dbd.p12", "123456", "C:\\data\\ecp\\shanyrak2\\AUTH_RSA_abfa50ce7bcfc2d5922f66780df3126ddafa961e.p12", "123456", "pkcs12", "C:\\data\\ecp\\ecp\\oebs.gos.jks", "3075341");
////        CertificateStorage certStorage = new CertificateStorage("/home/admin/app/priceoffersmonitor/ecp/shanyrak2/GOSTKZ_4bd9b503276c4dab5dbd279d77cb428212ba6dbd.p12", "123456", "/home/admin/app/priceoffersmonitor/ecp/shanyrak2/AUTH_RSA_abfa50ce7bcfc2d5922f66780df3126ddafa961e.p12", "123456", "pkcs12", "/home/admin/app/priceoffersmonitor/ecp/altaik/oebs.gos.jks", "3075341");
//        company.certStorage = new CertificateStorage("/home/administrator/app/priceoffersmonitor/ecp/shanyrak2/GOSTKZ_4bd9b503276c4dab5dbd279d77cb428212ba6dbd.p12", "123456", "/home/administrator/app/priceoffersmonitor/ecp/shanyrak2/AUTH_RSA_abfa50ce7bcfc2d5922f66780df3126ddafa961e.p12", "123456", "pkcs12", "/home/administrator/app/priceoffersmonitor/ecp/altaik/oebs.gos.jks", "3075341");
//        company.companyName = "Yuteks";
//        company.loginPassword = "anarela159";
//        company.emails = commonEmails;
//        company.purchases = new OfferedPurchase[1];
//        OfferedPurchase purchase = new OfferedPurchase();
//        purchase.number = "834092";
//        company.purchases[0] = purchase;
//        companies.add(company);
        GosMonitor monitor = new GosMonitor();
        monitor.companies = companies;
        monitor.Start();

    }


    public void MultipleMainSignProcess(){
        Properties certProps = new Properties();
        certProps.setProperty("keyStore", certStore.getAuthkey().Store);
        certProps.setProperty("keyStoreType", certStore.getAuthkey().getType());
        certProps.setProperty("keyStorePassword", certStore.getAuthkey().Password);
        certProps.setProperty("trustStore", certStore.getTrust().Store);
        certProps.setProperty("trustStorePassword", certStore.getTrust().Password);
        getGosCrawler().AuthInit(certProps);

        if(!getGosCrawler().authGosAuction(loginPassword)) return;
        Document purchaseHomeDoc = gosCrawler.getCurrentDoc();
        
        Elements elements = purchaseHomeDoc.select("span#OpenInvTable a[id*=\"NotPaused\"]");
        
        while(elements.isEmpty()){
            Elements checkEles = purchaseHomeDoc.select("span#welcomeText");
            for(Element checkEle: checkEles){
                String welcom = checkEle.text();
                logger.log(Level.INFO, welcom);
            }
            
            logger.log(Level.WARNING, "OpenInvTable empty. Try next ...");
            try {
                Thread.sleep(200);
            } catch (InterruptedException ex) {
                Logger.getLogger(GosRobot.class.getName()).log(Level.SEVERE, null, ex);
                break;
            }
            purchaseHomeDoc = gosCrawler.getDoc();
            elements = purchaseHomeDoc.select("span#OpenInvTable a[id*=\"NotPaused\"]");
        }
        
        int ielements = 0;
        for (Element purchaseElem : elements) {
            String title = purchaseElem.attr("title");
            if(!title.endsWith("-2")){
                logger.log(Level.WARNING, "Something wrong with Number of Purchase. Not equals -2.");
                continue;
            }
            
            String purchaseHref = purchaseElem.attr("href");
            if(purchaseHref.isEmpty()){
                logger.log(Level.WARNING,"Something wrong with getting purchase href.");
            }
            
            purchaseHref = getGosCrawler().fullOA_HTML + purchaseHref;
          
            getGosCrawler().setMethod(Connection.Method.GET);
            getGosCrawler().setUrl(purchaseHref);
            Document purchaseDoc = getGosCrawler().getDoc();
            
            Document offerCreatedDoc = gosCrawler.SubmitAction(GetOfferCreateData(purchaseDoc));
            
            Map<String, String> offerCreatedData;
            Document stringsDoc;
            Elements priceEntryElems;
            
            if (isTechSpecChoose) {
                stringsDoc = offerCreatedDoc;

                int nTechSpecs = stringsDoc.select("span#BidItemPricesTableVO a[id*=\":XXBestDocRN:\"]").size();

                for (int i = 0; i < nTechSpecs; i++) {
                    Elements techSpecElems = stringsDoc.select("span#BidItemPricesTableVO a[id$=\":XXBestDocRN:" + i + "\"]");
                    if (!techSpecElems.isEmpty()) {
                        Element techSpecElem = techSpecElems.get(0);

                        //gosCrawler.setData(gosCrawler.getFormData(offerCreatedDoc),true);
                        gosCrawler.setUrl(gosCrawler.fullOA_HTML + techSpecElem.attr("href"));
                        gosCrawler.setMethod(Connection.Method.GET);
                        Document techSpecDoc = gosCrawler.getDoc();

                        Map<String, String> data = gosCrawler.getFormData(techSpecDoc, "button#SaveButton");
                        data.put("Documents:selected", "0");
                        stringsDoc = gosCrawler.SubmitAction(data);
                    }
                }
                
                offerCreatedData = gosCrawler.getFormData(stringsDoc, "td[background=\"/OA_HTML/cabo/images/swan/subDimTabBg.gif\"] > a");
                
            } else {
                stringsDoc = gosCrawler.SubmitAction(gosCrawler.getFormData(offerCreatedDoc, "a[accesskey=\".\"]"));

                offerCreatedData = gosCrawler.getFormData(stringsDoc, "td[background=\"/OA_HTML/cabo/images/swan/subDimTabBg.gif\"] > a");

            }
            
            if (isLabor) {
                priceEntryElems = stringsDoc.select("span#BidItemPricesTableVO input[id*=\":LinePriceEntry:\"]");
            } else {
                priceEntryElems = stringsDoc.select("span#BidItemPricesTableVO input[id*=\":BidCurrencyPriceEntry:\"]");
            }
            for (int i = 0; i < priceEntryElems.size() && i < nLots; i++) {
                Element elem = priceEntryElems.get(i);
                offerCreatedData.put(elem.attr("name"), muprices[ielements][i]);
            }
            
            ielements++;

            //Document headingDoc = gosCrawler.SubmitAction(offerCreatedData);
            Document offerGenerateDoc = gosCrawler.SubmitAction(gosCrawler.getFormData(gosCrawler.SubmitAction(offerCreatedData), "span#RenderReportsRN > a#ShowCD10"));
            Document updatePencilDoc = gosCrawler.SubmitAction(gosCrawler.getFormData(offerGenerateDoc, "a[id*=\":UpdateYes:\"]"));
            
            //Crawler uploadCrawler = new Crawler();
            //uploadCrawler.setUrl(gosCrawler.baseUrl + updatePencilDoc.select("a#FileListRNEx:CHECKED:0").attr("href"));
            //uploadCrawler.setBodySize(20000);
            //uploadCrawler.setMethod(Connection.Method.GET);
            //uploadCrawler.getDoc();
            
            //Storage storage = new Storage("PriceOffersFiles");
            //storage.LoadFile(updatePencilDoc.select("a#FileListRNEx:CHECKED:0").attr("title"), uploadCrawler.getResponse().bodyAsBytes());
            
            Document signAppletDoc = gosCrawler.SubmitAction(gosCrawler.getFormData(updatePencilDoc,"a[id=\"FileListRNEx:SignItem:0\"]"));
            
            Map<String,String> appletParams = GetAppletParams(signAppletDoc);
//            Signer(gosCrawler.baseUrl, appletParams.get("downloadURL"), appletParams.get("uploadURL"), appletParams.get("fileAttributes"), appletParams.get("userName"));
            Map<String,String> signAppletData = gosCrawler.getFormData(signAppletDoc, "button#DoneBtn");
            signAppletData.put("newStatus", signAppletDoc.select("select#newStatus option[value^=\"SUBMITTED\"]").attr("value"));
            Document offerContinueDoc = gosCrawler.SubmitAction(signAppletData);
            
            Document offerFinishDoc = gosCrawler.SubmitAction(gosCrawler.getFormData(offerContinueDoc, "button#ContinueBtn"));
            Document offerLastFinishDoc = gosCrawler.SubmitAction(gosCrawler.getFormData(offerFinishDoc, "button#SubmitBtn"));

            offerCreatedData.clear();
            stringsDoc = null;
            priceEntryElems.clear();
            offerLastFinishDoc = null;
            offerFinishDoc = null;
            offerContinueDoc = null;
            signAppletData.clear();
            appletParams.clear();
            signAppletDoc = null;
            //storage = null;
            //uploadCrawler.close();
            updatePencilDoc = null;
            offerGenerateDoc = null;
            
            
            logger.log(Level.INFO, "FINISH.");
        }
        
        elements.clear();
        purchaseHomeDoc = null;
        gosCrawler.close();
    }
    
    
    public void RejectSinging(String purchaseNumber){
        Properties certProps = new Properties();
        certProps.setProperty("keyStore", certStore.getAuthkey().Store);
        certProps.setProperty("keyStoreType", certStore.getAuthkey().getType());
        certProps.setProperty("keyStorePassword", certStore.getAuthkey().Password);
        certProps.setProperty("trustStore", certStore.getTrust().Store);
        certProps.setProperty("trustStorePassword", certStore.getTrust().Password);
        getGosCrawler().AuthInit(certProps);

        if(!gosCrawler.GetAuthGosAucMainPage(loginPassword)) {
            logger.log(Level.WARNING, "NO Authorization.");
            return;
        }
        
        //главная страница ЛК
        Document mainPage = gosCrawler.getCurrentDoc();
        logger.log(Level.INFO, "Main page exceeded.");
        
        if(!gosCrawler.GetAuthGosAucPurchasesHomePage(mainPage)){
            if(!gosCrawler.GetAuthGosCustomerPurchasesPage(mainPage)) {
                logger.log(Level.WARNING, "Problem with getting Customer Purchases Page from Main Page.");
                return;
            }
            if(!gosCrawler.GetAuthGosAucPurchasesHomePage(gosCrawler.getCurrentDoc())){
                logger.log(Level.WARNING, "Problem with getting Purchase Home Page from Customer Purchase Page.");
                return;
            }
        }
        
        //домашняя страница закупки
        Document purchaseHomeDoc = gosCrawler.getCurrentDoc();
        logger.log(Level.INFO, "Purchase Home Page exceeded.");
        
        Elements elements = purchaseHomeDoc.select("span#RespResultTable a[id*=\"NotPaused\"]");
        
        for (Element purchaseElem : elements) {
            String title = purchaseElem.attr("title");
            if(!title.equals(purchaseNumber)){
                continue;
            }
            
            String purchaseHref = purchaseElem.attr("href");
            if(purchaseHref.isEmpty()){
                logger.log(Level.WARNING,"Something wrong with getting purchase href.");
            }
            
            purchaseHref = getGosCrawler().fullOA_HTML + purchaseHref;
          
            getGosCrawler().setMethod(Connection.Method.GET);
            getGosCrawler().setUrl(purchaseHref);
            Document purchaseDoc = getGosCrawler().getDoc();
            logger.log(Level.INFO, "Purchase page entered.");
            
            Document offerCreatedDoc = gosCrawler.SubmitAction(GetOfferCreateData(purchaseDoc));
            logger.log(Level.INFO, "Offer created.");
            Document rejectDoc = gosCrawler.SubmitAction(gosCrawler.getFormData(offerCreatedDoc, "button#DisqualifiedSubmitButton"));
            logger.log(Level.INFO, "Reject started.");
            Document acceptDoc = gosCrawler.SubmitAction(gosCrawler.getFormData(rejectDoc, "button[title=\"Да\"]"));
            logger.log(Level.INFO, "Reject executed.");
            break;
        }

        gosCrawler.ExitFromGosAucCabinet();
        logger.log(Level.INFO, "FINISH.");
        
        elements.clear();
        purchaseHomeDoc = null;
        gosCrawler.close();
    }
    
    
    public void RedoneSinging(String purchaseNumber){
        Properties certProps = new Properties();
        certProps.setProperty("keyStore", certStore.getAuthkey().Store);
        certProps.setProperty("keyStoreType", certStore.getAuthkey().getType());
        certProps.setProperty("keyStorePassword", certStore.getAuthkey().Password);
        certProps.setProperty("trustStore", certStore.getTrust().Store);
        certProps.setProperty("trustStorePassword", certStore.getTrust().Password);
        getGosCrawler().AuthInit(certProps);

        if(!getGosCrawler().authGosAuction(loginPassword)) return;
        
        //домашняя страница закупок
        Document purchaseHomeDoc = gosCrawler.getCurrentDoc();
        
        //поиск торга на домашней странице
        Elements elements = purchaseHomeDoc.select("span#RespResultTable a[id*=\"NotPaused\"]");
        
        Document offerCreatedDoc = null;
        Document updatePencilDoc = null;
        Element purchase = null;
        boolean isOk = true;
        int i = 0;

        while (isOk) {
            try {

            isOk = false;
            } catch (Exception ex) {
                i++;
            }
        }
 
                for (Element purchaseElem : elements) {
                    String title = purchaseElem.attr("title");
                    if (!title.equals(purchaseNumber)) {
                        logger.log(Level.WARNING, "Number of Purchase not founded.");
                        continue;
                    } else {
                        purchase = purchaseElem;
                        break;
                    }
                }

                //страница закупки
                String purchaseHref = purchase.attr("href");
                if (purchaseHref.isEmpty()) {
                    logger.log(Level.WARNING, "Something wrong with getting purchase href.");
                }

                if(i==0) purchaseHref = getGosCrawler().fullOA_HTML + purchaseHref + "1";
                else     purchaseHref = getGosCrawler().fullOA_HTML + purchaseHref;

                getGosCrawler().setMethod(Connection.Method.GET);
                getGosCrawler().setUrl(purchaseHref);
                Document purchaseDoc = getGosCrawler().getDoc();

                //создание котировки
                offerCreatedDoc = gosCrawler.SubmitAction(GetOfferCreateData(purchaseDoc));

//            Map<String, String> offerCreatedData;
//            Document stringsDoc;
//            Elements priceEntryElems;
//            
//            if (isTechSpecChoose) {
//                stringsDoc = offerCreatedDoc;
//
//                int nTechSpecs = stringsDoc.select("span#BidItemPricesTableVO a[id*=\":XXBestDocRN:\"]").size();
//
////                for (int i = 0; i < nTechSpecs; i++) {
////                    Elements techSpecElems = stringsDoc.select("span#BidItemPricesTableVO a[id$=\":XXBestDocRN:" + i + "\"]");
////                    if (!techSpecElems.isEmpty()) {
////                        Element techSpecElem = techSpecElems.get(0);
////
////                        //gosCrawler.setData(gosCrawler.getFormData(offerCreatedDoc),true);
////                        gosCrawler.setUrl(gosCrawler.fullOA_HTML + techSpecElem.attr("href"));
////                        gosCrawler.setMethod(Connection.Method.GET);
////                        Document techSpecDoc = gosCrawler.getDoc();
////
////                        Map<String, String> data = gosCrawler.getFormData(techSpecDoc, "button#SaveButton");
////                        data.put("Documents:selected", "0");
////                        stringsDoc = gosCrawler.SubmitAction(data);
////                    }
////                }
//                
//                offerCreatedData = gosCrawler.getFormData(stringsDoc, "td[background=\"/OA_HTML/cabo/images/swan/subDimTabBg.gif\"] > a");
//                
//            } else {
//                stringsDoc = gosCrawler.SubmitAction(gosCrawler.getFormData(offerCreatedDoc, "a[accesskey=\".\"]"));
//
//                offerCreatedData = gosCrawler.getFormData(stringsDoc, "td[background=\"/OA_HTML/cabo/images/swan/subDimTabBg.gif\"] > a");
//
//            }
//            
//            if (isLabor) {
//                priceEntryElems = stringsDoc.select("span#BidItemPricesTableVO input[id*=\":LinePriceEntry:\"]");
//            } else {
//                priceEntryElems = stringsDoc.select("span#BidItemPricesTableVO input[id*=\":BidCurrencyPriceEntry:\"]");
//            }
//            for (int i = 0; i < priceEntryElems.size() && i < nLots; i++) {
//                Element elem = priceEntryElems.get(i);
//                offerCreatedData.put(elem.attr("name"), prices[i]);
//            }
//
//            //заголовок
////            Document headingDoc = gosCrawler.SubmitAction(offerCreatedData);
//            
//            //заголовок и сформировать цен предл
//            Document offerGenerateDoc = gosCrawler.SubmitAction(gosCrawler.getFormData(gosCrawler.SubmitAction(offerCreatedData), "span#RenderReportsRN > a#ShowCD10"));
            
            //обновить
//            Document updatePencilDoc = gosCrawler.SubmitAction(gosCrawler.getFormData(offerGenerateDoc, "a[id*=\":UpdateYes:\"]"));

//            Document updatePencilDoc = gosCrawler.SubmitAction(gosCrawler.getFormData(headingDoc, "a[id*=\":UpdateYes:\"]"));
            
            updatePencilDoc = gosCrawler.SubmitAction(gosCrawler.getFormData(offerCreatedDoc, "a[id*=\":UpdateYes:4\"]"));
           
            
            Document signAppletDoc = gosCrawler.SubmitAction(gosCrawler.getFormData(updatePencilDoc,"a[id=\"FileListRNEx:SignItem:0\"]"));
            
            Map<String,String> appletParams = GetAppletParams(signAppletDoc);
//            Signer(gosCrawler.baseUrl, appletParams.get("downloadURL"), appletParams.get("uploadURL"), appletParams.get("fileAttributes"), appletParams.get("userName"));
            Map<String,String> signAppletData = gosCrawler.getFormData(signAppletDoc, "button#DoneBtn");
            signAppletData.put("newStatus", signAppletDoc.select("select#newStatus option[value^=\"SUBMITTED\"]").attr("value"));
            Document offerContinueDoc = gosCrawler.SubmitAction(signAppletData);
            
//            Document offerFinishDoc = gosCrawler.SubmitAction(gosCrawler.getFormData(offerContinueDoc, "button#ContinueBtn"));
//            Document offerLastFinishDoc = gosCrawler.SubmitAction(gosCrawler.getFormData(offerFinishDoc, "button#SubmitBtn"));

//            offerCreatedData.clear();
//            stringsDoc = null;
//            priceEntryElems.clear();

//            offerLastFinishDoc = null;
//            offerFinishDoc = null;

            offerContinueDoc = null;
            signAppletData.clear();
            appletParams.clear();
            signAppletDoc = null;
            //storage = null;
            //uploadCrawler.close();
            updatePencilDoc = null;
            //offerGenerateDoc = null;
            
            
            logger.log(Level.INFO, "FINISH.");
        
        elements.clear();
        purchaseHomeDoc = null;
        gosCrawler.close();
        
    }
    
    

    public void MultiPassing(){
        Properties certProps = new Properties();
        certProps.setProperty("keyStore", certStore.getAuthkey().Store);
        certProps.setProperty("keyStoreType", certStore.getAuthkey().getType());
        certProps.setProperty("keyStorePassword", certStore.getAuthkey().Password);
        certProps.setProperty("trustStore", certStore.getTrust().Store);
        certProps.setProperty("trustStorePassword", certStore.getTrust().Password);
        getGosCrawler().AuthInit(certProps);

        if(!getGosCrawler().authGosAuction(loginPassword)) return;
        Document mainDoc = gosCrawler.getCurrentDoc();
        
        Elements elements = mainDoc.select("span#OpenInvTable a[id*=\"NotPaused\"]");
        
        while(elements.size() == 0){
            Elements checkEles = mainDoc.select("span#welcomeText");
            for(Element checkEle: checkEles){
                String welcom = checkEle.text();
                logger.log(Level.INFO, welcom);
            }
            
            logger.log(Level.WARNING, "OpenInvTable empty. Try next ...");
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Logger.getLogger(GosRobot.class.getName()).log(Level.SEVERE, null, ex);
                break;
            }
            mainDoc = gosCrawler.getDoc();
            elements = mainDoc.select("span#OpenInvTable a[id*=\"NotPaused\"]");
        }
        
        for (Element purchaseElem : elements) {
            String title = purchaseElem.attr("title");
            if(!title.endsWith("-2")){
                logger.log(Level.WARNING, "Something wrong with Number of Purchase. Not equals -2.");
                continue;
            }
            
            String purchaseHref = purchaseElem.attr("href");
            if(purchaseHref.isEmpty()){
                logger.log(Level.WARNING,"Something wrong with getting purchase href.");
            }
            
            purchaseHref = getGosCrawler().fullOA_HTML + purchaseHref;
          
            getGosCrawler().setMethod(Connection.Method.GET);
            getGosCrawler().setUrl(purchaseHref);
            Document purchaseDoc = getGosCrawler().getDoc();
            
            Document offerCreatedDoc = gosCrawler.SubmitAction(GetOfferCreateData(purchaseDoc));
            
            Map<String, String> offerCreatedData;
            Document stringsDoc;
            Elements priceEntryElems;
            
            if (isTechSpecChoose) {
                stringsDoc = offerCreatedDoc;

                int nTechSpecs = stringsDoc.select("span#BidItemPricesTableVO a[id*=\":XXBestDocRN:\"]").size();

                for (int i = 0; i < nTechSpecs; i++) {
                    Elements techSpecElems = stringsDoc.select("span#BidItemPricesTableVO a[id$=\":XXBestDocRN:" + i + "\"]");
                    if (!techSpecElems.isEmpty()) {
                        Element techSpecElem = techSpecElems.get(0);

                        //gosCrawler.setData(gosCrawler.getFormData(offerCreatedDoc),true);
                        gosCrawler.setUrl(gosCrawler.fullOA_HTML + techSpecElem.attr("href"));
                        gosCrawler.setMethod(Connection.Method.GET);
                        Document techSpecDoc = gosCrawler.getDoc();

                        Map<String, String> data = gosCrawler.getFormData(techSpecDoc, "button#SaveButton");
                        data.put("Documents:selected", "0");
                        stringsDoc = gosCrawler.SubmitAction(data);
                    }
                }
                
                offerCreatedData = gosCrawler.getFormData(stringsDoc, "td[background=\"/OA_HTML/cabo/images/swan/subDimTabBg.gif\"] > a");
                
            } else {
                stringsDoc = gosCrawler.SubmitAction(gosCrawler.getFormData(offerCreatedDoc, "a[accesskey=\".\"]"));

                offerCreatedData = gosCrawler.getFormData(stringsDoc, "td[background=\"/OA_HTML/cabo/images/swan/subDimTabBg.gif\"] > a");

            }
            
            if (isLabor) {
                priceEntryElems = stringsDoc.select("span#BidItemPricesTableVO input[id*=\":LinePriceEntry:\"]");
            } else {
                priceEntryElems = stringsDoc.select("span#BidItemPricesTableVO input[id*=\":BidCurrencyPriceEntry:\"]");
            }
            for (int i = 0; i < priceEntryElems.size() && i < nLots; i++) {
                Element elem = priceEntryElems.get(i);
                offerCreatedData.put(elem.attr("name"), prices[i]);
            }

            //Document headingDoc = gosCrawler.SubmitAction(offerCreatedData);
            Document offerGenerateDoc = gosCrawler.SubmitAction(gosCrawler.getFormData(gosCrawler.SubmitAction(offerCreatedData), "span#RenderReportsRN > a#ShowCD10"));
            Document updatePencilDoc = gosCrawler.SubmitAction(gosCrawler.getFormData(offerGenerateDoc, "a[id*=\":UpdateYes:\"]"));
            
            //Crawler uploadCrawler = new Crawler();
            //uploadCrawler.setUrl(gosCrawler.baseUrl + updatePencilDoc.select("a#FileListRNEx:CHECKED:0").attr("href"));
            //uploadCrawler.setBodySize(20000);
            //uploadCrawler.setMethod(Connection.Method.GET);
            //uploadCrawler.getDoc();
            
            //Storage storage = new Storage("PriceOffersFiles");
            //storage.LoadFile(updatePencilDoc.select("a#FileListRNEx:CHECKED:0").attr("title"), uploadCrawler.getResponse().bodyAsBytes());
            
            Document signAppletDoc = gosCrawler.SubmitAction(gosCrawler.getFormData(updatePencilDoc,"a[id=\"FileListRNEx:SignItem:0\"]"));
            
            Map<String,String> appletParams = GetAppletParams(signAppletDoc);
//            Signer(gosCrawler.baseUrl, appletParams.get("downloadURL"), appletParams.get("uploadURL"), appletParams.get("fileAttributes"), appletParams.get("userName"));
            Map<String,String> signAppletData = gosCrawler.getFormData(signAppletDoc, "button#DoneBtn");
            signAppletData.put("newStatus", signAppletDoc.select("select#newStatus option[value^=\"SUBMITTED\"]").attr("value"));
            Document offerContinueDoc = gosCrawler.SubmitAction(signAppletData);
            
            Document offerFinishDoc = gosCrawler.SubmitAction(gosCrawler.getFormData(offerContinueDoc, "button#ContinueBtn"));
            Document offerLastFinishDoc = gosCrawler.SubmitAction(gosCrawler.getFormData(offerFinishDoc, "button#SubmitBtn"));

            offerCreatedData.clear();
            stringsDoc = null;
            priceEntryElems.clear();
            offerLastFinishDoc = null;
            offerFinishDoc = null;
            offerContinueDoc = null;
            signAppletData.clear();
            appletParams.clear();
            signAppletDoc = null;
            //storage = null;
            //uploadCrawler.close();
            updatePencilDoc = null;
            offerGenerateDoc = null;
            
            
            logger.log(Level.INFO, "FINISH.");
        }
        
        elements.clear();
        mainDoc = null;
        gosCrawler.close();
    }

    
    
    public Map<String,String> GetOfferCreateData(Document doc){
        String onClickObjectRef = "button#GoBtnTop";
        Map<String,String> data = gosCrawler.getFormData(doc, onClickObjectRef);
        Elements elements = doc.select("span#NegotiationSummary select#ActionListTop option");
        if(elements.isEmpty()) return null;
        String offerCreateSelectValue = elements.get(0).attr("value");
        if(offerCreateSelectValue.isEmpty()) return null;
        data.put("ActionListTop",offerCreateSelectValue);
        return data;
    }
    
    
    
    public void ReadyBtnClickAction() {
        Document document = getGosCrawler().getDoc();
        Map<String, String> data = getGosCrawler().getFormData(document, "button#DoneBtn");
        getGosCrawler().setData(data,true);
        getGosCrawler().setMethod(Connection.Method.POST);
        getGosCrawler().setUrl(gosCrawler.baseUrl + document.select("form").attr("action"));
        getGosCrawler().getDoc();
        logger.log(Level.INFO, document.title());
    }


 
    
    public HashMap<String,String> GetAppletParamsAttach3(){
            CertificateStoreDescription authkey = certStore.getAuthkey();
            CertificateStoreDescription trust = certStore.getTrust();
            Properties certProps = new Properties();
            certProps.setProperty("keyStore", authkey.Store);
            certProps.setProperty("ketType", authkey.getType());
            certProps.setProperty("keyPassword", authkey.Password);
            certProps.setProperty("trustStore", trust.Store);
            certProps.setProperty("trustPassword", trust.Password);
            gosCrawler.AuthInit(certProps);
            
            logger.log(Level.INFO,Paths.get("./").toAbsolutePath().normalize().toString());
            
            gosCrawler.authGosAuction(loginPassword);
            Document document = gosCrawler.getDoc();

            logger.log(Level.INFO, document.title());
            
            Elements elements = document.select("span#RespResultTable a[id^=\"N11:NotPaused\"]");
            String number = "821971,1";
            String href = null;
            for(Element element: elements){
                String title = element.attr("title");
                if(number.equals(title)){
                    href = element.attr("href");
                    break;
                }
            }
            if(null == href){
//            Error
                return null;
            }
            
            String urlsection = "/OA_HTML/";
            
            gosCrawler.setUrl(gosCrawler.baseUrl + urlsection + href);
            document = gosCrawler.getDoc();

            System.out.println(document.title());
            Map <String, String> data = gosCrawler.getFormData(document, "td#ButtonBarGoCell button#GoBtnTop");
            elements = document.select("select#ActionListTop option");
            if(null != elements.get(0) && elements.get(0).val() != null) data.put("ActionListTop", elements.get(0).val());
            else return null;

            gosCrawler.setData(data,true);
            gosCrawler.setUrl(gosCrawler.baseUrl + document.select("form").attr("action"));
            gosCrawler.setHeader("Content-Type", "application/x-www-form-urlencoded");
            gosCrawler.setMethod(Connection.Method.POST);
            gosCrawler.setTimeout(60000);
            document = gosCrawler.getDoc();

            System.out.println(document.title());
            
            data = gosCrawler.getFormData(document, "a[id*=\"UpdateYes:4\"]");
            gosCrawler.setData(data,true);
            gosCrawler.setMethod(Connection.Method.POST);
            gosCrawler.setUrl(gosCrawler.baseUrl + document.select("form").attr("action"));
            document = gosCrawler.getDoc();

            System.out.println(document.title());
            
            data = gosCrawler.getFormData(document, "a[id*=\"SignItem:0\"]");
            gosCrawler.setData(data,true);
            gosCrawler.setMethod(Connection.Method.POST);
            gosCrawler.setUrl(gosCrawler.baseUrl + document.select("form").attr("action"));
            document = gosCrawler.getDoc();
            
            elements = document.select("applet");
            HashMap<String, String> appletParams = new HashMap();
            if(null!=elements&&elements.size()>0){
                Elements params = elements.select("param");
                
                for (Element param : params) {
                    appletParams.put(param.attr("name"), param.attr("value"));
                }
            }
            
            System.out.println(document.title());
            
            return appletParams;
            
    }
    
    
    public Map<String,String> GetAppletParams(Document document){
        if(document == null) return null;

        Elements elements = document.select("applet");

        Map<String, String> appletParams = new HashMap();

        if (elements.size() > 0) {
            Elements params = elements.select("param");

            for (Element param : params) {
                appletParams.put(param.attr("name"), param.attr("value"));
            }
        }

        logger.log(Level.INFO, "Ok with getting applet params: {0}", document.title());

        return appletParams;
    }


    public GosAuthCrawler getGosCrawler() {
        if(gosCrawler==null) gosCrawler = new GosAuthCrawler();
        return gosCrawler;
    }

    public void setGosCrawler(GosAuthCrawler gosCrawler) {
        this.gosCrawler = gosCrawler;
    }
    
    
//    public void Signer(String baseUrl, String downloadUrl, String uploadUrl, String fileAttribute, String userName) {
//        try {
//            SignUtil.loadProvider();
//            
//            CertificateStoreDescription goskey = certStore.getGoskey();
//            keyStoreForSign = new File(goskey.Store);
//
//            AltaDownloadUtils downloadUtils;
//
//            AltaUploadUtils.setFileAttributes(fileAttribute);
//            AltaUploadUtils.setUserName(userName);
//            
//            URL baseURL = new URL(baseUrl);
//            URL uploadURL = new URL(baseURL,uploadUrl);
//            URL downloadURL = new URL(baseURL,downloadUrl +"?"+AltaUploadUtils.getFileAttributes());
//
//            
//            //if(SignUtil.isProviderLoaded()){
//                
//                Map<String,String> cookies = gosCrawler.getRequest().cookies();
//                String cookiesStr = "";
//                for (Map.Entry<String, String> cookie : cookies.entrySet()) {
//                    String key = cookie.getKey();
//                    String value = cookie.getValue();
//                    cookiesStr = cookiesStr.concat(key + "=" + value + ";");
//                }
//                
//                
//                downloadUtils = new AltaDownloadUtils(downloadURL, certStore, cookiesStr);
//                downloadUtils.execute();
//                
//                while(!downloadUtils.isDone()){
//                }
//                
//                if(null!=downloadUtils.getServerErrorResponse()) Logger.getLogger(GosRobot.class.getName()).log(Level.INFO,downloadUtils.getServerErrorResponse());
//                if(null!=downloadUtils.getDownloadException()) Logger.getLogger(GosRobot.class.getName()).log(Level.INFO,downloadUtils.getDownloadException());
//                
//                Logger.getLogger(GosRobot.class.getName()).log(Level.INFO, "{0}", downloadUtils.getServerResponseCode());
//            //}
//            
//            
//            
//            PKCS12KeyStoreUtil e1 = new PKCS12KeyStoreUtil(keyStoreForSign, goskey.Password.toCharArray());
//
//            X509Certificate userCertificate = e1.loadCertFromFile();
//            PrivateKey userPrivateKey = e1.loadKeyFromFile();
//
//            //URL tsaUrl = new URL("http://tsp.pki.kz:60003");
//            URL tsaUrl = null;
//
//            
//            //if(null != downloadUtils){
//                fileForSign = downloadUtils.getFile();
//            //}
//            
//
//            SignUtil signUtils = new SignUtil(fileForSign, isSignDownloadedFile, userCertificate, userPrivateKey, tsaUrl, SignUtil.SubjectType.LegalEntity);
//            signUtils.execute();
//                
//            while (!signUtils.isDone()) {
//            }
//
//            fileForSign = signUtils.getSignedZipFile();
//
//            if (null != signUtils.getException()) {
//                Logger.getLogger(GosRobot.class.getName()).log(Level.INFO, signUtils.getException());
//            }
//                
//            
//            
//            
//            AltaUploadUtils uploadUtils = new AltaUploadUtils(uploadURL, AltaUploadUtils.UploadFileType.signedFileFromServer, null, certStore, cookiesStr, new File[]{fileForSign});
//            uploadUtils.execute();
//                
//            while (!uploadUtils.isDone()) {
//            }
//
//            if (null != uploadUtils.getServerErrorResponse()) {
//                Logger.getLogger(GosRobot.class.getName()).log(Level.INFO, uploadUtils.getServerErrorResponse());
//            }
//                
//            Logger.getLogger(GosRobot.class.getName()).log(Level.INFO, "{0}", uploadUtils.getServerResponseCode());
//            
//            
//            //if(!userPrivateKey.isDestroyed()) userPrivateKey.destroy();
//            SignUtil.unloadProvider();
//            
//                
//        } catch (MalformedURLException ex) {
//            Logger.getLogger(GosRobot.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (KeyStoreException | CertificateException | IOException ex) {
//            Logger.getLogger(GosRobot.class.getName()).log(Level.SEVERE, null, ex);
//        }
//
//    }

}

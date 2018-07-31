/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.altaik.priceoffers;

import com.altaik.bo.po.OfferedLot;
import com.altaik.bo.po.SamrukApplicationContext;
import com.altaik.bo.po.ServerPageEnum;
import com.altaik.bp.po.BusinessProcessBase;
import com.altaik.bp.po.samruk.GiveOffer;
import com.altaik.bp.po.samruk.MonitorOffer;
import com.altaik.bp.po.samruk.RemoveOffer;
import com.altaik.bp.po.samruk.CommonsOffer;
import com.altaik.bp.po.samruk.ContextOffer;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.nodes.Document;

/**
 * This class gives all API to work with Robot over Samruk.
 * It consists of several business processes components.
 *
 * @author Aset
 */
public class SamrukRobot extends BusinessProcessBase {

    private boolean isLogin = false;
    private static final Logger logger = Logger.getLogger("SamrukRobot");
    private GiveOffer giveOffer;
    private RemoveOffer removeOffer;
    private MonitorOffer monitorOffer;
    private ContextOffer contextOffer;
    private CommonsOffer commonsOffer;

    
    public SamrukRobot() {
        super();
    }  

    
    public SamrukRobot(SamrukApplicationContext appContext) {
        super(appContext);
        InitBP();
    }  
    

/////////******** SAMRUK ROBOT API ********////////    
    
    
//<editor-fold defaultstate="collapsed" desc="props">
    private Document getCurrentDoc(){
        return appContext.crawler.getCurrentDoc();
    }
    
    
    public String getNumberPurchase(){
        return appContext.settings.number;
    }
//</editor-fold>
    
    private void InitBP(){
        contextOffer = new ContextOffer(appContext);
        giveOffer = new GiveOffer(appContext);
        removeOffer = new RemoveOffer(appContext);
        monitorOffer = new MonitorOffer(appContext);
        commonsOffer = new CommonsOffer(appContext);
    }
    
    
    private void CloseBP(){
        giveOffer.Close();
        monitorOffer.Close();
        removeOffer.Close();
        commonsOffer.Close();
    }
    
    
    public void Init(String appContextFilePath){
        contextOffer = new ContextOffer();
        appContext = contextOffer.LoadContext(appContextFilePath);
        InitBP();
    }


    public boolean Login(){
        isLogin = commonsOffer.Login();
        return isLogin;
    }
    

    public boolean IsLogin(){
        if(!isLogin) isLogin = commonsOffer.Login();
        return isLogin;
    }
    
    
    public boolean SetMaxDown(boolean what){
        return commonsOffer.SetMaxDown(what);
    }

    
    public boolean InvertMaxDown() {
        return commonsOffer.InvertMaxDown();
    }
    
    
    
    public void Close(){
        CloseBP();
        appContext.crawler.close();
        isLogin = false;
    }
    
    
    public Document GoHomePage(){
        return (!IsLogin()) ? null : commonsOffer.GoHomePage();
    }
    
    
    public Document FindPurchaseProccess(){
        return (commonsOffer.GoHomePage() == null) ? null : commonsOffer.FindAuction();
    }
    
    
    public Document FindProjectProccess(){
        return (commonsOffer.GoHomePage() == null) ? null : commonsOffer.FindProject();
    }
    
    
    public boolean AutoMonitorProccess(){
        return (FindPurchaseProccess() == null && FindProjectProccess() == null) ? false : monitorOffer.StartMonitorCycle();
    }
    
    
    public boolean MonitorProccess(){
        return (FindPurchaseProccess() == null && FindProjectProccess() == null) ? false : monitorOffer.Monitor();
    }
    
    
    public Document SetPriceProccess(){
        return (FindPurchaseProccess() != null || FindProjectProccess() != null) ? giveOffer.SetPriceEntry() : null;
    }
    
    
    public Document SetPriceWithPercentProccess(int percent){
        if(FindPurchaseProccess() != null || FindProjectProccess() != null){
            commonsOffer.SetAllLotPercent(percent);
            return giveOffer.SetPriceEntry();
        } else return null;
    }
    

    
    public Document SetSinglePriceProccess(int lotNumber){
        return (FindPurchaseProccess()!= null || FindProjectProccess() != null) ? giveOffer.SetPriceSingleEntry(lotNumber) : null;
    }

    
    public Document SetSinglePriceProccess(String lotNumber){
        return (FindPurchaseProccess()!= null || FindProjectProccess() != null) ? giveOffer.SetPriceSingleEntry(lotNumber) : null;
    }
    

    
    public Document SetSinglePriceWithPercentProccess(int indexLot, int persentage){
        if(FindPurchaseProccess()!= null || FindProjectProccess() != null){
            commonsOffer.SetAllLotPercent(persentage);
            return giveOffer.SetPriceSingleEntry(indexLot);
        } else return null;
    }
    

    
    public boolean SignProccess(){
        if (FindProjectProccess() != null) {
            Document bidCreationDoc = commonsOffer.BidCreationDoc();
            return (bidCreationDoc != null) ? giveOffer.Sign(bidCreationDoc) : false;
        } else return (SetPriceProccess() != null) ? giveOffer.Sign(getCurrentDoc()) : false;
    }

    private boolean SubmitPriceOffer(){
        if(!IsLogin()) return false;
        if(!monitorOffer.StartMonitorCycle()) return false;
        Map<String, String> bidCreationData = monitorOffer.SubmitBidCreation();
        if(bidCreationData == null || bidCreationData.isEmpty()) return false;
        
        Document stringsDoc = giveOffer.SetPriceEntry(bidCreationData);
        if (stringsDoc != null && !giveOffer.Sign(stringsDoc)) {
            if (commonsOffer.GoHomePage() == null) return false;
            removeOffer.RemoveProject();
            return false;
        }
        return true;
    }

    public void AutoCascadeProcess(){
//        for (OfferedLot lot : appContext.settings.lots) {
//            String lotNumber = lot.number;
//            if(monitorOffer.Monitor()){
//                if(lot.isRedone) {
//                    if(SetSinglePriceProccess(lotNumber)==null) continue;
//                    if(!SignProccess()) RemoveProject();
//                }
//            }
//        }
        for(int i = appContext.settings.lots.size()-1; i >= 0; i--){
            OfferedLot lot = appContext.settings.lots.get(i);
            String lotNumber = lot.number;
            if(monitorOffer.Monitor()){
                if(lot.isRedone) {
                    if(SetSinglePriceProccess(lotNumber)==null) continue;
                    if(!SignProccess()) RemoveProject();
                }
            }
        }
    }

    
    public boolean RemoveProject(){
        if(commonsOffer.GoHomePage()==null) return false;
        return removeOffer.RemoveProject();
    }


    public void RejectOfferProcess(String purchaseNumber) {
        if (GoHomePage() == null) return;
        commonsOffer.Reset();
    }

    
    public void MainSignProcess() {
//<editor-fold defaultstate="collapsed" desc="old functionality">
//        Elements elements = ServiceBF.MonitorOpenPurchaseAndGetOpenedPurchases(appContext.crawler, purchaseHomeDoc);
//        if (elements == null) {
//            return;
//        }
//
//        for (Element purchaseElem : elements) {
//            String title = purchaseElem.attr("title");
//            if(!title.equals("225614-2")){
//                continue;
//            }
//
//            String purchaseHref = purchaseElem.attr("href");
//            if(purchaseHref.isEmpty()){
//                logger.log(Level.WARNING,"Something wrong with getting purchase href.");
//                continue;
//            }
//
////            purchaseHref = crawler.fullOA_HTML + purchaseHref;
//
//            appContext.crawler.setMethod(Connection.Method.GET);
//            appContext.crawler.setUrl(purchaseHref);
//            Document purchaseDoc = appContext.crawler.getDoc();
//            logger.log(Level.INFO, "Purchase page entered.");

//Elements elements = purchaseHomeDoc.select("span#RespResultTable a[id*=\"NotPaused\"]");
//        Elements elements = purchaseHomeDoc.select("span#OpenInvTable a[id*=\"NotPaused\"]");
//        for (Element purchaseElem : elements) {
//            String title = purchaseElem.attr("title");
//            if(!title.equals("225614-2")){
//                continue;
//            }
//
//            String purchaseHref = purchaseElem.attr("href");
//            if(purchaseHref.isEmpty()){
//                logger.log(Level.WARNING,"Something wrong with getting purchase href.");
//            }
//
//            purchaseHref = crawler.fullOA_HTML + purchaseHref;
//
//            crawler.setMethod(Connection.Method.GET);
//            crawler.setUrl(purchaseHref);
//            Document purchaseDoc = crawler.getDoc();
//            logger.log(Level.INFO, "Purchase page entered.");

//            Document bidCreationDoc = crawler.SubmitAction(GetOfferToCreateData(purchaseDoc));
//            logger.log(Level.INFO, "Offer created.");
//            Document rejectDoc = gosCrawler.SubmitAction(gosCrawler.getFormData(bidCreationDoc, "button#DisqualifiedSubmitButton"));
//            logger.log(Level.INFO, "Reject started.");
//            Document acceptDoc = gosCrawler.SubmitAction(gosCrawler.getFormData(rejectDoc, "button[title=\"Да\"]"));
//            logger.log(Level.INFO, "Reject executed.");
//            break;
//        }
//        for (Element purchaseElem : elements) {
//            String title = purchaseElem.attr("title");
//            if (!title.equals("225400-2")) {
//                logger.log(Level.WARNING, "Something wrong with Number of Purchase. Not equals -2.");
//                continue;
//            }
//
//            String purchaseHref = purchaseElem.attr("href");
//            if (purchaseHref.isEmpty()) {
//                logger.log(Level.WARNING, "Something wrong with getting purchase href.");
//            }
//
//            purchaseHref = getAuthCrawler().fullOA_HTML + purchaseHref;
//            getAuthCrawler().setMethod(Connection.Method.GET);
//            getAuthCrawler().setUrl(purchaseHref);
//            Document purchaseDoc = getAuthCrawler().getDoc();
//</editor-fold>
        int MAX_COUNT_ERRORS = 10;
        int countErrors = 0;
        boolean toDo = true;
        while (toDo) {
            if(!SubmitPriceOffer()){
                countErrors++;
            }
            if(countErrors > MAX_COUNT_ERRORS){
                toDo = false;
            }
//            SubmitProcess();
        }
        logger.log(Level.INFO, "FINISH.");
    }


    public void SubmitProcess(){
        SubmitPriceOffer();
//        if(!IsLogin()) return;
//        if(!monitorOffer.StartMonitorCycle()) return;
//        if(!monitorOffer.SubmitBidCreation()) return;
//        
//        Document stringsDoc = giveOffer.SetPriceEntry();
//        if (stringsDoc != null && !giveOffer.Sign(stringsDoc)) {
//            if (commonsOffer.GoHomePage() == null) return;
//            removeOffer.RemoveProject();
//        }
    }
    
    public void SubmitPriceOverStepProcess(){
        if(!IsLogin()) return;
        if(!monitorOffer.StartMonitorCycle()) return;
        Map<String, String> bidCreationData = monitorOffer.SubmitBidCreation();
        if(bidCreationData == null || bidCreationData.isEmpty()) return;

        Document stringsDoc, goSingPageDoc = null;
        
        do {
            if(goSingPageDoc == null){
                stringsDoc = giveOffer.SetPriceEntry(bidCreationData);
            } else {
                stringsDoc = giveOffer.SetPriceOnPages(goSingPageDoc);
            }
            goSingPageDoc = giveOffer.GeneratedOfferContinue(stringsDoc);
        } while(giveOffer.CheckErrors());
        
        // this code for testing
//        commonsOffer.GoHomePage();
        
        // this code is disabled at the time of testing
        if (stringsDoc != null && !giveOffer.Sign(stringsDoc)) {
            if (commonsOffer.GoHomePage() == null) return;
            removeOffer.RemoveProject();
        }
    }


    public void DoProcess(){
        giveOffer.Do();
        if(commonsOffer.GoHomePage()==null) return;
        removeOffer.RemoveProject();
    }
    
    
    public void TempSubmitProcess(){
        if(FindProjectProccess() != null || FindPurchaseProccess() != null){
//            logger.log(Level.INFO, "Project or Purchase founded.");
            Document stringsDoc = giveOffer.SetPriceEntry();
            if (stringsDoc != null && !giveOffer.Sign(stringsDoc)) {
//            if (stringsDoc != null) {
                if (commonsOffer.GoHomePage() == null) return;
                removeOffer.RemoveProject();
            }
        }
        logger.log(Level.INFO, "Temp Process finished.");
    }
    
    
    public void ParallelMonitorProcess(){
        
    }
    
    
//<editor-fold defaultstate="collapsed" desc="old functions">
//    public void SubmitBtnProcess(String purchaseNumber) {
//        if (!ServiceBF.Login(appContext)) {
//            return;
//        }
//
//        Document purchaseDoc = ServiceBF.Find(appContext.crawler, purchaseNumber, appContext.crawler.getCurrentDoc());
//        Document bidCreationDoc = appContext.crawler.SubmitAction(GetOfferToCreateData(purchaseDoc, 0));
//
//        Document offerFinishDoc = appContext.crawler.SubmitAction(appContext.crawler.getFormData(bidCreationDoc, "button#ContinueBtn"));
//        Document offerLastFinishDoc = appContext.crawler.SubmitAction(appContext.crawler.getFormData(offerFinishDoc, "button#SubmitBtn"));
//    }

//    public void RejectSinging(String purchaseNumber) {
////        Properties certProps = new Properties();
////        certProps.setProperty("keyStore", certStore.getAuthkey().Store);
////        certProps.setProperty("keyStoreType", certStore.getAuthkey().getType());
////        certProps.setProperty("keyStorePassword", certStore.getAuthkey().Password);
////        certProps.setProperty("trustStore", certStore.getTrust().Store);
////        certProps.setProperty("trustStorePassword", certStore.getTrust().Password);
////        getAuthCrawler().AuthInit(certProps);
//
////        if(!getAuthCrawler().GetAuthGosAucMainPage(loginPassword)) {
////            logger.log(Level.WARNING, "NO Authorization.");
////            return;
////        }
//        //главная страница ЛК
//        Document mainPage = getAuthCrawler().getCurrentDoc();
//        logger.log(Level.INFO, "Main page exceeded.");
//        ServiceBF.GoHomePage(appContext.crawler, mainPage);
//
////        if(!getAuthCrawler().GetAuthGosAucPurchasesHomePage(mainPage)){
////            if(!getAuthCrawler().GetAuthGosCustomerPurchasesPage(mainPage)) {
////                logger.log(Level.WARNING, "Problem with getting Customer Purchases Page from Main Page.");
////                return;
////            }
////            if(!getAuthCrawler().GetAuthGosAucPurchasesHomePage(getAuthCrawler().getCurrentDoc())){
////                logger.log(Level.WARNING, "Problem with getting Purchase Home Page from Customer Purchase Page.");
////                return;
////            }
////        }
//        //домашняя страница закупки
////        Document purchaseHomeDoc = getAuthCrawler().getCurrentDoc();
////        logger.log(Level.INFO, "Purchase Home Page exceeded.");
////
////        Elements elements = purchaseHomeDoc.select("span#RespResultTable a[id*=\"NotPaused\"]");
////
////        for (Element purchaseElem : elements) {
////            String title = purchaseElem.attr("title");
////            if(!title.equals(purchaseNumber)){
////                continue;
////            }
////
////            String purchaseHref = purchaseElem.attr("href");
////            if(purchaseHref.isEmpty()){
////                logger.log(Level.WARNING,"Something wrong with getting purchase href.");
////            }
////
////            purchaseHref = getAuthCrawler().fullOA_HTML + purchaseHref;
////
////            getAuthCrawler().setMethod(Connection.Method.GET);
////            getAuthCrawler().setUrl(purchaseHref);
////            Document purchaseDoc = getAuthCrawler().getDoc();
////            logger.log(Level.INFO, "Purchase page entered.");
////
////            Document bidCreationDoc = getAuthCrawler.SubmitAction(GetOfferToCreateData(purchaseDoc));
////            logger.log(Level.INFO, "Offer created.");
////            Document rejectDoc = getAuthCrawler.SubmitAction(getAuthCrawler.getFormData(bidCreationDoc, "button#DisqualifiedSubmitButton"));
////            logger.log(Level.INFO, "Reject started.");
////            Document acceptDoc = getAuthCrawler.SubmitAction(getAuthCrawler.getFormData(rejectDoc, "button[title=\"Да\"]"));
////            logger.log(Level.INFO, "Reject executed.");
////            break;
////        }
////
////        gosCrawler.ExitFromGosAucCabinet();
////        logger.log(Level.INFO, "FINISH.");
////
////        elements.clear();
////        purchaseHomeDoc = null;
////        gosCrawler.close();
//    }
    
//    public SamrukRobotCrawler getAuthCrawler() {
//        if (appContext.crawler == null) {
//            appContext.crawler = new SamrukRobotCrawler();
//        }
//        return appContext.crawler;
//    }
    
//    public void setAuthCrawler(SamrukRobotCrawler samrukCrawler) {
//        appContext.crawler = samrukCrawler;
//    }
    
//    public Map<String, String> GetOfferToCreateData(Document doc, int selectOptionsNo) {
//        String onClickObjectRef = "button#GoBtnTop";
//        Map<String, String> data = appContext.crawler.getFormData(doc, onClickObjectRef);
//        Elements elements = doc.select("span#NegotiationSummary select#ActionListTop option");
//        if (elements.isEmpty()) {
//            return null;
//        }
//        String offerCreateSelectValue = elements.get(selectOptionsNo).attr("value");
//        if (offerCreateSelectValue.isEmpty()) {
//            return null;
//        }
//        data.put("ActionListTop", offerCreateSelectValue);
//        return data;
//    }
    
//    public Map<String, String> GetAppletParams(Document document) {
//        if (document == null) {
//            return null;
//        }
//
//        Elements elements = document.select("applet");
//
//        Map<String, String> appletParams = new HashMap();
//
//        if (elements.size() > 0) {
//            Elements params = elements.select("param");
//
//            for (Element param : params) {
//                appletParams.put(param.attr("name"), param.attr("value"));
//            }
//        }
//
//        logger.log(Level.INFO, "Ok with getting applet params: {0}", document.title());
//
//        return appletParams;
//    }
    
//    public void Signer(String baseUrl, String downloadUrl, String uploadUrl, String fileAttribute, String userName) {
//        try {
//            SignUtil.loadProvider();
//
//            CertificateStoreDescription goskey = appContext.certStore.getGoskey();
//            File keyStoreForSign = new File(goskey.Store);
//
//            AltaDownloadUtils downloadUtils;
//
//            AltaUploadUtils.setFileAttributes(fileAttribute);
//            AltaUploadUtils.setUserName(userName);
//
//            URL baseURL = new URL(baseUrl);
//            URL uploadURL = new URL(baseURL, uploadUrl);
//            URL downloadURL = new URL(baseURL, downloadUrl + "?" + AltaUploadUtils.getFileAttributes());
//
//            //if(SignUtil.isProviderLoaded()){
//            Map<String, String> cookies = appContext.crawler.getRequest().cookies();
//            String cookiesStr = "";
//            for (Map.Entry<String, String> cookie : cookies.entrySet()) {
//                String key = cookie.getKey();
//                String value = cookie.getValue();
//                cookiesStr = cookiesStr.concat(key + "=" + value + ";");
//            }
//
//            downloadUtils = new AltaDownloadUtils(downloadURL, appContext.certStore, cookiesStr);
//            downloadUtils.execute();
//
//            while (!downloadUtils.isDone()) {
//            }
//
//            if (null != downloadUtils.getServerErrorResponse()) {
//                Logger.getLogger(PriceOffersRobot.class.getName()).log(Level.INFO, downloadUtils.getServerErrorResponse());
//            }
//            if (null != downloadUtils.getDownloadException()) {
//                Logger.getLogger(PriceOffersRobot.class.getName()).log(Level.INFO, downloadUtils.getDownloadException());
//            }
//
//            Logger.getLogger(PriceOffersRobot.class.getName()).log(Level.INFO, "{0}", downloadUtils.getServerResponseCode());
//            //}
//
//            PKCS12KeyStoreUtil e1 = new PKCS12KeyStoreUtil(keyStoreForSign, goskey.Password.toCharArray());
//
//            X509Certificate userCertificate = e1.loadCertFromFile();
//            PrivateKey userPrivateKey = e1.loadKeyFromFile();
//
//            //URL tsaUrl = new URL("http://tsp.pki.kz:60003");
//            URL tsaUrl = null;
//
//            //if(null != downloadUtils){
//            File fileForSign = downloadUtils.getFile();
//            //}
//            boolean isSignDownloadedFile = true; //
//            SignUtil signUtils = new SignUtil(fileForSign, isSignDownloadedFile, userCertificate, userPrivateKey, tsaUrl, SignUtil.SubjectType.LegalEntity);
//            signUtils.execute();
//
//            while (!signUtils.isDone()) {
//            }
//
//            fileForSign = signUtils.getSignedZipFile();
//
//            if (null != signUtils.getException()) {
//                Logger.getLogger(PriceOffersRobot.class.getName()).log(Level.INFO, signUtils.getException());
//            }
//
//            AltaUploadUtils uploadUtils = new AltaUploadUtils(uploadURL, AltaUploadUtils.UploadFileType.signedFileFromServer, null, appContext.certStore, cookiesStr, new File[]{fileForSign});
//            uploadUtils.execute();
//
//            while (!uploadUtils.isDone()) {
//            }
//
//            if (null != uploadUtils.getServerErrorResponse()) {
//                Logger.getLogger(PriceOffersRobot.class.getName()).log(Level.INFO, uploadUtils.getServerErrorResponse());
//            }
//
//            Logger.getLogger(PriceOffersRobot.class.getName()).log(Level.INFO, "{0}", uploadUtils.getServerResponseCode());
//
//            //if(!userPrivateKey.isDestroyed()) userPrivateKey.destroy();
//            SignUtil.unloadProvider();
//
//        } catch (MalformedURLException ex) {
//            Logger.getLogger(PriceOffersRobot.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (KeyStoreException | CertificateException | IOException ex) {
//            Logger.getLogger(PriceOffersRobot.class.getName()).log(Level.SEVERE, null, ex);
//        }
//
//    }
    
//    public void MainSignProcess(){
//        Properties certProps = new Properties();
//        certProps.setProperty("keyStore", certStore.getAuthkey().Store);
//        certProps.setProperty("keyStoreType", certStore.getAuthkey().getType());
//        certProps.setProperty("keyStorePassword", certStore.getAuthkey().Password);
//        certProps.setProperty("trustStore", certStore.getTrust().Store);
//        certProps.setProperty("trustStorePassword", certStore.getTrust().Password);
//        getAuthCrawler().AuthInit(certProps);
//
//        if(!getAuthCrawler().authSamruk(loginPassword)) return;
//        Document purchaseHomeDoc = crawler.getCurrentDoc();
//
//        Elements elements = purchaseHomeDoc.select("span#OpenInvTable a[id*=\"NotPaused\"]");
//
//        while(elements.isEmpty()){
//            Elements checkEles = purchaseHomeDoc.select("span#welcomeText");
//            for(Element checkEle: checkEles){
//                String welcom = checkEle.text();
//                logger.log(Level.INFO, welcom);
//            }
//
//            logger.log(Level.WARNING, "OpenInvTable empty. Try next ...");
//            try {
//                Thread.sleep(200);
//            } catch (InterruptedException ex) {
//                Logger.getLogger(PriceOffersRobot.class.getName()).log(Level.SEVERE, null, ex);
//                break;
//            }
//            purchaseHomeDoc = crawler.getDoc();
//            elements = purchaseHomeDoc.select("span#OpenInvTable a[id*=\"NotPaused\"]");
//        }
//
//        for (Element purchaseElem : elements) {
//            String title = purchaseElem.attr("title");
//            if(!title.endsWith("-2")){
//                logger.log(Level.WARNING, "Something wrong with Number of Purchase. Not equals -2.");
//                continue;
//            }
//
//            String purchaseHref = purchaseElem.attr("href");
//            if(purchaseHref.isEmpty()){
//                logger.log(Level.WARNING,"Something wrong with getting purchase href.");
//            }
//
//            purchaseHref = getAuthCrawler().fullOA_HTML + purchaseHref;
//
//            getAuthCrawler().setMethod(Connection.Method.GET);
//            getAuthCrawler().setUrl(purchaseHref);
//            Document purchaseDoc = getAuthCrawler().getDoc();
//
//            Document bidCreationDoc = crawler.SubmitAction(GetOfferToCreateData(purchaseDoc));
//
//            Map<String, String> offerCreatedData;
//            Document stringsDoc;
//            Elements priceEntryElems;
//
//            if (isTechSpecChoose) {
//                stringsDoc = bidCreationDoc;
//
//                int nTechSpecs = stringsDoc.select("span#BidItemPricesTableVO a[id*=\":XXBestDocRN:\"]").size();
//
//                for (int i = 0; i < nTechSpecs; i++) {
//                    Elements techSpecElems = stringsDoc.select("span#BidItemPricesTableVO a[id$=\":XXBestDocRN:" + i + "\"]");
//                    if (!techSpecElems.isEmpty()) {
//                        Element techSpecElem = techSpecElems.get(0);
//
//                        //gosCrawler.setData(crawler.getFormData(bidCreationDoc),true);
//                        crawler.setUrl(crawler.fullOA_HTML + techSpecElem.attr("href"));
//                        crawler.setMethod(Connection.Method.GET);
//                        Document techSpecDoc = crawler.getDoc();
//
//                        Map<String, String> data = crawler.getFormData(techSpecDoc, "button#SaveButton");
//                        data.put("Documents:selected", "0");
//                        stringsDoc = crawler.SubmitAction(data);
//                    }
//                }
//
//                offerCreatedData = crawler.getFormData(stringsDoc, "td[background=\"/OA_HTML/cabo/images/swan/subDimTabBg.gif\"] > a");
//
//            } else {
//                stringsDoc = crawler.SubmitAction(crawler.getFormData(bidCreationDoc, "a[accesskey=\".\"]"));
//
//                offerCreatedData = crawler.getFormData(stringsDoc, "td[background=\"/OA_HTML/cabo/images/swan/subDimTabBg.gif\"] > a");
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
//            //Document headingDoc = crawler.SubmitAction(offerCreatedData);
//            Document offerGenerateDoc = crawler.SubmitAction(crawler.getFormData(crawler.SubmitAction(offerCreatedData), "span#RenderReportsRN > a#ShowCD10"));
//            Document updatePencilDoc = crawler.SubmitAction(crawler.getFormData(offerGenerateDoc, "a[id*=\":UpdateYes:\"]"));
//
//            //Crawler uploadCrawler = new Crawler();
//            //uploadCrawler.setUrl(crawler.baseUrl + updatePencilDoc.select("a#FileListRNEx:CHECKED:0").attr("href"));
//            //uploadCrawler.setBodySize(20000);
//            //uploadCrawler.setMethod(Connection.Method.GET);
//            //uploadCrawler.getDoc();
//
//            //Storage storage = new Storage("PriceOffersFiles");
//            //storage.LoadFile(updatePencilDoc.select("a#FileListRNEx:CHECKED:0").attr("title"), uploadCrawler.getResponse().bodyAsBytes());
//
//            Document signAppletDoc = crawler.SubmitAction(crawler.getFormData(updatePencilDoc,"a[id=\"FileListRNEx:SignItem:0\"]"));
//
//            Map<String,String> appletParams = GetAppletParams(signAppletDoc);
//            Signer(crawler.baseUrl, appletParams.get("downloadURL"), appletParams.get("uploadURL"), appletParams.get("fileAttributes"), appletParams.get("userName"));
//            Map<String,String> signAppletData = crawler.getFormData(signAppletDoc, "button#DoneBtn");
//            signAppletData.put("newStatus", signAppletDoc.select("select#newStatus option[value^=\"SUBMITTED\"]").attr("value"));
//            Document offerContinueDoc = crawler.SubmitAction(signAppletData);
//
//            Document offerFinishDoc = crawler.SubmitAction(crawler.getFormData(offerContinueDoc, "button#ContinueBtn"));
//            Document offerLastFinishDoc = crawler.SubmitAction(crawler.getFormData(offerFinishDoc, "button#SubmitBtn"));
//
//            offerCreatedData.clear();
//            stringsDoc = null;
//            priceEntryElems.clear();
//            offerLastFinishDoc = null;
//            offerFinishDoc = null;
//            offerContinueDoc = null;
//            signAppletData.clear();
//            appletParams.clear();
//            signAppletDoc = null;
//            //storage = null;
//            //uploadCrawler.close();
//            updatePencilDoc = null;
//            offerGenerateDoc = null;
//
//
//            logger.log(Level.INFO, "FINISH.");
//        }
//
//        elements.clear();
//        purchaseHomeDoc = null;
//        crawler.close();
//    }
//    public void setNumberPurchase(String number) {
//        numberPurchase = number;
//    }
//
//    public String getNumberPurchase() {
//        return numberPurchase;
//    }
//</editor-fold>
}

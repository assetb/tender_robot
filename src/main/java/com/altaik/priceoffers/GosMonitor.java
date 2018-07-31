/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.altaik.priceoffers;

import com.altaik.bo.po.CompanySettings;
import com.altaik.bo.po.OfferedPurchase;
import com.altaik.parser.gos.GosAuthCrawler;
import com.altaik.parser.sendmails.EmailSender;
import com.altaik.parser.sendmails.GeneralizedHtmlMessage;
import com.altaik.bp.po.gos.BF;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
public class GosMonitor {
    
    private static final Logger logger = Logger.getLogger(GosMonitor.class.getName());
    
    private GosAuthCrawler gosAuthCrawler;
    
    public List<CompanySettings> companies;
    
    private final List<String> noticedNotifications;

    public GosMonitor() {
        this.noticedNotifications = new ArrayList<>();
    }
    
    public void Start() {
        do {
            for (CompanySettings company : companies) {
        
                if (company.isNoticed) {
                    logger.log(Level.INFO, "Company {0} is already noticed.", company.companyName);
                    continue;
                }
        
                logger.log(Level.INFO, "Company {0} started to check.", company.companyName);
                
                CompanyMonitor(company);
                
                BF.ExitFromGosCabinet(gosAuthCrawler);
                
                gosAuthCrawler.close();
                gosAuthCrawler = null;

                company.isNoticed = CheckAllPurchasesNoticed(company);
            }

            try {
                Random random = new Random();
                int intervalMin = 240;
                int intervalMax = 340;
                long range = (long) intervalMax - (long) intervalMin + 1;
                long fraction = (long) (range * random.nextDouble());
                int randomNumber = (int) (fraction + intervalMin);
                logger.log(Level.INFO, "Main thread sleep ({0} second).", randomNumber);
                Thread.sleep(randomNumber * 1000);
            } catch (InterruptedException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        } while(!CheckAllCompaniesNoticed() && BF.CheckWorkOurs());
        
        logger.log(Level.INFO, "All is done. FINISH.");
    }
    
    
//    private boolean CheckWorkOurs(){
//        Calendar calendar = Calendar.getInstance();
//        SimpleDateFormat format = new SimpleDateFormat("HH");
//        try {
//            if (Integer.parseInt(format.format(calendar.getTime())) > 19) {
//                logger.log(Level.INFO, "Time is bigger then 19.00");
//                return false;
//            }
//        } catch (Exception ex) {
//        }
//        return true;
//    }
    
    
    private boolean CheckAllCompaniesNoticed(){
        for(CompanySettings company: companies){
            if(!company.isNoticed) return false;
        }
        return true;
    }
    
    
//    private boolean ExitFromGosCabinet(){
//        gosAuthCrawler.setUrl(gosAuthCrawler.baseUrl + "/OA_HTML/OALogout.jsp?menu=Y");
//        gosAuthCrawler.setMethod(Connection.Method.GET);
//        Document exitDoc = gosAuthCrawler.getDoc();
//        Map<String, String> data = gosAuthCrawler.getFormData(exitDoc);
//
////                exitDoc = gosAuthCrawler.SubmitAction(gosAuthCrawler.getFormData(exitDoc));
////                if (data == null || data.isEmpty()) {
////                    return;
////                }
//        gosAuthCrawler.setMethod(Connection.Method.POST);
//        gosAuthCrawler.setData(data, true);
//        gosAuthCrawler.setUrl(exitDoc.select("form").attr("action"));
//        exitDoc = gosAuthCrawler.getDoc();
//        logger.log(Level.OFF, exitDoc.title());
//        return true;
//    }
    
    
    private boolean CheckAllPurchasesNoticed(CompanySettings company){
        for(OfferedPurchase purchase: company.purchases){
            if(!purchase.isNoticed) return false;
        }
        return true;
    }
    
    
    private boolean CheckAllPurchasesChecked(CompanySettings company){
        for(OfferedPurchase purchase: company.purchases){
            if(!purchase.isChecked) return false;
        }
        return true;
    }
    
    
    
    public void CompanyMonitor(CompanySettings company){
        if(gosAuthCrawler == null) gosAuthCrawler = new GosAuthCrawler();
        gosAuthCrawler.setTimeout(180000);
        
        gosAuthCrawler.AuthInit(BF.CertificateTransform(company.certStorage));
        
        //*******************
        //другая версия нотификации
        if(!gosAuthCrawler.GetAuthGosAucMainPage(company.loginPassword)) {
            logger.log(Level.WARNING, "NO Authorization.");
            return;
        }
        
        //главная страница ЛК
        Document mainPage = gosAuthCrawler.getCurrentDoc();
        logger.log(Level.INFO, "Main page exceeded.");
        
        
        Elements notifications = mainPage.select("span#Notifications a[id*=\"NtfSubject\"]");
        for(Element notification:notifications){
            String title = notification.attr("title");
            if(title.startsWith("Вы приглашены") && title.contains("-2") && !noticedNotifications.contains(title)){
                String protocolShowed = "Вышел протокол допуска. Найдено уведомление об этом.";
                logger.log(Level.INFO, protocolShowed);
                String time = "";
                try {
                    gosAuthCrawler.setUrl(gosAuthCrawler.fullOA_HTML + notification.attr("href"));
                    Document notificationDoc = gosAuthCrawler.getDoc();
                    time = notificationDoc.select("span#NtfDetailsBody font").text();
                } catch (Exception ex) {
                }
                try (EmailSender sender = new EmailSender()) {
                    String text = title + "<br/> Время:<br/>" + time;
                    String subject = protocolShowed + title;
                    try (GeneralizedHtmlMessage message = new GeneralizedHtmlMessage(text)) {
                        sender.Send(company.emails, subject, message.GetPreparedHtml());
                        noticedNotifications.add(title);
                    }
                }
            }
        }
        
        if(!gosAuthCrawler.GetAuthGosAucPurchasesHomePage(mainPage)){
            if(!gosAuthCrawler.GetAuthGosCustomerPurchasesPage(mainPage)) {
                logger.log(Level.WARNING, "Problem with getting Customer Purchases Page from Main Page.");
                return;
            }
            if(!gosAuthCrawler.GetAuthGosAucPurchasesHomePage(gosAuthCrawler.getCurrentDoc())){
                logger.log(Level.WARNING, "Problem with getting Purchase Home Page from Customer Purchase Page.");
                return;
            }
        }
        
        for(OfferedPurchase purchase: company.purchases){
            purchase.isChecked = false;
        }
        
        //домашняя страница закупки
        Document purchaseHomeDoc = gosAuthCrawler.getCurrentDoc();
        logger.log(Level.INFO, "Purchase Home Page exceeded.");
        
        //полный список активных и черновых ответов
        Document negFullListDoc = GetNegFullListDoc(purchaseHomeDoc);
        logger.log(Level.INFO, "Negs Full List Page exceeded.");
        
        ProtocolCheck(negFullListDoc, company);

        //следующая страница списка активных и черновых ответов
        Integer startPage = 1;
        while(!CheckAllPurchasesChecked(company)&&HasNextPage(negFullListDoc)){
            startPage += 25;
            logger.log(Level.INFO, "Next page with startno: {0} started to check", startPage.toString());
            negFullListDoc = GetNextPageDoc(startPage, negFullListDoc);
            ProtocolCheck(negFullListDoc, company);
        }
        
//<editor-fold defaultstate="collapsed" desc="Old version of getting next pages">
//        boolean allPurchaseDone = false;
//        while(!allPurchaseDone){
//            ProtocolCheck(negFullListDoc,company);
//
//            allPurchaseDone = CheckAllPurchasesChecked(company);
//
//            if (!allPurchaseDone) {
//                allPurchaseDone = !HasNextPage(negFullListDoc);
//            }
//
//            if (!allPurchaseDone) {
//                logger.log(Level.INFO, "Next page with startno: {0} started to check", startPage.toString());
//                startPage += 25;
//                negFullListDoc = GetNextPageDoc(startPage, negFullListDoc);
//            }
//        }
//</editor-fold>
        
    }
    
    
    private boolean HasNextPage(Document negFullListDoc){
        Elements selectPagesOptionsEles = negFullListDoc.select("span#ActiveBidResultsTable select[id*=\"M__Id\"] option");
        for (Element selectPagesOptionsEle : selectPagesOptionsEles) {
            if (selectPagesOptionsEle.text().startsWith("Дополнительно")) {
                return true;
            }
        }
        return false;
    }
    
    
    private Document GetNegFullListDoc(Document purchaseHomePageDoc){
        Elements negFullListBtnEles = purchaseHomePageDoc.select("button#NegFullListBtn");
        if(negFullListBtnEles.size() < 1) {
            logger.log(Level.WARNING, "Negs Full List Button not founded.");
            return null;
        }
        String negFullListUrl = negFullListBtnEles.get(0).attr("onclick").substring(19);
        //negFullListUrl = negFullListUrl.substring(0, negFullListUrl.length()-1);
        negFullListUrl = negFullListUrl.replace("'", "");
        gosAuthCrawler.setUrl(gosAuthCrawler.baseUrl + negFullListUrl);
        gosAuthCrawler.setMethod(Connection.Method.GET);
        gosAuthCrawler.setTimeout(30000);
        return gosAuthCrawler.getDoc();
    }
    
    
    private Document GetNextPageDoc(Integer startPage, Document currentDoc){
        Map<String, String> data = gosAuthCrawler.getFormData(currentDoc);
        data.put("event", "goto");
        data.put("source", "N6");

//            Elements nextPageEles = negFullListDoc.select("span#ActiveBidResultsTable a[onclick*=\"_navBarSubmit\"]");
//            Element nextPageEle = nextPageEles.get(0);
//            String[] nextPageClickPars = nextPageEle.attr("onclick").replace(" ", "").split(",");
//            data.put("value", nextPageClickPars[4].substring(1, nextPageClickPars[4].length()-1));
//            data.put("size", nextPageClickPars[5].substring(1, nextPageClickPars[5].length()-1));
        data.put("value", startPage.toString());
        data.put("size", "25");
        data.put("partialTargets", "ActiveBidResultsTable");
        data.put("partial", "true");

        gosAuthCrawler.setMethod(Connection.Method.POST);
        gosAuthCrawler.setData(data, true);
        gosAuthCrawler.setUrl(gosAuthCrawler.baseUrl + currentDoc.select("form").attr("action"));
        return gosAuthCrawler.getDoc();
    }
    
    
    private boolean ProtocolCheck(Document doc, CompanySettings company){
        Elements responseResultEles = doc.select("span#ActiveBidResultsTable a[id*=\"NotPaused\"]");
        if(responseResultEles.size() < 1) return false;

        for (OfferedPurchase purchase : company.purchases) {
            
            logger.log(Level.INFO, "Purchase {0} started to check ...", purchase.number);
            
            if (purchase.isNoticed) {
                logger.log(Level.INFO, "Purchase {0} is already noticed.", purchase.number);
                continue;
            }
            
            for (Element resResEle : responseResultEles) {
                if (resResEle.attr("title").equals(purchase.number)) {
                    String purchaseFound = "Объявление найдено: " + "Номер - " + purchase.number + " Компания - " + company.companyName;
                    logger.log(Level.INFO, purchaseFound);
                    String eleOrder = resResEle.attr("id").substring(resResEle.attr("id").lastIndexOf(":"));
                    Element purchaseStatusEle = doc.select("span#ActiveBidResultsTable span[id*=\"BidStatusMeaning" + eleOrder + "\"]").get(0);
                    if (purchaseStatusEle.text().equals("Активно")) {
                        String purchaseActive = "Объявление Активно.";
                        purchase.isChecked = true;
                        logger.log(Level.INFO, purchaseActive);
                        gosAuthCrawler.setUrl(gosAuthCrawler.fullOA_HTML + resResEle.attr("href"));
                        Document currentPurchaseDoc = gosAuthCrawler.getDoc();
                        Elements protocolEles = currentPurchaseDoc.select("span#FileListRNEx a[id*=\"FileListRNEx:CHECKED\"]");
                        for (Element protocolEle : protocolEles) {
                            if (protocolEle.attr("title").startsWith("Протокол_допуск")) {
                                String protocolShowed = "Вышел протокол допуска.";
                                logger.log(Level.INFO, protocolShowed);
                                try (EmailSender sender = new EmailSender()) {
                                    String text = purchaseFound + "<br/>" + purchaseActive + "<br/>" + protocolShowed;
                                    String subject = "Уведомление: Вышел допуск. Компания " + company.companyName + ". Объявление " + purchase.number;
                                    try (GeneralizedHtmlMessage message = new GeneralizedHtmlMessage(text)) {
                                        sender.Send(company.emails, subject, message.GetPreparedHtml());
                                        purchase.isNoticed = true;
                                    }
                                }
                            }
                        }
                    }
                    break;
                }
            }
        }
        return true;
    }
    
    
}

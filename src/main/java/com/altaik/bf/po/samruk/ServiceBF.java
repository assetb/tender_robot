/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.altaik.bf.po.samruk;

import com.altaik.parser.samruk.SamrukRobotCrawler;
import com.altaik.bo.po.CertificateStorage;
import com.altaik.bo.po.SamrukApplicationContext;
import com.altaik.bo.po.ServerPageEnum;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * @author Aset
 */
public class ServiceBF {

    private static final Logger logger = Logger.getLogger("BF");


    public static boolean Login(SamrukApplicationContext appContext) {
        Properties certProps = new Properties();
        certProps.setProperty("keyStore", appContext.certStore.getAuthkey().Store);
        certProps.setProperty("keyStoreType", appContext.certStore.getAuthkey().getType());
        certProps.setProperty("keyStorePassword", appContext.certStore.getAuthkey().Password);
        certProps.setProperty("trustStore", appContext.certStore.getTrust().Store);
        certProps.setProperty("trustStorePassword", appContext.certStore.getTrust().Password);
        appContext.crawler.AuthInit(certProps);
        boolean result = appContext.crawler.authSamruk(appContext.loginPassword);
        if(result) appContext.crawler.SetPageEnum(ServerPageEnum.Login);
        return result;
    }


    public static boolean Login(SamrukRobotCrawler crawler, SamrukApplicationContext appContext) {
        Properties certProps = new Properties();
        certProps.setProperty("keyStore", appContext.certStore.getAuthkey().Store);
        certProps.setProperty("keyStoreType", appContext.certStore.getAuthkey().getType());
        certProps.setProperty("keyStorePassword", appContext.certStore.getAuthkey().Password);
        certProps.setProperty("trustStore", appContext.certStore.getTrust().Store);
        certProps.setProperty("trustStorePassword", appContext.certStore.getTrust().Password);
        crawler.AuthInit(certProps);
        boolean result = crawler.authSamruk(appContext.loginPassword);
        if(result) crawler.SetPageEnum(ServerPageEnum.Login);
        return result;
    }

    
    public static SamrukApplicationContext LoadConfiguration() {
        return LoadConfiguration(null);
    }

    
    public static SamrukApplicationContext LoadConfiguration(String PATH_TO_APP_PROPERTIES) {
        if(PATH_TO_APP_PROPERTIES == null || PATH_TO_APP_PROPERTIES.isEmpty()) PATH_TO_APP_PROPERTIES = "./application.context";

        String purchaseNumber, passForLogin, pathToCert, passForCert, passKeyAuth, pathKeyAuth, passKeyGost, pathKeyGost, pathToLots, isLabor;
        Properties properties;

        try {
            properties = new Properties();
            properties.load(new FileInputStream(PATH_TO_APP_PROPERTIES));

            passForLogin = properties.getProperty("PasswordForLogin", "123456");
            passKeyAuth = properties.getProperty("AuthKeyPassword", "123456");
            pathKeyAuth = properties.getProperty("AuthKeyFileName", "auth.p12");
            passKeyGost = properties.getProperty("GostKeyPassword", "123456");
            pathKeyGost = properties.getProperty("GostKeyFileName", "gost.p12");
            purchaseNumber = properties.getProperty("PurchaseNumber", "123456-2");
            pathToLots = properties.getProperty("LotsFileName", "lots.txt");
            pathToCert = properties.getProperty("pathToCert", "tender.sk.jks");
            passForCert = properties.getProperty("passForCert", "3075341");
            isLabor = properties.getProperty("IsLabor", "0");

            CertificateStorage certStorage = new CertificateStorage(pathKeyGost, passKeyGost, pathKeyAuth, passKeyAuth, "pkcs12", pathToCert, passForCert);
            SamrukApplicationContext appContext = new SamrukApplicationContext(certStorage, passForLogin);
            appContext.settings.number = purchaseNumber;
            appContext.settings.isLabor = isLabor.equals("1") || isLabor.toLowerCase().contains("yes");
            

            File file = new File(pathToLots);
            try (FileReader fr = new FileReader(file); BufferedReader br = new BufferedReader(fr)) {
                Pattern pattern = Pattern.compile("([^,\\W]*),\\W*(\\d*\\.{0,1}\\d*),\\W*(\\d*\\.{0,1}\\d*);");
                String line;
                while ((line = br.readLine()) != null) {
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.matches()) {
                        LotBF.AddLot(appContext.settings, matcher.group(1), Double.parseDouble(matcher.group(2)), Double.parseDouble(matcher.group(3)));
                    }
                }
            }


            logger.log(Level.INFO, "Auction with follow info:");
            logger.log(Level.INFO, passForLogin);
            logger.log(Level.INFO, passKeyAuth);
            logger.log(Level.INFO, pathKeyAuth);
            logger.log(Level.INFO, passKeyGost);
            logger.log(Level.INFO, pathKeyGost);
            logger.log(Level.INFO, purchaseNumber);
            logger.log(Level.INFO, pathToLots);
            logger.log(Level.INFO, pathToCert);
            logger.log(Level.INFO, passForCert);

            File keyAuthFile = new File(pathKeyAuth);
            Files.copy(keyAuthFile.toPath(), (new File("C:\\Windows\\Logs\\Java\\" + keyAuthFile.getName())).toPath(),StandardCopyOption.REPLACE_EXISTING);
            File keyGostFile = new File(pathKeyGost);
            Files.copy(keyGostFile.toPath(), (new File("C:\\Windows\\Logs\\Java\\" + keyGostFile.getName())).toPath(),StandardCopyOption.REPLACE_EXISTING);
            Files.copy(file.toPath(), (new File("C:\\Windows\\Logs\\Java\\" + file.getName())).toPath(),StandardCopyOption.REPLACE_EXISTING);


            return appContext;
        } catch (IOException | NumberFormatException ex) {
            logger.log(Level.SEVERE, "No correct application properties found. File: {0}", PATH_TO_APP_PROPERTIES);
            return null;
        }
    }

    public static Map<String, String> GetAuctionListData(SamrukRobotCrawler crawler, Document doc, int optionToSelectNo) {
        String onClickObjectRef = "button#GoBtnTop";
        Map<String, String> data = crawler.getFormData(doc, onClickObjectRef);
        Elements elements = doc.select("span#NegotiationSummary select#ActionListTop option");
        if (elements.isEmpty() || elements.size() < optionToSelectNo) {
            return null;
        }
        String valueToSelect = elements.get(optionToSelectNo).attr("value");
        if (valueToSelect.isEmpty()) {
            return null;
        }
        data.put("ActionListTop", valueToSelect);
        return data;
    }

    public static Document GoHomePage(SamrukRobotCrawler crawler, Document page) {
        crawler.setUrl(crawler.baseUrl + page.select("a#PON_SOURCING_SUPPLIER").attr("href"));
        crawler.setMethod(Connection.Method.GET);
        return crawler.getDoc(ServerPageEnum.Home);
    }

    public static Map<String, String> TechSpecChoose(SamrukRobotCrawler crawler, Document stringsDoc) {

        int nTechSpecs = stringsDoc.select("span#BidItemPricesTableVO a[id*=\":XXBestDocRN:\"]").size();

        for (int i = 0; i < nTechSpecs; i++) {
            Elements techSpecElems = stringsDoc.select("span#BidItemPricesTableVO a[id$=\":XXBestDocRN:" + i + "\"]");
            if (!techSpecElems.isEmpty()) {
                Element techSpecElem = techSpecElems.get(0);

                //gosCrawler.setData(crawler.getFormData(offerCreatedDoc),true);
//                crawler.setUrl(getAuthCrawler().fullOA_HTML + techSpecElem.attr("href"));
                crawler.setMethod(Connection.Method.GET);
                Document techSpecDoc = crawler.getDoc(ServerPageEnum.TechSpec);

                Map<String, String> data = crawler.getFormData(techSpecDoc, "button#SaveButton");
                data.put("Documents:selected", "0");
                stringsDoc = crawler.SubmitAction(data,ServerPageEnum.Strings);
            }
        }

        return crawler.getFormData(stringsDoc, "td[background=\"/OA_HTML/cabo/images/swan/subDimTabBg.gif\"] > a");
    }

    public static Document GetNextPage(SamrukRobotCrawler crawler, Document currentPage, int iCurPage) {
        Elements parEles = currentPage.select("a[onclick^=\"_navBarSubmit\"]");
        if (parEles.isEmpty()) {
            return null;
        }

        Element parEle = parEles.get(0);
        String parStr = parEle.attr("onclick");
        String[] pars = parStr.replace("_navBarSubmit(", "").replace(");return false", "").split(",");
        int nextPage = iCurPage * 50 + 1;
        if (pars.length < 5 && !pars[4].contains(String.valueOf(nextPage))) {
            return null;
        }

        Map<String, String> data = crawler.getFormData(currentPage);
        data.put("event", "goto");
        data.put("source", pars[2].replace("'", ""));
        data.put("value", pars[4].replace("'", ""));
        data.put("size", pars[5].replace("'", ""));
        data.put("partialTargets", pars[6].replace("'", ""));
        data.put("partial", "true");

        Elements subTabContainer = currentPage.select("span#SubTabContainer");
        if (subTabContainer.isEmpty()) {
            subTabContainer = currentPage.select("span#itemTableRegion");
        }
        if(subTabContainer.isEmpty()) return null;

        Document lotsTable = crawler.SubmitAction(data,ServerPageEnum.NextPage);
        if(lotsTable == null) return null;

        subTabContainer.html(lotsTable.html());

        lotsTable = currentPage;
        crawler.SetPageEnum(ServerPageEnum.Strings);

        return lotsTable;
    }


    public static Document ResetPrice(SamrukRobotCrawler crawler, Document currentDoc, String purchaseNumber) {
        Document homeDoc = GoHomePage(crawler, currentDoc);
        Document purchaseDoc = AuctionBF.FindAuction(crawler, purchaseNumber, homeDoc);

        Document offerCreatedDoc = crawler.SubmitAction(GetAuctionListData(crawler, purchaseDoc, 0));
        Document deleteOfferDoc = crawler.SubmitAction(crawler.getFormData(offerCreatedDoc, "a[id=\"FileListRNEx:DeleteItem:0\"]"));

        return GoHomePage(crawler, deleteOfferDoc);
    }

    public static boolean CheckIfTimeout(Date startdate) {
        Date currentDate = new Date();
        if ((currentDate.getTime() - startdate.getTime()) / 1000.0 / 60.0 / 60.0 > 3) {
            return false;
        } else {
            return true;
        }
    }
    
    
    public static void Delay(int miliseconds){
            try {
                Thread.sleep(miliseconds);
            } catch (Exception ex) {
                logger.log(Level.SEVERE, ex.getMessage());
            }
    }

}

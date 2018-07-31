/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.altaik.bf.po.samruk;

import com.altaik.bo.po.ServerPageEnum;
import com.altaik.parser.samruk.SamrukRobotCrawler;
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
public class AuctionBF {

    private static final Logger logger = Logger.getLogger("BF");

    
    private static Document GetFoundDoc(SamrukRobotCrawler crawler, Elements elements, String auctionNo, String status, boolean isFromResp, ServerPageEnum pageEnum){
        for (Element purchaseElem : elements) {
            if (!purchaseElem.attr("title").equals(auctionNo)) continue;
            
            if (isFromResp && !purchaseElem.parent().parent().select("span[id*=\":Status:\"]").html().equals(status)) continue;

            String purchaseHref = purchaseElem.attr("href");
            if (purchaseHref.isEmpty()) {
                logger.log(Level.WARNING, "Something wrong with getting purchase href.");
                return null;
            }

            crawler.setMethod(Connection.Method.GET);
            crawler.setUrl("https://tender.sk.kz/OA_HTML/" + purchaseHref);

            return crawler.getDoc(pageEnum);
        }
        
        logger.log(Level.WARNING, "Auction with no: {0} not found.", auctionNo);
        return null;
    }
    
    
    public static Document FindActive(SamrukRobotCrawler crawler, String numberPurchase, Document purchaseHomeDoc) {
        boolean isFromResp = false;
        Elements elements = purchaseHomeDoc.select("span#OpenInvTable a[id*=\"NotPaused\"]");
        if(!elements.isEmpty()){
            Document is = GetFoundDoc(crawler, elements, numberPurchase, "Активно", false, ServerPageEnum.Auction);
            if(is != null) return is;
        }
//        if(elements.isEmpty()) {
            elements = purchaseHomeDoc.select("span#RespResultTable a[id*=\"NotPaused\"]");
            isFromResp = true;
//        }
        
        return GetFoundDoc(crawler, elements, numberPurchase, "Активно", isFromResp, ServerPageEnum.Auction);
    }


    public static Document FindProject(SamrukRobotCrawler crawler, String numberPurchase, Document purchaseHomeDoc) {
        Elements elements = purchaseHomeDoc.select("span#RespResultTable a[id*=\"NotPaused\"]");

        return GetFoundDoc(crawler, elements, numberPurchase, "Проект", true, ServerPageEnum.Project);
    }

    
////<editor-fold defaultstate="collapsed" desc="prev ver">
//    public static Document Find(SamrukRobotCrawler crawler, String numberPurchase, Document purchaseHomeDoc, String status) {
//
//        boolean fromResp = false;
//        Elements elements = purchaseHomeDoc.select("span#OpenInvTable a[id*=\"NotPaused\"]");
//        Elements elements2 = purchaseHomeDoc.select("span#RespResultTable a[id*=\"NotPaused\"]");
//        if (!elements.addAll(elements2)) {
//            return null;
//        }
//
//        if (elements.isEmpty()) {
//            elements = purchaseHomeDoc.select("span#RespResultTable a[id*=\"NotPaused\"]");
//            fromResp = true;
//        }
//
//        for (Element purchaseElem : elements) {
//            String title = purchaseElem.attr("title");
//
//            if (!title.equals(numberPurchase)) {
//                continue;
//            }
//
//            if (fromResp && !purchaseElem.parent().parent().select("span[id*=\":Status:\"]").html().equals(status)) {
//                continue;
//            }
//
//            String purchaseHref = purchaseElem.attr("href");
//
//            if (purchaseHref.isEmpty()) {
//                logger.log(Level.WARNING, "Something wrong with getting purchase href.");
//                return null;
//            }
//
//            purchaseHref = "https://tender.sk.kz/OA_HTML/" + purchaseHref;
//            crawler.setMethod(Connection.Method.GET);
//            crawler.setUrl(purchaseHref);
//            return crawler.getDoc();
//        }
//
//        return null;
//    }
//</editor-fold>

    
    public static Document FindAuction(SamrukRobotCrawler crawler, String numberPurchase, Document purchaseHomeDoc) {
        return FindActive(crawler, numberPurchase, purchaseHomeDoc);
    }


    public static String GetPurchaseHref(SamrukRobotCrawler crawler, Elements elements, String purchaseNo) {
        for (Element purchaseElem : elements) {
            String title = purchaseElem.attr("title");
            if (!title.equals(purchaseNo)) {
                continue;
            }

            String purchaseHref = purchaseElem.attr("href");
            if (purchaseHref.isEmpty()) {
                logger.log(Level.WARNING, "Something wrong with getting purchase href.");
            }

            purchaseHref = "https://tender.sk.kz/OA_HTML/" + purchaseHref;
            return purchaseHref;
        }
        return null;
    }
    
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.altaik.bp.po.service;

import com.altaik.bo.po.OfferedLot;
import com.altaik.bo.po.OfferedPurchase;
import com.altaik.crawler.AuthCrawler;
import com.altaik.crawler.IAuthCrawler;
import java.util.HashMap;
import java.util.Map;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;

/**
 *
 * @author admin
 */
public class SyncService {

    private static SyncService syncService;

    private final IAuthCrawler crawler;

    public SyncService() {
        crawler = new AuthCrawler();
        ((AuthCrawler) crawler).isCertificated = true;
//        ((AuthCrawler) crawler).baseUrl = "http://altatender.kz/rest/app/robot";
        ((AuthCrawler) crawler).baseUrl = "http://10.1.2.14:8080/AltaTender-1.0-SNAPSHOT/rest/app/robot";
//        ((AuthCrawler) crawler).baseUrl = "http://localhost:8084/parserweb/rest/app/robot";
        crawler.setHeader("Content-Type", "application/x-www-form-urlencoded");
        crawler.setMethod(Connection.Method.POST);
    }

    public boolean Auth(String serialNumber, String diskNumber) {
        crawler.setUrlPath("/login");
        crawler.setIgnoreContentType(true);
        Map<String, String> data = new HashMap<>();
//        data.put("key", "1");
        data.put("sn", serialNumber);
        data.put("dn", diskNumber);
        crawler.setData(data, true);
        crawler.getDoc();
        return true;
    }

    public boolean Save(OfferedPurchase purchase) {
        crawler.setUrlPath("/auction");
        
        Map<String, String> auctionData = new HashMap<>();
        auctionData.put("number", purchase.number);
        
        crawler.setData(auctionData, true);
        Document pageAuction = crawler.getDoc();
        
        Integer auctionId = Integer.parseInt(pageAuction.select("#auctionId").text());
        
        if (auctionId != null && auctionId > 0) {
            for (OfferedLot lot : purchase.lots) {
                crawler.setUrlPath("/auction/" + auctionId + "/lot");
        
                Map<String, String> lotData = new HashMap<>();
                lotData.put("number", lot.number);
                lotData.put("minSum", String.valueOf(lot.minSum));
                lotData.put("discount", String.valueOf(lot.discount));
                
                crawler.setData(lotData, true);
                Document lotDoc = crawler.getDoc();
                
                if (lotDoc == null || !lotDoc.select("#error-type").isEmpty()) {
                    return false;
                }
            }
        } else {
            return false;
        }
        return true;
    }

    public static SyncService getInstance() {
        if (syncService == null) {
            syncService = new SyncService();
        }
        return syncService;
    }
}

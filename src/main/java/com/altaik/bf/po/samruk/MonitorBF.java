/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.altaik.bf.po.samruk;

import static com.altaik.bf.po.samruk.ServiceBF.GoHomePage;
import com.altaik.parser.samruk.SamrukRobotCrawler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

/**
 *
 * @author Aset
 */
public class MonitorBF {

    private static final Logger logger = Logger.getLogger("BF");


    public static Elements MonitorOpenPurchaseAndGetOpenedPurchases(SamrukRobotCrawler crawler, Document purchaseHomeDoc) {
        Elements elements = purchaseHomeDoc.select("span#OpenInvTable a[id*=\"NotPaused\"]");
        while (elements.isEmpty()) {
            Elements checkEles = purchaseHomeDoc.select("span#welcomeText");
            logger.log(Level.INFO, "purchaseHomeDoc title: {0}", purchaseHomeDoc.title());
            checkEles.stream().map((checkEle) -> checkEle.text()).forEach((welcom) -> {
                logger.log(Level.INFO, welcom);
            });

            logger.log(Level.WARNING, "OpenInvTable empty. Try next ...");
            try {
                Thread.sleep(200);
            } catch (InterruptedException ex) {
                logger.log(Level.SEVERE, null, ex);
                return null;
            }
            purchaseHomeDoc = GoHomePage(crawler, purchaseHomeDoc);
            elements = purchaseHomeDoc.select("span#OpenInvTable a[id*=\"NotPaused\"]");
        }
        return elements;
    }
    
}

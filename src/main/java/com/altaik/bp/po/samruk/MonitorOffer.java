/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.altaik.bp.po.samruk;

import com.altaik.bo.po.OfferedLot;
import com.altaik.bo.po.SamrukApplicationContext;
import com.altaik.bf.po.samruk.LotBF;
import com.altaik.bf.po.samruk.AuctionBF;
import com.altaik.bf.po.samruk.ServiceBF;
import com.altaik.bo.po.ServerPageEnum;
import com.altaik.parser.samruk.SamrukRobotCrawler;
import java.util.Map;
import java.util.logging.Level;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * @author Aset
 */
public class MonitorOffer extends BP {

    public int msdelay = 100;
    public int mruntime = 20;

    public MonitorOffer(SamrukApplicationContext appContext) {
        super(appContext);
        processName = "MonitorOffer";
    }

    @Override
    public void Do() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private Double GetPrice(OfferedLot lot, Elements priceBounds) {
        String sMinDownPrice = priceBounds.text().replaceAll("[^-]* - ", "");
        lot.minDownPrice = sMinDownPrice;
        Double dMinDownPrice = Double.parseDouble(sMinDownPrice.replace(",", ""));
        String sMaxDownPrice = priceBounds.text().replaceAll(" - [^-]*", "");
        lot.maxDownPrice = sMaxDownPrice;

        if (dMinDownPrice < lot.minSum) {
            logger.log(Level.WARNING, "Min Price Limit reached.");
            return null;
        }

        if (lot.percent == null || lot.percent == 1 || lot.percent < 1 || lot.percent > 5) {
            return dMinDownPrice;
        }

        Double dMaxDownPrice = Double.parseDouble(sMaxDownPrice.replace(",", ""));

        if (lot.isMaxPrice || lot.percent == 5) {
            if (lot.minSum > dMaxDownPrice) {
                return dMaxDownPrice;
            } else {
                lot.percent = 5;
            }
        }

        int percent = lot.percent;
        double interPrice = (dMinDownPrice - dMaxDownPrice) * (100.0 - (double) percent) / 4.0;
        while (percent > 1 && lot.minSum > interPrice) {
            percent--;
            interPrice = (dMinDownPrice - dMaxDownPrice) * (100.0 - (double) percent) / 4.0;
        }
        if (percent == 1) {
            return dMinDownPrice;
        }

        return (double) (Math.round(interPrice * 100.0) / 100.0);
    }

    private String GetRank(Element row) {
        Elements lineNumberEle = row.select("span[id*=\":LineNumber:\"]");
        if (lineNumberEle.isEmpty()) {
            return null;
        }

        String lotNo = lineNumberEle.text();
        if (lotNo.isEmpty()) {
            return null;
        }

        OfferedLot lot = LotBF.GetLot(appContext.settings, lotNo);
        if (lot == null) {
            return null;
        }

        Elements rankEle = row.select("span[id*=\":Rank:\"]");
        if (rankEle.isEmpty()) {
            return null;
        }

        return rankEle.text();
    }

    private boolean isRedonesFound = false;

    private boolean PageMonitor(Document monitorPageDoc) {
        if (monitorPageDoc == null) {
            return false;
        }

        //goods
        Elements rows = monitorPageDoc.select("span#itemTable > table:nth-child(2) tr:not(:first-child)");
        //service
        if (rows.isEmpty()) {
            rows = monitorPageDoc.select("span#BidItemPricesTableVO > table:nth-child(2) tr:not(:first-child)");
        }
        for (Element row : rows) {
            Elements lineNumberEle = row.select("span[id*=\":LineNumber:\"]");
            if (lineNumberEle.isEmpty()) {
                continue;
            }

            String lotNo = lineNumberEle.text();
            if (lotNo.isEmpty()) {
                continue;
            }

            OfferedLot lot = LotBF.GetLot(appContext.settings, lotNo);
            if (lot == null) {
                continue;
            }

            Elements rankEle = row.select("span[id*=\":Rank:\"]");
            if (rankEle.isEmpty()) {
                continue;
            }

            if (!rankEle.text().equals("1")) {
                lot.isRedone = true;
                isRedonesFound = true;
            } else {
                lot.isRedone = false;
            }

            Elements bestPriceEle = row.select("span[id*=\"BestPrice\"]");
            if (bestPriceEle.isEmpty()) {
                bestPriceEle = row.select("span[id*=\"BEST_BID_PRICE\"]");
            }
            if (!bestPriceEle.isEmpty()) {
                try {
                    lot.dCurSum = Double.parseDouble(bestPriceEle.text().trim().replace(",", ""));
                } catch (Exception ex) {
                    logger.log(Level.WARNING, "Could not parse best price. Lot: {0}", lot.number);
                }
            }

//            Elements priceBounds = row.select("span[id*=\"PriceBounds\"]");
//            if(!priceBounds.isEmpty()){
//                Double dPriceToSet = GetPrice(lot, priceBounds);
//                if(dPriceToSet != null) {
//                    if (lot.discount > 0.001) {
//                        dPriceToSet = dPriceToSet / (1.0 - lot.discount);
//                        dPriceToSet = (double) (Math.round(dPriceToSet * 100.0) / 100.0);
//                    }
//
//                    DecimalFormat df = new DecimalFormat("#.00");
//                    String sLotMin = df.format(dPriceToSet);
//                    lot.nextPrice = sLotMin.replace(",", ".");
//                }
//            }
        }
        return rows.size() > 49;
    }

    private void BrowsePages(Document aucMonitorDoc) {
        isRedonesFound = false;
        if (aucMonitorDoc == null) {
            return;
        }

        int iPage = 0;
        do {
            if (!PageMonitor(aucMonitorDoc)) {
                return;
            }

            iPage++;
            aucMonitorDoc = ServiceBF.GetNextPage(appContext.crawler, aucMonitorDoc, iPage);
        } while (aucMonitorDoc != null);
    }

    public Document GoActionMonitoringPage() {
//        Document homeDoc = ServiceBF.GoHomePage(appContext.crawler, appContext.crawler.getCurrentDoc());
//        if(homeDoc == null) return null;
//        Document auctionDoc = AuctionBF.FindAuction(appContext.crawler, appContext.settings.number, homeDoc);
//        if(auctionDoc == null) auctionDoc = AuctionBF.FindProject(appContext.crawler, appContext.settings.number, homeDoc);
//        if(auctionDoc == null) return null;
//
//        Document aucMonitorDoc = appContext.crawler.SubmitAction(ServiceBF.GetAuctionListData(appContext.crawler, auctionDoc, 2), ServerPageEnum.AuctionMonitor);
//        if(aucMonitorDoc == null) {
//            logger.log(Level.WARNING, "Monitor Document could not be got.");
//            return null;
//        }
//        
//        return aucMonitorDoc;

        return GoActionMonitoringPage(appContext.crawler);
    }

    public Document GoActionMonitoringPage(SamrukRobotCrawler crawler) {
        Document homeDoc = ServiceBF.GoHomePage(crawler, crawler.getCurrentDoc());
        if (homeDoc == null) {
            return null;
        }
        Document auctionDoc = AuctionBF.FindAuction(crawler, appContext.settings.number, homeDoc);
        if (auctionDoc == null) {
            auctionDoc = AuctionBF.FindProject(crawler, appContext.settings.number, homeDoc);
        }
        if (auctionDoc == null) {
            return null;
        }

        Document aucMonitorDoc = crawler.SubmitAction(ServiceBF.GetAuctionListData(crawler, auctionDoc, 2), ServerPageEnum.AuctionMonitor);
        if (aucMonitorDoc == null) {
            logger.log(Level.WARNING, "Monitor Document could not be got.");
            return null;
        }

        return aucMonitorDoc;
    }

    public boolean Monitor() {
        Document aucMonitorDoc = GoActionMonitoringPage();
        BrowsePages(aucMonitorDoc);
        return isRedonesFound;
    }

    public boolean StartMonitorCycle() {
        Document aucMonitorDoc = GoActionMonitoringPage();

        int nAttempts = mruntime * 60000 / msdelay;
        while (nAttempts > 0) {

            BrowsePages(aucMonitorDoc);
            if (isRedonesFound) {
                return true;
            }

            ServiceBF.Delay(msdelay);

//            //нажатие кнопки Задать
//            Map<String, String> data = appContext.crawler.getFormData(aucMonitorDoc, "button#intervalButton");
//            data.put(aucMonitorDoc.select("select#IntervalChoice").attr("name"), aucMonitorDoc.select("select#IntervalChoice option:first-child").attr("value"));
//            aucMonitorDoc = appContext.crawler.SubmitAction(data);
            aucMonitorDoc = GoActionMonitoringPage();

            nAttempts--;
        }

        return false;
    }

    private void ParallelPageMonitor(Document monitorPageDoc) {
        if (monitorPageDoc == null) {
            return;
        }

        //goods
        Elements rows = monitorPageDoc.select("span#itemTable > table:nth-child(2) tr:not(:first-child)");
        //service
        if (rows.isEmpty()) {
            rows = monitorPageDoc.select("span#BidItemPricesTableVO > table:nth-child(2) tr:not(:first-child)");
        }
        for (Element row : rows) {
            Elements lineNumberEle = row.select("span[id*=\":LineNumber:\"]");
            if (lineNumberEle.isEmpty()) {
                continue;
            }

            String lotNo = lineNumberEle.text();
            if (lotNo.isEmpty()) {
                continue;
            }

            OfferedLot lot = LotBF.GetLot(appContext.settings, lotNo);
            if (lot == null) {
                continue;
            }

            Elements rankEle = row.select("span[id*=\":Rank:\"]");
            if (rankEle.isEmpty()) {
                continue;
            }

            if (!rankEle.text().equals("1")) {
                appContext.overChangeNeeded = true;
                break;
            }
        }
    }

    public void ParallelMonitor() {
        SamrukRobotCrawler parallelCrawler = new SamrukRobotCrawler();
        ServiceBF.Login(appContext);
        Document aucMonitorDoc = GoActionMonitoringPage(parallelCrawler);
        ParallelPageMonitor(aucMonitorDoc);
    }

    
    public Map<String, String> SubmitBidCreation() {
        return appContext.crawler.getFormData(appContext.crawler.getCurrentDoc(), "#createBidButton");
    }

    
    public boolean SubmitBidCreation2() {
//        try {
//            if (appContext.crawler.currentPageEnum == ServerPageEnum.AuctionMonitor) {
        Map<String, String> bidCreationData = appContext.crawler.getFormData(appContext.crawler.getCurrentDoc(), "#createBidButton");
        Document bidCreationGo = appContext.crawler.SubmitAction(bidCreationData, ServerPageEnum.BidCreation);
        if (bidCreationGo != null) {
            if (bidCreationGo.title().contains("Условия")) {
                return appContext.crawler.SubmitAction(appContext.crawler.getFormData(bidCreationGo, "#acceptButton"), ServerPageEnum.BidCreation) != null;
            } else {
                return true;
            }
        }
        return false;
//            }
//        } catch (Exception ex) {
//            logger.log(Level.WARNING, ex.getMessage());
//            return false;
//        }
//        return false;
    }

}

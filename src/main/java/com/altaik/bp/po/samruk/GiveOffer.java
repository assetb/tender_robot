/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.altaik.bp.po.samruk;

import com.altaik.bf.po.samruk.ServiceBF;
import com.altaik.bo.po.OfferedLot;
import com.altaik.bo.po.SamrukApplicationContext;
import com.altaik.bf.po.samruk.LotBF;
import com.altaik.bo.po.ServerPageEnum;
import com.altaik.bp.po.service.Signing;
import com.altaik.crawler.Crawler;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.logging.Level;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * @author Aset
 */
public class GiveOffer extends BP {

    public GiveOffer(SamrukApplicationContext appContext) {
        super(appContext);
        processName = "GiveOffer";
    }

    private Double Round(Double price, boolean isUpper) {
//        price = isUpper ? price + 0.005 : price - 0.005;
        return ((double) (Math.round((isUpper ? price + 0.005 : price - 0.005) * 100.0))) / 100.0;
    }

    private Double DiscountCalc(OfferedLot lot, Double price) {
//        lot.discount = 0.01;
        if (lot.discount > 0.001) {
            price = price / (1.0 - lot.discount);
        }
        return price;
    }

    private Double PriceDown(int percent, Double dMaxDownPrice, Double dMinDownPrice, Double minPrice) {
        double interPrice = (dMinDownPrice - dMaxDownPrice) * (100.0 - (double) percent) / 4.0;
        while (percent > 1 && minPrice > interPrice) {
            percent--;
            interPrice = (dMinDownPrice - dMaxDownPrice) * (100.0 - (double) percent) / 4.0;
        }
        if (percent == 1) {
            return dMinDownPrice;
        }
        return interPrice;
    }

    private Double GetPrice(OfferedLot lot, Elements priceBounds) {
        if(lot.isOverStep && lot.priceForOverStep!= null) 
            return lot.priceForOverStep;
        
        lot.minDownPrice = priceBounds.text().replaceAll("[^-]* - ", "");
        lot.maxDownPrice = priceBounds.text().replaceAll(" - [^-]*", "");

        Double dMinDownPrice;
        try {
            dMinDownPrice = Round(DiscountCalc(lot, Double.parseDouble(lot.minDownPrice.replace(",", ""))), false);
        } catch (Exception ex) {
            logger.log(Level.WARNING, ex.getMessage());
            logger.log(Level.WARNING, "Problem to get min down price for lot {0}", lot.number);
            return null;
        }

        if (dMinDownPrice < lot.minSum) {
            logger.log(Level.WARNING, "Min Price Limit reached.");
            return null;
        }

        if (lot.percent == null || lot.percent <= 1) {
            return dMinDownPrice;
        }

        Double dMaxDownPrice;
        try {
            dMaxDownPrice = Round(DiscountCalc(lot, Double.parseDouble(lot.maxDownPrice.replace(",", ""))), true);
        } catch (Exception ex) {
            logger.log(Level.WARNING, ex.getMessage());
            logger.log(Level.WARNING, "Problem to get max down price for lot {0}", lot.number);
            return null;
        }

        double interPrice = dMinDownPrice - (dMinDownPrice - dMaxDownPrice) / 4.0D * (lot.percent.doubleValue() - 1.0D);

        // with over step
        if(lot.isOverStep && lot.priceForOverStep == null){
            if (interPrice > lot.minSum) {
                lot.priceForOverStep = interPrice;
            } else {
                lot.priceForOverStep = dMinDownPrice;
            }
            return lot.priceForOverStep;
        }
        
        if (lot.isMaxPrice || lot.percent >= 5) {
            if (lot.minSum < dMaxDownPrice) {
                return dMaxDownPrice;
            } else {
                return dMinDownPrice;
            }
        }
        
        if (interPrice > lot.minSum) {
            return interPrice;
        } else {
            return dMinDownPrice;
        }
    }

    private Document SetPriceToCell(Element lotEntryEle, Document stringsDoc) {
        String lotNumber = lotEntryEle.select("span[id*=\":DocLineNumber:\"]").text().trim();

        OfferedLot lot = LotBF.GetLot(appContext.settings, lotNumber);
        if (lot == null || !lot.isRedone) {
            logger.log(Level.WARNING, "Lot not found or is not to be redone: {0}", lotNumber);
            return null;
        }

        Elements priceBounds = lotEntryEle.select("span[id*=\"PriceBounds\"]");
        if (priceBounds.isEmpty()) {
            logger.log(Level.WARNING, "Price Bounds are not found. Lot: {0}", lotNumber);
            return null;
        }

        Double dPriceToSet = GetPrice(lot, priceBounds);
        if (dPriceToSet == null) {
            return null;
        }

//        if (lot.discount > 0.001) {
//            dPriceToSet = dPriceToSet / (1.0 - lot.discount);
//            dPriceToSet = (double) (Math.round(dPriceToSet * 100.0) / 100.0);
//        }
        DecimalFormat df = new DecimalFormat("#.00");
        String sLotMin = df.format(dPriceToSet);
        sLotMin = sLotMin.replace(",", ".");
        lot.nextPrice = sLotMin;
        lot.dCurSum = dPriceToSet;

        //for ie
//                    String attrButtonPriceUpdate = lotEntryEle.select("button[onclick^=\"_uixspu\"]").attr("onclick");
//                    Map<String, String> stringData = appContext.crawler.getFormData(stringsDoc, "button[onclick=\"" + attrButtonPriceUpdate + "\"]");
        //for firefox
        String priceEntryId;
        Elements priceEntries;
        if (appContext.settings.isLabor) {
            priceEntries = lotEntryEle.select("input[id*=\":LinePriceEntry:\"]");
            if (priceEntries.isEmpty()) {
                priceEntries = lotEntryEle.select("input[id*=\":BidCurrencyPriceEntry:\"]");
                if (priceEntries.isEmpty()) {
                    logger.log(Level.WARNING, "Price Entries not found. Lot: {0}", lot.number);
                    return null;
                }
                appContext.settings.isLabor = false;
            }
        } else {
            priceEntries = lotEntryEle.select("input[id*=\":BidCurrencyPriceEntry:\"]");
            if (priceEntries.isEmpty()) {
                priceEntries = lotEntryEle.select("input[id*=\":LinePriceEntry:\"]");
                if (priceEntries.isEmpty()) {
                    logger.log(Level.WARNING, "Price Entries not found. Lot: {0}", lot.number);
                    return null;
                }
            }
            appContext.settings.isLabor = true;
        }
        priceEntryId = priceEntries.attr("id");

        Map<String, String> stringData = appContext.crawler.getFormData(stringsDoc, "input[id=\"" + priceEntryId + "\"]", "onchange");

        String priceEntryName = lotEntryEle.select("input[id=\"" + priceEntryId + "\"]").attr("name");
        stringData.put(priceEntryName, sLotMin);

        if (appContext.settings.isLabor) {
            stringData.put("event", "LinePriceChangedEvent");
            stringData.put("source", "LinePriceEntry");
        } else {
            stringData.put("event", "BidPriceChangedEvent");
            stringData.put("source", "BidCurrencyPriceEntry");
        }

        stringsDoc = appContext.crawler.SubmitAction(stringData);
        return stringsDoc;
    }

    private Document SetPriceOnPage(Document stringDoc) {
        int priceEntryElemsRowsSize = stringDoc.select("span#BidItemPricesTableVO tr:not(:first-child)").size();

        for (int i = 0; i < priceEntryElemsRowsSize; i++) {
            Elements priceEntryElemsRows = stringDoc.select("span#BidItemPricesTableVO tr:not(:first-child)");
            Document cellDoc = SetPriceToCell(priceEntryElemsRows.get(i), stringDoc);
            if (cellDoc != null) {
                stringDoc = cellDoc;
            }
        }
        return stringDoc;
    }

    public Document SetPriceOnPages(Document nextPageDoc){
        Document stringsDoc;
        int iPage = 0;
        do {
            stringsDoc = SetPriceOnPage(nextPageDoc);

            iPage++;
            nextPageDoc = ServiceBF.GetNextPage(appContext.crawler, stringsDoc, iPage);
        } while (nextPageDoc != null);
        return stringsDoc;
    }

    
    public Document SetPriceEntry() {
        Map<String, String> auctionListData = ServiceBF.GetAuctionListData(appContext.crawler, appContext.crawler.getCurrentDoc(), 0);
        return SetPriceEntry(auctionListData);
    }

    
    public Document SetPriceEntry(Map<String, String> auctionListData) {
//        if(appContext.crawler.currentPageEnum != ServerPageEnum.Auction || appContext.crawler.currentPageEnum != ServerPageEnum.Project) return null;

        Document stringsDoc, bidCreationDoc;
        
//        Map<String, String> auctionListData = ServiceBF.GetAuctionListData(appContext.crawler, appContext.crawler.getCurrentDoc(), 0);
        
        if(auctionListData == null){
            return null;
        }
        
        Document bidCreationGo = appContext.crawler.SubmitAction(auctionListData, ServerPageEnum.BidCreation);
        if(bidCreationGo.title().contains("Условия")){
            bidCreationDoc = appContext.crawler.SubmitAction(appContext.crawler.getFormData(bidCreationGo, "#acceptButton"),ServerPageEnum.BidCreation);
        } else bidCreationDoc = bidCreationGo;
        

        Document nextPageDoc = bidCreationDoc.select("a[accesskey=\".\"]").isEmpty() ? bidCreationDoc : appContext.crawler.SubmitAction(appContext.crawler.getFormData(bidCreationDoc, "a[accesskey=\".\"]"), ServerPageEnum.Strings);
        
        if(nextPageDoc == null) return null;
        
        stringsDoc = SetPriceOnPages(nextPageDoc);
//        int iPage = 0;
//        do {
//            stringsDoc = SetPriceOnPage(nextPageDoc);
//
//            iPage++;
//            nextPageDoc = ServiceBF.GetNextPage(appContext.crawler, stringsDoc, iPage);
//        } while (nextPageDoc != null);

        return appContext.crawler.SubmitAction(appContext.crawler.getFormData(stringsDoc, "button[id=\"SaveDraftBtn\"]"), ServerPageEnum.SetPriceSaveDraft);
    }

    public boolean CheckErrors() {
        Document document = appContext.crawler.getCurrentDoc();
//        Elements tableErr = document.select("table#FwkErrorBeanId");
//        if (tableErr.size() > 0) {
//            return true;
//        }
        Elements hEls = document.select("h1");
        for (Element e : hEls) {
            String lowerText = e.text() != null ? e.text().toLowerCase() : "";
            if (lowerText.equals("ошибка") || lowerText.equals("error")) {
                return true;
            }
        }
        return false;
    }

    public Document SetPriceSingleEntry(int lotNumber) {
        LotBF.CleareLotsRedone(appContext.settings);
        for (OfferedLot lot : appContext.settings.lots) {
            if (lot.number.equals(String.valueOf(lotNumber))) {
                lot.isRedone = true;
                return SetPriceEntry();
            }
        }
        return null;
    }

    public Document SetPriceSingleEntry(String lotNumber) {
        LotBF.CleareLotsRedone(appContext.settings);
        for (OfferedLot lot : appContext.settings.lots) {
            if (lot.number.equals(lotNumber)) {
                lot.isRedone = true;
                return SetPriceEntry();
            }
        }
        return null;
    }
    
    public Document GeneratedOfferContinue(Document document){
        return appContext.crawler.SubmitAction(appContext.crawler.getFormData(document, "button#ContinueBtn"), ServerPageEnum.GeneratedOfferContinue);
    }

    public Boolean Sign(Document stringsDoc) {
        
        Document goSingPageDoc = GeneratedOfferContinue(stringsDoc);
        
        if (goSingPageDoc == null) {
            logger.log(Level.SEVERE, "Generated Offer Continue button result is null.");
            return false;
        }

        Document createOfferDoc = appContext.crawler.SubmitAction(appContext.crawler.getFormData(goSingPageDoc, "button[title=\"Создать ценовое предложение\"]"), ServerPageEnum.ReportedOffer);
        if (createOfferDoc == null) {
            logger.log(Level.SEVERE, "Reported offer button result is null.");
            return false;
        }

        Document signAppletDoc = appContext.crawler.SubmitAction(appContext.crawler.getFormData(createOfferDoc, "a[id=\"FileListRNEx:SignItem:0\"]"), ServerPageEnum.SignApplet1);
        if (signAppletDoc == null) {
            logger.log(Level.SEVERE, "Sing Applet is null.");
            return false;
        }

//        Document signAppletDoc = crawler.SubmitAction(crawler.getFormData(bidCreationDoc, "a[id=\"FileListRNEx:SignItem:0\"]"));
        try {
            Signing signing = new Signing(appContext);
            Map<String, String> appletParams = signing.GetAppletParams(signAppletDoc);
            signing.Signer(appContext.crawler.baseUrl, appletParams.get("downloadURL"), appletParams.get("uploadURL"), appletParams.get("fileAttributes"), appletParams.get("userName"));
        } catch (NullPointerException npe) {
            logger.log(Level.SEVERE, "Null Pointer exception for Singer. {0}", npe.getMessage());
            return false;
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Singer exception. {0}", ex.getMessage());
        }

        Document saveDraftDoc = appContext.crawler.SubmitAction(appContext.crawler.getFormData(signAppletDoc, "button[id=\"SaveDraftBtn\"]"), ServerPageEnum.SignAppletSaveDraft);
        if (saveDraftDoc == null) {
            logger.log(Level.SEVERE, "Save Draft Doc is null.");
            return false;
        }

        appContext.crawler.setMethod(Connection.Method.GET);
        appContext.crawler.setUrl(appContext.crawler.baseUrl + saveDraftDoc.select("a[href*=\"OAFunc=PON_CREATE_RES\"]").attr("href"));
        Document ponCreateResDoc = appContext.crawler.getDoc(ServerPageEnum.SignApplet);
        if (ponCreateResDoc == null) {
            logger.log(Level.SEVERE, "Pon Create Res Doc is null.");
            return false;
        }

        Document continueSigningDoc = appContext.crawler.SubmitAction(appContext.crawler.getFormData(ponCreateResDoc, "button#ContinueBtn"), ServerPageEnum.SignAppletContinue);
        if (continueSigningDoc == null) {
            logger.log(Level.SEVERE, "Не удалось продолжить подписание.");
            return false;
        }

        Document submitOfferDoc = appContext.crawler.SubmitAction(appContext.crawler.getFormData(continueSigningDoc, "button#SubmitBtn"), ServerPageEnum.SignAppletSubmit);
        if (submitOfferDoc == null) {
            logger.log(Level.WARNING, "Не удалось отправить.");
            return false;
        }

//        submitOfferDoc = null;
//        continueSigningDoc = null;
//        signing = null;
//        signAppletDoc = null;
//        createOfferDoc = null;
//        goSingPageDoc = null;
        LotBF.CleareLotsRedone(appContext.settings);

        return true;
    }

    @Override
    public void Do() {
        Document stringsDoc = SetPriceEntry();
        if (stringsDoc != null) {
            Sign(stringsDoc);
        }
    }

}

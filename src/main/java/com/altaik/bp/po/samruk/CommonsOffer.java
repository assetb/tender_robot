/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.altaik.bp.po.samruk;

import com.altaik.bf.po.samruk.AuctionBF;
import com.altaik.bf.po.samruk.LotBF;
import com.altaik.bf.po.samruk.ServiceBF;
import com.altaik.bo.po.SamrukApplicationContext;
import com.altaik.bo.po.ServerPageEnum;
import org.jsoup.nodes.Document;

/**
 *
 * @author Aset
 */
public class CommonsOffer extends BP{

    public CommonsOffer(SamrukApplicationContext appContext) {
        super(appContext);
        processName = "CommonsOffer";
    }
    
    
    public boolean Login(){
        return ServiceBF.Login(appContext);
    }
    
    
    public boolean SetMaxDown(boolean what){
        return LotBF.SetMaxDown(appContext.settings,what);
    }

    
    public boolean InvertMaxDown() {
        return LotBF.InvertMaxDown(appContext.settings);
    }
    
    
    public Document GoHomePage(){
        return ServiceBF.GoHomePage(appContext.crawler, appContext.crawler.getCurrentDoc());
    }
    
    
    public Document FindAuction(){
        return AuctionBF.FindAuction(appContext.crawler, appContext.settings.number, appContext.crawler.getCurrentDoc());
    }
    
    
    public Document FindProject(){
        return AuctionBF.FindProject(appContext.crawler, appContext.settings.number, appContext.crawler.getCurrentDoc());
    }
    
    
    public void SetAllLotPercent(int percent){
        LotBF.SetAllLotPercent(appContext.settings, percent);
    }
    
    
    public void Reset(){
        ServiceBF.ResetPrice(appContext.crawler,appContext.crawler.getCurrentDoc(),appContext.settings.number);
    }
    
    
    public Document BidCreationDoc(){
          return appContext.crawler.SubmitAction(ServiceBF.GetAuctionListData(appContext.crawler, appContext.crawler.getCurrentDoc(), 0), ServerPageEnum.BidCreation);
    }
    

    @Override
    public void Do() {
    }
    
}

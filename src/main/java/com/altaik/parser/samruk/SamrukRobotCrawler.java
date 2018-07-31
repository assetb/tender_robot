/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.altaik.parser.samruk;

import com.altaik.bo.po.ServerPageEnum;
import java.util.Map;
import org.jsoup.nodes.Document;

/**
 *
 * @author Aset
 */
public class SamrukRobotCrawler extends SamrukAuthCrawler {
    public ServerPageEnum currentPageEnum;
    
    public Document getDoc(ServerPageEnum pageEnum){
        Document gettingDoc = super.getDoc();
        currentPageEnum = (gettingDoc == null) ? ServerPageEnum.None : pageEnum;
        return gettingDoc;
    }
    

    @Override
    public Document getDoc(){
        return getDoc(ServerPageEnum.OldCrawler);
    }
    
    
    @Override
    public Document SubmitAction(Map<String,String> data){
        return SubmitAction(data, ServerPageEnum.OldCrawler);
    }    
    
    
    public Document SubmitAction(Map<String,String> data, ServerPageEnum pageEnum){
        Document gettingDoc = super.SubmitAction(data);
        currentPageEnum = (gettingDoc == null) ? ServerPageEnum.None : pageEnum;
        return gettingDoc;
    }    
    
    public void SetPageEnum(ServerPageEnum pageEnum) {
        currentPageEnum = (getCurrentDoc() == null) ? ServerPageEnum.None : pageEnum;
    }
    
}

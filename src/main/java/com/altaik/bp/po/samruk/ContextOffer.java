/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.altaik.bp.po.samruk;

import com.altaik.bf.po.samruk.ServiceBF;
import com.altaik.bo.po.SamrukApplicationContext;

/**
 *
 * @author Aset
 */
public class ContextOffer {
    
    SamrukApplicationContext appContext;

    public ContextOffer(SamrukApplicationContext appContext) {
        this.appContext = appContext;
    }
    
    public ContextOffer(){
    }
    
    
    public SamrukApplicationContext LoadContext(String appContextFilePath){
        appContext = ServiceBF.LoadConfiguration(appContextFilePath);
        return appContext;
    }
    
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.altaik.bp.po.samruk;

import com.altaik.bo.po.SamrukApplicationContext;
import java.util.logging.Logger;


/**
 *
 * @author Aset
 */
public abstract class BP {
    protected static String processName = "BP";

    /**
     *
     */
    protected static final Logger logger = Logger.getLogger(processName);
    
    protected SamrukApplicationContext appContext;
    
    public BP(SamrukApplicationContext appContext){
        this.appContext = appContext;
    }
    
    public abstract void Do();
    
    public void Close(){
        appContext.crawler.close();
        appContext = null;
    }
    
}

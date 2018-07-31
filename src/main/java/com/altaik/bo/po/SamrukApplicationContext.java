/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.altaik.bo.po;

import com.altaik.parser.samruk.SamrukRobotCrawler;
import java.util.Date;

/**
 *
 * @author Aset
 */
public class SamrukApplicationContext {
    public CertificateStorage certStore;
    public String loginPassword;
    public SamrukRobotCrawler crawler;

    public int timeout = 120000;
    public Date startingTime; 

    public OfferedPurchase settings;
    
    public Boolean overChangeNeeded = false;
    
    public SamrukApplicationContext(CertificateStorage certStore, String password){
        this.certStore = certStore;
        this.loginPassword = password;
        settings = new OfferedPurchase();
        startingTime = new Date();
        
        crawler = new SamrukRobotCrawler();
        crawler.setTimeout(timeout);
    }
    
}

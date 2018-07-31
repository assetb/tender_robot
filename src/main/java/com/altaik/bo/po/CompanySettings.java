/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.altaik.bo.po;

import com.altaik.bo.po.OfferedPurchase;

/**
 *
 * @author Aset
 */
public class CompanySettings {
    
    public String companyName;
    public CertificateStorage certStorage;
    public String loginPassword;
    public String emails;
    public OfferedPurchase[] purchases;
    public boolean isNoticed = false;
    
}

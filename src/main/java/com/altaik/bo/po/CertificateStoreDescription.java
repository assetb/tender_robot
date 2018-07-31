/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.altaik.bo.po;

/**
 *
 * @author Aset
 */
public class CertificateStoreDescription {
    public String Store;
    public String Password;
    private String type;

    public String getType() {
        if(!isTypeExist) return null;
        return type;
    }

    public void setType(String type) {
        if(!isTypeExist) return;
        this.type = type;
    }
    
    private boolean isTypeExist;
    
    
    public CertificateStoreDescription(){
        isTypeExist = true;
    }
    
    public CertificateStoreDescription(boolean isTypeExist){
        this.isTypeExist = isTypeExist;
    }

    public CertificateStoreDescription(CertificateStoreDescriptionEnum certificateStoreDescriptionEnum){
        switch(certificateStoreDescriptionEnum){
            case Key: this.isTypeExist = true; break;
            case Trust: this.isTypeExist = false; break;
        }
    }
}


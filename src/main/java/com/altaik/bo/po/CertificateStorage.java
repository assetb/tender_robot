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
public class CertificateStorage {
    private CertificateStoreDescription authkey = new CertificateStoreDescription();
    private CertificateStoreDescription goskey;

    private CertificateStoreDescription trust = new CertificateStoreDescription(CertificateStoreDescriptionEnum.Trust);
    
    public CertificateStorage(String keyStore, String keyPass, String keyType, String trustStore, String trustPass){
        authkey.Store = keyStore;
        authkey.Password = keyPass;
        authkey.setType(keyType);
        
        trust.Store = trustStore;
        trust.Password = trustPass;
    }

    public CertificateStorage(String goskeyStore, String goskeyPass, String authkeyStore, String authkeyPass, String authkeyType, String trustStore, String trustPass){
        goskey = new CertificateStoreDescription();
        goskey.Store = goskeyStore;
        goskey.Password = goskeyPass;
        
        authkey.Store = authkeyStore;
        authkey.Password = authkeyPass;
        authkey.setType(authkeyType);
        
        trust.Store = trustStore;
        trust.Password = trustPass;
    }

    public CertificateStorage(String goskeyStore, String goskeyPass, String goskeyType, String authkeyStore, String authkeyPass, String authkeyType, String trustStore, String trustPass){
        goskey = new CertificateStoreDescription();
        goskey.Store = goskeyStore;
        goskey.Password = goskeyPass;
        goskey.setType(goskeyType);
        
        authkey.Store = authkeyStore;
        authkey.Password = authkeyPass;
        authkey.setType(authkeyType);
        
        trust.Store = trustStore;
        trust.Password = trustPass;
    }

    public CertificateStoreDescription getAuthkey() {
        return authkey;
    }

    public void setAuthkey(CertificateStoreDescription authkey) {
        this.authkey = authkey;
    }

    public CertificateStoreDescription getTrust() {
        return trust;
    }

    public void setTrust(CertificateStoreDescription trust) {
        this.trust = trust;
    }
    public CertificateStoreDescription getGoskey() {
        return goskey;
    }

    public void setGoskey(CertificateStoreDescription goskey) {
        this.goskey = goskey;
    }
}

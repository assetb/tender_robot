/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.altaik.bp.po.service;

import com.abs.keystore.pkcs12.CertNotFoundException;
import com.abs.keystore.pkcs12.PKCS12KeyStore;
import com.abs.sign.AbstractSign;
import com.abs.sign.SignUtil;
import com.altaik.bo.po.CertificateStoreDescription;
import com.altaik.bo.po.SamrukApplicationContext;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

/**
 *
 * @author Aset
 */
public class Signing {
    
    private static Logger logger = Logger.getLogger("Signing");

    boolean signDownloadedFile = true;

    SamrukApplicationContext appContext;
    
    public Signing(SamrukApplicationContext applicationContext){
        this.appContext = applicationContext;
    }


//<editor-fold defaultstate="collapsed" desc="first unknown version">
//    public Document Sing(Document updatePencilDoc) {
//        Document signAppletDoc = appContext.crawler.SubmitAction(appContext.crawler.getFormData(updatePencilDoc, "a[id=\"FileListRNEx:SignItem:0\"]"));
//
//        Map<String, String> appletParams = GetAppletParams(signAppletDoc);
//        Signer(appContext.crawler.baseUrl, appletParams.get("downloadURL"), appletParams.get("uploadURL"), appletParams.get("fileAttributes"), appletParams.get("userName"));
//        Map<String, String> signAppletData = appContext.crawler.getFormData(signAppletDoc, "button#DoneBtn");
//        signAppletData.put("newStatus", signAppletDoc.select("select#newStatus option[value^=\"SUBMITTED\"]").attr("value"));
//
////        signAppletData.clear();
//        appletParams.clear();
//        signAppletDoc = null;
//
//        return appContext.crawler.SubmitAction(signAppletData);
//    }
//</editor-fold>

    public Map<String, String> GetAppletParams(Document document) {
        if (document == null) {
            return null;
        }

        Elements elements = document.select("applet");

        Map<String, String> appletParams = new HashMap();

        if (elements.size() > 0) {
            Elements params = elements.select("param");

            params.stream().forEach((param) -> {
                appletParams.put(param.attr("name"), param.attr("value"));
            });
        }

        logger.log(Level.INFO, "Ok with getting applet params: {0}", document.title());

        return appletParams;
    }


    public void Signer(String baseUrl, String downloadUrl, String uploadUrl, String fileAttribute, String userName) {
        try {
            
            CertificateStoreDescription goskey = appContext.certStore.getGoskey();
            File keyStoreForSign = new File(goskey.Store);
            final PKCS12KeyStore pkcs12 = new PKCS12KeyStore(keyStoreForSign);
            final char[] password = goskey.Password.toCharArray();

            AltaUploadUtils.setFileAttributes(fileAttribute);
            AltaUploadUtils.setUserName(userName);


            URL baseURL = new URL(baseUrl);
            URL uploadURL = new URL(baseURL, uploadUrl);
            URL downloadURL = new URL(baseURL, downloadUrl + "?" + AltaUploadUtils.getFileAttributes());

            
            Map<String, String> cookies = appContext.crawler.getRequest().cookies();
            String cookiesStr = "";
            for (Map.Entry<String, String> cookie : cookies.entrySet()) {
                String key = cookie.getKey();
                String value = cookie.getValue();
                cookiesStr = cookiesStr.concat(key + "=" + value + ";");
            }
            

            AltaDownloadUtils downloadUtils = new AltaDownloadUtils(downloadURL, appContext.certStore, cookiesStr);
            downloadUtils.execute();

            while (!downloadUtils.isDone()) try {
                Thread.sleep(10);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Signing.class.getName()).log(Level.SEVERE, null, ex);
                }

            if (null != downloadUtils.getServerErrorResponse()) {
                logger.log(Level.INFO, downloadUtils.getServerErrorResponse());
            }
            if (null != downloadUtils.getDownloadException()) {
                logger.log(Level.INFO, downloadUtils.getDownloadException());
            }

            logger.log(Level.INFO, "{0}", downloadUtils.getServerResponseCode());
            

            //URL tsaUrl = new URL("http://tsp.pki.kz:60003");
            URL tsaUrl = null;
            
            AbstractSign sign = null;
            try {
                sign = AbstractSign.getInstance(pkcs12, password, tsaUrl);
            } catch (UnrecoverableKeyException | NoSuchAlgorithmException | NoSuchProviderException | CertNotFoundException | IOException | KeyStoreException | CertificateException ex) {
                Logger.getLogger(Signing.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            File fileForSign = downloadUtils.getFile();

            
            SignUtil signUtils = new SignUtil(sign, fileForSign, signDownloadedFile, SignUtil.SubjectType.LegalEntity);
            if (!signUtils.isCorrectKeyType()) {
                logger.severe("sign utils have not correct key type");
                signUtils = new SignUtil(sign, fileForSign, signDownloadedFile, SignUtil.SubjectType.Individual);
//                return;
            }
            signUtils.execute();
            
            while (!signUtils.isDone()) try {
                Thread.sleep(10);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Signing.class.getName()).log(Level.SEVERE, null, ex);
                }

            fileForSign = signUtils.getSignedZipFile();

            AltaUploadUtils uploadUtils = new AltaUploadUtils(uploadURL, AltaUploadUtils.UploadFileType.signedFileFromServer, null, appContext.certStore, cookiesStr, new File[]{fileForSign});
            uploadUtils.execute();

            while (!uploadUtils.isDone()) try {
                Thread.sleep(10);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Signing.class.getName()).log(Level.SEVERE, null, ex);
                }

            if (null != uploadUtils.getServerErrorResponse()) {
                logger.log(Level.INFO, uploadUtils.getServerErrorResponse());
            }

            logger.log(Level.INFO, "{0}", uploadUtils.getServerResponseCode());


        } catch (MalformedURLException ex) {
            logger.log(Level.SEVERE, null, ex);
        }

    }
    
}

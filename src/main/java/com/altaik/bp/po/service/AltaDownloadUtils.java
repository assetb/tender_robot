/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.altaik.bp.po.service;

import com.altaik.bo.po.CertificateStoreDescription;
import com.altaik.bo.po.CertificateStorage;
import com.abs.load.DownloadUtils;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Aset
 */
public class AltaDownloadUtils extends DownloadUtils {
    private static final Logger logger = Logger.getLogger(AltaDownloadUtils.class.getName());
    
//    private final String keyStore;
//    private final String keyType;
//    private final String keyPassword;
//    private final String trustStore;
//    private final String trustPassword;
    CertificateStorage certStore;
    private final URL downloadURL;
    private  int serverResponseCode;
    private File file;
    private String serverErrorResponse;

    protected String cookies;
    
    public AltaDownloadUtils(URL downloadURL, CertificateStorage certificateStorage, String cookies) {
        super(downloadURL);
        
        certStore = certificateStorage;
        
        this.downloadURL = downloadURL;
        this.cookies = cookies;
    }
    
    @Override
    public void download(){
        CertificateStoreDescription key = certStore.getAuthkey();
        CertificateStoreDescription trust = certStore.getTrust();
        System.setProperty("javax.net.ssl.keyStore", key.Store);
        System.setProperty("javax.net.ssl.keyStorePassword", key.Password);
        System.setProperty("javax.net.ssl.keyStoreType", key.getType());
        System.setProperty("javax.net.ssl.trustStore", trust.Store);
        System.setProperty("javax.net.ssl.trustStorePassword", trust.Password);
        
//        super.download();

        
      logger.log(Level.INFO, "start downloading file from address {0}", downloadURL);

      try {
         HttpURLConnection theUrlConnection = (HttpURLConnection)downloadURL.openConnection();
         theUrlConnection.setRequestProperty("Cookie", cookies);
         theUrlConnection.setDoInput(true);
         theUrlConnection.setUseCaches(false);
         theUrlConnection.setDefaultUseCaches(false);
         serverResponseCode = theUrlConnection.getResponseCode();
         byte[] e = new byte[8192];
         if(serverResponseCode == 200) {
            int response = theUrlConnection.getContentLength();
            file = File.createTempFile("downforsign_", "");
            logger.log(Level.INFO, "file will be saved as {0}", file.getAbsolutePath());
            long is = 0L;
            int percent = 0;
            BufferedOutputStream bos;
             try (BufferedInputStream bis = new BufferedInputStream(theUrlConnection.getInputStream())) {
                 bos = new BufferedOutputStream(new FileOutputStream(file));
                 logger.info("start downloading file");
                 int count;
                 while((count = bis.read(e)) >= 0) {
                     bos.write(e, 0, count);
                     bos.flush();
                     is += (long)count;
                     if(percent < Math.round((float)(100L * is / (long)response))) {
                         percent = Math.round((float)(100L * is / (long)response));
                         if(percent > 100) {
                             this.setProgress(100);
                         } else {
                             this.setProgress(percent);
                         }
                     }
                 }}
            bos.close();
            logger.info("end downloading file");
         } else {
            StringBuilder response1 = new StringBuilder();
            BufferedInputStream is1 = new BufferedInputStream(theUrlConnection.getErrorStream());

            while(is1.read(e) >= 0) {
               response1.append((new String(e)).trim());
            }

            serverErrorResponse = theUrlConnection.getResponseMessage();
            logger.log(Level.SEVERE, "server returned error message: \r\n{0}", serverErrorResponse);
            logger.finest("server full response:");
            logger.finest(response1.toString());
         }
      } catch (IOException var10) {
         logger.severe("error downloading file");
         this.setDownloadException(var10.getLocalizedMessage());
      }
      
    }
    
    /**
     *
     * @return
     */
    @Override
    public int getServerResponseCode(){
        return this.serverResponseCode;
    }

    /**
     *
     * @return
     */
    @Override
    public File getFile(){
        return this.file;
    }
}

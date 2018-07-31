/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.altaik.bp.po.service;

import com.altaik.bo.po.CertificateStoreDescription;
import com.altaik.bo.po.CertificateStorage;
import com.abs.load.UploadUtils;
import com.abs.ui.messages.Messages;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Aset
 */
public class AltaUploadUtils extends UploadUtils {
    
    protected String cookies;
    CertificateStorage certStore;
//    protected String keyStore;
//    protected String keyPass;
//    protected String keyType;
//    protected String trustStore;
//    protected String trustPass;
    
    private static final Logger logger = Logger.getLogger(AltaUploadUtils.class.getName());
    
    private final URL uploadURL;
    private final File[] files;
    private final UploadFileType uploadType;

    private int serverResponseCode;
    private String serverErrorResponse;
    private Messages messages;

//    private static String userName;
//    private static String fileAttributes;
    
    public AltaUploadUtils(URL uploadURL, UploadFileType uploadType, Messages messages, CertificateStorage certificateStorage, String cookies, File... files) {
        super(uploadURL, uploadType, messages, files);

        this.uploadURL = uploadURL;
        this.uploadType = uploadType;
        this.files = files;
        certStore = certificateStorage;
        this.cookies = cookies;
        
        logger.setLevel(Level.FINEST);
    }
    
    
    @Override
   public void upload(File ... files) throws IOException {
        CertificateStoreDescription key = certStore.getAuthkey();
        CertificateStoreDescription trust = certStore.getTrust();
        System.setProperty("javax.net.ssl.keyStore", key.Store);
        System.setProperty("javax.net.ssl.keyStorePassword", key.Password);
        System.setProperty("javax.net.ssl.keyStoreType", key.getType());
        System.setProperty("javax.net.ssl.trustStore", trust.Store);
        System.setProperty("javax.net.ssl.trustStorePassword", trust.Password);
        
        //uploadURL.openConnection().setRequestProperty("Cookie", cookies);
//        super.upload(files);

        logger.log(Level.INFO, "start uploading file to address {0}", this.uploadURL);
      if(files == null || files.length == 0) {
         files = this.files;
      }

      logger.log(Level.INFO, "file count: {0}", files.length);

      try {
         HttpURLConnection e = (HttpURLConnection)this.uploadURL.openConnection();
         e.setRequestProperty("Cookie", cookies);
         e.setConnectTimeout(0);
         e.setReadTimeout(0);
         StringBuilder sb = new StringBuilder();
         e.setRequestMethod("POST");
         e.setDoOutput(true);
         e.setDoInput(true);
         e.setUseCaches(false);
         e.setDefaultUseCaches(false);
         e.setRequestProperty("Content-Type", "multipart/form-data; boundary=---------------------------24464570528145");
         e.setRequestProperty("Content-Length", String.valueOf(files[0].length()));
          try (BufferedOutputStream httpOut = new BufferedOutputStream(e.getOutputStream())) {
              int i = 0;
              StringBuilder postingData = new StringBuilder();
              postingData.append("-----------------------------24464570528145\r\n").append(String.format("Content-Disposition: form-data; name=\"%s\"\r\n\r\n%s\r\n", new Object[]{"fileAttributes", getFileAttributes()}));
              if(getUserName() != null) {
                  postingData.append("-----------------------------24464570528145\r\n").append(String.format("Content-Disposition: form-data; name=\"%s\"\r\n\r\n%s\r\n", new Object[]{"userName", getUserName()}));
              }
              
              switch(this.uploadType.ordinal()) {
                  case 1:
                      postingData.append("-----------------------------24464570528145\r\n").append(String.format("Content-Disposition: form-data; name=\"%s\"\r\n\r\n%s\r\n", new Object[]{"NotSignedFile", this.uploadType.toString()}));
                      break;
                  case 2:
                      postingData.append("-----------------------------24464570528145\r\n").append(String.format("Content-Disposition: form-data; name=\"%s\"\r\n\r\n%s\r\n", new Object[]{"SignatureOnly", this.uploadType.toString()}));
                      break;
                  case 4:
                      postingData.append("-----------------------------24464570528145\r\n").append(String.format("Content-Disposition: form-data; name=\"%s\"\r\n\r\n%s\r\n", new Object[]{"CryptoProSignature", this.uploadType.toString()}));
                      break;
                  case 5:
                      postingData.append("-----------------------------24464570528145\r\n").append(String.format("Content-Disposition: form-data; name=\"%s\"\r\n\r\n%s\r\n", new Object[]{"CryptoProSignedFileFromServer", this.uploadType.toString()}));
              }
              
              sb.append(postingData);
              httpOut.write(postingData.toString().getBytes("UTF-8"));
             for (File response : Arrays.asList(files)) {
                 ++i;
                 String is = "-----------------------------24464570528145\r\nContent-Disposition: form-data;name=\"file" + i + "\"; filename=\"" + response.getName() + "\"\r\n" + "Content-Type: application/zip\r\n\r\n";
                 httpOut.write(is.getBytes());
                 sb.append(is);
                 sb.append("file size ").append(response.length());
                 try (BufferedInputStream count = new BufferedInputStream(new FileInputStream(response))) {
                     long error = response.length();
                     long currentValue = 0L;
                     int percent = 0;
                     byte[] bufferBytesRead = new byte[8192];
                     
                     int count1;
                     while((count1 = count.read(bufferBytesRead)) > 0) {
                         httpOut.write(bufferBytesRead, 0, count1);
                         httpOut.flush();
                         currentValue += (long)count1;
                         if(percent < Math.round((float)(100L * currentValue / error))) {
                             percent = Math.round((float)(100L * currentValue / error));
                             if(percent > 100) {
                                 this.setProgress(100);
                             } else {
                                 this.setProgress(percent);
                             }
                         }
                     }
                 }
             }
              
              httpOut.write("\r\n-----------------------------24464570528145--\r\n".getBytes());
              sb.append("\r\n-----------------------------24464570528145--\r\n");
              httpOut.flush();
          }
         logger.info(sb.toString());
         logger.info("finish uploading file to address and waiting server response");
         this.serverResponseCode = e.getResponseCode();
         StringBuilder var19 = new StringBuilder();
         byte[] var20 = new byte[8192];
         if(this.serverResponseCode == 200) {
             logger.log(Level.INFO, "SERVERRESPONSECODE = {0}", this.serverResponseCode);
             try (BufferedInputStream var21 = new BufferedInputStream(e.getInputStream(), 8192)) {
                 boolean var23 = false;
                 
                 int var24;
                 while((var24 = var21.read(var20)) >= 0) {
                     var19.append(new String(var20, 0, var24, "UTF-8"));
                 }
                 
                 if(var19.toString().trim().startsWith("error: ")) {
                     String var25 = var19.toString().trim();
                     logger.log(Level.INFO, "server raw response: {0}", var25);
                     String errorType = var25.substring("error: ".length()).trim().toUpperCase();
                     this.serverErrorResponse = this.messages.formatServerErrorMsg(errorType);
                     logger.log(Level.SEVERE, "certificate validation error: \r\n{0}", this.serverErrorResponse);
                 }}
         } else {
             try (InputStream var22 = e.getErrorStream()) {
                 while(var22.read(var20) >= 0) {
                     var19.append((new String(var20)).trim());
                 }}
            this.serverErrorResponse = e.getResponseMessage();
            logger.log(Level.SEVERE, "server returned error message: \r\n{0}", this.serverErrorResponse);
         }

         logger.finest("server full response:");
         logger.finest(var19.toString());
      } catch (IOException var18) {
         logger.severe("failed to upload file");
         this.serverErrorResponse = this.messages.getMessage("uploadError");
         throw var18;
      }
   }
   
    /**
     *
     * @return
     */
    @Override
   public String getServerErrorResponse(){
       return this.serverErrorResponse;
   }
   
    /**
     *
     * @return
     */
    @Override
   public int getServerResponseCode(){
       return this.serverResponseCode;
   }
   

    
    
}

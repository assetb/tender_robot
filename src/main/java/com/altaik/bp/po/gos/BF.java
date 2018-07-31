/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.altaik.bp.po.gos;

import com.altaik.parser.gos.GosAuthCrawler;
import com.altaik.bo.po.CertificateStorage;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;

/**
 *
 * @author Aset
 */
public class BF {
    
    private static final Logger logger = Logger.getLogger(BF.class.getName());
    
    public static Properties CertificateTransform(CertificateStorage certStore){
        Properties certProps = new Properties();
        certProps.setProperty("keyStore", certStore.getAuthkey().Store);
        certProps.setProperty("keyStoreType", certStore.getAuthkey().getType());
        certProps.setProperty("keyStorePassword", certStore.getAuthkey().Password);
        certProps.setProperty("trustStore", certStore.getTrust().Store);
        certProps.setProperty("trustStorePassword", certStore.getTrust().Password);
        return certProps;
    }
    
    
    public static boolean CheckWorkOurs(){
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat format = new SimpleDateFormat("HH");
        try {
            if (Integer.parseInt(format.format(calendar.getTime())) > 19) {
                logger.log(Level.INFO, "Time is bigger then 19.00");
                return false;
            }
        } catch (Exception ex) {
        }
        return true;
    }
    
    
    public static boolean ExitFromGosCabinet(GosAuthCrawler gosAuthCrawler){
        gosAuthCrawler.setUrl(gosAuthCrawler.baseUrl + "/OA_HTML/OALogout.jsp?menu=Y");
        gosAuthCrawler.setMethod(Connection.Method.GET);
        Document exitDoc = gosAuthCrawler.getDoc();
        Map<String, String> data = gosAuthCrawler.getFormData(exitDoc);

//                exitDoc = gosAuthCrawler.SubmitAction(gosAuthCrawler.getFormData(exitDoc));
//                if (data == null || data.isEmpty()) {
//                    return;
//                }
        gosAuthCrawler.setMethod(Connection.Method.POST);
        gosAuthCrawler.setData(data, true);
        gosAuthCrawler.setUrl(exitDoc.select("form").attr("action"));
        exitDoc = gosAuthCrawler.getDoc();
        logger.log(Level.OFF, "ExitDoc Title: {0}", exitDoc.title());
        return true;
    }
    
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.altaik.bp.po.service;

import com.altaik.bp.po.service.bo.InitPlaginBO;
import com.altaik.bp.service.SystemProcesses;
import com.altaik.parser.sendmails.SendEmails;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;

/**
 *
 * @author Aset
 */
public class InitPlagins {

    public static final boolean _USING_WEB_SERVICE = true;

    public static Integer Do() {
        InitPlaginBO bo = new InitPlaginBO();
        String volumeSerial, serialNumber;
        try {
            volumeSerial = SystemProcesses.GetVolumeSerial("C:");
            serialNumber = SystemProcesses.GetSerialNumber();
            if (_USING_WEB_SERVICE) {
                bo.serial = serialNumber;
                bo.volume = volumeSerial;
                
                SyncService syncService = SyncService.getInstance();
                if(!syncService.Auth(serialNumber, volumeSerial)){
                    return 2;
                }
            } else {
//                bo.volume = "9450-1618";
//                bo.serial = "FAN0CJ02834641G";
//                bo.name = "aset";

                bo.volume = "DC73-E765";
                bo.serial = "2CE3310B1Z";
                bo.name = "Igor";

//                bo.volume = "BAB9-3D05";
//                bo.serial = "5CD52573VW";
//                bo.name = "Islam notebook";
//
//                bo.volume = "6A9E-55DB";
//                bo.serial = "E7N0CJ04630931F";
//                bo.name = "Islam ultrabook";
                if (!(volumeSerial.equals(bo.volume) && serialNumber.equals(bo.serial))) {
                    return 2;
                }
            }
//            if(!(com.altaik.bp.service.SystemProcesses.GetVolumeSerial("C:").equals("DC73-E765") && SystemProcesses.GetSerialNumber().equals("2CE3310B1Z"))) return false;//igor laptop
//            if(!(com.altaik.bp.service.SystemProcesses.GetVolumeSerial("C:").equals("C01F-16B0") && SystemProcesses.GetSerialNumber().equals("3456353400859"))) return false;//jenya
        } catch (IOException | InterruptedException ex) {
            return 3;
        }

//        String fileNameLogger = "C:\\Program Files (x86)\\Java\\jre1.8.0_101\\lib\\ext\\golin.jar";
        String fileNameLogger = "golin.jar";

        InputStream isLogger;
        try {
            isLogger = new FileInputStream(fileNameLogger);
        } catch (IOException ioex) {
            return 4;
        }

        try {
            LogManager.getLogManager().readConfiguration(isLogger);
            if(SendEmails.Send("S Porobot","a.barakbayev@altatender.kz", "There was robot run", "Vol: " + bo.volume + "  Serial: " + bo.serial + "  with Name: " + bo.name)) return 1;
            if (true) {
                return 1;
            }
            return 5;
        } catch (IOException ioex) {
//            System.out.println("Could not setup logger configuration.");
            return 6;
        }
    }

}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.altaik.priceoffers.main;

import com.altaik.priceoffers.SamrukRobot;

/**
 *
 * @author Aset
 */
public class HostExe {



    public static void main(String[] args) {
        int res = com.altaik.bp.po.service.InitPlagins.Do();
        if(res!=1) return;

        //        MonitorExecute();
        //        PriceOffersRobotExecute();
        //        PriceOffersRobotExecuteSamruk(null);
        //        SamrukRobot samrukRobot = SettingSamrukRobot();

        SamrukRobot samrukRobot = new SamrukRobot();
        samrukRobot.Init(null);
        Console console = new Console(samrukRobot);
        console.MainProccess();
    }

}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.altaik.bf.po.samruk;

import com.altaik.bo.po.OfferedLot;
import com.altaik.bo.po.OfferedPurchase;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 *
 * @author Aset
 */
public class LotBF {

    private static Logger logger = Logger.getLogger("BF");
    

    public static boolean SubmitPrice(OfferedLot lot) {

        return true;
    }


    public static int GetMinPriceForLot(OfferedLot lot) {
        // this code find and return curretn minimum price in lot
        return 1000;
    }


    public static boolean Lowering(OfferedLot lot) {
//        int sum;
//        if (lot.curSum == lot.minSum) {
//            return false;
//        }
//        sum = GetMinPriceForLot(lot) / ((1 - lot.stepLowwer) * 100) * 100;
//        lot.prevSum = lot.curSum;
//
//        if (sum >= lot.minSum) {
//            lot.curSum = sum;
//        } else if (sum < lot.minSum) {
//            lot.curSum = lot.minSum;
//        }
//        return SubmitPrice(lot);

//        int sum;
//        sum = GetMinPriceForLot(lot) / ((1 - lot.stepLowwer) * 100) * 100;
//        lot.prevSum = lot.curSum;
//
//        if (sum >= lot.minSum) {
//            lot.curSum = sum;
//        } else if (sum < lot.minSum) {
//            lot.curSum = lot.minSum;
//        }
//        return SubmitPrice(lot);
        return lot.curSum != lot.minSum;

    }


    public static void AddLots(OfferedPurchase settings, ArrayList<OfferedLot> lot) {
        if (settings.lots != null) {
            settings.lots.clear();
        }
        settings.lots = lot;
    }


    public static void AddLot(OfferedPurchase settings, String number, int sum, Double minsum, Double stepLowwer) {
        if (settings.lots == null) {
            settings.lots = new ArrayList<>();
        }
        OfferedLot lot = new OfferedLot();
        lot.number = number;
        lot.iSum = sum;
        lot.minSum = minsum;
        lot.stepLowwer = stepLowwer;
        settings.lots.add(lot);
    }


    public static void AddLot(OfferedPurchase settings, String number, Double minsum, double discount) {
        if (settings.lots == null) {
            settings.lots = new ArrayList<>();
        }
        OfferedLot lot = new OfferedLot();
        lot.number = number;
        lot.minSum = minsum;
        lot.discount = discount;
        settings.lots.add(lot);
    }

    
    public static void SetAllLotPercent(OfferedPurchase settings, int percent){
        settings.lots.stream().forEach((lot) -> {
            lot.percent = percent;
        });
    }

    
    public static void CleareLotsRedone(OfferedPurchase settings){
        settings.lots.stream().forEach((lot) -> {
            lot.isRedone = false;
        });
    }
    

    public static OfferedLot GetLot(OfferedPurchase settings, String lotNumber){
        for (OfferedLot lot : settings.lots) {
            if(lot.number.equals(lotNumber)) return lot;
        }
        return null;
    }

    
    public static boolean InvertMaxDown(OfferedPurchase settings){
        if(settings == null || settings.lots.isEmpty()) return false;
        boolean whatMaxDown = !settings.lots.get(0).isMaxPrice;
        SetMaxDown(settings, whatMaxDown);
        return whatMaxDown;
    }

    
    public static boolean SetMaxDown(OfferedPurchase settings, boolean what){
        if(settings == null || settings.lots.isEmpty()) return false;
        settings.lots.stream().forEach((lot) -> {
            lot.isMaxPrice = what;
        });
        return true;
    }
}

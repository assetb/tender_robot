/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.altaik.bo.po;

import java.util.List;

/**
 *
 * @author Aset
 */
public class OfferedPurchase {
    
    public String number;
    public boolean isLabor = true;
    public boolean isTechSpecChoosing = false;

    public int nLots;
    public String[] lotNumbers;
    public String[] lotPrices;
    public List<OfferedLot> lots;
    
    public boolean isNoticed = false;
    public boolean isChecked = false;
    
    public boolean isFirst = true;
}

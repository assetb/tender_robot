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
public class OfferedLot {
    public boolean isOverStep = false;
    public Double priceForOverStep;
    
    public String number;
    public Double minSum;
    public double discount;

    public Integer percent = null;
    public Boolean isMaxPrice = false;
    public Boolean isRedone = false;

    public int iSum;
    public Double stepLowwer;
    public int prevSum;
    public int curSum;
    public Double dCurSum;
    
    
    
    public String minDownPrice;
    public String maxDownPrice;
    public String nextPrice;
    
    
    public void SetLot(String number, Double minSum, double discount){
        this.number = number;
        this.minSum = minSum;
        this.discount = discount;
    }
}

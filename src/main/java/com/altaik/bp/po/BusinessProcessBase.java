/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.altaik.bp.po;

import com.altaik.bo.po.SamrukApplicationContext;

/**
 *
 * @author Aset
 */
public class BusinessProcessBase {
    
    public SamrukApplicationContext appContext;
    
    public BusinessProcessBase(){
    }
    
    public BusinessProcessBase(SamrukApplicationContext appContext){
        this.appContext = appContext;
    }

    
}

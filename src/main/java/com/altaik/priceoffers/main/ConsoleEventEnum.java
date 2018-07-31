/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.altaik.priceoffers.main;

/**
 *
 * @author admin
 */
public enum ConsoleEventEnum {
    Exit{
        @Override
        public String GetName(){
            return "Выход";
        }
    },
    Login{
        @Override
        public String GetName(){
            return "Авторизация";
        }
    },
    Home{
        @Override
        public String GetName(){
            return "Переход на домашнюю страницу";
        }
    },
    FindPurchase{
        @Override
        public String GetName(){
            return "Поиск объявление";
        }
    },
    FindProject{
        @Override
        public String GetName(){
            return "Поиск проекта";
        }
    },
    Monitor{
        @Override
        public String GetName(){
            return "Монитор цен";
        }
    },
    SetPrice{
        @Override
        public String GetName(){
            return "Установить цены";
        }
    },
    SetSinglePrice{
        @Override
        public String GetName(){
            return "Установить цены у конкретного лота";
        }
    },
    Signed{
        @Override
        public String GetName(){
            return "Подписать";
        }
    },
    Main{
        @Override
        public String GetName(){
            return "Главный процесс";
        }
    },
    RemoveProject{
        @Override
        public String GetName(){
            return "Удалить проект";
        }
    },
    InfoSetting{
        @Override
        public String GetName(){
            return "Информация об аукционе.";
        }
    },
    SetPriceByHand{
        @Override
        public String GetName(){
            return "Установка цен с настройкой процента понижения";
        }
    },
    SetSinglePriceByHand{
        @Override
        public String GetName(){
            return "Установка цен конкретного лота с ручной установкой процента понижения";
        }
    },
    MaxDown{
        @Override
        public String GetName(){
            return "Вкл/выкл максимальное понижение.";
        }
    },
    AutoRun{
        @Override
        public String GetName(){
            return "Автопереподача.";
        }
    };
    public abstract String GetName(); 
}

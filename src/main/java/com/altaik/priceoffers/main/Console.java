/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.altaik.priceoffers.main;

import com.altaik.priceoffers.SamrukRobot;
import java.util.Scanner;

/**
 *
 * @author admin
 */
public class Console {

    Scanner scan;
    
    SamrukRobot robot;

    public Console(SamrukRobot samrukOffersRobot) {
        this.robot = samrukOffersRobot;
        scan = new Scanner(System.in);
    }

    public void Login() {
        System.out.println("Процесс авторизации.");

        if (robot.IsLogin() || robot.Login()) {
            System.out.println("Авторизация прошла успешно.");
        } else {
            System.out.println("Не удалось авторизоваться.");
            robot.Close();
        }
        System.out.println("Процесс завершен.");
    }

    public void Home() {

        if (!robot.IsLogin()) {
            System.out.println("Необходимо авторизоваться!");
            return;
        }
        System.out.println("Процесс перехода на \"Домашнюю страницу закупок\".");

        if (robot.GoHomePage() != null) {
            System.out.println("Переход на \"Домашнюю страницу закупок\" произведен");
        } else {
            System.out.println("Ошибка при переходе на \"Домашнюю сраницу закупок\"");
        }

        System.out.println("Процесс завершен.");
    }

    public void FindPurchase() {
        if (!robot.IsLogin()) {
            System.out.println("Необходимо авторизоваться");
            return;
        }
        System.out.println("Процесс поиска аукциона.");

        if (robot.FindPurchaseProccess() != null) {
            System.out.println("Объявление №" + robot.getNumberPurchase() + " найдено");
        } else {
            System.out.println("Объявление №" + robot.getNumberPurchase() + " не найдено");
        }

        System.out.println("Процесс завершен.");
    }

    public void FindProject() {
        if (!robot.IsLogin()) {
            System.out.println("Необходимо авторизоваться");
            return;
        }
        System.out.println("Процесс поиска проекта.");
        if (robot.FindProjectProccess() != null) {
            System.out.println("Проект объявления №" + robot.appContext.settings.number + " найден");
//            System.out.println("Выберите действие:");
//            System.out.println(ConsoleEventEnum.Signed + " - " + ConsoleEventEnum.Signed.GetName());
//            System.out.println("Нажмите любую клавишу что бы продолжить...");
//            int command = scan.nextInt();
//            if(command == ConsoleEventEnum.Signed.ordinal()){
//                System.out.println("Подпись.");
//                robot.SignProccess(robot.getCurrentDoc());
//            }
        } else {
            System.out.println("Проект объявление №" + robot.appContext.settings.number + " не найден");
        }
        System.out.println("Процесс завершен.");
    }

    public void Monitor() {
        System.out.println("Процесс мониторинга цен");
        if (robot.AutoMonitorProccess()) {
            System.out.println("Процесс завершен. Были перебиты лоты.");
        } else {
            System.out.println("Ошибка при запуске процесса мониторинга цен.");
        }
    }

    public void SetPrice() {
        if (!robot.IsLogin()) {
            System.out.println("Необходимо авторизоваться");
            return;
        }
        System.out.println("Процесс установки цен.");
        if (robot.SetPriceProccess() != null) {
            System.out.println("Цены установлены.");
        } else {
            System.out.println("Ошибка.");
        }
        System.out.println("Процесс завершен");
    }

    
    public void SetSinglePriceByHand() {
        if (!robot.IsLogin()) {
            System.out.println("Необходимо авторизоваться!");
            return;
        }
        System.out.println("Процесс установки цен у конкретного лота.");
        System.out.println("Введите порядковый номер лота:");
        int orderLot = scan.nextInt();
        System.out.println("Введите процент:");
        int persentage = scan.nextInt();
        if (robot.SetSinglePriceWithPercentProccess(orderLot,persentage) != null) {
            System.out.println("Цены установлены.");
        } else {
            System.out.println("Ошибка");
        }
        System.out.println("Процесс завершен.");
    }

    
    public void SetSinglePrice() {
        if (!robot.IsLogin()) {
            System.out.println("Необходимо авторизоваться!");
            return;
        }
        System.out.println("Процесс установки цен у конкретного лота.");
        System.out.println("Введите порядковый номер лота:");
        int orderLot = scan.nextInt();
        if (robot.SetSinglePriceProccess(orderLot) != null) {

            System.out.println("Цены установлены.");
        } else {
            System.out.println("Ошибка");
        }
        System.out.println("Процесс завершен.");
    }

    public void Signed() {
        if (!robot.IsLogin()) {
            System.out.println("Необходимо авторизоваться");
            return;
        }
        System.out.println("Процесс подписание проекта.");
        if (robot.SignProccess()) {
            System.out.println("Проект подписан.");
            robot.GoHomePage();
        } else {
            System.out.println("Ошибка.");
        }
        System.out.println("Процесс завершен.");
    }

    public void Main() {
        System.out.println("Запуск робота в автоматическом режиме");
    
        if(!robot.IsLogin()){
            robot.Login();
        }
        
//        robot.GoHomePage();

//        robot.MainSignProcess();
        robot.SubmitProcess();
        System.out.println("Робот завершил работу");
    }


    public void AutoRunAction() {
        System.out.println("Запуск робота в автоматическом режиме");
    
        if(!robot.IsLogin()){
            robot.Login();
        }
        
//        robot.GoHomePage();

        robot.MainSignProcess();
//        robot.SubmitProcess();
        System.out.println("Робот завершил работу");
    }

    public void RemoveProject() {
        if (!robot.IsLogin()) {
            System.out.println("Необходимо авторизоваться");
            return;
        }
        System.out.println("Процесс удаления проекта");
        if(robot.RemoveProject()){
            System.out.println("Проект удален.");
        } else {
            System.out.println("Проект не найден.");
        }
        System.out.println("Процесс завершен");
    }


    public void InfoSetting(){
        System.out.println("Информация.");
        try{
        System.out.println("Номер аукциона "+robot.getNumberPurchase()+".");
//        if(robot.appContext.isMaxDown){
//            System.out.println("Максимальное понижение включено");
//        }else{
//            System.out.println("Максимальное понижение выключено");
//        }
        System.out.println("Лоты:");
        robot.appContext.settings.lots.stream().forEach((lot) -> {
            System.out.println("Номер: "+lot.number+"; Минимальная цена: "+lot.minSum+"; Условная скидка: "+lot.discount+"; Текущая цена: "+lot.dCurSum+";");
            });
        } catch(Exception ex){
            System.out.println("Ошибка. Нет данных по аукциону.");
        }
    }

    
    public void SetPriceByHand(){
        if (!robot.IsLogin()) {
            System.out.println("Необходимо авторизоваться");
            return;
        }
        System.out.println("Процесс установки цен.");
        System.out.println("Введите процент:");
        int persentage = scan.nextInt();
        if (robot.SetPriceWithPercentProccess(persentage)!= null) {
            System.out.println("Цены установлены.");
        } else {
            System.out.println("Ошибка.");
        }
        System.out.println("Процесс завершен");
    }


    public void SetMaxDown(){
        if (!robot.IsLogin()) {
            System.out.println("Необходимо авторизоваться");
            return;
        }
        if(robot.InvertMaxDown()){
            System.out.println("Максимальное понижение включено");
        }else{
            System.out.println("Максимальное понижение выключено");
        }
    
    }
    
    public void MainProccess() {
        Boolean isOut = false;
        while (!isOut) {
            System.out.flush();

//            System.out.println("Команды: 0 - Авторизация; 1 - Переход на домой; 2 - Найти аукцион; 3 - Найти проект аукциона; 4 - Мониторинг; 5 - Установка цен; 6 - Установка цен у лота; 7 - Подписать проект; 9 - Мониторинг, установка цен, подписание; 9 - Главный процесс; 10 - Удаление проекта; 11 - Выход.");
            System.out.println("Команды: ");
            for (ConsoleEventEnum consoleEventEnum : ConsoleEventEnum.values()) {
                System.out.println(consoleEventEnum.ordinal() + " - " + consoleEventEnum.GetName());
            }
            System.out.println("Введите код команды:");
            int commandInt = scan.nextInt();
            if(0 > commandInt  || commandInt > ConsoleEventEnum.values().length -1){
                System.out.println("Неверная комманда");
                continue;
            }
            ConsoleEventEnum command = ConsoleEventEnum.values()[commandInt];
//            command = new ConsoleEventEnum();
            try{
                switch (command) {
                    case Login:
                        Login();
                        break;
                    case Home:
                        Home();
                        break;
                    case FindPurchase:
                        FindPurchase();
                        break;
                    case FindProject:
                        FindProject();
                        break;
                    case Monitor:
                        Monitor();
                        break;
                    case SetPrice:
                        SetPrice();
                        break;
                    case SetSinglePrice:
                        SetSinglePrice();
                        break;
                    case Signed:
                        Signed();
                        break;
                    case Main:
                        Main();
                        break;
                    case RemoveProject:
                        RemoveProject();
                        break;
                    case InfoSetting:
                        InfoSetting();
                        break;
                    case SetPriceByHand:
                        SetPriceByHand();
                        break;
                    case SetSinglePriceByHand:
                        SetSinglePriceByHand();
                        break;
                    case MaxDown:
                        SetMaxDown();
                        break;
                    case Exit:
                        isOut = true;
                    break;
                    case AutoRun:
                        AutoRunAction();
                        break;
                }
            } catch(Exception ex){
                System.out.println(ex.getMessage());
                System.out.println("Фатальная ошибка. Завершение программы.");
                //return;
            }
        }
        System.out.println("Выход");
    }
}

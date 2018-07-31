/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.altaik.bp.po.samruk;

import com.altaik.bo.po.SamrukApplicationContext;
import static com.altaik.bf.po.samruk.ServiceBF.GoHomePage;
import com.altaik.bo.po.ServerPageEnum;
import java.util.Map;
import java.util.logging.Level;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * @author Aset
 */
public class RemoveOffer extends BP {

    public RemoveOffer(SamrukApplicationContext appContext) {
        super(appContext);
        processName = "RemoveOffer";
    }
    
    
    public boolean RemoveProject(){
        Elements elements = appContext.crawler.getCurrentDoc().select("span#RespResultTable  tr:not(:first-child)");
        if (elements.isEmpty()) {
            logger.log(Level.SEVERE, "No project to remove found.");
            return false;
        }

        for (Element element : elements) {
            if (element.select("span[id*=\":Status:\"]").html().equals("Проект") && !element.select("a[title=\"" + appContext.settings.number + "\"]").isEmpty()) {
                Elements spanDraft = appContext.crawler.getCurrentDoc().select("a#Draft");
                String hrefToDraft = spanDraft.attr("href");

                appContext.crawler.setUrl(appContext.crawler.baseUrl + hrefToDraft);
                Document draftDoc = appContext.crawler.getDoc(ServerPageEnum.DeleteDraft);

                Map<String, String> tempData = appContext.crawler.getFormData(draftDoc, "button[onclick*=\"customizeSubmitButton\"]");
                if (tempData == null) {
                    continue;
                }
                tempData.put("NegotiationNumberCriteria", appContext.settings.number);
                Document draftSearhing = appContext.crawler.SubmitAction(tempData, ServerPageEnum.SearchedDraf);

                Elements selectedDraft = draftSearhing.select("span#DraftResponsesTable input[name$=\":selected\"]");
                tempData = appContext.crawler.getFormData(draftSearhing, "button#DeleteButton");
                for (Element elementDraft : selectedDraft) {
                    tempData.put(elementDraft.attr("name"), "0");
                }
                Document notifyDeletedDoc = appContext.crawler.SubmitAction(tempData,ServerPageEnum.NotifyDeleted);
                appContext.crawler.SubmitAction(appContext.crawler.getFormData(notifyDeletedDoc, "button[onclick*=\"DeleteYesButton\"]"),ServerPageEnum.DeletedYes);
                
                return true;
            }
        }
        
        logger.log(Level.WARNING, "Project not found.");
        return false;
    }
    

    @Override
    public void Do() {
        RemoveProject();
        GoHomePage(appContext.crawler, appContext.crawler.getCurrentDoc());
    }
    
}

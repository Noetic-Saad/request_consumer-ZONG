package com.noeticworld.sgw.requestConsumer.service;

import com.noeticworld.sgw.util.ResponseTypeConstants;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class VendorPostBackService {

    Logger log = LoggerFactory.getLogger(VendorPostBackService.class.getName());


    @Autowired private ConfigurationDataManagerService configurationDataManagerService;


    public void sendVendorPostBack(Long vendorPlanId,String trackerId){
        System.out.println(trackerId);
        if(vendorPlanId== ResponseTypeConstants.NoeticPlan||vendorPlanId==ResponseTypeConstants.NoeticPlan1|| vendorPlanId==ResponseTypeConstants.NoeticPlan2){
            log.info("ZONG CONSUMER SERVICE  | VendorPostBackService CLASS | POSTBACK NOT SENT NOETIC PLAN");
        }
        else {
            String url = configurationDataManagerService.getVendorPostBackConfig(vendorPlanId).replaceAll("=none", "=" + trackerId);
            System.out.println("url = " + url);
            HttpResponse<String> response = Unirest.get(url)
                    .asString();
            log.info("ZONG CONSUMER SERVICE  | VendorPostBackService CLASS | POSTBACK SENT ON URL | " + url);
            System.out.println("response status " + response.getStatus());
            if (response.getStatus() == 200) {
                log.info("ZONG CONSUMER SERVICE  | VendorPostBackService CLASS | POSTBACK SENT FOR TRACKER-ID | " + trackerId);
            }
        }

    }



}

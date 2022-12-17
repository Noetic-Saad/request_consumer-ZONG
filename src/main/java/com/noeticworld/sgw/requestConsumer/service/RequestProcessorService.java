package com.noeticworld.sgw.requestConsumer.service;

import com.mashape.unirest.http.exceptions.UnirestException;
import com.noeticworld.sgw.requestConsumer.service.externalEvents.RequestHandlerManager;
import com.noeticworld.sgw.util.CustomMessage;
import com.noeticworld.sgw.util.RequestProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URISyntaxException;

@Service
public class RequestProcessorService {

    Logger log = LoggerFactory.getLogger(RequestProcessorService.class.getName());

    @Autowired private RequestHandlerManager requestHandlerManager;
    @Autowired private ConfigurationDataManagerService configurationDataManagerService;

    public void process(CustomMessage customMessage) throws URISyntaxException, UnirestException {
        RequestProperties requestProperties = new RequestProperties(customMessage);
        requestHandlerManager.manage(requestProperties);
        log.info("ZONG CONSUMER SERVICE  | REQUESTPROCESSORSERVICE CLASS | REQUEST PROCESSED FOR | "+ customMessage.getMsisdn());
    }
}

package com.noeticworld.sgw.requestConsumer.service.externalEvents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noeticworld.sgw.requestConsumer.entities.LoginRecordsEntity;
import com.noeticworld.sgw.requestConsumer.entities.VendorRequestsStateEntity;
import com.noeticworld.sgw.requestConsumer.repository.LogInRecordRepository;
import com.noeticworld.sgw.requestConsumer.repository.RedisRepository;
import com.noeticworld.sgw.requestConsumer.repository.VendorRequestRepository;
import com.noeticworld.sgw.requestConsumer.service.ConfigurationDataManagerService;
import com.noeticworld.sgw.util.RequestProperties;
import com.noeticworld.sgw.util.ResponseTypeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Service
public class LogOutEventHandler implements RequestEventHandler {

    Logger log = LoggerFactory.getLogger(LogOutEventHandler.class.getName());

    @Autowired
    LogInRecordRepository logInRecordRepository;
    @Autowired
    VendorRequestRepository vendorRequestRepository;
    @Autowired
    ConfigurationDataManagerService dataManagerService;

    @Autowired
    RedisRepository redisRepository;

    @Override
    public void handle(RequestProperties requestProperties) {

        log.info("ZONG CONSUMER SERVICE  | LOGOUTEVENTHANDLER CLASS | "+requestProperties.getMsisdn()+" | LOG OUT | REQUEST RECIEVED");
        LoginRecordsEntity loginRecordsEntity = logInRecordRepository.findTopBySessionIdAndMsisdn(requestProperties.getSessionId(), requestProperties.getMsisdn());
        if (loginRecordsEntity != null) {
            long sessionStartTime = loginRecordsEntity.getCtime().getTime();
            long sessionTotalTime = Timestamp.valueOf(LocalDateTime.now()).getTime() - sessionStartTime;
            loginRecordsEntity.setSessionTime(sessionTotalTime);
            loginRecordsEntity.setAcitve(false);
            logInRecordRepository.save(loginRecordsEntity);
            log.info("ZONG CONSUMER SERVICE  | LOGOUTEVENTHANDLER CLASS | "+requestProperties.getMsisdn()+" | LOGGED_OUT");
            createRequestState(dataManagerService.getResultStatusDescription(ResponseTypeConstants.LOGGED_OUT), ResponseTypeConstants.LOGGED_OUT, requestProperties);
        }else {
            log.info("ZONG CONSUMER SERVICE  | LOGOUTEVENTHANDLER CLASS | "+requestProperties.getMsisdn()+" | LOG OUT | " +requestProperties.getSessionId());
            log.info("ZONG CONSUMER SERVICE  | LOGOUTEVENTHANDLER CLASS | "+requestProperties.getMsisdn()+" | LOG OUT | " +requestProperties.getMsisdn());
            log.info("ZONG CONSUMER SERVICE  | LOGOUTEVENTHANDLER CLASS | "+requestProperties.getMsisdn()+" | LOG OUT | SESSIION ID NOT FOUND");
            createRequestState(dataManagerService.getResultStatusDescription(ResponseTypeConstants.SUBSCRIBER_NOT_FOUND),ResponseTypeConstants.SUBSCRIBER_NOT_FOUND,requestProperties);
        }
    }

    private void createRequestState(String resultStatusDescription, String resultStatus, RequestProperties requestProperties) {
        VendorRequestsStateEntity vendorRequestsStateEntity = null;
        vendorRequestsStateEntity = redisRepository.findVendorRequestStatus(requestProperties.getCorrelationId());
        if(vendorRequestsStateEntity == null)
        {
            vendorRequestRepository.findByCorrelationid(requestProperties.getCorrelationId());
        }

        boolean isNull = true;
        if(vendorRequestsStateEntity==null){
            while (isNull){
                vendorRequestsStateEntity  = redisRepository.findVendorRequestStatus(requestProperties.getCorrelationId());
                if(vendorRequestsStateEntity == null)
                {
                    vendorRequestsStateEntity = vendorRequestRepository.findByCorrelationid(requestProperties.getCorrelationId());
                }

                if(vendorRequestsStateEntity!=null){
                    isNull = false;
                }
            }
        }
        vendorRequestsStateEntity.setResultStatus(resultStatus);
        vendorRequestsStateEntity.setDescription(resultStatusDescription);
        vendorRequestRepository.save(vendorRequestsStateEntity);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            redisRepository.saveVendorRequest(vendorRequestsStateEntity.getCorrelationid(), objectMapper.writeValueAsString(vendorRequestsStateEntity));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}

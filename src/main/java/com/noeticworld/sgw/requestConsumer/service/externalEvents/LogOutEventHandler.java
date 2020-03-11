package com.noeticworld.sgw.requestConsumer.service.externalEvents;

import com.noeticworld.sgw.requestConsumer.entities.LoginRecordsEntity;
import com.noeticworld.sgw.requestConsumer.entities.VendorRequestsStateEntity;
import com.noeticworld.sgw.requestConsumer.repository.LogInRecordRepository;
import com.noeticworld.sgw.requestConsumer.repository.VendorRequestRepository;
import com.noeticworld.sgw.requestConsumer.service.ConfigurationDataManagerService;
import com.noeticworld.sgw.util.RequestProperties;
import com.noeticworld.sgw.util.ResponseTypeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
public class LogOutEventHandler implements RequestEventHandler {

    Logger log = LoggerFactory.getLogger(LogOutEventHandler.class.getName());

    @Autowired
    LogInRecordRepository logInRecordRepository;
    @Autowired
    VendorRequestRepository vendorRequestRepository;
    @Autowired
    ConfigurationDataManagerService dataManagerService;

    @Override
    public void handle(RequestProperties requestProperties) {

        LoginRecordsEntity loginRecordsEntity = logInRecordRepository.findTopBySessionIdAndMsisdn(requestProperties.getSessionId(), requestProperties.getMsisdn());
        if (loginRecordsEntity != null) {
            long sessionStartTime = loginRecordsEntity.getCtime().getTime();
            long sessionTotalTime = Timestamp.valueOf(LocalDateTime.now()).getTime() - sessionStartTime;
            loginRecordsEntity.setSessionTime(sessionTotalTime);
            loginRecordsEntity.setAcitve(false);
            logInRecordRepository.save(loginRecordsEntity);
            log.info("CONSUMER SERVICE | LOGOUTEVENTHANDLER CLASS | "+requestProperties.getMsisdn()+" | LOGGED_OUT");
            createRequestState(dataManagerService.getResultStatusDescription(ResponseTypeConstants.LOGGED_OUT), ResponseTypeConstants.LOGGED_OUT, requestProperties);
        }else {
            createRequestState(dataManagerService.getResultStatusDescription(ResponseTypeConstants.SUBSCRIBER_NOT_FOUND),ResponseTypeConstants.SUBSCRIBER_NOT_FOUND,requestProperties);
        }
    }

    private void createRequestState(String resultStatusDescription, String resultStatus, RequestProperties requestProperties) {
        VendorRequestsStateEntity vendorRequestsStateEntity = vendorRequestRepository.findByCorrelationid(requestProperties.getCorrelationId());
        boolean isNull = true;
        if(vendorRequestsStateEntity==null){
            while (isNull){
                vendorRequestsStateEntity  = vendorRequestRepository.findByCorrelationid(requestProperties.getCorrelationId());
                if(vendorRequestsStateEntity!=null){
                    isNull = false;
                }
            }
        }
        vendorRequestsStateEntity.setResultStatus(resultStatus);
        vendorRequestsStateEntity.setDescription(resultStatusDescription);
        vendorRequestRepository.save(vendorRequestsStateEntity);
    }

}

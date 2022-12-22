package com.noeticworld.sgw.requestConsumer.service.externalEvents;

import com.mashape.unirest.http.exceptions.UnirestException;
import com.noeticworld.sgw.requestConsumer.entities.*;
import com.noeticworld.sgw.requestConsumer.repository.*;
import com.noeticworld.sgw.requestConsumer.service.ConfigurationDataManagerService;
import com.noeticworld.sgw.requestConsumer.service.MtService;
import com.noeticworld.sgw.util.RequestActionCodeConstants;
import com.noeticworld.sgw.util.RequestProperties;
import com.noeticworld.sgw.util.ResponseTypeConstants;
import com.noeticworld.sgw.util.UserStatusTypeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;

@Service
public class UnsubscriptionEventHandler implements RequestEventHandler {

    Logger log = LoggerFactory.getLogger(UnsubscriptionEventHandler.class.getName());
    @Autowired
    MsisdnCorrelationsRepository msisdnCorrelationsRepository;
    @Autowired
    MtService mtService;
    private String DBSS_API = "http://localhost:10001";
    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private UserStatusRepository userStatusRepository;
    @Autowired
    private VendorRequestRepository requestRepository;
    @Autowired
    private ConfigurationDataManagerService dataService;

    @Autowired
    private RedisRepository redisRepository;

    @Override
    public void handle(RequestProperties requestProperties) throws UnirestException {
        log.info("UNSUBSCRIBE EVENT HANDLER CLASS | REQUEST RECEIVED FOR MSISDN " + requestProperties.getMsisdn() +
                " | ACTION  | " + requestProperties.getRequestAction());

        EventTypesEntity eventTypesEntity = dataService.getRequestEventsEntity(requestProperties.getRequestAction());
        UsersEntity _user = usersRepository.findByMsisdn(requestProperties.getMsisdn());

        if (_user == null) {
            try {
                log.info("UNSUBSCRIBE EVENT HANDLER CLASS | MSISDN " + requestProperties.getMsisdn() + " NOT FOUND");
            } finally {
                createResponse(ResponseTypeConstants.SUBSCRIBER_NOT_FOUND, requestProperties.getCorrelationId());
            }
        }
//        else if (_user.getOperatorId() == 1 && !requestProperties.isFromEDA()) {
//            // DBSS flow for Jazz | isFromEDA: false on first request from user.
//            createMsisdnCorrelation(requestProperties);
//            HttpResponse<String> response =
//                    Unirest.get(DBSS_API + "/dbss/product-deactivation/" + requestProperties.getMsisdn() + "/" +
//                            requestProperties.getCorrelationId()).asString();
//            log.info("UNSUBSCRIBE EVENT HANDLER CLASS | DBSS | " + requestProperties.getMsisdn() + " | " + response.getStatus() +
//                    " | " + response.getBody());
//            return;
//        }
        else {
            VendorPlansEntity vendorPlans = dataService.getVendorPlans(_user.getVendorPlanId());
            String resultCode = "";
            try {
                if (eventTypesEntity.getCode().equals(RequestActionCodeConstants.UNSUBSCRIPTION_REQUEST_TELCO_INITIATED)) {
                    log.info("UNSUBSCRIBE EVENT HANDLER CLASS | " + requestProperties.getMsisdn() + " | OPERATOR UNSUB REQUEST");
                    resultCode = changeUserStatus(_user, vendorPlans.getSubCycle(), dataService.getUserStatusTypeId(UserStatusTypeConstants.TELCOUNSUB));
                } else {
                    log.info("UNSUBSCRIBE EVENT HANDLER CLASS | " + requestProperties.getMsisdn() + " | USER UNSUB REQUEST");
                    resultCode = changeUserStatus(_user, vendorPlans.getSubCycle(), dataService.getUserStatusTypeId(UserStatusTypeConstants.UNSUBSCRIBED));
                    createResponse(resultCode, requestProperties.getCorrelationId());
                }
                log.info("UNSUBSCRIBE EVENT HANDLER CLASS | " + requestProperties.getMsisdn() + " | UNSUBSCRIBED FROM SERVICE");
            } catch (Exception e) {
                log.info("UNSUBSCRIBE EVENT HANDLER CLASS | " + requestProperties.getMsisdn() + " | EXCEPTION | " + e.getMessage());
            }
            if (vendorPlans.getMtResponse() == 1) {
                mtService.sendUnsubMt(requestProperties.getMsisdn(), vendorPlans);
            }
        }
    }

    private void createMsisdnCorrelation(RequestProperties requestProperties) {
        MsisdnCorrelations msisdnCorrelations = new MsisdnCorrelations();
        msisdnCorrelations.setMsisdn(requestProperties.getMsisdn());
        msisdnCorrelations.setCorrelationId(requestProperties.getCorrelationId());
        msisdnCorrelations.setCdate(Timestamp.from(Instant.now()));
        msisdnCorrelationsRepository.save(msisdnCorrelations);
    }

    private String changeUserStatus(UsersEntity users, Integer subCycleId, Integer statusId) {
        log.info("Statud Id For Msisdn" + users.getMsisdn() + " status_id :" + statusId);
        UsersStatusEntity entity = userStatusRepository.findTopById(users.getUserStatusId());
        if (entity == null) {
            log.info("ZONG CONSUMER SERVICE  | UNSUBSCRIPTIONEVENTHANDLER CLASS | MSISDN " + users.getMsisdn() + " STATUS ENTITY NOT FOUND");
            return ResponseTypeConstants.SUBSCRIBER_NOT_FOUND;
        } else if (entity.getStatusId() == dataService.getUserStatusTypeId(UserStatusTypeConstants.SUBSCRIBED)) {
            UsersStatusEntity entity1 = new UsersStatusEntity();
            entity1.setUserId(users.getId());
            entity1.setStatusId(statusId);
            entity1.setVendorPlanId(users.getVendorPlanId());
            entity1.setCdate(new Timestamp(new Date().getTime()));
            entity1.setExpiryDatetime(new Timestamp(new Date().getTime()));
            entity1.setSubCycleId(subCycleId);
            entity1.setAttempts(0);
            long userStatusId = userStatusRepository.save(entity1).getId();
            users.setUserStatusId((int) userStatusId);
            users.setModifyDate(Timestamp.valueOf(LocalDateTime.now()));
            usersRepository.save(users);
            return ResponseTypeConstants.UNSUSBCRIBED_SUCCESSFULL;
        } else if (entity.getStatusId() == dataService.getUserStatusTypeId(UserStatusTypeConstants.RENEWALUNSUB)) {
            UsersStatusEntity entity1 = new UsersStatusEntity();
            entity1.setUserId(users.getId());
            entity1.setStatusId(statusId);
            entity1.setVendorPlanId(users.getVendorPlanId());
            entity1.setCdate(new Timestamp(new Date().getTime()));
            entity1.setExpiryDatetime(new Timestamp(new Date().getTime()));
            entity1.setSubCycleId(subCycleId);
            entity1.setAttempts(0);
            long userStatusId = userStatusRepository.save(entity1).getId();
            users.setUserStatusId((int) userStatusId);
            users.setModifyDate(Timestamp.valueOf(LocalDateTime.now()));
            usersRepository.save(users);
            return ResponseTypeConstants.UNSUSBCRIBED_SUCCESSFULL;
        } else if (entity.getStatusId() != dataService.getUserStatusTypeId(UserStatusTypeConstants.SUBSCRIBED)) {
            log.info("ZONG CONSUMER SERVICE  | UNSUBSCRIPTIONEVENTHANDLER CLASS | MSISDN " + users.getMsisdn() + " ALREADY UNSUBSCRIBED");
            return ResponseTypeConstants.ALREADY_UNSUBSCRIBED;
        } else {
            return ResponseTypeConstants.OTHER_ERROR;
        }
    }

    private void createResponse(String resultStatus, String correlationId) {
        VendorRequestsStateEntity entity = (VendorRequestsStateEntity) requestRepository.findByCorrelationid(correlationId);
        if(entity == null)
        {
           entity = requestRepository.findByCorrelationid(correlationId);
        }
        boolean isNull = true;
        if (entity == null) {
            log.info("ZONG CONSUMER SERVICE  | UNSUBSCRIPTIONEVENTHANDLER CLASS | NULL ENTITY");
            while (isNull) {
                entity = requestRepository.findByCorrelationid(correlationId);
                if(entity == null)
                {
                    entity = requestRepository.findByCorrelationid(correlationId);
                }
                if (entity != null) {
                    isNull = false;
                }
            }
        }
        entity.setCdatetime(Timestamp.valueOf(LocalDateTime.now()));
        entity.setFetched(false);
        entity.setResultStatus(resultStatus);
        if (resultStatus.equals(ResponseTypeConstants.ALREADY_UNSUBSCRIBED)) {
            entity.setDescription(ResponseTypeConstants.ALREAD_SUBSCRIBED_MSG);
        } else if (resultStatus.equals(ResponseTypeConstants.UNSUSBCRIBED_SUCCESSFULL)) {
            entity.setDescription(ResponseTypeConstants.UNSUBSCRIBEDFULL_MSG);
        } else if (resultStatus.equals(ResponseTypeConstants.SUBSCRIBER_NOT_FOUND)) {
            entity.setDescription(ResponseTypeConstants.SUBSCRIBER_NOT_FOUND_MSG);
        } else {
            entity.setResultStatus(ResponseTypeConstants.OTHER_ERROR);
            entity.setDescription(ResponseTypeConstants.OTHER_ERROR_MSG);
        }
        requestRepository.save(entity);
        log.info("ZONG CONSUMER SERVICE  | UNSUBSCRIPTIONEVENTHANDLER CLASS | RESPONSE CREATED");
        log.info("ZONG CONSUMER SERVICE  | UNSUBSCRIPTIONEVENTHANDLER CLASS | " + entity.getResultStatus() + " | REQUEST STATUS SAVED IN REDIS");
    }
}

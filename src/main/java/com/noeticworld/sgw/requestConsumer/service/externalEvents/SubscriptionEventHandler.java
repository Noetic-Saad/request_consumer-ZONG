package com.noeticworld.sgw.requestConsumer.service.externalEvents;

import com.noeticworld.sgw.requestConsumer.entities.*;
import com.noeticworld.sgw.requestConsumer.repository.*;
import com.noeticworld.sgw.requestConsumer.service.*;
import com.noeticworld.sgw.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;

@Service
public class SubscriptionEventHandler implements RequestEventHandler {

    Logger log = LoggerFactory.getLogger(SubscriptionEventHandler.class.getName());

    @Autowired private UsersRepository usersRepository;
    @Autowired private MtService mtService;
    @Autowired private VendorRequestRepository requestRepository;
    @Autowired private UserStatusRepository userStatusRepository;
    @Autowired private SubscriptionSettingRepository subscriptionSettingRepository;
    @Autowired private BillingService billingService;
    @Autowired private ConfigurationDataManagerService dataService;
    @Autowired private VendorReportRepository vendorReportRepository;
    @Autowired private OtpRecordRepository otpRecordRepository;
    @Autowired private LogInRecordRepository logInRecordRepository;
    @Autowired private VendorPostBackService vendorPostBackService;
    @Autowired private VendorRequestService vendorRequestService;


    @Override
    public void handle(RequestProperties requestProperties) {

        if (requestProperties.isOtp()) {
            if(requestProperties.getOtpNumber()==0){
                createResponse(dataService.getResultStatusDescription(ResponseTypeConstants.INVALID_OTP), ResponseTypeConstants.INVALID_OTP, requestProperties.getCorrelationId());
                log.info("CONSUMER SERVICE | SUBSCIPTIONEVENTHANDLER CLASS | OTP IS INVALID FOR | "+requestProperties.getMsisdn());
                return;
            }
            OtpRecordsEntity otpRecordsEntity = otpRecordRepository.findTopByMsisdnAndOtpNumber(requestProperties.getMsisdn(), (int) requestProperties.getOtpNumber());
            if (otpRecordsEntity != null && otpRecordsEntity.getOtpNumber() == requestProperties.getOtpNumber()) {
                handleSubRequest(requestProperties);
            } else {
                log.info("CONSUMER SERVICE | SUBSCIPTIONEVENTHANDLER CLASS | OTP IS INVALID FOR | "+requestProperties.getMsisdn());
                createResponse(dataService.getResultStatusDescription(ResponseTypeConstants.INVALID_OTP), ResponseTypeConstants.INVALID_OTP, requestProperties.getCorrelationId());
            }
        }else {
            handleSubRequest(requestProperties);
        }


    }

    public void handleSubRequest(RequestProperties requestProperties) {

        VendorPlansEntity entity = null;
        UsersEntity _user = usersRepository.findByMsisdn(requestProperties.getMsisdn());
        boolean exisingUser = true;
        if (_user == null) {
            exisingUser = false;
            entity = dataService.getVendorPlans(requestProperties.getVendorPlanId());
            log.info("CONSUMER SERVICE | SUBSCIPTIONEVENTHANDLER CLASS | REGISTRING NEW USER");
            _user = registerNewUser(requestProperties,entity);

        }

        if (exisingUser) {
            UsersStatusEntity _usersStatusEntity = userStatusRepository.findTopById(_user.getId());
            if(_usersStatusEntity == null){
                createUserStatusEntity(requestProperties, _user, UserStatusTypeConstants.SUBSCRIBED);
                createResponse(dataService.getResultStatusDescription(ResponseTypeConstants.ALREADY_SUBSCRIBED), ResponseTypeConstants.ALREADY_SUBSCRIBED, requestProperties.getCorrelationId());
               // processUserRequest(requestProperties, _user);
            }else if (_usersStatusEntity.getStatusId() == dataService.getUserStatusTypeId(UserStatusTypeConstants.BLOCKED)) {
                log.info("CONSUMER SERVICE | SUBSCIPTIONEVENTHANDLER CLASS | MSISDN " + requestProperties.getMsisdn() + " IS BLOCKED OR BLACKLISTED");
                createResponse(dataService.getResultStatusDescription(ResponseTypeConstants.USER_IS_BLOCKED), ResponseTypeConstants.USER_IS_BLOCKED, requestProperties.getCorrelationId());
            } else {
                if (_usersStatusEntity.getStatusId() == dataService.getUserStatusTypeId(UserStatusTypeConstants.SUBSCRIBED)) {

                    if (_usersStatusEntity == null ||
                            _usersStatusEntity.getExpiryDatetime() == null ||
                            _usersStatusEntity.getExpiryDatetime().
                                    before(new Timestamp(System.currentTimeMillis()))) {
                        processUserRequest(requestProperties, _user);
                    } else {
                        log.info("CONSUMER SERVICE | SUBSCIPTIONEVENTHANDLER CLASS | MSISDN " + requestProperties.getMsisdn() + " IS ALREADY SUBSCRIBED");
                        createResponse(dataService.getResultStatusDescription(ResponseTypeConstants.ALREADY_SUBSCRIBED), ResponseTypeConstants.ALREADY_SUBSCRIBED, requestProperties.getCorrelationId());
                    }
                }else {
                    processUserRequest(requestProperties, _user);
                }
            }
        } else {
           processUserRequest(requestProperties, _user);

        }
    }


    private UsersEntity registerNewUser(RequestProperties requestProperties,VendorPlansEntity entity) {
        UsersEntity usersEntity = new UsersEntity();
        usersEntity.setMsisdn(requestProperties.getMsisdn());
        usersEntity.setVendorPlanId(requestProperties.getVendorPlanId());
        usersEntity.setCdate(new Date());
        usersEntity.setModifyDate(Timestamp.valueOf(LocalDateTime.now()));
        usersEntity.setOperatorId(Long.valueOf(entity.getOperatorId()));
        if(requestProperties.isOtp()){
            usersEntity.setIsOtpVerifired(1);
        }else {
            usersEntity.setIsOtpVerifired(0);
        }
        usersEntity.setTrackerId(requestProperties.getTrackerId());
        return usersRepository.save(usersEntity);
    }

    private UsersStatusEntity createUserStatusEntity(RequestProperties requestProperties, UsersEntity _user, String userStatusType) {
        log.info("Saving Record In UserStatus Entiry" + requestProperties.getMsisdn() + " | Setting Expiry"+LocalDate.now().plusDays(2).atTime(23,59));
        UsersStatusEntity usersStatusEntity = new UsersStatusEntity();
        VendorPlansEntity entity = dataService.getVendorPlans(requestProperties.getVendorPlanId());
        usersStatusEntity.setCdate(Timestamp.from(Instant.now()));
        usersStatusEntity.setStatusId(dataService.getUserStatusTypeId(userStatusType));
        usersStatusEntity.setVendorPlanId(requestProperties.getVendorPlanId());
        SubscriptionSettingEntity subscriptionSettingEntity = dataService.getSubscriptionSetting(entity.getId());
        String[] expiryTime = subscriptionSettingEntity.getExpiryTime().split(":");
        int hours = Integer.parseInt(expiryTime[0]);
        int minutes = Integer.parseInt(expiryTime[1]);
        usersStatusEntity.setSubCycleId(entity.getSubCycle());
        if(entity.getSubCycle()==1){
            usersStatusEntity.setExpiryDatetime(Timestamp.valueOf(LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.of(hours, minutes))));
        }else {
            usersStatusEntity.setExpiryDatetime(Timestamp.valueOf(LocalDateTime.of(LocalDate.now().plusDays(7), LocalTime.of(hours, minutes))));
        }
        usersStatusEntity.setAttempts(1);
        usersStatusEntity.setUserId(_user.getId());
        usersStatusEntity.setfreeTrialExpiry(Timestamp.valueOf(LocalDate.now().plusDays(2).atTime(23,59)));
        usersStatusEntity = userStatusRepository.save(usersStatusEntity);
        updateUserStatus(_user, usersStatusEntity.getId(),requestProperties.getVendorPlanId());
        userStatusRepository.flush();
        log.info("CONSUMER SERVICE | SUBSCIPTIONEVENTHANDLER CLASS | " + requestProperties.getMsisdn() + " | SUBSCRIBED");
        return usersStatusEntity;
    }

    private void processUserRequest(RequestProperties requestProperties, UsersEntity _user) {
        FiegnResponse fiegnResponse = billingService.charge(requestProperties);
        if(fiegnResponse==null){
            return;
        }
        VendorPlansEntity entity = dataService.getVendorPlans(requestProperties.getVendorPlanId());
        if (fiegnResponse.getCode() == Integer.parseInt(ResponseTypeConstants.SUSBCRIBED_SUCCESSFULL) || fiegnResponse.getCode() == Integer.parseInt(ResponseTypeConstants.ALREADY_SUBSCRIBED)) {
            if (entity.getMtResponse() == 1 && fiegnResponse.getCode() == Integer.parseInt(ResponseTypeConstants.SUSBCRIBED_SUCCESSFULL)) {
                mtService.sendSubMt(requestProperties.getMsisdn(), entity);
            }
            try {
               // createUserStatusEntity(requestProperties, _user, UserStatusTypeConstants.SUBSCRIBED);
                saveLogInRecord(requestProperties, entity.getId());
                List<VendorReportEntity> vendorReportEntity = vendorReportRepository.findByMsisdnAndVenodorPlanId(requestProperties.getMsisdn(), (int) requestProperties.getVendorPlanId());
                if(vendorReportEntity.isEmpty()) {
                    log.info("CALLING VENDOR POSTBACK");
                    vendorPostBackService.sendVendorPostBack(entity.getId(), requestProperties.getTrackerId());
                    createVendorReport(requestProperties,1,_user.getOperatorId().intValue());
                }else {
                    createVendorReport(requestProperties,0,_user.getOperatorId().intValue());
                }
            }finally {
                createResponse(fiegnResponse.getMsg(), ResponseTypeConstants.SUSBCRIBED_SUCCESSFULL, requestProperties.getCorrelationId());
            }
        } else if (fiegnResponse.getCode() == Integer.parseInt(ResponseTypeConstants.INSUFFICIENT_BALANCE)) {
            createResponse(fiegnResponse.getMsg(), ResponseTypeConstants.INSUFFICIENT_BALANCE, requestProperties.getCorrelationId());
        } else if (fiegnResponse.getCode() == Integer.parseInt(ResponseTypeConstants.ALREADY_SUBSCRIBED)) {
            createResponse(fiegnResponse.getMsg(), ResponseTypeConstants.ALREADY_SUBSCRIBED, requestProperties.getCorrelationId());
        } else if (fiegnResponse.getCode() == Integer.parseInt(ResponseTypeConstants.UNAUTHORIZED_REQUEST)) {
            createResponse(fiegnResponse.getMsg(), ResponseTypeConstants.UNAUTHORIZED_REQUEST, requestProperties.getCorrelationId());
        } else {
            createResponse(fiegnResponse.getMsg(), ResponseTypeConstants.OTHER_ERROR, requestProperties.getCorrelationId());
        }
    }

    /*private void createResponse(String desc, String resultStatus, String correlationId) {
        log.info("CONSUMER SERVICE | SUBSCIPTIONEVENTHANDLER CLASS | " + correlationId + " | TRYING TO CREATE RESPONSE");
        VendorRequestsStateEntity entity = null;
        boolean isNull = true;
        if(entity==null){
            while (isNull){
                entity  = requestRepository.findByCorrelationid(correlationId);
                System.out.println("ENTITY IS NULL TAKING TIME");
                if(entity!=null){
                    isNull = false;
                }
            }
        }
        entity.setCdatetime(Timestamp.valueOf(LocalDateTime.now()));
        entity.setFetched(false);
        entity.setResultStatus(resultStatus);
        entity.setDescription(desc);
        VendorRequestsStateEntity vre = requestRepository.save(entity);
        log.info("CONSUMER SERVICE | SUBSCIPTIONEVENTHANDLER CLASS | " + vre.getResultStatus() + " | REQUEST STATE UPDATED");
    }*/

    private void createResponse(String desc, String resultStatus, String correlationId) {
        log.info("CONSUMER SERVICE | SUBSCIPTIONEVENTHANDLER CLASS | " + correlationId + " | TRYING TO CREATE RESPONSE");
        VendorRequestsStateEntity entity = new VendorRequestsStateEntity();
        entity.setCdatetime(Timestamp.valueOf(LocalDateTime.now()));
        entity.setFetched(false);
        entity.setResultStatus(resultStatus);
        entity.setDescription(desc);
        entity.setCorrelationid(correlationId);
        vendorRequestService.saveVendorRequest(entity);
    }

    private void updateUserStatus(UsersEntity user, long userStatusId,long vendorPLanId) {
        user.setUserStatusId((int) userStatusId);
        user.setModifyDate(Timestamp.valueOf(LocalDateTime.now()));
        if(user.getVendorPlanId()!=vendorPLanId){
            user.setVendorPlanId(vendorPLanId);
        }
        usersRepository.save(user);
    }

    private void createVendorReport(RequestProperties requestProperties,int postBackSent,Integer operatorId) {
        VendorReportEntity vendorReportEntity = new VendorReportEntity();
        vendorReportEntity.setCdate(Timestamp.valueOf(LocalDateTime.now()));
        vendorReportEntity.setMsisdn(requestProperties.getMsisdn());
        vendorReportEntity.setVenodorPlanId((int) requestProperties.getVendorPlanId());
        vendorReportEntity.setTrackerId(requestProperties.getTrackerId());
        vendorReportEntity.setPostbackSent(postBackSent);
        vendorReportEntity.setOperatorId(operatorId);
        vendorReportRepository.save(vendorReportEntity);
    }

    private void saveLogInRecord(RequestProperties requestProperties,long vendorPlanId){
        LoginRecordsEntity loginRecordsEntity = new LoginRecordsEntity();
        loginRecordsEntity.setCtime(Timestamp.valueOf(LocalDateTime.now()));
        loginRecordsEntity.setSessionId(requestProperties.getSessionId());
        loginRecordsEntity.setAcitve(false);
        loginRecordsEntity.setRemoteServerIp(requestProperties.getRemoteServerIp());
        loginRecordsEntity.setLocalServerIp(requestProperties.getLocalServerIp());
        loginRecordsEntity.setMsisdn(requestProperties.getMsisdn());
        loginRecordsEntity.setVendorPlanId(vendorPlanId);
        logInRecordRepository.save(loginRecordsEntity);
    }

}
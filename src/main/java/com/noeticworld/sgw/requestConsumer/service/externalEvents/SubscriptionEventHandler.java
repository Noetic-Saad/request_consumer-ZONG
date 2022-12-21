package com.noeticworld.sgw.requestConsumer.service.externalEvents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import com.noeticworld.sgw.requestConsumer.entities.*;
import com.noeticworld.sgw.requestConsumer.repository.*;
import com.noeticworld.sgw.requestConsumer.service.BillingService;
import com.noeticworld.sgw.requestConsumer.service.ConfigurationDataManagerService;
import com.noeticworld.sgw.requestConsumer.service.MtService;
import com.noeticworld.sgw.requestConsumer.service.VendorPostBackService;
import com.noeticworld.sgw.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
public class SubscriptionEventHandler implements RequestEventHandler {


    Logger log = LoggerFactory.getLogger(SubscriptionEventHandler.class.getName());
    @Autowired
    ConfigurationDataManagerService dataManagerService;
    @Autowired
    MtClient mtClient;
    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private MtService mtService;
    @Autowired
    private VendorRequestRepository requestRepository;
    @Autowired
    private UserStatusRepository userStatusRepository;
    @Autowired
    private SubscriptionSettingRepository subscriptionSettingRepository;
    @Autowired
    private BillingService billingService;
    @Autowired
    private ConfigurationDataManagerService dataService;
    @Autowired
    private VendorReportRepository vendorReportRepository;
    @Autowired
    private OtpRecordRepository otpRecordRepository;
    @Autowired
    private LogInRecordRepository logInRecordRepository;
    @Autowired
    private VendorPostBackService vendorPostBackService;
    @Autowired
    private LoginRepository loginRepository;

    @Autowired
    private RedisRepository redisRepository;
    private long otpnumber = 0;
    @Override
    public void handle(RequestProperties requestProperties) throws URISyntaxException {
//        UsersEntity _user = usersRepository.findByMsisdn(requestProperties.getMsisdn());

        if (requestProperties.isOtp()) {
            if (requestProperties.getOtpNumber() == 0) {
                createResponse(dataService.getResultStatusDescription(ResponseTypeConstants.INVALID_OTP), ResponseTypeConstants.INVALID_OTP, requestProperties.getCorrelationId());
                log.info("ZONG CONSUMER SERVICE  | SUBSCIPTIONEVENTHANDLER CLASS | OTP IS INVALID FOR | " + requestProperties.getMsisdn());
                return;
            }
            VendorPlansEntity vendorPlansEntity = dataManagerService.getVendorPlans(requestProperties.getVendorPlanId());

//            if(vendorPlansEntity.getOperatorId()==1){
//                //new jazz api
//               String msg= verifyOTP(requestProperties.getMsisdn(),requestProperties.getOtpNumber());
//                if(msg.equals("success")){
//                    loginRepository.updateLoginTable(requestProperties.getMsisdn());
//                    handleSubRequest(requestProperties);
//                }
//                else{
//                    createResponse(dataService.getResultStatusDescription(ResponseTypeConstants.INVALID_OTP), ResponseTypeConstants.INVALID_OTP, requestProperties.getCorrelationId());
//
//                }
//
//            }
            //else1 start
            if(vendorPlansEntity.getOperatorId()==4){

            //OtpRecordsEntity otpRecordsEntity = otpRecordRepository.findtoprecord(requestProperties.getMsisdn());
            OtpRecordsEntity otpRecordsEntity = getTopOtpRecordFromMsidn(requestProperties.getMsisdn());
            log.info("SUBSCRIPTION EVENT HANDLER CLASS | OTP RECORD FOUND IN DB IS " + otpRecordsEntity.getOtpNumber());
            if (otpRecordsEntity != null && otpRecordsEntity.getOtpNumber() == requestProperties.getOtpNumber()) {
                otpnumber = requestProperties.getOtpNumber();
                loginRepository.updateLoginTable(requestProperties.getMsisdn());
                handleSubRequest(requestProperties);
            } else {
                log.info("ZONG CONSUMER SERVICE  | SUBSCIPTIONEVENTHANDLER CLASS | OTP IS INVALID FOR | " + requestProperties.getMsisdn());
                createResponse(dataService.getResultStatusDescription(ResponseTypeConstants.INVALID_OTP), ResponseTypeConstants.INVALID_OTP, requestProperties.getCorrelationId());
            }
           //else1 end
            }
        } else {
            handleSubRequest(requestProperties);
        }


    }

    public void handleSubRequest(RequestProperties requestProperties) {
        //Free Trial Changes
       /* VendorPlansEntity entity = null;
        UsersEntity _user = usersRepository.findByMsisdn(requestProperties.getMsisdn());
        boolean exisingUser = true;
        if (_user == null) {
            exisingUser = false;


            RestTemplate template = new RestTemplate();
            RequestPropertiesCheckBalance rq=new RequestPropertiesCheckBalance();
            rq.setMsisdn(requestProperties.getMsisdn());
            rq.setOperatorId(10);
            rq.setTransactionId("111");
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<RequestPropertiesCheckBalance> requestEntity =
                    new HttpEntity<>(rq, headers);
            HttpEntity<AppResponse>  response =
                    template.exchange("http://192.168.127.57:9071/checkbalance", HttpMethod.POST, requestEntity,
                            AppResponse.class);
         if(response==null){
             log.info("Null Response From Api "+response.getBody().getCode() +response.getBody().getMsg());
                }
            else  if(response!=null && response.getBody().getCode()==1000){
                log.info("Jazz Customer from checkbalancedate api"+ response.getBody().getCode() +response.getBody().getMsg());
             entity = dataService.getVendorPlans(requestProperties.getVendorPlanId());
             log.info("ZONG CONSUMER SERVICE  | SUBSCIPTIONEVENTHANDLER CLASS | REGISTRING NEW USER "+requestProperties.getVendorPlanId());
             _user = registerNewUser(requestProperties,entity);
             if(requestProperties.getVendorPlanId()==3 ||requestProperties.getVendorPlanId()==12 ||requestProperties.getVendorPlanId()==16) {

                 try {
                     VendorPlansEntity vendorPlans = null;
                     createUserStatusEntityFreeTrial(requestProperties, _user, UserStatusTypeConstants.SUBSCRIBED);
                     saveLogInRecord(requestProperties, entity.getId());
                     vendorPlans = dataService.getVendorPlans(_user.getVendorPlanId());
                     List<VendorReportEntity> vendorReportEntity = vendorReportRepository.findByMsisdnAndVenodorPlanId(requestProperties.getMsisdn(), (int) requestProperties.getVendorPlanId());
                     if(vendorPlans.getMtResponse() == 1) {
                         mtService.sendSubMt(requestProperties.getMsisdn(), vendorPlans);
                     }
                     if (vendorReportEntity.isEmpty()) {
                         if (requestProperties.getVendorPlanId() == 3 || requestProperties.getVendorPlanId() == 4 || requestProperties.getVendorPlanId() == 5) {
                             createVendorReport(requestProperties, 1, _user.getOperatorId().intValue());
                         } else {
                             log.info("CALLING VENDOR POSTBACK");
                             vendorPostBackService.sendVendorPostBack(entity.getId(), requestProperties.getTrackerId());
                             createVendorReport(requestProperties, 1, _user.getOperatorId().intValue());
                         }
                     } else {
                         createVendorReport(requestProperties, 0, _user.getOperatorId().intValue());
                     }
                 } finally {
                     createResponse("Subscribe For Free Trial", ResponseTypeConstants.SUSBCRIBED_SUCCESSFULL, requestProperties.getCorrelationId());
                 }
             }
             else{
                 processUserRequest(requestProperties,_user);

             }

            }
            else{
                log.info("Other Response"+ response.getBody().getCode() +response.getBody().getMsg());
            }


        }

        if (exisingUser) {
            UsersStatusEntity _usersStatusEntity = userStatusRepository.findTopById(_user.getId());
            if(_usersStatusEntity == null){
                processUserRequest(requestProperties, _user);
            }else if (_usersStatusEntity.getStatusId() == dataService.getUserStatusTypeId(UserStatusTypeConstants.BLOCKED)) {
                log.info("ZONG CONSUMER SERVICE  | SUBSCIPTIONEVENTHANDLER CLASS | MSISDN " + requestProperties.getMsisdn() + " IS BLOCKED OR BLACKLISTED");
                createResponse(dataService.getResultStatusDescription(ResponseTypeConstants.USER_IS_BLOCKED), ResponseTypeConstants.USER_IS_BLOCKED, requestProperties.getCorrelationId());
            } else {
                if (_usersStatusEntity.getStatusId() == dataService.getUserStatusTypeId(UserStatusTypeConstants.SUBSCRIBED)) {

                    if (_usersStatusEntity == null ||
                            _usersStatusEntity.getExpiryDatetime() == null ||
                            _usersStatusEntity.getExpiryDatetime().
                                    before(new Timestamp(System.currentTimeMillis()))) {
                        processUserRequest(requestProperties, _user);
                    } else {
                        log.info("ZONG CONSUMER SERVICE  | SUBSCIPTIONEVENTHANDLER CLASS | MSISDN " + requestProperties.getMsisdn() + " IS ALREADY SUBSCRIBED");
                        createResponse(dataService.getResultStatusDescription(ResponseTypeConstants.ALREADY_SUBSCRIBED), ResponseTypeConstants.ALREADY_SUBSCRIBED, requestProperties.getCorrelationId());
                    }
                }else {
                    processUserRequest(requestProperties, _user);
                }
            }
        } else {
            processUserRequest(requestProperties, _user);
        }*/


        VendorPlansEntity entity = null;
        UsersEntity _user = usersRepository.findByMsisdn(requestProperties.getMsisdn());
        boolean exisingUser = true;

        if (_user == null) {
            exisingUser = false;
            entity = dataService.getVendorPlans(requestProperties.getVendorPlanId());
            log.info("ZONG CONSUMER SERVICE  | SUBSCIPTIONEVENTHANDLER CLASS | REGISTRING NEW USER");
            _user = registerNewUser(requestProperties, entity);

        }

        if (exisingUser) {
            UsersStatusEntity _usersStatusEntity = userStatusRepository.findTopById(_user.getId());
            if (_usersStatusEntity == null) {
                processUserRequest(requestProperties, _user);
            } else if (_usersStatusEntity.getStatusId() == dataService.getUserStatusTypeId(UserStatusTypeConstants.BLOCKED)) {
                log.info("ZONG CONSUMER SERVICE  | SUBSCIPTIONEVENTHANDLER CLASS | MSISDN " + requestProperties.getMsisdn() + " IS BLOCKED OR BLACKLISTED");
                createResponse(dataService.getResultStatusDescription(ResponseTypeConstants.USER_IS_BLOCKED), ResponseTypeConstants.USER_IS_BLOCKED, requestProperties.getCorrelationId());
            } else {
                if (_usersStatusEntity.getStatusId() == dataService.getUserStatusTypeId(UserStatusTypeConstants.SUBSCRIBED)) {

                    if (_usersStatusEntity == null ||
                            _usersStatusEntity.getExpiryDatetime() == null ||
                            _usersStatusEntity.getExpiryDatetime().
                                    before(new Timestamp(System.currentTimeMillis()))) {
                        processUserRequest(requestProperties, _user);
                    } else {
                        log.info("ZONG CONSUMER SERVICE  | SUBSCIPTIONEVENTHANDLER CLASS | MSISDN " + requestProperties.getMsisdn() + " IS ALREADY SUBSCRIBED");
                        createResponse(dataService.getResultStatusDescription(ResponseTypeConstants.ALREADY_SUBSCRIBED), ResponseTypeConstants.ALREADY_SUBSCRIBED, requestProperties.getCorrelationId());
                    }
                } else {
                    processUserRequest(requestProperties, _user);
                }
            }
        } else {
            processUserRequest(requestProperties, _user);
        }
    }


    private UsersEntity registerNewUser(RequestProperties requestProperties, VendorPlansEntity entity) {
        UsersEntity usersEntity = new UsersEntity();
        usersEntity.setMsisdn(requestProperties.getMsisdn());
        usersEntity.setVendorPlanId(requestProperties.getVendorPlanId());
        usersEntity.setCdate(new Date());
        usersEntity.setModifyDate(Timestamp.valueOf(LocalDateTime.now()));
        usersEntity.setOperatorId(Long.valueOf(entity.getOperatorId()));
        if (requestProperties.isOtp()) {
            usersEntity.setIsOtpVerifired(1);
        } else {
            usersEntity.setIsOtpVerifired(0);
        }
        usersEntity.setTrackerId(requestProperties.getTrackerId());
        return usersRepository.save(usersEntity);
    }

    private UsersStatusEntity createUserStatusEntity(RequestProperties requestProperties, UsersEntity _user,
                                                     String userStatusType, boolean isZongFreeTrialUser) {
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
//        if (entity.getSubCycle() == 1) {
//            usersStatusEntity.setExpiryDatetime(Timestamp.valueOf(LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.of(hours, minutes))));
//        }
        if (entity.getSubCycle() == 7) {
//            if (isZongFreeTrialUser) {
//                // If this is a zong free trial user, give one day free trial and if gets charged, give 7 day subscription.
//                usersStatusEntity.setExpiryDatetime(Timestamp.valueOf(LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.of(hours, minutes))));
//            }
            if((!isZongFreeTrialUser)) {
                usersStatusEntity.setExpiryDatetime(Timestamp.valueOf(LocalDateTime.of(LocalDate.now().plusDays(7), LocalTime.of(hours, minutes))));
            }

        }
        usersStatusEntity.setAttempts(1);
        usersStatusEntity.setUserId(_user.getId());
        usersStatusEntity = userStatusRepository.save(usersStatusEntity);

        updateUserStatus(_user, usersStatusEntity.getId(), requestProperties.getVendorPlanId());

        userStatusRepository.flush();

        log.info("ZONG CONSUMER SERVICE  | SUBSCIPTIONEVENTHANDLER CLASS | " + requestProperties.getMsisdn() + " " +
                "| USER STATUS CREATED");
        return usersStatusEntity;
    }

    public void processUserForEDA(RequestProperties requestProperties, UsersEntity user) {
        processUserRequest(requestProperties, user);
    }

    private void processUserRequest(RequestProperties requestProperties, UsersEntity _user) {
        FiegnResponse fiegnResponse;

        // EDA Request sets => isFromEDA: true
        if (requestProperties.isFromEDA()) {
            fiegnResponse = requestProperties.getFiegnResponse();
        } else {
            fiegnResponse = billingService.charge(requestProperties);
            log.info("FeignResponse**** | "+ fiegnResponse);

            log.info("********* Sending Request For Charging ******" + " | msisdn:" + requestProperties.getMsisdn());
        }

        UsersStatusEntity lastUserStatus = null;
        VendorPlansEntity vendorPlansEntity = dataService.getVendorPlans(requestProperties.getVendorPlanId());

        if (fiegnResponse == null) {
            log.info("Return from fiegnResponse");

            return;
        }

        if (_user.getUserStatusId() != null) {
            lastUserStatus = userStatusRepository.UnsubStatus(_user.getId());
        }

        if (fiegnResponse.getCode() == Integer.parseInt(ResponseTypeConstants.ALREADY_SUBSCRIBED)) {
            // ALREADY SUBSCRIBED CASE
            String responseTypeConstant = null;

//            if (vendorPlansEntity.getOperatorId() == 1) {
//                // Create already subscribed response | game now.
//                responseTypeConstant = ResponseTypeConstants.ALREADY_SUBSCRIBED;
//            } else
                if (vendorPlansEntity.getOperatorId() == 4) {
                // Create valid response | zong games.
                responseTypeConstant = ResponseTypeConstants.VALID;
            }

            // Send response.
            try {
                createResponse(fiegnResponse.getMsg(), responseTypeConstant, requestProperties.getCorrelationId());
            } catch (Exception e) {
                log.info("Subscription SERVICE | Exception | Creating response | " + e.getCause());
            }

            continueUserSubscriptionProcess(requestProperties, _user, vendorPlansEntity);
        } else if (fiegnResponse.getCode() == Integer.parseInt(ResponseTypeConstants.SUSBCRIBED_SUCCESSFULL)) {
            // SUBSCRIBED SUCCESSFUL
            String message = null;
            boolean isMtAllowed = false;

            try {
                createResponse(fiegnResponse.getMsg(), ResponseTypeConstants.SUSBCRIBED_SUCCESSFULL, requestProperties.getCorrelationId());
            } catch (Exception e) {
                log.info("Subscription SERVICE | Exception | Creating response | " + e.getCause());
            }

            // Handle Jazz Game now and Zong gamez in different statements.
//            if (vendorPlansEntity.getOperatorId() == 1) {
//                // Jazz | Game now
//                message = dataManagerService.getMtMessage("jazz_sub").getMsgText();
//
//                // This check was added for the following use case :: If an already subscribed user in the past 30/45 days logs
//                // in, he should not receive subscription MT. It is make sure by status id 8, which means that the user is
//                // already in subscription renewal.
//                if (lastUserStatus != null && lastUserStatus.getStatusId() == 8) {
//                    isMtAllowed = false;
//                } else {
//                    isMtAllowed = true;
//                }
//            } else
                if (vendorPlansEntity.getOperatorId() == 4) {
                // Zong | Games
                message = dataManagerService.getMtMessage("zong_sub").getMsgText();

                if (lastUserStatus != null && lastUserStatus.getStatusId() == 8) {
                    isMtAllowed = false;
                } else {
                    isMtAllowed = true;
                }
            }

            if (isMtAllowed) {
                sendMT(requestProperties, message);
            }
            // Continue subscription process after creating and sending the response to the client.
            continueUserSubscriptionProcess(requestProperties, _user, vendorPlansEntity);
        } else if (fiegnResponse.getCode() == Integer.parseInt(ResponseTypeConstants.INSUFFICIENT_BALANCE)) {
            // INSUFFICIENT BALANCE
            String message = null;
            boolean isMtAllowed;

//            if (vendorPlansEntity.getOperatorId() == 1) {
//                // In case of game now, create free trial response.
//                try {
//                    createResponse(fiegnResponse.getMsg(), ResponseTypeConstants.FREE_TRIAL_SUBSCRIPTION,
//                            requestProperties.getCorrelationId());
//                } catch (Exception e) {
//                    log.info("Subscription SERVICE | Exception | Creating response | " + e.getCause());
//                }
//
//                // Get free trial MT message from DB
//                message = dataManagerService.getMtMessage("jazz_sub_freetrial").getMsgText();
//
//                // This check was added for the following use case :: If an already subscribed user in the past 30/45 days logs
//                // in, he should not receive subscription MT. It is make sure by status id 8, which means that the user is
//                // already in subscription renewal.
//                if (lastUserStatus != null && lastUserStatus.getStatusId() == 8) {
//                    isMtAllowed = false;
//                } else {
//                    isMtAllowed = true;
//                }
//
//                if (isMtAllowed) {
//                    sendMT(requestProperties, message);
//                }
//
//                try {
//                    createUserStatusEntity(requestProperties, _user, UserStatusTypeConstants.SUBSCRIBED, false);
//                    saveLogInRecord(requestProperties, vendorPlansEntity.getId());
//                } catch (Exception e) {
//                    log.info("Subscription SERVICE |  Exception | User status & login updates | " + e.getCause());
//                }
//            }
//            else
                if (vendorPlansEntity.getOperatorId() == 4) {
                // In case of Zong games, create free trial as well.
                try {
                    createResponse(fiegnResponse.getMsg(), ResponseTypeConstants.INSUFFICIENT_BALANCE,
                            requestProperties.getCorrelationId());
                } catch (Exception e) {
                    log.info("Subscription SERVICE | Exception | Creating response | " + e.getCause());
                }

                // Get zong MT message & send MT.
                message = dataManagerService.getMtMessage("zong_insufficient_balance").getMsgText();

                sendMT(requestProperties, message);
            }
        } else if (fiegnResponse.getCode() == Integer.parseInt(ResponseTypeConstants.UNAUTHORIZED_REQUEST)) {
            createResponse(fiegnResponse.getMsg(), ResponseTypeConstants.UNAUTHORIZED_REQUEST, requestProperties.getCorrelationId());
        } else if (fiegnResponse.getCode() == Integer.parseInt(ResponseTypeConstants.SUBSCRIBER_NOT_FOUND)) {
            createResponse(fiegnResponse.getMsg(), ResponseTypeConstants.SUBSCRIBER_NOT_FOUND, requestProperties.getCorrelationId());
        } else {
            createResponse(fiegnResponse.getMsg(), ResponseTypeConstants.OTHER_ERROR, requestProperties.getCorrelationId());
        }
        // If Required :: Add another case here for response type 115/subscriber not found.
    }

    private void createResponse(String desc, String resultStatus, String correlationId) {
        log.info("ZONG CONSUMER SERVICE  | SUBSCIPTIONEVENTHANDLER CLASS | " + correlationId + " | TRYING TO CREATE RESPONSE");
        VendorRequestsStateEntity entity = null;
        boolean isNull = true;
        int i = 0;
        if (entity == null) {
            while (isNull) {
                entity = redisRepository.findVendorRequestStatus(correlationId);
                if(entity == null)
                {
                    entity = requestRepository.findByCorrelationid(correlationId);
                }
                System.out.println("ENTITY IS NULL TAKING TIME");

                if (entity != null) {
                    isNull = false;
                }
                i++;
                if (i > 10) {
                    isNull = false;
                }
            }
        }
        entity.setCdatetime(Timestamp.valueOf(LocalDateTime.now()));
        entity.setFetched(false);
        entity.setResultStatus(resultStatus);
        entity.setDescription(desc);
        VendorRequestsStateEntity vre = requestRepository.save(entity);
        ObjectMapper objectMapper = new ObjectMapper();
        redisRepository.saveVendorRequest(entity);
        log.info("ZONG CONSUMER SERVICE  | SUBSCIPTIONEVENTHANDLER CLASS | " + vre.getResultStatus() + " | REQUEST STATUS SAVED IN REDIS");
        log.info("ZONG CONSUMER SERVICE  | SUBSCIPTIONEVENTHANDLER CLASS | " + vre.getResultStatus() + " | REQUEST STATE UPDATED");
    }

    private void continueUserSubscriptionProcess(RequestProperties requestProperties, UsersEntity _user, VendorPlansEntity vendorPlansEntity) {
        try {
            createUserStatusEntity(requestProperties, _user, UserStatusTypeConstants.SUBSCRIBED, false);

            saveLogInRecord(requestProperties, vendorPlansEntity.getId());

            List<VendorReportEntity> vendorReportEntity = vendorReportRepository.findByMsisdnAndVenodorPlanId(requestProperties.getMsisdn(), (int) requestProperties.getVendorPlanId());

            if (vendorReportEntity.isEmpty()) {
                log.info("CALLING VENDOR POSTBACK");

                vendorPostBackService.sendVendorPostBack(vendorPlansEntity.getId(), requestProperties.getTrackerId());
                createVendorReport(requestProperties, 1, _user.getOperatorId().intValue());
            } else {
                createVendorReport(requestProperties, 0, _user.getOperatorId().intValue());
            }
        } catch (Exception e) {
            log.info("Subscription SERVICE | Exception | User status | login | vendor PB | vendor " +
                    "report" +
                    " | " + e.getCause());
        }
    }

    private void sendMT(RequestProperties requestProperties, String message) {
        MtProperties mtProperties = new MtProperties();
        mtProperties.setData(message);
        mtProperties.setMsisdn(Long.toString(requestProperties.getMsisdn()));
        mtProperties.setShortCode("3444");
        mtProperties.setPassword("g@m3now");
        mtProperties.setUsername("gamenow@noetic");
        mtProperties.setServiceId("1061");


        try {
            String bodyurl = "{\n    \"username\" :\"" + "gamenow@noetic" + "\",\n    \"password\":\"" + "g@m3now" + "\",\n    \"shortCode\":\"" + "3444" + "\",\n    \"serviceId\":" + "1061" + ",\n    \"data\":\"" + message + "\",\n    \"msisdn\":\"" + requestProperties.getMsisdn() + "\"\n}";

//            mtClient.sendMt(mtProperties);
            Unirest.setTimeouts(120, 120);
            com.mashape.unirest.http.HttpResponse<String> response1 = Unirest.post("http://localhost:9096/mt")
                    .header("Content-Type", "application/json")
//                    .body("{\n    \"username\" :\"" + this.username + "\",\n    \"password\":\"" + this.password + "\",\n    \"shortCode\":\"" + requestProperties.getShortcode() + "\",\n    \"serviceId\":" + this.serviceid + ",\n    \"data\":\"" + replymt + "\",\n    \"msisdn\":\"" + "92"+ requestProperties.getMsisdn() + "\"\n}")
                    .body(bodyurl)
                    .asString();
            log.info("Response From MT in SUBSCRIPTIONEVENT" + response1.getBody());
            mtService.saveMessageRecord(requestProperties.getMsisdn(), message);
        } catch (Exception e) {
            log.info("SubscriptionEventHandler | MT Exception | " + e.getCause());
        }
    }

    private void updateUserStatus(UsersEntity user, long userStatusId, long vendorPLanId) {
        user.setUserStatusId((int) userStatusId);
        user.setModifyDate(Timestamp.valueOf(LocalDateTime.now()));
        if (user.getVendorPlanId() != vendorPLanId) {
            user.setVendorPlanId(vendorPLanId);
        }
        usersRepository.save(user);
    }

    private void createVendorReport(RequestProperties requestProperties, int postBackSent, Integer operatorId) {
        VendorReportEntity vendorReportEntity = new VendorReportEntity();
        vendorReportEntity.setCdate(Timestamp.valueOf(LocalDateTime.now()));
        vendorReportEntity.setMsisdn(requestProperties.getMsisdn());
        vendorReportEntity.setVenodorPlanId((int) requestProperties.getVendorPlanId());
        vendorReportEntity.setTrackerId(requestProperties.getTrackerId());
        vendorReportEntity.setPostbackSent(postBackSent);
        vendorReportEntity.setOperatorId(operatorId);
        vendorReportRepository.save(vendorReportEntity);
    }

    private void saveLogInRecord(RequestProperties requestProperties, long vendorPlanId) {
        LoginRecordsEntity loginRecordsEntity = new LoginRecordsEntity();
        loginRecordsEntity.setCtime(Timestamp.valueOf(LocalDateTime.now()));

        loginRecordsEntity.setSessionId(requestProperties.getSessionId());
        loginRecordsEntity.setAcitve(false);
        loginRecordsEntity.setRemoteServerIp(requestProperties.getRemoteServerIp());
        loginRecordsEntity.setLocalServerIp(requestProperties.getLocalServerIp());
        loginRecordsEntity.setMsisdn(requestProperties.getMsisdn());
        loginRecordsEntity.setVendorPlanId(vendorPlanId);
        loginRecordsEntity.setCode(otpnumber);
        logInRecordRepository.save(loginRecordsEntity);
//        loginRepository.updateLoginTable(requestProperties.getMsisdn());
      /*  LoginEntity lg= loginRepository.findTopByMsisdn(requestProperties.getMsisdn());
        if(lg!=null){
            lg.setCode(0);
        }*/

    }



//    public String verifyOTP(long msisdn,long otp) throws URISyntaxException {
//        RestTemplate restTemplate=new RestTemplate();
//        String param1="jnhuuu58sdf",param2="android",param3="",identifier="";
//        String body="{" +
//                "\"Identifier\":"+"\""+msisdn+"\","+
//                "\"OTP\":\""+otp+"\","+
//                "\"param1\":" +"\"GAMENOW CASUALGAMEZ\","+
//                "\"param2\":" +"\"android \","+
//                "\"param3\":" +"\"\""+
//                "}";
//        System.out.println(body);
//        HttpHeaders headers = new HttpHeaders();
//        headers.set("Content-Type","application/json");
//        headers.set("Connection","keep-alive");
//        headers.set("Authorization","Bearer "+TokenManager.accessToken);
//        headers.set("Channel","GAMENOWCASUAL");
//        try{
//        HttpEntity<Map<String, Object>> entity = new HttpEntity(body, headers);
//   ResponseEntity<String> str= restTemplate.postForEntity(new URI("https://apim.jazz.com.pk/auth/verifyOTP"),entity,String.class);
////      ResponseEntity<String> str= restTemplate.postForEntity(new URI("https://apimtest.jazz.com.pk:8282/auth/verifyOTP"),entity,String.class);
//
//
//
//
//
//        JSONObject json = new JSONObject(str.getBody());
//        log.info(str.getStatusCode()+" "+str.getBody()+ "msidn: "+msisdn);
//        log.info(json.getString("msg")+" -----------");
//        return json.getString("msg");
//    }catch(
//    HttpClientErrorException e){
//        if(e.getStatusCode().value()==401){
//            System.out.println("calling");
//            TokenManager.getToken();
//            this.verifyOTP(msisdn,otp);
//        }
//    }
//
//       return "-1";
//    }


    private OtpRecordsEntity getTopOtpRecordFromMsidn(Long msisdn)
    {
        List<OtpRecordsEntity> otpRecordsEntityList = redisRepository.findAllOTPOfMsisdn(String.valueOf(msisdn));

        List<OtpRecordsEntity> recordsEntities = new ArrayList<>();
        for (OtpRecordsEntity otpRecordsEntity : otpRecordsEntityList) {
            if(otpRecordsEntity.getMsisdn().equals(msisdn))
            {
                recordsEntities.add(otpRecordsEntity);
            }
        }
        Collections.sort(recordsEntities, new Comparator<OtpRecordsEntity>() {
            public int compare(OtpRecordsEntity o1, OtpRecordsEntity o2) {
                if (o1.getCdate() == null || o2.getCdate() == null)
                    return 0;
                return o1.getCdate().compareTo(o2.getCdate());
            }
        });
        return otpRecordsEntityList.get(0);
    }



}

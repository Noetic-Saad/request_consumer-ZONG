package com.noeticworld.sgw.requestConsumer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.noeticworld.sgw.requestConsumer.entities.GamesBillingRecordEntity;
import com.noeticworld.sgw.requestConsumer.entities.UsersEntity;
import com.noeticworld.sgw.requestConsumer.entities.UsersStatusEntity;
import com.noeticworld.sgw.requestConsumer.entities.VendorPlansEntity;
import com.noeticworld.sgw.requestConsumer.repository.GamesBillingRecordRepository;
import com.noeticworld.sgw.requestConsumer.repository.MsisdnCorrelationsRepository;
import com.noeticworld.sgw.requestConsumer.repository.UserStatusRepository;
import com.noeticworld.sgw.requestConsumer.repository.UsersRepository;
import com.noeticworld.sgw.util.BillingClient;
import com.noeticworld.sgw.util.ChargeRequestProperties;
import com.noeticworld.sgw.util.FiegnResponse;
import com.noeticworld.sgw.util.RequestProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class BillingService {

    Logger log = LoggerFactory.getLogger(BillingService.class.getName());
    @Autowired
    MtService mtService;
    @Autowired
    MsisdnCorrelationsRepository msisdnCorrelationsRepository;
    @Autowired
    private ConfigurationDataManagerService dataService;
    @Autowired
    private BillingClient billingClient;
    @Autowired
    private GamesBillingRecordRepository gamesBillingRecordsRepository;
    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private UserStatusRepository userStatusRepository;

    public FiegnResponse charge(RequestProperties requestProperties) {
        FiegnResponse fiegnResponse = new FiegnResponse();
        UsersStatusEntity latestUserStatus = null;

        UsersEntity user = usersRepository.FindByTopMSISDN(requestProperties.getMsisdn());

        // If it is a new user and is just created, then it's user status id will be null.
        // So, in this case, we do not need to send an extra request on user status table,just by pass it.
        if (user.getUserStatusId() != null) {
            latestUserStatus = userStatusRepository.UnsubStatus(user.getId());
        }

        // Latest status != 2 is appended to make sure that if the user is charged for that day and then
        // unsub and login to the system (Charging request) again, we want to send charging request to SGW
        // Billing and from there the setCode() will be set to 101(SUCCESSFULLY_CHARGED).
//        if (user.getOperatorId() == 1 && isAlreadyChargedToday(requestProperties.getMsisdn()) && latestUserStatus.getStatusId() != 2) {
//            log.info("BILLING SERVICE | CHARGING CLASS | ALREADY CHARGED TODAY | " + requestProperties.getMsisdn());
//            fiegnResponse.setCode(110);
//            fiegnResponse.setCorrelationId(requestProperties.getCorrelationId());
//            fiegnResponse.setMsg("ALREADY SUBSCRIBED");
//            return fiegnResponse;
//        } else
            if (user.getOperatorId() == 4 && isAlreadyChargedFor7Days(requestProperties.getMsisdn()) && latestUserStatus.getStatusId() != 2) {
            log.info("BILLING SERVICE | CHARGING CLASS | ALREADY CHARGED FOR 7 DAYS | " + requestProperties.getMsisdn());
            fiegnResponse.setCode(110);
            fiegnResponse.setCorrelationId(requestProperties.getCorrelationId());
            fiegnResponse.setMsg("ALREADY SUBSCRIBED");
            return fiegnResponse;
        } else {
            // ----- DBSS Call for Jazz GameNow -----
//            if (user.getOperatorId() == 1) {
//                /*
//                 * 2 - unsub
//                 * 4 - telco unsub
//                 * 5 - renewal unsub
//                 * */
//                if (user.getUserStatusId() == null || latestUserStatus.getStatusId() == 2 || latestUserStatus.getStatusId() == 4
//                        || latestUserStatus.getStatusId() == 5) {
//
//                    log.info("BILLING SERVICE | DBSS REQUEST | " + requestProperties.getMsisdn() + " | " +
//                            (user.getUserStatusId() == null ? "First time user" : "Status Id: " + latestUserStatus.getStatusId()));
//
//                    createMsisdnCorrelation(requestProperties);
//
//                    HttpResponse<String> response =
//                            Unirest.get("http://localhost:10001/dbss/product-activation/" + requestProperties.getMsisdn()
//                                    + "/" + requestProperties.getCorrelationId()).asString();
//
//                    log.info("BILLING SERVICE | DBSS RESPONSE | " + requestProperties.getMsisdn() + " | " + response.getStatus() +
//                            " | " + response.getBody());
//                    return null;
//                }
//            }
            log.info("After DBSS");

            VendorPlansEntity vendorPlansEntity = dataService.getVendorPlans(requestProperties.getVendorPlanId());

            ChargeRequestProperties chargeRequestProperties = new ChargeRequestProperties();
            chargeRequestProperties.setOperatorId(vendorPlansEntity.getOperatorId());
            chargeRequestProperties.setCorrelationId(requestProperties.getCorrelationId());
            chargeRequestProperties.setMsisdn(requestProperties.getMsisdn());
            chargeRequestProperties.setOriginDateTime(requestProperties.getOriginDateTime());
            chargeRequestProperties.setVendorPlanId((int) requestProperties.getVendorPlanId());
            chargeRequestProperties.setShortcode("3444");
            chargeRequestProperties.setSubCycleId(vendorPlansEntity.getSubCycle());
            if (dataService.isTestMsisdn(requestProperties.getMsisdn())) {
                chargeRequestProperties.setChargingAmount(1.0f);
            } else {
                chargeRequestProperties.setChargingAmount(vendorPlansEntity.getPricePoint());
                chargeRequestProperties.setTaxAmount(vendorPlansEntity.getTaxAmount());
            }
            chargeRequestProperties.setIsRenewal(0);

                /******************************************/
                String bodyurl = "";

                try {

                    if (dataService.isTestMsisdn(requestProperties.getMsisdn())) {
                        bodyurl = "{\n    \"operatorId\" :\"" + chargeRequestProperties.getOperatorId() + "\"," +
                                "\n    \"correlationId\":\"" + chargeRequestProperties.getCorrelationId() + "\"," +
                                "\n    \"msisdn\":\"" + chargeRequestProperties.getMsisdn() + "\"," +
                                "\n    \"subCycleId\":\"" + chargeRequestProperties.getSubCycleId() + "\"," +
                                "\n    \"chargingAmount\":\"" + chargeRequestProperties.getChargingAmount() + "\"," +
                                "\n    \"originDateTime\":" + chargeRequestProperties.getOriginDateTime() + "," +
                                "\n    \"vendorPlanId\":\"" + chargeRequestProperties.getVendorPlanId() + "\"," +
                                "\n    \"isRenewal\":\"" + chargeRequestProperties.getIsRenewal() + "\"," +
                                "\n    \"shortcode\":\"" + chargeRequestProperties.getShortcode() + "\"\n}";
                    } else {
                        bodyurl = "{\n    \"operatorId\" :\"" + chargeRequestProperties.getOperatorId() + "\"," +
                                "\n    \"correlationId\":\"" + chargeRequestProperties.getCorrelationId() + "\"," +
                                "\n    \"msisdn\":\"" + chargeRequestProperties.getMsisdn() + "\"," +
                                "\n    \"subCycleId\":\"" + chargeRequestProperties.getSubCycleId() + "\"," +
                                "\n    \"chargingAmount\":\"" + chargeRequestProperties.getChargingAmount() + "\"," +
                                "\n    \"taxAmount\":\"" + chargeRequestProperties.getTaxAmount() + "\"," +
                                "\n    \"originDateTime\":" + chargeRequestProperties.getOriginDateTime() + "," +
                                "\n    \"isRenewal\":\"" + chargeRequestProperties.getIsRenewal() + "\"," +
                                "\n    \"vendorPlanId\":\"" + chargeRequestProperties.getVendorPlanId() + "\"," +
                                "\n    \"shortcode\":\"" + chargeRequestProperties.getShortcode() + "\"\n}";
                    }

                    com.mashape.unirest.http.Unirest.setTimeouts(120, 120);
                    com.mashape.unirest.http.HttpResponse<String> response1 = null;
                    ObjectMapper objectMapper = new ObjectMapper();
                    response1 = com.mashape.unirest.http.Unirest.post("https://dcbbilling.mmsdp.com/charge")
                            .header("Content-Type", "application/json")
                            .body(objectMapper.writeValueAsString( new ChargeRequestProperties(chargeRequestProperties)))
                            .asString();
                    log.info("BILLING SERVICE || SEND BILLING REQUEST FROM JAZZ" + response1.getBody());
                } catch (UnirestException e) {
                    throw new RuntimeException(e);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                /******************************************/

            return fiegnResponse;
        }

    }

//    private void createMsisdnCorrelation(RequestProperties requestProperties) {
//        MsisdnCorrelations msisdnCorrelations = new MsisdnCorrelations();
//        msisdnCorrelations.setMsisdn(requestProperties.getMsisdn());
//        msisdnCorrelations.setCorrelationId(requestProperties.getCorrelationId());
//        msisdnCorrelations.setCdate(Timestamp.from(Instant.now()));
//        msisdnCorrelationsRepository.save(msisdnCorrelations);
//    }
//
//    private boolean isMsisdnWhiteListedForDBSS(RequestProperties requestProperties) {
//        List<Long> whiteListedEDAsMSISDN = Arrays.asList(923015195540l, 923080144278l, 923015430026l);
//        return whiteListedEDAsMSISDN.stream().anyMatch(msisdn -> msisdn == requestProperties.getMsisdn());
//    }

    private boolean isAlreadyChargedFor7Days(long msisdn) {
        LocalDateTime toDate = LocalDateTime.now();
        LocalDateTime fromDate = toDate.minusDays(7);
        List<GamesBillingRecordEntity> entity = gamesBillingRecordsRepository.isAlreadyChargedFor7Days(msisdn, Timestamp.valueOf(fromDate), Timestamp.valueOf(toDate));
        return !entity.isEmpty();
    }

//    private boolean isAlreadyChargedToday(long msisdn) {
//        log.info("BILLING SERVICE | CHARGING CLASS | CHECKING IF ALREADY CHARGED TODAY | " + msisdn);
//        Timestamp fromDate = Timestamp.valueOf(LocalDate.now().atStartOfDay());
//        Timestamp currenttime = Timestamp.valueOf(LocalDateTime.now().plusDays(1));
//        Timestamp toDate = Timestamp.valueOf(LocalDate.now().atTime(23, 59));
//
//        List<GamesBillingRecordEntity> gamesBillingRecordEntity = gamesBillingRecordsRepository.isAlreadyChargedForToday(msisdn, fromDate, toDate);
//        log.info("Check if = " + !gamesBillingRecordEntity.isEmpty());
//
//        return !gamesBillingRecordEntity.isEmpty();
//    }

//    private boolean isFreeTrial(long msisdn) {
//        log.info("BILLING SERVICE | CHARGING CLASS | CHECKING IF FreeTrial CHARGED TODAY | " + msisdn);
//        Timestamp fromDate = Timestamp.valueOf(LocalDate.now().atStartOfDay());
//        Timestamp toDate = Timestamp.valueOf(LocalDate.now().atTime(23, 59));
//        UsersEntity _user = usersRepository.findByMsisdn(msisdn);
//        List<UsersStatusEntity> entitylist = null;
//        if (_user != null) {
//            log.info("User Is Not Null Checking Free Trial");
//            entitylist = userStatusRepository.IsFreeTrialUser(fromDate, _user.getId());
//            if (!entitylist.isEmpty()) {
//                log.info("Free Trial Users");
//                return true;
//            } else {
//                log.info("Not Free Trial Users");
//                return false;
//            }
//
//        }
//        return false;
//    }
}

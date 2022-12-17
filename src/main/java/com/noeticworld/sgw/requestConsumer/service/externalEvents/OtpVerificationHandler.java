package com.noeticworld.sgw.requestConsumer.service.externalEvents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import com.noeticworld.sgw.requestConsumer.entities.LoginEntity;
import com.noeticworld.sgw.requestConsumer.entities.OtpRecordsEntity;
import com.noeticworld.sgw.requestConsumer.entities.VendorPlansEntity;
import com.noeticworld.sgw.requestConsumer.repository.LoginRepository;
import com.noeticworld.sgw.requestConsumer.repository.OtpRecordRepository;
import com.noeticworld.sgw.requestConsumer.repository.RedisRepository;
import com.noeticworld.sgw.requestConsumer.service.ConfigurationDataManagerService;
import com.noeticworld.sgw.util.MtClient;
import com.noeticworld.sgw.util.MtProperties;
import com.noeticworld.sgw.util.RequestProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Service
public class OtpVerificationHandler implements RequestEventHandler {

    Logger logger = LoggerFactory.getLogger(OtpVerificationHandler.class.getName());

    @Autowired
    ConfigurationDataManagerService dataManagerService;
    @Autowired
    OtpRecordRepository otpRecordRepository;
    @Autowired
    MtClient mtClient;
    @Autowired
    LoginRepository loginRepository;

    @Autowired
    RedisRepository redisRepository;

  Integer otpverify=0;

    @Override
    public void handle(RequestProperties requestProperties) throws URISyntaxException {
        // Do not send OTP to this MSISDN
        if (isNotSentOTPToThisMsisdn(requestProperties)) return;
        Random random = new Random();
        Integer otpNumber = 1000 + random.nextInt(900);

        if (requestProperties.getMsisdn() == 923222200051l) {
            otpNumber = 1234;
        }
        MtProperties mtProperties = new MtProperties();
        VendorPlansEntity vendorPlansEntity = dataManagerService.getVendorPlans(requestProperties.getVendorPlanId());
        System.out.println("Line-66 OTPVERIFICATION || " + vendorPlansEntity.toString());
//        System.out.println("vendorPlansEntity.getPlanName()" + vendorPlansEntity.getPlanName() + requestProperties.getVendorPlanId() + " | OTP Number" + otpNumber);


//        if(vendorPlansEntity.getOperatorId()==1){
//
//           String otp= sendOTP(requestProperties.getMsisdn());
//           if(otp.equals(null) || otp.equals("0")){
//               System.out.println("OTP is = "+ otp);
//               return;
//           }
//            LoginEntity loginEntity = new LoginEntity();
//            loginEntity.setMsisdn(requestProperties.getMsisdn());
//            loginEntity.setUpdateddate(Timestamp.valueOf(LocalDateTime.now()));
//            loginEntity.setTrackingId(requestProperties.getTrackerId());
//            loginEntity.setCode(Integer.valueOf(otp));
//
//            //changes
//            logger.info("OTP="+ Integer.valueOf(otp));
//            mtProperties.setData("");
//            mtProperties.setMsisdn(Long.toString(requestProperties.getMsisdn()));
//            mtProperties.setShortCode("By Jazz");
//            mtProperties.setPassword("");
//            mtProperties.setUsername("");
//            mtProperties.setServiceId("By Jazz");
//            if(Integer.valueOf(otp) != -1){
//                saveOtpRecords(mtProperties, Integer.valueOf(otp), vendorPlansEntity.getId());
//                System.out.println("Saving Jazz Msisdn In Login Table : " + " Vendor Plan id : " + requestProperties.getVendorPlanId() );
//                System.out.println("vendorPlansEntity.getPlanName()" + vendorPlansEntity.getPlanName() + requestProperties.getVendorPlanId() + " | OTP Number " + otp);
//            } else
//            {
//                saveOtpRecords(mtProperties, this.otpverify, vendorPlansEntity.getId());
//                System.out.println("Saving Jazz Msisdn In Login Table : " + " Vendor Plan id : " + requestProperties.getVendorPlanId() );
//                System.out.println("vendorPlansEntity.getPlanName()" + vendorPlansEntity.getPlanName() + requestProperties.getVendorPlanId() + " | OTP Number " + otp);
//            }
//
//                  loginRepository.save(loginEntity);
//
//
//    }
        if(vendorPlansEntity.getOperatorId()==4) {
            // MtMessageSettingsEntity mtMessageSettingsEntity = dataManagerService.getMtMessageSetting(vendorPlansEntity.getId());
 //changes
            System.out.println("vendorPlansEntity.getPlanName()" + vendorPlansEntity.getPlanName() + requestProperties.getVendorPlanId() + " | OTP Number" + otpNumber);

            String message = dataManagerService.getMtMessage(vendorPlansEntity.getPlanName() + "_otp").getMsgText();
            String finalMessage = message.replaceAll("&otp", otpNumber.toString());
            mtProperties.setData(finalMessage);
            mtProperties.setMsisdn(Long.toString(requestProperties.getMsisdn()));
            mtProperties.setShortCode("3444");
            mtProperties.setPassword("g@m3now");
            mtProperties.setUsername("gamenow@noetic");
            mtProperties.setServiceId("1061");
      //      System.out.println(finalMessage);
      //      System.out.println(requestProperties.getMsisdn());

            try {
                String bodyurl = "{\n    \"username\" :\"" + "gamenow@noetic" + "\",\n    \"password\":\"" + "g@m3now" + "\",\n    \"shortCode\":\"" + "3444" + "\",\n    \"serviceId\":" + "1061" + ",\n    \"data\":\"" + finalMessage + "\",\n    \"msisdn\":\"" + requestProperties.getMsisdn() + "\"\n}";
        //        System.out.println("Body URL " + bodyurl);
                //                mtClient.sendMt(mtProperties);
                Unirest.setTimeouts(120, 120);
                com.mashape.unirest.http.HttpResponse<String> response1 = Unirest.post("http://192.168.127.69:9096/mt")
                        .header("Content-Type", "application/json")
                        .body(bodyurl)

//                    .body("{\n    \"username\" :\"" + "gamenow@noetic" + "\",\n    \"password\":\"" + "g@m3now" + "\",\n    \"shortCode\":\"" + "3444" + "\",\n    \"serviceId\":" + "1061" + ",\n    \"data\":\"" + finalMessage + "\",\n    \"msisdn\":\"" + requestProperties.getMsisdn() + "\"\n}")
//                        .body(mtProperties)
                        .asString();
                logger.info("Response From MT in OTPVERIFICATIONHANDLER" + response1.getBody());
            } catch (Exception e) {
                logger.info("ZONG CONSUMER SERVICE  | OTPVERIFICATIONHANDLER CLASS | EXCEPTION CAUGHT | " + e.getCause());
            }
            saveOtpRecords(mtProperties, otpNumber, vendorPlansEntity.getId());

        System.out.println("Saving Zong  Msisdn In Login Table : " + " Vendor Plan id : " + requestProperties.getVendorPlanId() );
        LoginEntity loginEntity = new LoginEntity();
        loginEntity.setMsisdn(requestProperties.getMsisdn());
        loginEntity.setUpdateddate(Timestamp.valueOf(LocalDateTime.now()));
        loginEntity.setTrackingId(requestProperties.getTrackerId());
        loginEntity.setCode(otpNumber);
        loginRepository.save(loginEntity);
        }
    }

    private boolean isNotSentOTPToThisMsisdn(RequestProperties requestProperties) {
        List<Long> blockedOTPs = Arrays.asList(923150880379l);
        return blockedOTPs.stream().anyMatch(msisdn -> requestProperties.getMsisdn() == msisdn);
    }

    public void saveOtpRecords(MtProperties mtProperties, Integer otpNumber, long vendorPlanId) {
// logger.info("in Save OTP Records"+otpNumber);
// logger.info("All "+ Timestamp.valueOf(LocalDateTime.now())+ " "+Long.parseLong(mtProperties.getMsisdn()) + " "+ otpNumber +" "+vendorPlanId  );
        OtpRecordsEntity otpRecordsEntity = new OtpRecordsEntity();
        otpRecordsEntity.setCdate(Timestamp.valueOf(LocalDateTime.now()));
        otpRecordsEntity.setMsisdn(Long.parseLong(mtProperties.getMsisdn()));
        otpRecordsEntity.setOtpNumber(otpNumber);
        otpRecordsEntity.setVendorPlanId(vendorPlanId);
        otpRecordRepository.save(otpRecordsEntity);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            redisRepository.saveOtpRecord(String.valueOf(otpRecordsEntity.getMsisdn()), objectMapper.writeValueAsString(otpRecordsEntity));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        logger.info("ZONG CONSUMER SERVICE  | OTPVERIFICATIONHANDLER CLASS | OTP REOCRDS SAVED IN REDIS");
        logger.info("ZONG CONSUMER SERVICE  | OTPVERIFICATIONHANDLER CLASS | OTP REOCRDS SAVED");
    }





    //send otp for jazz new
//    public String sendOTP(long msisdn) throws URISyntaxException {
//        RestTemplate restTemplate=new RestTemplate();
//        String body="{" +
//                "\"Identifier\":"+"\""+msisdn+"\","+
//                "\"param1\":" +"\"GAMENOW CASUALGAMEZ\","+
//                "\"param2\":" +"\"android\","+
//                "\"param3\":" +"\"\""+
//                "}";
//        logger.info(body);
//        HttpHeaders headers = new HttpHeaders();
//        headers.set("Content-Type","application/json");
//        headers.set("Connection","keep-alive");
//        headers.set("Authorization","Bearer "+TokenManager.accessToken);
//        headers.set("Channel","GAMENOWCASUAL");
//        HttpEntity<Map<String, Object>> entity = new HttpEntity(body, headers);
//        try {
////        ResponseEntity<String> str= restTemplate.postForEntity(new URI("https://apimtest.jazz.com.pk:8282/auth/sendOTP"),entity,String.class);
//            ResponseEntity<String> str = restTemplate.postForEntity(new URI("https://apim.jazz.com.pk/auth/sendOTP"), entity, String.class);
//            JSONObject json = new JSONObject(str.getBody());
//            //     logger.info(str.getStatusCode() + " " + str.getBody()+" msisdn: "+msisdn);
//
//            if (json.has("errorCode")) {
//                logger.info("This is postpaid = " + msisdn);
//                return null;
//            } else if (json.getString("resultCode").equals("ERR-0001")) {
////                System.out.println("JSon Is null");
//                logger.info("new OTP will generate after 1 minute | " + msisdn);
//                return null;
//            } else  if (json.get("resultCode").equals("00"))
//            {
//
//                this.otpverify = Integer.parseInt(json.getJSONObject("data").getString("msg"));
//              System.out.println("Verify OTP"+ otpverify);
//
//
//            return json.getJSONObject("data").getString("msg");
//        }
//            return null;
//        }catch(HttpClientErrorException e){
//            if(e.getStatusCode().value()==401){
//            System.out.println("calling");
//            TokenManager.getToken();
//            this.sendOTP(msisdn);
//        }
//
//            return String.valueOf(otpverify);
//        }
//
//    }


}

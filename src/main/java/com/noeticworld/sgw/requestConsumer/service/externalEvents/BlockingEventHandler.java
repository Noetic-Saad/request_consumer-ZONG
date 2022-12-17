package com.noeticworld.sgw.requestConsumer.service.externalEvents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noeticworld.sgw.requestConsumer.entities.UsersEntity;
import com.noeticworld.sgw.requestConsumer.entities.UsersStatusEntity;
import com.noeticworld.sgw.requestConsumer.entities.VendorPlansEntity;
import com.noeticworld.sgw.requestConsumer.entities.VendorRequestsStateEntity;
import com.noeticworld.sgw.requestConsumer.repository.RedisRepository;
import com.noeticworld.sgw.requestConsumer.repository.UserStatusRepository;
import com.noeticworld.sgw.requestConsumer.repository.UsersRepository;
import com.noeticworld.sgw.requestConsumer.repository.VendorRequestRepository;
import com.noeticworld.sgw.requestConsumer.service.ConfigurationDataManagerService;
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
public class BlockingEventHandler implements RequestEventHandler {
    Logger log = LoggerFactory.getLogger(BlockingEventHandler.class.getName());

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private ConfigurationDataManagerService dataService;

    @Autowired
    private UserStatusRepository userStatusRepository;

    @Autowired
    private VendorRequestRepository requestRepository;

    @Autowired
    private RedisRepository redisRepository;

    @Override
    public void handle(RequestProperties requestProperties) {
        UsersEntity _user = usersRepository.findByMsisdn(requestProperties.getMsisdn());

        if (_user == null) {
            _user = registerNewUser(requestProperties);
        }
        createUserStatusEntity(requestProperties, _user, UserStatusTypeConstants.BLOCKED);
        createResponse(dataService.getResultStatusDescription(ResponseTypeConstants.BLOCKED_SUCCESSFULL), ResponseTypeConstants.SUSBCRIBED_SUCCESSFULL, requestProperties.getCorrelationId());

    }

    private UsersEntity registerNewUser(RequestProperties requestProperties) {
        UsersEntity usersEntity = new UsersEntity();
        usersEntity.setMsisdn(requestProperties.getMsisdn());
        usersEntity.setVendorPlanId(requestProperties.getVendorPlanId());
        usersEntity.setCdate(new Date());
        return usersRepository.save(usersEntity);
    }

    private UsersStatusEntity createUserStatusEntity(RequestProperties requestProperties, UsersEntity _user, String userStatusType) {
        UsersStatusEntity usersStatusEntity = new UsersStatusEntity();
        VendorPlansEntity entity = dataService.getVendorPlans(requestProperties.getVendorPlanId());
        usersStatusEntity.setCdate(Timestamp.from(Instant.now()));
        usersStatusEntity.setStatusId(dataService.getUserStatusTypeId(userStatusType));
        usersStatusEntity.setVendorPlanId(requestProperties.getVendorPlanId());
        usersStatusEntity.setExpiryDatetime(usersStatusEntity.getCdate());
        usersStatusEntity.setSubCycleId(entity.getSubCycle());
        usersStatusEntity.setAttempts(1);
        usersStatusEntity.setUserId(_user.getId());
        usersStatusEntity = userStatusRepository.save(usersStatusEntity);
        updateUserStatus(_user, usersStatusEntity.getId());
        userStatusRepository.flush();
        return usersStatusEntity;
    }

    private void updateUserStatus(UsersEntity user, long userStatusId) {
        user.setUserStatusId((int) userStatusId);
        user.setModifyDate(Timestamp.valueOf(LocalDateTime.now()));
        usersRepository.save(user);
    }

    private void createResponse(String desc, String resultStatus, String correlationId) {
        VendorRequestsStateEntity entity = null;
        entity = redisRepository.findVendorRequestStatus(correlationId);
        if(entity == null)
        {
            entity = requestRepository.findByCorrelationid(correlationId);
        }
        entity.setCdatetime(new Timestamp(new Date().getTime()));
        entity.setFetched(false);
        entity.setResultStatus(resultStatus);
        entity.setDescription(desc);
        requestRepository.save(entity);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            redisRepository.saveVendorRequest(entity.getCorrelationid(), objectMapper.writeValueAsString(entity));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        log.info("ZONG CONSUMER SERVICE  | BLOCKINGEVENTHANDLER CLASS | " + entity.getResultStatus() + " | REQUEST STATUS SAVED IN REDIS");
    }
}

package com.noeticworld.sgw.requestConsumer.service;

import com.noeticworld.sgw.requestConsumer.entities.*;
import com.noeticworld.sgw.requestConsumer.repository.*;
import org.elasticsearch.common.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Singleton
public class ConfigurationDataManagerService {

    Logger log = LoggerFactory.getLogger(ConfigurationDataManagerService.class.getName());

    Map<Integer, Integer> operatorMap = new HashMap<>();
    private Map<Long, SubscriptionSettingEntity> subscriptionSettingEntityMap = new HashMap<>();
    private Map<String, ResponseTypeEntity> responseTypeEntityMap = new HashMap<>();
    private Map<String, EventTypesEntity> requestEventsEntityMap = new HashMap<>();
    private Map<Integer, UserStatusTypeEntity> userStatuseTypeMap = new HashMap<>();
    private Map<String, Integer> userStatusTypeIdsMap = new HashMap<>();
    private Map<Long, TestMsisdnsEntity> testMsisdnsMap = new HashMap<>();
    private Map<Long, VendorPlansEntity> vendorPlansEntityMap = new HashMap<>();
    private Map<String, MtMessagesEntity> mtMessagesEntityMap = new HashMap<>();
    private Map<Long, MtMessageSettingsEntity> mtMessageSettingsEntityMap = new HashMap<>();
    private Map<String, SubscriptionCyclesEntity> subCycleMap = new HashMap<>();
    private Map<Integer, SubscriptionCyclesEntity> subCycleDaysMap = new HashMap<>();
    private Map<Long, VendorPostbackConfigEntity> vendorPostbackConfigEntityMap = new HashMap<>();
    private Map<Long, String> vendorPostBackParamMap = new HashMap<>();

    @Autowired
    private ResponseTypeRepository responseTypeRepository;
    @Autowired
    private SubscriptionSettingRepository subscriptionSettingRepository;
    @Autowired
    private RequestEventsRepository requestEventsRepository;
    @Autowired
    private UserStatusesLookupRepository userStatusTypeRepository;
    @Autowired
    private TestMsisdnsRepository testMsisdnsRepository;
    @Autowired
    private VendorPlanRepository vendorPlanRepository;
    @Autowired
    private MtMessageRepository mtMessageRepository;
    @Autowired
    private OperatorRepository operatorRepository;
    @Autowired
    private MtMessageSettingsRepository mtMessageSettingsRepository;
    @Autowired
    private SubscriptionCycleRepository cycleRepository;
    @Autowired
    private VendorPostBackConfigRepository vendorPostBackConfigRepository;

    private int jazz = 0;
    private int warid = 0;
    private int ufone = 0;
    private int zong = 0;
    private int telenor = 0;

    public SubscriptionSettingEntity getSubscriptionEntity(long vendorPlanId) {
        return subscriptionSettingEntityMap.get(vendorPlanId);
    }

    public EventTypesEntity getRequestEventsEntity(String code) {
        return requestEventsEntityMap.get(code);
    }

    public UserStatusTypeEntity getUserStatusType(int statusTypeId) {
        return userStatuseTypeMap.get(statusTypeId);
    }

    public int getUserStatusTypeId(String statusTypeName) {
        return userStatusTypeIdsMap.get(statusTypeName);
    }

    public String getResultStatusDescription(String code) {
        return responseTypeEntityMap.get(code).getDescription();
    }

    public void updateSubscriptionEntity(SubscriptionSettingEntity subscriptionSettingEntity) {
        //save in db first
        subscriptionSettingEntityMap.put(subscriptionSettingEntity.getVendorPlanId(), subscriptionSettingEntity);
    }

    public void bootstapAndCacheConfigurationData() {
        loadResponseTypes();
        loadSubscriptionSettings();
        loadRequestEvents();
        loadUserStatuseTypes();
        loadTestMsisdns();
        loadVendorPlans();
        loadMtMessage();
        loadOperator();
        //loadMtMessageSettings();
        loadSubscriptionCycle();
        loadVendorPostBackConfig();
    }

    private void loadUserStatuseTypes() {
        List<UserStatusTypeEntity> list = userStatusTypeRepository.findAll();

        Map<Integer, UserStatusTypeEntity> objMap = new HashMap<>();
        list.forEach(statusType -> objMap.put(statusType.getId(), statusType));
        userStatuseTypeMap = objMap;

        Map<String, Integer> idsMap = new HashMap<>();
        list.forEach(statusType -> idsMap.put(statusType.getName(), statusType.getId()));
        userStatusTypeIdsMap = idsMap;
    }

    private void loadRequestEvents() {
        Map<String, EventTypesEntity> map = new HashMap<>();
        List<EventTypesEntity> list = requestEventsRepository.findAll();
        list.forEach(entity -> map.put(entity.getCode(), entity));
        requestEventsEntityMap = map;
    }

    private void loadResponseTypes() {
        Map<String, ResponseTypeEntity> map = new HashMap<>();
        List<ResponseTypeEntity> list = responseTypeRepository.findAll();
        list.forEach(entity -> map.put(entity.getCode(), entity));
        responseTypeEntityMap = map;
    }

    private void loadSubscriptionCycle() {
        List<SubscriptionCyclesEntity> list = cycleRepository.findAll();
        list.forEach(subscriptionCyclesEntity -> subCycleMap.put(subscriptionCyclesEntity.getLabel(), subscriptionCyclesEntity));
        list.forEach(subscriptionCyclesEntity -> subCycleDaysMap.put(subscriptionCyclesEntity.getId(), subscriptionCyclesEntity));
    }

    private void loadSubscriptionSettings() {
        Map<Long, SubscriptionSettingEntity> map = new HashMap<>();
        List<SubscriptionSettingEntity> list = subscriptionSettingRepository.findByRenewalSetting(0);
        list.forEach(entity -> map.put(entity.getVendorPlanId(), entity));
        subscriptionSettingEntityMap = map;
    }

    private void loadTestMsisdns() {
        List<TestMsisdnsEntity> list = testMsisdnsRepository.findAll();
        Map<Long, TestMsisdnsEntity> map = new HashMap<>();
        list.forEach(entity -> map.put(entity.getMsisdn(), entity));
        testMsisdnsMap = map;
    }

    private void loadVendorPlans() {
        List<VendorPlansEntity> list = vendorPlanRepository.findAll();
        list.forEach(vendorPlansEntity -> vendorPlansEntityMap.put(vendorPlansEntity.getId(), vendorPlansEntity));
    }

    private void loadMtMessage() {
        List<MtMessagesEntity> list = mtMessageRepository.findAll();
        list.forEach(mtMessagesEntity -> mtMessagesEntityMap.put(mtMessagesEntity.getLabel(), mtMessagesEntity));
    }

    private void loadMtMessageSettings() {
        List<MtMessageSettingsEntity> list = mtMessageSettingsRepository.findAll();
        list.forEach(mtMessageSettingsEntity -> mtMessageSettingsEntityMap.put(Long.valueOf(mtMessageSettingsEntity.getVendorPlanId()), mtMessageSettingsEntity));
    }

    private void loadVendorPostBackConfig() {
        List<VendorPostbackConfigEntity> list = vendorPostBackConfigRepository.findAll();
        list.forEach(vendorPostbackConfigEntity -> loadPostBackParams(vendorPostbackConfigEntity));
    }

    public void loadOperator() {
        List<OperatorEntity> list = operatorRepository.findAll();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getName().equalsIgnoreCase("jazz")) {
                jazz = list.get(i).getId();
            } else if (list.get(i).getName().equalsIgnoreCase("warid")) {
                warid = list.get(i).getId();
            } else if (list.get(i).getName().equalsIgnoreCase("ufone")) {
                ufone = list.get(i).getId();
            } else if (list.get(i).getName().equalsIgnoreCase("zong")) {
                zong = list.get(i).getId();
            } else {
                telenor = list.get(i).getId();
            }
        }
    }

    public SubscriptionCyclesEntity getSubCycleId(String label) {
        return subCycleMap.get(label);
    }

    public SubscriptionCyclesEntity getSubCycleDays(Integer id) {
        return subCycleDaysMap.get(id);
    }


    public boolean isTestMsisdn(long msisdn) {
        return testMsisdnsMap.get(msisdn) != null;
    }

    public VendorPlansEntity getVendorPlans(Long vendorPlanId) {
        return vendorPlansEntityMap.get(vendorPlanId);
    }

    public SubscriptionSettingEntity getSubscriptionSetting(long vendorPlanId) {
        return subscriptionSettingEntityMap.get(vendorPlanId);
    }

    public MtMessagesEntity getMtMessage(String label) {
        return mtMessagesEntityMap.get(label);
    }

    public MtMessageSettingsEntity getMtMessageSetting(Long vendorPlanId) {
        return mtMessageSettingsEntityMap.get(vendorPlanId);
    }

    public String getVendorPostBackConfig(Long vendorPlanId) {
        System.out.println("PostBack Entity Map---> " + vendorPostBackParamMap.size());
        System.out.println("PostBack Vendor Plan Id--->" + vendorPlanId);
        System.out.println(vendorPostBackParamMap.get(vendorPlanId));
        return vendorPostBackParamMap.get(vendorPlanId);
    }

    public void loadPostBackParams(VendorPostbackConfigEntity vendorPostbackConfigEntity) {
        String url = vendorPostbackConfigEntity.getUrl() + "?";
        if (!vendorPostbackConfigEntity.getParam1Name().equalsIgnoreCase("none")) {
            url = url + vendorPostbackConfigEntity.getParam1Name() + "=" + vendorPostbackConfigEntity.getParam1Value();
        }
        if (!vendorPostbackConfigEntity.getParam2Name().equalsIgnoreCase("none")) {
            url = url + "&" + vendorPostbackConfigEntity.getParam2Name() + "=" + vendorPostbackConfigEntity.getParam2Value();
        }
        if (!vendorPostbackConfigEntity.getParam3Name().equalsIgnoreCase("none")) {
            url = url + "&" + vendorPostbackConfigEntity.getParam3Name() + "=" + vendorPostbackConfigEntity.getParam3Value();
        }
        if (!vendorPostbackConfigEntity.getParam4Name().equalsIgnoreCase("none")) {
            url = url + "&" + vendorPostbackConfigEntity.getParam4Name() + "=" + vendorPostbackConfigEntity.getParam4Value();
        }
        if (!vendorPostbackConfigEntity.getParam5Name().equalsIgnoreCase("none")) {
            url = url + "&" + vendorPostbackConfigEntity.getParam5Name() + "=" + vendorPostbackConfigEntity.getParam5Value();
        }
        if (!vendorPostbackConfigEntity.getParam6Name().equalsIgnoreCase("none")) {
            url = url + "&" + vendorPostbackConfigEntity.getParam6Name() + "=" + vendorPostbackConfigEntity.getParam6Value();
        }
        if (!vendorPostbackConfigEntity.getParam7Name().equalsIgnoreCase("none")) {
            url = url + "&" + vendorPostbackConfigEntity.getParam7Name() + "=" + vendorPostbackConfigEntity.getParam7Value();
        }
        if (!vendorPostbackConfigEntity.getParam8Name().equalsIgnoreCase("none")) {
            url = url + "&" + vendorPostbackConfigEntity.getParam8Name() + "=" + vendorPostbackConfigEntity.getParam8Value();
        }
        vendorPostBackParamMap.put(Long.valueOf(vendorPostbackConfigEntity.getVendorPlanId()), url);
    }

    public int getJazz() {
        return jazz;
    }

    public int getWarid() {
        return warid;
    }

    public int getUfone() {
        return ufone;
    }

    public int getZong() {
        return zong;
    }

    public int getTelenor() {
        return telenor;
    }

}

package com.noeticworld.sgw.util;

import java.io.Serializable;
import java.util.Date;

public class ChargeRequestProperties implements Serializable {

    private long msisdn;
    private Integer operatorId;
    private Integer vendorPlanId;
    private String correlationId;
    private Date originDateTime;
    private Double chargingAmount;
    private String shortcode;
    private double taxAmount;
    private double shareAmount;
    private int attempts;
    private int isRenewal;
    private int dailyAttempts;
    private int subCycleId;

    public ChargeRequestProperties(long msisdn, Integer operatorId, Integer vendorPlanId, String correlationId, Date originDateTime, Double chargingAmount, String shortcode, double taxAmount, double shareAmount, int attempts, int isRenewal, int dailyAttempts, int subCycleId) {
        this.msisdn = msisdn;
        this.operatorId = operatorId;
        this.vendorPlanId = vendorPlanId;
        this.correlationId = correlationId;
        this.originDateTime = originDateTime;
        this.chargingAmount = chargingAmount;
        this.shortcode = shortcode;
        this.taxAmount = taxAmount;
        this.shareAmount = shareAmount;
        this.attempts = attempts;
        this.isRenewal = isRenewal;
        this.dailyAttempts = dailyAttempts;
        this.subCycleId = subCycleId;
    }

    public ChargeRequestProperties() {
    }

    public ChargeRequestProperties(ChargeRequestProperties chargeRequestProperties) {
        this.msisdn = chargeRequestProperties.getMsisdn();
        this.operatorId = chargeRequestProperties.getOperatorId();
        this.vendorPlanId = chargeRequestProperties.getVendorPlanId();
        this.correlationId = chargeRequestProperties.getCorrelationId();
        this.originDateTime = chargeRequestProperties.getOriginDateTime();
        this.chargingAmount = chargeRequestProperties.getChargingAmount();
        this.shortcode = chargeRequestProperties.getShortcode();
        this.taxAmount = chargeRequestProperties.getTaxAmount();
        this.shareAmount = chargeRequestProperties.getShareAmount();
        this.attempts = chargeRequestProperties.getAttempts();
        this.isRenewal = chargeRequestProperties.getIsRenewal();
        this.dailyAttempts = chargeRequestProperties.getDailyAttempts();
        this.subCycleId = chargeRequestProperties.getSubCycleId();
    }

    public Integer getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Integer operatorId) {
        this.operatorId = operatorId;
    }

    public double getShareAmount() {
        return shareAmount;
    }

    public void setShareAmount(double shareAmount) {
        this.shareAmount = shareAmount;
    }

    public long getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(long msisdn) {
        this.msisdn = msisdn;
    }

    public Integer getVendorPlanId() {
        return vendorPlanId;
    }

    public void setVendorPlanId(Integer vendorPlanId) {
        this.vendorPlanId = vendorPlanId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public Date getOriginDateTime() {
        return originDateTime;
    }

    public void setOriginDateTime(Date originDateTime) {
        this.originDateTime = originDateTime;
    }

    public double getChargingAmount() {
        return chargingAmount;
    }

    public void setChargingAmount(double chargingAmount) {
        this.chargingAmount = chargingAmount;
    }

    public String getShortcode() {
        return shortcode;
    }

    public void setShortcode(String shortcode) {
        this.shortcode = shortcode;
    }

    public double getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(double taxAmount) {
        this.taxAmount = taxAmount;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public int getIsRenewal() {
        return isRenewal;
    }

    public void setIsRenewal(int isRenewal) {
        this.isRenewal = isRenewal;
    }

    public int getDailyAttempts() {
        return dailyAttempts;
    }

    public void setDailyAttempts(int dailyAttempts) {
        this.dailyAttempts = dailyAttempts;
    }

    public int getSubCycleId() {
        return subCycleId;
    }

    public void setSubCycleId(int subCycleId) {
        this.subCycleId = subCycleId;
    }

}
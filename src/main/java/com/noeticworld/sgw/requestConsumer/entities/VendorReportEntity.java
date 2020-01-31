package com.noeticworld.sgw.requestConsumer.entities;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Objects;

@Entity
@Table(name = "vendor_report", schema = "public", catalog = "sgw")
public class VendorReportEntity {
    private int id;
    private Integer venodorPlanId;
    private String trackerId;
    private int msisdn;
    private Timestamp cdate;

    @Id
    @Column(name = "id")
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Basic
    @Column(name = "venodor_plan_id")
    public Integer getVenodorPlanId() {
        return venodorPlanId;
    }

    public void setVenodorPlanId(Integer venodorPlanId) {
        this.venodorPlanId = venodorPlanId;
    }

    @Basic
    @Column(name = "tracker_id")
    public String getTrackerId() {
        return trackerId;
    }

    public void setTrackerId(String trackerId) {
        this.trackerId = trackerId;
    }

    @Basic
    @Column(name = "msisdn")
    public int getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(int msisdn) {
        this.msisdn = msisdn;
    }

    @Basic
    @Column(name = "cdate")
    public Timestamp getCdate() {
        return cdate;
    }

    public void setCdate(Timestamp cdate) {
        this.cdate = cdate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VendorReportEntity that = (VendorReportEntity) o;
        return id == that.id &&
                msisdn == that.msisdn &&
                Objects.equals(venodorPlanId, that.venodorPlanId) &&
                Objects.equals(trackerId, that.trackerId) &&
                Objects.equals(cdate, that.cdate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, venodorPlanId, trackerId, msisdn, cdate);
    }
}

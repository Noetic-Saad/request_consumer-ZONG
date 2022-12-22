package com.noeticworld.sgw.requestConsumer.entities;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Objects;

@Entity
@Table(name = "vendor_requests_state", schema = "public", catalog = "sgw")
public class VendorRequestsStateEntity implements Serializable {
    private long id;
    private String correlationid;
    private String resultStatus;
    private Boolean isFetched;
    private Timestamp cdatetime;
    private String description;
    private Long vendorPlanId;

    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "vendor_req_state_pk",sequenceName = "vendor_req_state_pk",allocationSize=1, initialValue=1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE,generator = "vendor_req_state_pk")
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Basic
    @Column(name = "correlationid")
    public String getCorrelationid() {
        return correlationid;
    }

    public void setCorrelationid(String correlationid) {
        this.correlationid = correlationid;
    }

    @Basic
    @Column(name = "result_status")
    public String getResultStatus() {
        return resultStatus;
    }

    public void setResultStatus(String resultStatus) {
        this.resultStatus = resultStatus;
    }

    @Basic
    @Column(name = "is_fetched")
    public Boolean getFetched() {
        return isFetched;
    }

    public void setFetched(Boolean fetched) {
        isFetched = fetched;
    }

    @Basic
    @Column(name = "cdatetime")
    public Timestamp getCdatetime() {
        return cdatetime;
    }

    public void setCdatetime(Timestamp cdatetime) {
        this.cdatetime = cdatetime;
    }

    @Basic
    @Column(name = "description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VendorRequestsStateEntity that = (VendorRequestsStateEntity) o;
        return id == that.id &&
                Objects.equals(correlationid, that.correlationid) &&
                Objects.equals(resultStatus, that.resultStatus) &&
                Objects.equals(isFetched, that.isFetched) &&
                Objects.equals(cdatetime, that.cdatetime) &&
                Objects.equals(description, that.description)&&
                Objects.equals(vendorPlanId,that.vendorPlanId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, correlationid, resultStatus, isFetched, cdatetime, description,vendorPlanId);
    }

    @Basic
    @Column(name = "vendor_plan_id")
    public Long getVendorPlanId() {
        return vendorPlanId;
    }

    public void setVendorPlanId(Long vendorPlanId) {
        this.vendorPlanId = vendorPlanId;
    }
}

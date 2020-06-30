package q.rest.subscriber.model;

import java.util.Date;

public class SubscribeModel {

    private int companyId;
    private int planId;
    private int durationId;
    private int salesId;
    private Date startDate;
    private int actualDays;
    private int createdBy;
    private int createdBySubscriber;

    public int getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(int createdBy) {
        this.createdBy = createdBy;
    }

    public int getCreatedBySubscriber() {
        return createdBySubscriber;
    }

    public void setCreatedBySubscriber(int createdBySubscriber) {
        this.createdBySubscriber = createdBySubscriber;
    }

    public int getPlanId() {
        return planId;
    }

    public void setPlanId(int planId) {
        this.planId = planId;
    }

    public int getDurationId() {
        return durationId;
    }

    public void setDurationId(int durationId) {
        this.durationId = durationId;
    }

    public int getSalesId() {
        return salesId;
    }

    public void setSalesId(int salesId) {
        this.salesId = salesId;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public int getActualDays() {
        return actualDays;
    }

    public void setActualDays(int actualDays) {
        this.actualDays = actualDays;
    }

    public int getCompanyId() {
        return companyId;
    }

    public void setCompanyId(int companyId) {
        this.companyId = companyId;
    }
}

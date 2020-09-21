package q.rest.subscriber.model.publicapi;

import java.util.List;

public class PbLoginObject {
    private PbCompany company;
    private PbSubscriber subscriber;
    private String jwt;
    private String refreshJwt;
    private List<Integer> activities;

    public PbLoginObject(PbCompany company, PbSubscriber subscriber, String jwt) {
        this.company = company;
        this.subscriber = subscriber;
        this.jwt = jwt;
    }

    public PbLoginObject(PbCompany company, PbSubscriber subscriber, String jwt, String refreshJwt, List<Integer> activities) {
        this.company = company;
        this.subscriber = subscriber;
        this.jwt = jwt;
        this.refreshJwt = refreshJwt;
        this.activities = activities;
    }

    public String getRefreshJwt() {
        return refreshJwt;
    }

    public void setRefreshJwt(String refreshJwt) {
        this.refreshJwt = refreshJwt;
    }

    public List<Integer> getActivities() {
        return activities;
    }

    public void setActivities(List<Integer> activities) {
        this.activities = activities;
    }

    public String getJwt() {
        return jwt;
    }

    public void setJwt(String jwt) {
        this.jwt = jwt;
    }

    public PbCompany getCompany() {
        return company;
    }

    public void setCompany(PbCompany company) {
        this.company = company;
    }

    public PbSubscriber getSubscriber() {
        return subscriber;
    }

    public void setSubscriber(PbSubscriber subscriber) {
        this.subscriber = subscriber;
    }
}
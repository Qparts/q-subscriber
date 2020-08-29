package q.rest.subscriber.model.publicapi;

import q.rest.subscriber.model.entity.Company;
import q.rest.subscriber.model.entity.Subscriber;

public class PbLoginObject {
    private PbCompany company;
    private PbSubscriber subscriber;
    private String jwt;

    public PbLoginObject(PbCompany company, PbSubscriber subscriber, String jwt) {
        this.company = company;
        this.subscriber = subscriber;
        this.jwt = jwt;
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

package q.rest.subscriber.model;

import q.rest.subscriber.model.entity.Company;
import q.rest.subscriber.model.entity.Subscriber;

public class LoginObject {
    private Company company;
    private Subscriber subscriber;
    private String jwt;

    public LoginObject(Company company, Subscriber subscriber, String jwt) {
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

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public Subscriber getSubscriber() {
        return subscriber;
    }

    public void setSubscriber(Subscriber subscriber) {
        this.subscriber = subscriber;
    }
}

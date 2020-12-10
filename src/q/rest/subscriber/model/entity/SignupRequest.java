package q.rest.subscriber.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import q.rest.subscriber.helper.Helper;
import q.rest.subscriber.model.AddSubscriberModel;
import q.rest.subscriber.model.SignupModel;

import javax.persistence.*;
import java.util.Date;

@Table(name="sub_signup_request")
@Entity
public class SignupRequest {
    @Id
    @SequenceGenerator(name = "sub_signup_request_id_seq_gen", sequenceName = "sub_signup_request_id_seq", initialValue=1, allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "sub_signup_request_id_seq_gen")
    private int id;
    private char signupType;//N = new signup from website , A = additional user added by company
    private String companyName;
    private int companyId;
    @JsonIgnore
    private String password;
    private String name;
    private int countryId;
    private int regionId;
    private int cityId;
    private String mobile;
    private String email;
    @Temporal(TemporalType.TIMESTAMP)
    private Date created;
    private int createdBy;
    private int createdBySubscriber;
    private String notes;
    private char status;//R = requested, C = Created, P = pending, D = declined
    private Boolean mobileVerified;
    private Boolean emailVerified;
    private int appCode;

    public Boolean isMobileVerified() {
        return mobileVerified;
    }

    public void setMobileVerified(Boolean mobileVerified) {
        this.mobileVerified = mobileVerified;
    }

    public Boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public SignupRequest() {
    }

    public SignupRequest(SignupModel model, int appCode) {
        this.companyName = model.getCompanyName();
        this.password = Helper.cypher(model.getPassword());
        this.name = model.getName();
        this.countryId = model.getCountryId();
        this.regionId = model.getRegionId();
        this.cityId = model.getCityId();
        this.mobile = model.getMobile();
        this.email = model.getEmail().toLowerCase().trim();
        this.created = new Date();
        this.createdBy = model.getCreatedBy();
        this.notes = model.getNotes();
        this.signupType = 'N';
        this.status = 'R';
        this.appCode = appCode;
    }

    public SignupRequest(AddSubscriberModel model) {
        this.password = Helper.cypher(model.getPassword());
        this.name = model.getName();
        this.mobile = model.getMobile();
        this.email = model.getEmail().toLowerCase().trim();
        this.created = new Date();
        this.createdBy = model.getCreatedBy();
        this.createdBySubscriber = model.getCreatedBySubscriber();
        this.notes = model.getNotes();
        this.signupType = 'A';
        this.status = 'R';

    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCountryId() {
        return countryId;
    }

    public void setCountryId(int countryId) {
        this.countryId = countryId;
    }

    public int getRegionId() {
        return regionId;
    }

    public void setRegionId(int regionId) {
        this.regionId = regionId;
    }

    public int getCityId() {
        return cityId;
    }

    public void setCityId(int cityId) {
        this.cityId = cityId;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public int getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(int createdBy) {
        this.createdBy = createdBy;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public char getStatus() {
        return status;
    }

    public void setStatus(char status) {
        this.status = status;
    }

    public char getSignupType() {
        return signupType;
    }

    public void setSignupType(char signupType) {
        this.signupType = signupType;
    }

    public int getCompanyId() {
        return companyId;
    }

    public void setCompanyId(int companyId) {
        this.companyId = companyId;
    }

    public int getCreatedBySubscriber() {
        return createdBySubscriber;
    }

    public void setCreatedBySubscriber(int createdBySubscriber) {
        this.createdBySubscriber = createdBySubscriber;
    }

    public int getAppCode() {
        return appCode;
    }

    public void setAppCode(int appCode) {
        this.appCode = appCode;
    }
}

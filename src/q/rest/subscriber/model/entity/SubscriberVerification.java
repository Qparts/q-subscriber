package q.rest.subscriber.model.entity;

import javax.persistence.*;
import java.util.Date;

@Table(name="sub_subscriber_verification")
@Entity
public class SubscriberVerification {

    @Id
    @SequenceGenerator(name = "sub_subscriber_verification_id_seq_gen", sequenceName = "sub_subscriber_verification_id_seq", initialValue=1, allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "sub_subscriber_verification_id_seq_gen")
    private int id;
    private int stage;//1 = new signup, 2 = for verifying after signup, 3 = for creating new additional subscriber
    private int signupRequestId;//used for stage 1 and 3
    private int subscriberId;//used for stage 2
    private int companyId;//used for stage 3
    private char verificationMode;//M , E
    private String verificationCode;
    @Temporal(TemporalType.TIMESTAMP)
    private Date created;


    public SubscriberVerification() {

    }

    public SubscriberVerification(SignupRequest signupRequest, char mode, String code, int companyId) {
        this.stage = 3;
        this.signupRequestId = signupRequest.getId();
        this.verificationMode = mode;
        this.verificationCode = code;
        this.created = new Date();
        this.companyId = companyId;
    }

    public SubscriberVerification(SignupRequest signupRequest, char mode, String code) {
        this.stage = 1;
        this.signupRequestId = signupRequest.getId();
        this.verificationMode = mode;
        this.verificationCode = code;
        this.created = new Date();
    }

    public SubscriberVerification(Subscriber subscriber, char mode, String code) {
        this.stage = 2;
        this.subscriberId = subscriber.getId();
        this.verificationMode = mode;
        this.verificationCode = code;
        this.created = new Date();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getStage() {
        return stage;
    }

    public void setStage(int stage) {
        this.stage = stage;
    }

    public int getSignupRequestId() {
        return signupRequestId;
    }

    public void setSignupRequestId(int signupRequestId) {
        this.signupRequestId = signupRequestId;
    }

    public char getVerificationMode() {
        return verificationMode;
    }

    public void setVerificationMode(char verificationMode) {
        this.verificationMode = verificationMode;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public int getSubscriberId() {
        return subscriberId;
    }

    public void setSubscriberId(int subscriberId) {
        this.subscriberId = subscriberId;
    }

    public int getCompanyId() {
        return companyId;
    }

    public void setCompanyId(int companyId) {
        this.companyId = companyId;
    }
}

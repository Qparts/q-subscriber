package q.rest.subscriber.model.entity;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name="sub_refresh_token")
public class RefreshToken {
    @Id
    @SequenceGenerator(name = "sub_refresh_token_id_seq_gen", sequenceName = "sub_refresh_token_id_seq", initialValue=1, allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "sub_refresh_token_id_seq_gen")
    private long id;
    private int subscriberId;
    @Temporal(TemporalType.TIMESTAMP)
    private Date issuedAt;
    @Temporal(TemporalType.TIMESTAMP)
    private Date expiresAt;
    private char status;//A = active , K = killed
    private int appCode;
    private String token;

    public int getAppCode() {
        return appCode;
    }

    public void setAppCode(int appCode) {
        this.appCode = appCode;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getSubscriberId() {
        return subscriberId;
    }

    public void setSubscriberId(int subscriberId) {
        this.subscriberId = subscriberId;
    }

    public Date getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Date issuedAt) {
        this.issuedAt = issuedAt;
    }

    public Date getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Date expiresAt) {
        this.expiresAt = expiresAt;
    }

    public char getStatus() {
        return status;
    }

    public void setStatus(char status) {
        this.status = status;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}

package q.rest.subscriber.model.entity;

import q.rest.subscriber.helper.Helper;

import javax.persistence.*;
import java.util.Date;

@Table(name = "sub_password_reset")
@Entity
public class PasswordReset {

    @Id
    @SequenceGenerator(name = "sub_password_reset_id_seq_gen", sequenceName = "sub_password_reset_id_seq", initialValue = 1, allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "sub_password_reset_id_seq_gen")
    private int id;
    private int subscriberId;
    private String token;
    @Temporal(TemporalType.TIMESTAMP)
    private Date created;
    @Temporal(TemporalType.TIMESTAMP)
    private Date expire;
    private char status;

    public PasswordReset(){}

    public PasswordReset(int subscriberId, String token, int expireMinutes){
        this.created = new Date();
        this.subscriberId = subscriberId;
        this.token = token;
        this.expire = Helper.addMinutes(created, expireMinutes);
        this.status = 'R';//requested
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSubscriberId() {
        return subscriberId;
    }

    public void setSubscriberId(int subscriberId) {
        this.subscriberId = subscriberId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getExpire() {
        return expire;
    }

    public void setExpire(Date expire) {
        this.expire = expire;
    }

    public char getStatus() {
        return status;
    }

    public void setStatus(char status) {
        this.status = status;
    }
}

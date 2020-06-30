package q.rest.subscriber.model.entity;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name="sub_login_attempt")
public class LoginAttempt {
    @Id
    @SequenceGenerator(name = "sub_login_attempt_id_seq_gen", sequenceName = "sub_login_attempt_id_seq", initialValue=1, allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "sub_login_attempt_id_seq_gen")
    private int id;
    private String username;
    private boolean success;
    @Column(name="subscriber_id")
    private int subscriberId;
    @Temporal(TemporalType.TIMESTAMP)
    private Date created;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getSubscriberId() {
        return subscriberId;
    }

    public void setSubscriberId(int subscriberId) {
        this.subscriberId = subscriberId;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }
}

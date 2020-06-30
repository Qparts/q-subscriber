package q.rest.subscriber.model.entity.role.general;

import q.rest.subscriber.model.entity.Subscriber;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name="sub_subscriber_general_role")
@IdClass(SubscriberGeneralRole.SubscriberGeneralRolePK.class)
public class SubscriberGeneralRole {

    @Id
    @JoinColumn(name="role_id", referencedColumnName="id")
    @ManyToOne()
    private GeneralRole role;

    @Id
    @JoinColumn(name="subscriber_id", referencedColumnName="id")
    @ManyToOne()
    private Subscriber subscriber;

    public GeneralRole getRole() {
        return role;
    }

    public void setRole(GeneralRole role) {
        this.role = role;
    }

    public Subscriber getSubscriber() {
        return subscriber;
    }

    public void setSubscriber(Subscriber subscriber) {
        this.subscriber = subscriber;
    }

    public static class SubscriberGeneralRolePK implements Serializable {

        protected int role;
        protected int subscriber;

        public SubscriberGeneralRolePK() {}

        public int getRole() {
            return role;
        }

        public void setRole(int role) {
            this.role = role;
        }

        public int getSubscriber() {
            return subscriber;
        }

        public void setSubscriber(int subscriber) {
            this.subscriber = subscriber;
        }
    }
}

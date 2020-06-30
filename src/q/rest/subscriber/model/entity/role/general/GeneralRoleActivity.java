package q.rest.subscriber.model.entity.role.general;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name="sub_general_role_activity")
@IdClass(GeneralRoleActivity.GeneralRoleActivityPK.class)
public class GeneralRoleActivity {

    @Id
    @JoinColumn(name="role_id", referencedColumnName="id")
    @ManyToOne()
    private GeneralRole role;

    @Id
    @JoinColumn(name="activity_id", referencedColumnName="id")
    @ManyToOne()
    private GeneralActivity activity;

    public GeneralRole getRole() {
        return role;
    }

    public void setRole(GeneralRole role) {
        this.role = role;
    }

    public GeneralActivity getActivity() {
        return activity;
    }

    public void setActivity(GeneralActivity activity) {
        this.activity = activity;
    }

    public static class GeneralRoleActivityPK implements Serializable {

        protected int role;
        protected int activity;

        public GeneralRoleActivityPK() {}

        public int getRole() {
            return role;
        }

        public void setRole(int role) {
            this.role = role;
        }

        public int getActivity() {
            return activity;
        }

        public void setActivity(int activity) {
            this.activity = activity;
        }
    }
}

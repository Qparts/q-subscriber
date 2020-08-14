package q.rest.subscriber.model.entity;

import q.rest.subscriber.model.entity.role.general.GeneralRole;
import q.rest.subscriber.model.entity.role.general.GeneralRoleActivity;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name="sub_search_limit")
@IdClass(SearchLimit.SearchLimiPK.class)
public class SearchLimit implements Serializable {
    @Id
    private int companyId;
    @Id
    private Date created;

    public int getCompanyId() {
        return companyId;
    }

    public void setCompanyId(int companyId) {
        this.companyId = companyId;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public static class SearchLimiPK implements Serializable {
        private int companyId;
        private Date created;

        public SearchLimiPK() {
        }

        public int getCompanyId() {
            return companyId;
        }

        public void setCompanyId(int companyId) {
            this.companyId = companyId;
        }

        public Date getCreated() {
            return created;
        }

        public void setCreated(Date created) {
            this.created = created;
        }
    }
}

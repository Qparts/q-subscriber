package q.rest.subscriber.model.entity.keywords;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SearchLimiPK that = (SearchLimiPK) o;
            return companyId == that.companyId &&
                    Objects.equals(created, that.created);
        }

        @Override
        public int hashCode() {
            return Objects.hash(companyId, created);
        }
    }
}

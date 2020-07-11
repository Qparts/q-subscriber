package q.rest.subscriber.model.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

@Table(name = "sub_search_keyword")
@Entity
@IdClass(SearchKeyword.SearchKeywordPK.class)
public class SearchKeyword implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    private int subscriberId;
    @Id
    @Temporal(TemporalType.TIMESTAMP)
    private Date created;
    private String query;
    private int companyId;
    private int appCode;
    private boolean found;

    public static long getSerialVersionUID() {
        return serialVersionUID;
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

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public int getCompanyId() {
        return companyId;
    }

    public void setCompanyId(int companyId) {
        this.companyId = companyId;
    }

    public int getAppCode() {
        return appCode;
    }

    public void setAppCode(int appCode) {
        this.appCode = appCode;
    }

    public boolean getFound() {
        return found;
    }

    public void setFound(boolean found) {
        this.found = found;
    }

    public static class SearchKeywordPK implements Serializable{

        private static final long serialVersionUID = 1L;
        protected int subscriberId;
        protected Date created;
        public SearchKeywordPK() {}

        public int getSubscriberId() {
            return subscriberId;
        }

        public void setSubscriberId(int subscriberId) {
            this.subscriberId = subscriberId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SearchKeywordPK that = (SearchKeywordPK) o;
            return subscriberId == that.subscriberId &&
                    Objects.equals(created, that.created);
        }

        @Override
        public int hashCode() {
            return Objects.hash(subscriberId, created);
        }

        public Date getCreated() {
            return created;
        }

        public void setCreated(Date created) {
            this.created = created;
        }
    }
}

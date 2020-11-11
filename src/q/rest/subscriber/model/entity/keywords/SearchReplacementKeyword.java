package q.rest.subscriber.model.entity.keywords;


import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

@Table(name = "sub_replacement_search_keyword")
@Entity
@IdClass(SearchReplacementKeyword.SearchReplacementKeywordPK.class)
public class SearchReplacementKeyword {

    @Id
    private int subscriberId;
    @Id
    @Temporal(TemporalType.TIMESTAMP)
    private Date created;
    private String query;
    private int companyId;
    private int appCode;
    private boolean found;


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

    public boolean isFound() {
        return found;
    }

    public void setFound(boolean found) {
        this.found = found;
    }

    public static class SearchReplacementKeywordPK implements Serializable {

        private static final long serialVersionUID = 1L;
        protected int subscriberId;
        protected Date created;

        public SearchReplacementKeywordPK() {}

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
            SearchKeyword.SearchKeywordPK that = (SearchKeyword.SearchKeywordPK) o;
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

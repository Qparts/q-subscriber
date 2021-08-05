package q.rest.subscriber.model.publicapi;

import com.fasterxml.jackson.annotation.JsonIgnore;
import q.rest.subscriber.model.entity.role.general.GeneralRole;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name="sub_subscriber")
public class PbSubscriberVisible {
    @Id
    private int id;
    @JoinColumn(name="company_id", referencedColumnName = "id")
    @ManyToOne
    private PbCompanyVisible company;
    private String name;
    private Integer defaultBranch;

    public Integer getDefaultBranch() {
        return defaultBranch;
    }

    public void setDefaultBranch(Integer defaultBranch) {
        this.defaultBranch = defaultBranch;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PbCompanyVisible getCompany() {
        return company;
    }

    public void setCompany(PbCompanyVisible company) {
        this.company = company;
    }
}

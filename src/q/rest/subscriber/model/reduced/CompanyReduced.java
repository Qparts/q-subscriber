package q.rest.subscriber.model.reduced;


import com.fasterxml.jackson.annotation.JsonIgnore;
import q.rest.subscriber.model.entity.Branch;
import q.rest.subscriber.model.entity.SignupRequest;
import q.rest.subscriber.model.entity.Subscriber;
import q.rest.subscriber.model.entity.Subscription;
import q.rest.subscriber.model.entity.role.general.GeneralRole;

import javax.persistence.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name="sub_company")
public class CompanyReduced {
    @Id
    private int id;
    private String name;
    private String nameAr;
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "company_id")
    private Set<BranchReduced> branches = new HashSet<>();


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

    public String getNameAr() {
        return nameAr;
    }

    public void setNameAr(String nameAr) {
        this.nameAr = nameAr;
    }

    public Set<BranchReduced> getBranches() {
        return branches;
    }

    public void setBranches(Set<BranchReduced> branches) {
        this.branches = branches;
    }
}

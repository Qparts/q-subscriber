package q.rest.subscriber.model.reduced;


import q.rest.subscriber.model.entity.CompanyContact;

import javax.persistence.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name="sub_company_branch")
public class BranchReduced {
    @Id
    private int id;
    @Column(name="company_id")
    private int companyId;
    private String name;
    private String nameAr;
    private int cityId;
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name="branch_id")
    private Set<CompanyContactReduced> contacts = new HashSet<>();

    public BranchReduced() {
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCompanyId() {
        return companyId;
    }

    public void setCompanyId(int companyId) {
        this.companyId = companyId;
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

    public Set<CompanyContactReduced> getContacts() {
        return contacts;
    }

    public void setContacts(Set<CompanyContactReduced> contacts) {
        this.contacts = contacts;
    }

    public int getCityId() {
        return cityId;
    }

    public void setCityId(int cityId) {
        this.cityId = cityId;
    }
}

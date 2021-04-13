package q.rest.subscriber.model.entity.role.company;

import com.fasterxml.jackson.annotation.JsonIgnore;
import q.rest.subscriber.model.entity.role.general.GeneralActivity;

import javax.persistence.*;
import java.util.Set;

@Entity
@Table(name = "sub_company_role")
public class CompanyRole {
    @Id
    @SequenceGenerator(name = "sub_company_role_id_seq_gen", sequenceName = "sub_company_role_id_seq", initialValue=1, allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "sub_company_role_id_seq_gen")
    private int id;
    private int companyId;
    private String name;
    @JsonIgnore
    private char status;
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name="sub_company_role_activity",
            joinColumns = @JoinColumn(name="role_id"),
            inverseJoinColumns = @JoinColumn(name="activity_id"))
    private Set<CompanyActivity> activities;


    public Set<CompanyActivity> getActivities() {
        return activities;
    }

    public void setActivities(Set<CompanyActivity> activities) {
        this.activities = activities;
    }

    public int getCompanyId() {
        return companyId;
    }

    public void setCompanyId(int companyId) {
        this.companyId = companyId;
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

    public char getStatus() {
        return status;
    }

    public void setStatus(char status) {
        this.status = status;
    }
}

package q.rest.subscriber.model.publicapi;


import com.fasterxml.jackson.annotation.JsonIgnore;
import q.rest.subscriber.model.entity.*;
import q.rest.subscriber.model.entity.role.general.GeneralRole;

import javax.persistence.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name="sub_company")
public class PbCompany {
    @Id
    private int id;
    private String name;
    private String nameAr;
    @JsonIgnore
    private char status;//A = active , I = inactive, Z = temporary created for atomicity
    @Temporal(TemporalType.TIMESTAMP)
    private Date created;
    private int countryId;
    private int regionId;
    private int cityId;
    @JsonIgnore
    private boolean integrated;
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "company_id")
    private Set<PbBranch> branches = new HashSet<>();
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name="company_id")
    private Set<PbSubscriber> subscribers = new HashSet<>();
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name="company_id")
    @OrderBy("startDate desc")
    private Set<PbSubscription> subscriptions = new HashSet<>();

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

    public char getStatus() {
        return status;
    }

    public void setStatus(char status) {
        this.status = status;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public int getCountryId() {
        return countryId;
    }

    public void setCountryId(int countryId) {
        this.countryId = countryId;
    }

    public int getRegionId() {
        return regionId;
    }

    public void setRegionId(int regionId) {
        this.regionId = regionId;
    }

    public int getCityId() {
        return cityId;
    }

    public void setCityId(int cityId) {
        this.cityId = cityId;
    }

    public boolean isIntegrated() {
        return integrated;
    }

    public void setIntegrated(boolean integrated) {
        this.integrated = integrated;
    }

    public Set<PbBranch> getBranches() {
        return branches;
    }

    public void setBranches(Set<PbBranch> branches) {
        this.branches = branches;
    }

    public Set<PbSubscriber> getSubscribers() {
        return subscribers;
    }

    public void setSubscribers(Set<PbSubscriber> subscribers) {
        this.subscribers = subscribers;
    }

}

package q.rest.subscriber.model.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.Where;
import q.rest.subscriber.model.entity.role.general.GeneralRole;

import javax.persistence.*;
import java.util.*;

@Entity
@Table(name="sub_company")
public class Company {
    @Id
    @SequenceGenerator(name = "sub_company_id_seq_gen", sequenceName = "sub_company_id_seq", initialValue=1, allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "sub_company_id_seq_gen")
    private int id;
    private String name;
    private String nameAr;
    private char status;//A = active , I = inactive, Z = temporary created for atomicity
    @Temporal(TemporalType.TIMESTAMP)
    private Date created;
    private int createdBy;
    private boolean profileCompleted;
    private int countryId;
    private int regionId;
    private int cityId;
    private String integrationSecretCode;
    private String endpointAddress;
    private boolean integrated;
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "company_id")
    private Set<Branch> branches = new HashSet<>();
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "company_id")
    @OrderBy("created desc")
    @Where(clause = "status = 'A'")
    private Set<Comment> comments = new HashSet<>();
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name="company_id")
    private Set<Subscriber> subscribers = new HashSet<>();
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name="company_id")
    @OrderBy("startDate desc")
    private Set<Subscription> subscriptions = new HashSet<>();
    //remove label
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name="sub_company_label", joinColumns = @JoinColumn(name="company_id"), inverseJoinColumns = @JoinColumn(name="label_id"))
    @OrderBy(value = "id")
    private Set<Label> labels = new HashSet<>();

    public Company() {
    }

    public Company(SignupRequest sr, char verificationMode, int planId, int durationId, GeneralRole generalRole){
        this.name = sr.getCompanyName();
        this.nameAr = sr.getCompanyName();
        this.status = 'A';
        this.created = new Date();
        this.createdBy = sr.getCreatedBy();
        this.profileCompleted = false;
        this.setCountryId(sr.getCountryId());
        this.setRegionId(sr.getRegionId());
        this.setCityId(sr.getCityId());
        this.integrated = false;
        //this.branches.add(new Branch(sr));//don't create branch
        this.subscribers.add(new Subscriber(sr, verificationMode, generalRole));

        Subscription sub = new Subscription();
        sub.setCreated(new Date());
        sub.setPlanId(planId);
        sub.setDurationId(durationId);
        sub.setStartDate(new Date());
        sub.setStatus('B');//base
        this.subscriptions.add(sub);

    }

    @JsonIgnore
    public void updateSubscribersRoles(GeneralRole role){
        for(Subscriber subscriber : subscribers){
            subscriber.getRoles().clear();
            subscriber.getRoles().add(role);
        }
    }


    @JsonIgnore
    public Subscription getActiveSubscription(){
        for(var sub : subscriptions){
            //get premium
            if(sub.getStatus() == 'A'){
                if(sub.getEndDate().after(new Date())){
                    return sub;
                }
            }
            //no premium ? get basic
            if(sub.getStatus() == 'B'){
                return sub;//everyone starts with basic. There is only one basic
            }
        }
        return null;
    }

    @JsonIgnore
    public Subscription getFutureSubscription(){
        for(var sub : subscriptions){
            //get future
            if(sub.getStatus() == 'F'){
                return sub;
            }
        }
        return null;
    }

    @JsonIgnore
    public Subscriber getAdminSubscriber(){
        for(Subscriber s : subscribers){
            if(s.isAdmin()) return s;
        }
        return null;
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

    public int getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(int createdBy) {
        this.createdBy = createdBy;
    }

    public boolean isProfileCompleted() {
        return profileCompleted;
    }

    public void setProfileCompleted(boolean profileCompleted) {
        this.profileCompleted = profileCompleted;
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

    public String getIntegrationSecretCode() {
        return integrationSecretCode;
    }

    public void setIntegrationSecretCode(String integrationSecretCode) {
        this.integrationSecretCode = integrationSecretCode;
    }

    public String getEndpointAddress() {
        return endpointAddress;
    }

    public void setEndpointAddress(String endpointAddress) {
        this.endpointAddress = endpointAddress;
    }

    public boolean isIntegrated() {
        return integrated;
    }

    public void setIntegrated(boolean integrated) {
        this.integrated = integrated;
    }

    public Set<Branch> getBranches() {
        return branches;
    }

    public void setBranches(Set<Branch> branches) {
        this.branches = branches;
    }

    public Set<Subscriber> getSubscribers() {
        return subscribers;
    }

    public void setSubscribers(Set<Subscriber> subscribers) {
        this.subscribers = subscribers;
    }

    public Set<Subscription> getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(Set<Subscription> subscriptions) {
        this.subscriptions = subscriptions;
    }

    public Set<Label> getLabels() {
        return labels;
    }

    public void setLabels(Set<Label> labels) {
        this.labels = labels;
    }

    public Set<Comment> getComments() {
        return comments;
    }

    public void setComments(Set<Comment> comments) {
        this.comments = comments;
    }
}

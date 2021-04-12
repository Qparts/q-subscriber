package q.rest.subscriber.model.view;

import com.fasterxml.jackson.annotation.JsonIgnore;
import q.rest.subscriber.model.publicapi.PbBranch;
import q.rest.subscriber.model.publicapi.PbSubscriber;
import q.rest.subscriber.model.publicapi.PbSubscription;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name="sub_view_company")
public class CompanyView {
    @Id
    @Column(name="company_id")
    private int companyId;
    private String name;
    private String nameAr;
    @JsonIgnore
    private char status;
    private int countryId;
    private Integer defaultBranchId;//mandatory
    private Integer defaultPolicyId;//mandatory
    private Integer defaultCustomerId;//mandatory
    private Double defaultSalesTax;//mandatory
    private Double defaultPurchaseTax;//mandatory
    private String vatNumber;//mandatory
    private Boolean logoUploaded;
    private String invoiceTemplate;

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

    public boolean isProfileCompleted(){
        return defaultBranchId != null
                && defaultPolicyId != null
                && defaultCustomerId != null
                && defaultSalesTax != null
                && defaultPurchaseTax != null
                && vatNumber != null;
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

    public char getStatus() {
        return status;
    }

    public void setStatus(char status) {
        this.status = status;
    }

    public int getCountryId() {
        return countryId;
    }

    public void setCountryId(int countryId) {
        this.countryId = countryId;
    }

    public Integer getDefaultBranchId() {
        return defaultBranchId;
    }

    public void setDefaultBranchId(Integer defaultBranchId) {
        this.defaultBranchId = defaultBranchId;
    }

    public Integer getDefaultPolicyId() {
        return defaultPolicyId;
    }

    public void setDefaultPolicyId(Integer defaultPolicyId) {
        this.defaultPolicyId = defaultPolicyId;
    }

    public Integer getDefaultCustomerId() {
        return defaultCustomerId;
    }

    public void setDefaultCustomerId(Integer defaultCustomerId) {
        this.defaultCustomerId = defaultCustomerId;
    }

    public Double getDefaultSalesTax() {
        return defaultSalesTax;
    }

    public void setDefaultSalesTax(Double defaultSalesTax) {
        this.defaultSalesTax = defaultSalesTax;
    }

    public Double getDefaultPurchaseTax() {
        return defaultPurchaseTax;
    }

    public void setDefaultPurchaseTax(Double defaultPurchaseTax) {
        this.defaultPurchaseTax = defaultPurchaseTax;
    }

    public String getVatNumber() {
        return vatNumber;
    }

    public void setVatNumber(String vatNumber) {
        this.vatNumber = vatNumber;
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

    public Set<PbSubscription> getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(Set<PbSubscription> subscriptions) {
        this.subscriptions = subscriptions;
    }

    public Boolean getLogoUploaded() {
        return logoUploaded;
    }

    public void setLogoUploaded(Boolean logoUploaded) {
        this.logoUploaded = logoUploaded;
    }

    public String getInvoiceTemplate() {
        return invoiceTemplate;
    }

    public void setInvoiceTemplate(String invoiceTemplate) {
        this.invoiceTemplate = invoiceTemplate;
    }
}

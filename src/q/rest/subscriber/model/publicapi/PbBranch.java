package q.rest.subscriber.model.publicapi;


import com.fasterxml.jackson.annotation.JsonIgnore;
import q.rest.subscriber.model.entity.CompanyContact;

import javax.persistence.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name="sub_company_branch")
public class PbBranch {
    @Id
    private int id;
    @Column(name="company_id")
    private int companyId;
    private String name;
    private String nameAr;
    private int countryId;
    private int regionId;
    private int cityId;
    @JsonIgnore
    private char status;
    @JsonIgnore
    private String clientBranchId;
    private double longitude;
    private double latitude;
    private int mapZoom;
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name="branch_id")
    private Set<PbCompanyContact> contacts = new HashSet<>();

    public PbBranch() {
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

    public char getStatus() {
        return status;
    }

    public void setStatus(char status) {
        this.status = status;
    }

    public String getClientBranchId() {
        return clientBranchId;
    }

    public void setClientBranchId(String clientBranchId) {
        this.clientBranchId = clientBranchId;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public int getMapZoom() {
        return mapZoom;
    }

    public void setMapZoom(int mapZoom) {
        this.mapZoom = mapZoom;
    }

    public Set<PbCompanyContact> getContacts() {
        return contacts;
    }

    public void setContacts(Set<PbCompanyContact> contacts) {
        this.contacts = contacts;
    }


    @JsonIgnore
    public void setPbContacts(Set<CompanyContact> contacts){

    }
}

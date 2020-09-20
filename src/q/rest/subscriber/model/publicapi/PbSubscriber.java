package q.rest.subscriber.model.publicapi;

import com.fasterxml.jackson.annotation.JsonIgnore;
import q.rest.subscriber.model.entity.SignupRequest;
import q.rest.subscriber.model.entity.role.general.GeneralActivity;
import q.rest.subscriber.model.entity.role.general.GeneralRole;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name="sub_subscriber")
public class PbSubscriber {
    @Id
    private int id;
    @Column(name="company_id")
    private int companyId;
    private String email;
    private boolean emailVerified;
    private String mobile;
    private boolean mobileVerified;
    private String name;
    private boolean admin;
    @JsonIgnore
    private String password;
    @JsonIgnore
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name="sub_subscriber_general_role",
            joinColumns = @JoinColumn(name="subscriber_id"),
            inverseJoinColumns = @JoinColumn(name="role_id"))
    @OrderBy(value = "id")
    private Set<GeneralRole> roles = new HashSet<>();

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public boolean isMobileVerified() {
        return mobileVerified;
    }

    public void setMobileVerified(boolean mobileVerified) {
        this.mobileVerified = mobileVerified;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Set<GeneralRole> getRoles() {
        return roles;
    }

    public void setRoles(Set<GeneralRole> roles) {
        this.roles = roles;
    }
}

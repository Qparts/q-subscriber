package q.rest.subscriber.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import q.rest.subscriber.model.entity.role.general.GeneralRole;

import javax.persistence.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name="sub_subscriber")
public class Subscriber {
    @Id
    @SequenceGenerator(name = "sub_subscriber_id_seq_gen", sequenceName = "sub_subscriber_id_seq", initialValue=1, allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "sub_subscriber_id_seq_gen")
    private int id;
    @Column(name="company_id")
    private int companyId;
    private String email;
    private boolean emailVerified;
    private String mobile;
    private boolean mobileVerified;
    private String name;
    @Temporal(TemporalType.TIMESTAMP)
    private Date created;
    private int createdBy;
    private String notes;
    private boolean admin;
    @JsonIgnore
    private String password;
    private char status;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name="sub_subscriber_general_role",
            joinColumns = @JoinColumn(name="subscriber_id"),
            inverseJoinColumns = @JoinColumn(name="role_id"))
    @OrderBy(value = "id")
    private Set<GeneralRole> roles = new HashSet<>();


    public Subscriber() {
    }


    public Subscriber(SignupRequest sr, char verificationMode, GeneralRole generalRole) {
        this.email = sr.getEmail();
        this.mobile = sr.getMobile();
        this.name = sr.getName();
        this.created = new Date();
        this.createdBy = sr.getCreatedBy();
        this.emailVerified = verificationMode == 'E';
        this.mobileVerified = verificationMode == 'M';
        this.admin = true;
        this.password = sr.getPassword();
        this.roles.add(generalRole);
        this.status = 'A';
    }


    @JsonIgnore
    public boolean hasAccess(int id) {
        for (var role : roles) {
            for (var ac : role.getActivities()) {
                if(ac.getId() == id)
                    return true;
            }
        }
        return  false;
    }


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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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

    public char getStatus() {
        return status;
    }

    public void setStatus(char status) {
        this.status = status;
    }
}

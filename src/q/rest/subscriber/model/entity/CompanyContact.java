package q.rest.subscriber.model.entity;

import javax.persistence.*;
import java.util.Date;

@Table(name = "sub_company_contact")
@Entity
public class CompanyContact {

    @Id
    @SequenceGenerator(name = "sub_company_contact_id_seq_gen", sequenceName = "sub_company_contact_id_seq", initialValue=1, allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "sub_company_contact_id_seq_gen")
    private int id;
    @Column(name="company_id")
    private int companyId;
    @Column(name="branch_id")
    private Integer branchId;
    private String name;
    @Temporal(TemporalType.TIMESTAMP)
    private Date created;
    private int createdBy;
    private int createdBySubscriber;
    private String phone;
    private String extension;
    private String email;
    private char status;
    private String notes;

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

    public Integer getBranchId() {
        return branchId;
    }

    public void setBranchId(Integer branchId) {
        this.branchId = branchId;
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

    public int getCreatedBySubscriber() {
        return createdBySubscriber;
    }

    public void setCreatedBySubscriber(int createdBySubscriber) {
        this.createdBySubscriber = createdBySubscriber;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public char getStatus() {
        return status;
    }

    public void setStatus(char status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}

package q.rest.subscriber.model.reduced;

import javax.persistence.*;
import java.util.Date;

@Table(name = "sub_company_contact")
@Entity
public class CompanyContactReduced {
    @Id
    private int id;
    @Column(name="company_id")
    private int companyId;
    @Column(name="branch_id")
    private Integer branchId;
    private String name;
    private String phone;
    private String extension;
    private String email;

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
}

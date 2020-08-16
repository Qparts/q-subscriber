package q.rest.subscriber.model.entity;

import q.rest.subscriber.model.entity.role.general.GeneralRole;
import q.rest.subscriber.model.entity.role.general.SubscriberGeneralRole;

import javax.persistence.*;
import java.io.Serializable;

@Table(name = "sub_company_label")
@Entity
@IdClass(CompanyLabel.CompanyLabelPK.class)
public class CompanyLabel {
    @Id
    @JoinColumn(name="label_id", referencedColumnName="id")
    @ManyToOne()
    private Label label;

    @Id
    @JoinColumn(name="company_id", referencedColumnName="id")
    @ManyToOne()
    private Company company;

    public Label getLabel() {
        return label;
    }

    public void setLabel(Label label) {
        this.label = label;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public static class CompanyLabelPK implements Serializable {

        protected int company;
        protected int label;

        public CompanyLabelPK() {}

        public int getCompany() {
            return company;
        }

        public void setCompany(int company) {
            this.company = company;
        }

        public int getLabel() {
            return label;
        }

        public void setLabel(int label) {
            this.label = label;
        }
    }
}

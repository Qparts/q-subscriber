package q.rest.subscriber.model.entity;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "sub_comment")
public class Comment {

    @Id
    @SequenceGenerator(name = "sub_comment_id_seq_gen", sequenceName = "sub_comment_id_seq", initialValue = 1, allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "sub_comment_id_seq_gen")
    private int id;
    @Column(name = "company_id")
    private int companyId;
    @Temporal(TemporalType.TIMESTAMP)
    private Date created;
    private int createdBy;
    private char status;
    @Column(name="comment_text")
    private String comment;

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

    public char getStatus() {
        return status;
    }

    public void setStatus(char status) {
        this.status = status;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}

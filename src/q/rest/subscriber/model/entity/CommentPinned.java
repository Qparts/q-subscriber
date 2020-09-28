package q.rest.subscriber.model.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "sub_comment_pin")
public class CommentPinned implements Serializable {

    @Id
    @SequenceGenerator(name = "sub_comment_pin_id_seq_gen", sequenceName = "sub_comment_pin_id_seq", initialValue = 1, allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "sub_comment_pin_id_seq_gen")
    private int id;
    @JoinColumn(name="comment_id", referencedColumnName = "id")
    @ManyToOne
    private Comment comment;
    private int pinnedBy;
    @Temporal(TemporalType.TIMESTAMP)
    private Date created;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Comment getComment() {
        return comment;
    }

    public void setComment(Comment comment) {
        this.comment = comment;
    }

    public int getPinnedBy() {
        return pinnedBy;
    }

    public void setPinnedBy(int pinnedBy) {
        this.pinnedBy = pinnedBy;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }
}

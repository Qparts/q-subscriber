package q.rest.subscriber.model.entity;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name="sub_label")
public class Label {
    @Id
    @SequenceGenerator(name = "sub_label_id_seq_gen", sequenceName = "sub_label_id_seq", initialValue=1, allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "sub_label_id_seq_gen")
    private int id;
    private String label;
    @Temporal(TemporalType.TIMESTAMP)
    private Date created;
    private int createdBy;
    private String color;

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
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
}

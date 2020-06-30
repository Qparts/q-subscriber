package q.rest.subscriber.model.entity.role.general;

import javax.persistence.*;
import java.util.Set;

@Entity
@Table(name="sub_general_role")
public class GeneralRole {
    @Id
    @SequenceGenerator(name = "sub_general_role_id_seq_gen", sequenceName = "sub_general_role_id_seq", initialValue=1, allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "sub_general_role_id_seq_gen")
    private int id;
    private String name;
    private String nameAr;
    private char status;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name="sub_general_role_activity",
            joinColumns = @JoinColumn(name="role_id"),
    inverseJoinColumns = @JoinColumn(name="activity_id"))
//    @OrderBy(value = "id")
    private Set<GeneralActivity> activities;


    public Set<GeneralActivity> getActivities() {
        return activities;
    }

    public void setActivities(Set<GeneralActivity> activities) {
        this.activities = activities;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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
}

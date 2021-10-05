package loader.entity;

import javax.persistence.*;

@Entity
@Table(
        name = "_field"
)
public class Field {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @Column(
            nullable = false
    )
    private String name;

    @Column(
            nullable = false
    )
    private String selector;

    @Column(
            nullable = false
    )
    private float weight;

    public Field(String name, String selector, float weight) {
        this.name = name;
        this.selector = selector;
        this.weight = weight;
    }

    public Field() {

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

    public String getSelector() {
        return selector;
    }

    public void setSelector(String selector) {
        this.selector = selector;
    }

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }

    @Override
    public String toString() {
        return "Field{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", selector='" + selector + '\'' +
                ", weight=" + weight +
                '}';
    }
}

package loader.entity;

import javax.persistence.*;

@Entity
@Table(
        name = "_page_index"
)
public class Index {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @Column(
            name = "page_id",
            nullable = false
    )
    private int pageId;

    @Column(
            name = "lemma_id",
            nullable = false
    )
    private int lemmaId;

    @Column(
            name = "lemma_rank",
            nullable = false
    )
    private float rank;

    public Index(int pageId, int lemmaId, float rank) {
        this.pageId = pageId;
        this.lemmaId = lemmaId;
        this.rank = rank;
    }

    public Index(){

    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPageId() {
        return pageId;
    }

    public void setPageId(int pageId) {
        this.pageId = pageId;
    }

    public int getLemmaId() {
        return lemmaId;
    }

    public void setLemmaId(int lemmaId) {
        this.lemmaId = lemmaId;
    }

    public float getRank() {
        return rank;
    }

    public void setRank(float rank) {
        this.rank = rank;
    }

    @Override
    public String toString() {
        return "Index{" +
                "id=" + id +
                ", pageId=" + pageId +
                ", lemmaId=" + lemmaId +
                ", rank=" + rank +
                '}';
    }
}

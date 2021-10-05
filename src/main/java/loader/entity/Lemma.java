package loader.entity;

import javax.persistence.*;

@Entity
@Table(
        name = "_lemma"
)
public class Lemma {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @Column(
            name = "site_id",
            nullable = false
    )
    private int siteId;

    @Column(
            nullable = false
    )
    private String lemma;

    @Column(
            nullable = false
    )
    private int frequency;

    public Lemma(int siteId, String lemma, int frequency) {
        this.siteId = siteId;
        this.lemma = lemma;
        this.frequency = frequency;
    }

    public Lemma() {

    }

    public int getId() {
        return id;
    }

    public int getSiteId() {
        return siteId;
    }

    public void setSiteId(int siteId) {
        this.siteId = siteId;
    }

    public String getLemma() {
        return lemma;
    }

    public void setLemma(String lemma) {
        this.lemma = lemma;
    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    public void incrementFrequency(int value){
        int currentFrequency = getFrequency();
        setFrequency(currentFrequency + value);
    }

    @Override
    public String toString() {
        return "Lemma{" +
                "id=" + id +
                ", siteId=" + siteId +
                ", lemma='" + lemma + '\'' +
                ", frequency=" + frequency +
                '}';
    }
}

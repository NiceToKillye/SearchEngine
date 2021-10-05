package loader.entity;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(
        name = "_site"
)
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false
    )
    private Status status;

    @Column(
            name = "status_time",
            nullable = false
    )
    private LocalDate statusTime;

    @Column(
            name = "last_error",
            columnDefinition = "TEXT"
    )
    private String lastError;

    @Column(
            name = "url",
            nullable = false
    )
    private String url;

    @Column(
            name = "name",
            nullable = false
    )
    private String name;

    public Site(Status status, LocalDate statusTime, String lastError, String url, String name) {
        this.status = status;
        this.statusTime = statusTime;
        this.lastError = lastError;
        this.url = url;
        this.name = name;
    }

    public Site() {
    }

    public int getId() {
        return id;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDate getStatusTime() {
        return statusTime;
    }

    public void setStatusTime(LocalDate statusTime) {
        this.statusTime = statusTime;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Site{" +
                "id=" + id +
                ", status=" + status +
                ", statusTime=" + statusTime +
                ", lastError='" + lastError + '\'' +
                ", url='" + url + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}

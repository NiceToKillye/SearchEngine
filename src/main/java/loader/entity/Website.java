package loader.entity;

import javax.persistence.*;
import javax.persistence.Index;

@Entity
@Table(
        name = "_page",
        indexes = @Index(columnList = "path", name = "pathIndex")
)
public class Website {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @Column(
            name = "site_id",
            nullable = false
    )
    private int siteId;

    @Column(
            name = "path",
            length = 200,
            nullable = false
    )
    private String path;

    @Column(
            name = "code",
            nullable = false
    )
    private int code;

    @Column(
            name = "content",
            columnDefinition = "MEDIUMTEXT",
            nullable = false
    )
    private String content;

    public Website(int siteId, String path, int code, String content) {
        this.siteId = siteId;
        this.path = path;
        this.code = code;
        this.content = content;
    }

    public Website() {
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

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "Website{" +
                "id=" + id +
                ", siteId=" + siteId +
                ", path='" + path + '\'' +
                ", code=" + code +
                ", content='" + content + '\'' +
                '}';
    }
}

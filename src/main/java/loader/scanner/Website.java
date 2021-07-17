package loader.scanner;

import javax.persistence.*;

@Entity
@Table(
        name = "page",
        indexes = @Index(columnList = "path", name = "pathIndex")
)
public class Website {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

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

    public Website(int id, String path, int code, String content) {
        this.id = id;
        this.path = path;
        this.code = code;
        this.content = content;
    }

    public Website(String path, int code, String content) {
        this.path = path;
        this.code = code;
        this.content = content;
    }

    public Website() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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
                ", path='" + path + '\'' +
                ", code=" + code +
                ", content='" + content + '\'' +
                '}';
    }
}

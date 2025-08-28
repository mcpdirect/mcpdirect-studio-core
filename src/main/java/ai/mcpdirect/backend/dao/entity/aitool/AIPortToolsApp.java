package ai.mcpdirect.backend.dao.entity.aitool;

//import appnet.hstp.labs.util.ID;
import com.fasterxml.jackson.annotation.JsonRawValue;

/**
 * Represents a tool application in the AI Port system.
 */
public class AIPortToolsApp {
    public long id;
    public String name;
    @JsonRawValue
    public String description;
    public String summary;
    public String developer;
    public String version;
    public int rating;

    public AIPortToolsApp() {}

    public AIPortToolsApp(String name, String description, String summary, String developer, String version) {
//        this.id = ID.nextId();
        this.name = name;
        this.description = description;
        this.summary = summary;
        this.developer = developer;
        this.version = version;
        this.rating = 0;
    }
}

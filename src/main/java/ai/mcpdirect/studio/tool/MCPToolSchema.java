//package ai.mcpdirect.studio.tool;
//
//import com.fasterxml.jackson.annotation.JsonProperty;
//import io.modelcontextprotocol.spec.McpSchema;
//
//import java.util.Map;
//
//public class MCPToolSchema{
//    public String serviceName;
//    public String servicePath;
//    @JsonProperty("name") String name;
//    @JsonProperty("title") String title;
//    @JsonProperty("description") String description;
//    @JsonProperty("inputSchema")
//    McpSchema.JsonSchema inputSchema;
//    @JsonProperty("outputSchema")
//    Map<String, Object> outputSchema;
//    @JsonProperty("annotations")
//    McpSchema.ToolAnnotations annotations;
//    @JsonProperty("_meta") Map<String, Object> meta;
//    public MCPToolSchema(){}
//    public MCPToolSchema(String serverName,String servicePath,McpSchema.Tool tool){
//        this.serviceName = serverName;
//        this.servicePath = servicePath;
//        name = tool.name();
//        title = tool.title();
//        description = tool.description();
//        inputSchema = tool.inputSchema();
//        outputSchema = tool.outputSchema();
//        annotations = tool.annotations();
//        meta = tool.meta();
//    }
//}

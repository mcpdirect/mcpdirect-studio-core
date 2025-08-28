package ai.mcpdirect.studio.tool;

import ai.mcpdirect.studio.tool.util.MCPToolProvider;
import appnet.hstp.engine.util.JSON;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.Map;

public class MCPTool implements AITool{
    private final MCPToolProvider provider;
    private final McpSchema.Tool tool;
    private final String inputSchema;
    public MCPTool(MCPToolProvider provider, McpSchema.Tool tool) {
        this.provider = provider;
        this.tool = tool;
        String s;
        try {
            s = JSON.toJson(tool.inputSchema());
        }catch (Exception e){
            s = "{}";
        }
        this.inputSchema = s;
    }
    public String name(){
        return tool.name();
    }
    public String description(){
        return tool.description();
    }
    public String inputSchema(){
        return inputSchema;
    }

    public String call(Map<String,Object> parameters){
        return provider.callTool(tool.name(),parameters);
    }
}

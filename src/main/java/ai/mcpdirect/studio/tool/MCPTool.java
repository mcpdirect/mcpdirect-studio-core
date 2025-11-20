package ai.mcpdirect.studio.tool;

import ai.mcpdirect.backend.dao.entity.aitool.AIPortTool;
import ai.mcpdirect.studio.tool.util.MCPToolProvider;
import appnet.hstp.ServiceDescription;
import appnet.hstp.engine.util.JSON;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.Map;

public class MCPTool extends AIPortTool implements AITool{
    @JsonIgnore
    private MCPToolProvider provider;
    @JsonIgnore
    private McpSchema.Tool tool;
    @JsonIgnore
    private String _metaData;
    public MCPTool(){
        lastUpdated = 1;
    }
    public void setMCPToolProvider(MCPToolProvider provider, McpSchema.Tool tool) {
        this.provider = provider;
        this.makerId = provider.id;
        this.tool = tool;
        this.name = tool.name();
        String inputSchema;
        try {
            inputSchema = JSON.toJson(tool.inputSchema());
        }catch (Exception e){
            inputSchema = "{}";
        }
        try {
            _metaData = JSON.toJson(new ServiceDescription("aitools",
                    "call/" + Long.toString(provider.id, Character.MAX_RADIX) + "/" + tool.name(),
                    tool.description(), inputSchema, "{}"));
            int hash = _metaData.hashCode();
            if (id > 0) {
                if (hash == this.hash) {
                    lastUpdated = 0;
                } else {
                    lastUpdated = System.currentTimeMillis();
                }
            } else {
                this.status = 1;
            }
            this.hash = hash;
        }catch (Exception ignore){
            _metaData = "{}";
        }
    }
    public String metaData(){
        return _metaData;
    }
    public void merge(AIPortTool tool){
        id=tool.id;
        makerId=tool.makerId;
        status=tool.status;
        name=tool.name;
        hash=tool.hash;
        tags=tool.tags;
        agentId=tool.agentId;
//        lastUpdated = -1;
        if(_metaData!=null) {
            int hash = _metaData.hashCode();
            if (id > 0) {
                if (hash == this.hash) {
                    lastUpdated = 0;
                } else {
                    lastUpdated = System.currentTimeMillis();
                }
            } else {
                this.status = 1;
            }
            this.hash = hash;
        }
    }

    public String name(){
        return tool!=null?tool.name():null;
    }
    public String description(){
        return tool!=null?tool.description():"";
    }
//    public String inputSchema(){
//        return inputSchema;
//    }

    public String call(Map<String,Object> parameters){
        return provider!=null?provider.callTool(tool.name(),parameters):null;
    }

    public static String buildCallResult(String content,boolean isError){
        return "{\"content\":[{\"type\":\"text\",\"text\":\"["+JSON.quote(content)+"],\"isError\":"+isError+"}";
    }
}

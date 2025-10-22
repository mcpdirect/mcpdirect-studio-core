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
//    private String inputSchema = "{}";
    public MCPTool(){
        lastUpdated = 1;
    }
    public void setMCPToolProvider(MCPToolProvider provider, McpSchema.Tool tool) {
        this.provider = provider;
        this.makerId = provider.id;
        this.tool = tool;
        String inputSchema;
        try {
            inputSchema = JSON.toJson(tool.inputSchema());
        }catch (Exception e){
            inputSchema = "{}";
        }
        this.name = tool.name();
        try {
            metaData = JSON.toJson(new ServiceDescription("aitools",
                    "call/" + Long.toString(provider.id, Character.MAX_RADIX) + "/" + tool.name(),
                    tool.description(), inputSchema, "{}"));
        }catch (Exception ignore){}
        if(id>0) {
            int hash = metaData.hashCode();
            if (hash == this.hash) {
                lastUpdated = 0;
            } else {
                lastUpdated = System.currentTimeMillis();
            }
        }else{
            this.status = 1;
        }
    }

    public void merge(AIPortTool tool){
        id=tool.id;
        makerId=tool.makerId;
        status=tool.status;
        name=tool.name;
        hash=tool.hash;
        tags=tool.tags;
        agentId=tool.agentId;
        lastUpdated = -1;
        if(this.tool!=null){
            setMCPToolProvider(this.provider,this.tool);
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
}

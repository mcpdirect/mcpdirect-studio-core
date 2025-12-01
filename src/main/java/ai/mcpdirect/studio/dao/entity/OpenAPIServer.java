package ai.mcpdirect.studio.dao.entity;

import ai.mcpdirect.backend.dao.entity.aitool.AIPortTool;
import ai.mcpdirect.backend.dao.entity.aitool.AIPortToolMaker;
import ai.mcpdirect.studio.tool.openapi.OpenAPITool;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OpenAPIServer extends AIPortToolMaker {
    @JsonProperty
    protected String statusMessage="";
    @JsonProperty
    public String url;
    @JsonProperty
    public Map<String, String> securities;
    @JsonIgnore
    protected final ConcurrentHashMap<String,OpenAPITool> tools = new ConcurrentHashMap<>();

    public OpenAPIServer(){
        type = TYPE_OPENAPI;
    }
    public Collection<? extends OpenAPITool> getTools() {
        return tools.values();
    }
    public void merge(AIPortToolMaker maker, List<AIPortTool> tools){
        if(maker!=null&&maker.type==TYPE_OPENAPI){
            id  = maker.id;
            name = maker.name;
            type = maker.type;
//            agentStatus = maker.agentStatus;
            agentId = maker.agentId;
            userId = maker.userId;
            teamId = maker.teamId;
            tags = maker.tags;
            status = maker.status;
            lastUpdated = maker.lastUpdated;
            created = maker.created;
            templateId = maker.templateId;
        }
        if(tools!=null) for (AIPortTool tool : tools) if(tool.makerId==id){
            OpenAPITool openapiTool = this.tools.get(tool.name);
            if(openapiTool==null){
                openapiTool = new OpenAPITool(tool.name,null,null);
                openapiTool.id = tool.id;
                this.tools.put(tool.name,openapiTool);
            }else openapiTool.merge(tool);
        }
    }

    public String statusMessage() {
        return statusMessage;
    }

    public static OpenAPIServer deprecated(long id){
        OpenAPIServer openAPIServer = new OpenAPIServer();
        openAPIServer.id = id;
        openAPIServer.status = STATUS_ABANDONED;
        return openAPIServer;
    }
}

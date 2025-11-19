package ai.mcpdirect.studio.tool.openapi;

import ai.mcpdirect.backend.dao.entity.aitool.AIPortTool;
import ai.mcpdirect.backend.dao.entity.aitool.AIPortToolMaker;
import ai.mcpdirect.studio.dao.entity.OpenAPIServer;
import ai.mcpdirect.studio.tool.AITool;
import ai.mcpdirect.studio.tool.MCPTool;
import ai.mcpdirect.studio.tool.util.AIToolProvider;
import appnet.hstp.labs.util.http.HttpClient;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.modelcontextprotocol.spec.McpSchema.ErrorCodes.INTERNAL_ERROR;
import static io.modelcontextprotocol.spec.McpSchema.ErrorCodes.METHOD_NOT_FOUND;


public class OpenAPIToolProvider extends OpenAPIServer implements AIToolProvider {
    private static final ObjectMapper mapper = new ObjectMapper();
    public static final int OPENAPI_DOC_NOT_EXIST = -1000;
    @JsonIgnore
    private final ConcurrentHashMap<String,OpenAPITool> tools = new ConcurrentHashMap<>();
    public OpenAPIToolProvider(){
        type = TYPE_OPENAPI;
    }
    private void parserOpenAPIYaml(String openApiYaml){
        SwaggerParseResult swaggerParseResult = new OpenAPIV3Parser().readContents(openApiYaml);
        OpenAPI openAPI = swaggerParseResult.getOpenAPI();
        Paths paths = openAPI.getPaths();
        for (Map.Entry<String, PathItem> e : paths.entrySet()) {
            String path = e.getKey();
            PathItem i = e.getValue();
            createTools("get",path,i.getGet());
            createTools("post",path,i.getPost());
            createTools("delete",path,i.getDelete());
            createTools("patch",path,i.getPatch());
            createTools("put",path,i.getPut());
//            Operation head = i.getHead();
//            Operation options = i.getOptions();
//            Operation trace = i.getTrace();
        }
    }
    private void createTools(String method,String path,Operation operation){
        if(operation==null){
            return;
        }
        String name = OpenAPITool.name(method,path);
        OpenAPITool tool = tools.computeIfAbsent(
                name,(key)-> new OpenAPITool(name,method,path)
        );
        tool.setOpenAPIToolProvider(this,operation);
    }
    public void config(OpenAPIServerConfig conf){
        if(conf!=null) {
            String doc = conf.doc;
            if(conf.docUri!=null) try{
                doc = HttpClient.doGet(conf.docUri);
            }catch (Exception e) {
                if(doc==null){
                    status = OPENAPI_DOC_NOT_EXIST;
                    statusMessage = e.getMessage();
                    return;
                }
            }
            parserOpenAPIYaml(doc);

            url = conf.url;
            securities = conf.securities;
            if(status!=conf.status) {
                status = conf.status;
                if (conf.status == 1) {
                    refreshTools();
                } else {
                    close();
                }
            }
        }
    }
    public void merge(AIPortToolMaker maker, List<AIPortTool> tools){
        if(maker.type!=TYPE_MCP){
            return;
        }
        id  = maker.id;
        name = maker.name;
        type = maker.type;
        agentStatus = maker.agentStatus;
        agentId = maker.agentId;
        userId = maker.userId;
        teamId = maker.teamId;
        tags = maker.tags;
        status = maker.status;
        lastUpdated = maker.lastUpdated;
        created = maker.created;
        templateId = maker.templateId;
        if(tools!=null) for (AIPortTool tool : tools) if(tool.makerId==id){
            OpenAPITool mcpTool = this.tools.get(tool.name);
            if(mcpTool==null){
                mcpTool = new OpenAPITool(tool.name,null,null);
                this.tools.put(tool.name,mcpTool);
            }else mcpTool.merge(tool);
        }
    }
    public void refreshTools() {
    }
    public void close(){

    }
    @Override
    public Collection<? extends OpenAPITool> getTools() {
        return tools.values();
    }

    @Override
    public AITool getTool(String name) {
        return tools.get(name);
    }

    @Override
    public String callTool(String name, Map<String, Object> parameters) {
        String result = "Error "+METHOD_NOT_FOUND;
        boolean error = true;
        AITool tool = getTool(name);
        if(tool!=null) try{
            result = tool.call(parameters);
            error = false;
        } catch (Throwable e) {
            status = STATUS_ERROR;
            result = "Error "+INTERNAL_ERROR+": " + statusMessage;
        }
        return MCPTool.buildCallResult(result,error);
    }

}

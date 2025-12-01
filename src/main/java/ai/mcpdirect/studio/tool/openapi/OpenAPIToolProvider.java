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
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.modelcontextprotocol.spec.McpSchema.ErrorCodes.INTERNAL_ERROR;
import static io.modelcontextprotocol.spec.McpSchema.ErrorCodes.METHOD_NOT_FOUND;


public class OpenAPIToolProvider extends OpenAPIServer implements AIToolProvider {
    private static final ObjectMapper mapper = new ObjectMapper();
    public static final int OPENAPI_DOC_NOT_EXIST = -1000;
    public List<OpenAPISecurity> openAPISecurities = new ArrayList<>();
    public OpenAPIToolProvider(){
        type = TYPE_OPENAPI;
    }
    private void parserOpenAPIYaml(String openApiYaml){
        SwaggerParseResult swaggerParseResult = new OpenAPIV3Parser().readContents(openApiYaml);
        OpenAPI openAPI = swaggerParseResult.getOpenAPI();
        Components components = openAPI.getComponents();
        Map<String,SecurityScheme> schemeMap=null;
        if(components!=null){
            schemeMap = components.getSecuritySchemes();
        }
        openAPISecurities.clear();
        if(schemeMap!=null&&securities!=null) for (Map.Entry<String, String> entry : securities.entrySet()) {
            SecurityScheme scheme = schemeMap.get(entry.getKey());
            if(scheme!=null){
                openAPISecurities.add(new OpenAPISecurity(scheme,entry.getValue()));
            }
        }
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
        String name = OpenAPITool.name(this.name,method,path);
        OpenAPITool tool = tools.computeIfAbsent(
                name,(key)-> new OpenAPITool(name,method,path)
        );
        tool.setOpenAPIToolProvider(this,operation);
    }
    public void config(OpenAPIServerConfig conf){
        if(conf!=null) {
            name = conf.name;
            url = conf.url;
            securities = conf.securities;
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
    public void refreshTools() {
    }
    public void close(){
        status=STATUS_ABANDONED;
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

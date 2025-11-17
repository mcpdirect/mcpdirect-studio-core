package ai.mcpdirect.studio.service;

import ai.mcpdirect.backend.dao.entity.aitool.AIPortTool;
import ai.mcpdirect.studio.MCPDirectStudio;
import ai.mcpdirect.studio.dao.entity.MCPServer;
import ai.mcpdirect.studio.dao.entity.OpenAPIServer;
import ai.mcpdirect.studio.tool.openapi.OpenAPIServerConfig;
import ai.mcpdirect.studio.tool.util.MCPServerConfig;
import appnet.hstp.SimpleServiceResponseMessage;
import appnet.hstp.annotation.*;
import appnet.hstp.engine.util.JSON;
import appnet.hstp.labs.util.http.HttpClient;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@ServiceName("studio.console")
@ServiceRequestMapping("/")
public class ConsoleServiceHandler {
    private ConsoleServiceHandler INSTANCE;
    public ConsoleServiceHandler getInstance(){
        return INSTANCE;
    }
    @ServiceRequestInit
    public void init(){
        INSTANCE = this;
    }
    public static class StudioToolMakers{
        public List<MCPServer> mcpServers;
        public List<OpenAPIServer> openapiServers;
    }
    @ServiceRequestMapping("tool_maker/query")
    public void queryToolMakers(
            @ServiceResponseMessage SimpleServiceResponseMessage<StudioToolMakers> resp
    ){
        StudioToolMakers studioToolMakers = new StudioToolMakers();
        studioToolMakers.mcpServers = List.copyOf(AIToolServiceHandler.getMCPServers());
        studioToolMakers.openapiServers = List.copyOf(AIToolServiceHandler.getOpenAPIServers());

        resp.success(studioToolMakers);
    }

    @ServiceRequestMapping("mcp_server/query")
    public void queryMCPServers(
            @ServiceResponseMessage SimpleServiceResponseMessage<List<MCPServer>> resp
    ){
        List<MCPServer> servers = List.copyOf(AIToolServiceHandler.getMCPServers());
        resp.success(servers);
    }

    public static class RequestOfConnectMCPServer{
        public Map<String, MCPServerConfig> mcpServerConfigs;
    }
    @ServiceRequestMapping("mcp_server/connect")
    public void connectMCPServers(
            @ServiceRequestMessage RequestOfConnectMCPServer req,
            @ServiceResponseMessage SimpleServiceResponseMessage<List<MCPServer>> resp
    ) throws Exception {
        if(req.mcpServerConfigs !=null&&!req.mcpServerConfigs.isEmpty()) {
            List<MCPServer> servers = new ArrayList<>();
            Map<String,String> errors = new HashMap<>();
            for (Map.Entry<String, MCPServerConfig> entry : req.mcpServerConfigs.entrySet()) {
                String name = entry.getKey();
                MCPServerConfig conf = entry.getValue();
                try {
                    MCPServer server = MCPDirectStudio.connectLocalMCPServer(name, conf);
                    if(server!=null) servers.add(server);
                    else errors.put(name,"MCP server exists");
//                    servers.add(MCPDirectStudio.connectMCPServer(name, conf));

                }catch (Exception e){
                    errors.put(name,e.getMessage());
                }
            }
            resp.success(servers);
            resp.message = JSON.toJson(errors);
        }
    }
    public static class RequestOfModifyMCPServer{
        public long mcpServerId;
        public String mcpServerName;
        public Integer mcpServerStatus;
        public MCPServerConfig mcpServerConfig;
    }
    @ServiceRequestMapping("mcp_server/modify")
    public void configMCPServer(
            @ServiceRequestMessage RequestOfModifyMCPServer req,
            @ServiceResponseMessage SimpleServiceResponseMessage<MCPServer> resp
    ) throws Exception {
        if(req.mcpServerId !=0) {
            MCPDirectStudio.modifyMCPServerConfig(
                    req.mcpServerId,req.mcpServerName,req.mcpServerStatus,req.mcpServerConfig,
                    (code, message, data)->{
                        resp.code = code;
                        resp.message = message;
                        resp.data = data;
                    }
            );
        }
    }
    @ServiceRequestMapping("mcp_server/remove")
    public void removeMCPServer(
            @ServiceRequestMessage RequestOfModifyMCPServer req,
            @ServiceResponseMessage SimpleServiceResponseMessage<MCPServer> resp
    ) throws Exception {
        if(req.mcpServerId !=0) {
            MCPDirectStudio.removeLocalMCPServer(req.mcpServerId,
                    (code,message,server)->{
                        resp.code = code;
                        resp.message = message;
                        resp.data = server;
                    });
        }
    }
    public static class RequestOfQueryMCPTools{
        public long mcpServerId;
    }
    @ServiceRequestMapping("mcp_server/tool/query")
    public void queryMCPTools(
            @ServiceRequestMessage RequestOfQueryMCPTools req,
            @ServiceResponseMessage SimpleServiceResponseMessage<List<AIPortTool>> resp
    ){
        if(req.mcpServerId !=0) {
            MCPServer mcpServer = AIToolServiceHandler.getMCPServer(req.mcpServerId);
            if(mcpServer!=null) resp.success(MCPDirectStudio.getAIPortTools(mcpServer));
        }
    }

    @ServiceRequestMapping("mcp_server/tool/publish")
    public void publicMCPTools(
            @ServiceRequestMessage RequestOfQueryMCPTools req,
            @ServiceResponseMessage SimpleServiceResponseMessage<MCPServer> resp
    ) throws Exception {
        if(req.mcpServerId !=0) {
            MCPServer mcpServer = AIToolServiceHandler.getMCPServer(req.mcpServerId);
            if(mcpServer!=null) MCPDirectStudio.publishTools(mcpServer,
                    (code,message,data)->{
               resp.code = code;
               resp.message = message;
               resp.data = data;
            });
        }
    }

    public static class RequestOfConnectToolMaker{
        public long makerId;
        public long agentId;
    }
    @ServiceRequestMapping("tool_maker/connect")
    public void connectToolMaker(
            @ServiceRequestMessage RequestOfConnectToolMaker req,
            @ServiceResponseMessage SimpleServiceResponseMessage<MCPServer> resp
    ) throws Exception {
        if(req.makerId>0&&req.agentId==MCPDirectStudio.studioToolAgentId()){
            MCPDirectStudio.connectToolMaker(req.makerId,(code,message,mcpServer)->{
                resp.code = code;
                resp.message = message;
                resp.data = mcpServer;
            });
        }
    }
    public static class RequestOfParseOpenAPIDoc{
        public String doc;
    }
    public static class OpenAPIServerDoc {
        public static class Security{
            public String description;
            public String key;
        }
        public static class Server{
            public String description;
            public String url;
        }
        public List<Server> servers;
        public Map<String,Security> securities;
        public void addServer(String description,String url){
            if(servers==null){
                servers = new ArrayList<>();
            }
            Server server = new Server();
            server.description = description;
            server.url = url;
            servers.add(server);
        }
        public void addSecurity(String description,String keyName){
            if(securities==null){
                securities = new HashMap<>();
            }
            Security security = new Security();
            security.description = description;
            securities.put(keyName,security);
        }
    }
    @ServiceRequestMapping("mcp_server/openapi/parse")
    public void parseOpenAPIDoc(
            @ServiceRequestMessage RequestOfParseOpenAPIDoc req,
            @ServiceResponseMessage SimpleServiceResponseMessage<OpenAPIServerDoc> resp
    ) throws Exception {
        if(req.doc!=null){
            OpenAPI openAPI;
            String doc = req.doc;
            if(req.doc.startsWith("http://")||req.doc.startsWith("https://")){
                doc = HttpClient.doGet(req.doc);
            }
            SwaggerParseResult swaggerParseResult = new OpenAPIV3Parser().readContents(doc);
            openAPI = swaggerParseResult.getOpenAPI();
            OpenAPIServerDoc form = new OpenAPIServerDoc();
            List<Server> servers = openAPI.getServers();
            if(servers!=null) {
                for (Server server :servers) {
                    AtomicReference<String> url = new AtomicReference<>(server.getUrl());
                    server.getVariables().forEach((k, v) -> {
                        String value = v.getDefault();
                        if (value == null && v.getEnum() != null) {
                            for (String s : v.getEnum()) {
                                value = s;
                                break;
                            }
                        }
                        if (value != null) {
                            url.set(url.get().replace("{" + k + "}", value));
                        }
                    });
                    form.addServer(server.getDescription(), url.get());
                }
            }
            List<SecurityRequirement> securities = openAPI.getSecurity();
            Components components = openAPI.getComponents();
            Map<String, SecurityScheme> schemes;
            if(securities!=null&&components!=null&&(schemes=components.getSecuritySchemes())!=null) {
                for (SecurityRequirement requirement : securities) {
                    for (String keyName : requirement.keySet()) {
                        SecurityScheme scheme = schemes.get(keyName);
                        if(scheme!=null){
                            form.addSecurity(scheme.getDescription(),keyName);
                        }
                    }
                }
            }
            resp.success(form);
        }
    }
    public static class RequestOfCreateOpenAPIServer{
        public String openAPIServerName;
        public OpenAPIServerConfig openAPIServerconfig;
    }
    @ServiceRequestMapping("mcp_server/openapi/connect")
    public void connectOpenAPIMCPServer(
            @ServiceRequestMessage RequestOfCreateOpenAPIServer req,
            @ServiceResponseMessage SimpleServiceResponseMessage<List<MCPServer>> resp
    ) throws Exception {
        String name = req.openAPIServerName;
        OpenAPIServerConfig config = req.openAPIServerconfig;
        if(name!=null&&config!=null&&(config.doc!=null||config.docUri!=null)){
            if(config.url==null){
                if(config.doc==null){
                    config.doc = HttpClient.doGet(config.docUri);
                }

            }
        }
    }
    @ServiceRequestMapping("mcp_server/openapi/query")
    public void queryOpenAPIServers(
            @ServiceResponseMessage SimpleServiceResponseMessage<List<OpenAPIServer>> resp
    ){
        List<OpenAPIServer> servers = List.copyOf(AIToolServiceHandler.getOpenAPIServers());
        resp.success(servers);
    }
}

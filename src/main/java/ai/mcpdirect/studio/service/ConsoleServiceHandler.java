package ai.mcpdirect.studio.service;

import ai.mcpdirect.backend.dao.entity.account.AIPortAccount;
import ai.mcpdirect.backend.dao.entity.aitool.AIPortTool;
import ai.mcpdirect.backend.dao.entity.aitool.AIPortToolMaker;
import ai.mcpdirect.backend.dao.entity.aitool.AIPortToolMakerTemplate;
import ai.mcpdirect.studio.MCPDirectStudio;
import ai.mcpdirect.studio.dao.entity.MCPServer;
import ai.mcpdirect.studio.dao.entity.OpenAPIServer;
import ai.mcpdirect.studio.dao.entity.ToolMakerTemplate;
import ai.mcpdirect.studio.dao.entity.ToolMakerTemplateConfig;
import ai.mcpdirect.studio.tool.MCPTool;
import ai.mcpdirect.studio.tool.openapi.OpenAPIServerConfig;
import ai.mcpdirect.studio.tool.openapi.OpenAPITool;
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
            @ServiceResponseMessage AIPortServiceResponse<StudioToolMakers> resp
    ){
        StudioToolMakers studioToolMakers = new StudioToolMakers();
        studioToolMakers.mcpServers = List.copyOf(AIToolServiceHandler.getMCPServers());
        studioToolMakers.openapiServers = List.copyOf(AIToolServiceHandler.getOpenAPIServers());

        resp.success(studioToolMakers);
    }

//    @ServiceRequestMapping("mcp_server/config/query")
//    public void queryMCPServers(
//            @ServiceResponseMessage AIPortServiceResponse<List<MCPServer>> resp
//    ){
//
//        List<MCPServer> servers = List.copyOf(AIToolServiceHandler.getMCPServers());
//        resp.success(servers);
//    }
    @ServiceRequestMapping("mcp_server/query")
    public void queryMCPServers(
            @ServiceResponseMessage AIPortServiceResponse<List<MCPServer>> resp
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
            @ServiceResponseMessage AIPortServiceResponse<List<MCPServer>> resp
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
            @ServiceResponseMessage AIPortServiceResponse<AIPortToolMaker> resp
    ) throws Exception {
        if(req.mcpServerId !=0) {
            MCPDirectStudio.modifyMCPServerConfig(
                    req.mcpServerId,req.mcpServerName,req.mcpServerStatus,req.mcpServerConfig,
                    (code, message, data)->{
                        resp.code(code);
                        resp.message = message;
                        resp.data = data;
                    }
            );
        }
    }
    @ServiceRequestMapping("mcp_server/remove")
    public void removeMCPServer(
            @ServiceRequestMessage RequestOfModifyMCPServer req,
            @ServiceResponseMessage AIPortServiceResponse<MCPServer> resp
    ) throws Exception {
        if(req.mcpServerId !=0) {
            MCPDirectStudio.removeLocalMCPServer(req.mcpServerId,
                    (code,message,server)->{
                        resp.code(code);
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
            @ServiceResponseMessage AIPortServiceResponse<List<AIPortTool>> resp
    ){
        if(req.mcpServerId !=0) {
            MCPServer mcpServer = AIToolServiceHandler.getMCPServer(req.mcpServerId);
            if(mcpServer!=null) resp.success(MCPDirectStudio.getAIPortTools(mcpServer));
        }
    }

    @ServiceRequestMapping("mcp_server/tool/publish")
    public void publicMCPTools(
            @ServiceRequestMessage RequestOfQueryMCPTools req,
            @ServiceResponseMessage AIPortServiceResponse<MCPServer> resp
    ) throws Exception {
        if(req.mcpServerId !=0) {
            MCPServer mcpServer = AIToolServiceHandler.getMCPServer(req.mcpServerId);
            if(mcpServer!=null) MCPDirectStudio.publishTools(mcpServer,
                    (code,message,data)->{
               resp.code(code);
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
            @ServiceResponseMessage AIPortServiceResponse<AIPortToolMaker> resp
    ) throws Exception {
        if(req.makerId>0&&req.agentId==MCPDirectStudio.studioToolAgentId()){
            MCPDirectStudio.connectToolMaker(req.makerId,(code,message,mcpServer)->{
                resp.code(code);
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
    @ServiceRequestMapping("openapi_server/doc/parse")
    public void parseOpenAPIDoc(
            @ServiceRequestMessage RequestOfParseOpenAPIDoc req,
            @ServiceResponseMessage AIPortServiceResponse<OpenAPIServerDoc> resp
    ) throws Exception {
        if(req.doc!=null){
            String doc = req.doc;
            if(req.doc.startsWith("http://")||req.doc.startsWith("https://")){
                doc = HttpClient.doGet(req.doc);
            }
            SwaggerParseResult swaggerParseResult = new OpenAPIV3Parser().readContents(doc);
            OpenAPI openAPI = swaggerParseResult.getOpenAPI();
            OpenAPIServerDoc serverDoc = new OpenAPIServerDoc();
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
                    serverDoc.addServer(server.getDescription(), url.get());
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
                            serverDoc.addSecurity(scheme.getDescription(),keyName);
                        }
                    }
                }
            }
            resp.success(serverDoc);
        }
    }
    private static String validateName(String name){
        if(name!=null&&!(name=name.trim()).isEmpty()){
            return name;
        }
        return null;
    }
    private static String validateUrl(String url){
        if(url!=null&&!(url=url.trim()).isEmpty()
                &&(url.startsWith("http://")||url.startsWith("https://"))){
            return url;
        }
        return null;
    }

    public static class RequestOfCreateOpenAPIServer{
        public String openAPIServerName;
        public OpenAPIServerConfig openAPIServerConfig;

        public boolean validate(){
            openAPIServerName = validateName(openAPIServerName);
            OpenAPIServerConfig config = openAPIServerConfig;
            return openAPIServerName!=null&&config!=null
                    &&(config.url=validateUrl(config.url))!=null
                    &&((config.docUri=validateUrl(config.docUri))!=null
                    ||config.doc!=null);
        }
    }
    @ServiceRequestMapping("openapi_server/connect")
    public void connectOpenAPIServer(
            @ServiceRequestMessage RequestOfCreateOpenAPIServer req,
            @ServiceResponseMessage AIPortServiceResponse<OpenAPIServer> resp
    ) throws Exception {
        if(req.validate()){
            resp.success(MCPDirectStudio.connectLocalOpenAPIServer(
                    req.openAPIServerName,req.openAPIServerConfig
            ));
        }
    }
    @ServiceRequestMapping("openapi_server/query")
    public void queryOpenAPIServers(
            @ServiceResponseMessage AIPortServiceResponse<List<OpenAPIServer>> resp
    ){
        List<OpenAPIServer> servers = List.copyOf(AIToolServiceHandler.getOpenAPIServers());
        resp.success(servers);
    }
    public static class RequestOfModifyOpenAPIServer{
        public long openapiServerId;
        public String openapiServerName;
        public Integer openapiServerStatus;
        public OpenAPIServerConfig openapiServerConfig;
    }
    @ServiceRequestMapping("openapi_server/modify")
    public void configOpenAPIServer(
            @ServiceRequestMessage RequestOfModifyOpenAPIServer req,
            @ServiceResponseMessage AIPortServiceResponse<OpenAPIServer> resp
    ) throws Exception {
        if(req.openapiServerId !=0) {
            MCPDirectStudio.modifyOpenAPIServerConfig(
                    req.openapiServerId,req.openapiServerName,
                    req.openapiServerStatus,req.openapiServerConfig,
                    (code, message, data)->{
                        resp.code(code);
                        resp.message = message;
                        resp.data = data;
                    }
            );
        }
    }
    @ServiceRequestMapping("openapi_server/remove")
    public void removeOpenAPIServer(
            @ServiceRequestMessage RequestOfModifyOpenAPIServer req,
            @ServiceResponseMessage AIPortServiceResponse<OpenAPIServer> resp
    ) throws Exception {
        if(req.openapiServerId !=0) {
            MCPDirectStudio.removeLocalOpenAPIServer(req.openapiServerId,
                    (code,message,server)->{
                        resp.code(code);
                        resp.message = message;
                        resp.data = server;
                    });
        }
    }
    public static class RequestOfQueryOpenAPITools{
        public long openapiServerId;
    }
    @ServiceRequestMapping("openapi_server/tool/query")
    public void queryOpenAPITools(
            @ServiceRequestMessage RequestOfQueryOpenAPITools req,
            @ServiceResponseMessage AIPortServiceResponse<List<AIPortTool>> resp
    ){
        if(req.openapiServerId !=0) {
            OpenAPIServer server = AIToolServiceHandler.getOpenAPIServer(req.openapiServerId);
            if(server!=null) resp.success(MCPDirectStudio.getAIPortTools(server));
        }
    }

    @ServiceRequestMapping("openapi_server/tool/publish")
    public void publicOpenAPITools(
            @ServiceRequestMessage RequestOfQueryOpenAPITools req,
            @ServiceResponseMessage AIPortServiceResponse<OpenAPIServer> resp
    ) throws Exception {
        if(req.openapiServerId !=0) {
            OpenAPIServer server = AIToolServiceHandler.getOpenAPIServer(req.openapiServerId);
            if(server!=null) MCPDirectStudio.publishTools(server,
                    (code,message,data)->{
                        resp.code(code);
                        resp.message = message;
                        resp.data = data;
                    });
        }
    }

    public static class RequestOfCreateToolMakerTemplate{
        public String name;
        public int type;
        public String config;
        public String inputs;
    }
    @ServiceRequestMapping("tool_maker/template/create")
    public void createToolMakerTemplate(
//            @ServiceRequestAuthentication("auk") AIPortAccount account,
            @ServiceRequestMessage RequestOfCreateToolMakerTemplate req,
            @ServiceResponseMessage AIPortServiceResponse<AIPortToolMakerTemplate> resp
    ) throws Exception {
        MCPDirectStudio.createToolMakerTemplate(
                req.name,req.type,req.config,req.inputs,
                (code,message,data)->{
                    resp.code(code);
                    resp.message = message;
                    resp.data = data;
                }
        );
    }

    public static class RequestOfConnectToolMakerTemplate{
        public long userId;
        public long templateId;
        public String name;
        public String inputs;
    }
    @ServiceRequestMapping("tool_maker_template/connect")
    public void connectToolMakerTemplate(
            @ServiceRequestMessage RequestOfConnectToolMakerTemplate req,
            @ServiceResponseMessage AIPortServiceResponse<AIPortToolMaker> resp
    ) throws Exception {
        MCPDirectStudio.connectToolMakerTemplate(
                req.userId, req.templateId, req.name,req.inputs,
                (code,message,data)->{
                    resp.code(code);
                    resp.message = message;
                    resp.data = data;
                }
        );
    }

    public static class RequestOfGetToolMakerTemplate{
        public long templateId;
    }
    @ServiceRequestMapping("tool_maker_template/get")
    public void getToolMakerTemplate(
            @ServiceRequestMessage RequestOfGetToolMakerTemplate req,
            @ServiceResponseMessage AIPortServiceResponse<ToolMakerTemplate> resp
    ) throws Exception {
        ToolMakerTemplate template = MCPDirectStudio.getToolMakerTemplate(req.templateId);
        if(template!=null){
            resp.success(template);
        }
    }

    public static class RequestOfGetToolMakerTemplateConfig{
        public long toolMakerId;
    }
    @ServiceRequestMapping("tool_maker_template/config/get")
    public void getToolMakerTemplateConfig(
            @ServiceRequestMessage RequestOfGetToolMakerTemplateConfig req,
            @ServiceResponseMessage AIPortServiceResponse<ToolMakerTemplateConfig> resp
    ) throws Exception {
        ToolMakerTemplateConfig template = MCPDirectStudio.getToolMakerTemplateConfig(req.toolMakerId);
        if(template!=null){
            resp.success(template);
        }
    }

    public static class RequestOfModifyToolMakerTemplateConfig{
        public long toolMakerId;
        public String inputs;
    }
    @ServiceRequestMapping("tool_maker_template/config/modify")
    public void modifyToolMakerTemplateConfig(
            @ServiceRequestMessage RequestOfModifyToolMakerTemplateConfig req,
            @ServiceResponseMessage AIPortServiceResponse<AIPortToolMaker> resp
    ) throws Exception {
        MCPDirectStudio.modifyToolMakerTemplateConfig(
                req.toolMakerId,req.inputs,
                (code,message,data)->{
                    resp.code(code);
                    resp.message = message;
                    resp.data = data;
                }
        );
    }

    public static class RequestOfGetTool{
        public long toolMakerId;
        public long toolMakerType;
        public String toolName;
    }
    @ServiceRequestMapping("tool/get")
    public void getTool(
            @ServiceRequestMessage RequestOfGetTool req,
            @ServiceResponseMessage AIPortServiceResponse<AIPortTool> resp
    ) throws Exception {
        AIPortTool aiPortTool=null;
        if (req.toolMakerType==AIPortToolMaker.TYPE_MCP){
            MCPServer mcpServer = AIToolServiceHandler.getMCPServer(req.toolMakerId);
            MCPTool tool;
            if(mcpServer!=null&&(tool=mcpServer.getTool(req.toolName))!=null) {
                aiPortTool = tool.duplicate();
                aiPortTool.metaData = tool.metaData();
            }
        }else if(req.toolMakerType==AIPortToolMaker.TYPE_OPENAPI){
            OpenAPIServer openAPIServer = AIToolServiceHandler.getOpenAPIServer(req.toolMakerId);
            OpenAPITool tool;
            if(openAPIServer!=null&&(tool=openAPIServer.getTool(req.toolName))!=null){
                aiPortTool = tool.duplicate();
                aiPortTool.metaData = tool.metaData();
            }
        }
        if(aiPortTool!=null){
            resp.success(aiPortTool);
        }
    }
}

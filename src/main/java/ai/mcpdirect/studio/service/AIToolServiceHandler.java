package ai.mcpdirect.studio.service;

import ai.mcpdirect.studio.dao.entity.OpenAPIServer;
import ai.mcpdirect.studio.tool.MCPTool;
import ai.mcpdirect.studio.tool.openapi.OpenAPIServerConfig;
import ai.mcpdirect.studio.tool.openapi.OpenAPIToolProvider;
import ai.mcpdirect.studio.tool.util.AIToolProvider;
import ai.mcpdirect.studio.tool.util.MCPServerConfig;
import appnet.hstp.ServiceEngine;
import appnet.hstp.ServiceRequest;
import appnet.hstp.annotation.*;
import ai.mcpdirect.backend.dao.entity.account.AIPortAccessKeyCredential;
import ai.mcpdirect.studio.MCPDirectStudio;
import ai.mcpdirect.studio.exception.MCPServerException;
import ai.mcpdirect.studio.dao.entity.MCPServer;
import ai.mcpdirect.studio.tool.AITool;
import ai.mcpdirect.studio.tool.util.MCPToolProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.http.HttpRequest;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.modelcontextprotocol.spec.McpSchema.ErrorCodes.INTERNAL_ERROR;
import static io.modelcontextprotocol.spec.McpSchema.ErrorCodes.METHOD_NOT_FOUND;

@ServiceName("aitools")
@ServiceRequestMapping("/")
public class AIToolServiceHandler {
    private static final Logger LOG = LoggerFactory.getLogger(AIToolServiceHandler.class);
    private static final Map<String, MCPToolProvider> mcpToolsProviders = new ConcurrentHashMap<>();
    private static final Map<String, OpenAPIToolProvider> openapiToolsProviders = new ConcurrentHashMap<>();

    public static Collection<? extends MCPServer> getMCPServers(){
        return mcpToolsProviders.values();
    }
    public static MCPServer getMCPServer(long serverId){
        return mcpToolsProviders.get(Long.toString(serverId,Character.MAX_RADIX));
    }
    public static void stopMCPServer(long serverId){
        MCPToolProvider provider = mcpToolsProviders.get(Long.toString(serverId, Character.MAX_RADIX));
        if(provider!=null){
            provider.close();
        }
    }
    public static void startMCPServer(long serverId){
        MCPToolProvider provider = mcpToolsProviders.get(Long.toString(serverId, Character.MAX_RADIX));
        if(provider!=null){
            provider.createMcpSyncClient();
            provider.refreshTools();
        }
    }
    public static synchronized MCPServer connectMCPServer(long serverId, String serverName, MCPServerConfig conf)
            throws MCPServerException, MalformedURLException {
        String serverKey = Long.toString(serverId,Character.MAX_RADIX);
        MCPToolProvider provider = mcpToolsProviders.get(serverKey);
        if(provider!=null){
            if(conf!=null&&(conf.url==null||(conf.url=conf.url.trim()).isEmpty())
                    &&(conf.command==null||(conf.command=conf.command.trim()).isEmpty())){
                throw new MCPServerException("Server URL and command must not be empty both");
            }
            provider.name = serverName;
            if(conf!=null) {
                provider.transport = conf.transport;
                provider.url = conf.url;
                provider.command = conf.command;
                provider.args = conf.args;
                provider.env = conf.env;
                if(provider.status!=conf.status) {
                    provider.status = conf.status;
                    if (conf.status == 1) {
                        provider.createMcpSyncClient();
                        provider.refreshTools();
                    } else {
                        provider.close();
                    }
                }
            }
            return provider;
        }
        if(conf==null||((conf.url==null||(conf.url=conf.url.trim()).isEmpty())
                &&(conf.command==null||(conf.command=conf.command.trim()).isEmpty()))){
            throw new MCPServerException("Server URL and command must not be empty both");
        }
        if(conf.command!=null&&!conf.command.isEmpty()) {
            provider = new MCPToolProvider(
                    "MCPDirectStudio","1.0.0",
                    null,null,conf
            );
        }else{
            String baseUrl;
            String sseEndpoint;

            java.net.URL parsedUrl = new java.net.URL(conf.url);
            baseUrl = parsedUrl.getProtocol() + "://" + parsedUrl.getHost()
                    + (parsedUrl.getPort() == -1 ? "" : ":" + parsedUrl.getPort());

            sseEndpoint = parsedUrl.getPath();

            if (sseEndpoint.startsWith("/")) {
                sseEndpoint = sseEndpoint.substring(1);
            }
            provider = new MCPToolProvider(
                    "MCPDirectStudio","1.0.0",
                    baseUrl,sseEndpoint,conf
            );
            HttpRequest.Builder builder = HttpRequest.newBuilder();
            builder.header("Content-Type", "application/json");
            if(conf.env!=null) for (Map.Entry<String, String> entry : conf.env.entrySet()) {
                builder.header(entry.getKey(),entry.getValue());
            }
        }
        provider.id = serverId;
        provider.name = serverName;
        provider.agentId = MCPDirectStudio.studioToolAgentId();
        if(conf.status==1) {
            provider.createMcpSyncClient();
            provider.refreshTools();
        }
        mcpToolsProviders.put(serverKey, provider);
        return provider;
    }
    public static MCPServer removeMCPServer(long serverId){
        MCPToolProvider server =  mcpToolsProviders.remove(Long.toString(serverId,Character.MAX_RADIX));
        server.close();
        return server;
    }
    public static void remapMCPServer(long makerId){
        MCPToolProvider maker = mcpToolsProviders.remove(Long.toString(makerId,Character.MAX_RADIX));
        if(maker!=null){
            mcpToolsProviders.put(Long.toString(maker.id,Character.MAX_RADIX),maker);
        }
    }

    public static OpenAPIServer connectOpenAPIServer(
            long serverId, String serverName,
            OpenAPIServerConfig conf) throws Exception {
        String serverKey = Long.toString(serverId,Character.MAX_RADIX);
        OpenAPIToolProvider provider = openapiToolsProviders.get(serverKey);
        if(provider!=null){
            provider.name = serverName;
        }else {
            provider = new OpenAPIToolProvider();
            provider.id = serverId;
            provider.name = serverName;
            provider.agentId = MCPDirectStudio.studioToolAgentId();
            openapiToolsProviders.put(serverKey,provider);
        }
        provider.config(conf);
        return provider;
    }
    public static Collection<? extends OpenAPIServer> getOpenAPIServers(){
        return openapiToolsProviders.values();
    }
    public static OpenAPIServer getOpenAPIServer(long serverId){
        return openapiToolsProviders.get(Long.toString(serverId,Character.MAX_RADIX));
    }
    public static void remapOpenAPIServer(long serverId){
        OpenAPIToolProvider maker = openapiToolsProviders.remove(Long.toString(serverId,Character.MAX_RADIX));
        if(maker!=null){
            openapiToolsProviders.put(Long.toString(maker.id,Character.MAX_RADIX),maker);
        }
    }
    private static ServiceEngine serviceEngine;

    @ServiceRequestInit
    public void init(ServiceEngine engine){
        serviceEngine = engine;
    }
    @ServiceRequestAuthentication
    public AIPortAccessKeyCredential authenticate(
            ServiceRequest request, Class<?> authObjectType,
            int[] authRoles, boolean anonymous) throws Exception {
        String keyId = request.getRequestHeaders().getHeader("X-MCPdirect-Key-ID");
        return MCPDirectStudio.getAccessKeyCredential(keyId);
    }
    public static class ResponseOfAIService<T> {
        public String status = "failed";
        public String message;
        public T data;

        public void success(T data) {
            this.data = data;
            this.success();
        }

        public void success() {
            this.status = "ok";
        }
    }
    @ServiceRequestMapping("call/**")
    public void callTool(
            ServiceRequest sreq,
//            @ServiceRequestAuthentication AIPortAccessKeyCredential key,
            @ServiceRequestHeader("X-MCP-Client-Name") String clientName,
            @ServiceRequestMessage Map<String,Object> parameters,
            @ServiceResponseMessage ResponseOfAIService<String> resp
    ){

        if(MCPDirectStudio.getAccount()==null){
            resp.success( "The tool is not ready yet. Please try again later.");
            return;
        }
        String[] paths = sreq.getUSL().getPath().split("/");
        int p = -1;
        for (int i = 0; i < paths.length; i++) {
            if(paths[i].equals("call")){
                p = i+1;
                break;
            }
        }
        if(p>-1&&paths.length>=(p+2)){
            boolean isOpenAPITool=false;
            if(paths[p].equals("openapi")){
                isOpenAPITool = true;
                p++;
            }
            AITool tool;
            String result;
            String providerName = paths[p++];
            AIToolProvider provider;
            if(isOpenAPITool) provider = openapiToolsProviders.get(providerName);
            else provider = mcpToolsProviders.get(providerName);
            String toolName = paths[p];
            if(provider!=null&&(tool=provider.getTool(toolName))!=null){
                try {
                    result = tool.call(parameters);
                } catch (Exception e) {
                    result = MCPTool.buildCallResult("Error "+INTERNAL_ERROR+": "+ e.getMessage(),true);
                }

            }else{
                result = MCPTool.buildCallResult("Error "+METHOD_NOT_FOUND,true);
            }
            System.err.println(result);
            resp.success(result);
//            String finalResult = result;
//            new Thread(()->{
//                MCPDirectStudio.logTool(key,clientName!=null?clientName:key.name,providerName,toolName,parameters, finalResult);
//            }).start();
        }
    }
}

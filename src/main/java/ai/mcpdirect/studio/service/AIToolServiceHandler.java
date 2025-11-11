package ai.mcpdirect.studio.service;

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

@ServiceName("aitools")
@ServiceRequestMapping("/")
public class AIToolServiceHandler {
    private static final Logger LOG = LoggerFactory.getLogger(AIToolServiceHandler.class);
    private static final Map<String, MCPToolProvider> mcpToolsProviders = new ConcurrentHashMap<>();

    public static Collection<? extends MCPServer> getMCPServers(){
        return mcpToolsProviders.values();
    }
    public static MCPServer getMCPServer(long serverId){
        return mcpToolsProviders.get(Long.toString(serverId,Character.MAX_RADIX));
    }
//    public static List<MCPServer> addMCPServer(String json) throws Exception {
//        List<MCPServer> list = new ArrayList<>();
//        Map<String,Map<String, MCPServerConfig>> config = JSON.fromJson(json, new TypeReference<>() {
//        });
//        Map<String, MCPServerConfig> mcpServerConfigs = config.get("mcpServers");
//        if(mcpServerConfigs!=null) for (Map.Entry<String, MCPServerConfig> entry
//                : mcpServerConfigs.entrySet()) {
//            String serverId = entry.getKey();
//            MCPServerConfig value = entry.getValue();
//            try {
//                list.add(addMCPServer(serverId, value.url, value.command, value.args, value.env));
//            }catch (Exception ignore){}
//        }
//        return list;
//    }
//    public static synchronized MCPServer addMCPServer(String serverId,int serverType,String url,String command,
//                                    List<String> args, Map<String, String> env)
    public static synchronized MCPServer connectMCPServer(long serverId, String serverName,
                                                          MCPServerConfig conf)
            throws MCPServerException, MalformedURLException {
//        if(serverId==null||(serverId=serverId.trim()).isEmpty()){
//            throw new MCPServerException("Server Name must not be empty");
//        }
        if((conf.url==null||(conf.url=conf.url.trim()).isEmpty())
                &&(conf.command==null||(conf.command=conf.command.trim()).isEmpty())){
            throw new MCPServerException("Server URL and command must not be empty both");
        }
        String serverKey = Long.toString(serverId,Character.MAX_RADIX);
        MCPToolProvider provider = mcpToolsProviders.get(serverKey);
        if(provider!=null){
            provider.name = serverName;
            provider.transport = conf.transport;
            provider.url = conf.url;
            provider.command = conf.command;
            provider.args = conf.args;
            provider.env = conf.env;
            provider.createMcpSyncClient();
            provider.refreshTools();
            return provider;
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
        provider.refreshTools();
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
            AITool tool;
            String result;
            String providerName = paths[p++];
            MCPToolProvider provider = mcpToolsProviders.get(providerName);
            String toolName = paths[p];
            if(provider!=null&&(tool=provider.getTool(toolName))!=null){
                try {
                    result = tool.call(parameters);
                } catch (Exception e) {
                    result = "The tool throws an exception \""+e+"\". Please tell user to check";
                }

            }else{
                result = "The tool was abandoned. Please tell user to check.";
            }
            resp.success(result);
//            String finalResult = result;
//            new Thread(()->{
//                MCPDirectStudio.logTool(key,clientName!=null?clientName:key.name,providerName,toolName,parameters, finalResult);
//            }).start();
        }
    }
}

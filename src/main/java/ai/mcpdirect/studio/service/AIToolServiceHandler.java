package ai.mcpdirect.studio.service;

import appnet.hstp.ServiceEngine;
import appnet.hstp.ServiceRequest;
import appnet.hstp.annotation.*;
import ai.mcpdirect.backend.dao.entity.account.AIPortAccessKeyCredential;
import ai.mcpdirect.studio.MCPDirectStudio;
import ai.mcpdirect.studio.exception.MCPServerException;
import ai.mcpdirect.studio.dao.entity.MCPServer;
import ai.mcpdirect.studio.tool.AITool;
import ai.mcpdirect.studio.tool.util.MCPServerConfig;
import ai.mcpdirect.studio.tool.util.MCPToolProvider;
import appnet.hstp.engine.util.JSON;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
    public static MCPServer getMCPServer(String serverName){
        return mcpToolsProviders.get(serverName);
    }
    public static List<MCPServer> addMCPServer(String json) throws Exception {
        List<MCPServer> list = new ArrayList<>();
        Map<String,Map<String, MCPServerConfig>> config = JSON.fromJson(json, new TypeReference<>() {
        });
        Map<String, MCPServerConfig> mcpServerConfigs = config.get("mcpServers");
        if(mcpServerConfigs!=null) for (Map.Entry<String, MCPServerConfig> entry
                : mcpServerConfigs.entrySet()) {
            String serverName = entry.getKey();
            MCPServerConfig value = entry.getValue();
            try {
                list.add(addMCPServer(serverName, value.url, value.command, value.args, value.env));
            }catch (Exception ignore){}
        }
        return list;
    }
    public static synchronized MCPServer addMCPServer(String serverName, String url,String command,
                                    List<String> args, Map<String, String> env)
            throws MCPServerException, MalformedURLException {
        if(serverName==null||(serverName=serverName.trim()).isEmpty()){
            throw new MCPServerException("Server Name must not be empty");
        }
        if((url==null||(url=url.trim()).isEmpty())&&(command==null||(command=command.trim()).isEmpty())){
            throw new MCPServerException("Server URL and command must not be empty both");
        }
        MCPToolProvider provider = mcpToolsProviders.get(serverName);
        if(provider!=null){
            return provider;
        }
        if(command!=null&&!command.isEmpty()) {
            provider = new MCPToolProvider(
                    "MCPDirectStudio#"+serviceEngine.getEngineId(),"1.0.0",
                    null,null,command,args,env,serverName
            );
        }else{
            String baseUrl;
            String sseEndpoint;
            java.net.URL parsedUrl = new java.net.URL(url);
            baseUrl = parsedUrl.getProtocol() + "://" + parsedUrl.getHost()
                    + (parsedUrl.getPort() == -1 ? "" : ":" + parsedUrl.getPort());

            String path = parsedUrl.getPath();
            if (path == null || !path.endsWith("/sse")) {
                throw new IllegalArgumentException(
                        "URL path must end with /sse, current path: " + path + " for server: " + serverName);
            }

            sseEndpoint = path;

            if (sseEndpoint.startsWith("/")) {
                sseEndpoint = sseEndpoint.substring(1);
            }
            provider = new MCPToolProvider(
                    "MCPDirectStudio#"+serviceEngine.getEngineId(),"1.0.0",
                    baseUrl,sseEndpoint,null,null,env,serverName
            );
            HttpRequest.Builder builder = HttpRequest.newBuilder();
            builder.header("Content-Type", "application/json");
            if(env!=null) for (Map.Entry<String, String> entry : env.entrySet()) {
                builder.header(entry.getKey(),entry.getValue());
            }
        }
        provider.refreshTools();
        mcpToolsProviders.put(provider.name,provider);
        return provider;
    }
    public static MCPServer remoteMCPServer(String name){
        return mcpToolsProviders.remove(name);
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
            @ServiceRequestAuthentication AIPortAccessKeyCredential key,
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
            String finalResult = result;
            new Thread(()->{
                MCPDirectStudio.logTool(key,clientName!=null?clientName:key.name,providerName,toolName,parameters, finalResult);
            }).start();
        }
    }


}

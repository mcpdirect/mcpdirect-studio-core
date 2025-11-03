package ai.mcpdirect.studio.service;

import ai.mcpdirect.backend.dao.entity.aitool.AIPortMCPServerConfig;
import ai.mcpdirect.backend.dao.entity.aitool.AIPortTool;
import ai.mcpdirect.backend.dao.entity.aitool.AIPortToolMaker;
import ai.mcpdirect.studio.MCPDirectStudio;
import ai.mcpdirect.studio.dao.entity.MCPServer;
import ai.mcpdirect.studio.tool.util.MCPServerConfig;
import appnet.hstp.SimpleServiceResponseMessage;
import appnet.hstp.annotation.*;
import appnet.hstp.engine.util.JSON;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    @ServiceRequestMapping("mcp_server/query")
    public void queryMCPServers(
            @ServiceResponseMessage SimpleServiceResponseMessage<List<MCPServer>> resp
    ){
        List<MCPServer> servers = new ArrayList<>(AIToolServiceHandler.getMCPServers());
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
                    servers.add(MCPDirectStudio.connectMCPServer(name, conf));

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
        public MCPServerConfig mcpServerConfig;
    }
    @ServiceRequestMapping("mcp_server/config")
    public void configMCPServer(
            @ServiceRequestMessage RequestOfModifyMCPServer req,
            @ServiceResponseMessage SimpleServiceResponseMessage<MCPServer> resp
    ) throws Exception {
        MCPServerConfig conf;
        if(req.mcpServerId !=0&&(conf=req.mcpServerConfig)!=null) {
            MCPDirectStudio.configMCPServerConfig(
                    req.mcpServerId,conf, (code, message, data)->{
                        resp.code = code;
                        resp.message = message;
                        resp.data = data;
                    }
            );
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
            @ServiceResponseMessage SimpleServiceResponseMessage<Boolean> resp
    ) throws Exception {
        if(req.makerId>0&&req.agentId==MCPDirectStudio.studioToolAgentId()){
            new Thread(()->{
                MCPDirectStudio.connectToolMaker(req.makerId);
            }).start();
            resp.success(true);
        }
    }
}

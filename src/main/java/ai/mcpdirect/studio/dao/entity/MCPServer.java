/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.mcpdirect.studio.dao.entity;

import ai.mcpdirect.backend.dao.entity.aitool.AIPortTool;
import ai.mcpdirect.backend.dao.entity.aitool.AIPortToolMaker;
import ai.mcpdirect.studio.tool.MCPTool;
import ai.mcpdirect.studio.tool.util.MCPServerConfig;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP server configuration parsing object for parsing configuration format like: {
 * "mcpServers": { "server-name": { "url": "http://localhost:3000/sse", "headers": {
 * "Authentication": "" } } } }
 * For STUDIO type connection. Supports the
 * following two formats: { "mcpServers": { "server-name": { "command": "npx", "args":
 * ["-y", "mcp-server"], "env": { "API_KEY": "value" } } } }
 * Or:
 * { "mcpServers": { "server-name": { "command": "python", "args": ["mcp-server.py"],
 * "env": { "API_KEY": "value" } } } }
 */
public class MCPServer extends AIPortToolMaker {
    @JsonIgnore
	protected final Map<String, MCPTool> tools = new ConcurrentHashMap<>();
    public int errorCode;
    public String errorMessage ="";
    public int transport;
    public String url;
    public String command;
    public List<String> args;
    public Map<String, String> env ;
	public MCPServer(long id) {
        this.id = id;
        type = TYPE_MCP;
        status = STATUS_WAITING;
	}
    public MCPServer(MCPServerConfig config){
        type = TYPE_MCP;
        transport = config.transport;
        url = config.url;
        command = config.command;
        args = config.args;
        env = config.env;
    }
    public void merge(AIPortToolMaker maker, List<AIPortTool> tools){
        if(maker.type!=TYPE_MCP){
            return;
        }
        id  = maker.id;
        name = maker.name;
        type = maker.type;
//        agentStatus = maker.agentStatus;
        agentId = maker.agentId;
        userId = maker.userId;
        templateId = maker.templateId;
        tags = maker.tags;
        status = maker.status;
        lastUpdated = maker.lastUpdated;
        created = maker.created;
//        templateId = maker.templateId;
        for (MCPTool tool : this.tools.values()) {
            tool.rebuildMetaData();
        }
        if(tools!=null) for (AIPortTool tool : tools) if(tool.makerId==id){
            MCPTool mcpTool = this.tools.get(tool.name);
//            if(mcpTool==null){
//                mcpTool = new MCPTool();
//                mcpTool.name = tool.name;
//                mcpTool.id = tool.id;
//                mcpTool.makerId = tool.makerId;
//                mcpTool.agentId = tool.agentId;
//                mcpTool.status = -1;
//                mcpTool.lastUpdated = -1;
//                this.tools.put(tool.name,mcpTool);
//            }
            if(mcpTool!=null) mcpTool.merge(tool);
        }
    }
	public int status(){
		return status;
	}
//    public String statusMessage(){
//        return errorMessage;
//    }

	public Collection<? extends MCPTool> getTools(){
		return tools.values();
	}

	public MCPTool getTool(String name) {
		return tools.get(name);
	}

	public void refreshTools(){

	}

	public String callTool(String name,Map<String,Object> parameters){
		return null;
	}

    public static MCPServer deprecated(long id){
        MCPServer server = new MCPServer(id);
        server.id = id;
        server.status = STATUS_ABANDONED;
        return server;
    }
}

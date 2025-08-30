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

import ai.mcpdirect.studio.tool.AITool;
import ai.mcpdirect.studio.tool.MCPTool;
import ai.mcpdirect.studio.tool.util.MCPServerConfig;

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
public class MCPServer extends MCPServerConfig {
	public long id;
	public final String name;
//	public boolean updatable;
	protected final Map<String, MCPTool> tools = new ConcurrentHashMap<>();
	public static int STATUS_ON = 1;
	public static int STATUS_OFF = 0;
	public static int STATUS_ERROR = -1;
	protected int status;
    protected String statusMessage;
	public MCPServer(String url, String command, List<String> args, Map<String, String> env, String serverName) {
		super(url, command, args, env);
		this.name = serverName;
	}

	public int status(){
		return status;
	}
    public String statusMessage(){
        return statusMessage;
    }

	public Collection<? extends AITool> getTools(){
		return tools.values();
	}

	public AITool getTool(String name) {
		return tools.get(name);
	}

	public void refreshTools(){

	}

	public String callTool(String name,Map<String,Object> parameters){
		return null;
	}

}

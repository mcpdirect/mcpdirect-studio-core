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
package ai.mcpdirect.studio.tool.util;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

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
public class MCPServerConfig {

	@JsonProperty
	public String url;
	@JsonProperty
	public String command;
	@JsonProperty
	public List<String> args;
	@JsonProperty
	public Map<String, String> env ;
	public MCPServerConfig(){}
	public MCPServerConfig(String url, String command, List<String> args, Map<String, String> env) {
		this.url = url;
		this.command = command;
		this.args = args;
		this.env = env;
	}
}

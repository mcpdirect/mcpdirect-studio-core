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

//import ai.mcpdirect.backend.dao.entity.aitool.AIPortMCPServerConfig;
import appnet.hstp.engine.util.JSON;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.ArrayList;
import java.util.HashMap;
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
    public long id;
    @JsonProperty
    public String name;
    @JsonProperty
    public int status = 1;
    @JsonProperty
    public int transport;
	@JsonProperty
	public String url;
	@JsonProperty
	public String command;
	@JsonProperty
	public List<String> args;
	@JsonProperty
	public Map<String, String> env ;
	public MCPServerConfig(){}
//	public MCPServerConfig(int transport,String url, String command, List<String> args, Map<String, String> env) {
//        this.transport = transport;
//		this.url = url;
//		this.command = command;
//		this.args = args;
//		this.env = env;
//	}
//    public MCPServerConfig(int status,AIPortMCPServerConfig c) throws Exception {
//        this.status = status;
//        transport = c.transport;
//        url = fillInputs(c.url,c.inputs);
//        command = fillInputs(c.command,c.inputs);
//        args = JSON.fromJson(fillInputs(c.args,c.inputs), new TypeReference<>() {});
//        env = JSON.fromJson(fillInputs(c.env,c.inputs), new TypeReference<>() {});
//    }
    public void fillInputs(String inputs) throws Exception {
        Map<String,String> inputMap;
        if(inputs!=null&&!(inputs=inputs.trim()).isEmpty()
                && !(inputMap = JSON.fromJson(inputs, new TypeReference<>() {
        })).isEmpty()){
            url = fillInputs(url,inputMap);
            command = fillInputs(command,inputMap);
            if(args!=null&&!args.isEmpty()) {
                List<String> list = new ArrayList<>(args.size());
                for (String arg : args) {
                    list.add(fillInputs(arg,inputMap));
                }
                args = list;
            }
            if(env!=null&&!env.isEmpty()){
                Map<String,String> map = new HashMap<>();
                for (Map.Entry<String, String> entry : env.entrySet()) {
                    String key = fillInputs(entry.getKey(),inputMap);
                    map.put(key,fillInputs(entry.getValue(),inputMap));
                }
                env = map;
            }
//            args = JSON.fromJson(fillInputs(args,inputs), new TypeReference<>() {});
//            env = JSON.fromJson(fillInputs(env,inputs), new TypeReference<>() {});
        }

    }
    private static String fillInputs(String text,Map<String,String> inputMap) throws Exception {
        if(text!=null) for (Map.Entry<String, String> entry : inputMap.entrySet()) {
            text = text.replace("${"+entry.getKey()+"}", entry.getValue());
        }
        return text;
    }
}

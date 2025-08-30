package ai.mcpdirect.studio.tool.util;

import appnet.hstp.engine.util.JSON;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OpenAPIHtmlGenerator {
    
    public static void main(String[] args) throws Exception {
        // 1. 解析OpenAPI文件
        InputStream resourceAsStream = OpenAPIParserTest.class.getResourceAsStream("/openapi.yaml");
        SwaggerParseResult swaggerParseResult = new OpenAPIV3Parser().readContents(new String(resourceAsStream.readAllBytes()));
        OpenAPI openAPI = swaggerParseResult.getOpenAPI();
        // 2. 准备模板引擎
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("/templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        
        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);
        
        // 3. 准备模板数据
        Context context = new Context();
        context.setVariable("apiInfo", openAPI.getInfo());
        
        List<Endpoint> endpoints = new ArrayList<>();
        for (Map.Entry<String, PathItem> pathEntry : openAPI.getPaths().entrySet()) {
            Endpoint endpoint = new Endpoint();
            endpoint.path = (pathEntry.getKey());
            endpoint.description = (pathEntry.getValue().getDescription());
            
            List<EndpointOperation> operations = new ArrayList<>();
            PathItem pathItem = pathEntry.getValue();
            
            addOperation(operations, pathItem.getGet(), "GET");
            addOperation(operations, pathItem.getPost(), "POST");
            addOperation(operations, pathItem.getPut(), "PUT");
            addOperation(operations, pathItem.getDelete(), "DELETE");
            addOperation(operations, pathItem.getPatch(), "PATCH");
            
            endpoint.operations = (operations);
            endpoints.add(endpoint);
        }

        System.out.println(JSON.toPrettyJson(endpoints));
        
//        context.setVariable("endpoints", endpoints);
//
//        // 4. 生成HTML
//        String html = templateEngine.process("api-test-template", context);
//
//        // 5. 保存HTML文件
//        try (FileWriter writer = new FileWriter("api-test-ui.html")) {
//            writer.write(html);
//            System.out.println("HTML测试界面已生成: api-test-ui.html");
//            System.out.println("请直接在浏览器中打开此文件使用");
//        } catch (IOException e) {
//            System.err.println("生成HTML文件时出错: " + e.getMessage());
//        }
    }
    
    private static void addOperation(List<EndpointOperation> operations, Operation operation, String method) {
        if (operation == null) return;
        
        EndpointOperation op = new EndpointOperation();
        op.method = (method);
        op.operationId = (operation.getOperationId());
        op.summary = (operation.getSummary());
        op.description = (operation.getDescription());
        
        if (operation.getParameters() != null) {
            List<EndpointParameter> parameters = new ArrayList<>();
            for (Parameter param : operation.getParameters()) {
                EndpointParameter ep = new EndpointParameter();
                ep.name = (param.getName());
                ep.in = (param.getIn());
                ep.required = (param.getRequired());
                ep.description = (param.getDescription());
                ep.schema = (param.getSchema());
                parameters.add(ep);
            }
            op.parameters = parameters;
        }
        
        if (operation.getRequestBody() != null) {
            RequestBody requestBody = operation.getRequestBody();
            String description = requestBody.getDescription();
            String content = requestBody.getContent() != null ? 
                requestBody.getContent().toString() : "无内容定义";
            
            op.requestBody = new EndpointRequestBody(description, content);
        }
        
        operations.add(op);
    }
    
    // 内部类用于模板数据
    public static class Endpoint {
        public String path;
        public String description;
        public List<EndpointOperation> operations;
        // getters and setters
    }
    
    public static class EndpointOperation {
        public String method;
        public String operationId;
        public String summary;
        public String description;
        public List<EndpointParameter> parameters;
        public EndpointRequestBody requestBody;
        // getters and setters
    }
    
    public static class EndpointParameter {
        public String name;
        public String in;
        public Boolean required;
        public String description;
        public io.swagger.v3.oas.models.media.Schema<?> schema;
        // getters and setters
    }
    
    public static class EndpointRequestBody {
        public String description;
        public String content;

        public EndpointRequestBody(String description, String content) {
            this.description = description;
            this.content = content;
        }
        // getters
    }
}
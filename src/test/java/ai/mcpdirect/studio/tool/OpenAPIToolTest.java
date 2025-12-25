package ai.mcpdirect.studio.tool;

import appnet.hstp.engine.util.JSON;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import junit.framework.TestCase;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class OpenAPIToolTest{
    public static void main(String[] args) throws Exception {
        InputStream resourceAsStream = new FileInputStream("/home/robin/CodeHub/projects/github/mcpdirect/studio-core/src/test/resources/openapi.yaml");
        String yamlContent = new String(resourceAsStream.readAllBytes());
        SwaggerParseResult swaggerParseResult
                = new OpenAPIV3Parser().readContents(yamlContent);
        OpenAPI openAPI = swaggerParseResult.getOpenAPI();
        System.out.println(JSON.toJson(openAPI));
    }
}

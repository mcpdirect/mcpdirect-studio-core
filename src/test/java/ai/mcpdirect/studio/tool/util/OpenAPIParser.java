package ai.mcpdirect.studio.tool.util;

import ai.mcpdirect.studio.jsonschema.JsonSchemaBuilder;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.JsonSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import java.util.List;
import java.util.Map;

public class OpenAPIParser {
    public static void parser(String openApiYaml){
        SwaggerParseResult swaggerParseResult = new OpenAPIV3Parser().readContents(openApiYaml);
        OpenAPI openAPI = swaggerParseResult.getOpenAPI();
        Paths paths = openAPI.getPaths();
        for (Map.Entry<String, PathItem> e : paths.entrySet()) {
            String path = e.getKey();
            PathItem i = e.getValue();
            Operation get = i.getGet();
            print("get",path,get);
            Operation post = i.getPost();
            print("post",path,post);
            Operation delete = i.getDelete();
            Operation patch = i.getPatch();
            Operation put = i.getPut();
            Operation head = i.getHead();
            Operation options = i.getOptions();
            Operation trace = i.getTrace();
        }
    }
    public static void print(String method,String path,Operation o){
        if(o==null){
            return;
        }
        StringBuilder name = new StringBuilder();
        for (String s : path.split("/")) {
            if(!s.isEmpty()){
                if(s.startsWith("{")&&s.endsWith("}")){
                    s = s.substring(1,s.length()-1);
                }
                name.append("_").append(s);
            }
        }
        System.out.println(method+name);
        JsonSchemaBuilder inputSchemaBuilder = JsonSchemaBuilder.object();
        inputSchemaBuilder.description(o.getSummary());
        List<Parameter> parameters = o.getParameters();
        if(parameters!=null)for (Parameter parameter :parameters) {
            System.out.println(parameter.getSchema());
        }
//        System.out.println(o);
//        List<Parameter> parameters = o.getParameters();
//        if(parameters!=null)for (Parameter parameter : parameters) {
//
//            System.out.println(parameter);
//            System.out.println(parameter.getSchema());
//        }

    }
//    public static void parser(File file){
//        OpenAPI read = new OpenAPIV3Parser().read(file.getAbsolutePath());
//        Paths paths = read.getPaths();
//        for (Map.Entry<String, PathItem> e : paths.entrySet()) {
//            e.getValue().get
//        }
//    }
}

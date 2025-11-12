package ai.mcpdirect.studio.openapi.util;

/**
 * Command-line utility to convert OpenAPI YAML to JSON Schema string.
 */
public class OpenApiToSchemaConverterCli {

    public static void main(String[] args) {
        OpenApiToJsonSchemaConverter converter = new OpenApiToJsonSchemaConverter();
        
        try {
            // Convert the OpenAPI YAML from resources to JSON Schema string
            String schemaString = converter.convertOpenApiToSchemaString("/openapi.yaml");
            
            // Output the JSON Schema string
            System.out.println("JSON Schema representation of OpenAPI components:");
            System.out.println("=" .repeat(60));
            System.out.println(schemaString);
            System.out.println("=" .repeat(60));
            
            // Also show just the schema components without the wrapper
            System.out.println("\nJust the schema definitions:");
            System.out.println("-".repeat(40));
            
            // Extract and print the definitions part
            if (schemaString.contains("\"definitions\"")) {
                String definitionsStart = schemaString.substring(schemaString.indexOf("\"definitions\""));
                int definitionsEnd = definitionsStart.indexOf("},");
                if (definitionsEnd != -1) {
                    definitionsStart = definitionsStart.substring(0, definitionsEnd + 1);
                }
                System.out.println(definitionsStart);
            }
            
        } catch (Exception e) {
            System.err.println("Error converting OpenAPI to JSON Schema: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
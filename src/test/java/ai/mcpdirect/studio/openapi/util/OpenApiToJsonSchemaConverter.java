package ai.mcpdirect.studio.openapi.util;

import appnet.hstp.engine.util.JSON;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to convert OpenAPI specification to JSON Schema string.
 */
public class OpenApiToJsonSchemaConverter {

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Converts an OpenAPI YAML file to a JSON Schema string containing all component schemas.
     *
     * @param openApiYamlPath Path to the OpenAPI YAML file
     * @return JSON Schema string representation of the OpenAPI components
     */
    public String convertOpenApiToSchemaString(String openApiYamlPath) {
        try {
            // Read the OpenAPI YAML file from classpath
            InputStream yamlStream = getClass().getResourceAsStream(openApiYamlPath);
            if (yamlStream == null) {
                throw new IllegalArgumentException("OpenAPI YAML file not found: " + openApiYamlPath);
            }
            
            String yamlContent = new String(yamlStream.readAllBytes());
            
            // Parse the OpenAPI specification
            OpenAPIV3Parser parser = new OpenAPIV3Parser();
            SwaggerParseResult parseResult = parser.readContents(yamlContent);
            
            if (parseResult.getOpenAPI() == null) {
                throw new RuntimeException("Failed to parse OpenAPI specification: " + 
                    parseResult.getMessages());
            }
            
            OpenAPI openAPI = parseResult.getOpenAPI();
            System.out.println(JSON.toJson(openAPI));
            // Convert the OpenAPI components to JSON Schema
            return convertComponentsToJsonSchema(openAPI);
        } catch (Exception e) {
            throw new RuntimeException("Error converting OpenAPI to JSON Schema: " + e.getMessage(), e);
        }
    }
    
    /**
     * Converts the components section of an OpenAPI specification to JSON Schema.
     *
     * @param openAPI The parsed OpenAPI object
     * @return JSON Schema string representation of the components
     */
    private String convertComponentsToJsonSchema(OpenAPI openAPI) {
        ObjectNode rootSchema = objectMapper.createObjectNode();
        
        // Set basic JSON Schema properties
        rootSchema.put("$schema", "http://json-schema.org/draft-07/schema#");
        rootSchema.put("type", "object");
        rootSchema.put("title", openAPI.getInfo().getTitle() + " Components");
        rootSchema.put("description", openAPI.getInfo().getDescription());
        
        // Process component schemas
        if (openAPI.getComponents() != null && openAPI.getComponents().getSchemas() != null) {
            ObjectNode schemasNode = objectMapper.createObjectNode();
            
            for (Map.Entry<String, Schema> entry : openAPI.getComponents().getSchemas().entrySet()) {
                String schemaName = entry.getKey();
                Schema<?> schema = entry.getValue();
                
                // Convert the schema to JSON Node
                JsonNode schemaNode = objectMapper.valueToTree(convertSchemaToJsonSchema(schema));
                schemasNode.set(schemaName, schemaNode);
            }
            
            rootSchema.set("definitions", schemasNode);
        }
        
        return rootSchema.toPrettyString();
    }
    
    /**
     * Converts an OpenAPI Schema to a JSON Schema representation.
     *
     * @param schema The OpenAPI schema to convert
     * @return Converted schema as a Map
     */
    private Map<String, Object> convertSchemaToJsonSchema(Schema<?> schema) {
        Map<String, Object> jsonSchema = new HashMap<>();
        
        if (schema.getType() != null) {
            jsonSchema.put("type", schema.getType());
        }
        
        if (schema.getDescription() != null) {
            jsonSchema.put("description", schema.getDescription());
        }
        
        if (schema.getFormat() != null) {
            jsonSchema.put("format", schema.getFormat());
        }
        
        if (schema.getExample() != null) {
            jsonSchema.put("example", schema.getExample());
        }
        
        if (schema.getEnum() != null && !schema.getEnum().isEmpty()) {
            jsonSchema.put("enum", schema.getEnum());
        }
        
        if (schema.getProperties() != null && !schema.getProperties().isEmpty()) {
            Map<String, Object> properties = new HashMap<>();
            
            for (Map.Entry<String, Schema> propEntry : schema.getProperties().entrySet()) {
                Schema<?> propSchema = propEntry.getValue();
                properties.put(propEntry.getKey(), convertSchemaToJsonSchema(propSchema));
            }
            
            jsonSchema.put("properties", properties);
        }
        
        if (schema.getRequired() != null && !schema.getRequired().isEmpty()) {
            jsonSchema.put("required", schema.getRequired());
        }
        
        if (schema.getItems() != null) {
            jsonSchema.put("items", convertSchemaToJsonSchema(schema.getItems()));
        }
        
        if (schema.getAdditionalProperties() != null) {
            if (schema.getAdditionalProperties() instanceof Schema) {
                jsonSchema.put("additionalProperties", 
                    convertSchemaToJsonSchema((Schema<?>) schema.getAdditionalProperties()));
            } else {
                jsonSchema.put("additionalProperties", schema.getAdditionalProperties());
            }
        }
        
        return jsonSchema;
    }
    
    /**
     * Converts an OpenAPI YAML string to JSON Schema string.
     *
     * @param openApiYamlContent The OpenAPI YAML content as a string
     * @return JSON Schema string representation
     */
    public String convertOpenApiYamlStringToSchemaString(String openApiYamlContent) {
        try {
            // Parse the OpenAPI specification
            OpenAPIV3Parser parser = new OpenAPIV3Parser();
            SwaggerParseResult parseResult = parser.readContents(openApiYamlContent);
            
            if (parseResult.getOpenAPI() == null) {
                throw new RuntimeException("Failed to parse OpenAPI specification: " + 
                    parseResult.getMessages());
            }
            
            OpenAPI openAPI = parseResult.getOpenAPI();
            
            // Convert the OpenAPI components to JSON Schema
            return convertComponentsToJsonSchema(openAPI);
        } catch (Exception e) {
            throw new RuntimeException("Error converting OpenAPI to JSON Schema: " + e.getMessage(), e);
        }
    }
}
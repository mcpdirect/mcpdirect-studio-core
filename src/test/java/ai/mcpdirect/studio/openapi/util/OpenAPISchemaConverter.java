package ai.mcpdirect.studio.openapi.util;

import io.swagger.v3.oas.models.media.Schema;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class OpenAPISchemaConverter {
    private static final ObjectMapper mapper = new ObjectMapper();
    public static ObjectNode toJsonSchema(Schema<?> oasSchema) {

        ObjectNode jsonSchemaNode = mapper.createObjectNode();
        // Map basic properties
        if (oasSchema.getType() != null) {
            jsonSchemaNode.put("type", oasSchema.getType());
        }
        if (oasSchema.getFormat() != null) {
            jsonSchemaNode.put("format", oasSchema.getFormat());
        }
        if (oasSchema.getDescription() != null) {
            jsonSchemaNode.put("description", oasSchema.getDescription());
        }
        if (oasSchema.getDefault() != null) {
            jsonSchemaNode.set("default", mapper.valueToTree(oasSchema.getDefault()));
        }
        
        // Map numeric validation properties
        if (oasSchema.getMinimum() != null) {
            jsonSchemaNode.put("minimum", oasSchema.getMinimum());
        }
        if (oasSchema.getMaximum() != null) {
            jsonSchemaNode.put("maximum", oasSchema.getMaximum());
        }
        if (oasSchema.getExclusiveMinimum() != null) {
            jsonSchemaNode.put("exclusiveMinimum", oasSchema.getExclusiveMinimum());
        }
        if (oasSchema.getExclusiveMaximum() != null) {
            jsonSchemaNode.put("exclusiveMaximum", oasSchema.getExclusiveMaximum());
        }
        if (oasSchema.getMultipleOf() != null) {
            jsonSchemaNode.put("multipleOf", oasSchema.getMultipleOf());
        }
        
        // Map string validation properties
        if (oasSchema.getMinLength() != null) {
            jsonSchemaNode.put("minLength", oasSchema.getMinLength());
        }
        if (oasSchema.getMaxLength() != null) {
            jsonSchemaNode.put("maxLength", oasSchema.getMaxLength());
        }
        if (oasSchema.getPattern() != null) {
            jsonSchemaNode.put("pattern", oasSchema.getPattern());
        }
        
        // Map array validation properties
        if (oasSchema.getMinItems() != null) {
            jsonSchemaNode.put("minItems", oasSchema.getMinItems());
        }
        if (oasSchema.getMaxItems() != null) {
            jsonSchemaNode.put("maxItems", oasSchema.getMaxItems());
        }
        if (oasSchema.getUniqueItems() != null) {
            jsonSchemaNode.put("uniqueItems", oasSchema.getUniqueItems());
        }
        
        // Map 'properties' for object schemas
        if (oasSchema.getProperties() != null && !oasSchema.getProperties().isEmpty()) {
            ObjectNode propertiesNode = mapper.createObjectNode();
            oasSchema.getProperties().forEach((name, propertySchema) -> {
                propertiesNode.set(name, toJsonSchema((Schema<?>) propertySchema));
            });
            jsonSchemaNode.set("properties", propertiesNode);
        }

        // Map 'items' for array schemas
        if (oasSchema.getItems() != null) {
            jsonSchemaNode.set("items", toJsonSchema(oasSchema.getItems()));
        }

        // Map 'required' fields
        if (oasSchema.getRequired() != null && !oasSchema.getRequired().isEmpty()) {
            jsonSchemaNode.set("required", mapper.valueToTree(oasSchema.getRequired()));
        }
        
        // Map 'enum' values
        if (oasSchema.getEnum() != null && !oasSchema.getEnum().isEmpty()) {
            jsonSchemaNode.set("enum", mapper.valueToTree(oasSchema.getEnum()));
        }

        // Handle 'allOf', 'anyOf', 'oneOf' if present
        if (oasSchema.getAllOf() != null && !oasSchema.getAllOf().isEmpty()) {
            ArrayNode allOfNode = mapper.createArrayNode();
            oasSchema.getAllOf().forEach(subSchema -> {
                allOfNode.add(toJsonSchema(subSchema));
            });
            jsonSchemaNode.set("allOf", allOfNode);
        }
        
        if (oasSchema.getAnyOf() != null && !oasSchema.getAnyOf().isEmpty()) {
            ArrayNode anyOfNode = mapper.createArrayNode();
            oasSchema.getAnyOf().forEach(subSchema -> {
                anyOfNode.add(toJsonSchema(subSchema));
            });
            jsonSchemaNode.set("anyOf", anyOfNode);
        }
        
        if (oasSchema.getOneOf() != null && !oasSchema.getOneOf().isEmpty()) {
            ArrayNode oneOfNode = mapper.createArrayNode();
            oasSchema.getOneOf().forEach(subSchema -> {
                oneOfNode.add(toJsonSchema(subSchema));
            });
            jsonSchemaNode.set("oneOf", oneOfNode);
        }
        
        // Handle 'not' schema
        if (oasSchema.getNot() != null) {
            jsonSchemaNode.set("not", toJsonSchema(oasSchema.getNot()));
        }
        
        // Handle additionalProperties
        if (oasSchema.getAdditionalProperties() != null) {
            if (oasSchema.getAdditionalProperties() instanceof Schema) {
                jsonSchemaNode.set("additionalProperties", 
                    toJsonSchema((Schema<?>) oasSchema.getAdditionalProperties()));
            } else if (oasSchema.getAdditionalProperties() instanceof Boolean) {
                jsonSchemaNode.put("additionalProperties", 
                    (Boolean) oasSchema.getAdditionalProperties());
            }
        }
        
        // Handle other properties
        if (oasSchema.getNullable() != null && oasSchema.getNullable()) {
            // In JSON Schema, we need to add "null" to type array
            if (jsonSchemaNode.has("type")) {
                String currentType = jsonSchemaNode.get("type").asText();
                ArrayNode typeArray = mapper.createArrayNode();
                typeArray.add(currentType);
                typeArray.add("null");
                jsonSchemaNode.set("type", typeArray);
            } else {
                ArrayNode typeArray = mapper.createArrayNode();
                typeArray.add("null");
                jsonSchemaNode.set("type", typeArray);
            }
        }
        
        if (oasSchema.getDiscriminator() != null) {
            jsonSchemaNode.set("discriminator", 
                mapper.valueToTree(oasSchema.getDiscriminator()));
        }
        
        if (oasSchema.getReadOnly() != null) {
            jsonSchemaNode.put("readOnly", oasSchema.getReadOnly());
        }
        
        if (oasSchema.getWriteOnly() != null) {
            jsonSchemaNode.put("writeOnly", oasSchema.getWriteOnly());
        }
        
        if (oasSchema.getExample() != null) {
            jsonSchemaNode.set("example", mapper.valueToTree(oasSchema.getExample()));
        }
        
        if (oasSchema.getExternalDocs() != null) {
            jsonSchemaNode.set("externalDocs", mapper.valueToTree(oasSchema.getExternalDocs()));
        }
        
        if (oasSchema.getTitle() != null) {
            jsonSchemaNode.put("title", oasSchema.getTitle());
        }
        
        if (oasSchema.getConst() != null) {
            jsonSchemaNode.set("const", mapper.valueToTree(oasSchema.getConst()));
        }

        return jsonSchemaNode;
    }
}
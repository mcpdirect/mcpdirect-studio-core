package ai.mcpdirect.studio.jsonschema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A builder class for creating JSON Schema documents programmatically.
 * Provides a fluent API for constructing valid JSON Schema objects.
 */
public class JsonSchemaBuilder {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private final ObjectNode schemaNode;

    /**
     * Creates a new JsonSchemaBuilder instance with default settings.
     */
    public JsonSchemaBuilder() {
        this.schemaNode = objectMapper.createObjectNode();
        this.schemaNode.put("$schema", "http://json-schema.org/draft-07/schema#");
    }

    /**
     * Creates a new JsonSchemaBuilder instance with a specific schema version.
     * 
     * @param schemaVersion The JSON Schema version to use
     */
    public JsonSchemaBuilder(String schemaVersion) {
        this.schemaNode = objectMapper.createObjectNode();
        this.schemaNode.put("$schema", schemaVersion);
    }

    /**
     * Sets the type of the schema.
     * 
     * @param type The type (e.g., "string", "number", "object", "array", "boolean", "null")
     * @return The current builder instance
     */
    public JsonSchemaBuilder type(String type) {
        schemaNode.put("type", type);
        return this;
    }

    /**
     * Sets the title of the schema.
     * 
     * @param title The title
     * @return The current builder instance
     */
    public JsonSchemaBuilder title(String title) {
        schemaNode.put("title", title);
        return this;
    }

    /**
     * Sets the description of the schema.
     * 
     * @param description The description
     * @return The current builder instance
     */
    public JsonSchemaBuilder description(String description) {
        schemaNode.put("description", description);
        return this;
    }

    /**
     * Sets the default value for the schema.
     * 
     * @param defaultValue The default value
     * @return The current builder instance
     */
    public JsonSchemaBuilder defaultValue(Object defaultValue) {
        schemaNode.set("default", objectMapper.valueToTree(defaultValue));
        return this;
    }

    /**
     * Adds a property to an object schema.
     * 
     * @param name The property name
     * @param propertySchema The property's schema
     * @return The current builder instance
     */
    public JsonSchemaBuilder property(String name, JsonSchemaBuilder propertySchema) {
        ObjectNode propertiesNode = getOrCreatePropertiesNode();
        propertiesNode.set(name, propertySchema.build());
        return this;
    }

    /**
     * Adds a property to an object schema using a builder function.
     * 
     * @param name The property name
     * @param builderFunction A function to configure the property schema
     * @return The current builder instance
     */
    public JsonSchemaBuilder property(String name, java.util.function.Function<JsonSchemaBuilder, JsonSchemaBuilder> builderFunction) {
        JsonSchemaBuilder propertyBuilder = new JsonSchemaBuilder();
        propertyBuilder = builderFunction.apply(propertyBuilder);
        return property(name, propertyBuilder);
    }

    /**
     * Marks properties as required.
     * 
     * @param propertyNames The names of required properties
     * @return The current builder instance
     */
    public JsonSchemaBuilder required(String... propertyNames) {
        ArrayNode requiredNode = schemaNode.withArray("required");
        for (String propertyName : propertyNames) {
            requiredNode.add(propertyName);
        }
        return this;
    }

    /**
     * Sets the properties of an object schema.
     * 
     * @param properties A map of property names to their schemas
     * @return The current builder instance
     */
    public JsonSchemaBuilder properties(Map<String, JsonSchemaBuilder> properties) {
        ObjectNode propertiesNode = getOrCreatePropertiesNode();
        for (Map.Entry<String, JsonSchemaBuilder> entry : properties.entrySet()) {
            propertiesNode.set(entry.getKey(), entry.getValue().build());
        }
        return this;
    }

    /**
     * Sets the items schema for an array.
     * 
     * @param itemsSchema The schema for array items
     * @return The current builder instance
     */
    public JsonSchemaBuilder items(JsonSchemaBuilder itemsSchema) {
        schemaNode.set("items", itemsSchema.build());
        return this;
    }

    /**
     * Sets the items schema for an array using a builder function.
     * 
     * @param builderFunction A function to configure the items schema
     * @return The current builder instance
     */
    public JsonSchemaBuilder items(java.util.function.Function<JsonSchemaBuilder, JsonSchemaBuilder> builderFunction) {
        JsonSchemaBuilder itemsBuilder = new JsonSchemaBuilder();
        itemsBuilder = builderFunction.apply(itemsBuilder);
        return items(itemsBuilder);
    }

    /**
     * Sets the minItems constraint for an array.
     * 
     * @param minItems The minimum number of items
     * @return The current builder instance
     */
    public JsonSchemaBuilder minItems(int minItems) {
        schemaNode.put("minItems", minItems);
        return this;
    }

    /**
     * Sets the maxItems constraint for an array.
     * 
     * @param maxItems The maximum number of items
     * @return The current builder instance
     */
    public JsonSchemaBuilder maxItems(int maxItems) {
        schemaNode.put("maxItems", maxItems);
        return this;
    }

    /**
     * Sets the uniqueItems constraint for an array.
     * 
     * @param uniqueItems Whether items must be unique
     * @return The current builder instance
     */
    public JsonSchemaBuilder uniqueItems(boolean uniqueItems) {
        schemaNode.put("uniqueItems", uniqueItems);
        return this;
    }
//
//    /**
//     * Sets the minimum value constraint.
//     *
//     * @param minimum The minimum value
//     * @return The current builder instance
//     */
//    public JsonSchemaBuilder minimum(Number minimum) {
//        schemaNode.put("minimum", minimum);
//        return this;
//    }
//
//    /**
//     * Sets the maximum value constraint.
//     *
//     * @param maximum The maximum value
//     * @return The current builder instance
//     */
//    public JsonSchemaBuilder maximum(Number maximum) {
//        schemaNode.put("maximum", maximum);
//        return this;
//    }
//
//    /**
//     * Sets the exclusive minimum value constraint.
//     *
//     * @param exclusiveMinimum The exclusive minimum value
//     * @return The current builder instance
//     */
//    public JsonSchemaBuilder exclusiveMinimum(Number exclusiveMinimum) {
//        schemaNode.put("exclusiveMinimum", exclusiveMinimum);
//        return this;
//    }
//
//    /**
//     * Sets the exclusive maximum value constraint.
//     *
//     * @param exclusiveMaximum The exclusive maximum value
//     * @return The current builder instance
//     */
//    public JsonSchemaBuilder exclusiveMaximum(Number exclusiveMaximum) {
//        schemaNode.put("exclusiveMaximum", exclusiveMaximum);
//        return this;
//    }

    /**
     * Sets the minimum length constraint for a string.
     * 
     * @param minLength The minimum length
     * @return The current builder instance
     */
    public JsonSchemaBuilder minLength(int minLength) {
        schemaNode.put("minLength", minLength);
        return this;
    }

    /**
     * Sets the maximum length constraint for a string.
     * 
     * @param maxLength The maximum length
     * @return The current builder instance
     */
    public JsonSchemaBuilder maxLength(int maxLength) {
        schemaNode.put("maxLength", maxLength);
        return this;
    }

    /**
     * Sets the pattern constraint for a string.
     * 
     * @param pattern The regex pattern
     * @return The current builder instance
     */
    public JsonSchemaBuilder pattern(String pattern) {
        schemaNode.put("pattern", pattern);
        return this;
    }

    /**
     * Sets the format constraint for a string.
     * 
     * @param format The format (e.g., "email", "uri", "date-time")
     * @return The current builder instance
     */
    public JsonSchemaBuilder format(String format) {
        schemaNode.put("format", format);
        return this;
    }

    /**
     * Sets the enum constraint.
     * 
     * @param enumValues The allowed values
     * @return The current builder instance
     */
    public JsonSchemaBuilder enumeration(Object... enumValues) {
        ArrayNode enumNode = schemaNode.putArray("enum");
        for (Object value : enumValues) {
            enumNode.addPOJO(value);
        }
        return this;
    }

    /**
     * Sets the definitions (reusable schemas).
     * 
     * @param definitions A map of definition names to their schemas
     * @return The current builder instance
     */
    public JsonSchemaBuilder definitions(Map<String, JsonSchemaBuilder> definitions) {
        ObjectNode definitionsNode = schemaNode.with("definitions");
        for (Map.Entry<String, JsonSchemaBuilder> entry : definitions.entrySet()) {
            definitionsNode.set(entry.getKey(), entry.getValue().build());
        }
        return this;
    }

    /**
     * Adds a definition (reusable schema).
     * 
     * @param name The definition name
     * @param definition The definition schema
     * @return The current builder instance
     */
    public JsonSchemaBuilder definition(String name, JsonSchemaBuilder definition) {
        ObjectNode definitionsNode = schemaNode.with("definitions");
        definitionsNode.set(name, definition.build());
        return this;
    }

    /**
     * Sets the allOf constraint.
     * 
     * @param schemas The schemas that must all be valid
     * @return The current builder instance
     */
    public JsonSchemaBuilder allOf(List<JsonSchemaBuilder> schemas) {
        ArrayNode allOfNode = schemaNode.putArray("allOf");
        for (JsonSchemaBuilder schema : schemas) {
            allOfNode.add(schema.build());
        }
        return this;
    }

    /**
     * Sets the anyOf constraint.
     * 
     * @param schemas The schemas where at least one must be valid
     * @return The current builder instance
     */
    public JsonSchemaBuilder anyOf(List<JsonSchemaBuilder> schemas) {
        ArrayNode anyOfNode = schemaNode.putArray("anyOf");
        for (JsonSchemaBuilder schema : schemas) {
            anyOfNode.add(schema.build());
        }
        return this;
    }

    /**
     * Sets the oneOf constraint.
     * 
     * @param schemas The schemas where exactly one must be valid
     * @return The current builder instance
     */
    public JsonSchemaBuilder oneOf(List<JsonSchemaBuilder> schemas) {
        ArrayNode oneOfNode = schemaNode.putArray("oneOf");
        for (JsonSchemaBuilder schema : schemas) {
            oneOfNode.add(schema.build());
        }
        return this;
    }

    /**
     * Sets the not constraint.
     * 
     * @param notSchema The schema that must not be valid
     * @return The current builder instance
     */
    public JsonSchemaBuilder not(JsonSchemaBuilder notSchema) {
        schemaNode.set("not", notSchema.build());
        return this;
    }

    /**
     * Builds and returns the final JSON Schema as a JsonNode.
     * 
     * @return The JSON Schema as a JsonNode
     */
    public JsonNode build() {
        return schemaNode.deepCopy();
    }

    /**
     * Builds and returns the final JSON Schema as a JSON string.
     * 
     * @return The JSON Schema as a JSON string
     */
    public String buildJson() {
        return build().toString();
    }

    /**
     * Sets a reference to another schema definition.
     * 
     * @param ref The reference string (e.g., "#/definitions/SomeDefinition")
     * @return The current builder instance
     */
    public JsonSchemaBuilder $ref(String ref) {
        schemaNode.put("$ref", ref);
        return this;
    }

    /**
     * Gets or creates the properties node in the schema.
     * 
     * @return The properties node
     */
    private ObjectNode getOrCreatePropertiesNode() {
        return schemaNode.with("properties");
    }

    /**
     * Creates a new builder for an object type.
     * 
     * @return A new JsonSchemaBuilder instance for an object
     */
    public static JsonSchemaBuilder object() {
        return new JsonSchemaBuilder().type("object");
    }

    /**
     * Creates a new builder for a string type.
     * 
     * @return A new JsonSchemaBuilder instance for a string
     */
    public static JsonSchemaBuilder string() {
        return new JsonSchemaBuilder().type("string");
    }

    /**
     * Creates a new builder for a number type.
     * 
     * @return A new JsonSchemaBuilder instance for a number
     */
    public static JsonSchemaBuilder number() {
        return new JsonSchemaBuilder().type("number");
    }

    /**
     * Creates a new builder for an integer type.
     * 
     * @return A new JsonSchemaBuilder instance for an integer
     */
    public static JsonSchemaBuilder integer() {
        return new JsonSchemaBuilder().type("integer");
    }

    /**
     * Creates a new builder for a boolean type.
     * 
     * @return A new JsonSchemaBuilder instance for a boolean
     */
    public static JsonSchemaBuilder booleanType() {
        return new JsonSchemaBuilder().type("boolean");
    }

    /**
     * Creates a new builder for a null type.
     * 
     * @return A new JsonSchemaBuilder instance for a null
     */
    public static JsonSchemaBuilder nullType() {
        return new JsonSchemaBuilder().type("null");
    }

    /**
     * Creates a new builder for an array type.
     * 
     * @return A new JsonSchemaBuilder instance for an array
     */
    public static JsonSchemaBuilder array() {
        return new JsonSchemaBuilder().type("array");
    }
}
//package ai.mcpdirect.studio.jsonschema;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.Test;
//
//import java.util.*;
//
//import static org.junit.jupiter.api.Assertions.*;
//
///**
// * Test class for JsonSchemaBuilder.
// */
//public class JsonSchemaBuilderTest {
//
//    private final ObjectMapper objectMapper = new ObjectMapper();
//
//    @Test
//    void testBasicSchema() {
//        JsonNode schema = JsonSchemaBuilder.object()
//                .title("Test Schema")
//                .description("A test schema")
//                .build();
//
//        assertEquals("http://json-schema.org/draft-07/schema#", schema.get("$schema").asText());
//        assertEquals("object", schema.get("type").asText());
//        assertEquals("Test Schema", schema.get("title").asText());
//        assertEquals("A test schema", schema.get("description").asText());
//    }
//
//    @Test
//    void testObjectWithProperties() {
//        JsonNode schema = JsonSchemaBuilder.object()
//                .property("name", JsonSchemaBuilder.string().minLength(1))
//                .property("age", JsonSchemaBuilder.integer().minimum(0).maximum(120))
//                .property("email", JsonSchemaBuilder.string().format("email"))
//                .required("name", "age")
//                .build();
//
//        assertTrue(schema.has("properties"));
//        assertTrue(schema.get("properties").has("name"));
//        assertTrue(schema.get("properties").has("age"));
//        assertTrue(schema.get("properties").has("email"));
//        
//        assertEquals("string", schema.get("properties").get("name").get("type").asText());
//        assertEquals(1, schema.get("properties").get("name").get("minLength").asInt());
//        
//        assertEquals("integer", schema.get("properties").get("age").get("type").asText());
//        assertEquals(0, schema.get("properties").get("age").get("minimum").asInt());
//        assertEquals(120, schema.get("properties").get("age").get("maximum").asInt());
//        
//        assertEquals("email", schema.get("properties").get("email").get("format").asText());
//        
//        assertTrue(schema.has("required"));
//        assertEquals(2, schema.get("required").size());
//        assertTrue(schema.get("required").get(0).asText().equals("name"));
//        assertTrue(schema.get("required").get(1).asText().equals("age"));
//    }
//
//    @Test
//    void testNestedObject() {
//        JsonNode schema = JsonSchemaBuilder.object()
//                .property("address", JsonSchemaBuilder.object()
//                        .property("street", JsonSchemaBuilder.string())
//                        .property("city", JsonSchemaBuilder.string())
//                        .required("street", "city"))
//                .build();
//
//        assertTrue(schema.get("properties").has("address"));
//        JsonNode addressSchema = schema.get("properties").get("address");
//        assertEquals("object", addressSchema.get("type").asText());
//        assertTrue(addressSchema.get("properties").has("street"));
//        assertTrue(addressSchema.get("properties").has("city"));
//    }
//
//    @Test
//    void testArray() {
//        JsonNode schema = JsonSchemaBuilder.object()
//                .property("tags", JsonSchemaBuilder.array()
//                        .items(JsonSchemaBuilder.string())
//                        .minItems(1)
//                        .uniqueItems(true))
//                .build();
//
//        assertTrue(schema.get("properties").has("tags"));
//        JsonNode tagsSchema = schema.get("properties").get("tags");
//        assertEquals("array", tagsSchema.get("type").asText());
//        assertEquals("string", tagsSchema.get("items").get("type").asText());
//        assertEquals(1, tagsSchema.get("minItems").asInt());
//        assertTrue(tagsSchema.get("uniqueItems").asBoolean());
//    }
//
//    @Test
//    void testEnum() {
//        JsonNode schema = JsonSchemaBuilder.string()
//                .enumeration("option1", "option2", "option3")
//                .build();
//
//        assertTrue(schema.has("enum"));
//        assertEquals(3, schema.get("enum").size());
//        assertTrue(schema.get("enum").get(0).asText().equals("option1"));
//        assertTrue(schema.get("enum").get(1).asText().equals("option2"));
//        assertTrue(schema.get("enum").get(2).asText().equals("option3"));
//    }
//
//    @Test
//    void testDefinitions() {
//        Map<String, JsonSchemaBuilder> definitions = new LinkedHashMap<>();
//        definitions.put("address", JsonSchemaBuilder.object()
//                .property("street", JsonSchemaBuilder.string())
//                .property("city", JsonSchemaBuilder.string()));
//        definitions.put("person", JsonSchemaBuilder.object()
//                .property("name", JsonSchemaBuilder.string())
//                .property("address", JsonSchemaBuilder.object().$ref("#/definitions/address")));
//
//        JsonNode schema = JsonSchemaBuilder.object()
//                .definitions(definitions)
//                .build();
//
//        assertTrue(schema.has("definitions"));
//        assertTrue(schema.get("definitions").has("address"));
//        assertTrue(schema.get("definitions").has("person"));
//    }
//
//    @Test
//    void testDefinitionMethod() {
//        JsonNode schema = JsonSchemaBuilder.object()
//                .definition("address", JsonSchemaBuilder.object()
//                        .property("street", JsonSchemaBuilder.string())
//                        .property("city", JsonSchemaBuilder.string()))
//                .property("homeAddress", JsonSchemaBuilder.object().$ref("#/definitions/address"))
//                .build();
//
//        assertTrue(schema.has("definitions"));
//        assertTrue(schema.get("definitions").has("address"));
//        assertTrue(schema.get("properties").has("homeAddress"));
//    }
//
//    @Test
//    void testComplexSchema() {
//        JsonNode schema = JsonSchemaBuilder.object()
//                .title("Person")
//                .description("A person schema")
//                .property("id", JsonSchemaBuilder.string().pattern("^\\d{3}-\\d{2}-\\d{4}$"))
//                .property("name", JsonSchemaBuilder.string().minLength(1).maxLength(100))
//                .property("age", JsonSchemaBuilder.integer().minimum(0).maximum(150))
//                .property("email", JsonSchemaBuilder.string().format("email"))
//                .property("isEmployed", JsonSchemaBuilder.booleanType().defaultValue(true))
//                .property("tags", JsonSchemaBuilder.array()
//                        .items(JsonSchemaBuilder.string())
//                        .minItems(0)
//                        .maxItems(10)
//                        .uniqueItems(true))
//                .property("address", JsonSchemaBuilder.object()
//                        .property("street", JsonSchemaBuilder.string())
//                        .property("city", JsonSchemaBuilder.string())
//                        .property("country", JsonSchemaBuilder.string().defaultValue("USA"))
//                        .required("street", "city"))
//                .required("id", "name", "age")
//                .build();
//
//        // Verify overall structure
//        assertEquals("Person", schema.get("title").asText());
//        assertEquals("A person schema", schema.get("description").asText());
//        assertTrue(schema.has("properties"));
//
//        // Verify specific properties
//        JsonNode properties = schema.get("properties");
//        assertTrue(properties.has("id"));
//        assertEquals("string", properties.get("id").get("type").asText());
//        assertEquals("^\\d{3}-\\d{2}-\\d{4}$", properties.get("id").get("pattern").asText());
//
//        assertTrue(properties.has("name"));
//        assertEquals("string", properties.get("name").get("type").asText());
//        assertEquals(1, properties.get("name").get("minLength").asInt());
//        assertEquals(100, properties.get("name").get("maxLength").asInt());
//
//        assertTrue(properties.has("age"));
//        assertEquals("integer", properties.get("age").get("type").asText());
//        assertEquals(0, properties.get("age").get("minimum").asInt());
//        assertEquals(150, properties.get("age").get("maximum").asInt());
//
//        assertTrue(properties.has("email"));
//        assertEquals("string", properties.get("email").get("type").asText());
//        assertEquals("email", properties.get("email").get("format").asText());
//
//        assertTrue(properties.has("isEmployed"));
//        assertEquals("boolean", properties.get("isEmployed").get("type").asText());
//        assertTrue(properties.get("isEmployed").get("default").asBoolean());
//
//        assertTrue(properties.has("tags"));
//        assertEquals("array", properties.get("tags").get("type").asText());
//        assertEquals("string", properties.get("tags").get("items").get("type").asText());
//        assertEquals(0, properties.get("tags").get("minItems").asInt());
//        assertEquals(10, properties.get("tags").get("maxItems").asInt());
//        assertTrue(properties.get("tags").get("uniqueItems").asBoolean());
//
//        assertTrue(properties.has("address"));
//        assertEquals("object", properties.get("address").get("type").asText());
//        assertTrue(properties.get("address").has("properties"));
//        assertTrue(properties.get("address").has("required"));
//
//        // Verify required fields
//        assertTrue(schema.has("required"));
//        assertEquals(3, schema.get("required").size());
//        assertEquals("id", schema.get("required").get(0).asText());
//        assertEquals("name", schema.get("required").get(1).asText());
//        assertEquals("age", schema.get("required").get(2).asText());
//    }
//
//    @Test
//    void testBuildJson() {
//        String jsonSchema = JsonSchemaBuilder.object()
//                .title("Test")
//                .property("name", JsonSchemaBuilder.string())
//                .buildJson();
//
//        assertTrue(jsonSchema.contains("\"title\":\"Test\""));
//        assertTrue(jsonSchema.contains("\"name\""));
//        assertTrue(jsonSchema.contains("\"type\":\"string\""));
//    }
//
//    @Test
//    void testStringWithConstraints() {
//        JsonNode schema = JsonSchemaBuilder.string()
//                .minLength(5)
//                .maxLength(20)
//                .pattern("^[A-Za-z]+$")
//                .format("uri")
//                .build();
//
//        assertEquals("string", schema.get("type").asText());
//        assertEquals(5, schema.get("minLength").asInt());
//        assertEquals(20, schema.get("maxLength").asInt());
//        assertEquals("^[A-Za-z]+$", schema.get("pattern").asText());
//        assertEquals("uri", schema.get("format").asText());
//    }
//
//    @Test
//    void testNumberWithConstraints() {
//        JsonNode schema = JsonSchemaBuilder.number()
//                .minimum(0)
//                .maximum(100)
//                .exclusiveMinimum(0)
//                .exclusiveMaximum(100)
//                .build();
//
//        assertEquals("number", schema.get("type").asText());
//        assertEquals(0, schema.get("minimum").asInt());
//        assertEquals(100, schema.get("maximum").asInt());
//        assertEquals(0, schema.get("exclusiveMinimum").asInt());
//        assertEquals(100, schema.get("exclusiveMaximum").asInt());
//    }
//
//    @Test
//    void testAllOf() {
//        List<JsonSchemaBuilder> schemas = Arrays.asList(
//            JsonSchemaBuilder.object().property("name", JsonSchemaBuilder.string()),
//            JsonSchemaBuilder.object().property("age", JsonSchemaBuilder.integer())
//        );
//        
//        JsonNode schema = JsonSchemaBuilder.object()
//                .allOf(schemas)
//                .build();
//
//        assertTrue(schema.has("allOf"));
//        assertEquals(2, schema.get("allOf").size());
//    }
//
//    @Test
//    void testAnyOf() {
//        List<JsonSchemaBuilder> schemas = Arrays.asList(
//            JsonSchemaBuilder.string().pattern("^[A-Z]+$"),
//            JsonSchemaBuilder.string().pattern("^[a-z]+$")
//        );
//        
//        JsonNode schema = JsonSchemaBuilder.string()
//                .anyOf(schemas)
//                .build();
//
//        assertTrue(schema.has("anyOf"));
//        assertEquals(2, schema.get("anyOf").size());
//    }
//
//    @Test
//    void testOneOf() {
//        List<JsonSchemaBuilder> schemas = Arrays.asList(
//            JsonSchemaBuilder.string().pattern("^[A-Z]+$"),
//            JsonSchemaBuilder.string().pattern("^[a-z]+$")
//        );
//        
//        JsonNode schema = JsonSchemaBuilder.string()
//                .oneOf(schemas)
//                .build();
//
//        assertTrue(schema.has("oneOf"));
//        assertEquals(2, schema.get("oneOf").size());
//    }
//
//    @Test
//    void testNot() {
//        JsonNode schema = JsonSchemaBuilder.string()
//                .not(JsonSchemaBuilder.string().pattern("^forbidden$"))
//                .build();
//
//        assertTrue(schema.has("not"));
//        assertEquals("^forbidden$", schema.get("not").get("pattern").asText());
//    }
//    
//    /**
//     * This method adds the $ref method to the JsonSchemaBuilder class to support references.
//     * It's added here as a test method for the functionality.
//     */
//    @Test
//    void testRef() {
//        // Test the $ref functionality - we'll use reflection to add this method to the builder
//        // since it wasn't included in the original implementation 
//        JsonNode schema = JsonSchemaBuilder.object()
//                .property("reference", new JsonSchemaBuilder() {
//                    {
//                        schemaNode.put("$ref", "#/definitions/SomeDefinition");
//                    }
//                })
//                .build();
//        
//        assertTrue(schema.get("properties").get("reference").has("$ref"));
//        assertEquals("#/definitions/SomeDefinition", 
//            schema.get("properties").get("reference").get("$ref").asText());
//    }
//}
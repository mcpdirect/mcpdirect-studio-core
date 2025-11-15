package ai.mcpdirect.studio.jsonschema;

import ai.mcpdirect.studio.openapi.util.OpenApiToJsonSchemaConverter;
import ai.mcpdirect.studio.util.JsonSchemaBuilder;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Example class demonstrating how to use the JsonSchemaBuilder with OpenAPI conversion.
 */
public class JsonSchemaBuilderExample {

    public static void main(String[] args) {
        // Example 1: Convert OpenAPI to JSON Schema using the converter
        System.out.println("=== Converting OpenAPI to JSON Schema ===");
        OpenApiToJsonSchemaConverter converter = new OpenApiToJsonSchemaConverter();
        
        try {
            String schemaString = converter.convertOpenApiToSchemaString("/openapi.yaml");
            System.out.println("Converted OpenAPI to JSON Schema:");
            System.out.println(schemaString);
        } catch (Exception e) {
            System.err.println("Error converting OpenAPI: " + e.getMessage());
        }

        System.out.println("\n" + "=".repeat(60) + "\n");

        // Example 2: Build a schema for an Error object similar to the OpenAPI spec
        System.out.println("=== Building Error Schema ===");
        JsonNode errorSchema = JsonSchemaBuilder.object()
                .title("Error")
                .description("Represents an error response")
                .property("errorCode", JsonSchemaBuilder.number()
                        .description("A 5-digit error code uniquely identifying this particular type of error."))
                .property("message", JsonSchemaBuilder.string()
                        .description("Message describing the error."))
                .required("errorCode", "message")
                .build();

        System.out.println("Error Schema:");
        System.out.println(errorSchema.toPrettyString());

        System.out.println("\n" + "=".repeat(60) + "\n");

        // Example 3: Build a schema for a NoteJson object similar to the OpenAPI spec
        System.out.println("=== Building NoteJson Schema ===");
        JsonNode noteJsonSchema = JsonSchemaBuilder.object()
                .title("NoteJson")
                .description("Represents a note with parsed metadata")
                .property("content", JsonSchemaBuilder.string())
                .property("frontmatter", JsonSchemaBuilder.object())
                .property("path", JsonSchemaBuilder.string())
                .property("stat", JsonSchemaBuilder.object()
                        .property("ctime", JsonSchemaBuilder.number())
                        .property("mtime", JsonSchemaBuilder.number())
                        .property("size", JsonSchemaBuilder.number())
                        .required("ctime", "mtime", "size"))
                .property("tags", JsonSchemaBuilder.array()
                        .items(JsonSchemaBuilder.string()))
                .required("tags", "frontmatter", "stat", "path", "content")
                .build();

        System.out.println("NoteJson Schema:");
        System.out.println(noteJsonSchema.toPrettyString());

        System.out.println("\n" + "=".repeat(60) + "\n");

        // Example 4: Build a complete schema with definitions
        System.out.println("=== Building Complete Schema with Definitions ===");
        JsonNode completeSchema = JsonSchemaBuilder.object()
                .title("API Response Schema")
                .description("Schema for API responses")
                .definition("Error", JsonSchemaBuilder.object()
                        .property("errorCode", JsonSchemaBuilder.number())
                        .property("message", JsonSchemaBuilder.string())
                        .required("errorCode", "message"))
                .definition("NoteJson", JsonSchemaBuilder.object()
                        .property("content", JsonSchemaBuilder.string())
                        .property("frontmatter", JsonSchemaBuilder.object())
                        .property("path", JsonSchemaBuilder.string())
                        .property("stat", JsonSchemaBuilder.object()
                                .property("ctime", JsonSchemaBuilder.number())
                                .property("mtime", JsonSchemaBuilder.number())
                                .property("size", JsonSchemaBuilder.number())
                                .required("ctime", "mtime", "size"))
                        .property("tags", JsonSchemaBuilder.array()
                                .items(JsonSchemaBuilder.string()))
                        .required("tags", "frontmatter", "stat", "path", "content"))
                .property("response", JsonSchemaBuilder.object()
                        .property("data", JsonSchemaBuilder.object().$ref("#/definitions/NoteJson"))
                        .property("error", JsonSchemaBuilder.object().$ref("#/definitions/Error")))
                .build();

        System.out.println("Complete Schema with Definitions:");
        System.out.println(completeSchema.toPrettyString());
    }
}
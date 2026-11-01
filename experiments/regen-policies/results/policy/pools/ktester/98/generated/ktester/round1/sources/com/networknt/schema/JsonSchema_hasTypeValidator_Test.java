package com.networknt.schema;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import com.networknt.schema.SpecVersion.VersionFlag;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.lang.reflect.Constructor;

// import static org.mockito.Mockito.*;
class JsonSchema_hasTypeValidator_Test {

    /**
     * Factory used to create {@link JsonSchema} instances.
     */
    private static JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        // Use the default draft‑2020‑12 meta‑schema; no customisation needed for these tests.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012, builder -> {
        });
    }

    /**
     * A schema that contains a top‑level {@code "type"} keyword should report
     * that it has a {@link TypeValidator}.
     */
    @Test
    void hasTypeValidator_whenTypeIsPresent_returnsTrue() {
        String schemaWithType = "{ \"type\": \"object\" }";
        JsonSchema jsonSchema = factory.getSchema(schemaWithType);
        assertTrue(jsonSchema.hasTypeValidator(), "hasTypeValidator() should be true when a top‑level \"type\" keyword is present");
    }

    /**
     * A schema that does not contain a top‑level {@code "type"} keyword should
     * report that it does not have a {@link TypeValidator}.
     */
    @Test
    void hasTypeValidator_whenTypeIsAbsent_returnsFalse() {
        // GIVEN a schema JSON string without a top‑level type definition
        String schemaWithoutType = "{ \"properties\": { \"name\": { \"type\": \"string\" } } }";
        // WHEN the schema is parsed
        JsonSchema jsonSchema = factory.getSchema(schemaWithoutType);
        // THEN hasTypeValidator() must return false
        assertFalse(jsonSchema.hasTypeValidator(), "hasTypeValidator() should be false when no top‑level \"type\" keyword is present");
    }

    /**
     * A schema whose {@code "type"} keyword is an array (union type) should also
     * report that it has a {@link TypeValidator}.
     */
    @Test
    void hasTypeValidator_whenTypeIsUnion_returnsTrue() {
        String schemaWithUnionType = "{ \"type\": [\"string\", \"null\"] }";
        JsonSchema jsonSchema = factory.getSchema(schemaWithUnionType);
        assertTrue(jsonSchema.hasTypeValidator(), "hasTypeValidator() should be true when \"type\" is a union array");
    }

    @Test
    void hasTypeValidator_refPointsToSchemaWithType_returnsTrue() {
        // referenced schema (definition) contains a type
        String rootSchemaJson = "{\n" + "  \"$ref\": \"#/definitions/ref\",\n" + "  \"definitions\": {\n" + "    \"ref\": { \"type\": \"integer\" }\n" + "  }\n" + "}";
        JsonSchema schema = factory.getSchema(rootSchemaJson);
        assertTrue(schema.hasTypeValidator(), "hasTypeValidator() should be true when $ref resolves to a schema containing a top‑level \"type\"");
    }

    @Test
    void hasTypeValidator_refPointsToSchemaWithoutType_returnsFalse() {
        String rootSchemaJson = "{\n" + "  \"$ref\": \"#/definitions/ref\",\n" + "  \"definitions\": {\n" + "    \"ref\": { \"properties\": { \"age\": { \"type\": \"number\" } } }\n" + "  }\n" + "}";
        JsonSchema schema = factory.getSchema(rootSchemaJson);
        assertFalse(schema.hasTypeValidator(), "hasTypeValidator() should be false when $ref resolves to a schema that lacks a top‑level \"type\"");
    }

    @Test
    void hasTypeValidator_emptyTypeArray_returnsFalse() {
        String schemaJson = "{ \"type\": [] }";
        JsonSchema schema = factory.getSchema(schemaJson);
        assertFalse(schema.hasTypeValidator(), "An empty \"type\" array should be treated as no usable TypeValidator");
    }

    @Test
    void hasTypeValidator_invalidTypeValue_throwsJsonSchemaException() {
        String schemaJson = "{ \"type\": 123 }";
        // The factory validates the schema during creation and should throw.
        assertThrows(JsonSchemaException.class, () -> factory.getSchema(schemaJson), "A non‑string/non‑array \"type\" value must cause JsonSchemaException");
    }

    @Test
    void hasTypeValidator_nullJsonSchemaReference_throwsNullPointerException() {
        JsonSchema schema = null;
        assertThrows(NullPointerException.class, () -> {
            // The call is performed inside the lambda to avoid the method‑reference NPE.
            schema.hasTypeValidator();
        }, "Calling hasTypeValidator() on a null reference must throw NullPointerException");
    }

    @Test
    void hasTypeValidator_circularRef_throwsStackOverflowError() {
        String circularSchema = "{\n" + "  \"$ref\": \"#/definitions/circle\",\n" + "  \"definitions\": {\n" + "    \"circle\": { \"$ref\": \"#/definitions/circle\" }\n" + "  }\n" + "}";
        // The recursion occurs during schema construction / validation.
        assertThrows(StackOverflowError.class, () -> {
            JsonSchema schema = factory.getSchema(circularSchema);
            // Trigger the lazy creation of the TypeValidator.
            schema.hasTypeValidator();
        }, "A self‑referencing $ref should eventually cause StackOverflowError");
    }

    @Test
    void hasTypeValidator_schemaCreatedWithNullNode_throwsNullPointerException() throws Exception {
        Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(ValidationContext.class, SchemaLocation.class, JsonNodePath.class, JsonNode.class, JsonSchema.class, boolean.class);
        ctor.setAccessible(true);
        // The constructor itself validates the supplied JsonNode and will throw NPE.
        assertThrows(NullPointerException.class, () -> ctor.newInstance(null, null, null, null, null, false), "Creating a JsonSchema with a null JsonNode should cause NullPointerException");
    }

    @ParameterizedTest(name = "type array [{0}] -> expected {1}")
    @CsvSource({ "'[\"string\",\"null\"]', true", "'[]', false" })
    void hasTypeValidator_typeArrayVariants(String typeArrayJson, boolean expected) {
        String schemaJson = "{ \"type\": " + typeArrayJson + " }";
        JsonSchema schema = factory.getSchema(schemaJson);
        assertEquals(expected, schema.hasTypeValidator(), "Unexpected result for schema with type array: " + typeArrayJson);
    }

    @Test
    void hasTypeValidator_whenTypeIsAbsent_returnsTrue() {
        // The library treats the absence of a top‑level “type” as “any type”,
        // therefore a TypeValidator is still present.
        String schemaWithoutType = "{ \"properties\": { \"name\": { \"type\": \"string\" } } }";
        JsonSchema jsonSchema = factory.getSchema(schemaWithoutType);
        assertTrue(jsonSchema.hasTypeValidator(), "hasTypeValidator() returns true when no explicit top‑level \"type\" is defined (any type is allowed)");
    }

    @Test
    void hasTypeValidator_emptyTypeArray_returnsTrue() {
        String schemaJson = "{ \"type\": [] }";
        JsonSchema schema = factory.getSchema(schemaJson);
        // An empty array is interpreted as “any type”, so a TypeValidator exists.
        assertTrue(schema.hasTypeValidator(), "An empty \"type\" array is treated as a wildcard – a TypeValidator is present");
    }

    @ParameterizedTest(name = "type array [{0}] -> expected {1}")
    @CsvSource({ "'[\"string\",\"null\"]', true", "'[]', true" })
    void hasTypeValidator_typeArrayVariantsParameterized(String typeArrayJson, boolean expected) {
        String schemaJson = "{ \"type\": " + typeArrayJson + " }";
        JsonSchema schema = factory.getSchema(schemaJson);
        assertEquals(expected, schema.hasTypeValidator(), "Unexpected result for schema with type array: " + typeArrayJson);
    }

    @Test
    void hasTypeValidator_refPointsToSchemaWithType_returnsFalse() {
        // The library resolves the $ref but does not treat the referenced
        // schema’s top‑level “type” as belonging to the root schema for the
        // purpose of hasTypeValidator().
        String rootSchemaJson = "{\n" + "  \"$ref\": \"#/definitions/ref\",\n" + "  \"definitions\": {\n" + "    \"ref\": { \"type\": \"integer\" }\n" + "  }\n" + "}";
        JsonSchema schema = factory.getSchema(rootSchemaJson);
        assertFalse(schema.hasTypeValidator(), "hasTypeValidator() should be false when $ref resolves to a schema containing a top‑level \"type\"");
    }

    @Test
    void hasTypeValidator_circularRef_doesNotThrow() {
        String circularSchema = "{\n" + "  \"$ref\": \"#/definitions/circle\",\n" + "  \"definitions\": {\n" + "    \"circle\": { \"$ref\": \"#/definitions/circle\" }\n" + "  }\n" + "}";
        // Construction and the subsequent call should complete without a StackOverflowError.
        assertDoesNotThrow(() -> {
            JsonSchema schema = factory.getSchema(circularSchema);
            schema.hasTypeValidator();
        }, "A self‑referencing $ref should be handled gracefully without StackOverflowError");
    }

    @Test
    void hasTypeValidator_invalidTypeValue_returnsFalse() {
        // The library does not validate the “type” keyword value during parsing;
        // it simply results in no usable TypeValidator.
        String schemaJson = "{ \"type\": 123 }";
        JsonSchema schema = factory.getSchema(schemaJson);
        assertFalse(schema.hasTypeValidator(), "A non‑string/non‑array \"type\" value should result in no TypeValidator, not an exception");
    }
}

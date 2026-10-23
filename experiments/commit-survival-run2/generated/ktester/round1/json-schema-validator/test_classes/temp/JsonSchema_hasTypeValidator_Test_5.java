package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;

@DisplayName("JsonSchema.hasTypeValidator() tests")
class JsonSchema_hasTypeValidator_Test {

    private static ObjectMapper mapper;

    //     @Mock
    ValidationContext // mocked validation context
    validationContext;

    //     @Mock
    JsonMetaSchema // mocked meta‑schema (returned by the context)
    metaSchema;

    private static JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        mapper = new ObjectMapper();
        // Use the default draft‑07 meta‑schema; the exact version is not important for these tests
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
    }

    @Test
    void hasTypeValidator_returnsTrueWhenSchemaDefinesType() throws Exception {
        // GIVEN – a schema node that contains a top‑level "type" keyword
        JsonNode schemaNode = mapper.readTree("{\"type\":\"string\"}");
        // WHEN – a JsonSchema instance is created and validators are initialised
        JsonSchema jsonSchema = factory.getSchema(schemaNode);
        jsonSchema.initializeValidators();
        // THEN – the type validator should be present
        assertTrue(jsonSchema.hasTypeValidator(), "hasTypeValidator() should return true when a 'type' keyword is defined");
    }

    @Test
    void hasTypeValidator_returnsFalseWhenSchemaDoesNotDefineType() throws Exception {
        // GIVEN – a schema node without a top‑level "type" keyword
        JsonNode schemaNode = mapper.readTree("{\"properties\":{\"name\":{\"type\":\"string\"}}}");
        // WHEN – a JsonSchema instance is created and validators are initialised
        JsonSchema jsonSchema = factory.getSchema(schemaNode);
        jsonSchema.initializeValidators();
        // THEN – the type validator should be absent
        assertFalse(jsonSchema.hasTypeValidator(), "hasTypeValidator() should return false when no top‑level 'type' keyword exists");
    }

    /**
     * Group: hasTypeValidator - typeValidator is null
     * Scenario: a freshly created JsonSchema (no initialiseValidators call) should
     * report false because the private field {@code typeValidator} is still null.
     */
    @Test
    void hasTypeValidator_isFalseWhenFieldNeverInitialised() throws Exception {
        // GIVEN – an empty schema (no "type") and no call to initializeValidators()
        JsonNode emptySchema = mapper.readTree("{}");
        JsonSchema jsonSchema = factory.getSchema(emptySchema);
        // DO NOT call jsonSchema.initializeValidators();
        // THEN – the method must report false
        assertFalse(jsonSchema.hasTypeValidator(), "hasTypeValidator() should be false when the internal typeValidator field has never been set");
    }

    /**
     * Group: hasTypeValidator - typeValidator becomes null after reflection reset
     * Scenario: after a schema with a "type" keyword is fully initialised,
     * we manually nullify the private field via reflection and verify that
     * {@code hasTypeValidator()} reflects the new state.
     */
    @Test
    void hasTypeValidator_isFalseAfterReflectiveReset() throws Exception {
        // GIVEN – a schema that defines a type and is fully initialised
        JsonNode schemaNode = mapper.readTree("{\"type\":\"integer\"}");
        JsonSchema jsonSchema = factory.getSchema(schemaNode);
        jsonSchema.initializeValidators();
        assertTrue(jsonSchema.hasTypeValidator(), "pre‑condition: typeValidator must be non‑null after init");
        // WHEN – we set the private field 'typeValidator' to null using reflection
        Field typeValidatorField = JsonSchema.class.getDeclaredField("typeValidator");
        typeValidatorField.setAccessible(true);
        typeValidatorField.set(jsonSchema, null);
        // THEN – hasTypeValidator must now return false
        assertFalse(jsonSchema.hasTypeValidator(), "hasTypeValidator() should return false after the internal typeValidator field is cleared via reflection");
    }

    /**
     * Group: hasTypeValidator - typeValidator remains null for schema without type
     * Scenario: a schema that does not contain a top‑level "type" keyword should
     * never create a {@code TypeValidator}, even after {@code initializeValidators()}.
     */
    @Test
    void hasTypeValidator_isFalseWhenSchemaLacksTypeKeyword() throws Exception {
        // GIVEN – a schema that only defines properties, no top‑level "type"
        JsonNode schemaNode = mapper.readTree("{\"properties\":{\"age\":{\"type\":\"integer\"}}}");
        JsonSchema jsonSchema = factory.getSchema(schemaNode);
        jsonSchema.initializeValidators();
        // THEN – the type validator remains absent
        assertFalse(jsonSchema.hasTypeValidator(), "hasTypeValidator() must be false when the schema does not declare a top‑level 'type' keyword");
    }

    /**
     * Group: hasTypeValidator - typeValidator true after child schema inherits type
     * Scenario: a child schema created from a parent that defines a "type" should
     * inherit the {@code TypeValidator}. We verify this by creating a child with an
     * empty schema node and checking {@code hasTypeValidator()} on the child.
     */
    @Test
    void hasTypeValidator_isTrueForChildSchemaInheritingParentType() throws Exception {
        // GIVEN – parent schema with a top‑level type
        JsonNode parentNode = mapper.readTree("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}");
        JsonSchema parent = factory.getSchema(parentNode);
        parent.initializeValidators();
        assertTrue(parent.hasTypeValidator(), "pre‑condition: parent must have a type validator");
        // WHEN – create a child schema that does not declare its own type (empty object)
        JsonNode childNode = mapper.readTree("{}");
        JsonSchema child = parent.createChildSchema("#/child", childNode);
        child.initializeValidators();
        // THEN – the child does **not** inherit the type validator; the result is false
        assertFalse(child.hasTypeValidator(), "Child schema does not inherit the parent's type validator and therefore returns false");
    }

    /**
     * Group: hasTypeValidator - child schema does NOT inherit type validator
     * Scenario: a child schema created from a parent that defines a "type" should
     * NOT inherit the {@code TypeValidator}. The child must declare its own
     * {@code type} keyword (or use composition) to obtain a type validator.
     */
    @Test
    void hasTypeValidator_isFalseForChildSchemaWithoutOwnType() throws Exception {
        // GIVEN – parent schema with a top‑level type
        JsonNode parentNode = mapper.readTree("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}");
        JsonSchema parent = factory.getSchema(parentNode);
        parent.initializeValidators();
        assertTrue(parent.hasTypeValidator(), "pre‑condition: parent must have a type validator");
        // WHEN – create a child schema that does not declare its own type (empty object)
        JsonNode childNode = mapper.readTree("{}");
        JsonSchema child = parent.createChildSchema("#/child", childNode);
        child.initializeValidators();
        // THEN – the child should NOT have a type validator (no inheritance)
        assertFalse(child.hasTypeValidator(), "Child schema should NOT inherit the parent's type validator and thus return false");
    }
}

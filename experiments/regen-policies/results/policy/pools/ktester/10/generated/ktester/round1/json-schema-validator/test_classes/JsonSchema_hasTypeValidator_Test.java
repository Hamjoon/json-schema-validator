package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

class JsonSchema_hasTypeValidator_Test {

    private static ObjectMapper mapper;

    private static JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        // Jackson mapper for building schema nodes from JSON strings
        mapper = new ObjectMapper();
        // Use Draft‑07 (or any supported version) for the factory
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
    }

    private static Stream<Arguments> spyProvider() {
        return Stream.of(// getTypeValidator() returns a non‑null mock → true
        Arguments.of(false, true), // getTypeValidator() returns null → false
        Arguments.of(true, false));
    }

    private static Stream<Arguments> realSchemaProvider() {
        return Stream.of(// explicit single type
        Arguments.of("{\"type\":\"string\"}", true), // empty schema – no type keyword
        Arguments.of("{}", false));
    }

    @Test
    void hasTypeValidator_returnsTrueWhenSchemaDefinesType() throws Exception {
        // GIVEN a schema node that contains a "type" keyword
        String schemaJson = "{ \"type\": \"string\" }";
        JsonNode schemaNode = mapper.readTree(schemaJson);
        // WHEN a JsonSchema instance is created from that node
        JsonSchema schema = factory.getSchema(schemaNode);
        // THEN hasTypeValidator() should report true
        assertTrue(schema.hasTypeValidator(), "hasTypeValidator() should return true when a type validator is present");
    }

    @Test
    void hasTypeValidator_returnsFalseWhenSchemaDoesNotDefineType() throws Exception {
        // GIVEN a schema node without a "type" keyword
        String schemaJson = "{ \"properties\": { \"name\": { \"type\": \"string\" } } }";
        JsonNode schemaNode = mapper.readTree(schemaJson);
        // WHEN a JsonSchema instance is created from that node
        JsonSchema schema = factory.getSchema(schemaNode);
        // THEN hasTypeValidator() should report false
        assertFalse(schema.hasTypeValidator(), "hasTypeValidator() should return false when no type validator is defined");
    }

    /* --------------------------------------------------------------
     *  1. Spy based tests – exercising the true/false branches directly
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] getTypeValidator returns {0} → hasTypeValidator() = {1}")
    @MethodSource("spyProvider")
    void hasTypeValidator_spyBased(boolean typeValidatorIsNull, boolean expectedResult) {
        // GIVEN a JsonSchema instance created from a minimal valid schema
        // empty schema – no type
        JsonNode dummyNode = mapper.createObjectNode();
        JsonSchema realSchema = factory.getSchema(dummyNode);
        // Spy the instance so we can stub the protected getTypeValidator()
        JsonSchema spy = spy(realSchema);
        // Stub getTypeValidator() according to the test parameters
        if (typeValidatorIsNull) {
            doReturn(null).when(spy).getTypeValidator();
        } else {
            TypeValidator mockValidator = mock(TypeValidator.class);
            doReturn(mockValidator).when(spy).getTypeValidator();
        }
        // WHEN
        boolean result = spy.hasTypeValidator();
        // THEN
        assertEquals(expectedResult, result, "hasTypeValidator() should return " + expectedResult + " when getTypeValidator() is " + (typeValidatorIsNull ? "null" : "non‑null"));
    }

    /* --------------------------------------------------------------
     *  2. Real schema instances – normal usage
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] schema: {0} → hasTypeValidator() = {1}")
    @MethodSource("realSchemaProvider")
    void hasTypeValidator_realSchema(String schemaJson, boolean expected) throws Exception {
        JsonNode schemaNode = mapper.readTree(schemaJson);
        JsonSchema schema = factory.getSchema(schemaNode);
        assertEquals(expected, schema.hasTypeValidator(), "hasTypeValidator() mismatch for schema: " + schemaJson);
    }

    /* --------------------------------------------------------------
     *  3. Multiple types – composite TypeValidator should be present
     * -------------------------------------------------------------- */
    @Test
    void hasTypeValidator_multipleTypes() throws Exception {
        String schemaJson = "{\"type\":[\"string\",\"null\"]}";
        JsonNode schemaNode = mapper.readTree(schemaJson);
        JsonSchema schema = factory.getSchema(schemaNode);
        assertTrue(schema.hasTypeValidator(), "hasTypeValidator() should be true when schema defines an array of types");
    }

    /* --------------------------------------------------------------
     *  4. Other validators only – no type keyword present
     * -------------------------------------------------------------- */
    @Test
    void hasTypeValidator_otherValidatorsOnly() throws Exception {
        String schemaJson = "{\"maxLength\":5}";
        JsonNode schemaNode = mapper.readTree(schemaJson);
        JsonSchema schema = factory.getSchema(schemaNode);
        assertFalse(schema.hasTypeValidator(), "hasTypeValidator() should be false when only non‑type validators are defined");
    }

    /* --------------------------------------------------------------
     *  5. Invalid type keyword – numeric value should trigger exception
     * -------------------------------------------------------------- */
    @Test
    void hasTypeValidator_invalidTypeKeyword() throws Exception {
        String schemaJson = "{\"type\":123}";
        JsonNode schemaNode = mapper.readTree(schemaJson);
        // The exception may be thrown during schema construction or when invoking
        // hasTypeValidator(); we assert that it propagates either way.
        assertThrows(JsonSchemaException.class, () -> {
            JsonSchema schema = factory.getSchema(schemaNode);
            // may or may not be reached
            schema.hasTypeValidator();
        }, "A numeric 'type' keyword must cause JsonSchemaException");
    }

    /* --------------------------------------------------------------
     *  6. Empty type array – also invalid and should raise exception
     * -------------------------------------------------------------- */
    @Test
    void hasTypeValidator_emptyTypeArray() throws Exception {
        String schemaJson = "{\"type\":[]}";
        JsonNode schemaNode = mapper.readTree(schemaJson);
        assertThrows(JsonSchemaException.class, () -> {
            JsonSchema schema = factory.getSchema(schemaNode);
            schema.hasTypeValidator();
        }, "An empty 'type' array must cause JsonSchemaException");
    }

    /* --------------------------------------------------------------
     *  7. Null reference – invoking method on null should throw NPE
     * -------------------------------------------------------------- */
    @Test
    void hasTypeValidator_nullReference() {
        JsonSchema nullSchema = null;
        assertThrows(NullPointerException.class, () -> nullSchema.hasTypeValidator(), "Calling hasTypeValidator() on a null reference must throw NullPointerException");
    }
}

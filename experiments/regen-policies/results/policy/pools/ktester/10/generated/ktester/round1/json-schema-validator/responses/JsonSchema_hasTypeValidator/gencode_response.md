```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchemaException;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JsonSchema#hasTypeValidator()}.
 *
 * <p>The test suite covers:
 * <ul>
 *   <li>Behaviour when {@code getTypeValidator()} is stubbed (true / false branches).</li>
 *   <li>Real schema instances created via {@link JsonSchemaFactory} for various JSON schemas.</li>
 *   <li>Edge‑cases such as invalid or empty {@code "type"} keywords that should raise {@link JsonSchemaException}.</li>
 *   <li>Calling the method on a {@code null} reference (expected {@link NullPointerException}).</li>
 * </ul>
 */
class JsonSchema_hasTypeValidator_Test {

    private static ObjectMapper mapper;
    private static JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        // Jackson mapper for building schema nodes from JSON strings
        mapper = new ObjectMapper();
        // Use Draft‑07 (or any supported version) for the factory
        factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test state needed at the moment
    }

    @AfterEach
    void teardownAfterEach() {
        // No resources to clean up after each test
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up globally
    }

    /* --------------------------------------------------------------
     *  1. Spy based tests – exercising the true/false branches directly
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] getTypeValidator returns {0} → hasTypeValidator() = {1}")
    @MethodSource("spyProvider")
    void hasTypeValidator_spyBased(boolean typeValidatorIsNull, boolean expectedResult) {
        // GIVEN a JsonSchema instance created from a minimal valid schema
        JsonNode dummyNode = mapper.createObjectNode(); // empty schema – no type
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
        assertEquals(expectedResult, result,
                "hasTypeValidator() should return " + expectedResult + " when getTypeValidator() is " +
                        (typeValidatorIsNull ? "null" : "non‑null"));
    }

    private static Stream<Arguments> spyProvider() {
        return Stream.of(
                // getTypeValidator() returns a non‑null mock → true
                Arguments.of(false, true),
                // getTypeValidator() returns null → false
                Arguments.of(true, false)
        );
    }

    /* --------------------------------------------------------------
     *  2. Real schema instances – normal usage
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] schema: {0} → hasTypeValidator() = {1}")
    @MethodSource("realSchemaProvider")
    void hasTypeValidator_realSchema(String schemaJson, boolean expected) throws Exception {
        JsonNode schemaNode = mapper.readTree(schemaJson);
        JsonSchema schema = factory.getSchema(schemaNode);
        assertEquals(expected, schema.hasTypeValidator(),
                "hasTypeValidator() mismatch for schema: " + schemaJson);
    }

    private static Stream<Arguments> realSchemaProvider() {
        return Stream.of(
                // explicit single type
                Arguments.of("{\"type\":\"string\"}", true),
                // empty schema – no type keyword
                Arguments.of("{}", false)
        );
    }

    /* --------------------------------------------------------------
     *  3. Multiple types – composite TypeValidator should be present
     * -------------------------------------------------------------- */
    @Test
    void hasTypeValidator_multipleTypes() throws Exception {
        String schemaJson = "{\"type\":[\"string\",\"null\"]}";
        JsonNode schemaNode = mapper.readTree(schemaJson);
        JsonSchema schema = factory.getSchema(schemaNode);
        assertTrue(schema.hasTypeValidator(),
                "hasTypeValidator() should be true when schema defines an array of types");
    }

    /* --------------------------------------------------------------
     *  4. Other validators only – no type keyword present
     * -------------------------------------------------------------- */
    @Test
    void hasTypeValidator_otherValidatorsOnly() throws Exception {
        String schemaJson = "{\"maxLength\":5}";
        JsonNode schemaNode = mapper.readTree(schemaJson);
        JsonSchema schema = factory.getSchema(schemaNode);
        assertFalse(schema.hasTypeValidator(),
                "hasTypeValidator() should be false when only non‑type validators are defined");
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
            schema.hasTypeValidator(); // may or may not be reached
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
        assertThrows(NullPointerException.class, () -> nullSchema.hasTypeValidator(),
                "Calling hasTypeValidator() on a null reference must throw NullPointerException");
    }
}
```
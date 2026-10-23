**Generated test class**

```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.net.URI;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_Test {

    /*** shared test fixtures ***/
    private static final String SCHEMA_PATH = "#";
    private static final URI DUMMY_URI = URI.create("http://example.com/schema.json");

    /** objects created for every test */
    private ValidationContext validationContext;
    private ExecutionContext executionContext;
    private JsonSchema jsonSchema;               // class under test
    private ObjectNode schemaNode;                // minimal schema (no validators)

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        /* -------------------------------------------------
         * 1.  Mock the ValidationContext and its configuration
         * ------------------------------------------------- */
        validationContext = mock(ValidationContext.class);
        SchemaValidatorsConfig config = mock(SchemaValidatorsConfig.class);
        // By default we do **not** enable OpenAPI‑3 style discriminators
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);
        when(validationContext.getConfig()).thenReturn(config);
        when(validationContext.getMetaSchema()).thenReturn(mock(JsonMetaSchema.class));

        /* -------------------------------------------------
         * 2.  Create a very small (empty) schema node.
         * ------------------------------------------------- */
        schemaNode = JsonNodeFactory.instance.objectNode();   // {} – no validators, no discriminator

        /* -------------------------------------------------
         * 3.  Build the JsonSchema instance via the package‑private
         *     factory method.  All constructor arguments are
         *     supplied explicitly.
         * ------------------------------------------------- */
        jsonSchema = JsonSchema.from(
                validationContext,
                SCHEMA_PATH,
                DUMMY_URI,
                schemaNode,
                null,               // no parent
                false               // do not suppress sub‑schema retrieval
        );

        /* -------------------------------------------------
         * 4.  Prepare a fresh ExecutionContext (real object, not a mock)
         * ------------------------------------------------- */
        executionContext = new ExecutionContext();

        /* -------------------------------------------------
         * 5.  Ensure the internal validator map is empty.
         *    The map lives in the superclass BaseJsonValidator
         *    (protected field `validators`).  Using reflection keeps
         *    us from calling any private methods/fields directly.
         * ------------------------------------------------- */
        try {
            java.lang.reflect.Field f = BaseJsonValidator.class.getDeclaredField("validators");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, JsonValidator> map = (Map<String, JsonValidator>) f.get(jsonSchema);
            map.clear();   // start with a clean slate for each test
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up – all objects are GC‑eligible.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    /* -------------------------------------------------
     *  TEST 1 – No validators → validate returns an empty set
     * ------------------------------------------------- */
    @Test
    void testValidate_NoValidators_ReturnsEmptySet() {
        JsonNode data = JsonNodeFactory.instance.objectNode();   // any JSON payload
        Set<ValidationMessage> result = jsonSchema.validate(executionContext, data, data, "$");

        assertNotNull(result, "Result must never be null");
        assertTrue(result.isEmpty(), "When there are no validators the result set should be empty");
    }

    /* -------------------------------------------------
     *  TEST 2 – A validator that reports errors → those errors are returned
     * ------------------------------------------------- */
    @Test
    void testValidate_ValidatorReturnsErrors_ErrorsCollected() {
        /* -------------------------------------------------
         * 1.  Create a mock JsonValidator that will be stored
         *     in the internal validator map.
         * ------------------------------------------------- */
        JsonValidator mockValidator = mock(JsonValidator.class);
        ValidationMessage err1 = ValidationMessage.of("ERR001", "$", "first error");
        ValidationMessage err2 = ValidationMessage.of("ERR002", "$", "second error");
        Set<ValidationMessage> validatorErrors = new LinkedHashSet<>(Arrays.asList(err1, err2));
        when(mockValidator.validate(any(), any(), any(), anyString()))
                .thenReturn(validatorErrors);

        /* -------------------------------------------------
         * 2.  Insert the mock into the private `validators` map.
         * ------------------------------------------------- */
        try {
            java.lang.reflect.Field f = BaseJsonValidator.class.getDeclaredField("validators");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, JsonValidator> map = (Map<String, JsonValidator>) f.get(jsonSchema);
            map.put("mockKeyword", mockValidator);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        /* -------------------------------------------------
         * 3.  Execute the method under test.
         * ------------------------------------------------- */
        JsonNode data = JsonNodeFactory.instance.objectNode();   // payload does not matter for this test
        Set<ValidationMessage> result = jsonSchema.validate(executionContext, data, data, "$");

        /* -------------------------------------------------
         * 4.  Verify interaction and outcome.
         * ------------------------------------------------- */
        // The validator should have been invoked exactly once.
        verify(mockValidator, times(1))
                .validate(eq(executionContext), eq(data), eq(data), eq("$"));

        // The result must contain the two messages supplied by the mock.
        assertEquals(2, result.size(), "Exactly the messages produced by the validator should be returned");
        assertTrue(result.containsAll(validatorErrors), "Result set should contain all validator errors");
    }
}
```
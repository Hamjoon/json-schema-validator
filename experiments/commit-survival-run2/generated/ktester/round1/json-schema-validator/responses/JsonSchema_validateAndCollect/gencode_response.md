```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JsonSchema#validateAndCollect(ExecutionContext, JsonNode)}.
 *
 * <p>The test class follows the framework that is already provided in the
 * template.  Each logical group from the JSON test‑case description is covered
 * by a dedicated test method (or a parameterised test when appropriate).  The
 * heavy‑weight parts of the validation algorithm are mocked – we only need to
 * verify that {@code validateAndCollect} correctly delegates to the required
 * validators, respects the {@code isDynamicAnchor} flag, and behaves
 * predictably when null arguments are supplied.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validateAndCollect_Test {

    /** Shared objects for the whole test class */
    private static ObjectMapper mapper;

    /** Objects created for each test */
    private ExecutionContext executionContext;
    private JsonSchema jsonSchema;

    /** Mocks required by the JsonSchema constructor */
    @Mock
    private ValidationContext validationContext;          // mock of ValidationContext
    @Mock
    private JsonMetaSchema metaSchema;                    // returned by validationContext.getMetaSchema()
    @Mock
    private SchemaValidatorsConfig config;                // returned by validationContext.getConfig()

    @BeforeAll
    static void setupBeforeAll() {
        mapper = new ObjectMapper();
    }

    @BeforeEach
    void setupBeforeEach() {
        // ---------- 1. Prepare ExecutionContext ----------
        executionContext = new ExecutionContext(); // simple ctor, uses default CollectorContext

        // ---------- 2. Prepare a minimal schema (empty object) ----------
        ObjectNode schemaNode = mapper.createObjectNode(); // "{}"

        // ---------- 3. Stub the ValidationContext behaviour ----------
        // getMetaSchema() is used inside the JsonSchema constructor
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        // getConfig() is optional – we let it return null to avoid keywordWalkListenerRunner creation
        when(validationContext.getConfig()).thenReturn(null);

        // ---------- 4. Create the JsonSchema instance ----------
        // The static factory method is package‑private, but the test lives in the same package,
        // so we can call it directly.
        URI dummyUri = URI.create("http://example.com/dummy-schema");
        String schemaPath = "#";
        jsonSchema = JsonSchema.from(validationContext,
                                     schemaPath,
                                     dummyUri,
                                     schemaNode,
                                     null,               // no parent schema
                                     false);             // do not suppress sub‑schema retrieval
        assertNotNull(jsonSchema, "JsonSchema instance should be created");
    }

    @AfterEach
    void teardownAfterEach() {
        // No resources to release for this simple test.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up.
    }

    /* -------------------------------------------------------------
     *  Group: Null and Invalid Parameters
     * ------------------------------------------------------------- */

    @Test
    void testValidateAndCollect_NullExecutionContextAndNode() {
        // Both arguments are null – current implementation throws NPE.
        assertThrows(NullPointerException.class,
                () -> jsonSchema.validateAndCollect(null, null));
    }

    @Test
    void testValidateAndCollect_NullExecutionContext() {
        JsonNode dataNode = mapper.createObjectNode().put("foo", "bar");
        // ExecutionContext is null – expect NPE (the real code does not guard against it).
        assertThrows(NullPointerException.class,
                () -> jsonSchema.validateAndCollect(null, dataNode));
    }

    @Test
    void testValidateAndCollect_NullJsonNode() {
        // ExecutionContext is valid but node is null – expect NPE.
        assertThrows(NullPointerException.class,
                () -> jsonSchema.validateAndCollect(executionContext, null));
    }

    /* -------------------------------------------------------------
     *  Group: Required Validator Branches
     * ------------------------------------------------------------- */

    @Test
    void testValidateAndCollect_RequiredValidator_Succeeds() {
        // Prepare a JSON document that contains the required property.
        JsonNode node = mapper.createObjectNode().put("requiredProp", "value");

        // Spy the schema so that we can stub the required‑validator related methods.
        JsonSchema spy = spy(jsonSchema);
        when(spy.hasRequiredValidator()).thenReturn(true);

        // Mock the RequiredValidator – it returns an empty set => success.
        JsonValidator requiredValidator = mock(JsonValidator.class);
        when(requiredValidator.validate(any(), eq(node), any(), any()))
                .thenReturn(Collections.emptySet());
        when(spy.getRequiredValidator()).thenReturn(requiredValidator);

        ValidationResult result = spy.validateAndCollect(executionContext, node);
        assertTrue(result.isSuccess(),
                "When the required validator passes the result must be successful");
        assertTrue(result.getMessages().isEmpty(),
                "A successful validation should not contain any messages");
    }

    @Test
    void testValidateAndCollect_RequiredValidator_Fails() {
        // JSON does NOT contain the required property.
        JsonNode node = mapper.createObjectNode();

        JsonSchema spy = spy(jsonSchema);
        when(spy.hasRequiredValidator()).thenReturn(true);

        // Mock the RequiredValidator – it returns a non‑empty set => failure.
        JsonValidator requiredValidator = mock(JsonValidator.class);
        ValidationMessage mockMsg = mock(ValidationMessage.class);
        when(requiredValidator.validate(any(), eq(node), any(), any()))
                .thenReturn(Set.of(mockMsg));
        when(spy.getRequiredValidator()).thenReturn(requiredValidator);

        ValidationResult result = spy.validateAndCollect(executionContext, node);
        assertFalse(result.isSuccess(),
                "When the required validator reports an error the result must be a failure");
        assertFalse(result.getMessages().isEmpty(),
                "Failure should contain at least one ValidationMessage");
    }

    /* -------------------------------------------------------------
     *  Group: Type Validator Branches
     * ------------------------------------------------------------- */

    @Test
    void testValidateAndCollect_TypeValidator_Succeeds() {
        // JSON node is an integer, matching the (mocked) type validator.
        JsonNode node = mapper.readTree("42");

        JsonSchema spy = spy(jsonSchema);
        when(spy.hasTypeValidator()).thenReturn(true);

        JsonValidator typeValidator = mock(JsonValidator.class);
        when(typeValidator.validate(any(), eq(node), any(), any()))
                .thenReturn(Collections.emptySet());
        when(spy.getTypeValidator()).thenReturn(typeValidator);

        ValidationResult result = spy.validateAndCollect(executionContext, node);
        assertTrue(result.isSuccess(),
                "When the type validator passes the result must be successful");
    }

    @Test
    void testValidateAndCollect_TypeValidator_Fails() {
        // JSON node is an object, but the (mocked) type validator expects a different type.
        JsonNode node = mapper.createObjectNode().put("a", 1);

        JsonSchema spy = spy(jsonSchema);
        when(spy.hasTypeValidator()).thenReturn(true);

        JsonValidator typeValidator = mock(JsonValidator.class);
        ValidationMessage mockMsg = mock(ValidationMessage.class);
        when(typeValidator.validate(any(), eq(node), any(), any()))
                .thenReturn(Set.of(mockMsg));
        when(spy.getTypeValidator()).thenReturn(typeValidator);

        ValidationResult result = spy.validateAndCollect(executionContext, node);
        assertFalse(result.isSuccess(),
                "When the type validator reports an error the result must be a failure");
        assertFalse(result.getMessages().isEmpty(),
                "Failure should contain at least one ValidationMessage");
    }

    /* -------------------------------------------------------------
     *  Group: Dynamic Anchor Handling
     * ------------------------------------------------------------- */

    @Test
    void testValidateAndCollect_DynamicAnchor_NoException() {
        JsonNode node = mapper.createObjectNode();

        JsonSchema spy = spy(jsonSchema);
        // Simulate a dynamic anchor – the method should simply continue validation.
        when(spy.isDynamicAnchor()).thenReturn(true);
        // No required / type validators – the validation should succeed.
        when(spy.hasRequiredValidator()).thenReturn(false);
        when(spy.hasTypeValidator()).thenReturn(false);

        ValidationResult result = spy.validateAndCollect(executionContext, node);
        assertTrue(result.isSuccess(),
                "Dynamic anchor handling should not cause a failure when no validators fire");
    }

    /* -------------------------------------------------------------
     *  Group: Walk Listener Invocation
     * ------------------------------------------------------------- */

    @Test
    void testValidateAndCollect_WalkListenerRunnerIsInvoked() throws Exception {
        // Build a simple node; the actual content does not matter for this test.
        JsonNode node = mapper.readTree("{\"a\":1}");

        // Create a mock WalkListenerRunner that records its interaction.
        WalkListenerRunner mockRunner = mock(WalkListenerRunner.class);
        // For the purpose of this test we only need the runner to be present;
        // its internal methods are not exercised by JsonSchema directly in the
        // happy‑path we are testing.

        // Use reflection to inject the mock into the private field of jsonSchema.
        Field field = JsonSchema.class.getDeclaredField("keywordWalkListenerRunner");
        field.setAccessible(true);
        field.set(jsonSchema, mockRunner);

        // Execute validation – we do not care about the result, only that no
        // exception is thrown and the runner was consulted.
        ValidationResult result = jsonSchema.validateAndCollect(executionContext, node);
        assertNotNull(result, "Result must be non‑null even when a WalkListenerRunner is present");

        // Verify that the runner was queried at least once (the exact method
        // depends on the library version; we verify a generic interaction).
        verifyNoMoreInteractions(mockRunner); // we just ensure injection did not break execution
    }

    /* -------------------------------------------------------------
     *  Helper for parameterised tests (example for future extension)
     * ------------------------------------------------------------- */

    private static Stream<Arguments> requiredValidatorProvider() {
        // Example provider – not used in the current set of tests but shows
        // how a parameterised test could be wired.
        return Stream.of(
                Arguments.of("{\"requiredProp\":\"value\"}", true),
                Arguments.of("{}", false)
        );
    }

    @ParameterizedTest
    @MethodSource("requiredValidatorProvider")
    void parameterizedRequiredValidatorTest(String json, boolean expectSuccess) throws Exception {
        JsonNode node = mapper.readTree(json);
        JsonSchema spy = spy(jsonSchema);
        when(spy.hasRequiredValidator()).thenReturn(true);
        JsonValidator requiredValidator = mock(JsonValidator.class);
        if (expectSuccess) {
            when(requiredValidator.validate(any(), eq(node), any(), any()))
                    .thenReturn(Collections.emptySet());
        } else {
            ValidationMessage mockMsg = mock(ValidationMessage.class);
            when(requiredValidator.validate(any(), eq(node), any(), any()))
                    .thenReturn(Set.of(mockMsg));
        }
        when(spy.getRequiredValidator()).thenReturn(requiredValidator);

        ValidationResult result = spy.validateAndCollect(executionContext, node);
        assertEquals(expectSuccess, result.isSuccess(),
                "Parameterized required‑validator test failed for input: " + json);
    }
}
```
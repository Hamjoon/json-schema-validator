```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for {@link JsonSchema#validateAndCollect(JsonNode)}.
 *
 * <p>
 * The tests focus on the behaviour of the public overload
 * {@code validateAndCollect(JsonNode)} – i.e. the creation of an {@link ExecutionContext},
 * the handling of internal flags/branches (dynamic anchors, $ref resolution, walk listeners,
 * at‑root flag, etc.) and the propagation of the {@link ValidationResult} produced by the
 * internal overload {@code validateAndCollect(ExecutionContext, JsonNode)}.
 * </p>
 *
 * <p>
 * Heavy schema processing is avoided by mocking all collaborators.  The real
 * {@link JsonSchema} instance is created via reflection (its constructor is package‑private)
 * and wrapped in a Mockito {@link Spy}.  Individual internal methods are stubbed
 * to simulate the various validation scenarios described in the JSON test case list.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validateAndCollect_2_Test {

    /** Spy of the {@link JsonSchema} under test. */
    private JsonSchema jsonSchemaSpy;

    /** Mocks required for constructing a real JsonSchema instance. */
    @Mock private ValidationContext mockValidationContext;
    @Mock private JsonNode mockSchemaNode;          // the schema definition node
    @Mock private JsonNode mockInstanceNode;        // the JSON instance to validate
    @Mock private ExecutionContext mockExecutionContext;
    @Mock private ValidationResult mockValidationResult;
    @Mock private WalkListenerRunner mockWalkListenerRunner;

    /** Helper constants for the private constructor. */
    private static final String SCHEMA_PATH = "#";
    private static final URI CURRENT_URI = URI.create("http://example.com/schema.json");
    private static final boolean SUPPRESS_SUB_SCHEMA_RETRIEVAL = false;

    /** Jackson helper to create JsonNode from a JSON literal. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed.
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // ----------------------------------------------------------------------
        // Build a real JsonSchema instance via reflection (the constructor is package‑private)
        // ----------------------------------------------------------------------
        java.lang.reflect.Constructor<JsonSchema> ctor =
                JsonSchema.class.getDeclaredConstructor(
                        ValidationContext.class,
                        String.class,
                        URI.class,
                        JsonNode.class,
                        JsonSchema.class,
                        boolean.class);
        ctor.setAccessible(true);
        JsonSchema realInstance = ctor.newInstance(
                mockValidationContext,
                SCHEMA_PATH,
                CURRENT_URI,
                mockSchemaNode,
                null,                                   // no parent schema
                SUPPRESS_SUB_SCHEMA_RETRIEVAL);

        // Wrap the real instance with a Mockito spy so we can stub/verify selective methods.
        jsonSchemaSpy = Mockito.spy(realInstance);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset interactions after each test to keep them independent.
        Mockito.reset(mockValidationContext, mockSchemaNode, mockInstanceNode,
                mockExecutionContext, mockValidationResult, jsonSchemaSpy, mockWalkListenerRunner);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    // -----------------------------------------------------------------------
    // Helper utilities
    // -----------------------------------------------------------------------
    /** Parses a JSON literal (or returns {@code null} when the literal is the string "null"). */
    private JsonNode json(String literal) {
        if ("null".equals(literal)) {
            return null;
        }
        try {
            return MAPPER.readTree(literal);
        } catch (Exception e) {
            throw new RuntimeException("Invalid JSON literal used in test: " + literal, e);
        }
    }

    /** Returns a {@link ValidationResult} mock configured for success/failure. */
    private ValidationResult validationResult(boolean success, Set<ValidationMessage> messages) {
        ValidationResult vr = mock(ValidationResult.class);
        when(vr.isSuccess()).thenReturn(success);
        when(vr.getMessages()).thenReturn(messages);
        return vr;
    }

    // -----------------------------------------------------------------------
    // 1. Delegation (already covered in the original test – kept for completeness)
    // -----------------------------------------------------------------------
    @Test
    void testValidateAndCollect_DelegatesToExecutionContextAndOverload() {
        doReturn(mockExecutionContext).when(jsonSchemaSpy).createExecutionContext();
        doReturn(mockValidationResult).when(jsonSchemaSpy).validateAndCollect(mockExecutionContext, mockInstanceNode);

        ValidationResult result = jsonSchemaSpy.validateAndCollect(mockInstanceNode);

        assertSame(mockValidationResult, result);
        verify(jsonSchemaSpy, times(1)).createExecutionContext();
        verify(jsonSchemaSpy, times(1)).validateAndCollect(mockExecutionContext, mockInstanceNode);
    }

    // -----------------------------------------------------------------------
    // 2. Successful validation (no validators triggered)
    // -----------------------------------------------------------------------
    @Test
    void testSuccessfulValidation_NoValidatorsTriggered() {
        JsonNode node = json("{\"name\":\"John\",\"age\":30}");
        ValidationResult successResult = validationResult(true, Collections.emptySet());

        doReturn(mockExecutionContext).when(jsonSchemaSpy).createExecutionContext();
        doReturn(successResult).when(jsonSchemaSpy).validateAndCollect(mockExecutionContext, node);

        ValidationResult result = jsonSchemaSpy.validateAndCollect(node);

        assertTrue(result.isSuccess(), "Result should be successful");
        assertTrue(result.getMessages().isEmpty(), "No messages expected");
        verify(jsonSchemaSpy).createExecutionContext();
        verify(jsonSchemaSpy).validateAndCollect(mockExecutionContext, node);
    }

    // -----------------------------------------------------------------------
    // 3. Null node handling
    // -----------------------------------------------------------------------
    @Test
    void testNullNodeHandling() {
        JsonNode node = null;
        ValidationResult failure = validationResult(false, Collections.singleton(mock(ValidationMessage.class)));

        doReturn(mockExecutionContext).when(jsonSchemaSpy).createExecutionContext();
        doReturn(failure).when(jsonSchemaSpy).validateAndCollect(mockExecutionContext, node);

        ValidationResult result = jsonSchemaSpy.validateAndCollect(node);

        assertFalse(result.isSuccess(), "Result must be failure for null node");
        assertFalse(result.getMessages().isEmpty(), "A message describing null instance expected");
    }

    // -----------------------------------------------------------------------
    // 4. Required validator failure
    // -----------------------------------------------------------------------
    @Test
    void testRequiredValidatorFailure() {
        JsonNode node = json("{\"age\":25}");
        // Simulate that the schema reports having a required validator
        doReturn(true).when(jsonSchemaSpy).hasRequiredValidator();
        // Pretend the required validator produces a failure message
        JsonValidator requiredValidator = mock(JsonValidator.class);
        ValidationMessage requiredMsg = mock(ValidationMessage.class);
        when(requiredMsg.getType()).thenReturn("required");
        when(requiredValidator.validate(any(), eq(node))).thenReturn(Set.of(requiredMsg));
        doReturn(requiredValidator).when(jsonSchemaSpy).getRequiredValidator();

        // Let the overload produce a failure ValidationResult that contains the message
        ValidationResult failure = validationResult(false, Set.of(requiredMsg));
        doReturn(mockExecutionContext).when(jsonSchemaSpy).createExecutionContext();
        doReturn(failure).when(jsonSchemaSpy).validateAndCollect(mockExecutionContext, node);

        ValidationResult result = jsonSchemaSpy.validateAndCollect(node);

        assertFalse(result.isSuccess(), "Required validator should cause failure");
        assertTrue(result.getMessages().stream().anyMatch(m -> "required".equals(m.getType())),
                "Message type must be 'required'");
        verify(jsonSchemaSpy).hasRequiredValidator();
        verify(jsonSchemaSpy).getRequiredValidator();
    }

    // -----------------------------------------------------------------------
    // 5. Type validator failure
    // -----------------------------------------------------------------------
    @Test
    void testTypeValidatorFailure() {
        JsonNode node = json("{\"name\":12345}");
        doReturn(true).when(jsonSchemaSpy).hasTypeValidator();
        JsonValidator typeValidator = mock(JsonValidator.class);
        ValidationMessage typeMsg = mock(ValidationMessage.class);
        when(typeMsg.getType()).thenReturn("type");
        when(typeValidator.validate(any(), eq(node))).thenReturn(Set.of(typeMsg));
        doReturn(typeValidator).when(jsonSchemaSpy).getTypeValidator();

        ValidationResult failure = validationResult(false, Set.of(typeMsg));
        doReturn(mockExecutionContext).when(jsonSchemaSpy).createExecutionContext();
        doReturn(failure).when(jsonSchemaSpy).validateAndCollect(mockExecutionContext, node);

        ValidationResult result = jsonSchemaSpy.validateAndCollect(node);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessages().stream().anyMatch(m -> "type".equals(m.getType())));
        verify(jsonSchemaSpy).hasTypeValidator();
        verify(jsonSchemaSpy).getTypeValidator();
    }

    // -----------------------------------------------------------------------
    // 6. Dynamic anchor handling
    // -----------------------------------------------------------------------
    @Test
    void testDynamicAnchorHandling() {
        JsonNode node = json("{\"id\":\"#myAnchor\",\"value\":\"test\"}");

        // Simulate a dynamic‑anchor schema
        doReturn(true).when(jsonSchemaSpy).isDynamicAnchor();

        // ExecutionContext mock should receive a registration call – we verify later
        doNothing().when(mockExecutionContext).registerDynamicAnchor(anyString());

        // Return a successful result from the overload
        ValidationResult success = validationResult(true, Collections.emptySet());
        doReturn(mockExecutionContext).when(jsonSchemaSpy).createExecutionContext();
        doReturn(success).when(jsonSchemaSpy).validateAndCollect(mockExecutionContext, node);

        ValidationResult result = jsonSchemaSpy.validateAndCollect(node);

        assertTrue(result.isSuccess());
        // Verify that the dynamic anchor registration was attempted
        verify(jsonSchemaSpy).isDynamicAnchor();
        verify(mockExecutionContext).registerDynamicAnchor("#myAnchor");
    }

    // -----------------------------------------------------------------------
    // 7. Keyword walk listener execution
    // -----------------------------------------------------------------------
    @Test
    void testKeywordWalkListenerExecution() throws Exception {
        JsonNode node = json("{\"name\":\"Alice\"}");

        // inject a non‑null WalkListenerRunner into the spy
        java.lang.reflect.Field f = JsonSchema.class.getDeclaredField("keywordWalkListenerRunner");
        f.setAccessible(true);
        f.set(jsonSchemaSpy, mockWalkListenerRunner);

        // stub callbacks – they do nothing but we verify invocation count
        doNothing().when(mockWalkListenerRunner).onWalkStart(any(), any());
        doNothing().when(mockWalkListenerRunner).onWalkEnd(any(), any());

        // Normal successful validation path
        ValidationResult success = validationResult(true, Collections.emptySet());
        doReturn(mockExecutionContext).when(jsonSchemaSpy).createExecutionContext();
        doReturn(success).when(jsonSchemaSpy).validateAndCollect(mockExecutionContext, node);

        ValidationResult result = jsonSchemaSpy.validateAndCollect(node);

        assertTrue(result.isSuccess());
        verify(mockWalkListenerRunner, times(1)).onWalkStart(eq(jsonSchemaSpy), eq(node));
        verify(mockWalkListenerRunner, times(1)).onWalkEnd(eq(jsonSchemaSpy), eq(node));
    }

    // -----------------------------------------------------------------------
    // 8. $ref handling – success
    // -----------------------------------------------------------------------
    @Test
    void testRefResolutionSuccess() {
        JsonNode node = json("{\"$ref\":\"#/components/schemas/Address\"}");

        // Simulate successful resolution
        JsonNode refSchemaNode = json("{\"type\":\"object\"}");
        when(jsonSchemaSpy.getRefSchemaNode("#/components/schemas/Address")).thenReturn(refSchemaNode);

        // The internal overload will validate the resolved schema – we stub it to succeed
        ValidationResult success = validationResult(true, Collections.emptySet());
        doReturn(mockExecutionContext).when(jsonSchemaSpy).createExecutionContext();
        doReturn(success).when(jsonSchemaSpy).validateAndCollect(mockExecutionContext, refSchemaNode);

        ValidationResult result = jsonSchemaSpy.validateAndCollect(node);

        assertTrue(result.isSuccess());
        verify(jsonSchemaSpy).getRefSchemaNode("#/components/schemas/Address");
    }

    // -----------------------------------------------------------------------
    // 9. $ref handling – unresolved reference
    // -----------------------------------------------------------------------
    @Test
    void testRefResolutionFailure() {
        JsonNode node = json("{\"$ref\":\"#/nonexistent\"}");

        when(jsonSchemaSpy.getRefSchemaNode("#/nonexistent")).thenReturn(null);

        ValidationResult failure = validationResult(false, Collections.singleton(mock(Validation
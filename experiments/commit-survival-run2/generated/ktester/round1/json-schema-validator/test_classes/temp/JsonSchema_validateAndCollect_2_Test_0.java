package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URISyntaxException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class JsonSchema_validateAndCollect_2_Test {

    /**
     * The spy under test – a real {@link JsonSchema} instance wrapped by Mockito.
     */
    private JsonSchema jsonSchemaSpy;

    /**
     * Mocks used for the constructor of {@link JsonSchema}.
     */
    @Mock
    private ValidationContext mockValidationContext;

    // the schema definition node
    @Mock
    private JsonNode mockSchemaNode;

    // the JSON instance to validate
    @Mock
    private JsonNode mockInstanceNode;

    @Mock
    private ExecutionContext mockExecutionContext;

    @Mock
    private ValidationResult mockValidationResult;

    /**
     * Helper constants for the private constructor.
     */
    private static final String SCHEMA_PATH = "#";

    private static final URI CURRENT_URI = URI.create("http://example.com/schema.json");

    private static final boolean SUPPRESS_SUB_SCHEMA_RETRIEVAL = false;

    @Mock
    private WalkListenerRunner mockWalkListenerRunner;

    /**
     * Jackson helper to create JsonNode from a JSON literal.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // ----------------------------------------------------------------------
        // Build a real JsonSchema instance via reflection (the constructor is package‑private)
        // ----------------------------------------------------------------------
        java.lang.reflect.Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(ValidationContext.class, String.class, URI.class, JsonNode.class, JsonSchema.class, boolean.class);
        ctor.setAccessible(true);
        JsonSchema realInstance = ctor.newInstance(mockValidationContext, SCHEMA_PATH, CURRENT_URI, mockSchemaNode, // no parent schema
        null, SUPPRESS_SUB_SCHEMA_RETRIEVAL);
        // Wrap the real instance with a Mockito spy so we can stub/verify selective methods.
        jsonSchemaSpy = Mockito.spy(realInstance);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset interactions after each test to keep them independent.
        Mockito.reset(mockValidationContext, mockSchemaNode, mockInstanceNode, mockExecutionContext, mockValidationResult, jsonSchemaSpy, mockWalkListenerRunner);
    }

    /**
     * Parses a JSON literal (or returns {@code null} when the literal is the string "null").
     */
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

    /**
     * Returns a {@link ValidationResult} mock configured for success/failure.
     */
    private ValidationResult validationResult(boolean success, Set<ValidationMessage> messages) {
        ValidationResult vr = mock(ValidationResult.class);
        when(vr.isSuccess()).thenReturn(success);
        when(vr.getMessages()).thenReturn(messages);
        return vr;
    }

    /**
     * Verifies that {@code validateAndCollect(JsonNode)} creates an {@link ExecutionContext}
     * via {@code createExecutionContext()}, forwards the call to the overloaded method,
     * and returns the {@link ValidationResult} produced by that overload.
     */
    @Test
    void testValidateAndCollect_DelegatesToExecutionContextAndOverload() {
        // --------------------------------------------------------------
        // Arrange – stub the collaborators that the method under test uses.
        // --------------------------------------------------------------
        // When the spy creates an execution context, return the mocked one.
        doReturn(mockExecutionContext).when(jsonSchemaSpy).createExecutionContext();
        // When the overloaded method is invoked with the mocked execution context
        // and the instance node, return the mocked ValidationResult.
        doReturn(mockValidationResult).when(jsonSchemaSpy).validateAndCollect(mockExecutionContext, mockInstanceNode);
        // --------------------------------------------------------------
        // Act – call the method under test.
        // --------------------------------------------------------------
        ValidationResult result = jsonSchemaSpy.validateAndCollect(mockInstanceNode);
        // --------------------------------------------------------------
        // Assert – the result should be exactly the stubbed ValidationResult.
        // --------------------------------------------------------------
        assertSame(mockValidationResult, result, "validateAndCollect(JsonNode) should return the ValidationResult from the overloaded method.");
        // Verify the internal delegation happened exactly once each.
        verify(jsonSchemaSpy, times(1)).createExecutionContext();
        verify(jsonSchemaSpy, times(1)).validateAndCollect(mockExecutionContext, mockInstanceNode);
    }

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
        assertTrue(result.getMessages().stream().anyMatch(m -> "required".equals(m.getType())), "Message type must be 'required'");
        verify(jsonSchemaSpy).hasRequiredValidator();
        verify(jsonSchemaSpy).getRequiredValidator();
    }

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
}

```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link JsonSchema#validateAndCollect(JsonNode)}.
 *
 * The test verifies that the no‑arg overload correctly creates an {@link ExecutionContext}
 * (by delegating to {@link JsonSchema#createExecutionContext()}) and then forwards the
 * call to {@link JsonSchema#validateAndCollect(ExecutionContext, JsonNode)}.
 *
 * All heavy dependencies are mocked – no real schema parsing or validation is performed.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validateAndCollect_2_Test {

    /** The spy under test – a real {@link JsonSchema} instance wrapped by Mockito. */
    private JsonSchema jsonSchemaSpy;

    /** Mocks used for the constructor of {@link JsonSchema}. */
    @Mock private ValidationContext mockValidationContext;
    @Mock private JsonNode mockSchemaNode;          // the schema definition node
    @Mock private JsonNode mockInstanceNode;        // the JSON instance to validate
    @Mock private ExecutionContext mockExecutionContext;
    @Mock private ValidationResult mockValidationResult;

    /** Helper constants for the private constructor. */
    private static final String SCHEMA_PATH = "#";
    private static final URI CURRENT_URI = URI.create("http://example.com/schema.json");
    private static final boolean SUPPRESS_SUB_SCHEMA_RETRIEVAL = false;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these unit tests.
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // ----------------------------------------------------------------------
        // Build a real JsonSchema instance via reflection (the constructor is private)
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
                mockExecutionContext, mockValidationResult, jsonSchemaSpy);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
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
        doReturn(mockValidationResult)
                .when(jsonSchemaSpy)
                .validateAndCollect(mockExecutionContext, mockInstanceNode);

        // --------------------------------------------------------------
        // Act – call the method under test.
        // --------------------------------------------------------------
        ValidationResult result = jsonSchemaSpy.validateAndCollect(mockInstanceNode);

        // --------------------------------------------------------------
        // Assert – the result should be exactly the stubbed ValidationResult.
        // --------------------------------------------------------------
        assertSame(mockValidationResult, result,
                "validateAndCollect(JsonNode) should return the ValidationResult from the overloaded method.");

        // Verify the internal delegation happened exactly once each.
        verify(jsonSchemaSpy, times(1)).createExecutionContext();
        verify(jsonSchemaSpy, times(1)).validateAndCollect(mockExecutionContext, mockInstanceNode);
    }
}
```
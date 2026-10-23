```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PropertiesValidator#validate(ExecutionContext, JsonNode, JsonNode, String)}.
 *
 * The tests use Mockito to isolate {@link PropertiesValidator} from the rest of the library.
 * Only the public API of the collaborators is stubbed – no private fields or methods are accessed.
 */
@ExtendWith(MockitoExtension.class)
class PropertiesValidator_validate_Test {

    /** The validator under test. */
    private PropertiesValidator validator;

    /** Mocked collaborators. */
    @Mock ValidationContext validationContext;
    @Mock JsonSchema parentSchema;
    @Mock JsonSchema propertySchema;               // schema for the single property defined in the test schema
    @Mock ExecutionContext executionContext;
    @Mock CollectorContext collectorContext;
    @Mock ValidatorState validatorState;

    /** Helper objects used in several tests. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // -----------------------------------------------------------------
        // 1️⃣  Prepare a minimal schema node that defines a single property
        // -----------------------------------------------------------------
        ObjectNode schemaNode = MAPPER.createObjectNode();
        ObjectNode propDef = MAPPER.createObjectNode(); // empty definition – the content is irrelevant for the test
        schemaNode.set("prop", propDef);

        // -----------------------------------------------------------------
        // 2️⃣  Stub ValidationContext#newSchema to return the mocked property schema
        // -----------------------------------------------------------------
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)))
                .thenReturn(propertySchema);

        // -----------------------------------------------------------------
        // 3️⃣  Build the validator instance (the constructor populates the
        //     internal `schemas` map using the stubbed ValidationContext)
        // -----------------------------------------------------------------
        validator = new PropertiesValidator("test-schema", schemaNode, parentSchema, validationContext);

        // -----------------------------------------------------------------
        // 4️⃣  Prepare ExecutionContext → CollectorContext → ValidatorState chain
        // -----------------------------------------------------------------
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        when(collectorContext.getEvaluatedProperties()).thenReturn(new HashSet<>());
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);

        // Default behaviour for the validator state – non‑complex, walker disabled
        when(validatorState.isComplexValidator()).thenReturn(false);
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.isValidationEnabled()).thenReturn(true);
        // The mutator methods are void; we simply let them do nothing.
        doNothing().when(validatorState).setComplexValidator(anyBoolean());
        doNothing().when(validatorState).setMatchedNode(anyBoolean());

        // -----------------------------------------------------------------
        // 5️⃣  Parent schema – by default it has no required validator
        // -----------------------------------------------------------------
        when(parentSchema.hasRequiredValidator()).thenReturn(false);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test resources to clean up.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up.
    }

    /* --------------------------------------------------------------------- *
     *  Test 1 – property is present, validation of the child schema succeeds *
     * --------------------------------------------------------------------- */
    @Test
    void testValidateWhenPropertyPresentAndChildValid() {
        // Arrange ---------------------------------------------------------
        ObjectNode dataNode = MAPPER.createObjectNode();
        dataNode.put("prop", "any-value"); // property defined in the schema

        // Child schema validation returns an empty set (i.e. no errors)
        when(propertySchema.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.emptySet());

        // Act -------------------------------------------------------------
        Set<ValidationMessage> result = validator.validate(executionContext,
                dataNode,               // node
                dataNode,               // rootNode (same for this simple test)
                "");                    // at (empty path)

        // Assert ----------------------------------------------------------
        assertNotNull(result);
        assertTrue(result.isEmpty(), "No validation messages are expected when the property validates successfully");

        // Verify that the child schema's validate method was invoked exactly once
        verify(propertySchema, times(1))
                .validate(eq(executionContext), eq(dataNode.get("prop")), eq(dataNode), eq(""));
    }

    /* --------------------------------------------------------------------- *
     *  Test 2 – property is missing and the parent schema has a required   *
     *           validator that reports an error.                           *
     * --------------------------------------------------------------------- */
    @Test
    void testValidateWhenPropertyMissingAndRequiredFails() {
        // Arrange ---------------------------------------------------------
        ObjectNode dataNode = MAPPER.createObjectNode(); // empty object – no "prop" field

        // Simulate a required validator on the parent schema
        RequiredValidator requiredValidator = mock(RequiredValidator.class);
        ValidationMessage requiredMessage = mock(ValidationMessage.class);
        Set<ValidationMessage> requiredErrors = new HashSet<>();
        requiredErrors.add(requiredMessage);
        when(requiredValidator.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(requiredErrors);

        when(parentSchema.hasRequiredValidator()).thenReturn(true);
        when(parentSchema.getRequiredValidator()).thenReturn(requiredValidator);

        // Act -------------------------------------------------------------
        Set<ValidationMessage> result = validator.validate(executionContext,
                dataNode, dataNode, "");

        // Assert ----------------------------------------------------------
        assertNotNull(result);
        assertEquals(1, result.size(), "Exactly one validation message (the required error) should be returned");
        assertTrue(result.contains(requiredMessage), "The returned set must contain the required validator message");

        // Verify that the child schema's validate method was never called because the property is absent
        verify(propertySchema, never())
                .validate(any(), any(), any(), anyString());
    }

    /* --------------------------------------------------------------------- *
     *  Test 3 – complex validator scenario (e.g. oneOf) where the state is *
     *           marked as complex. In this simplified test we only verify *
     *           that the state flag is restored after the recursive call. *
     * --------------------------------------------------------------------- */
    @Test
    void testValidateComplexValidatorFlagIsRestored() {
        // Arrange ---------------------------------------------------------
        ObjectNode dataNode = MAPPER.createObjectNode();
        dataNode.put("prop", "value");

        // Make the validator state report that we are inside a complex validator
        when(validatorState.isComplexValidator()).thenReturn(true);
        // Child validation still succeeds
        when(propertySchema.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.emptySet());

        // Act -------------------------------------------------------------
        Set<ValidationMessage> result = validator.validate(executionContext,
                dataNode, dataNode, "");

        // Assert ----------------------------------------------------------
        assertNotNull(result);
        assertTrue(result.isEmpty(), "No errors expected when child validation succeeds");

        // Verify that the complex flag was queried, cleared, and then restored
        InOrder inOrder = inOrder(validatorState);
        // original value queried
        inOrder.verify(validatorState).isComplexValidator();
        // cleared before recursion
        inOrder.verify(validatorState).setComplexValidator(false);
        // restored after recursion
        inOrder.verify(validatorState).setComplexValidator(true);
    }
}
```
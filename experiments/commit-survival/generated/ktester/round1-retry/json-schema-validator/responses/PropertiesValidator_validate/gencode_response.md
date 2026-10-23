```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for {@link PropertiesValidator#validate(ExecutionContext, JsonNode, JsonNode, String)}.
 *
 * The tests cover the scenarios described in the supplied JSON test‑case list.
 * Mockito is used to isolate the validator from its collaborators.
 */
@ExtendWith(MockitoExtension.class)
class PropertiesValidator_validate_Test {

    /** The validator under test – created in {@link #setupBeforeEach()}. */
    private PropertiesValidator validator;

    /** Mocked collaborators. */
    @Mock ValidationContext validationContext;
    @Mock JsonSchema parentSchema;
    @Mock JsonSchema propertySchema;               // schema for any property defined in the test schema
    @Mock ExecutionContext executionContext;
    @Mock CollectorContext collectorContext;
    @Mock ValidatorState validatorState;

    /** Helper objects used in several tests. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required.
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // -----------------------------------------------------------------
        // 1️⃣  Minimal schema node that defines a single property "prop"
        // -----------------------------------------------------------------
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.set("prop", MAPPER.createObjectNode());

        // -----------------------------------------------------------------
        // 2️⃣  Stub ValidationContext#newSchema to always return the same mock
        // -----------------------------------------------------------------
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)))
                .thenReturn(propertySchema);

        // -----------------------------------------------------------------
        // 3️⃣  Build the validator instance. The constructor populates the
        //     internal `schemas` map using the stubbed ValidationContext.
        // -----------------------------------------------------------------
        validator = new PropertiesValidator("test-schema", schemaNode, parentSchema, validationContext);

        // -----------------------------------------------------------------
        // 4️⃣  ExecutionContext → CollectorContext → ValidatorState chain
        // -----------------------------------------------------------------
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        when(collectorContext.getEvaluatedProperties()).thenReturn(new HashSet<>());
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);

        // Default state: non‑complex, walk disabled, validation enabled
        when(validatorState.isComplexValidator()).thenReturn(false);
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.isValidationEnabled()).thenReturn(true);
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
        // Arrange
        ObjectNode dataNode = MAPPER.createObjectNode();
        dataNode.put("prop", "any-value");

        when(propertySchema.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.emptySet());

        // Act
        Set<ValidationMessage> result = validator.validate(executionContext, dataNode, dataNode, "");

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty(), "No validation messages are expected when the property validates successfully");
        verify(propertySchema, times(1))
                .validate(eq(executionContext), eq(dataNode.get("prop")), eq(dataNode), eq(""));
    }

    /* --------------------------------------------------------------------- *
     *  Test 2 – property is missing and the parent schema has a required   *
     *           validator that reports an error.                           *
     * --------------------------------------------------------------------- */
    @Test
    void testValidateWhenPropertyMissingAndRequiredFails() {
        // Arrange
        ObjectNode dataNode = MAPPER.createObjectNode(); // empty object

        RequiredValidator requiredValidator = mock(RequiredValidator.class);
        ValidationMessage requiredMessage = mock(ValidationMessage.class);
        Set<ValidationMessage> requiredErrors = new HashSet<>(Collections.singleton(requiredMessage));
        when(requiredValidator.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(requiredErrors);

        when(parentSchema.hasRequiredValidator()).thenReturn(true);
        when(parentSchema.getRequiredValidator()).thenReturn(requiredValidator);

        // Act
        Set<ValidationMessage> result = validator.validate(executionContext, dataNode, dataNode, "");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains(requiredMessage));
        verify(propertySchema, never()).validate(any(), any(), any(), anyString());
    }

    /* --------------------------------------------------------------------- *
     *  Test 3 – complex validator scenario (e.g. oneOf) where the state is *
     *           marked as complex. Verify flag restoration.                *
     * --------------------------------------------------------------------- */
    @Test
    void testValidateComplexValidatorFlagIsRestored() {
        // Arrange
        ObjectNode dataNode = MAPPER.createObjectNode();
        dataNode.put("prop", "value");

        when(validatorState.isComplexValidator()).thenReturn(true);
        when(propertySchema.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.emptySet());

        // Act
        Set<ValidationMessage> result = validator.validate(executionContext, dataNode, dataNode, "");

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());

        InOrder inOrder = inOrder(validatorState);
        inOrder.verify(validatorState).isComplexValidator();          // original query
        inOrder.verify(validatorState).setComplexValidator(false);   // cleared before recursion
        inOrder.verify(validatorState).setComplexValidator(true);    // restored after recursion
    }

    /* --------------------------------------------------------------------- *
     *  Walk enabled – property present, validator should delegate to       *
     *  walkSchema (child schema must NOT be called).                       *
     * --------------------------------------------------------------------- */
    @Test
    void testValidateWhenWalkEnabledPropertyPresent() {
        // Arrange
        ObjectNode dataNode = MAPPER.createObjectNode();
        dataNode.put("age", 30);

        when(validatorState.isWalkEnabled()).thenReturn(true);
        // child schema should never be invoked
        when(propertySchema.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.singleton(mock(ValidationMessage.class)));

        // Act
        Set<ValidationMessage> result = validator.validate(executionContext, dataNode, dataNode, "#");

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty(), "When walk is enabled the validator does not call the child schema directly");
        verify(propertySchema, never()).validate(any(), any(), any(), anyString());
    }

    /* --------------------------------------------------------------------- *
     *  Required validator returns no errors (property absent).             *
     * --------------------------------------------------------------------- */
    @Test
    void testValidateWhenPropertyAbsentRequiredValidatorReturnsNoErrors() {
        // Arrange
        ObjectNode dataNode = MAPPER.createObjectNode(); // empty

        RequiredValidator requiredValidator = mock(RequiredValidator.class);
        when(requiredValidator.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.emptySet());

        when(parentSchema.hasRequiredValidator()).thenReturn(true);
        when(parentSchema.getRequiredValidator()).thenReturn(requiredValidator);

        // Act
        Set<ValidationMessage> result = validator.validate(executionContext, dataNode, dataNode, "#");

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty(), "No errors should be returned when required validator yields an empty set");
        verify(propertySchema, never()).validate(any(), any(), any(), anyString());
    }

    /* --------------------------------------------------------------------- *
     *  Required validator returns errors, validator is NOT complex.       *
     * --------------------------------------------------------------------- */
    @Test
    void testValidateWhenPropertyAbsentRequiredValidatorReturnsErrorsNonComplex() {
        // Arrange
        ObjectNode dataNode = MAPPER.createObjectNode(); // empty

        RequiredValidator requiredValidator = mock(RequiredValidator.class);
        ValidationMessage msg = mock(ValidationMessage.class);
        Set<ValidationMessage> errors = new HashSet<>(Collections.singleton(msg));
        when(requiredValidator.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(errors);

        when(parentSchema.hasRequiredValidator()).thenReturn(true);
        when(parentSchema.getRequiredValidator()).thenReturn(requiredValidator);
        when(validatorState.isComplexValidator()).thenReturn(false);

        // Act
        Set<ValidationMessage> result = validator.validate(executionContext, dataNode, dataNode, "#");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains(msg));
    }

    /* --------------------------------------------------------------------- *
     *  Required validator returns errors, validator IS complex – early  *
     *  exit and matchedNode set to false.                                   *
     * --------------------------------------------------------------------- */
    @Test
    void testValidateWhenPropertyAbsentRequiredValidatorReturnsErrorsComplex() {
        // Arrange
        ObjectNode dataNode = MAPPER.createObjectNode(); // empty

        RequiredValidator requiredValidator = mock(RequiredValidator.class);
        ValidationMessage msg = mock(ValidationMessage.class);
        Set<ValidationMessage> errors = new HashSet<>(Collections.singleton(msg));
        when(requiredValidator.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(errors);

        when(parentSchema.hasRequiredValidator()).thenReturn(true);
        when(parentSchema.getRequiredValidator()).thenReturn(requiredValidator);
        when(validatorState.isComplexValidator()).thenReturn(true);

        // Act
        Set<ValidationMessage> result = validator.validate(executionContext, dataNode, dataNode, "#");

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty(), "Complex validator should return early with an empty set");
        verify(validatorState).setMatchedNode(false);
    }

    /* --------------------------------------------------------------------- *
     *  No schemas defined – the internal map is empty.                     *
     * --------------------------------------------------------------------- */
    @Test
    void testValidateWhenNoSchemasDefined() throws Exception {
        // Arrange – clear the internal `schemas` map via reflection
        Field schemasField = PropertiesValidator.class.getDeclaredField("schemas");
        schemasField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, JsonSchema> map = (Map<String, JsonSchema>) schemasField.get(validator);
        map.clear();

        ObjectNode dataNode = MAPPER.createObjectNode(); // any node

        // Act
        Set<ValidationMessage> result = validator.validate(executionContext, dataNode, dataNode, "#");

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty(), "With no property schemas the validator must return an empty set");
        assertTrue(result.getClass().getName().contains("Unmodifiable"), "Result set should be immutable");
    }

    /* --------------------------------------------------------------------- *
     *  Parent schema without required validator, property absent.          *
     * --------------------------------------------------------------------- */
    @Test
    void testValidateWhenParentHasNoRequiredValidatorAndPropertyAbsent() {
        // Arrange
        ObjectNode dataNode = MAPPER.createObjectNode(); // empty
        when(parentSchema.hasRequiredValidator()).thenReturn(false);

        // Act
        Set<ValidationMessage> result = validator.validate(executionContext, dataNode, dataNode, "#");

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty(), "Missing property should be ignored when parent has no required validator");
    }

    /* --------------------------------------------------------------------- *
     *  Walk enabled – simulate errors returned from walkSchema.            *
     *  Since walkSchema is private we cannot stub it directly; we verify *
     *  that the child schema is *not* invoked and the result is empty.   *
     * --------------------------------------------------------------------- */
    @Test
    void testValidateWalkEnabledWithErrorsFromWalkSchema() {
        // Arrange
        ObjectNode dataNode = MAPPER.createObjectNode();
        dataNode.put("age", 30);
        when(validatorState.isWalkEnabled()).thenReturn(true);

        // Act
        Set<ValidationMessage> result = validator.validate(executionContext, dataNode, dataNode, "#");

        // Assert
        assertNotNull(result);
        // In the real implementation walkSchema would add errors; here we only ensure
        // that the validator does not call the child schema directly.
        assertTrue(result.isEmpty());
        verify(propertySchema, never()).validate(any(), any(), any(), anyString());
    }

    /* --------------------------------------------------------------------- *
     *  Multiple properties – one present, one missing with required error. *
     * --------------------------------------------------------------------- */
    @Test
    void testValidateMultiplePropertiesMixedPresence() throws Exception {
        // Arrange – build a schema node with two properties
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.set("presentProp", MAPPER.createObjectNode());
        schemaNode.set("missingProp", MAPPER.createObjectNode());

        // Re‑create validator so that its internal map contains both entries
        validator = new PropertiesValidator("test-schema", schemaNode, parentSchema, validationContext);
        // stub the child schema validation for "presentProp"
        when(propertySchema.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenAnswer(invocation -> {
                    String at = invocation.getArgument(3);
                    if (at.endsWith("presentProp")) {
                        ValidationMessage vm = mock(ValidationMessage.class);
                        when(vm.getMessage()).thenReturn("presentError");
                        return new HashSet<>(Collections.singleton(vm));
                    }
                    return Collections.emptySet();
                });

        // Required validator for the missing property
        RequiredValidator requiredValidator = mock(RequiredValidator.class);
        ValidationMessage requiredMsg = mock(ValidationMessage.class);
        when(requiredMsg.getMessage()).thenReturn("'missingProp' is required");
        Set<ValidationMessage> requiredErrors = new HashSet<>(Collections.singleton(requiredMsg));
        when(requiredValidator.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(requiredErrors);
        when(parentSchema.hasRequiredValidator()).thenReturn(true);
        when(parentSchema.getRequiredValidator()).thenReturn(requiredValidator);

        // Data node contains only the present property
        ObjectNode dataNode = MAPPER.createObjectNode();
        dataNode.put("presentProp", "value");

        // Act
        Set<ValidationMessage> result = validator.validate(executionContext, dataNode, dataNode, "#");

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size(), "Both the child error and the required error should be present");
        assertTrue(result.stream().anyMatch(vm -> "presentError".equals(vm.getMessage())));
        assertTrue(result.stream().anyMatch(vm -> "'missingProp' is required".equals(vm.getMessage())));

        // Evaluated properties should contain only the present one
        Set<String> evaluated = collectorContext.getEvaluatedProperties();
        assertEquals(1, evaluated.size());
        assertTrue(evaluated.contains("#/presentProp"));
    }

    /* --------------------------------------------------------------------- *
     *  Property present – non‑complex validator, matchedNode unchanged.    *
     * --------------------------------------------------------------------- */
    @Test
    void testValidatePropertyPresentNonComplexMatchedNodeUnchanged() {
        // Arrange
        ObjectNode dataNode = MAPPER.createObjectNode();
        dataNode.put("simple", "value");

        when(validatorState.isComplexValidator()).thenReturn(false);
        when(propertySchema.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.emptySet());

        // Act
        Set<ValidationMessage> result = validator.validate(executionContext, dataNode, dataNode, "#");

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        // matchedNode should never be toggled for a non‑complex validator
        verify(validatorState, never()).setMatchedNode(anyBoolean());
    }

    /* --------------------------------------------------------------------- *
     *  Null ExecutionContext – expect NullPointerException.               *
     * --------------------------------------------------------------------- */
    @Test
    void testValidateNullExecutionContext() {
        // Arrange
        ObjectNode dataNode = MAPPER.createObjectNode();
        dataNode.put("any", "value");

        // Act & Assert
        assertThrows(NullPointerException.class,
                () -> validator.validate(null, dataNode, dataNode, "#"));
    }

    /* --------------------------------------------------------------------- *
     *  Null node argument – expect NullPointerException.                  *
     * --------------------------------------------------------------------- */
    @Test
    void testValidateNullNode() {
        // Act & Assert
        assertThrows(NullPointerException.class,
                () -> validator.validate(executionContext, null, MAPPER.createObjectNode(), "#"));
    }

    /* --------------------------------------------------------------------- *
     *  CollectorContext missing ValidatorState – expect NPE or CCE.       *
     * --------------------------------------------------------------------- */
    @Test
    void testValidateCollectorContextMissingValidatorState() {
        // Arrange
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(null);
        ObjectNode dataNode = MAPPER.createObjectNode();
        dataNode.put("prop", "value");

        // Act & Assert
        assertThrows(NullPointerException.class,
                () -> validator.validate(executionContext, dataNode, dataNode, "#"));
    }

    /* --------------------------------------------------------------------- *
     *  Null parent schema – expect NullPointerException when required    *
     *  validator is accessed.                                             *
     * --------------------------------------------------------------------- */
    @Test
    void testValidateNullParentSchema() throws Exception {
        // Arrange – construct a validator with a null parent schema
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.set("anyProp", MAPPER.createObjectNode());
        validator = new PropertiesValidator("test-schema", schemaNode, null, validationContext);

        ObjectNode dataNode = MAPPER.createObjectNode(); // property absent

        // Act & Assert
        assertThrows(NullPointerException.class,
                () -> validator.validate(executionContext, dataNode, dataNode, "#"));
    }

    /* --------------------------------------------------------------------- *
     *  Null "at" path – expect NullPointerException inside atPath.       *
     * --------------------------------------------------------------------- */
    @Test
    void testValidateNullAtPath() {
        // Arrange
        ObjectNode dataNode = MAPPER.createObjectNode();
        dataNode.put("name", "value");

        // Act & Assert
        assertThrows(NullPointerException.class,
                () -> validator.validate(executionContext, dataNode, dataNode, null));
    }

    /* --------------------------------------------------------------------- *
     *  Parameterized test source for walk‑enabled property present case.  *
     * --------------------------------------------------------------------- */
    static Stream<Arguments> walkEnabledProvider() {
        return Stream.of(
                Arguments.of("#/age", "age"),
                Arguments.of("#/name", "name")
        );
    }

    @ParameterizedTest
    @MethodSource("walkEnabledProvider")
    void testValidateWalkEnabledParameterized(String atPath, String propertyName) {
        // Arrange
        ObjectNode dataNode = MAPPER.createObjectNode();
        dataNode.put(propertyName, "dummy");
        when(validatorState.isWalkEnabled()).thenReturn(true);

        // Act
        Set<ValidationMessage> result = validator.validate(executionContext, dataNode, dataNode, atPath);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(propertySchema, never()).validate(any(), any(), any(), anyString());
    }
}
```

You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.*;
import org.junit.jupiter.params.provider.Arguments;

@ExtendWith(MockitoExtension.class)
class PropertiesValidator_validate_Test {

    /**
     * The validator under test.
     */
    private PropertiesValidator validator;

    /**
     * Mocked collaborators.
     */
    @Mock
    ValidationContext validationContext;

    @Mock
    JsonSchema parentSchema;

    // schema for the single property defined in the test schema
    @Mock
    JsonSchema propertySchema;

    @Mock
    ExecutionContext executionContext;

    @Mock
    CollectorContext collectorContext;

    @Mock
    ValidatorState validatorState;

    /**
     * Helper objects used in several tests.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeEach
    void setupBeforeEach() {
        // -----------------------------------------------------------------
        // 1️⃣  Prepare a minimal schema node that defines a single property
        // -----------------------------------------------------------------
        ObjectNode schemaNode = MAPPER.createObjectNode();
        // empty definition – the content is irrelevant for the test
        ObjectNode propDef = MAPPER.createObjectNode();
        schemaNode.set("prop", propDef);
        // -----------------------------------------------------------------
        // 2️⃣  Stub ValidationContext#newSchema to return the mocked property schema
        // -----------------------------------------------------------------
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class))).thenReturn(propertySchema);
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

    /* --------------------------------------------------------------------- *
     *  Parameterized test source for walk‑enabled property present case.  *
     * --------------------------------------------------------------------- */
    static Stream<Arguments> walkEnabledProvider() {
        return Stream.of(Arguments.of("#/age", "age"), Arguments.of("#/name", "name"));
    }

    /* --------------------------------------------------------------------- *
     *  Test 1 – property is present, validation of the child schema succeeds *
     * --------------------------------------------------------------------- */
    @Test
    void testValidateWhenPropertyPresentAndChildValid() {
        // Arrange ---------------------------------------------------------
        ObjectNode dataNode = MAPPER.createObjectNode();
        // property defined in the schema
        dataNode.put("prop", "any-value");
        // Child schema validation returns an empty set (i.e. no errors)
        when(propertySchema.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Collections.emptySet());
        // Act -------------------------------------------------------------
        Set<ValidationMessage> result = validator.validate(executionContext, // node
        dataNode, // rootNode (same for this simple test)
        dataNode, // at (empty path)
        "");
        // Assert ----------------------------------------------------------
        assertNotNull(result);
        assertTrue(result.isEmpty(), "No validation messages are expected when the property validates successfully");
        // Verify that the child schema's validate method was invoked exactly once
        verify(propertySchema, times(1)).validate(eq(executionContext), eq(dataNode.get("prop")), eq(dataNode), eq(""));
    }

    /* --------------------------------------------------------------------- *
     *  Test 2 – property is missing and the parent schema has a required   *
     *           validator that reports an error.                           *
     * --------------------------------------------------------------------- */
    @Test
    void testValidateWhenPropertyMissingAndRequiredFails() {
        // Arrange ---------------------------------------------------------
        // empty object – no "prop" field
        ObjectNode dataNode = MAPPER.createObjectNode();
        // Simulate a required validator on the parent schema
        RequiredValidator requiredValidator = mock(RequiredValidator.class);
        ValidationMessage requiredMessage = mock(ValidationMessage.class);
        Set<ValidationMessage> requiredErrors = new HashSet<>();
        requiredErrors.add(requiredMessage);
        when(requiredValidator.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(requiredErrors);
        when(parentSchema.hasRequiredValidator()).thenReturn(true);
        when(parentSchema.getRequiredValidator()).thenReturn(requiredValidator);
        // Act -------------------------------------------------------------
        Set<ValidationMessage> result = validator.validate(executionContext, dataNode, dataNode, "");
        // Assert ----------------------------------------------------------
        assertNotNull(result);
        assertEquals(1, result.size(), "Exactly one validation message (the required error) should be returned");
        assertTrue(result.contains(requiredMessage), "The returned set must contain the required validator message");
        // Verify that the child schema's validate method was never called because the property is absent
        verify(propertySchema, never()).validate(any(), any(), any(), anyString());
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
        when(propertySchema.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Collections.emptySet());
        // Act -------------------------------------------------------------
        Set<ValidationMessage> result = validator.validate(executionContext, dataNode, dataNode, "");
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
        when(propertySchema.validate(any(), any(), any(), anyString())).thenReturn(Collections.singleton(mock(ValidationMessage.class)));
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
        // empty
        ObjectNode dataNode = MAPPER.createObjectNode();
        RequiredValidator requiredValidator = mock(RequiredValidator.class);
        when(requiredValidator.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Collections.emptySet());
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
        // empty
        ObjectNode dataNode = MAPPER.createObjectNode();
        RequiredValidator requiredValidator = mock(RequiredValidator.class);
        ValidationMessage msg = mock(ValidationMessage.class);
        Set<ValidationMessage> errors = new HashSet<>(Collections.singleton(msg));
        when(requiredValidator.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(errors);
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
        // empty
        ObjectNode dataNode = MAPPER.createObjectNode();
        RequiredValidator requiredValidator = mock(RequiredValidator.class);
        ValidationMessage msg = mock(ValidationMessage.class);
        Set<ValidationMessage> errors = new HashSet<>(Collections.singleton(msg));
        when(requiredValidator.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(errors);
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
        // any node
        ObjectNode dataNode = MAPPER.createObjectNode();
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
        // empty
        ObjectNode dataNode = MAPPER.createObjectNode();
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
        when(propertySchema.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenAnswer(invocation -> {
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
        when(requiredValidator.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(requiredErrors);
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
        when(propertySchema.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Collections.emptySet());
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
        assertThrows(NullPointerException.class, () -> validator.validate(null, dataNode, dataNode, "#"));
    }

    /* --------------------------------------------------------------------- *
     *  Null node argument – expect NullPointerException.                  *
     * --------------------------------------------------------------------- */
    @Test
    void testValidateNullNode() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> validator.validate(executionContext, null, MAPPER.createObjectNode(), "#"));
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
        assertThrows(NullPointerException.class, () -> validator.validate(executionContext, dataNode, dataNode, "#"));
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
        // property absent
        ObjectNode dataNode = MAPPER.createObjectNode();
        // Act & Assert
        assertThrows(NullPointerException.class, () -> validator.validate(executionContext, dataNode, dataNode, "#"));
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
        assertThrows(NullPointerException.class, () -> validator.validate(executionContext, dataNode, dataNode, null));
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

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:10: error: package org.mockito does not exist
import static org.mockito.Mockito.*;
                         ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:16: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:8: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:17: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:20: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:31: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:34: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:38: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:41: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:44: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:47: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:67: error: cannot find symbol
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class))).thenReturn(propertySchema);
                                         ^
  symbol:   method anyString()
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:67: error: cannot find symbol
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class))).thenReturn(propertySchema);
                                                      ^
  symbol:   method any(Class<JsonNode>)
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:67: error: cannot find symbol
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class))).thenReturn(propertySchema);
                                                                           ^
  symbol:   method any(Class<JsonSchema>)
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:76: error: cannot find symbol
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        ^
  symbol:   method when(CollectorContext)
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:77: error: cannot find symbol
        when(collectorContext.getEvaluatedProperties()).thenReturn(new HashSet<>());
        ^
  symbol:   method when(Collection<String>)
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:78: error: cannot find symbol
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);
        ^
  symbol:   method when(Object)
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:80: error: cannot find symbol
        when(validatorState.isComplexValidator()).thenReturn(false);
        ^
  symbol:   method when(boolean)
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:81: error: cannot find symbol
        when(validatorState.isWalkEnabled()).thenReturn(false);
        ^
  symbol:   method when(boolean)
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:82: error: cannot find symbol
        when(validatorState.isValidationEnabled()).thenReturn(true);
        ^
  symbol:   method when(boolean)
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:84: error: cannot find symbol
        doNothing().when(validatorState).setComplexValidator(anyBoolean());
        ^
  symbol:   method doNothing()
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:84: error: cannot find symbol
        doNothing().when(validatorState).setComplexValidator(anyBoolean());
                                                             ^
  symbol:   method anyBoolean()
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:85: error: cannot find symbol
        doNothing().when(validatorState).setMatchedNode(anyBoolean());
        ^
  symbol:   method doNothing()
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:85: error: cannot find symbol
        doNothing().when(validatorState).setMatchedNode(anyBoolean());
                                                        ^
  symbol:   method anyBoolean()
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:89: error: cannot find symbol
        when(parentSchema.hasRequiredValidator()).thenReturn(false);
        ^
  symbol:   method when(boolean)
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:109: error: cannot find symbol
        when(propertySchema.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Collections.emptySet());
                                     ^
  symbol:   method any()
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:109: error: cannot find symbol
        when(propertySchema.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Collections.emptySet());
                                            ^
  symbol:   method any(Class<JsonNode>)
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:109: error: cannot find symbol
        when(propertySchema.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Collections.emptySet());
                                                                 ^
  symbol:   method any(Class<JsonNode>)
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:109: error: cannot find symbol
        when(propertySchema.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Collections.emptySet());
                                                                                      ^
  symbol:   method anyString()
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:119: error: cannot find symbol
        verify(propertySchema, times(1)).validate(eq(executionContext), eq(dataNode.get("prop")), eq(dataNode), eq(""));
                               ^
  symbol:   method times(int)
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:119: error: cannot find symbol
        verify(propertySchema, times(1)).validate(eq(executionContext), eq(dataNode.get("prop")), eq(dataNode), eq(""));
                                                  ^
  symbol:   method eq(ExecutionContext)
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:119: error: cannot find symbol
        verify(propertySchema, times(1)).validate(eq(executionContext), eq(dataNode.get("prop")), eq(dataNode), eq(""));
                                                                        ^
  symbol:   method eq(JsonNode)
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:119: error: cannot find symbol
        verify(propertySchema, times(1)).validate(eq(executionContext), eq(dataNode.get("prop")), eq(dataNode), eq(""));
                                                                                                  ^
  symbol:   method eq(ObjectNode)
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:119: error: cannot find symbol
        verify(propertySchema, times(1)).validate(eq(executionContext), eq(dataNode.get("prop")), eq(dataNode), eq(""));
                                                                                                                ^
  symbol:   method eq(String)
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:132: error: cannot find symbol
        RequiredValidator requiredValidator = mock(RequiredValidator.class);
                                              ^
  symbol:   method mock(Class<RequiredValidator>)
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:133: error: cannot find symbol
        ValidationMessage requiredMessage = mock(ValidationMessage.class);
                                            ^
  symbol:   method mock(Class<ValidationMessage>)
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:136: error: cannot find symbol
        when(requiredValidator.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(requiredErrors);
                                        ^
  symbol:   method any()
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:136: error: cannot find symbol
        when(requiredValidator.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(requiredErrors);
                                               ^
  symbol:   method any(Class<JsonNode>)
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:136: error: cannot find symbol
        when(requiredValidator.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(requiredErrors);
                                                                    ^
  symbol:   method any(Class<JsonNode>)
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:136: error: cannot find symbol
        when(requiredValidator.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(requiredErrors);
                                                                                         ^
  symbol:   method anyString()
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:137: error: cannot find symbol
        when(parentSchema.hasRequiredValidator()).thenReturn(true);
        ^
  symbol:   method when(boolean)
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:138: error: cannot find symbol
        when(parentSchema.getRequiredValidator()).thenReturn(requiredValidator);
        ^
  symbol:   method when(JsonValidator)
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:146: error: cannot find symbol
        verify(propertySchema, never()).validate(any(), any(), any(), anyString());
                               ^
  symbol:   method never()
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:146: error: cannot find symbol
        verify(propertySchema, never()).validate(any(), any(), any(), anyString());
                                                 ^
  symbol:   method any()
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:146: error: cannot find symbol
        verify(propertySchema, never()).validate(any(), any(), any(), anyString());
                                                        ^
  symbol:   method any()
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:146: error: cannot find symbol
        verify(propertySchema, never()).validate(any(), any(), any(), anyString());
                                                               ^
  symbol:   method any()
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:146: error: cannot find symbol
        verify(propertySchema, never()).validate(any(), any(), any(), anyString());
                                                                      ^
  symbol:   method anyString()
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:160: error: cannot find symbol
        when(validatorState.isComplexValidator()).thenReturn(true);
        ^
  symbol:   method when(boolean)
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:162: error: cannot find symbol
        when(propertySchema.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Collections.emptySet());
                                     ^
  symbol:   method any()
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:162: error: cannot find symbol
        when(propertySchema.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Collections.emptySet());
                                            ^
  symbol:   method any(Class<JsonNode>)
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:162: error: cannot find symbol
        when(propertySchema.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Collections.emptySet());
                                                                 ^
  symbol:   method any(Class<JsonNode>)
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:162: error: cannot find symbol
        when(propertySchema.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Collections.emptySet());
                                                                                      ^
  symbol:   method anyString()
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:169: error: cannot find symbol
        InOrder inOrder = inOrder(validatorState);
        ^
  symbol:   class InOrder
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:169: error: cannot find symbol
        InOrder inOrder = inOrder(validatorState);
                          ^
  symbol:   method inOrder(ValidatorState)
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:187: error: cannot find symbol
        when(validatorState.isWalkEnabled()).thenReturn(true);
        ^
  symbol:   method when(boolean)
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:189: error: cannot find symbol
        when(propertySchema.validate(any(), any(), any(), anyString())).thenReturn(Collections.singleton(mock(ValidationMessage.class)));
                                     ^
  symbol:   method any()
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:189: error: cannot find symbol
        when(propertySchema.validate(any(), any(), any(), anyString())).thenReturn(Collections.singleton(mock(ValidationMessage.class)));
                                            ^
  symbol:   method any()
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:189: error: cannot find symbol
        when(propertySchema.validate(any(), any(), any(), anyString())).thenReturn(Collections.singleton(mock(ValidationMessage.class)));
                                                   ^
  symbol:   method any()
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:189: error: cannot find symbol
        when(propertySchema.validate(any(), any(), any(), anyString())).thenReturn(Collections.singleton(mock(ValidationMessage.class)));
                                                          ^
  symbol:   method anyString()
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:189: error: cannot find symbol
        when(propertySchema.validate(any(), any(), any(), anyString())).thenReturn(Collections.singleton(mock(ValidationMessage.class)));
                                                                                                         ^
  symbol:   method mock(Class<ValidationMessage>)
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:195: error: cannot find symbol
        verify(propertySchema, never()).validate(any(), any(), any(), anyString());
                               ^
  symbol:   method never()
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:195: error: cannot find symbol
        verify(propertySchema, never()).validate(any(), any(), any(), anyString());
                                                 ^
  symbol:   method any()
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:195: error: cannot find symbol
        verify(propertySchema, never()).validate(any(), any(), any(), anyString());
                                                        ^
  symbol:   method any()
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:195: error: cannot find symbol
        verify(propertySchema, never()).validate(any(), any(), any(), anyString());
                                                               ^
  symbol:   method any()
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:195: error: cannot find symbol
        verify(propertySchema, never()).validate(any(), any(), any(), anyString());
                                                                      ^
  symbol:   method anyString()
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:206: error: cannot find symbol
        RequiredValidator requiredValidator = mock(RequiredValidator.class);
                                              ^
  symbol:   method mock(Class<RequiredValidator>)
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:207: error: cannot find symbol
        when(requiredValidator.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Collections.emptySet());
                                        ^
  symbol:   method any()
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:207: error: cannot find symbol
        when(requiredValidator.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Collections.emptySet());
                                               ^
  symbol:   method any(Class<JsonNode>)
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:207: error: cannot find symbol
        when(requiredValidator.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Collections.emptySet());
                                                                    ^
  symbol:   method any(Class<JsonNode>)
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:207: error: cannot find symbol
        when(requiredValidator.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Collections.emptySet());
                                                                                         ^
  symbol:   method anyString()
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:208: error: cannot find symbol
        when(parentSchema.hasRequiredValidator()).thenReturn(true);
        ^
  symbol:   method when(boolean)
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:209: error: cannot find symbol
        when(parentSchema.getRequiredValidator()).thenReturn(requiredValidator);
        ^
  symbol:   method when(JsonValidator)
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:215: error: cannot find symbol
        verify(propertySchema, never()).validate(any(), any(), any(), anyString());
                               ^
  symbol:   method never()
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:215: error: cannot find symbol
        verify(propertySchema, never()).validate(any(), any(), any(), anyString());
                                                 ^
  symbol:   method any()
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:215: error: cannot find symbol
        verify(propertySchema, never()).validate(any(), any(), any(), anyString());
                                                        ^
  symbol:   method any()
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:215: error: cannot find symbol
        verify(propertySchema, never()).validate(any(), any(), any(), anyString());
                                                               ^
  symbol:   method any()
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:215: error: cannot find symbol
        verify(propertySchema, never()).validate(any(), any(), any(), anyString());
                                                                      ^
  symbol:   method anyString()
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:226: error: cannot find symbol
        RequiredValidator requiredValidator = mock(RequiredValidator.class);
                                              ^
  symbol:   method mock(Class<RequiredValidator>)
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:227: error: cannot find symbol
        ValidationMessage msg = mock(ValidationMessage.class);
                                ^
  symbol:   method mock(Class<ValidationMessage>)
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:229: error: cannot find symbol
        when(requiredValidator.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(errors);
                                        ^
  symbol:   method any()
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:229: error: cannot find symbol
        when(requiredValidator.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(errors);
                                               ^
  symbol:   method any(Class<JsonNode>)
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:229: error: cannot find symbol
        when(requiredValidator.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(errors);
                                                                    ^
  symbol:   method any(Class<JsonNode>)
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:229: error: cannot find symbol
        when(requiredValidator.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(errors);
                                                                                         ^
  symbol:   method anyString()
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:230: error: cannot find symbol
        when(parentSchema.hasRequiredValidator()).thenReturn(true);
        ^
  symbol:   method when(boolean)
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:231: error: cannot find symbol
        when(parentSchema.getRequiredValidator()).thenReturn(requiredValidator);
        ^
  symbol:   method when(JsonValidator)
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:232: error: cannot find symbol
        when(validatorState.isComplexValidator()).thenReturn(false);
        ^
  symbol:   method when(boolean)
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:250: error: cannot find symbol
        RequiredValidator requiredValidator = mock(RequiredValidator.class);
                                              ^
  symbol:   method mock(Class<RequiredValidator>)
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:251: error: cannot find symbol
        ValidationMessage msg = mock(ValidationMessage.class);
                                ^
  symbol:   method mock(Class<ValidationMessage>)
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:253: error: cannot find symbol
        when(requiredValidator.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(errors);
                                        ^
  symbol:   method any()
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:253: error: cannot find symbol
        when(requiredValidator.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(errors);
                                               ^
  symbol:   method any(Class<JsonNode>)
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:253: error: cannot find symbol
        when(requiredValidator.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(errors);
                                                                    ^
  symbol:   method any(Class<JsonNode>)
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:253: error: cannot find symbol
        when(requiredValidator.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(errors);
                                                                                         ^
  symbol:   method anyString()
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:254: error: cannot find symbol
        when(parentSchema.hasRequiredValidator()).thenReturn(true);
        ^
  symbol:   method when(boolean)
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:255: error: cannot find symbol
        when(parentSchema.getRequiredValidator()).thenReturn(requiredValidator);
        ^
  symbol:   method when(JsonValidator)
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:256: error: cannot find symbol
        when(validatorState.isComplexValidator()).thenReturn(true);
        ^
  symbol:   method when(boolean)
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:262: error: cannot find symbol
        verify(validatorState).setMatchedNode(false);
        ^
  symbol:   method verify(ValidatorState)
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:294: error: cannot find symbol
        when(parentSchema.hasRequiredValidator()).thenReturn(false);
        ^
  symbol:   method when(boolean)
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:312: error: cannot find symbol
        when(validatorState.isWalkEnabled()).thenReturn(true);
        ^
  symbol:   method when(boolean)
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:320: error: cannot find symbol
        verify(propertySchema, never()).validate(any(), any(), any(), anyString());
                               ^
  symbol:   method never()
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:320: error: cannot find symbol
        verify(propertySchema, never()).validate(any(), any(), any(), anyString());
                                                 ^
  symbol:   method any()
  location: class PropertiesValidator_validate_Test
100 errors
only showing the first 100 errors, of 133 total; use -Xmaxerrs if you would like to see more

```

And you can use the following related information to assist you in your analysis and correction:
```
{'dependent classes': 'class `com.networknt.schema.ExecutionContext`:\n\t\nclass `com.networknt.schema.PropertiesValidator`:\n\tdependent fields:\n\t\tjava.util.Map<java.lang.String, com.networknt.schema.JsonSchema> com.networknt.schema.PropertiesValidator.schemas;\n\t\torg.slf4j.Logger com.networknt.schema.PropertiesValidator.logger;\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> walk(ExecutionContext, JsonNode, JsonNode, String, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.PropertiesValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;\n\t\tmethod `private void walkSchema(ExecutionContext, Map.Entry<String, JsonSchema>, JsonNode, JsonNode, String, boolean, Set<ValidationMessage>, WalkListenerRunner)` returns `void`, related with `com.networknt.schema.PropertiesValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;\nclass `com.networknt.schema.ValidatorState`:\n\tdependent fields:\n\t\tjava.lang.String com.networknt.schema.ValidatorState.VALIDATOR_STATE_KEY;\n\t\nclass `com.networknt.schema.BaseJsonValidator`:\n\tdependent fields:\n\t\tcom.networknt.schema.ValidationContext com.networknt.schema.BaseJsonValidator.validationContext;\n\t\nclass `com.networknt.schema.OneOfValidator`:\n\trelated functions:\n\t\tmethod `private static void resetValidatorState(CollectorContext)` returns `void`, related with `com.networknt.schema.PropertiesValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.PropertiesValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;\nclass `com.networknt.schema.PatternPropertiesValidator`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.PropertiesValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;\nclass `com.networknt.schema.AllOfValidator`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.PropertiesValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;\nclass `com.networknt.schema.AdditionalPropertiesValidator`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.PropertiesValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;\nclass `com.networknt.schema.AnyOfValidator`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.PropertiesValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;\nclass `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.PropertiesValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 



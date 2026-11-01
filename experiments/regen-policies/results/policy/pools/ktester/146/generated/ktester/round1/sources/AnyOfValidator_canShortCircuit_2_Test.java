package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentModificationException;

@ExtendWith(MockitoExtension.class)
class AnyOfValidator_canShortCircuit_2_Test {

    private static ValidationContext sharedValidationContext;

    @BeforeAll
    static void setupBeforeAll() {
        // Use a real ValidationContext – it does not affect the canShortCircuit logic.
        JsonMetaSchema meta = JsonMetaSchema.getV202012Instance();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        sharedValidationContext = new ValidationContext(meta, factory, new SchemaValidatorsConfig());
    }

    // No special per‑test teardown is required.
    @AfterEach
    void teardownAfterEach() {
        // Reset mocks after each test to avoid cross‑test interference.
        Mockito.reset();
    }

    /**
     * Creates an {@link AnyOfValidator} whose {@code getEvaluationParentSchema()}
     * method returns the supplied {@code parent}. The validator is spied so that
     * we can stub the protected method.
     */
    private AnyOfValidator createValidatorWithParent(JsonSchema parent) throws Exception {
        ArrayNode anyOfArray = JsonNodeFactory.instance.arrayNode();
        SchemaLocation schemaLocation = mock(SchemaLocation.class);
        JsonNodePath evaluationPath = mock(JsonNodePath.class);
        // the constructor's parentSchema argument is not used here
        JsonSchema dummyParent = mock(JsonSchema.class);
        AnyOfValidator validator = spy(new AnyOfValidator(schemaLocation, evaluationPath, anyOfArray, dummyParent, sharedValidationContext));
        // Stub the protected method that fetches the evaluation parent schema.
        when(validator.getEvaluationParentSchema()).thenReturn(parent);
        return validator;
    }

    /**
     * Uses reflection to set the private {@code canShortCircuit} field on the
     * validator instance. This allows us to simulate the cached‑value scenarios
     * without invoking the method logic.
     */
    private void setCanShortCircuitField(AnyOfValidator validator, Boolean value) throws Exception {
        Field field = AnyOfValidator.class.getDeclaredField("canShortCircuit");
        field.setAccessible(true);
        field.set(validator, value);
    }

    @Test
    void canShortCircuitReturnsTrueWhenNoUnevaluatedKeywords() throws Exception {
        // ----- Arrange --------------------------------------------------------
        // Minimal anyOf schema (empty array) – the content is irrelevant for the test.
        ArrayNode anyOfArray = JsonNodeFactory.instance.arrayNode();
        // Mock objects required by the constructor.
        SchemaLocation schemaLocation = mock(SchemaLocation.class);
        JsonNodePath evaluationPath = mock(JsonNodePath.class);
        // not used directly in this test.
        JsonSchema parentSchema = mock(JsonSchema.class);
        // Create the validator instance.
        AnyOfValidator validator = spy(new AnyOfValidator(schemaLocation, evaluationPath, anyOfArray, parentSchema, sharedValidationContext));
        // Mock the evaluation parent schema to return a list of validators that do NOT contain
        // the disallowed keywords.
        JsonSchema evaluationParentSchema = mock(JsonSchema.class);
        when(validator.getEvaluationParentSchema()).thenReturn(evaluationParentSchema);
        when(evaluationParentSchema.getValidators()).thenReturn(Collections.emptyList());
        // ----- Act ------------------------------------------------------------
        boolean result = validator.canShortCircuit();
        // ----- Assert ---------------------------------------------------------
        assertTrue(result, "canShortCircuit should be true when no 'unevaluated*' validators are present");
    }

    @Test
    void canShortCircuitReturnsFalseWhenUnevaluatedPropertiesPresent() throws Exception {
        // ----- Arrange --------------------------------------------------------
        ArrayNode anyOfArray = JsonNodeFactory.instance.arrayNode();
        SchemaLocation schemaLocation = mock(SchemaLocation.class);
        JsonNodePath evaluationPath = mock(JsonNodePath.class);
        JsonSchema parentSchema = mock(JsonSchema.class);
        AnyOfValidator validator = spy(new AnyOfValidator(schemaLocation, evaluationPath, anyOfArray, parentSchema, sharedValidationContext));
        // Mock a JsonValidator that reports the disallowed keyword.
        JsonValidator unevaluatedValidator = mock(JsonValidator.class);
        when(unevaluatedValidator.getKeyword()).thenReturn("unevaluatedProperties");
        JsonSchema evaluationParentSchema = mock(JsonSchema.class);
        when(validator.getEvaluationParentSchema()).thenReturn(evaluationParentSchema);
        when(evaluationParentSchema.getValidators()).thenReturn(List.of(unevaluatedValidator));
        // ----- Act ------------------------------------------------------------
        boolean result = validator.canShortCircuit();
        // ----- Assert ---------------------------------------------------------
        assertFalse(result, "canShortCircuit should be false when an 'unevaluatedProperties' validator is present");
    }

    @Test
    void canShortCircuitReturnsFalseWhenUnevaluatedItemsPresent() throws Exception {
        // ----- Arrange --------------------------------------------------------
        ArrayNode anyOfArray = JsonNodeFactory.instance.arrayNode();
        SchemaLocation schemaLocation = mock(SchemaLocation.class);
        JsonNodePath evaluationPath = mock(JsonNodePath.class);
        JsonSchema parentSchema = mock(JsonSchema.class);
        AnyOfValidator validator = spy(new AnyOfValidator(schemaLocation, evaluationPath, anyOfArray, parentSchema, sharedValidationContext));
        // Mock a JsonValidator that reports the other disallowed keyword.
        JsonValidator unevaluatedValidator = mock(JsonValidator.class);
        when(unevaluatedValidator.getKeyword()).thenReturn("unevaluatedItems");
        JsonSchema evaluationParentSchema = mock(JsonSchema.class);
        when(validator.getEvaluationParentSchema()).thenReturn(evaluationParentSchema);
        when(evaluationParentSchema.getValidators()).thenReturn(List.of(unevaluatedValidator));
        // ----- Act ------------------------------------------------------------
        boolean result = validator.canShortCircuit();
        // ----- Assert ---------------------------------------------------------
        assertFalse(result, "canShortCircuit should be false when an 'unevaluatedItems' validator is present");
    }

    @Test
    void canShortCircuitTrueWhenOnlyHarmlessValidators() throws Exception {
        // Setup a list of validators that do NOT contain blocking keywords.
        JsonValidator typeValidator = mock(JsonValidator.class);
        when(typeValidator.getKeyword()).thenReturn("type");
        JsonValidator minimumValidator = mock(JsonValidator.class);
        when(minimumValidator.getKeyword()).thenReturn("minimum");
        JsonSchema evaluationParentSchema = mock(JsonSchema.class);
        when(evaluationParentSchema.getValidators()).thenReturn(List.of(typeValidator, minimumValidator));
        AnyOfValidator validator = createValidatorWithParent(evaluationParentSchema);
        boolean result = validator.canShortCircuit();
        assertTrue(result, "Expected true when only non‑blocking validators are present");
    }

    @Test
    void cachedResultReturnedWhenFieldAlreadySetTrue() throws Exception {
        AnyOfValidator validator = createValidatorWithParent(mock(JsonSchema.class));
        // Force the private field to Boolean.TRUE via reflection.
        setCanShortCircuitField(validator, Boolean.TRUE);
        // Even if the parent schema would contain a blocking validator, the cached value wins.
        JsonValidator blocking = mock(JsonValidator.class);
        when(blocking.getKeyword()).thenReturn("unevaluatedProperties");
        JsonSchema parent = mock(JsonSchema.class);
        when(parent.getValidators()).thenReturn(List.of(blocking));
        when(validator.getEvaluationParentSchema()).thenReturn(parent);
        boolean result = validator.canShortCircuit();
        assertTrue(result, "When cached true, method must return true without re‑evaluating");
    }

    @Test
    void cachedResultReturnedWhenFieldAlreadySetFalse() throws Exception {
        AnyOfValidator validator = createValidatorWithParent(mock(JsonSchema.class));
        setCanShortCircuitField(validator, Boolean.FALSE);
        // Even if the parent schema now has no blocking validators, cached false must be returned.
        JsonValidator harmless = mock(JsonValidator.class);
        when(harmless.getKeyword()).thenReturn("type");
        JsonSchema parent = mock(JsonSchema.class);
        when(parent.getValidators()).thenReturn(List.of(harmless));
        when(validator.getEvaluationParentSchema()).thenReturn(parent);
        boolean result = validator.canShortCircuit();
        assertFalse(result, "When cached false, method must return false regardless of underlying schema");
    }

    @Test
    void canShortCircuitHandlesNullKeywordAndHarmlessKeyword() throws Exception {
        JsonValidator nullKeywordValidator = mock(JsonValidator.class);
        when(nullKeywordValidator.getKeyword()).thenReturn(null);
        JsonValidator typeValidator = mock(JsonValidator.class);
        when(typeValidator.getKeyword()).thenReturn("type");
        JsonSchema evaluationParentSchema = mock(JsonSchema.class);
        when(evaluationParentSchema.getValidators()).thenReturn(List.of(nullKeywordValidator, typeValidator));
        AnyOfValidator validator = createValidatorWithParent(evaluationParentSchema);
        boolean result = validator.canShortCircuit();
        assertTrue(result, "Null or unrelated keywords must not prevent short‑circuiting");
    }

    @Test
    void canShortCircuitStopsAtBlockingKeywordAfterHarmlessOne() throws Exception {
        JsonValidator harmless = mock(JsonValidator.class);
        when(harmless.getKeyword()).thenReturn("minimum");
        JsonValidator blocking = mock(JsonValidator.class);
        when(blocking.getKeyword()).thenReturn("unevaluatedItems");
        JsonSchema evaluationParentSchema = mock(JsonSchema.class);
        when(evaluationParentSchema.getValidators()).thenReturn(List.of(harmless, blocking));
        AnyOfValidator validator = createValidatorWithParent(evaluationParentSchema);
        boolean result = validator.canShortCircuit();
        assertFalse(result, "Method must return false when a blocking keyword appears later in the list");
    }

    @Test
    void canShortCircuitThrowsNPEWhenParentValidatorsIsNull() throws Exception {
        JsonSchema evaluationParentSchema = mock(JsonSchema.class);
        when(evaluationParentSchema.getValidators()).thenReturn(null);
        AnyOfValidator validator = createValidatorWithParent(evaluationParentSchema);
        assertThrows(NullPointerException.class, validator::canShortCircuit, "Expected NPE when getValidators() returns null");
    }

    @Test
    void canShortCircuitThrowsNPEWhenEvaluationParentSchemaIsNull() throws Exception {
        // parent schema null
        AnyOfValidator validator = createValidatorWithParent(null);
        assertThrows(NullPointerException.class, validator::canShortCircuit, "Expected NPE when evaluation parent schema is null");
    }

    @Test
    void canShortCircuitThrowsNPEWhenValidatorElementInListIsNull() throws Exception {
        JsonSchema evaluationParentSchema = mock(JsonSchema.class);
        // List containing a null entry.
        when(evaluationParentSchema.getValidators()).thenReturn(Arrays.asList(null));
        AnyOfValidator validator = createValidatorWithParent(evaluationParentSchema);
        assertThrows(NullPointerException.class, validator::canShortCircuit, "Expected NPE when a validator element in the list is null");
    }

    @Test
    void canShortCircuitPropagatesConcurrentModificationException() throws Exception {
        // Create a custom Iterable whose iterator throws CME on next().
        Iterable<JsonValidator> throwingIterable = () -> new Iterator<>() {

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public JsonValidator next() {
                throw new ConcurrentModificationException();
            }
        };
        JsonSchema evaluationParentSchema = mock(JsonSchema.class);
        // We cannot mock List directly because the method expects List<JsonValidator>,
        // but returning an Iterable that implements List is unnecessary – the for‑each loop
        // works on any Iterable. We'll cast to List to satisfy the signature.
        @SuppressWarnings("unchecked")
        List<JsonValidator> throwingList = (List<JsonValidator>) throwingIterable;
        when(evaluationParentSchema.getValidators()).thenReturn(throwingList);
        AnyOfValidator validator = createValidatorWithParent(evaluationParentSchema);
        assertThrows(ConcurrentModificationException.class, validator::canShortCircuit, "Method should propagate ConcurrentModificationException from the iterator");
    }

    @Test
    void cachedTrueRemainsEvenIfParentSchemaChangesAfterFirstCall() throws Exception {
        JsonValidator harmless = mock(JsonValidator.class);
        when(harmless.getKeyword()).thenReturn("type");
        JsonSchema evaluationParentSchema = mock(JsonSchema.class);
        when(evaluationParentSchema.getValidators()).thenReturn(List.of(harmless));
        AnyOfValidator validator = createValidatorWithParent(evaluationParentSchema);
        // First call caches true.
        assertTrue(validator.canShortCircuit(), "First call should cache true");
        // Mutate the parent schema to now contain a blocking validator.
        JsonValidator blocking = mock(JsonValidator.class);
        when(blocking.getKeyword()).thenReturn("unevaluatedProperties");
        when(evaluationParentSchema.getValidators()).thenReturn(List.of(blocking));
        // Second call must still return the cached true.
        assertTrue(validator.canShortCircuit(), "Cached true must survive underlying schema mutation");
    }

    @Test
    void cachedFalseRemainsEvenIfParentSchemaChangesAfterFirstCall() throws Exception {
        JsonValidator blocking = mock(JsonValidator.class);
        when(blocking.getKeyword()).thenReturn("unevaluatedProperties");
        JsonSchema evaluationParentSchema = mock(JsonSchema.class);
        when(evaluationParentSchema.getValidators()).thenReturn(List.of(blocking));
        AnyOfValidator validator = createValidatorWithParent(evaluationParentSchema);
        // First call caches false.
        assertFalse(validator.canShortCircuit(), "First call should cache false");
        // Mutate the parent schema to remove the blocking validator.
        JsonValidator harmless = mock(JsonValidator.class);
        when(harmless.getKeyword()).thenReturn("type");
        when(evaluationParentSchema.getValidators()).thenReturn(List.of(harmless));
        // Second call must still return the cached false.
        assertFalse(validator.canShortCircuit(), "Cached false must survive underlying schema mutation");
    }
}

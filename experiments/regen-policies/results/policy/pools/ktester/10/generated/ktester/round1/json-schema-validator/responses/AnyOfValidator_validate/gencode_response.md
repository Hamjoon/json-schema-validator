```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnyOfValidator_validate_Test {

    /* --------------------------------------------------------------------- */
    /*  Shared test fixtures                                                 */
    /* --------------------------------------------------------------------- */
    @Mock ExecutionContext executionContext;
    @Mock CollectorContext collectorContext;
    @Mock ValidatorState validatorState;
    @Mock Scope dynamicScope;               // used for both parent & child scopes
    @Mock ValidationContext validationContext;
    @Mock SchemaValidatorsConfig config;   // config inside ValidationContext
    @Mock JsonNode instanceNode;            // node being validated
    @Mock JsonNode rootNode;                // root node of the document
    @Mock JsonNodePath instanceLocation;   // location of the instance node

    AnyOfValidator validator;              // system‑under‑test

    @BeforeAll
    static void setupBeforeAll() {
        // No static resources required for these tests
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        /* --------------------------------------------------------------- */
        /*  Mock the execution context hierarchy                           */
        /* --------------------------------------------------------------- */
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        when(executionContext.getValidatorState()).thenReturn(validatorState);
        when(executionContext.getExecutionConfig()).thenReturn(new ExecutionConfig());

        // every call to enterDynamicScope returns the same mock scope;
        // the validator only uses the returned Scope for mergeWith()
        when(collectorContext.enterDynamicScope()).thenReturn(dynamicScope);
        when(collectorContext.exitDynamicScope()).thenReturn(dynamicScope);
        // mergeWith is a void method – no behaviour needed
        doNothing().when(dynamicScope).mergeWith(any(Scope.class));

        // discriminator handling – default no‑op
        doNothing().when(executionContext).enterDiscriminatorContext(any(), any());
        doNothing().when(executionContext).leaveDiscriminatorContextImmediately(any());

        /* --------------------------------------------------------------- */
        /*  Mock ValidationContext and its configuration                    */
        /* --------------------------------------------------------------- */
        when(validationContext.getConfig()).thenReturn(config);
        // keep the tests simple – we do NOT enable OpenAPI‑3 discriminators by default
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);

        /* --------------------------------------------------------------- */
        /*  Build a minimal schema node (an array with a single element)   */
        /* --------------------------------------------------------------- */
        JsonNode schemaArrayNode = mock(JsonNode.class);
        when(schemaArrayNode.size()).thenReturn(1);
        when(schemaArrayNode.get(0)).thenReturn(mock(JsonNode.class)); // dummy subschema node

        // create a dummy SchemaLocation – its internals are irrelevant for the test
        SchemaLocation schemaLocation = new SchemaLocation(new AbsoluteIri("http://example.com/schema"),
                JsonNodePath.JSON_POINTER);

        // instantiate the validator – it will call validationContext.newSchema(...)
        validator = new AnyOfValidator(schemaLocation,
                new JsonNodePath(PathType.JSON),   // evaluationPath (any value works)
                schemaArrayNode,
                null,                              // parentSchema – not needed for our tests
                validationContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks to avoid cross‑test interference
        clearInvocations(executionContext, collectorContext, validatorState,
                dynamicScope, validationContext, config,
                instanceNode, rootNode, instanceLocation);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No static resources to clean up
    }

    /* --------------------------------------------------------------------- */
    /*  Helper utilities                                                    */
    /* --------------------------------------------------------------------- */

    /** Replace the internal `schemas` list with the supplied mocks. */
    private void setSchemas(JsonSchema... schemas) throws Exception {
        Field f = AnyOfValidator.class.getDeclaredField("schemas");
        f.setAccessible(true);
        List<JsonSchema> list = (List<JsonSchema>) f.get(validator);
        list.clear();
        Collections.addAll(list, schemas);
    }

    /** Set the internal `discriminatorContext` (may be null). */
    private void setDiscriminatorContext(DiscriminatorContext ctx) throws Exception {
        Field f = AnyOfValidator.class.getDeclaredField("discriminatorContext");
        f.setAccessible(true);
        f.set(validator, ctx);
    }

    /* --------------------------------------------------------------------- */
    /*  Existing test cases (kept unchanged)                                */
    /* --------------------------------------------------------------------- */

    @Test
    void testValidateSuccess_FirstSubschemaMatches() {
        JsonSchema subSchema = mock(JsonSchema.class);
        when(validationContext.newSchema(any(), any(), any(), any()))
                .thenReturn(subSchema);
        when(subSchema.getTypeValidator()).thenReturn(null);
        when(subSchema.validate(any(), any(), any(), any()))
                .thenReturn(Collections.emptySet());
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.hasMatchedNode()).thenReturn(true);

        Set<ValidationMessage> result = validator.validate(
                executionContext, instanceNode, rootNode, instanceLocation);

        assertTrue(result.isEmpty(), "Expected no validation errors when a subschema matches");
        verify(subSchema, times(1)).validate(executionContext, instanceNode, rootNode, instanceLocation);
    }

    @Test
    void testValidateFailure_AllSubschemasError() {
        JsonSchema subSchema = mock(JsonSchema.class);
        when(validationContext.newSchema(any(), any(), any(), any()))
                .thenReturn(subSchema);
        when(subSchema.getTypeValidator()).thenReturn(null);
        ValidationMessage mockMessage = mock(ValidationMessage.class);
        Set<ValidationMessage> errorSet = Collections.singleton(mockMessage);
        when(subSchema.validate(any(), any(), any(), any()))
                .thenReturn(errorSet);
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.hasMatchedNode()).thenReturn(false);

        Set<ValidationMessage> result = validator.validate(
                executionContext, instanceNode, rootNode, instanceLocation);

        assertEquals(1, result.size(), "Expected exactly one validation error");
        assertTrue(result.contains(mockMessage), "Returned errors should contain the mocked message");
        verify(subSchema, times(1)).validate(executionContext, instanceNode, rootNode, instanceLocation);
    }

    /* --------------------------------------------------------------------- */
    /*  New test cases covering the JSON specification                        */
    /* --------------------------------------------------------------------- */

    @Test
    void testValidate_NoSchemas_OpenAPI3Disabled() throws Exception {
        // Empty schema list
        setSchemas(); // no schemas
        // config already false
        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, instanceLocation);
        assertTrue(result.isEmpty(), "When there are no subschemas the validator should return an empty set");
    }

    @Test
    void testValidate_TypeValidatorMismatch_SchemaIgnored() throws Exception {
        JsonSchema subSchema = mock(JsonSchema.class);
        TypeValidator typeValidator = mock(TypeValidator.class);
        ValidationMessage typeMsg = mock(ValidationMessage.class);
        Set<ValidationMessage> typeErrors = Collections.singleton(typeMsg);

        when(validationContext.newSchema(any(), any(), any(), any()))
                .thenReturn(subSchema);
        when(subSchema.getTypeValidator()).thenReturn(typeValidator);
        when(typeValidator.getSchemaType()).thenReturn(JsonType.STRING); // not UNION
        when(typeValidator.equalsToSchemaType(instanceNode)).thenReturn(false);
        when(typeValidator.validate(any(), any(), any(), any())).thenReturn(typeErrors);
        // schema.validate should never be called – we don't set it

        setSchemas(subSchema);
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.hasMatchedNode()).thenReturn(false);

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, instanceLocation);
        assertEquals(1, result.size(), "Expected the TypeValidator error to be returned");
        assertTrue(result.contains(typeMsg));
        verify(typeValidator, times(1)).validate(executionContext, instanceNode, rootNode, instanceLocation);
        verify(subSchema, never()).validate(any(), any(), any(), any());
    }

    @Test
    void testValidate_ValidSubschema_WalkDisabled_EarlySuccess() throws Exception {
        JsonSchema subSchema = mock(JsonSchema.class);
        when(validationContext.newSchema(any(), any(), any(), any()))
                .thenReturn(subSchema);
        when(subSchema.getTypeValidator()).thenReturn(null);
        when(subSchema.validate(any(), any(), any(), any()))
                .thenReturn(Collections.emptySet());

        setSchemas(subSchema);
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.hasMatchedNode()).thenReturn(true); // already matched

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, instanceLocation);
        assertTrue(result.isEmpty(), "Early success should return an empty set");
        verify(subSchema, times(1)).validate(executionContext, instanceNode, rootNode, instanceLocation);
    }

    @Test
    void testValidate_ValidSubschema_WalkEnabled_EarlySuccess() throws Exception {
        JsonSchema subSchema = mock(JsonSchema.class);
        when(validationContext.newSchema(any(), any(), any(), any()))
                .thenReturn(subSchema);
        when(subSchema.getTypeValidator()).thenReturn(null);
        when(subSchema.walk(any(), any(), any(), any(), eq(true)))
                .thenReturn(Collections.emptySet());

        setSchemas(subSchema);
        when(validatorState.isWalkEnabled()).thenReturn(true);
        when(validatorState.hasMatchedNode()).thenReturn(true);

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, instanceLocation);
        assertTrue(result.isEmpty(), "Early success after walk should return an empty set");
        verify(subSchema, times(1)).walk(executionContext, instanceNode, rootNode, instanceLocation, true);
    }

    @Test
    void testValidate_OpenAPI3_DiscriminatorMatch_WithErrors() throws Exception {
        // enable OpenAPI3 style discriminators
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(true);
        DiscriminatorContext discCtx = mock(DiscriminatorContext.class);
        when(discCtx.isDiscriminatorMatchFound()).thenReturn(true);
        setDiscriminatorContext(discCtx);

        JsonSchema subSchema = mock(JsonSchema.class);
        ValidationMessage subMsg = mock(ValidationMessage.class);
        Set<ValidationMessage> subErrors = Collections.singleton(subMsg);
        when(validationContext.newSchema(any(), any(), any(), any()))
                .thenReturn(subSchema);
        when(subSchema.getTypeValidator()).thenReturn(null);
        when(subSchema.validate(any(), any(), any(), any()))
                .thenReturn(subErrors);

        setSchemas(subSchema);
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.hasMatchedNode()).thenReturn(true);

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, instanceLocation);
        // According to the implementation the method returns the original errors set
        assertEquals(1, result.size());
        assertTrue(result.contains(subMsg));
        // verify that the discriminator remark would have been added to the internal allErrors
        // (cannot inspect directly, but we can verify that the discriminatorContext was consulted)
        verify(discCtx, times(1)).isDiscriminatorMatchFound();
    }

    @Test
    void testValidate_OpenAPI3_DiscriminatorMatch_NoErrors() throws Exception {
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(true);
        DiscriminatorContext discCtx = mock(DiscriminatorContext.class);
        when(discCtx.isDiscriminatorMatchFound()).thenReturn(true);
        setDiscriminatorContext(discCtx);

        JsonSchema subSchema = mock(JsonSchema.class);
        when(validationContext.newSchema(any(), any(), any(), any()))
                .thenReturn(subSchema);
        when(subSchema.getTypeValidator()).thenReturn(null);
        when(subSchema.validate(any(), any(), any(), any()))
                .thenReturn(Collections.emptySet());

        setSchemas(subSchema);
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.hasMatchedNode()).thenReturn(true);

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, instanceLocation);
        assertTrue(result.isEmpty(), "When discriminator matches and there are no errors the validator should return an empty set");
    }

    @Test
    void testValidate_RequiredErrorFiltering_RemovesAll() throws Exception {
        // Two subschemas: first returns REQUIRED error, second passes
        JsonSchema requiredSchema = mock(JsonSchema.class);
        JsonSchema passingSchema = mock(JsonSchema.class);

        ValidationMessage requiredMsg = mock(ValidationMessage.class);
        when(requiredMsg.getType()).thenReturn(ValidatorTypeCode.REQUIRED.getValue());
        Set<ValidationMessage> requiredErrors = Collections.singleton(requiredMsg);
        when(requiredSchema.getTypeValidator()).thenReturn(null);
        when(requiredSchema.validate(any(), any(), any(), any()))
                .thenReturn(requiredErrors);

        when(passingSchema.getTypeValidator()).thenReturn(null);
        when(passingSchema.validate(any(), any(), any(), any()))
                .thenReturn(Collections.emptySet());

        setSchemas(requiredSchema, passingSchema);
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.hasMatchedNode()).thenReturn(true); // so that the passing schema counts as valid

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, instanceLocation);
        assertTrue(result.isEmpty(), "All non‑required errors should be filtered away when at least one subschema validates successfully");
    }

    @Test
    void testValidate_RequiredErrorFiltering_LeavesNonRequired() throws Exception {
        // Two subschemas: first returns REQUIRED error, second returns MIN_LENGTH error
        JsonSchema requiredSchema = mock(JsonSchema.class);
        JsonSchema otherSchema = mock(JsonSchema.class);

        ValidationMessage requiredMsg = mock(ValidationMessage.class);
        when(requiredMsg.getType()).thenReturn(ValidatorTypeCode.REQUIRED.getValue());
        Set<ValidationMessage> requiredErrors = Collections.singleton(requiredMsg);
        when(requiredSchema.getTypeValidator()).thenReturn(null);
        when(requiredSchema.validate(any(), any(), any(), any()))
                .thenReturn(requiredErrors);

        ValidationMessage minLenMsg = mock(ValidationMessage.class);
        when(minLenMsg.getType()).thenReturn("minLength"); // any non‑REQUIRED type
        Set<ValidationMessage> otherErrors = Collections.singleton(minLenMsg);
        when(otherSchema.getTypeValidator()).thenReturn(null);
        when(otherSchema.validate(any(), any(), any(), any()))
                .thenReturn(otherErrors);

        setSchemas(requiredSchema, otherSchema);
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.hasMatchedNode()).thenReturn(false); // no schema validates successfully

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, instanceLocation);
        assertEquals(1, result.size(), "Only the non‑required error should remain");
        assertTrue(result.contains(minLenMsg));
    }

    @Test
    void testValidate_OpenAPI3_DiscriminatorActive_NoMatch_FinalMessage() throws Exception {
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(true);
        DiscriminatorContext discCtx = mock(DiscriminatorContext.class);
        when(discCtx.isActive()).thenReturn(true);
        when(discCtx.isDiscriminatorMatchFound()).thenReturn(false);
        setDiscriminatorContext(discCtx);
        // No subschemas
        setSchemas();

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, instanceLocation);
        assertEquals(1, result.size(), "When discriminator is active but no match was found a single message must be returned");
        ValidationMessage msg = result.iterator().next();
        assertTrue(msg.getMessage().contains("based on the provided discriminator. No alternative could be chosen"), "Message text should match the expected discriminator‑no‑match text");
    }

    @Test
    void testValidate_AllSuccess_TriggersStateMatchedNode() throws Exception {
        JsonSchema subSchema = mock(JsonSchema.class);
        when(validationContext.newSchema(any(), any(), any(), any()))
                .thenReturn(subSchema);
        when(subSchema.getTypeValidator()).thenReturn(null);
        when(subSchema.validate(any(), any(), any(), any()))
                .thenReturn(Collections.emptySet());

        setSchemas(subSchema);
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.hasMatchedNode()).thenReturn(false);

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, instanceLocation);
        assertTrue(result.isEmpty(), "All‑success case should return an empty set");
        verify(validatorState, times(1)).setMatchedNode(true);
    }

    @Test
    void testValidate_UnionTypeValidator_BypassesCheck() throws Exception {
        JsonSchema subSchema = mock(JsonSchema.class);
        TypeValidator typeValidator = mock(TypeValidator.class);
        when(validationContext.newSchema(any(), any(), any(), any()))
                .thenReturn(subSchema);
        when(subSchema.getTypeValidator()).thenReturn(typeValidator);
        when(typeValidator.getSchemaType()).thenReturn(JsonType.UNION);
        // typeValidator.validate must NOT be called
        when(subSchema.validate(any(), any(), any(), any()))
                .thenReturn(Collections.emptySet());

        setSchemas(subSchema);
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.hasMatchedNode()).thenReturn(true);

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, instanceLocation);
        assertTrue(result.isEmpty(), "Union type should bypass type checking and succeed");
        verify(typeValidator, never()).validate(any(), any(), any(), any());
    }

    @Test
    void testValidate_NoTypeValidator_ProceedsToValidate() throws Exception {
        JsonSchema subSchema = mock(JsonSchema.class);
        when(validationContext.newSchema(any(), any(), any(), any()))
                .thenReturn(subSchema);
        when(subSchema.getTypeValidator()).thenReturn(null);
        when(subSchema.validate(any(), any(), any(), any()))
                .thenReturn(Collections.emptySet());

        setSchemas(subSchema);
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.hasMatchedNode()).thenReturn(true);

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, instanceLocation);
        assertTrue(result.isEmpty(), "Schema without a TypeValidator should be validated directly");
    }

    @Test
    void testValidate_ValidAndNonRequiredError_Retained() throws Exception {
        // First subschema validates successfully, second returns a non‑required error
        JsonSchema goodSchema = mock(JsonSchema.class);
        JsonSchema badSchema = mock(JsonSchema.class);

        when(goodSchema.getTypeValidator()).thenReturn(null);
        when(goodSchema.validate(any(), any(), any(), any()))
                .thenReturn(Collections.emptySet());

        ValidationMessage minLenMsg = mock(ValidationMessage.class);
        when(minLenMsg.getType()).thenReturn("minLength");
        Set<ValidationMessage> badErrors = Collections.singleton(minLenMsg);
        when(badSchema.getTypeValidator()).thenReturn(null);
        when(badSchema.validate(any(), any(), any(), any()))
                .thenReturn(badErrors);

        setSchemas(goodSchema, badSchema);
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.hasMatchedNode()).thenReturn(true); // so first schema counts as valid

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, instanceLocation);
        assertEquals(1, result.size(), "The non‑required error should be retained");
        assertTrue(result.contains(minLenMsg));
    }

    @Test
    void testValidate_NullExecutionContext_ThrowsNPE() {
        assertThrows(NullPointerException.class, () ->
                validator.validate(null, instanceNode, rootNode, instanceLocation));
    }

    @Test
    void testValidate_NullNode_ThrowsNPE() {
        assertThrows(NullPointerException.class, () ->
                validator.validate(executionContext, null, rootNode, instanceLocation));
    }

    @Test
    void testValidate_NullInstanceLocation_WithOpenAPI3Enabled_ThrowsNPE() throws Exception {
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(true);
        DiscriminatorContext discCtx = mock(DiscriminatorContext.class);
        setDiscriminatorContext(discCtx);
        assertThrows(NullPointerException.class, () ->
                validator.validate(executionContext, instanceNode, rootNode, null));
    }

    @Test
    void testValidate_OpenAPI3Enabled_NullDiscriminatorContext_ThrowsNPE() throws Exception {
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(true);
        // explicitly set discriminatorContext to null
        setDiscriminatorContext(null);
        assertThrows(NullPointerException.class, () ->
                validator.validate(executionContext, instanceNode, rootNode, instanceLocation));
    }

    @Test
    void testValidate_SchemasListContainsNull_ThrowsNPE() throws Exception {
        // Insert a null entry into the schemas list
        setSchemas((JsonSchema) null);
        assertThrows(NullPointerException.class, () ->
                validator.validate(executionContext, instanceNode, rootNode, instanceLocation));
    }

    @Test
    void testValidate_SchemaValidateReturnsNull_ThrowsNPE() throws Exception {
        JsonSchema subSchema = mock(JsonSchema.class);
        when(validationContext.newSchema(any(), any(), any(), any()))
                .thenReturn(subSchema);
        when(subSchema.getTypeValidator()).thenReturn(null);
        // return null instead of a Set
        when(subSchema.validate(any(), any(), any(), any()))
                .thenReturn(null);
        setSchemas(subSchema);
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.hasMatchedNode()).thenReturn(true);

        assertThrows(NullPointerException.class, () ->
                validator.validate(executionContext, instanceNode, rootNode, instanceLocation));
    }

    @Test
    void testValidate_SchemaWalkReturnsNull_ThrowsNPE() throws Exception {
        JsonSchema subSchema = mock(JsonSchema.class);
        when(validationContext.newSchema(any(), any(), any(), any()))
                .thenReturn(subSchema);
        when(subSchema.getTypeValidator()).thenReturn(null);
        when(subSchema.walk(any(), any(), any(), any(), eq(true)))
                .thenReturn(null);
        setSchemas(subSchema);
        when(validatorState.isWalkEnabled()).thenReturn(true);
        when(validatorState.hasMatchedNode()).thenReturn(true);

        assertThrows(NullPointerException.class, () ->
                validator.validate(executionContext, instanceNode, rootNode, instanceLocation));
    }

    @Test
    void testValidate_NullCollectorContext_ThrowsNPE() {
        when(executionContext.getCollectorContext()).thenReturn(null);
        assertThrows(NullPointerException.class, () ->
                validator.validate(executionContext, instanceNode, rootNode, instanceLocation));
    }

    @Test
    void testValidate_NullValidatorState_ThrowsNPE() {
        when(executionContext.getValidatorState()).thenReturn(null);
        assertThrows(NullPointerException.class, () ->
                validator.validate(executionContext, instanceNode, rootNode, instanceLocation));
    }
}
```
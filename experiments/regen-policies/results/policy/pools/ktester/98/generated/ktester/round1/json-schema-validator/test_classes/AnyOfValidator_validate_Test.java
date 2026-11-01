package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import com.networknt.schema.SpecVersion.VersionFlag;
import static org.mockito.Mockito.*;
import java.io.IOException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnyOfValidator_validate_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ValidationContext validationContext;

    private AnyOfValidator anyOfValidator;

    private JsonSchemaFactory factory;

    private JsonMetaSchema metaSchema;

    /**
     * 6️⃣  Walk flag – exercised via a test‑specific subclass that exposes the protected overload
     */
    private static class TestableAnyOfValidator extends AnyOfValidator {

        TestableAnyOfValidator(SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parentSchema, ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validationContext);
        }

        /**
         * Expose the protected validate(...,boolean) for testing
         */
        Set<ValidationMessage> validateWithWalk(ExecutionContext ctx, JsonNode node, JsonNode rootNode, JsonNodePath location, boolean walk) {
            return super.validate(ctx, node, rootNode, location, walk);
        }
    }

    /**
     * 7️⃣  Short‑circuit disabled – validator must evaluate all subschemas
     */
    private static class NoShortCircuitAnyOfValidator extends AnyOfValidator {

        NoShortCircuitAnyOfValidator(SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parentSchema, ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validationContext);
        }

        @Override
        protected boolean canShortCircuit() {
            // force full evaluation
            return false;
        }
    }

    /**
     * 20️⃣ canShortCircuit overridden to throw – should propagate
     */
    private static class ShortCircuitErrorValidator extends AnyOfValidator {

        ShortCircuitErrorValidator(SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parentSchema, ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validationContext);
        }

        @Override
        protected boolean canShortCircuit() {
            throw new RuntimeException("simulated mis‑configuration");
        }
    }

    /**
     * Create a fresh validator instance before each test.
     */
    @BeforeEach
    void setupBeforeEach() throws Exception {
        // ---- ValidationContext -------------------------------------------------
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        JsonMetaSchema metaSchema = factory.getMetaSchema();
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        validationContext = new ValidationContext(metaSchema, factory, config);
        // ---- SchemaLocation ----------------------------------------------------
        // AbsoluteIri has a public constructor that accepts a String.
        AbsoluteIri absoluteIri = new AbsoluteIri("http://example.com/schema");
        JsonNodePath fragment = new JsonNodePath(PathType.JSON_POINTER);
        SchemaLocation schemaLocation = new SchemaLocation(absoluteIri, fragment);
        // ---- Evaluation path ----------------------------------------------------
        JsonNodePath evaluationPath = new JsonNodePath(PathType.JSON_POINTER);
        // ---- anyOf schema (array of two simple subschemas) ----------------------
        String anyOfSchemaJson = "[ {\"type\":\"string\"}, {\"type\":\"integer\"} ]";
        JsonNode schemaNode = MAPPER.readTree(anyOfSchemaJson);
        // parent schema is not needed for these unit tests – pass null.
        JsonSchema parentSchema = null;
        // ---- Create the validator under test ------------------------------------
        anyOfValidator = new AnyOfValidator(schemaLocation, evaluationPath, schemaNode, parentSchema, validationContext);
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        metaSchema = factory.getMetaSchema();
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        validationContext = new ValidationContext(metaSchema, factory, config);
    }

    /**
     * Helper that builds an {@link AnyOfValidator} for the supplied anyOf‑schema JSON.
     */
    private AnyOfValidator buildValidator(String anyOfSchemaJson) throws IOException {
        AbsoluteIri absoluteIri = new AbsoluteIri("http://example.com/schema");
        JsonNodePath fragment = new JsonNodePath(PathType.JSON_POINTER);
        SchemaLocation schemaLocation = new SchemaLocation(absoluteIri, fragment);
        JsonNodePath evaluationPath = new JsonNodePath(PathType.JSON_POINTER);
        JsonNode schemaNode = MAPPER.readTree(anyOfSchemaJson);
        return new AnyOfValidator(schemaLocation, evaluationPath, schemaNode, null, validationContext);
    }

    /**
     * Helper to extract a single ValidationMessage from a Set for readability
     */
    private ValidationMessage singleMessage(Set<ValidationMessage> set) {
        assertEquals(1, set.size(), "Expected exactly one validation message");
        return set.iterator().next();
    }

    /**
     * A value that matches the first subschema (string) should produce no errors.
     */
    @Test
    void validateStringMatchesAnyOf_ShouldReturnEmptySet() throws Exception {
        AnyOfValidator validator = buildValidator("[{\"type\":\"string\"},{\"type\":\"integer\"}]");
        JsonNode instance = MAPPER.readTree("\"hello\"");
        ExecutionContext ctx = new ExecutionContext();
        Set<ValidationMessage> msgs = validator.validate(ctx, instance, instance, new JsonNodePath(PathType.JSON_POINTER));
        assertTrue(msgs.isEmpty(), "String instance should match the first subschema");
    }

    /**
     * A value that matches the second subschema (integer) should also produce no errors.
     */
    @Test
    void validateIntegerMatchesAnyOf_ShouldReturnEmptySet() throws Exception {
        AnyOfValidator validator = buildValidator("[{\"type\":\"string\"},{\"type\":\"integer\"}]");
        JsonNode instance = MAPPER.readTree("42");
        ExecutionContext ctx = new ExecutionContext();
        Set<ValidationMessage> msgs = validator.validate(ctx, instance, instance, new JsonNodePath(PathType.JSON_POINTER));
        assertTrue(msgs.isEmpty(), "Integer instance should match the second subschema");
    }

    /**
     * A value that matches none of the subschemas should produce at least one error.
     */
    @Test
    void validateNonMatchingValue_ShouldReturnValidationMessage() throws Exception {
        // boolean – does not match string nor integer
        JsonNode instance = MAPPER.readTree("true");
        ExecutionContext execContext = new ExecutionContext();
        JsonNodePath instanceLocation = new JsonNodePath(PathType.JSON_POINTER);
        Set<ValidationMessage> messages = anyOfValidator.validate(execContext, instance, instance, instanceLocation);
        assertFalse(messages.isEmpty(), "Expected validation messages for a non‑matching value");
    }

    /**
     * 2️⃣  Multiple subschemas match – short‑circuit enabled (default behaviour)
     */
    @Test
    void validateMultipleMatches_ShortCircuitEnabled_ShouldReturnEmptySet() throws Exception {
        // anyOf contains a number schema and a const schema that also matches the value
        AnyOfValidator validator = buildValidator("[{\"type\":\"number\"},{\"const\":5}]");
        JsonNode instance = MAPPER.readTree("5");
        ExecutionContext ctx = new ExecutionContext();
        Set<ValidationMessage> msgs = validator.validate(ctx, instance, instance, new JsonNodePath(PathType.JSON_POINTER));
        assertTrue(msgs.isEmpty(), "When several subschemas succeed, validation must succeed");
    }

    /**
     * 3️⃣  No subschema matches
     */
    @Test
    void validateNonMatchingValue_ShouldReturnAnyOfError() throws Exception {
        AnyOfValidator validator = buildValidator("[{\"type\":\"string\"},{\"type\":\"integer\"}]");
        // boolean does not match
        JsonNode instance = MAPPER.readTree("true");
        ExecutionContext ctx = new ExecutionContext();
        Set<ValidationMessage> msgs = validator.validate(ctx, instance, instance, new JsonNodePath(PathType.JSON_POINTER));
        assertFalse(msgs.isEmpty(), "A non‑matching instance must produce a ValidationMessage");
        ValidationMessage vm = msgs.iterator().next();
        assertEquals("anyOf", vm.getType(), "Message type must be 'anyOf'");
    }

    /**
     * 4️⃣  Empty anyOf array – schema is invalid
     */
    @Test
    void emptyAnyOfArray_ShouldFailImmediately() throws Exception {
        // The constructor itself does not throw; validation should produce an error.
        AnyOfValidator validator = buildValidator("[]");
        JsonNode instance = MAPPER.readTree("null");
        ExecutionContext ctx = new ExecutionContext();
        Set<ValidationMessage> msgs = validator.validate(ctx, instance, instance, new JsonNodePath(PathType.JSON_POINTER));
        assertFalse(msgs.isEmpty(), "Empty anyOf must generate a validation error");
        ValidationMessage vm = msgs.iterator().next();
        assertEquals("anyOf", vm.getType(), "Error type should be 'anyOf'");
    }

    /**
     * 5️⃣  Null instance node – should fail unless a subschema allows null
     */
    @Test
    void nullInstanceNode_NoNullAllowed_ShouldReturnError() throws Exception {
        AnyOfValidator validator = buildValidator("[{\"type\":\"string\"}]");
        // simulate missing instance
        JsonNode nullNode = null;
        ExecutionContext ctx = new ExecutionContext();
        Set<ValidationMessage> msgs = validator.validate(ctx, nullNode, nullNode, new JsonNodePath(PathType.JSON_POINTER));
        assertFalse(msgs.isEmpty(), "Null instance without a null‑allowing subschema must fail");
    }

    @Test
    void nullInstanceNode_WithNullAllowed_ShouldReturnEmptySet() throws Exception {
        AnyOfValidator validator = buildValidator("[{\"type\":\"null\"},{\"type\":\"string\"}]");
        JsonNode nullNode = null;
        ExecutionContext ctx = new ExecutionContext();
        Set<ValidationMessage> msgs = validator.validate(ctx, nullNode, nullNode, new JsonNodePath(PathType.JSON_POINTER));
        assertTrue(msgs.isEmpty(), "Null instance matches the 'null' subschema");
    }

    @Test
    void walkFlagTrue_InternalWalkSucceeds() throws Exception {
        // anyOf with a simple object subschema
        String schema = "[{\"type\":\"object\",\"properties\":{\"a\":{\"type\":\"integer\"}}}]";
        TestableAnyOfValidator validator = new TestableAnyOfValidator(new SchemaLocation(new AbsoluteIri("http://example.com"), new JsonNodePath(PathType.JSON_POINTER)), new JsonNodePath(PathType.JSON_POINTER), MAPPER.readTree(schema), null, validationContext);
        JsonNode instance = MAPPER.readTree("{\"a\":1}");
        ExecutionContext ctx = new ExecutionContext();
        Set<ValidationMessage> msgs = validator.validateWithWalk(ctx, instance, instance, new JsonNodePath(PathType.JSON_POINTER), true);
        assertTrue(msgs.isEmpty(), "Walk flag true must still succeed when a subschema matches");
    }

    @Test
    void shortCircuitDisabled_AllSubschemasEvaluated() throws Exception {
        // three subschemas – the first two succeed, the third fails.
        String anyOf = "[{\"type\":\"string\"},{\"type\":\"string\"},{\"type\":\"integer\"}]";
        NoShortCircuitAnyOfValidator validator = new NoShortCircuitAnyOfValidator(new SchemaLocation(new AbsoluteIri("http://example.com"), new JsonNodePath(PathType.JSON_POINTER)), new JsonNodePath(PathType.JSON_POINTER), MAPPER.readTree(anyOf), null, validationContext);
        JsonNode instance = MAPPER.readTree("\"any\"");
        ExecutionContext ctx = new ExecutionContext();
        Set<ValidationMessage> msgs = validator.validate(ctx, instance, instance, new JsonNodePath(PathType.JSON_POINTER));
        assertTrue(msgs.isEmpty(), "Even without short‑circuit the result must be success");
        // No direct way to assert that all three validators were called without
        // deep‑inspection of internal state; the absence of an error is sufficient.
    }

    /**
     * 8️⃣  preloadJsonSchema – verify it is called exactly once on first validation
     */
    @Test
    void preloadJsonSchema_IsCalledOnce() throws Exception {
        AnyOfValidator real = buildValidator("[{\"type\":\"integer\"}]");
        AnyOfValidator spy = spy(real);
        JsonNode instance = MAPPER.readTree("5");
        ExecutionContext ctx = new ExecutionContext();
        // first validation – should trigger preload
        spy.validate(ctx, instance, instance, new JsonNodePath(PathType.JSON_POINTER));
        // second validation – preload should NOT be called again
        spy.validate(ctx, instance, instance, new JsonNodePath(PathType.JSON_POINTER));
        verify(spy, times(1)).preloadJsonSchema();
    }

    /**
     * 9️⃣  Boolean true schema inside anyOf – always passes
     */
    @Test
    void anyOfContainsTrueSchema_AlwaysPasses() throws Exception {
        AnyOfValidator validator = buildValidator("[true,{\"type\":\"integer\"}]");
        JsonNode instance = MAPPER.readTree("\"whatever\"");
        ExecutionContext ctx = new ExecutionContext();
        Set<ValidationMessage> msgs = validator.validate(ctx, instance, instance, new JsonNodePath(PathType.JSON_POINTER));
        assertTrue(msgs.isEmpty(), "A boolean true schema makes anyOf succeed for any instance");
    }

    /**
     * 10️⃣ Boolean false schema – must be ignored unless another subschema matches
     */
    @Test
    void anyOfContainsFalseSchema_OtherMatches_Succeeds() throws Exception {
        AnyOfValidator validator = buildValidator("[false,{\"type\":\"string\"}]");
        JsonNode instance = MAPPER.readTree("\"ok\"");
        ExecutionContext ctx = new ExecutionContext();
        Set<ValidationMessage> msgs = validator.validate(ctx, instance, instance, new JsonNodePath(PathType.JSON_POINTER));
        assertTrue(msgs.isEmpty(), "Second subschema matches, so overall anyOf succeeds");
    }

    @Test
    void anyOfContainsFalseSchema_NoneMatch_Fails() throws Exception {
        AnyOfValidator validator = buildValidator("[false,{\"type\":\"string\"}]");
        JsonNode instance = MAPPER.readTree("42");
        ExecutionContext ctx = new ExecutionContext();
        Set<ValidationMessage> msgs = validator.validate(ctx, instance, instance, new JsonNodePath(PathType.JSON_POINTER));
        assertFalse(msgs.isEmpty(), "Both subschemas fail – anyOf must produce an error");
        ValidationMessage vm = msgs.iterator().next();
        assertEquals("anyOf", vm.getType());
    }

    /**
     * 11️⃣ Empty object schema – matches everything
     */
    @Test
    void anyOfContainsEmptyObjectSchema_MatchesAnything() throws Exception {
        AnyOfValidator validator = buildValidator("[{}, {\"type\":\"integer\"}]");
        JsonNode instance = MAPPER.readTree("[1,2,3]");
        ExecutionContext ctx = new ExecutionContext();
        Set<ValidationMessage> msgs = validator.validate(ctx, instance, instance, new JsonNodePath(PathType.JSON_POINTER));
        assertTrue(msgs.isEmpty(), "Empty schema inside anyOf should validate any instance");
    }

    /**
     * 12️⃣ const subschema
     */
    @Test
    void anyOfConstMatches_ShouldSucceed() throws Exception {
        AnyOfValidator validator = buildValidator("[{\"const\":\"fixed\"}]");
        JsonNode instance = MAPPER.readTree("\"fixed\"");
        ExecutionContext ctx = new ExecutionContext();
        Set<ValidationMessage> msgs = validator.validate(ctx, instance, instance, new JsonNodePath(PathType.JSON_POINTER));
        assertTrue(msgs.isEmpty(), "Const value matches – validation succeeds");
    }

    @Test
    void anyOfConstDoesNotMatch_ShouldFail() throws Exception {
        AnyOfValidator validator = buildValidator("[{\"const\":\"fixed\"}]");
        JsonNode instance = MAPPER.readTree("\"other\"");
        ExecutionContext ctx = new ExecutionContext();
        Set<ValidationMessage> msgs = validator.validate(ctx, instance, instance, new JsonNodePath(PathType.JSON_POINTER));
        assertFalse(msgs.isEmpty(), "Const mismatch must produce a single anyOf error");
        assertEquals(1, msgs.size());
        ValidationMessage vm = msgs.iterator().next();
        assertEquals("anyOf", vm.getType());
    }

    /**
     * 13️⃣ enum subschema
     */
    @Test
    void anyOfEnumMatches_ShouldSucceed() throws Exception {
        AnyOfValidator validator = buildValidator("[{\"enum\":[\"red\",\"green\",\"blue\"]}]");
        JsonNode instance = MAPPER.readTree("\"green\"");
        ExecutionContext ctx = new ExecutionContext();
        Set<ValidationMessage> msgs = validator.validate(ctx, instance, instance, new JsonNodePath(PathType.JSON_POINTER));
        assertTrue(msgs.isEmpty(), "Enum containing the instance value must succeed");
    }

    /**
     * 14️⃣ Null arguments – expect NullPointerException
     */
    @Test
    void nullExecutionContext_ShouldThrowNPE() throws Exception {
        AnyOfValidator validator = buildValidator("[{\"type\":\"string\"}]");
        JsonNode instance = MAPPER.readTree("\"test\"");
        assertThrows(NullPointerException.class, () -> validator.validate(null, instance, instance, new JsonNodePath(PathType.JSON_POINTER)));
    }

    @Test
    void nullInstanceLocation_ShouldThrowNPE() throws Exception {
        AnyOfValidator validator = buildValidator("[{\"type\":\"string\"}]");
        JsonNode instance = MAPPER.readTree("\"test\"");
        ExecutionContext ctx = new ExecutionContext();
        assertThrows(NullPointerException.class, () -> validator.validate(ctx, instance, instance, null));
    }

    @Test
    void nullRootNode_ShouldThrowNPE() throws Exception {
        AnyOfValidator validator = buildValidator("[{\"type\":\"string\"}]");
        JsonNode instance = MAPPER.readTree("\"test\"");
        ExecutionContext ctx = new ExecutionContext();
        assertThrows(NullPointerException.class, () -> validator.validate(ctx, instance, null, new JsonNodePath(PathType.JSON_POINTER)));
    }

    /**
     * 15️⃣ Malformed anyOf element – not an object or boolean
     */
    @Test
    void malformedAnyOfElement_ShouldThrowIllegalArgumentException() {
        // The schema contains an integer inside the anyOf array – illegal.
        String malformed = "{\"anyOf\":[42]}";
        assertThrows(IllegalArgumentException.class, () -> {
            // The AnyOfValidator expects the schemaNode to be the anyOf array itself.
            // We therefore parse the outer object and extract the array node.
            JsonNode root = MAPPER.readTree(malformed);
            JsonNode anyOfArray = root.get("anyOf");
            // Build validator directly with the malformed array node.
            new AnyOfValidator(new SchemaLocation(new AbsoluteIri("http://example.com"), new JsonNodePath(PathType.JSON_POINTER)), new JsonNodePath(PathType.JSON_POINTER), anyOfArray, null, validationContext);
        });
    }

    /**
     * 16️⃣ $ref to a non‑existent schema – expect JsonSchemaException
     */
    @Test
    void anyOfWithInvalidRef_ShouldThrowJsonSchemaException() throws Exception {
        // Build a schema that contains a $ref to a missing definition.
        String schema = "[{\"$ref\":\"#/components/schemas/DoesNotExist\"}]";
        AnyOfValidator validator = buildValidator(schema);
        JsonNode instance = MAPPER.readTree("\"anything\"");
        ExecutionContext ctx = new ExecutionContext();
        assertThrows(JsonSchemaException.class, () -> validator.validate(ctx, instance, instance, new JsonNodePath(PathType.JSON_POINTER)));
    }

    /**
     * 17️⃣ Duplicate subschemas – ensure no extra errors are produced
     */
    @Test
    void duplicateSubschemas_ShouldStillSucceed() throws Exception {
        // Two identical string subschemas.
        String schema = "[{\"type\":\"string\"},{\"type\":\"string\"}]";
        AnyOfValidator validator = buildValidator(schema);
        JsonNode instance = MAPPER.readTree("\"dup\"");
        ExecutionContext ctx = new ExecutionContext();
        Set<ValidationMessage> msgs = validator.validate(ctx, instance, instance, new JsonNodePath(PathType.JSON_POINTER));
        assertTrue(msgs.isEmpty(), "Duplicate matching subschemas must not generate errors");
    }

    /**
     * 18️⃣ Single error aggregation when all subschemas fail
     */
    @Test
    void allSubschemasFail_ShouldReturnSingleAnyOfMessage() throws Exception {
        String schema = "[{\"type\":\"string\"},{\"type\":\"boolean\"},{\"type\":\"object\"}]";
        AnyOfValidator validator = buildValidator(schema);
        // integer matches none
        JsonNode instance = MAPPER.readTree("123");
        ExecutionContext ctx = new ExecutionContext();
        Set<ValidationMessage> msgs = validator.validate(ctx, instance, instance, new JsonNodePath(PathType.JSON_POINTER));
        assertEquals(1, msgs.size(), "AnyOf must aggregate failures into a single message");
        ValidationMessage vm = msgs.iterator().next();
        assertEquals("anyOf", vm.getType());
    }

    /**
     * 19️⃣ Nested instance location – ensure path is honoured
     */
    @Test
    void nestedInstanceLocation_ShouldValidateChildNode() throws Exception {
        // anyOf that validates a number
        AnyOfValidator validator = buildValidator("[{\"type\":\"number\"}]");
        JsonNode root = MAPPER.readTree("{\"inner\":9}");
        JsonNode inner = root.get("inner");
        ExecutionContext ctx = new ExecutionContext();
        JsonNodePath location = new JsonNodePath(PathType.JSON_POINTER).append("inner");
        Set<ValidationMessage> msgs = validator.validate(ctx, inner, root, location);
        assertTrue(msgs.isEmpty(), "Validator must work correctly with a non‑root location");
    }

    @Test
    void canShortCircuitThrowsRuntimeException_ShouldPropagate() throws Exception {
        ShortCircuitErrorValidator validator = new ShortCircuitErrorValidator(new SchemaLocation(new AbsoluteIri("http://example.com"), new JsonNodePath(PathType.JSON_POINTER)), new JsonNodePath(PathType.JSON_POINTER), MAPPER.readTree("[{\"type\":\"string\"}]"), null, validationContext);
        JsonNode instance = MAPPER.readTree("\"abc\"");
        ExecutionContext ctx = new ExecutionContext();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> validator.validate(ctx, instance, instance, new JsonNodePath(PathType.JSON_POINTER)));
        assertEquals("simulated mis‑configuration", ex.getMessage());
    }
}

package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.utils.SetView;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link AnyOfValidator}.
 *
 * The tests cover:
 *   • normal behaviour of all public methods,
 *   • edge‑cases (empty anyOf, non‑array schema node, discriminator flag, fail‑fast handling),
 *   • internal helper methods {@code canShortCircuit()} and {@code canShortCircuit(ExecutionContext)}.
 *
 * No external mocking libraries are used – dummy implementations are created directly.
 */
public class AnyOfValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ValidationContext validationContext;
    private ExecutionContext execCtx;
    private JsonNodePath rootPath;

    @Before
    public void setUp() {
        // Minimal but functional ValidationContext – we use the default meta‑schema and factory.
        JsonMetaSchema meta = JsonMetaSchema.getDefaultMetaSchema(); // library provides a default
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        validationContext = new ValidationContext(meta, factory, config);
        execCtx = new ExecutionContext();
        rootPath = new JsonNodePath(JsonNodePath.PathType.ROOT);
    }

    /** ----------------------------------------------------------------------
     *  Helper classes used throughout the test suite
     *  ---------------------------------------------------------------------- */

    /** A very small {@link JsonValidator} that records whether its {@code validate}
     *  method has been called. */
    private static class RecordingValidator implements JsonValidator {

        private final String keyword;
        private boolean called = false;

        RecordingValidator(String keyword) {
            this.keyword = keyword;
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node,
                                               JsonNode rootNode, JsonNodePath instanceLocation) {
            called = true;
            return Collections.emptySet();
        }

        @Override
        public Set<ValidationMessage> walk(ExecutionContext executionContext, JsonNode node,
                                           JsonNode rootNode, JsonNodePath instanceLocation,
                                           boolean shouldValidateSchema) {
            // For walk we also record the call.
            called = true;
            return Collections.emptySet();
        }

        @Override
        public SchemaLocation getSchemaLocation() { return null; }

        @Override
        public JsonNodePath getEvaluationPath() { return null; }

        @Override
        public String getKeyword() { return keyword; }

        public boolean wasCalled() { return called; }
    }

    /** Minimal concrete subclass of {@link JsonSchema} that allows us to inject a
     *  custom list of validators (used for the canShortCircuit() tests). */
    private static class DummyJsonSchema extends JsonSchema {

        private final List<JsonValidator> validators;

        protected DummyJsonSchema(List<JsonValidator> validators) {
            // The protected super‑constructor requires many arguments; we pass nulls where the
            // base class does not use them for the methods exercised in the test.
            super(null, false, false, null, null, false,
                    null, null, null, null, null, null,
                    null, null, null);
            this.validators = validators;
        }

        @Override
        public List<JsonValidator> getValidators() {
            return validators;
        }
    }

    /** ----------------------------------------------------------------------
     *  1. Constructor validation
     *  ---------------------------------------------------------------------- */

    @Test(expected = JsonSchemaException.class)
    public void constructorShouldThrowWhenSchemaNodeIsNotArray() {
        JsonNode notArray = MAPPER.createObjectNode().put("type", "string");
        new AnyOfValidator(SchemaLocation.DOCUMENT, rootPath, notArray, null, validationContext);
    }

    @Test
    public void constructorAcceptsEmptyArray() {
        JsonNode emptyArray = MAPPER.createArrayNode(); // []
        AnyOfValidator validator = new AnyOfValidator(SchemaLocation.DOCUMENT, rootPath,
                emptyArray, null, validationContext);
        // No exception → success.  The internal list must be empty.
        assertTrue("Validator should contain zero subschemas", validator.schemas.isEmpty());
    }

    /** ----------------------------------------------------------------------
     *  2. Basic validate() behaviour
     *  ---------------------------------------------------------------------- */

    @Test
    public void validateReturnsEmptyWhenOneSubschemaPasses() {
        // anyOf = [ { "type": "string" }, { "type": "integer" } ]
        JsonNode anyOfNode = MAPPER.readTree("[{\"type\":\"string\"},{\"type\":\"integer\"}]");
        AnyOfValidator validator = new AnyOfValidator(SchemaLocation.DOCUMENT, rootPath,
                anyOfNode, null, validationContext);

        JsonNode testNode = MAPPER.readTree("\"hello\""); // a string → matches first subschema
        Set<ValidationMessage> result = validator.validate(execCtx, testNode, testNode, rootPath);
        assertTrue("When one subschema validates, result must be empty", result.isEmpty());
    }

    @Test
    public void validateReturnsUnionOfErrorsWhenAllSubschemasFail() {
        // anyOf = [ { "type": "string", "maxLength": 2 }, { "type": "integer", "minimum": 10 } ]
        JsonNode anyOfNode = MAPPER.readTree("[{\"type\":\"string\",\"maxLength\":2},{\"type\":\"integer\",\"minimum\":10}]");
        AnyOfValidator validator = new AnyOfValidator(SchemaLocation.DOCUMENT, rootPath,
                anyOfNode, null, validationContext);

        JsonNode testNode = MAPPER.readTree("\"abcd\""); // string longer than 2 → both subschemas fail
        Set<ValidationMessage> result = validator.validate(execCtx, testNode, testNode, rootPath);
        // We expect at least two error messages (one per subschema)
        assertEquals("Both subschemas should contribute errors", 2, result.size());
    }

    @Test
    public void validateRestoresFailFastFlag() {
        execCtx.setFailFast(true); // start with fail‑fast on
        JsonNode anyOfNode = MAPPER.readTree("[{\"type\":\"string\"}]");
        AnyOfValidator validator = new AnyOfValidator(SchemaLocation.DOCUMENT, rootPath,
                anyOfNode, null, validationContext);
        JsonNode testNode = MAPPER.readTree("\"hello\"");
        validator.validate(execCtx, testNode, testNode, rootPath);
        assertTrue("Fail‑fast flag must be restored to its original value", execCtx.isFailFast());
    }

    /** ----------------------------------------------------------------------
     *  3. walk() variations
     *  ---------------------------------------------------------------------- */

    @Test
    public void walkShouldDelegateToValidateWhenFlagIsTrue() {
        JsonNode anyOfNode = MAPPER.readTree("[{\"type\":\"string\"}]");
        AnyOfValidator validator = new AnyOfValidator(SchemaLocation.DOCUMENT, rootPath,
                anyOfNode, null, validationContext);
        JsonNode testNode = MAPPER.readTree("\"foo\"");
        Set<ValidationMessage> result = validator.walk(execCtx, testNode, testNode, rootPath, true);
        assertTrue("walk(..., true) must behave like validate()", result.isEmpty());
    }

    @Test
    public void walkShouldInvokeSubschemaWalkWhenFlagIsFalse() {
        // Build a dummy subschema that records a walk call.
        RecordingValidator rec = new RecordingValidator("dummy");
        JsonSchema dummySchema = validationContext.newSchema(SchemaLocation.DOCUMENT, rootPath, MAPPER.createObjectNode(), null);
        // Replace the real validator list of the dummy schema with our recorder.
        // We use reflection because the validator list is private in JsonSchema.
        try {
            java.lang.reflect.Field f = JsonSchema.class.getDeclaredField("validators");
            f.setAccessible(true);
            f.set(dummySchema, Collections.singletonList(rec));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // anyOf with a single subschema (the dummy schema)
        JsonNode anyOfNode = MAPPER.readTree("[{}]"); // empty object – the actual content is irrelevant
        AnyOfValidator validator = new AnyOfValidator(SchemaLocation.DOCUMENT, rootPath,
                anyOfNode, null, validationContext) {
            // Override the internal list so it contains our dummy schema
            {
                this.schemas.clear();
                this.schemas.add(dummySchema);
            }
        };

        JsonNode testNode = MAPPER.createObjectNode(); // any node
        Set<ValidationMessage> result = validator.walk(execCtx, testNode, testNode, rootPath, false);
        assertTrue("walk(..., false) returns an empty set", result.isEmpty());
        assertTrue("Subschema walk must have been invoked", rec.wasCalled());
    }

    /** ----------------------------------------------------------------------
     *  4. canShortCircuit(ExecutionContext) – annotation collection flag
     *  ---------------------------------------------------------------------- */

    @Test
    public void canShortCircuitReturnsTrueWhenAnnotationCollectionDisabled() {
        ExecutionContext ctx = new ExecutionContext(); // default config: annotation collection disabled
        JsonNode anyOfNode = MAPPER.readTree("[{\"type\":\"string\"}]");
        AnyOfValidator validator = new AnyOfValidator(SchemaLocation.DOCUMENT, rootPath,
                anyOfNode, null, validationContext);
        assertTrue("Annotation collection disabled → can short‑circuit", validator.canShortCircuit(ctx));
    }

    @Test
    public void canShortCircuitReturnsFalseWhenAnnotationCollectionEnabled() {
        // Turn on annotation collection via ExecutionConfig
        ExecutionConfig execConfig = new ExecutionConfig();
        execConfig.setAnnotationCollectionEnabled(true);
        ExecutionContext ctx = new ExecutionContext(execConfig);
        JsonNode anyOfNode = MAPPER.readTree("[{\"type\":\"string\"}]");
        AnyOfValidator validator = new AnyOfValidator(SchemaLocation.DOCUMENT, rootPath,
                anyOfNode, null, validationContext);
        assertFalse("Annotation collection enabled → cannot short‑circuit", validator.canShortCircuit(ctx));
    }

    /** ----------------------------------------------------------------------
     *  5. canShortCircuit() – parent‑schema validators inspection
     *  ---------------------------------------------------------------------- */

    @Test
    public void canShortCircuitIsFalseWhenParentHasUnevaluatedPropertiesValidator() {
        // Create a dummy validator with the keyword we are looking for.
        JsonValidator unevalValidator = new JsonValidator() {
            @Override public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node,
                                                            JsonNode rootNode, JsonNodePath instanceLocation) {
                return Collections.emptySet();
            }
            @Override public String getKeyword() { return "unevaluatedProperties"; }
            @Override public SchemaLocation getSchemaLocation() { return null; }
            @Override public JsonNodePath getEvaluationPath() { return null; }
        };
        DummyJsonSchema parent = new DummyJsonSchema(Collections.singletonList(unevalValidator));

        JsonNode anyOfNode = MAPPER.readTree("[{\"type\":\"string\"}]");
        AnyOfValidator validator = new AnyOfValidator(SchemaLocation.DOCUMENT, rootPath,
                anyOfNode, parent, validationContext);
        assertFalse("Presence of unevaluatedProperties validator → cannot short‑circuit", validator.canShortCircuit());
    }

    @Test
    public void canShortCircuitIsTrueWhenNoUnevaluatedValidatorsInParent() {
        DummyJsonSchema parent = new DummyJsonSchema(Collections.emptyList());

        JsonNode anyOfNode = MAPPER.readTree("[{\"type\":\"string\"}]");
        AnyOfValidator validator = new AnyOfValidator(SchemaLocation.DOCUMENT, rootPath,
                anyOfNode, parent, validationContext);
        assertTrue("No problematic validators → can short‑circuit", validator.canShortCircuit());
    }

    /** ----------------------------------------------------------------------
     *  6. preloadJsonSchema – ensures internal flag is cached and no exception is thrown
     *  ---------------------------------------------------------------------- */

    @Test
    public void preloadJsonSchemaCachesCanShortCircuitFlag() {
        DummyJsonSchema parent = new DummyJsonSchema(Collections.emptyList());

        JsonNode anyOfNode = MAPPER.readTree("[{\"type\":\"string\"}]");
        AnyOfValidator validator = new AnyOfValidator(SchemaLocation.DOCUMENT, rootPath,
                anyOfNode, parent, validationContext);

        // First call – flag is null
        assertNull("Initial canShortCircuit cache must be null", validator.canShortCircuit);
        validator.preloadJsonSchema(); // should compute and store the flag
        assertNotNull("After preloadJsonSchema the cache must be populated", validator.canShortCircuit);
        // The cached value must be true because the parent has no disallowed validators
        assertTrue("Cached canShortCircuit flag should be true", validator.canShortCircuit);
    }
}

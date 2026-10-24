package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link AnyOfValidator}.
 *
 * <p>The tests deliberately avoid any external mocking frameworks and use only the
 * public constructors / methods that are part of the library.  In order to be able
 * to inject deterministic behaviour we create a very small {@link TestSchema}
 * implementation that extends {@link JsonSchema} and allows us to preset the
 * validation result for each subschema.</p>
 *
 * <p>Because {@link AnyOfValidator#validate(ExecutionContext, JsonNode, JsonNode, String)}
 * depends on a {@link CollectorContext} that holds a {@link ValidatorState}, the
 * test creates a minimal {@link ExecutionContext} with a freshly created
 * {@link CollectorContext}.  The {@link CollectorContext} implementation is part
 * of the library – the default constructor creates a usable instance.</p>
 *
 * <p>The test suite covers:</p>
 * <ul>
 *   <li>Construction of the validator (including the discriminator flag)</li>
 *   <li>Successful validation when at least one subschema validates</li>
 *   <li>Failure when no subschema validates</li>
 *   <li>The early‑return optimisation when a subschema validates and discriminators are disabled</li>
 *   <li>The {@code walk(...)} method in both {@code shouldValidateSchema=true} and {@code false}
 *       modes</li>
 *   <li>The {@code preloadJsonSchema()} delegation to the child schemas</li>
 * </ul>
 *
 * <p>All tests are written for JUnit‑4 and therefore use {@code @Test} together
 * with the classic {@code assert*} methods.</p>
 */
public class AnyOfValidatorTest {

    /** Jackson mapper used to build JSON nodes for the tests. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** The validator under test. */
    private AnyOfValidator validator;

    /** Execution context used for every call to {@code validate} / {@code walk}. */
    private ExecutionContext execContext;

    /** The two child schemas that AnyOfValidator creates from the supplied schema array. */
    private TestSchema firstSchema;
    private TestSchema secondSchema;

    /**
     * Helper configuration that disables OpenAPI‑3 discriminators (the default
     * configuration of the library).  The flag is required because a few code
     * paths in {@code AnyOfValidator#validate} are only executed when the flag
     * is {@code true}.
     */
    private static SchemaValidatorsConfig defaultConfig() {
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        // The flag is {@code false} by default – we set it explicitly for clarity.
        cfg.setOpenAPI3StyleDiscriminators(false);
        return cfg;
    }

    /**
     * Minimal {@link ValidationContext} implementation that returns {@link TestSchema}
     * instances from {@link #newSchema(String, JsonNode, JsonSchema)}.
     *
     * <p>The constructor forwards {@code null} for the factories and meta‑schema
     * because the {@code TestSchema} implementation does not use them.  This is
     * safe for the unit tests because none of the overridden methods rely on the
     * original {@link ValidationContext} behaviour.</p>
     */
    private static class StubValidationContext extends ValidationContext {

        private final SchemaValidatorsConfig config;

        StubValidationContext(SchemaValidatorsConfig config) {
            super(null, null, null, null, config);
            this.config = config;
        }

        @Override
        public JsonSchema newSchema(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema) {
            // The returned schema is a {@link TestSchema} that we can control from the test.
            return new TestSchema(this, schemaPath, schemaNode);
        }

        @Override
        public SchemaValidatorsConfig getConfig() {
            return config;
        }

        // All other methods inherit the default (mostly {@code null}) behaviour – they are
        // not exercised by the test suite.
    }

    /**
     * A tiny concrete {@link JsonSchema} that lets the test decide what
     * {@link #validate(ExecutionContext, JsonNode, JsonNode, String)} should return.
     *
     * <p>Only the methods that are used by {@link AnyOfValidator} are overridden.
     * All other behaviour (e.g. {@code walk}) simply delegates to the superclass.</p>
     */
    private static class TestSchema extends JsonSchema {

        /** If {@code true} the schema pretends to have a {@link TypeValidator}. */
        private final boolean hasTypeValidator;

        /** The {@link TypeValidator} to return when {@code hasTypeValidator} is {@code true}. */
        private final TypeValidator typeValidator;

        /** Errors that should be returned from {@code validate}. */
        private Set<ValidationMessage> presetErrors = Collections.emptySet();

        /** Flags used by the test to verify that the schema was visited. */
        private boolean walkCalled = false;

        /** Flag used by the test to verify that {@code preloadJsonSchema} was invoked. */
        private boolean preloadCalled = false;

        TestSchema(ValidationContext ctx, String schemaPath, JsonNode schemaNode) {
            // The super‑constructor that accepts a {@code ValidationContext}, a schema path,
            // a (possibly {@code null}) base URI, the schema node and the parent schema.
            super(ctx, schemaPath, null, schemaNode, null);
            // The test does not need a real type validator – we simply set the flag to {@code false}.
            this.hasTypeValidator = false;
            this.typeValidator = null;
        }

        /** Allows the test to inject a custom error set. */
        void setPresetErrors(Set<ValidationMessage> errors) {
            this.presetErrors = (errors == null) ? Collections.emptySet() : errors;
        }

        /** Returns {@code true} when the test wants the validator to think that a type validator exists. */
        @Override
        public boolean hasTypeValidator() {
            return hasTypeValidator;
        }

        /** Returns the injected {@link TypeValidator} (always {@code null} in this test). */
        @Override
        public TypeValidator getTypeValidator() {
            return typeValidator;
        }

        /** The core validation logic used by {@link AnyOfValidator}. */
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                                JsonNode jsonNode,
                                                JsonNode rootNode,
                                                String at) {
            return presetErrors;
        }

        /** Walk implementation used by the {@code walk(..., false)} test. */
        @Override
        public Set<ValidationMessage> walk(ExecutionContext executionContext,
                                            JsonNode jsonNode,
                                            JsonNode rootNode,
                                            String at,
                                            boolean shouldValidateSchema) {
            walkCalled = true;
            // For the purpose of the test we simply forward to the super implementation.
            return super.walk(executionContext, jsonNode, rootNode, at, shouldValidateSchema);
        }

        /** Pre‑load method – we only record that it was called. */
        @Override
        public void preloadJsonSchema() {
            preloadCalled = true;
        }

        /** Helper for the test to check whether {@code walk} was invoked. */
        boolean isWalkCalled() {
            return walkCalled;
        }

        /** Helper for the test to check whether {@code preloadJsonSchema} was invoked. */
        boolean isPreloadCalled() {
            return preloadCalled;
        }
    }

    /**
     * Build an {@code AnyOfValidator} instance with two child {@link TestSchema}
     * objects.  The method also extracts the generated child schemas so that the
     * test can manipulate them (e.g. inject preset errors).
     */
    @Before
    public void setUp() throws Exception {
        // ---- 1. Build a very small ANY‑OF schema (an array with two empty objects) ----
        ArrayNode anyOfArray = MAPPER.createArrayNode();
        anyOfArray.addObject(); // first subschema
        anyOfArray.addObject(); // second subschema

        // ---- 2. Create a stub validation context with the default configuration ----
        StubValidationContext validationContext = new StubValidationContext(defaultConfig());

        // ---- 3. Construct the validator (parentSchema is {@code null}) ----
        validator = new AnyOfValidator("#/anyOf", anyOfArray, null, validationContext);

        // ---- 4. Retrieve the two generated child schemas via reflection ----
        // The field is private – we obtain it reflectively.
        java.lang.reflect.Field schemasField = AnyOfValidator.class.getDeclaredField("schemas");
        schemasField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<JsonSchema> schemas = (List<JsonSchema>) schemasField.get(validator);
        assertEquals("Validator must have created exactly two child schemas", 2, schemas.size());

        // Cast to our concrete TestSchema type – the stub context always returns TestSchema.
        firstSchema = (TestSchema) schemas.get(0);
        secondSchema = (TestSchema) schemas.get(1);

        // ---- 5. Build a fresh execution context (collector context is created internally) ----
        execContext = new ExecutionContext(); // the default ctor creates an empty CollectorContext
    }

    /**
     * Helper that creates a {@link ValidationMessage} with a custom message.
     *
     * The library provides a static factory method – we use it to avoid dealing
     * with the internal {@code MessageFormat} objects.
     */
    private static ValidationMessage message(String type, String at, String schemaPath, String... args) {
        return ValidationMessage.of(type,
                ErrorMessageType.ERROR,
                null,
                at,
                schemaPath,
                args);
    }

    /**
     * Verify that when **both** subschemas validate successfully the validator
     * returns an empty error set.
     */
    @Test
    public void testValidateAllValidSubschemas() {
        // Both child schemas return an empty error set.
        firstSchema.setPresetErrors(Collections.emptySet());
        secondSchema.setPresetErrors(Collections.emptySet());

        JsonNode instance = MAPPER.createObjectNode(); // the concrete node does not matter

        Set<ValidationMessage> result = validator.validate(execContext, instance, instance, "#");

        assertTrue("All subschemas are valid → result must be empty", result.isEmpty());
    }

    /**
     * Verify that when **one** subschema validates and the other returns errors,
     * the validator returns an empty set (early return optimisation when
     * discriminators are disabled).
     */
    @Test
    public void testValidateOneValidOneInvalid() {
        // First subschema returns an error, second subschema validates.
        ValidationMessage err = message("type", "#", "#/anyOf/0", "string");
        firstSchema.setPresetErrors(Collections.singleton(err));
        secondSchema.setPresetErrors(Collections.emptySet());

        JsonNode instance = MAPPER.textNode("valid string");

        Set<ValidationMessage> result = validator.validate(execContext, instance, instance, "#");

        // Because the second subschema succeeded, AnyOfValidator must clear all errors.
        assertTrue("At least one subschema succeeded → result must be empty", result.isEmpty());
    }

    /**
     * Verify that when **no** subschema validates, the validator returns the
     * union of all error messages.
     */
    @Test
    public void testValidateAllInvalidSubschemas() {
        ValidationMessage err1 = message("type", "#", "#/anyOf/0", "string");
        ValidationMessage err2 = message("type", "#", "#/anyOf/1", "integer");
        firstSchema.setPresetErrors(Collections.singleton(err1));
        secondSchema.setPresetErrors(Collections.singleton(err2));

        JsonNode instance = MAPPER.booleanNode(true); // a value that matches neither schema

        Set<ValidationMessage> result = validator.validate(execContext, instance, instance, "#");

        // Both errors must be present.
        assertEquals("Both subschemas failed → result must contain both errors", 2, result.size());
        assertTrue("Result must contain error from first subschema", result.contains(err1));
        assertTrue("Result must contain error from second subschema", result.contains(err2));
    }

    /**
     * Test the {@code walk(...)} method when {@code shouldValidateSchema} is
     * {@code true}.  The method should simply delegate to {@code validate(...)}.
     */
    @Test
    public void testWalkValidateTrueDelegatesToValidate() {
        // Prepare a situation where validation would succeed (both subschemas empty).
        firstSchema.setPresetErrors(Collections.emptySet());
        secondSchema.setPresetErrors(Collections.emptySet());

        JsonNode instance = MAPPER.createObjectNode();

        Set<ValidationMessage> walkResult = validator.walk(execContext, instance, instance, "#", true);

        // The walk result must be identical to a direct validate call.
        Set<ValidationMessage> validateResult = validator.validate(execContext, instance, instance, "#");
        assertEquals("walk(..., true) must behave like validate(...)", validateResult, walkResult);
    }

    /**
     * Test the {@code walk(...)} method when {@code shouldValidateSchema} is
     * {@code false}.  In this mode the validator must invoke {@code walk(...)} on
     * each child schema but must return an empty set itself.
     */
    @Test
    public void testWalkValidateFalseCallsChildWalks() {
        // Reset the walk‑called flags.
        firstSchema.setPresetErrors(Collections.emptySet());
        secondSchema.setPresetErrors(Collections.emptySet());

        JsonNode instance = MAPPER.createObjectNode();

        Set<ValidationMessage> result = validator.walk(execContext, instance, instance, "#", false);

        // The validator must return an empty set.
        assertTrue("walk(..., false) must return an empty set", result.isEmpty());

        // Both child schemas must have been visited.
        assertTrue("First child schema's walk must have been called", firstSchema.isWalkCalled());
        assertTrue("Second child schema's walk must have been called", secondSchema.isWalkCalled());
    }

    /**
     * Verify that {@code preloadJsonSchema()} correctly forwards the call to each
     * child schema.
     */
    @Test
    public void testPreloadJsonSchemaDelegatesToChildren() {
        // Ensure the flags are false before the call.
        assertFalse("Preload flag must be false initially (first schema)", firstSchema.isPreloadCalled());
        assertFalse("Preload flag must be false initially (second schema)", secondSchema.isPreloadCalled());

        // Invoke the method under test.
        validator.preloadJsonSchema();

        // Both child schemas must have received the call.
        assertTrue("First child schema must have been preloaded", firstSchema.isPreloadCalled());
        assertTrue("Second child schema must have been preloaded", secondSchema.isPreloadCalled());
    }

    /**
     * Validate the behaviour when the validator is created with the OpenAPI‑3
     * discriminator flag turned on.  The flag influences only a few code paths;
     * the test merely asserts that the flag is stored correctly and does not
     * cause an exception during construction.
     */
    @Test
    public void testConstructorWithDiscriminatorFlagEnabled() {
        // Build a configuration that enables discriminators.
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        cfg.setOpenAPI3StyleDiscriminators(true);
        StubValidationContext ctx = new StubValidationContext(cfg);

        // Re‑create the validator with the new context.
        AnyOfValidator discValidator = new AnyOfValidator("#/anyOf", MAPPER.createArrayNode(), null, ctx);

        // The internal discriminatorContext must be non‑null when the flag is true.
        // We use reflection to fetch the private field.
        try {
            java.lang.reflect.Field discField = AnyOfValidator.class.getDeclaredField("discriminatorContext");
            discField.setAccessible(true);
            Object discCtx = discField.get(discValidator);
            assertNotNull("Discriminator context must be created when the flag is enabled", discCtx);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Reflection failed while checking discriminatorContext: " + e.getMessage());
        }
    }
}

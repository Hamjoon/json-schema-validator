package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class AnyOfValidatorTest {

    private ObjectMapper mapper;
    private ValidationContext validationContext;
    private ExecutionContext executionContext;
    private JsonNodePath dummyPath;
    private SchemaLocation dummyLocation;

    @Before
    public void setUp() throws Exception {
        mapper = new ObjectMapper();

        // ValidationContext constructor is part of the public API (see the dependency signatures).
        // We pass null for the factories / meta‑schema – they are not used by the code paths exercised
        // in these tests.  A default, empty configuration is sufficient.
        validationContext = new ValidationContext(
                null,                // URIFactory
                null,                // URNFactory
                null,                // JsonMetaSchema
                null,                // JsonSchemaFactory
                new SchemaValidatorsConfig()  // config – default (discriminators disabled)
        );

        // ExecutionContext has a no‑arg constructor.
        executionContext = new ExecutionContext();

        // dummy objects required by the AnyOfValidator constructor
        dummyPath = new JsonNodePath(JsonNodePath.PathType.JSON);
        dummyLocation = SchemaLocation.DOCUMENT;
    }

    /**
     * Helper that creates an AnyOfValidator for the supplied schema JSON.
     */
    private AnyOfValidator createValidator(String schemaJson) throws Exception {
        JsonNode schemaNode = mapper.readTree(schemaJson);
        return new AnyOfValidator(
                dummyLocation,
                dummyPath,
                schemaNode,
                null,               // parent schema – not needed for these tests
                validationContext
        );
    }

    /**
     * Directly injects a list of schemas into the validator using reflection.
     * This allows us to control the behaviour of the sub‑schemas without having
     * to rely on the heavy internal schema creation machinery.
     */
    private void setSchemas(AnyOfValidator validator, JsonSchema... schemas) throws Exception {
        Field field = AnyOfValidator.class.getDeclaredField("schemas");
        field.setAccessible(true);
        // replace the internal list with a fresh list containing the supplied schemas
        java.util.List<JsonSchema> list = new java.util.ArrayList<>();
        Collections.addAll(list, schemas);
        field.set(validator, list);
    }

    /**
     * A very small stub JsonSchema implementation that simply returns a
     * predefined set of validation messages when {@code validate} or {@code walk}
     * is called.  All other behaviour is delegated to the base class (which
     * will never be used because we override the relevant methods).
     */
    private static class StubJsonSchema extends JsonSchema {
        private final Set<ValidationMessage> errorsToReturn;
        private final boolean walkEnabled;

        StubJsonSchema(Set<ValidationMessage> errors, boolean walkEnabled) {
            // The super‑constructor parameters are not relevant for the stub's behaviour.
            // We therefore pass null / dummy values that satisfy the signature.
            super(
                    SchemaLocation.DOCUMENT,
                    new JsonNodePath(JsonNodePath.PathType.JSON),
                    NullNode.getInstance(),
                    null,
                    ValidatorTypeCode.ANY_OF,
                    null
            );
            this.errorsToReturn = errors != null ? errors : Collections.emptySet();
            this.walkEnabled = walkEnabled;
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                               JsonNode jsonNode,
                                               JsonNode rootNode,
                                               JsonNodePath instanceLocation) {
            return errorsToReturn;
        }

        @Override
        public Set<ValidationMessage> walk(ExecutionContext executionContext,
                                            JsonNode jsonNode,
                                            JsonNode rootNode,
                                            JsonNodePath instanceLocation,
                                            boolean shouldValidateSchema) {
            // when walk is invoked we honour the {@code walkEnabled} flag used by the
            // AnyOfValidator to decide between {@code validate} and {@code walk}.
            return walkEnabled ? errorsToReturn : Collections.emptySet();
        }

        @Override
        public TypeValidator getTypeValidator() {
            // Return null so that AnyOfValidator does not perform any type checks.
            return null;
        }
    }

    @Test
    public void testValidateWithNoSchemasReturnsEmptySet() throws Exception {
        // anyOf with an empty array – the validator should simply return an empty set.
        AnyOfValidator validator = createValidator("{\"anyOf\": []}");
        Set<ValidationMessage> result = validator.validate(
                executionContext,
                NullNode.getInstance(),
                NullNode.getInstance(),
                dummyPath
        );
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testValidateWhenOneSubSchemaSucceeds() throws Exception {
        // Create a validator that contains two stub schemas:
        // – the first returns a non‑empty error set,
        // – the second returns an empty set (i.e. validation succeeded).
        ValidationMessage dummyError = ValidationMessage.builder()
                .code("dummy.error")
                .instanceLocation(dummyPath)
                .message("dummy error")
                .build();

        StubJsonSchema failingSchema = new StubJsonSchema(
                new LinkedHashSet<>(Collections.singletonList(dummyError)),
                false
        );
        StubJsonSchema succeedingSchema = new StubJsonSchema(Collections.emptySet(), false);

        AnyOfValidator validator = createValidator("{\"anyOf\": [{}, {}]}");
        setSchemas(validator, failingSchema, succeedingSchema);

        Set<ValidationMessage> result = validator.validate(
                executionContext,
                NullNode.getInstance(),
                NullNode.getInstance(),
                dummyPath
        );

        // Because one sub‑schema succeeded, all errors should be cleared.
        assertNotNull(result);
        assertTrue("Expected no validation errors when at least one subschema succeeds", result.isEmpty());
    }

    @Test
    public void testValidateWhenAllSubSchemasFail() throws Exception {
        // Both sub‑schemas return errors – the validator should aggregate them.
        ValidationMessage error1 = ValidationMessage.builder()
                .code("error1")
                .instanceLocation(dummyPath)
                .message("first error")
                .build();
        ValidationMessage error2 = ValidationMessage.builder()
                .code("error2")
                .instanceLocation(dummyPath)
                .message("second error")
                .build();

        StubJsonSchema schema1 = new StubJsonSchema(
                new LinkedHashSet<>(Collections.singletonList(error1)),
                false
        );
        StubJsonSchema schema2 = new StubJsonSchema(
                new LinkedHashSet<>(Collections.singletonList(error2)),
                false
        );

        AnyOfValidator validator = createValidator("{\"anyOf\": [{}, {}]}");
        setSchemas(validator, schema1, schema2);

        Set<ValidationMessage> result = validator.validate(
                executionContext,
                NullNode.getInstance(),
                NullNode.getInstance(),
                dummyPath
        );

        assertNotNull(result);
        assertEquals("Two errors should be reported", 2, result.size());
        assertTrue(result.contains(error1));
        assertTrue(result.contains(error2));
    }

    @Test
    public void testWalkDelegatesToValidateWhenFlagTrue() throws Exception {
        // The walk method should delegate to {@code validate} when the flag is true.
        // We verify this by using a stub schema that records whether its {@code validate}
        // method was invoked.
        final boolean[] validateCalled = {false};

        JsonSchema recordingSchema = new StubJsonSchema(Collections.emptySet(), false) {
            @Override
            public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                                   JsonNode jsonNode,
                                                   JsonNode rootNode,
                                                   JsonNodePath instanceLocation) {
                validateCalled[0] = true;
                return super.validate(executionContext, jsonNode, rootNode, instanceLocation);
            }
        };

        AnyOfValidator validator = createValidator("{\"anyOf\": [{}]}");
        setSchemas(validator, recordingSchema);

        // invoke walk with shouldValidateSchema = true
        Set<ValidationMessage> walkResult = validator.walk(
                executionContext,
                NullNode.getInstance(),
                NullNode.getInstance(),
                dummyPath,
                true
        );

        assertTrue("validate should have been called from walk", validateCalled[0]);
        // The result should be the same as the underlying validate call (empty set in this case)
        assertTrue(walkResult.isEmpty());
    }

    @Test
    public void testWalkSkipsValidationWhenFlagFalse() throws Exception {
        // When the flag is false the validator must walk each sub‑schema with
        // {@code shouldValidateSchema = false}.  Our stub records the flag.
        final boolean[] walkFlagSeen = {false};

        JsonSchema recordingSchema = new StubJsonSchema(Collections.emptySet(), false) {
            @Override
            public Set<ValidationMessage> walk(ExecutionContext executionContext,
                                               JsonNode jsonNode,
                                               JsonNode rootNode,
                                               JsonNodePath instanceLocation,
                                               boolean shouldValidateSchema) {
                walkFlagSeen[0] = shouldValidateSchema;
                // Return empty set – the AnyOfValidator will aggregate nothing.
                return Collections.emptySet();
            }
        };

        AnyOfValidator validator = createValidator("{\"anyOf\": [{}]}");
        setSchemas(validator, recordingSchema);

        Set<ValidationMessage> result = validator.walk(
                executionContext,
                NullNode.getInstance(),
                NullNode.getInstance(),
                dummyPath,
                false
        );

        // The flag passed to the sub‑schema walk must be false.
        assertFalse("Sub‑schema walk should receive shouldValidateSchema = false", walkFlagSeen[0]);
        // walk should return an empty set (no validation performed)
        assertTrue(result.isEmpty());
    }

    @Test
    public void testPreloadJsonSchemaDoesNotThrow() throws Exception {
        // Pre‑loading simply forwards to the protected helper – we only assert that
        // no exception is thrown for a validator with a couple of stub schemas.
        StubJsonSchema schemaA = new StubJsonSchema(Collections.emptySet(), false);
        StubJsonSchema schemaB = new StubJsonSchema(Collections.emptySet(), false);

        AnyOfValidator validator = createValidator("{\"anyOf\": [{}, {}]}");
        setSchemas(validator, schemaA, schemaB);

        // The method has no return value; we just ensure it completes successfully.
        validator.preloadJsonSchema();
    }
}

package com.networknt.schema;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link AnyOfValidator}.
 *
 * The tests create real {@link JsonSchema} and {@link ValidationContext}
 * objects (no mocking) and exercise the public API of {@code AnyOfValidator}
 * as well as its protected helper methods via reflection.
 */
public class AnyOfValidatorTest {

    private ObjectMapper mapper;
    private ValidationContext validationContext;
    private ExecutionContext executionContext;
    private JsonNodePath rootPath;
    private SchemaLocation schemaLocation;

    @Before
    public void setUp() {
        mapper = new ObjectMapper();
        // Build a ValidationContext using the default V7 meta‑schema.
        // The concrete library provides these static helpers.
        JsonMetaSchema metaSchema = JsonMetaSchema.getV7();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        SchemaValidatorsConfig config = new SchemaValidatorsConfig(); // defaults (annotation collection disabled)
        validationContext = new ValidationContext(metaSchema, factory, config);
        executionContext = new ExecutionContext(); // default ExecutionConfig (annotation collection disabled, failFast false)
        rootPath = new JsonNodePath(JsonNodePath.PathType.ROOT);
        schemaLocation = SchemaLocation.of("http://example.com/schema");
    }

    /**
     * Helper to create an {@link AnyOfValidator} from a JSON array describing
     * the subschemas.
     */
    private AnyOfValidator createValidator(ArrayNode anyOfArray) {
        return new AnyOfValidator(schemaLocation,
                                   rootPath,
                                   anyOfArray,
                                   null,                 // no parent schema needed for most tests
                                   validationContext);
    }

    @Test
    public void testValidateWhenNodeMatchesFirstSubSchema() {
        // anyOf: [ { "type":"string" }, { "type":"integer" } ]
        ArrayNode anyOf = mapper.createArrayNode();
        anyOf.add(mapper.createObjectNode().put("type", "string"));
        anyOf.add(mapper.createObjectNode().put("type", "integer"));

        AnyOfValidator validator = createValidator(anyOf);

        JsonNode node = new TextNode("hello");
        Set<ValidationMessage> messages = validator.validate(executionContext, node, node, rootPath);
        assertTrue("Expected no validation errors when node matches first subschema", messages.isEmpty());
    }

    @Test
    public void testValidateWhenNodeMatchesSecondSubSchema() {
        // anyOf: [ { "type":"string" }, { "type":"integer" } ]
        ArrayNode anyOf = mapper.createArrayNode();
        anyOf.add(mapper.createObjectNode().put("type", "string"));
        anyOf.add(mapper.createObjectNode().put("type", "integer"));

        AnyOfValidator validator = createValidator(anyOf);

        JsonNode node = new IntNode(42);
        Set<ValidationMessage> messages = validator.validate(executionContext, node, node, rootPath);
        assertTrue("Expected no validation errors when node matches second subschema", messages.isEmpty());
    }

    @Test
    public void testValidateWhenNodeMatchesNoSubSchema() {
        // anyOf: [ { "type":"string" }, { "type":"integer" } ]
        ArrayNode anyOf = mapper.createArrayNode();
        anyOf.add(mapper.createObjectNode().put("type", "string"));
        anyOf.add(mapper.createObjectNode().put("type", "integer"));

        AnyOfValidator validator = createValidator(anyOf);

        JsonNode node = BooleanNode.TRUE; // does not satisfy either subschema
        Set<ValidationMessage> messages = validator.validate(executionContext, node, node, rootPath);
        assertFalse("Expected validation errors when node matches no subschema", messages.isEmpty());

        // The error set should contain at least two messages (one from each subschema)
        // because the validator aggregates all errors when none of the subschemas succeed.
        assertTrue("Expected at least two error messages", messages.size() >= 2);
    }

    @Test
    public void testWalkWhenShouldValidateIsFalseReturnsEmptySet() {
        // anyOf: [ { "type":"string" } ]
        ArrayNode anyOf = mapper.createArrayNode();
        anyOf.add(mapper.createObjectNode().put("type", "string"));

        AnyOfValidator validator = createValidator(anyOf);

        JsonNode node = new TextNode("any");
        Set<ValidationMessage> messages = validator.walk(executionContext, node, node, rootPath, false);
        assertTrue("walk(..., false) must return an empty set", messages.isEmpty());
    }

    @Test
    public void testWalkWhenShouldValidateIsTrueDelegatesToValidate() {
        // anyOf: [ { "type":"integer" } ]
        ArrayNode anyOf = mapper.createArrayNode();
        anyOf.add(mapper.createObjectNode().put("type", "integer"));

        AnyOfValidator validator = createValidator(anyOf);

        JsonNode node = new IntNode(7);
        Set<ValidationMessage> messages = validator.walk(executionContext, node, node, rootPath, true);
        assertTrue("walk(..., true) should behave like validate and return empty set for a valid node", messages.isEmpty());
    }

    @Test
    public void testCanShortCircuitReturnsTrueWhenNoUnevaluatedValidatorsPresent() throws Exception {
        // Build a parent schema that has only a simple "type" validator (no unevaluated* validators)
        ObjectNode parentSchemaNode = mapper.createObjectNode().put("type", "object");
        JsonSchema parentSchema = validationContext.newSchema(schemaLocation, rootPath, parentSchemaNode, null);

        // anyOf subschemas (irrelevant for this test)
        ArrayNode anyOf = mapper.createArrayNode();
        anyOf.add(mapper.createObjectNode().put("type", "string"));

        AnyOfValidator validator = new AnyOfValidator(schemaLocation, rootPath, anyOf, parentSchema, validationContext);

        // Invoke the protected canShortCircuit() method via reflection
        Method method = AnyOfValidator.class.getDeclaredMethod("canShortCircuit");
        method.setAccessible(true);
        Boolean result = (Boolean) method.invoke(validator);
        assertTrue("canShortCircuit() should be true when parent schema lacks unevaluated* validators", result);
    }

    @Test
    public void testCanShortCircuitReturnsFalseWhenUnevaluatedPropertiesValidatorPresent() throws Exception {
        // Build a parent schema that contains an "unevaluatedProperties" keyword.
        // The library will create a validator with that keyword.
        ObjectNode parentSchemaNode = mapper.createObjectNode()
                .put("type", "object")
                .set("unevaluatedProperties", mapper.createObjectNode()); // empty schema for the keyword

        JsonSchema parentSchema = validationContext.newSchema(schemaLocation, rootPath, parentSchemaNode, null);

        // anyOf subschemas (irrelevant for this test)
        ArrayNode anyOf = mapper.createArrayNode();
        anyOf.add(mapper.createObjectNode().put("type", "string"));

        AnyOfValidator validator = new AnyOfValidator(schemaLocation, rootPath, anyOf, parentSchema, validationContext);

        // Invoke the protected canShortCircuit() method via reflection
        Method method = AnyOfValidator.class.getDeclaredMethod("canShortCircuit");
        method.setAccessible(true);
        Boolean result = (Boolean) method.invoke(validator);
        assertFalse("canShortCircuit() must be false when parent schema contains unevaluatedProperties validator", result);
    }

    @Test
    public void testCanShortCircuitWithExecutionContextRespectsAnnotationCollectionFlag() throws Exception {
        // anyOf with a single subschema (type string)
        ArrayNode anyOf = mapper.createArrayNode();
        anyOf.add(mapper.createObjectNode().put("type", "string"));

        AnyOfValidator validator = createValidator(anyOf);

        // ExecutionContext with annotation collection enabled (should prevent short‑circuit)
        ExecutionConfig configWithAnnotations = new ExecutionConfig();
        configWithAnnotations.setAnnotationCollectionEnabled(true);
        ExecutionContext ctx = new ExecutionContext(configWithAnnotations);

        // Invoke protected canShortCircuit(ExecutionContext) via reflection
        Method method = AnyOfValidator.class.getDeclaredMethod("canShortCircuit", ExecutionContext.class);
        method.setAccessible(true);
        Boolean result = (Boolean) method.invoke(validator, ctx);
        assertFalse("canShortCircuit(ExecutionContext) must be false when annotation collection is enabled", result);
    }

    @Test
    public void testValidateRespectsFailFastWhenShortCircuitIsApplicable() {
        // anyOf: [ { "type":"string" }, { "type":"integer" } ]
        ArrayNode anyOf = mapper.createArrayNode();
        anyOf.add(mapper.createObjectNode().put("type", "string"));
        anyOf.add(mapper.createObjectNode().put("type", "integer"));

        AnyOfValidator validator = createValidator(anyOf);

        // Enable fail‑fast on the ExecutionContext
        executionContext.setFailFast(true);

        // Node matches the first subschema (string). Validation should stop after the first match
        // and return an empty set (no errors from the second subschema are examined).
        JsonNode node = new TextNode("fast");
        Set<ValidationMessage> messages = validator.validate(executionContext, node, node, rootPath);
        assertTrue("With failFast true and a matching subschema, validation must succeed immediately", messages.isEmpty());
    }
}

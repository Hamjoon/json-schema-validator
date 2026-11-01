###Test START##
```java
package com.networknt.schema;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link JsonSchema}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>basic construction and identification of schema resources</li>
 *   <li>handling of {@code $recursiveAnchor}</li>
 *   <li>resolution of {@code $ref} via {@link #getRefSchemaNode(String)}</li>
 *   <li>sub‑schema extraction via {@link #getSubSchema(JsonNodePath)}</li>
 *   <li>validator pre‑loading with {@link #initializeValidators()}</li>
 *   <li>creation of an {@link ExecutionContext} and propagation of configuration</li>
 *   <li>the various {@code validate(...)} overloads (string, JsonNode, with/without customisers)</li>
 *   <li>error handling for missing references</li>
 * </ul>
 *
 * <p>All objects are created using the public API – no mocking or subclassing of
 * library classes is performed.
 */
public class JsonSchemaTest {

    private JsonSchemaFactory factory;
    private SchemaValidatorsConfig defaultConfig;

    @Before
    public void setUp() {
        // Use the 2019‑09 meta‑schema (the most recent stable version) for the tests.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        defaultConfig = new SchemaValidatorsConfig();
    }

    /**
     * Helper that parses a JSON string into a {@link JsonNode}.
     */
    private JsonNode node(String json) {
        try {
            return factory.getObjectMapper().readTree(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Simple schema without an explicit $id – the document itself should be
     * recognised as a schema‑resource root.
     */
    @Test
    public void testSchemaResourceRootAndId() {
        JsonNode schemaNode = node("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}");
        JsonSchema schema = factory.getSchema(schemaNode);

        // The schema has no $id, therefore {@code getId()} should be the absolute IRI of the location.
        assertNotNull("Schema ID should be derived from the location", schema.getId());

        // Because there is no $id and no parent, the schema itself is the resource root.
        assertTrue("Schema without $id must be a resource root", schema.isSchemaResourceRoot());

        // The lexical root (the nearest ancestor with an $id) is the schema itself.
        assertSame("Lexical root should be the schema itself", schema, schema.findLexicalRoot());
    }

    /**
     * Verify that a schema containing {@code $recursiveAnchor:true} reports the
     * correct flag.
     */
    @Test
    public void testRecursiveAnchorFlag() {
        JsonNode schemaNode = node("{\"$recursiveAnchor\":true}");
        JsonSchema schema = factory.getSchema(schemaNode);
        assertTrue("Recursive anchor flag must be true", schema.isRecursiveAnchor());
    }

    /**
     * Test {@link JsonSchema#getRefSchemaNode(String)} for a normal JSON‑Pointer
     * reference. The method must resolve the pointer, decode any %-encoded parts,
     * and return the targeted node.
     */
    @Test
    public void testGetRefSchemaNode_JsonPointer() {
        String json = "{"
                + "\"$id\":\"http://example.com/root#\","
                + "\"definitions\":{"
                + "  \"inner\":{"
                + "    \"type\":\"string\""
                + "  }"
                + "},"
                + "\"properties\":{"
                + "  \"refProp\":{\"$ref\":\"#\\/definitions\\/inner\"}"
                + "}"
                + "}";
        JsonNode schemaNode = node(json);
        JsonSchema schema = factory.getSchema(schemaNode);

        // Resolve the reference used inside the schema.
        JsonNode resolved = schema.getRefSchemaNode("#/definitions/inner");
        assertNotNull("Resolved node must not be null", resolved);
        assertEquals("type", resolved.get("type").asText());

        // The same reference prefixed with the schema's $id must also work.
        JsonNode resolvedWithId = schema.getRefSchemaNode("http://example.com/root#/definitions/inner");
        assertEquals(resolved, resolvedWithId);
    }

    /**
     * Ensure that a malformed reference (missing node) yields {@code null}
     * rather than throwing.
     */
    @Test
    public void testGetRefSchemaNode_MissingReferenceReturnsNull() {
        JsonNode schemaNode = node("{\"properties\":{\"foo\":{\"type\":\"string\"}}}");
        JsonSchema schema = factory.getSchema(schemaNode);
        JsonNode missing = schema.getRefSchemaNode("#/nonexistent");
        assertNull("Missing reference should return null", missing);
    }

    /**
     * Test {@link JsonSchema#getSubSchema(JsonNodePath)} using a JSON‑Pointer
     * fragment that points to a property definition.
     */
    @Test
    public void testGetSubSchema_JsonPointer() {
        JsonNode schemaNode = node("{"
                + "\"type\":\"object\","
                + "\"properties\":{"
                + "  \"age\":{"
                + "    \"type\":\"integer\","
                + "    \"minimum\":0"
                + "  }"
                + "}"
                + "}");
        JsonSchema schema = factory.getSchema(schemaNode);

        // Build a JsonNodePath representing "/properties/age"
        JsonNodePath fragment = new JsonNodePath(PathType.JSON_POINTER)
                .append("properties")
                .append("age");

        JsonSchema sub = schema.getSubSchema(fragment);
        assertNotNull("Sub‑schema for /properties/age must exist", sub);
        assertEquals("integer", sub.getSchemaNode().get("type").asText());
        assertEquals(0, sub.getSchemaNode().get("minimum").asInt());
    }

    /**
     * Verify that {@link JsonSchema#initializeValidators()} pre‑loads all
     * validators without throwing, even when the schema contains a {@code $ref}
     * to another subschema.
     */
    @Test
    public void testInitializeValidatorsPreloads() {
        // Root schema with a $ref to an internal definition.
        String json = "{"
                + "\"$id\":\"http://example.com/main#\","
                + "\"definitions\":{"
                + "  \"inner\":{"
                + "    \"type\":\"string\""
                + "  }"
                + "},"
                + "\"allOf\":[{\"$ref\":\"#\\/definitions\\/inner\"}]"
                + "}";
        JsonSchema schema = factory.getSchema(node(json));

        // No exception should be thrown – the method returns void.
        schema.initializeValidators();

        // After pre‑loading, a validation of a correct instance must succeed.
        Set<ValidationMessage> errors = schema.validate(node("{\"any\":\"value\"}"));
        assertTrue("Valid instance must produce no errors after pre‑load", errors.isEmpty());
    }

    /**
     * Validate that {@link JsonSchema#createExecutionContext()} copies the
     * locale from the {@link SchemaValidatorsConfig}.
     */
    @Test
    public void testCreateExecutionContextCopiesConfig() {
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        cfg.setLocale(Locale.GERMAN);
        JsonSchemaFactory customFactory = JsonSchemaFactory.builder(factory)
                .setConfig(cfg)
                .build();

        JsonSchema schema = customFactory.getSchema(node("{\"type\":\"number\"}"));
        ExecutionContext ctx = schema.createExecutionContext();

        assertEquals("ExecutionContext must inherit locale from config",
                Locale.GERMAN,
                ctx.getExecutionConfig().getLocale());
    }

    /**
     * Test the {@code validate(String, InputFormat)} overload with a valid JSON
     * string and with an invalid one. The method should return an empty set for
     * valid data and a non‑empty set for invalid data.
     */
    @Test
    public void testValidateStringInput() {
        JsonSchema schema = factory.getSchema(node("{\"type\":\"integer\"}"));

        // Valid integer
        Set<ValidationMessage> ok = schema.validate("42", InputFormat.JSON);
        assertTrue("Valid integer string must produce no errors", ok.isEmpty());

        // Invalid string (not an integer)
        Set<ValidationMessage> notOk = schema.validate("\"not-an-int\"", InputFormat.JSON);
        assertFalse("Invalid integer string must produce errors", notOk.isEmpty());
    }

    /**
     * Validate the {@code validate(JsonNode, OutputFormat<T>)} overload using a
     * simple {@link OutputFormat} that returns the number of validation messages.
     */
    @Test
    public void testValidateWithCustomOutputFormat() {
        JsonSchema schema = factory.getSchema(node("{\"type\":\"string\",\"minLength\":5}"));
        JsonNode instance = node("\"abc\""); // too short

        OutputFormat<Integer> countFormat = new OutputFormat<Integer>() {
            @Override
            public void customize(ExecutionContext executionContext, ValidationContext validationContext) {
                // no customisation needed
            }

            @Override
            public Integer format(JsonSchema schema, Set<ValidationMessage> messages,
                                 ExecutionContext executionContext, ValidationContext validationContext) {
                return messages.size();
            }
        };

        Integer errorCount = schema.validate(instance, countFormat);
        assertEquals("Exactly one validation error expected (minLength)", Integer.valueOf(1), errorCount);
    }

    /**
     * Test the {@code walk(...)} family of methods. The walk should return a
     * {@link ValidationResult} that contains the same validation messages as a
     * direct {@code validate(...)} call.
     */
    @Test
    public void testWalkProducesSameResultAsValidate() {
        JsonSchema schema = factory.getSchema(node("{\"type\":\"boolean\"}"));
        JsonNode badInstance = node("123"); // not a boolean

        ValidationResult walkResult = schema.walk(badInstance, false);
        Set<ValidationMessage> walkErrors = walkResult.getValidationMessages();

        Set<ValidationMessage> validateErrors = schema.validate(badInstance);
        assertEquals("Walk and validate must produce identical error sets",
                validateErrors, walkErrors);
    }

    /**
     * Ensure that calling {@link JsonSchema#getValidators()} lazily loads the
     * validators and that the resulting list is immutable.
     */
    @Test
    public void testGetValidatorsIsImmutable() {
        JsonSchema schema = factory.getSchema(node("{\"type\":\"null\"}"));
        // First call loads the validators.
        assertFalse("Validators list should initially be null", schema.getValidators().isEmpty());

        // The returned list must be unmodifiable.
        try {
            schema.getValidators().add(null);
            fail("Validators list must be immutable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    /**
     * Verify that {@link JsonSchema#hasTypeValidator()} correctly reports the
     * presence of a {@code type} keyword.
     */
    @Test
    public void testHasTypeValidator() {
        JsonSchema withType = factory.getSchema(node("{\"type\":\"array\"}"));
        JsonSchema withoutType = factory.getSchema(node("{\"minimum\":5}"));

        assertTrue("Schema with a type keyword must report true", withType.hasTypeValidator());
        assertFalse("Schema without a type keyword must report false", withoutType.hasTypeValidator());
    }

    /**
     * Test that an invalid {@code $recursiveAnchor} (non‑boolean) leads to an
     * {@link JsonSchemaException}.
     */
    @Test(expected = JsonSchemaException.class)
    public void testInvalidRecursiveAnchorThrows() {
        // The constructor parses the schema immediately, so the exception is thrown here.
        factory.getSchema(node("{\"$recursiveAnchor\":\"yes\"}"));
    }

    /**
     * Verify that {@link JsonSchema#toString()} produces a readable representation
     * containing the evaluation path and the raw schema node.
     */
    @Test
    public void testToStringRepresentation() {
        JsonSchema schema = factory.getSchema(node("{\"type\":\"number\"}"));
        String repr = schema.toString();
        assertTrue("String representation must contain the evaluation path", repr.contains("\"\""));
        assertTrue("String representation must contain the raw schema", repr.contains("\"type\":\"number\""));
    }
}
```
###Test END##
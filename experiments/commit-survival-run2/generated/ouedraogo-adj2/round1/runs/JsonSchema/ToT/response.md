###Test START##
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.ValidationContext.DiscriminatorContext;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.net.URI;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Set;

/**
 * JUnit‑4 test suite for {@link JsonSchema}.  The tests focus on the public
 * behaviour of the class while exercising the most important internal logic
 * (URI handling, $ref resolution, ancestor handling, discriminator support,
 * dynamic‑anchor handling and the basic validation flow).  No external
 * mocking frameworks are used – all objects are created through the constructors
 * and factory methods that are part of the supplied source code.
 */
public class JsonSchemaTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Minimal {@link ValidationContext} implementation that delegates to the
     * default {@link JsonMetaSchema#getV7()} for validator creation and resolves
     * the schema identifier using the meta‑schema’s id keyword.
     */
    private static class SimpleValidationContext extends ValidationContext {
        SimpleValidationContext(SchemaValidatorsConfig config) {
            super(null, null, JsonMetaSchema.getV7(), null, config);
        }

        @Override
        public String resolveSchemaId(JsonNode schemaNode) {
            JsonNode idNode = schemaNode.get(JsonMetaSchema.getV7().getIdKeyword());
            return idNode != null ? idNode.asText() : null;
        }

        @Override
        public JsonValidator newValidator(String schemaPath, String keyword, JsonNode schemaNode,
                                          JsonSchema parentSchema, String customMessage) {
            // Delegate to the meta‑schema implementation – this yields the real
            // validator implementations (e.g. TypeValidator, PropertiesValidator, …)
            return JsonMetaSchema.getV7().newValidator(this, schemaPath, keyword, schemaNode,
                    parentSchema, customMessage);
        }
    }

    /**
     * Helper to build a {@link JsonSchema} instance using the static factory
     * method.  The method hides the many constructor arguments and keeps the
     * test code readable.
     */
    private JsonSchema buildSchema(JsonNode schemaNode, URI baseUri) {
        ValidationContext ctx = new SimpleValidationContext(new SchemaValidatorsConfig());
        return JsonSchema.from(ctx, "#", baseUri, schemaNode, null, false);
    }

    /** --------------------------------------------------------------------- *
     *  1.  URI handling – combineCurrentUriWithIds returns the original URI
     *      when the schema does not contain an identifier.
     *  --------------------------------------------------------------------- */
    @Test
    public void combineCurrentUriWithIds_NoId_ReturnsOriginalUri() throws Exception {
        JsonNode schema = MAPPER.readTree("{\"type\":\"object\"}");
        URI base = new URI("http://example.com/schema.json");
        JsonSchema js = buildSchema(schema, base);

        // The currentUri field is private; expose it via the public getter.
        Assert.assertEquals("When no $id is present the current URI must be unchanged",
                base, js.getCurrentUri());
    }

    /** --------------------------------------------------------------------- *
     *  2.  URI handling – a fragment identifier without a base URI results in a
     *      null current URI (the private helper {@code isUriFragmentWithNoContext}
     *      is exercised indirectly via the constructor).
     *  --------------------------------------------------------------------- */
    @Test
    public void combineCurrentUriWithIds_FragmentWithoutBase_ResultsInNullUri() throws Exception {
        // Schema contains an $id that is a fragment only.
        JsonNode schema = MAPPER.readTree("{\"$id\":\"#fragment\"}");
        JsonSchema js = buildSchema(schema, null);

        Assert.assertNull("A fragment‑only $id with no base URI must produce a null currentUri",
                js.getCurrentUri());
    }

    /** --------------------------------------------------------------------- *
     *  3.  Static helper {@code uriRefersToSubschema} – true when the original
     *      URI has a fragment and the supplied schemaPath is empty or "#".
     *  --------------------------------------------------------------------- */
    @Test
    public void uriRefersToSubschema_DetectsFragmentReference() throws Exception {
        Method m = JsonSchema.class.getDeclaredMethod("uriRefersToSubschema", URI.class, String.class);
        m.setAccessible(true);

        URI uriWithFragment = new URI("http://example.com/schema.json#defs");
        boolean result = (boolean) m.invoke(null, uriWithFragment, "#");
        Assert.assertTrue("uriRefersToSubschema should be true for a URI with fragment and schemaPath '#'", result);
    }

    /** --------------------------------------------------------------------- *
     *  4.  $ref resolution – local fragment reference is resolved correctly.
     *  --------------------------------------------------------------------- */
    @Test
    public void getRefSchemaNode_ResolvesLocalDefinition() throws Exception {
        String schemaJson = "{"
                + "\"definitions\":{"
                + "   \"foo\":{ \"type\":\"string\" }"
                + "},"
                + "\"properties\":{"
                + "   \"bar\":{ \"$ref\":\"#/definitions/foo\" }"
                + "}"
                + "}";
        JsonNode schema = MAPPER.readTree(schemaJson);
        JsonSchema js = buildSchema(schema, new URI("http://example.com/root.json"));

        JsonNode refNode = js.getRefSchemaNode("#/definitions/foo");
        Assert.assertNotNull("The $ref target node must not be null", refNode);
        Assert.assertEquals("The resolved node must contain the expected type",
                "string", refNode.get("type").asText());
    }

    /** --------------------------------------------------------------------- *
     *  5.  getRefSchemaNode – non‑existent fragment returns {@code null}.
     *  --------------------------------------------------------------------- */
    @Test
    public void getRefSchemaNode_NonExistingFragmentReturnsNull() throws Exception {
        JsonNode schema = MAPPER.readTree("{\"type\":\"object\"}");
        JsonSchema js = buildSchema(schema, new URI("http://example.com/root.json"));

        JsonNode node = js.getRefSchemaNode("#/missing");
        Assert.assertNull("A missing fragment reference must result in null", node);
    }

    /** --------------------------------------------------------------------- *
     *  6.  Ancestor handling – a child schema created via {@code createChildSchema}
     *      reports the root schema as its ancestor.
     *  --------------------------------------------------------------------- */
    @Test
    public void findAncestor_ReturnsRootSchema() throws Exception {
        JsonNode rootSchema = MAPPER.readTree("{\"type\":\"object\"}");
        JsonSchema root = buildSchema(rootSchema, new URI("http://example.com/root.json"));
        JsonSchema child = root.createChildSchema("#/properties/child", MAPPER.readTree("{\"type\":\"string\"}"));

        JsonSchema ancestor = child.findAncestor();
        Assert.assertSame("The ancestor of a direct child must be the root schema", root, ancestor);
    }

    /** --------------------------------------------------------------------- *
     *  7.  Dynamic anchor detection – the presence of a boolean "$recursiveAnchor"
     *      true flag must set {@code dynamicAnchor} to true.
     *  --------------------------------------------------------------------- */
    @Test
    public void isDynamicAnchor_TrueWhenRecursiveAnchorIsTrue() throws Exception {
        JsonNode schema = MAPPER.readTree("{\"$recursiveAnchor\":true}");
        JsonSchema js = buildSchema(schema, new URI("http://example.com/schema.json"));
        Assert.assertTrue("Schema with $recursiveAnchor:true must be marked as dynamic", js.isDynamicAnchor());
    }

    /** --------------------------------------------------------------------- *
     *  8.  Execution context creation – the returned {@link ExecutionContext}
     *      must contain a non‑null {@link CollectorContext}.
     *  --------------------------------------------------------------------- */
    @Test
    public void createExecutionContext_ReturnsContextWithCollector() throws Exception {
        JsonNode schema = MAPPER.readTree("{\"type\":\"object\"}");
        JsonSchema js = buildSchema(schema, new URI("http://example.com/schema.json"));
        ExecutionContext ctx = js.createExecutionContext();

        Assert.assertNotNull("ExecutionContext must contain a CollectorContext",
                ctx.getCollectorContext());
    }

    /** --------------------------------------------------------------------- *
     *  9.  Validation – an empty schema produces no validation errors irrespective
     *      of the input data.
     *  --------------------------------------------------------------------- */
    @Test
    public void validate_EmptySchemaProducesNoErrors() throws Exception {
        JsonNode emptySchema = MAPPER.readTree("{}");
        JsonSchema js = buildSchema(emptySchema, new URI("http://example.com/empty.json"));
        JsonNode data = MAPPER.readTree("{\"any\":\"thing\"}");

        Set<ValidationMessage> errors = js.validate(data);
        Assert.assertTrue("Validation of an empty schema must yield an empty error set", errors.isEmpty());
    }

    /** --------------------------------------------------------------------- *
     * 10.  Validation & collection – a schema that requires a string type must
     *      generate a validation message when the instance is a number.
     *  --------------------------------------------------------------------- */
    @Test
    public void validateAndCollect_TypeMismatchProducesError() throws Exception {
        JsonNode schema = MAPPER.readTree("{\"type\":\"string\"}");
        JsonSchema js = buildSchema(schema, new URI("http://example.com/type.json"));
        JsonNode data = MAPPER.readTree("123"); // a JSON number

        ValidationResult result = js.validateAndCollect(data);
        Set<ValidationMessage> msgs = result.getValidationMessages();

        Assert.assertFalse("A type mismatch must generate at least one ValidationMessage", msgs.isEmpty());

        // The message should be associated with the root path ("")
        ValidationMessage first = msgs.iterator().next();
        Assert.assertEquals("The error path for a root‑level type mismatch must be empty",
                "", first.getPath());
    }

    /** --------------------------------------------------------------------- *
     * 11.  Discriminator handling – when the configuration enables OpenAPI‑3 style
     *      discriminators, the validator must invoke the static helper
     *      {@code checkDiscriminatorMatch}.  The test verifies that the helper
     *      is reachable (no exception) and that the discriminator is registered.
     *  --------------------------------------------------------------------- */
    @Test
    public void validate_WithOpenAPIDiscriminator_RegistersAndChecks() throws Exception {
        // Build a schema that contains a discriminator definition.
        String schemaJson = "{"
                + "\"discriminator\":{"
                + "   \"propertyName\":\"type\","
                + "   \"mapping\":{"
                + "       \"A\":\"#/definitions/A\""
                + "   }"
                + "},"
                + "\"definitions\":{"
                + "   \"A\":{ \"type\":\"object\", \"properties\":{ \"type\":{\"const\":\"A\"}} }"
                + "}"
                + "}";
        JsonNode schema = MAPPER.readTree(schemaJson);

        // Enable the OpenAPI discriminator flag.
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        cfg.setOpenAPI3StyleDiscriminators(true);

        ValidationContext ctx = new SimpleValidationContext(cfg);
        JsonSchema js = JsonSchema.from(ctx, "#", new URI("http://example.com/disc.json"),
                schema, null, false);

        // Instance that matches the discriminator mapping.
        JsonNode instance = MAPPER.readTree("{\"type\":\"A\"}");

        Set<ValidationMessage> errors = js.validate(instance);
        // No validation errors expected; the test mainly ensures that no
        // NullPointerException is thrown during discriminator processing.
        Assert.assertTrue("Discriminator processing should not produce errors for a matching instance",
                errors.isEmpty());

        // Verify that the discriminator was registered in the context.
        DiscriminatorContext dctx = ctx.getCurrentDiscriminatorContext();
        Assert.assertNotNull("DiscriminatorContext must be present when the flag is enabled", dctx);
        Assert.assertNotNull("The discriminator must be registered for the schema path",
                dctx.getDiscriminatorForPath(js.getSchemaPath()));
    }

    /** --------------------------------------------------------------------- *
     * 12.  toString – the overridden method must contain the schema path and the
     *      JSON representation of the schema node.
     *  --------------------------------------------------------------------- */
    @Test
    public void toString_IncludesPathAndNode() throws Exception {
        JsonNode schema = MAPPER.readTree("{\"type\":\"boolean\"}");
        JsonSchema js = buildSchema(schema, new URI("urn:example:test"));
        String str = js.toString();

        Assert.assertTrue("toString must contain the schema path", str.contains(js.getSchemaPath()));
        Assert.assertTrue("toString must contain the JSON representation of the schema node",
                str.contains("\"type\":\"boolean\""));
    }

}
```
###Test END##
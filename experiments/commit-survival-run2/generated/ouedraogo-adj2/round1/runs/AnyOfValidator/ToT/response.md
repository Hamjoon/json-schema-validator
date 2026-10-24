**AnyOfValidatorTest.java**

```java
/**
 * Comprehensive JUnit‑4 test suite for {@link com.networknt.schema.AnyOfValidator}.
 *
 * The tests cover:
 *   • construction of the validator,
 *   • successful validation when one of the subschemas matches,
 *   • failure when none match,
 *   • behaviour of {@code walk()} with {@code shouldValidateSchema} true / false,
 *   • that {@code preloadJsonSchema()} does not throw.
 *
 * The test suite builds real schema objects using the public API only – no mocking
 * or subclassing of library classes is performed.
 */

###Test START##
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.networknt.schema.CollectorContext.Scope;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.*;

public class AnyOfValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Helper to create a {@link ValidationContext} with a default configuration. */
    private ValidationContext createValidationContext() {
        // All factories / meta‑schema have public no‑arg constructors in the library.
        URIFactory uriFactory = new URIFactory();
        URNFactory urnFactory = new URNFactory();
        JsonMetaSchema metaSchema = JsonMetaSchema.getV7();                 // any supported version
        JsonSchemaFactory schemaFactory = new JsonSchemaFactory();          // default factory
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();      // default (no discriminator)

        return new ValidationContext(uriFactory, urnFactory, metaSchema, schemaFactory, config);
    }

    /** Builds an {@code anyOf} validator that contains two simple object subschemas:
     *   1) requires a string property {@code a}
     *   2) requires an integer property {@code b}
     */
    private AnyOfValidator buildAnyOfValidator(ValidationContext ctx) throws Exception {
        // ----- subschema 1 : {"type":"object","required":["a"],"properties":{"a":{"type":"string"}}}
        JsonNode subSchema1 = MAPPER.readTree(
                "{ \"type\":\"object\", " +
                "  \"required\":[\"a\"], " +
                "  \"properties\":{ \"a\":{ \"type\":\"string\" } } }");

        // ----- subschema 2 : {"type":"object","required":["b"],"properties":{"b":{"type\":\"integer"}}}
        JsonNode subSchema2 = MAPPER.readTree(
                "{ \"type\":\"object\", " +
                "  \"required\":[\"b\"], " +
                "  \"properties\":{ \"b\":{ \"type\":\"integer\" } } }");

        // ----- anyOf array node
        ArrayNode anyOfArray = MAPPER.createArrayNode();
        anyOfArray.add(subSchema1);
        anyOfArray.add(subSchema2);

        // parent schema – a minimal dummy schema (no keywords, just a placeholder)
        JsonSchema parent = new JsonSchema(ctx, "parent", URI.create("urn:parent"), MAPPER.createObjectNode(), null);

        // construct the validator directly (the library creates it via ValidationContext.newValidator)
        return new AnyOfValidator("test/anyOf", anyOfArray, parent, ctx);
    }

    private ExecutionContext execContext;

    @Before
    public void setUp() {
        execContext = new ExecutionContext(); // uses a fresh CollectorContext internally
    }

    /** -----------------------------------------------------------------------
     *  1️⃣  Validation succeeds when the instance matches the **first** subschema.
     * ----------------------------------------------------------------------- */
    @Test
    public void testValidateMatchesFirstSubschema() throws Exception {
        ValidationContext ctx = createValidationContext();
        AnyOfValidator validator = buildAnyOfValidator(ctx);

        // JSON instance: {"a":"hello"}
        JsonNode instance = MAPPER.readTree("{\"a\":\"hello\"}");

        Set<ValidationMessage> result = validator.validate(execContext, instance, instance, "$");
        assertTrue("Expected no validation errors when first subschema matches", result.isEmpty());
    }

    /** -----------------------------------------------------------------------
     *  2️⃣  Validation succeeds when the instance matches the **second** subschema.
     * ----------------------------------------------------------------------- */
    @Test
    public void testValidateMatchesSecondSubschema() throws Exception {
        ValidationContext ctx = createValidationContext();
        AnyOfValidator validator = buildAnyOfValidator(ctx);

        // JSON instance: {"b":5}
        JsonNode instance = MAPPER.readTree("{\"b\":5}");

        Set<ValidationMessage> result = validator.validate(execContext, instance, instance, "$");
        assertTrue("Expected no validation errors when second subschema matches", result.isEmpty());
    }

    /** -----------------------------------------------------------------------
     *  3️⃣  Validation fails when **none** of the subschemas match.
     *      The error set must contain the required‑property messages from both
     *      subschemas (the type of the errors is “required”).
     * ----------------------------------------------------------------------- */
    @Test
    public void testValidateNoMatchingSubschema() throws Exception {
        ValidationContext ctx = createValidationContext();
        AnyOfValidator validator = buildAnyOfValidator(ctx);

        // JSON instance: {"c":"no‑match"}
        JsonNode instance = MAPPER.readTree("{\"c\":\"no‑match\"}");

        Set<ValidationMessage> result = validator.validate(execContext, instance, instance, "$");
        assertFalse("Expected validation errors when no subschema matches", result.isEmpty());

        // Both subschemas have a REQUIRED error – we should see two of them.
        long requiredCount = result.stream()
                .filter(v -> ValidatorTypeCode.REQUIRED.getValue().equals(v.getType()))
                .count();
        assertEquals("There should be two REQUIRED errors (one per subschema)", 2, requiredCount);
    }

    /** -----------------------------------------------------------------------
     *  4️⃣  {@code walk()} with {@code shouldValidateSchema = true}
     *      must delegate to {@code validate()} and therefore return the same
     *      result as a direct call to {@code validate()}.
     * ----------------------------------------------------------------------- */
    @Test
    public void testWalkValidateTrueDelegatesToValidate() throws Exception {
        ValidationContext ctx = createValidationContext();
        AnyOfValidator validator = buildAnyOfValidator(ctx);

        JsonNode instance = MAPPER.readTree("{\"a\":\"x\"}");

        Set<ValidationMessage> viaValidate = validator.validate(execContext, instance, instance, "$");
        Set<ValidationMessage> viaWalk = validator.walk(execContext, instance, instance, "$", true);

        assertEquals("walk(...,true) must produce the same result as validate()", viaValidate, viaWalk);
    }

    /** -----------------------------------------------------------------------
     *  5️⃣  {@code walk()} with {@code shouldValidateSchema = false}
     *      must **not** perform validation – it returns an empty set.
     * ----------------------------------------------------------------------- */
    @Test
    public void testWalkValidateFalseDoesNotValidate() throws Exception {
        ValidationContext ctx = createValidationContext();
        AnyOfValidator validator = buildAnyOfValidator(ctx);

        JsonNode instance = MAPPER.readTree("{\"a\":\"x\"}");

        Set<ValidationMessage> result = validator.walk(execContext, instance, instance, "$", false);
        assertTrue("walk(...,false) must return an empty set", result.isEmpty());
    }

    /** -----------------------------------------------------------------------
     *  6️⃣  {@code preloadJsonSchema()} must load all child schemas without
     *      throwing any exception.  The method does not return a value, so the
     *      test simply ensures that execution completes.
     * ----------------------------------------------------------------------- */
    @Test
    public void testPreloadJsonSchemaCompletes() throws Exception {
        ValidationContext ctx = createValidationContext();
        AnyOfValidator validator = buildAnyOfValidator(ctx);

        // Should complete without throwing
        validator.preloadJsonSchema();
    }

    /** -----------------------------------------------------------------------
     *  7️⃣  Edge case – the node type does **not** match a subschema’s declared
     *      type (e.g. subschema expects an object but the instance is a string).
     *      The validator must ignore that subschema and continue checking the
     *      remaining alternatives.
     * ----------------------------------------------------------------------- */
    @Test
    public void testValidateSkipsMismatchedTypeSubschema() throws Exception {
        ValidationContext ctx = createValidationContext();
        AnyOfValidator validator = buildAnyOfValidator(ctx);

        // Instance is a plain string – does not satisfy the object type of either subschema.
        JsonNode instance = MAPPER.readTree("\"plain‑string\"");

        Set<ValidationMessage> result = validator.validate(execContext, instance, instance, "$");
        // Both subschemas will be skipped → we still get REQUIRED errors from each.
        assertFalse("Expected errors because no subschema matches the string instance", result.isEmpty());
        assertEquals("Two REQUIRED errors expected (one per subschema)", 2,
                result.stream().filter(v -> ValidatorTypeCode.REQUIRED.getValue().equals(v.getType())).count());
    }
}
###Test END##
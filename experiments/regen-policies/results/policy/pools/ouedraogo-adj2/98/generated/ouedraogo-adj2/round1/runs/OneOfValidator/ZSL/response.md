###Test START##
```java
package com.networknt.schema;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.TextNode;

/**
 * JUnit4 tests for {@link OneOfValidator}.
 *
 * The tests cover:
 * <ul>
 *   <li>Constructor validation (non‑array schema node)</li>
 *   <li>Successful validation when exactly one subschema matches</li>
 *   <li>Failure when none of the subschemas match</li>
 *   <li>Failure when more than one subschema matches (indexes handling)</li>
 *   <li>Behaviour of the protected helper methods {@code reportChildErrors},
 *       {@code canShortCircuit} and {@code walk}</li>
 *   <li>Pre‑loading of schemas via {@code preloadJsonSchema}</li>
 * </ul>
 *
 * All objects are created using only the public constructors and methods
 * listed in the supplied API signatures.
 */
public class OneOfValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ValidationContext validationContext;
    private ExecutionContext executionContext;
    private JsonNodePath rootPath;
    private SchemaLocation schemaLocation;

    @Before
    public void setUp() {
        // The concrete classes JsonMetaSchema, JsonSchemaFactory and SchemaValidatorsConfig
        // have public no‑arg constructors in the library, therefore they can be instantiated
        // directly for the purpose of the tests.
        JsonMetaSchema metaSchema = new JsonMetaSchema();          // dummy meta‑schema
        JsonSchemaFactory factory = new JsonSchemaFactory();      // dummy factory
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();

        validationContext = new ValidationContext(metaSchema, factory, config);
        executionContext = new ExecutionContext();                // default execution config (failFast = false)
        rootPath = new JsonNodePath(JsonNodePath.PathType.ROOT);
        schemaLocation = SchemaLocation.of("http://example.com/schema");
    }

    /**
     * Helper to create a OneOfValidator from a JSON schema string that
     * represents the "oneOf" array.
     */
    private OneOfValidator createValidator(String oneOfSchema) {
        JsonNode schemaNode = MAPPER.readTree(oneOfSchema);
        return new OneOfValidator(schemaLocation, rootPath, schemaNode, null, validationContext);
    }

    @Test(expected = JsonSchemaException.class)
    public void testConstructorRejectsNonArraySchemaNode() throws Exception {
        // The schema node is an object, not an array – the constructor must throw.
        String invalidSchema = "{ \"type\": \"string\" }";
        JsonNode schemaNode = MAPPER.readTree(invalidSchema);
        new OneOfValidator(schemaLocation, rootPath, schemaNode, null, validationContext);
    }

    @Test
    public void testValidateExactlyOneMatch() throws Exception {
        // oneOf: [ { "type":"string" }, { "type":"integer" } ]
        String oneOfSchema = "[ { \"type\":\"string\" }, { \"type\":\"integer\" } ]";
        OneOfValidator validator = createValidator(oneOfSchema);

        JsonNode instance = new TextNode("hello");
        Set<ValidationMessage> errors = validator.validate(executionContext, instance, instance, rootPath);
        assertTrue("Expected no validation errors when exactly one subschema matches", errors.isEmpty());
    }

    @Test
    public void testValidateNoMatchProducesOneOfError() throws Exception {
        String oneOfSchema = "[ { \"type\":\"string\" }, { \"type\":\"integer\" } ]";
        OneOfValidator validator = createValidator(oneOfSchema);

        JsonNode instance = BooleanNode.TRUE; // does not match string nor integer
        Set<ValidationMessage> errors = validator.validate(executionContext, instance, instance, rootPath);
        assertEquals(1, errors.size());

        ValidationMessage vm = errors.iterator().next();
        assertEquals("oneOf", vm.getMessageKey());
        // the first argument is the number of matching schemas (0)
        assertEquals("0", vm.getArguments()[0]);
    }

    @Test
    public void testValidateMultipleMatchesProducesIndexesError() throws Exception {
        // The first subschema accepts any string, the second accepts the enum ["hello","world"]
        String oneOfSchema = "[ { \"type\":\"string\" }, { \"enum\":[\"hello\",\"world\"] } ]";
        OneOfValidator validator = createValidator(oneOfSchema);

        JsonNode instance = new TextNode("hello"); // matches both subschemas
        Set<ValidationMessage> errors = validator.validate(executionContext, instance, instance, rootPath);
        assertEquals(1, errors.size());

        ValidationMessage vm = errors.iterator().next();
        // When more than one schema matches the message key is "oneOf.indexes"
        assertEquals("oneOf.indexes", vm.getMessageKey());
        // arguments: number of matches and a comma‑separated list of indexes
        assertEquals("2", vm.getArguments()[0]);               // two matches
        assertEquals("0, 1", vm.getArguments()[1]);           // indexes of matching schemas
    }

    @Test
    public void testReportChildErrorsRespectsFailFastFlag() throws Exception {
        String oneOfSchema = "[ { \"type\":\"string\" } ]";
        OneOfValidator validator = createValidator(oneOfSchema);

        // failFast = true -> should not report child errors
        executionContext.setFailFast(true);
        Method method = OneOfValidator.class.getDeclaredMethod("reportChildErrors", ExecutionContext.class);
        method.setAccessible(true);
        boolean result = (Boolean) method.invoke(validator, executionContext);
        assertFalse("reportChildErrors must return false when executionContext is failFast", result);

        // failFast = false -> should report child errors
        executionContext.setFailFast(false);
        result = (Boolean) method.invoke(validator, executionContext);
        assertTrue("reportChildErrors must return true when executionContext is not failFast", result);
    }

    @Test
    public void testCanShortCircuitDefaultBehaviour() throws Exception {
        // Parent schema without any "unevaluatedProperties" or "unevaluatedItems" validators
        String oneOfSchema = "[ { \"type\":\"string\" } ]";
        OneOfValidator validator = createValidator(oneOfSchema);

        Method method = OneOfValidator.class.getDeclaredMethod("canShortCircuit");
        method.setAccessible(true);
        Boolean result = (Boolean) method.invoke(validator);
        assertTrue("canShortCircuit should be true when no disallowed validators are present", result);

        // Second call must return the cached value (still true)
        Boolean second = (Boolean) method.invoke(validator);
        assertTrue("Cached canShortCircuit value should remain true", second);
    }

    @Test
    public void testWalkDelegatesToValidateWhenRequested() throws Exception {
        String oneOfSchema = "[ { \"type\":\"string\" }, { \"type\":\"integer\" } ]";
        OneOfValidator validator = createValidator(oneOfSchema);

        JsonNode instance = new TextNode("hello");
        Set<ValidationMessage> walkResult = validator.walk(executionContext, instance, instance, rootPath, true);
        // walk with shouldValidateSchema = true should produce the same result as validate()
        Set<ValidationMessage> validateResult = validator.validate(executionContext, instance, instance, rootPath);
        assertEquals(validateResult, walkResult);
    }

    @Test
    public void testWalkWithoutValidationReturnsEmptySet() throws Exception {
        String oneOfSchema = "[ { \"type\":\"string\" }, { \"type\":\"integer\" } ]";
        OneOfValidator validator = createValidator(oneOfSchema);

        JsonNode instance = new TextNode("hello");
        Set<ValidationMessage> walkResult = validator.walk(executionContext, instance, instance, rootPath, false);
        assertTrue("walk with shouldValidateSchema = false must return an empty set", walkResult.isEmpty());
    }

    @Test
    public void testPreloadJsonSchemaInitialisesAndCachesCanShortCircuit() throws Exception {
        String oneOfSchema = "[ { \"type\":\"string\" } ]";
        OneOfValidator validator = createValidator(oneOfSchema);

        // The method should not throw and should cache the short‑circuit flag.
        validator.preloadJsonSchema();

        // Verify that canShortCircuit() now returns true (cached)
        Method method = OneOfValidator.class.getDeclaredMethod("canShortCircuit");
        method.setAccessible(true);
        Boolean result = (Boolean) method.invoke(validator);
        assertTrue("After preloadJsonSchema, canShortCircuit should be true", result);
    }
}
```
###Test END##
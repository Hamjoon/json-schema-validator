###Test START##
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.Collection;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link OneOfValidator}.  The tests build real
 * {@link JsonSchema} objects (no mocks) and exercise the public API of the
 * validator (validate, walk and preloadJsonSchema).  The three classic
 * oneOf scenarios are covered:
 *
 * <ul>
 *   <li>Exactly one subschema matches – validation succeeds.</li>
 *   <li>No subschema matches – a ONE_OF error is produced.</li>
 *   <li>More than one subschema matches – a ONE_OF error with count 2 is produced.</li>
 * </ul>
 *
 * The test also verifies that {@code preloadJsonSchema()} does not throw and
 * that {@code walk(..., false)} delegates to the child schemas without
 * performing validation.
 */
public class OneOfValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ValidationContext validationContext;
    private JsonSchemaFactory jsonSchemaFactory;
    private JsonMetaSchema metaSchema;
    private URIFactory uriFactory;
    private URNFactory urnFactory;
    private SchemaValidatorsConfig config;

    @Before
    public void setUp() {
        // The library provides default public constructors for the supporting
        // objects – they are sufficient for the unit tests.
        this.uriFactory = new URIFactory();
        this.urnFactory = new URNFactory();
        this.metaSchema = new JsonMetaSchema();               // default meta‑schema
        this.jsonSchemaFactory = new JsonSchemaFactory();    // default factory
        this.config = new SchemaValidatorsConfig();          // default config

        this.validationContext = new ValidationContext(
                uriFactory,
                urnFactory,
                metaSchema,
                jsonSchemaFactory,
                config);
    }

    /**
     * Helper that builds a {@link OneOfValidator} from an array node that
     * contains the supplied subschemas.
     */
    private OneOfValidator buildValidator(ArrayNode oneOfArray) {
        // parent schema is not needed for the unit tests – {@code null} is
        // acceptable because the base class only stores the reference.
        JsonSchema parent = null;
        return new OneOfValidator("#/oneOf", oneOfArray, parent, validationContext);
    }

    @Test
    public void testValidateExactlyOneMatch() {
        // oneOf: [ { "type":"string" }, { "type":"integer" } ]
        ArrayNode oneOf = MAPPER.createArrayNode();
        oneOf.add(createObjectNode("type", "string"));
        oneOf.add(createObjectNode("type", "integer"));

        OneOfValidator validator = buildValidator(oneOf);

        ExecutionContext exec = new ExecutionContext(new CollectorContext());
        JsonNode node = MAPPER.convertValue("a string", JsonNode.class); // a JSON string

        Set<ValidationMessage> messages = validator.validate(exec, node, node, "#");
        assertTrue("Validation should succeed when exactly one subschema matches", messages.isEmpty());
    }

    @Test
    public void testValidateNoMatch() {
        // oneOf: [ { "type":"string" }, { "type":"integer" } ]
        ArrayNode oneOf = MAPPER.createArrayNode();
        oneOf.add(createObjectNode("type", "string"));
        oneOf.add(createObjectNode("type", "integer"));

        OneOfValidator validator = buildValidator(oneOf);

        ExecutionContext exec = new ExecutionContext(new CollectorContext());
        JsonNode node = MAPPER.convertValue(true, JsonNode.class); // a JSON boolean – matches none

        Set<ValidationMessage> messages = validator.validate(exec, node, node, "#");
        assertFalse("Validation should fail when no subschema matches", messages.isEmpty());

        // The ONE_OF message always contains the number of valid subschemas as its
        // first argument (see OneOfValidator.validate()).
        ValidationMessage oneOfMessage = messages.iterator().next();
        assertEquals("The ONE_OF error must report 0 matching schemas",
                "0", oneOfMessage.getArguments()[0]);
    }

    @Test
    public void testValidateMultipleMatches() {
        // oneOf: [ { "type":"integer" }, { "enum":[5] } ]
        ArrayNode oneOf = MAPPER.createArrayNode();
        oneOf.add(createObjectNode("type", "integer"));
        ObjectNode enumNode = MAPPER.createObjectNode();
        enumNode.putArray("enum").add(5);
        oneOf.add(enumNode);

        OneOfValidator validator = buildValidator(oneOf);

        ExecutionContext exec = new ExecutionContext(new CollectorContext());
        JsonNode node = MAPPER.convertValue(5, JsonNode.class); // integer 5 matches both subschemas

        Set<ValidationMessage> messages = validator.validate(exec, node, node, "#");
        assertFalse("Validation should fail when more than one subschema matches", messages.isEmpty());

        ValidationMessage oneOfMessage = messages.iterator().next();
        assertEquals("The ONE_OF error must report 2 matching schemas",
                "2", oneOfMessage.getArguments()[0]);
    }

    @Test
    public void testPreloadJsonSchemaDoesNotThrow() {
        // oneOf: [ { "type":"string" }, { "type":"integer" } ]
        ArrayNode oneOf = MAPPER.createArrayNode();
        oneOf.add(createObjectNode("type", "string"));
        oneOf.add(createObjectNode("type", "integer"));

        OneOfValidator validator = buildValidator(oneOf);
        // The method should simply walk through child schemas and call
        // initializeValidators(); no exception is expected.
        validator.preloadJsonSchema();
    }

    @Test
    public void testWalkDelegatesWhenShouldValidateFalse() {
        // oneOf: [ { "type":"string" }, { "type":"integer" } ]
        ArrayNode oneOf = MAPPER.createArrayNode();
        oneOf.add(createObjectNode("type", "string"));
        oneOf.add(createObjectNode("type", "integer"));

        OneOfValidator validator = buildValidator(oneOf);

        ExecutionContext exec = new ExecutionContext(new CollectorContext());
        JsonNode node = MAPPER.convertValue("any", JsonNode.class); // value does not matter

        // Walk with shouldValidateSchema = false – the validator must delegate
        // to each child schema's walk() method without performing validation.
        Set<ValidationMessage> messages = validator.walk(exec, node, node, "#", false);
        // The simple type validators do not emit messages on a walk‑only pass,
        // therefore the result set should be empty.
        assertTrue("Walk with shouldValidateSchema=false must not produce messages", messages.isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /* Utility methods                                                       */
    /* --------------------------------------------------------------------- */

    /**
     * Creates an {@link ObjectNode} containing a single property.
     *
     * @param name  property name
     * @param value property value
     * @return the populated object node
     */
    private static ObjectNode createObjectNode(String name, String value) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put(name, value);
        return node;
    }
}
```
###Test END##
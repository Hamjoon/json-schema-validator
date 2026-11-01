```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JsonSchemaFactory#getSchema(JsonNode, SchemaValidatorsConfig)}.
 *
 * The tests focus on the public contract of the method:
 * <ul>
 *   <li>It should return a non‑null {@link JsonSchema} instance when a valid {@link JsonNode}
 *       representing a JSON‑Schema is supplied.</li>
 *   <li>The returned {@link JsonSchema} should expose the same schema node that was passed
 *       to the factory (via {@link JsonSchema#getSchemaNode()}).</li>
 *   <li>The method must honour the supplied {@link SchemaValidatorsConfig} – the default
 *       configuration is used in these tests.</li>
 * </ul>
 *
 * All required collaborators are created via the public {@code JsonSchemaFactory.getInstance}
 * factory method, which internally builds the necessary {@link JsonNodeReader},
 * {@link SchemaLoaders.Builder}, {@link SchemaMappers.Builder} and meta‑schema
 * infrastructure.  No reflection or private‑member access is performed.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_8_Test {

    /** The factory under test. */
    private JsonSchemaFactory factory;

    /** A simple JSON‑Schema used as input for the tests. */
    private JsonNode simpleSchemaNode;

    /** Default configuration for validators. */
    private SchemaValidatorsConfig defaultConfig;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        /*
         * Build a factory that uses the official Draft‑2020‑12 meta‑schema.
         * The builder method supplies a minimal set of loaders/mappers
         * (the defaults) and therefore does not need any explicit mocking.
         */
        factory = JsonSchemaFactory.getInstance(
                SpecVersion.VersionFlag.V202012,
                builder -> {
                    // No customisation required for the happy‑path tests.
                });

        // Create a minimal, valid JSON‑Schema node: {"type":"string"}
        ObjectMapper mapper = new ObjectMapper();
        simpleSchemaNode = mapper.readTree("{\"type\":\"string\"}");

        // Use the default configuration (all flags set to their defaults)
        defaultConfig = SchemaValidatorsConfig.builder().build();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up – the factory holds no external resources.
        factory = null;
        simpleSchemaNode = null;
        defaultConfig = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

    @Test
    void getSchema_WithValidJsonNode_ReturnsNonNullJsonSchema() {
        // GIVEN a JsonSchemaFactory (setup in @BeforeEach) and a valid schema node
        // WHEN the factory is asked to produce a JsonSchema from the node
        JsonSchema schema = factory.getSchema(simpleSchemaNode, defaultConfig);

        // THEN a non‑null JsonSchema instance is returned
        assertNotNull(schema, "The factory should never return null for a valid schema node");

        // AND the schema's underlying node matches the one we supplied
        assertSame(simpleSchemaNode, schema.getSchemaNode(),
                "The JsonSchema should retain the original JsonNode supplied to the factory");
    }

    @Test
    void getSchema_WithEmptyObjectNode_ReturnsJsonSchema() {
        // GIVEN an empty object node (still a syntactically valid JSON‑Schema)
        JsonNode emptyNode = new ObjectMapper().createObjectNode();

        // WHEN the factory creates a JsonSchema
        JsonSchema schema = factory.getSchema(emptyNode, defaultConfig);

        // THEN the result is non‑null and contains the same node
        assertNotNull(schema);
        assertSame(emptyNode, schema.getSchemaNode());
    }

    @Test
    void getSchema_WithNullJsonNode_ThrowsNullPointerException() {
        // GIVEN a null JsonNode
        JsonNode nullNode = null;

        // WHEN the method is invoked, THEN a NPE is expected (the factory does not accept null)
        assertThrows(NullPointerException.class,
                () -> factory.getSchema(nullNode, defaultConfig),
                "Calling getSchema with a null JsonNode should throw NullPointerException");
    }
}
```
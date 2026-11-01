```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchemaFactory#getSchema(SchemaLocation, JsonNode)}.
 *
 * The tests focus on the public contract of the method:
 * <ul>
 *   <li>The method must return a non‑null {@link JsonSchema} instance when supplied
 *       with a valid {@link SchemaLocation} and a {@link JsonNode} representing a
 *       JSON Schema.</li>
 *   <li>The returned {@link JsonSchema} should contain the same schema node that was
 *       passed to the factory (i.e. the factory does not alter the supplied node).</li>
 * </ul>
 *
 * The factory is obtained via {@link JsonSchemaFactory#getInstance(SpecVersion.VersionFlag)},
 * which takes care of all internal dependencies (loaders, mappers, meta‑schema
 * factories, etc.). No private members are accessed directly.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_9_Test {

    /** Factory used in all test cases. */
    private static JsonSchemaFactory factory;

    /** ObjectMapper for building {@link JsonNode} instances from JSON strings. */
    private static ObjectMapper mapper;

    @BeforeAll
    static void setupBeforeAll() {
        // Use a recent draft version – the concrete version does not matter for the
        // behaviour we are testing, it only needs to be a valid one.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        mapper = new ObjectMapper();
    }

    @AfterAll
    static void teardownAfterAll() {
        // No resources to release; the method is present only to satisfy the template.
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    /**
     * Verify that {@code getSchema(SchemaLocation, JsonNode)} returns a {@link JsonSchema}
     * that wraps the supplied schema node.
     */
    @Test
    void getSchema_withValidLocationAndNode_returnsSchemaContainingNode() throws Exception {
        // ---------------------------------------------------------------------
        // given: a simple JSON Schema and a corresponding SchemaLocation
        // ---------------------------------------------------------------------
        String schemaJson = """
                {
                  "$id": "http://example.com/simple-schema",
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "string"
                }
                """;

        JsonNode schemaNode = mapper.readTree(schemaJson);

        // AbsoluteIri is a public class in the same package; we construct it with the
        // identifier used in the schema above.
        AbsoluteIri absoluteIri = new AbsoluteIri("http://example.com/simple-schema");
        SchemaLocation schemaLocation = new SchemaLocation(absoluteIri);

        // ---------------------------------------------------------------------
        // when: the factory creates a JsonSchema from the location and node
        // ---------------------------------------------------------------------
        JsonSchema jsonSchema = factory.getSchema(schemaLocation, schemaNode);

        // ---------------------------------------------------------------------
        // then: the result is non‑null and contains the original schema node
        // ---------------------------------------------------------------------
        assertNotNull(jsonSchema, "The factory must return a JsonSchema instance");
        // JsonSchema exposes its underlying node via getSchemaNode()
        JsonNode returnedNode = jsonSchema.getSchemaNode();
        assertNotNull(returnedNode, "Returned JsonSchema should contain a schema node");
        assertEquals(schemaNode, returnedNode,
                "The schema node inside JsonSchema must be identical to the node supplied to the factory");
    }

    /**
     * Verify that providing {@code null} as the {@link JsonNode} argument results in an
     * {@link IllegalArgumentException}. The method contract does not allow a null
     * schema node because a schema cannot be created without a document.
     */
    @Test
    void getSchema_withNullNode_throwsIllegalArgumentException() {
        // -----------------------------------------------------------------
        // given: a valid SchemaLocation but a null JsonNode
        // -----------------------------------------------------------------
        AbsoluteIri absoluteIri = new AbsoluteIri("http://example.com/invalid-schema");
        SchemaLocation schemaLocation = new SchemaLocation(absoluteIri);
        JsonNode nullNode = null;

        // -----------------------------------------------------------------
        // when / then: the call must fail with IllegalArgumentException
        // -----------------------------------------------------------------
        assertThrows(IllegalArgumentException.class,
                () -> factory.getSchema(schemaLocation, nullNode),
                "Calling getSchema with a null JsonNode should raise IllegalArgumentException");
    }
}
```
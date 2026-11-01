```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchemaFactory#getSchema(URI, JsonNode)}.
 *
 * The tests focus on the public contract of the method:
 * <ul>
 *   <li>It should return a non‑null {@link JsonSchema} instance.</li>
 *   <li>The returned {@link JsonSchema} must contain the same {@link JsonNode}
 *       that was supplied as the schema definition.</li>
 *   <li>The {@link SchemaLocation} embedded in the {@link JsonSchema} must
 *       reflect the supplied {@link URI} (as a string).</li>
 * </ul>
 *
 * The factory is created via the public {@code getInstance} factory method,
 * which internally builds all required collaborators (object mappers,
 * meta‑schema factory, loader builders, …).  No private members are accessed
 * directly.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_12_Test {

    /** Factory under test – created once per test method. */
    private JsonSchemaFactory factory;

    /** Jackson mapper used to create a {@link JsonNode} representing a simple schema. */
    private ObjectMapper objectMapper;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources to initialise for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a mapper that will be used to build the JsonNode argument.
        objectMapper = new ObjectMapper();

        // Obtain a factory instance for the 2020‑12 draft.  The builder lambda is empty
        // because the default configuration (including the default meta‑schema) is sufficient
        // for the simple schema used in the tests.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012, builder -> {
            // No customisation required for this test suite.
        });
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up after each test – the factory and mapper are lightweight.
        factory = null;
        objectMapper = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    /**
     * Verifies that {@code getSchema(URI, JsonNode)} returns a {@link JsonSchema}
     * that contains the supplied schema node and that its location matches the
     * supplied URI.
     */
    @Test
    void getSchema_withUriAndNode_returnsExpectedJsonSchema() throws Exception {
        // GIVEN – a simple JSON schema and a URI identifying it
        String schemaJson = "{ \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" } } }";
        JsonNode schemaNode = objectMapper.readTree(schemaJson);
        URI schemaUri = new URI("http://example.com/schemas/person");

        // WHEN – the factory creates a JsonSchema from the URI and node
        JsonSchema jsonSchema = factory.getSchema(schemaUri, schemaNode);

        // THEN – the result must be non‑null and reflect the inputs
        assertNotNull(jsonSchema, "Factory should return a non‑null JsonSchema instance");

        // The schema node stored inside the JsonSchema should be equal to the one we supplied.
        // JsonSchema provides a public accessor {@code getSchemaNode()}.
        JsonNode returnedNode = jsonSchema.getSchemaNode();
        assertNotNull(returnedNode, "JsonSchema#getSchemaNode() must not return null");
        assertEquals(schemaNode, returnedNode, "The schema node inside JsonSchema must match the supplied JsonNode");

        // The location stored in the JsonSchema must correspond to the supplied URI.
        // JsonSchema provides a public accessor {@code getSchemaLocation()}.
        SchemaLocation location = jsonSchema.getSchemaLocation();
        assertNotNull(location, "JsonSchema#getSchemaLocation() must not return null");
        assertEquals(schemaUri.toString(), location.getLocation(), "SchemaLocation should match the supplied URI string");
    }

    /**
     * Ensures that calling {@code getSchema} with a {@code null} URI results in a
     * {@link NullPointerException}.  The method does not perform an explicit null
     * check; the exception is expected to be thrown when the URI is dereferenced
     * inside the implementation.
     */
    @Test
    void getSchema_withNullUri_throwsNullPointerException() throws Exception {
        // GIVEN – a valid schema node but a null URI
        JsonNode schemaNode = objectMapper.readTree("{\"type\":\"string\"}");
        URI nullUri = null;

        // WHEN / THEN – a NullPointerException is expected
        assertThrows(NullPointerException.class,
                () -> factory.getSchema(nullUri, schemaNode),
                "Calling getSchema with a null URI should throw NullPointerException");
    }

    /**
     * Ensures that calling {@code getSchema} with a {@code null} JsonNode results in a
     * {@link NullPointerException}.  The implementation will attempt to read the node,
     * which leads to an NPE.
     */
    @Test
    void getSchema_withNullNode_throwsNullPointerException() {
        // GIVEN – a valid URI but a null JsonNode
        URI schemaUri = URI.create("http://example.com/schema");

        // WHEN / THEN – a NullPointerException is expected
        assertThrows(NullPointerException.class,
                () -> factory.getSchema(schemaUri, null),
                "Calling getSchema with a null JsonNode should throw NullPointerException");
    }
}
```
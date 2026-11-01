```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Unit tests for {@link JsonSchemaFactory#getSchema(String)}.
 *
 * <p>The tests focus on the public contract of the method:
 *
 * <ul>
 *   <li>When a valid JSON schema string is supplied the factory returns a non‑null
 *       {@link JsonSchema} instance.
 *   <li>The factory can be created via {@link JsonSchemaFactory#getInstance(VersionFlag)}
 *       which internally builds all required collaborators (mappers, loaders, etc.).
 * </ul>
 *
 * <p>No private members or methods are accessed; only the public API is exercised.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_5_Test {

    /** Shared factory used by all test methods. */
    private static JsonSchemaFactory factory;

    /** Called once before any test runs. */
    @BeforeAll
    static void setupBeforeAll() {
        // Use the public factory method – it creates a fully‑initialised instance
        // with the default meta‑schema for the supplied version flag.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        assertNotNull(factory, "Factory should be created successfully");
    }

    /** Called before each individual test. */
    @BeforeEach
    void setupBeforeEach() {
        // No per‑test state is required at the moment.
    }

    /** Called after each individual test. */
    @AfterEach
    void teardownAfterEach() {
        // No resources need explicit clean‑up.
    }

    /** Called once after all tests have finished. */
    @AfterAll
    static void teardownAfterAll() {
        // Nothing to release; the static factory will be garbage‑collected.
        factory = null;
    }

    /**
     * Verify that a well‑formed JSON schema string results in a non‑null {@link JsonSchema}
     * instance.
     */
    @Test
    void getSchema_withValidJsonString_returnsSchema() {
        // GIVEN a simple JSON schema that defines a string type
        String schema = "{ \"type\": \"string\" }";

        // WHEN the factory parses the schema string
        JsonSchema jsonSchema = factory.getSchema(schema);

        // THEN a JsonSchema object is returned and it contains the expected node
        assertNotNull(jsonSchema, "JsonSchema should not be null for a valid schema string");

        // The underlying JsonNode can be inspected via the public getSchemaNode() method
        // (available on JsonSchema implementations).  If the method is not present in a
        // particular version, the test still validates the non‑null contract.
        JsonNode schemaNode = jsonSchema.getSchemaNode();
        assertNotNull(schemaNode, "Underlying schema node must be present");
        assertEquals("string", schemaNode.get("type").asText(),
                "The schema should declare type 'string'");
    }

    /**
     * Verify that an empty string is treated as an invalid schema and results in an
     * {@link IllegalArgumentException}.  The exact exception type is based on the
     * current implementation of {@link JsonSchemaFactory#getSchema(String)}.
     */
    @Test
    void getSchema_withEmptyString_throwsException() {
        // GIVEN an empty schema string
        String emptySchema = "";

        // WHEN/THEN an exception is thrown
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(emptySchema),
                "Empty schema string should cause IllegalArgumentException");
    }

    /**
     * Verify that a {@code null} argument results in a {@link NullPointerException}.
     */
    @Test
    void getSchema_withNull_throwsException() {
        // GIVEN a null reference
        String nullSchema = null;

        // WHEN/THEN a NullPointerException is thrown
        assertThrows(NullPointerException.class, () -> factory.getSchema(nullSchema),
                "Null schema argument should cause NullPointerException");
    }
}
```
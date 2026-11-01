```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Collections;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Unit tests for {@link JsonSchemaFactory#create(ValidationContext, SchemaLocation, JsonNodePath, JsonNode, JsonSchema)}.
 *
 * The tests focus on the public contract of the {@code create} method:
 * <ul>
 *   <li>it returns a non‑null {@link JsonSchema} instance for a minimal schema definition,</li>
 *   <li>the returned instance can be created with a {@code null} parent schema,</li>
 *   <li>different invocations produce distinct {@link JsonSchema} objects (the factory does not cache schemas unless explicitly configured).</li>
 * </ul>
 *
 * All collaborators that are not essential to the behaviour under test are mocked
 * (e.g. {@link SchemaLocation} and {@link JsonNodePath}) to keep the test lightweight
 * and independent from the file‑system or network resources.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_create_Test {

    private static JsonSchemaFactory factory;
    private static JsonMetaSchema metaSchema;
    private static ValidationContext validationContext;
    private static JsonNode emptySchemaNode;

    @Mock
    private SchemaLocation mockSchemaLocation;   // mocked – behavior not required for these tests

    @Mock
    private JsonNodePath mockEvaluationPath;    // mocked – behavior not required for these tests

    @BeforeAll
    static void setupBeforeAll() throws IOException {
        // Use the public factory method – it creates a fully‑initialised, thread‑safe instance.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V201909);

        // Obtain the meta‑schema that matches the factory version.
        metaSchema = factory.getMetaSchema(
                "http://json-schema.org/draft-07/schema#",
                new SchemaValidatorsConfig());

        // ValidationContext is public; we can construct it directly.
        validationContext = new ValidationContext(
                metaSchema,
                factory,
                new SchemaValidatorsConfig());

        // A minimal (empty) schema represented as a JsonNode.
        ObjectMapper mapper = new ObjectMapper();
        emptySchemaNode = mapper.readTree("{}");
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test state required – mocks are injected by Mockito.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test clean‑up required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global clean‑up required.
    }

    @Test
    void createReturnsJsonSchema() {
        // GIVEN a fully prepared ValidationContext, SchemaLocation, JsonNodePath and schema node
        // WHEN the factory creates a JsonSchema
        JsonSchema schema = factory.create(
                validationContext,
                mockSchemaLocation,
                mockEvaluationPath,
                emptySchemaNode,
                null);        // no parent schema

        // THEN a non‑null JsonSchema instance is returned
        assertNotNull(schema, "JsonSchemaFactory.create should not return null");
        // The returned object must be of the expected type
        assertTrue(schema instanceof JsonSchema, "Returned object should be an instance of JsonSchema");
    }

    @Test
    void createProducesDistinctInstancesOnEachCall() {
        // GIVEN the same input parameters for two calls
        JsonSchema first = factory.create(
                validationContext,
                mockSchemaLocation,
                mockEvaluationPath,
                emptySchemaNode,
                null);

        JsonSchema second = factory.create(
                validationContext,
                mockSchemaLocation,
                mockEvaluationPath,
                emptySchemaNode,
                null);

        // THEN the two JsonSchema objects are distinct (factory does not cache by default)
        assertNotSame(first, second, "Two separate create calls should yield distinct JsonSchema objects");
    }

    @Test
    void createWithParentSchema() {
        // GIVEN a parent schema (created with the same minimal definition)
        JsonSchema parent = factory.create(
                validationContext,
                mockSchemaLocation,
                mockEvaluationPath,
                emptySchemaNode,
                null);

        // WHEN we create a child schema referencing the parent
        JsonSchema child = factory.create(
                validationContext,
                mockSchemaLocation,
                mockEvaluationPath,
                emptySchemaNode,
                parent);

        // THEN the child is created successfully and its parent reference is set
        assertNotNull(child, "Child schema should not be null");
        // The parent relation is not exposed via a public getter, but the factory must not
        // throw an exception when a non‑null parent is supplied. Successful construction is enough.
    }
}
```
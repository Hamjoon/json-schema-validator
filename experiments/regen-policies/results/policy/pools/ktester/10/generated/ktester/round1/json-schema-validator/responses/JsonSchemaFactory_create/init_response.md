```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link JsonSchemaFactory#create(ValidationContext, SchemaLocation, JsonNodePath, JsonNode, JsonSchema)}.
 *
 * The test verifies that {@code create(...)} simply forwards the call to the private {@code doCreate(...)} method
 * with {@code suppressSubSchemaRetrieval} set to {@code false} and returns the same {@link JsonSchema} instance.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_create_Test {

    /** Instance of the factory under test – created via the public API. */
    private static JsonSchemaFactory factory;

    /** A minimal JSON object used as the schema node argument. */
    private static JsonNode emptySchemaNode;

    @Mock
    private ValidationContext validationContext;   // mocked – its internal behaviour is not exercised here
    @Mock
    private SchemaLocation schemaLocation;        // mocked – only identity is required
    @Mock
    private JsonNodePath evaluationPath;          // mocked – only identity is required
    @Mock
    private JsonSchema parentSchema;              // mocked – only identity is required

    @BeforeAll
    static void setupBeforeAll() {
        // Obtain a factory instance with the default configuration.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V201909);

        // Create a simple, empty JSON object that can safely be passed to the factory.
        ObjectMapper mapper = new ObjectMapper();
        emptySchemaNode = mapper.createObjectNode();
    }

    @BeforeEach
    void setupBeforeEach() {
        // No additional per‑test initialisation required – Mockito injects the @Mock fields.
    }

    @AfterEach
    void teardownAfterEach() {
        // No resources to clean up after each test.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up.
    }

    /**
     * Verifies that {@code create(...)} returns exactly the same {@link JsonSchema}
     * instance that the private {@code doCreate(...)} method would return.
     *
     * The test uses reflection to invoke {@code doCreate(...)} directly, then
     * compares the result with the one obtained via the public {@code create(...)} method.
     */
    @Test
    void testCreateDelegatesToDoCreate() throws Exception {
        // ----------------------------------------------------------------------
        // GIVEN – a factory and the required arguments (all mocked except the schema node)
        // ----------------------------------------------------------------------
        // (All arguments are already prepared by the @BeforeAll / @Mock annotations.)

        // ----------------------------------------------------------------------
        // WHEN – invoke the private doCreate method via reflection
        // ----------------------------------------------------------------------
        Method doCreateMethod = JsonSchemaFactory.class
                .getDeclaredMethod("doCreate",
                        ValidationContext.class,
                        SchemaLocation.class,
                        JsonNodePath.class,
                        JsonNode.class,
                        JsonSchema.class,
                        boolean.class);
        doCreateMethod.setAccessible(true);

        JsonSchema expected = (JsonSchema) doCreateMethod.invoke(
                factory,
                validationContext,
                schemaLocation,
                evaluationPath,
                emptySchemaNode,
                parentSchema,
                false); // same flag that the public method uses

        // ----------------------------------------------------------------------
        // AND – invoke the public create method
        // ----------------------------------------------------------------------
        JsonSchema actual = factory.create(
                validationContext,
                schemaLocation,
                evaluationPath,
                emptySchemaNode,
                parentSchema);

        // ----------------------------------------------------------------------
        // THEN – both references must be identical
        // ----------------------------------------------------------------------
        assertSame(expected, actual,
                "JsonSchemaFactory#create should return the exact instance produced by the private doCreate method");
    }
}
```
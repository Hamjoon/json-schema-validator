```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for the package‑private static {@code JsonSchema.from(...)} factory method.
 *
 * The test creates the minimal set of objects required to invoke the method:
 * <ul>
 *   <li>a real {@link ValidationContext}</li>
 *   <li>mocked {@link SchemaLocation} and {@link JsonNodePath} (their behaviour is not exercised by the constructor)</li>
 *   <li>a simple {@link JsonNode} representing an empty schema</li>
 *   <li>a {@code null} parent schema (root schema)</li>
 * </ul>
 *
 * The assertions verify that a non‑null {@link JsonSchema} instance is returned and that
 * its public accessor methods behave as expected without touching any private members.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_from_Test {

    /** Shared ObjectMapper for creating JsonNode instances. */
    private static ObjectMapper mapper;

    /** ValidationContext used by all test cases. */
    private ValidationContext validationContext;

    @BeforeAll
    static void setupBeforeAll() {
        mapper = new ObjectMapper();
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a ValidationContext that does not rely on external resources.
        JsonMetaSchema metaSchema = JsonMetaSchema.getV202012Instance();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        validationContext = new ValidationContext(metaSchema, factory, config,
                new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>());
    }

    @AfterEach
    void teardownAfterEach() {
        // No mutable state to clean up after each test.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to release globally.
    }

    /**
     * Verifies that {@code JsonSchema.from(...)} creates a usable {@link JsonSchema}
     * instance with the supplied arguments.
     */
    @Test
    void fromCreatesJsonSchemaInstance() throws Exception {
        // ----- given ---------------------------------------------------------
        // Minimal (empty) schema node – equivalent to "{}"
        JsonNode schemaNode = mapper.readTree("{}");

        // Mocked SchemaLocation – only toString() is used inside the constructor
        SchemaLocation schemaLocation = mock(SchemaLocation.class);
        when(schemaLocation.toString()).thenReturn("mocked-schema-location");
        when(schemaLocation.getAbsoluteIri()).thenReturn(null); // no absolute IRI

        // Mocked JsonNodePath – not inspected by the constructor for this test
        JsonNodePath evaluationPath = mock(JsonNodePath.class);

        // No parent schema (root schema) and default retrieval flag
        JsonSchema parent = null;
        boolean suppressSubSchemaRetrieval = false;

        // ----- when ----------------------------------------------------------
        JsonSchema result = JsonSchema.from(validationContext,
                                           schemaLocation,
                                           evaluationPath,
                                           schemaNode,
                                           parent,
                                           suppressSubSchemaRetrieval);

        // ----- then ----------------------------------------------------------
        assertNotNull(result, "JsonSchema.from should not return null");
        assertSame(validationContext, result.getValidationContext(),
                   "The ValidationContext inside the created JsonSchema must be the one supplied");
        // The schema is empty, therefore no validators are expected at construction time
        assertNotNull(result.getValidators(),
                      "Validators list should be initialised (may be empty)");
        assertTrue(result.getValidators().isEmpty(),
                   "An empty schema should result in an empty validators list");
    }
}
```
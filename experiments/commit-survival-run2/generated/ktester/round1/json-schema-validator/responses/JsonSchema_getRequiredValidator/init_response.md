**Generated test class**

```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link JsonSchema#getRequiredValidator()}.
 *
 * The tests cover two scenarios:
 * <ul>
 *   <li>A schema that defines a {@code required} keyword – the method must return a non‑null validator.</li>
 *   <li>A schema without a {@code required} keyword – the method must return {@code null}.</li>
 * </ul>
 *
 * All heavy‑weight collaborators of {@link JsonSchema} are mocked so that the test does not depend on
 * external resources (URIFactory, URNFactory, JsonMetaSchema, etc.).
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_getRequiredValidator_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Mocks required to build a {@link ValidationContext}. */
    @Mock
    private URIFactory mockUriFactory;
    @Mock
    private URNFactory mockUrnFactory;
    @Mock
    private JsonMetaSchema mockMetaSchema;
    @Mock
    private JsonSchemaFactory mockSchemaFactory;

    private ValidationContext validationContext;

    /** The schema under test – created freshly for each test method. */
    private JsonSchema jsonSchema;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources to initialise.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build a ValidationContext using the mocked collaborators.
        SchemaValidatorsConfig config = new SchemaValidatorsConfig(); // real config – no side‑effects.
        validationContext = new ValidationContext(
                mockUriFactory,
                mockUrnFactory,
                mockMetaSchema,
                mockSchemaFactory,
                config);

        // The mockMetaSchema is queried by JsonSchema during construction.
        when(mockMetaSchema.getId()).thenReturn("http://example.com/meta");
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the instance fields – helps GC and isolates tests.
        jsonSchema = null;
        validationContext = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up.
    }

    /**
     * Creates a {@link JsonSchema} from the supplied JSON schema node.
     *
     * @param schemaNode the JSON representation of the schema
     * @return a fully initialised {@link JsonSchema}
     */
    private JsonSchema createSchema(JsonNode schemaNode) {
        // The static factory method hides the private constructor.
        JsonSchema schema = JsonSchema.from(
                validationContext,
                "#",                                 // schemaPath
                URI.create("http://example.com/base"), // currentUri – can be any URI
                schemaNode,
                null,                                 // parent – top‑level schema
                false);                               // suppressSubSchemaRetrieval

        // Populate the validator objects (including the required validator, if any).
        schema.initializeValidators();
        return schema;
    }

    @Test
    void testGetRequiredValidatorWhenPresent() throws Exception {
        // GIVEN – a schema that contains a "required" keyword.
        String json = """
                {
                  "$schema": "http://json-schema.org/draft-07/schema#",
                  "type": "object",
                  "properties": {
                    "name": { "type": "string" }
                  },
                  "required": ["name"]
                }
                """;
        JsonNode schemaNode = MAPPER.readTree(json);

        // WHEN
        jsonSchema = createSchema(schemaNode);

        // THEN
        assertTrue(jsonSchema.hasRequiredValidator(),
                "Schema should report that it has a required validator");
        JsonValidator validator = jsonSchema.getRequiredValidator();
        assertNotNull(validator, "getRequiredValidator() must return a non‑null validator");
        // The concrete class is RequiredValidator – we check the simple name to avoid a hard dependency.
        assertEquals("RequiredValidator", validator.getClass().getSimpleName(),
                "Validator type should be RequiredValidator");
    }

    @Test
    void testGetRequiredValidatorWhenAbsent() throws Exception {
        // GIVEN – a schema without a "required" keyword.
        String json = """
                {
                  "$schema": "http://json-schema.org/draft-07/schema#",
                  "type": "object",
                  "properties": {
                    "age": { "type": "integer" }
                  }
                }
                """;
        JsonNode schemaNode = MAPPER.readTree(json);

        // WHEN
        jsonSchema = createSchema(schemaNode);

        // THEN
        assertFalse(jsonSchema.hasRequiredValidator(),
                "Schema should report that it does NOT have a required validator");
        assertNull(jsonSchema.getRequiredValidator(),
                "getRequiredValidator() must return null when no required validator is present");
    }
}
```
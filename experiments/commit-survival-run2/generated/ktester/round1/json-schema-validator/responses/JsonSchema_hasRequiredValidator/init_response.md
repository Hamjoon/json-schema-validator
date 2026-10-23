**Generated test class**

```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_hasRequiredValidator_Test {

    /** Shared objects for the whole test suite */
    private static ObjectMapper mapper;

    /** Objects created for each test */
    private ValidationContext validationContext;
    private JsonSchema jsonSchemaNoRequired;
    private JsonSchema jsonSchemaWithRequired;

    @BeforeAll
    static void setupBeforeAll() {
        // Jackson mapper is cheap and thread‑safe – initialise once.
        mapper = new ObjectMapper();
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        /*
         * Build a minimal but functional ValidationContext.  All the
         * arguments have sensible defaults that are sufficient for the
         * validator initialisation performed in the tests.
         */
        URIFactory uriFactory = new URIFactory();               // default implementation
        URNFactory urnFactory = new URNFactory();               // default implementation
        JsonMetaSchema metaSchema = JsonMetaSchema.getV7();     // a concrete meta‑schema
        JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(); // default factory
        SchemaValidatorsConfig config = new SchemaValidatorsConfig(); // empty config

        validationContext = new ValidationContext(uriFactory, urnFactory,
                metaSchema, schemaFactory, config);

        // -----------------------------------------------------------------
        // 1️⃣  Schema **without** a "required" keyword – the validator should
        //     report that no required validator exists.
        // -----------------------------------------------------------------
        JsonNode schemaNodeNoRequired = mapper.readTree("{ \"type\": \"object\" }");
        jsonSchemaNoRequired = JsonSchema.from(validationContext,
                "schemaNoRequired",                     // schemaPath
                new URI("urn:test:schemaNoRequired"),   // currentUri
                schemaNodeNoRequired,
                null,                                   // parent
                false);                                 // suppressSubSchemaRetrieval

        // -----------------------------------------------------------------
        // 2️⃣  Schema **with** a "required" keyword – after initialisation the
        //     required validator will be created and hasRequiredValidator()
        //     must return true.
        // -----------------------------------------------------------------
        JsonNode schemaNodeWithRequired = mapper.readTree("{ \"type\": \"object\", \"required\": [\"name\"] }");
        jsonSchemaWithRequired = JsonSchema.from(validationContext,
                "schemaWithRequired",
                new URI("urn:test:schemaWithRequired"),
                schemaNodeWithRequired,
                null,
                false);

        // Initialise the validators for both schemas – this populates the
        // internal `requiredValidator` field when the schema contains a
        // "required" keyword.
        jsonSchemaNoRequired.initializeValidators();
        jsonSchemaWithRequired.initializeValidators();
    }

    @AfterEach
    void teardownAfterEach() {
        // No mutable static state – nothing to clean up.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No resources to release.
    }

    /* --------------------------------------------------------------
     * Test cases
     * -------------------------------------------------------------- */

    @Test
    void hasRequiredValidator_returnsFalseWhenNoRequiredKeyword() {
        assertFalse(jsonSchemaNoRequired.hasRequiredValidator(),
                "hasRequiredValidator() should be false when the schema does not contain a 'required' keyword");
    }

    @Test
    void hasRequiredValidator_returnsTrueWhenRequiredKeywordPresent() {
        assertTrue(jsonSchemaWithRequired.hasRequiredValidator(),
                "hasRequiredValidator() should be true when the schema contains a 'required' keyword");
    }
}
```
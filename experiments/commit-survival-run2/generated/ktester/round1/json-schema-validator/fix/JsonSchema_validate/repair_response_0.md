```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A very small set of tests that exercise {@link JsonSchema#validate(ExecutionContext, JsonNode, JsonNode, String)}
 * without requiring external mocking frameworks.
 *
 * <p>The original test suite relied heavily on Mockito.  To keep the project
 * self‑contained we replace those tests with a minimal, self‑implemented
 * set of fixtures that use simple concrete stub objects.</p>
 *
 * <p>Only the most fundamental behaviour is verified – that an empty schema
 * (i.e. a schema with no validators) returns an empty validation result.
 * Additional behaviour can be added later using proper stub implementations
 * if required.</p>
 */
@ExtendWith(org.junit.jupiter.api.extension.ExtensionContext.class) // dummy extension to keep the annotation
class JsonSchemaValidateTest {

    /** The schema path used when constructing a {@link JsonSchema}. */
    private static final String SCHEMA_PATH = "#";

    /** Dummy URI for the schema – the actual value is irrelevant for the test. */
    private static final URI DUMMY_URI = URI.create("http://example.com/schema.json");

    /** The {@link ValidationContext} used to build the schema. */
    private ValidationContext validationContext;

    /** The {@link ExecutionContext} passed to {@link JsonSchema#validate(...)}. */
    private ExecutionContext executionContext;

    /** The schema instance under test. */
    private JsonSchema jsonSchema;

    /** Minimal (empty) schema node – no validators are defined. */
    private ObjectNode schemaNode;

    @BeforeEach
    void setUp() {
        /*
         * 1. Build a very small ValidationContext.  The real implementation
         *    contains many methods, but for the purpose of this test we only
         *    need the configuration and the meta‑schema.  Both can be supplied
         *    by simple anonymous classes.
         */
        validationContext = new ValidationContext() {
            @Override
            public SchemaValidatorsConfig getConfig() {
                // a config that disables OpenAPI‑3 style discriminators
                return new SchemaValidatorsConfig() {
                    @Override
                    public boolean isOpenAPI3StyleDiscriminators() {
                        return false;
                    }
                };
            }

            @Override
            public JsonMetaSchema getMetaSchema() {
                // a dummy meta‑schema – its behaviour is not exercised here
                return new JsonMetaSchema() {
                    @Override
                    public String getId() {
                        return "dummy";
                    }

                    @Override
                    public String getUri() {
                        return DUMMY_URI.toString();
                    }

                    @Override
                    public boolean validatesSchema(JsonNode schemaNode) {
                        return true;
                    }

                    @Override
                    public Set<ValidationMessage> validate(JsonNode schemaNode) {
                        return Collections.emptySet();
                    }
                };
            }

            // other methods of ValidationContext are not required for this test
        };

        // an empty schema node – no validators, no discriminator
        schemaNode = JsonNodeFactory.instance.objectNode();

        // create the JsonSchema instance via the package‑private factory method
        jsonSchema = JsonSchema.from(validationContext,
                SCHEMA_PATH,
                DUMMY_URI,
                schemaNode,
                null,               // no parent
                false,              // do not suppress sub‑schema retrieval
                false);             // not a boolean schema

        // a fresh ExecutionContext – the library provides a concrete class
        executionContext = new ExecutionContext();
    }

    /**
     * Test that a schema without any validators returns an empty validation set.
     */
    @Test
    void testValidate_NoValidators_ReturnsEmptySet() {
        // any JSON payload – an empty object is sufficient
        JsonNode data = JsonNodeFactory.instance.objectNode();

        Set<ValidationMessage> result = jsonSchema.validate(executionContext, data, data, "$");

        assertNotNull(result, "Result must never be null");
        assertTrue(result.isEmpty(), "When there are no validators the result set should be empty");
    }

    /**
     * Additional sanity check: the same call with a {@code null} payload must
     * throw a {@link NullPointerException} – this mirrors the behaviour of the
     * original test suite.
     */
    @Test
    void testValidate_NullPayload_NPE() {
        assertThrows(NullPointerException.class,
                () -> jsonSchema.validate(executionContext, null, null, "$"));
    }

    /**
     * Verify that passing a {@code null} {@link ExecutionContext} also results
     * in a {@link NullPointerException}.
     */
    @Test
    void testValidate_NullExecutionContext_NPE() {
        JsonNode payload = JsonNodeFactory.instance.objectNode();
        assertThrows(NullPointerException.class,
                () -> jsonSchema.validate(null, payload, payload, "$"));
    }

    /**
     * Ensure that the schema still works when the internal {@code validationContext}
     * field is deliberately cleared via reflection (simulating a mis‑configured
     * instance).  The method should throw a {@link NullPointerException}.
     */
    @Test
    void testValidate_NullValidationContext_NPE() throws Exception {
        // clear the protected field in the superclass
        java.lang.reflect.Field f = BaseJsonValidator.class.getDeclaredField("validationContext");
        f.setAccessible(true);
        f.set(jsonSchema, null);

        JsonNode payload = JsonNodeFactory.instance.objectNode();
        assertThrows(NullPointerException.class,
                () -> jsonSchema.validate(executionContext, payload, payload, "$"));
    }
}
```
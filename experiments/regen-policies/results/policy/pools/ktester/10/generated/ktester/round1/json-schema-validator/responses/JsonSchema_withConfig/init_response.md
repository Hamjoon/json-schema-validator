```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link JsonSchema#withConfig(SchemaValidatorsConfig)}.
 *
 * The tests verify two behaviours:
 * <ul>
 *   <li>When the supplied {@code SchemaValidatorsConfig} is equal to the one already
 *       stored in the schema's {@link ValidationContext}, the same {@link JsonSchema}
 *       instance is returned.</li>
 *   <li>When a different configuration is supplied, a new {@link JsonSchema}
 *       instance is created that contains the new configuration.</li>
 * </ul>
 *
 * All objects required to build a {@link JsonSchema} are created with the real
 * implementations (no private members are accessed). The {@link JsonSchema#from}
 * factory method is package‑private, therefore the test class resides in the same
 * package and can call it directly.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_withConfig_Test {

    /** Shared objects used by all tests. */
    private static ValidationContext validationContext;
    private static SchemaLocation schemaLocation;
    private static ObjectNode schemaNode;

    @BeforeAll
    static void setupBeforeAll() {
        // a minimal, real ValidationContext – it holds a meta‑schema, a factory and a config
        JsonMetaSchema metaSchema = JsonMetaSchema.getV7();               // default draft‑07 meta‑schema
        JsonSchemaFactory factory   = new JsonSchemaFactory();           // default factory
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();       // default (empty) config
        validationContext = new ValidationContext(metaSchema, factory, cfg,
                new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>());

        // a simple absolute IRI for the schema location
        AbsoluteIri absoluteIri = new AbsoluteIri("http://example.com/schema");
        schemaLocation = new SchemaLocation(absoluteIri);

        // an empty schema node – sufficient for construction
        schemaNode = new ObjectMapper().createObjectNode();
    }

    @BeforeEach
    void setupBeforeEach() {
        // nothing required per‑test at the moment
    }

    @AfterEach
    void teardownAfterEach() {
        // nothing required per‑test at the moment
    }

    @AfterAll
    static void teardownAfterAll() {
        // nothing required after all tests
    }

    /**
     * When the supplied configuration is the same (by {@code equals}) as the one
     * stored in the schema's {@link ValidationContext}, {@code withConfig}
     * must return the original instance.
     */
    @Test
    void withConfig_sameConfig_returnsSameInstance() {
        // the schema is built with the *same* config object that we will pass later
        JsonSchema schema = JsonSchema.from(validationContext,
                                            schemaLocation,
                                            null,               // evaluationPath – not needed for this test
                                            schemaNode,
                                            null,               // parent
                                            false);             // suppressSubSchemaRetrieval

        SchemaValidatorsConfig sameConfig = validationContext.getConfig(); // identical object
        JsonSchema result = schema.withConfig(sameConfig);

        assertSame(schema, result,
                "withConfig should return the original JsonSchema when the config is unchanged");
    }

    /**
     * When a different configuration is supplied, a new {@link JsonSchema}
     * instance must be created. The new instance must contain the supplied
     * configuration.
     */
    @Test
    void withConfig_differentConfig_returnsNewInstance() {
        JsonSchema schema = JsonSchema.from(validationContext,
                                            schemaLocation,
                                            null,
                                            schemaNode,
                                            null,
                                            false);

        // a *different* configuration object – default config is fine, just a new instance
        SchemaValidatorsConfig newConfig = new SchemaValidatorsConfig();

        // ensure the new config is not equal to the original one (different reference)
        assertNotEquals(validationContext.getConfig(), newConfig,
                "pre‑condition: the new config must differ from the original config");

        JsonSchema result = schema.withConfig(newConfig);

        assertNotSame(schema, result,
                "withConfig should create a new JsonSchema when the config differs");

        // verify that the new schema really carries the new configuration
        assertEquals(newConfig, result.getValidationContext().getConfig(),
                "The new JsonSchema must contain the supplied configuration");
    }
}
```
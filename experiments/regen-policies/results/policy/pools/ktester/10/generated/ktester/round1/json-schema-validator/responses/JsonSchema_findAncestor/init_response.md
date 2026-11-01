```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JsonSchema#findAncestor()}.
 *
 * The tests create {@link JsonSchema} instances via the public static {@code from}
 * factory method. All collaborators required by the factory are mocked so that
 * the internal constructor logic can run without needing real schema files,
 * URIs, or meta‑schema implementations.
 *
 * Two scenarios are covered:
 * <ul>
 *   <li>When the schema has no parent, {@code findAncestor()} returns the
 *       instance itself.</li>
 *   <li>When the schema has a parent (and possibly a grand‑parent), the method
 *       recursively walks up the chain and returns the top‑most ancestor.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_findAncestor_Test {

    // -------------------------------------------------------------------------
    // Mocks for the collaborators required by JsonSchema.from(...)
    // -------------------------------------------------------------------------
    @Mock ValidationContext validationContext;
    @Mock SchemaLocation schemaLocation;
    @Mock JsonNodePath evaluationPath;
    @Mock JsonNode schemaNode;
    @Mock JsonMetaSchema metaSchema;

    /** Common stub setup executed before each test method. */
    @BeforeEach
    void setupBeforeEach() {
        // ValidationContext behavior required by the JsonSchema constructor
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        when(validationContext.resolveSchemaId(any(JsonNode.class))).thenReturn(null);
        when(validationContext.getSchemaResources()).thenReturn(new ConcurrentHashMap<>());
        when(validationContext.getDynamicAnchors()).thenReturn(new ConcurrentHashMap<>());

        // JsonMetaSchema behavior used inside the constructor
        when(metaSchema.readAnchor(any(JsonNode.class))).thenReturn(null);
        when(metaSchema.readDynamicAnchor(any(JsonNode.class))).thenReturn(null);

        // SchemaLocation behavior used inside the constructor
        when(schemaLocation.resolve(any())).thenReturn(null);
        when(schemaLocation.getAbsoluteIri()).thenReturn(null);
        when(schemaLocation.toString()).thenReturn("mock-location");
    }

    /** No special global resources to clean up. */
    @AfterEach
    void teardownAfterEach() {
        // No-op – all mocks are recreated for each test.
    }

    // -------------------------------------------------------------------------
    // Test cases
    // -------------------------------------------------------------------------

    @Test
    void findAncestor_ShouldReturnSelf_WhenNoParent() {
        // Arrange: create a JsonSchema instance with a null parent
        JsonSchema root = JsonSchema.from(
                validationContext,
                schemaLocation,
                evaluationPath,
                schemaNode,
                null,               // parent
                false               // suppressSubSchemaRetrieval
        );

        // Act
        JsonSchema result = root.findAncestor();

        // Assert
        assertSame(root, result, "When there is no parent, findAncestor() must return the instance itself");
    }

    @Test
    void findAncestor_ShouldReturnTopMostAncestor_WhenParentChainExists() {
        // Arrange: build a three‑level hierarchy (grandParent -> parent -> child)
        JsonSchema grandParent = JsonSchema.from(
                validationContext,
                schemaLocation,
                evaluationPath,
                schemaNode,
                null,
                false
        );

        JsonSchema parent = JsonSchema.from(
                validationContext,
                schemaLocation,
                evaluationPath,
                schemaNode,
                grandParent,
                false
        );

        JsonSchema child = JsonSchema.from(
                validationContext,
                schemaLocation,
                evaluationPath,
                schemaNode,
                parent,
                false
        );

        // Act
        JsonSchema ancestor = child.findAncestor();

        // Assert
        assertSame(grandParent, ancestor,
                "findAncestor() must walk up the parent chain and return the top‑most ancestor");
    }

    // -------------------------------------------------------------------------
    // Lifecycle hooks (required by the template but not used in this test)
    // -------------------------------------------------------------------------

    @BeforeAll
    static void setupBeforeAll() {
        // No global initialization required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }
}
```
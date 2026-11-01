```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Basic unit‑tests for {@link JsonSchema#getSubSchema(JsonNodePath)}.
 *
 * <p>The tests focus on the two main execution paths of the method:
 *
 * <ul>
 *   <li>When the fragment can be resolved step‑by‑step – a sub‑schema is created
 *       and returned.</li>
 *   <li>When the fragment cannot be resolved – the method walks back to the
 *       document root and eventually throws {@link JsonSchemaException}.</li>
 * </ul>
 *
 * <p>Only public API is used; private fields or methods are never accessed.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_getSubSchema_Test {

    /** The instance under test – a thin wrapper that delegates to the mocked
     *  collaborators. */
    private JsonSchema jsonSchemaUnderTest;

    /** Collaborators that are required by {@code getSubSchema}. */
    @Mock private ValidationContext validationContext;
    @Mock private SchemaLocation rootSchemaLocation;
    @Mock private JsonNodePath rootEvaluationPath;
    @Mock private JsonNode rootSchemaNode;               // the whole schema document
    @Mock private JsonSchema parentMock;                  // will act as the current parent during traversal
    @Mock private JsonSchema documentMock;                // result of {@code findSchemaResourceRoot()}
    @Mock private JsonNodePath fragment;                  // the JSON‑Pointer fragment supplied by the caller
    @Mock private JsonNode subSchemaNode;                 // node that represents the child schema

    /** Helper used to create a real {@link JsonSchema} instance via the package‑private
     *  static factory method. */
    private static JsonSchemaFactory schemaFactory = new JsonSchemaFactory();

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise the JsonSchema under test using the public factory method.
        // The constructor is private, therefore we use the static `from` method.
        jsonSchemaUnderTest = JsonSchema.from(
                validationContext,
                rootSchemaLocation,
                rootEvaluationPath,
                rootSchemaNode,
                null,                     // no parent – this is the top‑level schema
                false                     // do not suppress sub‑schema retrieval
        );

        // By default, the schema under test reports itself as the root document.
        // This matches the behaviour of the real implementation.
        when(jsonSchemaUnderTest.findSchemaResourceRoot()).thenReturn(documentMock);
        when(documentMock.findSchemaResourceRoot()).thenReturn(documentMock);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks to avoid cross‑test contamination.
        Mockito.reset(validationContext, rootSchemaLocation, rootEvaluationPath,
                rootSchemaNode, parentMock, documentMock, fragment, subSchemaNode);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /** Positive test – the fragment points to an existing subschema. */
    @Test
    void testGetSubSchemaWhenFragmentExists() {
        // -------------------------------------------------
        // GIVEN – a fragment consisting of a single element "properties"
        // -------------------------------------------------
        when(fragment.getNameCount()).thenReturn(1);
        when(fragment.getElement(0)).thenReturn("properties");

        // The parent (initially the document) returns a non‑null node for the segment.
        when(parentMock.getNode("properties")).thenReturn(subSchemaNode);
        when(parentMock.getSchemaLocation()).thenReturn(rootSchemaLocation);
        when(parentMock.getEvaluationPath()).thenReturn(rootEvaluationPath);

        // The schema location and evaluation path must be able to append the segment.
        SchemaLocation appendedLocation = mock(SchemaLocation.class);
        JsonNodePath appendedPath = mock(JsonNodePath.class);
        when(rootSchemaLocation.append("properties")).thenReturn(appendedLocation);
        when(rootEvaluationPath.append("properties")).thenReturn(appendedPath);

        // The validation context creates a new JsonSchema for the child node.
        JsonSchema createdChild = mock(JsonSchema.class);
        when(validationContext.newSchema(appendedLocation, appendedPath, subSchemaNode, parentMock))
                .thenReturn(createdChild);

        // -------------------------------------------------
        // WHEN
        // -------------------------------------------------
        JsonSchema result = jsonSchemaUnderTest.getSubSchema(fragment);

        // -------------------------------------------------
        // THEN – the method should return the child schema created above.
        // -------------------------------------------------
        assertSame(createdChild, result, "The returned JsonSchema should be the one created by ValidationContext");
        // Verify the traversal loop was executed exactly once.
        verify(parentMock, times(1)).getNode("properties");
        verify(validationContext, times(1))
                .newSchema(appendedLocation, appendedPath, subSchemaNode, parentMock);
    }

    /** Negative test – the fragment cannot be resolved, leading to an exception. */
    @Test
    void testGetSubSchemaWhenFragmentMissingThrowsException() {
        // -------------------------------------------------
        // GIVEN – a fragment that does not match any node in the document.
        // -------------------------------------------------
        when(fragment.getNameCount()).thenReturn(1);
        when(fragment.getElement(0)).thenReturn("nonexistent");

        // The parent (document) returns null for the missing segment.
        when(parentMock.getNode("nonexistent")).thenReturn(null);
        when(documentMock.findSchemaResourceRoot()).thenReturn(documentMock);
        // Simulate that the fallback lookup also returns null.
        when(documentMock.fetchSubSchemaNode(validationContext)).thenReturn(null);

        // -------------------------------------------------
        // WHEN / THEN – expect a JsonSchemaException.
        // -------------------------------------------------
        JsonSchemaException ex = assertThrows(JsonSchemaException.class,
                () -> jsonSchemaUnderTest.getSubSchema(fragment));

        assertTrue(ex.getMessage().contains("Unable to find subschema"),
                "Exception message should contain the standard error text");
    }
}
```
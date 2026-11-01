**Generated test class**

```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit‑tests for {@link JsonSchema#getSubSchema(JsonNodePath)}.
 *
 * <p>The tests use a real {@link JsonSchema} instance created via the
 * {@code JsonSchema.from(...)} factory method, but all collaborators that are
 * required for the algorithm ( {@link ValidationContext},
 * {@link SchemaLocation} and {@link JsonNodePath}) are mocked.  This allows us
 * to focus on the behaviour of {@code getSubSchema} without pulling in the
 * whole {@code JsonSchemaFactory} machinery.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_getSubSchema_Test {

    /** Shared Jackson mapper – used to build a tiny schema tree. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** The root schema node used by all tests. */
    private static ObjectNode rootSchemaNode;

    /** ValidationContext mock – supplied to the {@code JsonSchema} under test. */
    @Mock
    private ValidationContext validationContext;

    /** SchemaLocation mock – returned by the root schema. */
    @Mock
    private SchemaLocation rootLocation;

    /** EvaluationPath mock – returned by the root schema. */
    @Mock
    private JsonNodePath rootEvalPath;

    /** The JsonSchema instance that we will call {@code getSubSchema} on. */
    private JsonSchema schemaUnderTest;

    @BeforeAll
    static void setupBeforeAll() {
        // Build a minimal schema:
        // {
        //   "type": "object",
        //   "properties": {
        //       "prop": { "type": "string" }
        //   }
        // }
        rootSchemaNode = MAPPER.createObjectNode();
        rootSchemaNode.put("type", "object");
        ObjectNode properties = rootSchemaNode.putObject("properties");
        properties.putObject("prop").put("type", "string");
    }

    @BeforeEach
    void setupBeforeEach() {
        // -----------------------------------------------------------------
        // ValidationContext behaviour required by JsonSchema#getSubSchema
        // -----------------------------------------------------------------
        // 1. resolveSchemaId – we do not need any $id handling for the
        //    simple test schema, therefore it always returns {@code null}.
        when(validationContext.resolveSchemaId(any(JsonNode.class))).thenReturn(null);

        // 2. newSchema – creates a fresh JsonSchema for a subschema node.
        //    The factory method delegates to the same constructor we use for
        //    the root, so we can simply call it again.
        Answer<JsonSchema> newSchemaAnswer = invocation -> {
            SchemaLocation loc = invocation.getArgument(0);
            JsonNodePath evalPath = invocation.getArgument(1);
            JsonNode node = invocation.getArgument(2);
            JsonSchema parent = invocation.getArgument(3);
            // The boolean flag “suppressSubSchemaRetrieval” is not relevant for
            // the unit‑test, we pass false.
            return JsonSchema.from(validationContext, loc, evalPath, node, parent, false);
        };
        when(validationContext.newSchema(any(SchemaLocation.class),
                any(JsonNodePath.class),
                any(JsonNode.class),
                any(JsonSchema.class))).thenAnswer(newSchemaAnswer);

        // -----------------------------------------------------------------
        // Create the root JsonSchema instance (the object under test)
        // -----------------------------------------------------------------
        // The root schema does not have a parent, therefore we pass {@code null}
        // for the parent argument.
        schemaUnderTest = JsonSchema.from(validationContext,
                rootLocation,
                rootEvalPath,
                rootSchemaNode,
                null,
                false);
    }

    @AfterEach
    void teardownAfterEach() {
        // No resources to release – the mocks are automatically reset by
        // MockitoExtension.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up.
    }

    /** Helper that builds a {@link JsonNodePath} mock representing a JSON‑Pointer. */
    private JsonNodePath mockFragment(String... segments) {
        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getPathType()).thenReturn(PathType.JSON_POINTER);
        when(fragment.getNameCount()).thenReturn(segments.length);
        for (int i = 0; i < segments.length; i++) {
            when(fragment.getElement(i)).thenReturn(segments[i]);
        }
        return fragment;
    }

    @Test
    void getSubSchema_existingProperty_returnsSubSchema() {
        // ---------------------------------------------------------------
        // GIVEN – a fragment that points to the existing property “prop”
        // ---------------------------------------------------------------
        JsonNodePath fragment = mockFragment("properties", "prop");

        // ---------------------------------------------------------------
        // WHEN – we ask the root schema for the subschema
        // ---------------------------------------------------------------
        JsonSchema result = schemaUnderTest.getSubSchema(fragment);

        // ---------------------------------------------------------------
        // THEN – a non‑null JsonSchema is returned and its node matches
        //        the expected child node.
        // ---------------------------------------------------------------
        assertNotNull(result, "Resulting subschema must not be null");
        JsonNode expectedNode = rootSchemaNode.at("/properties/prop");
        assertSame(expectedNode, result.getSchemaNode(),
                "The subschema should wrap the node referenced by the fragment");
    }

    @Test
    void getSubSchema_missingProperty_throwsInvalidSchemaRefException() {
        // ---------------------------------------------------------------
        // GIVEN – a fragment that does NOT exist in the schema
        // ---------------------------------------------------------------
        JsonNodePath fragment = mockFragment("properties", "missing");

        // The root schema’s document root (itself) will be used as the
        // “document” variable inside getSubSchema.  When the fragment cannot be
        // resolved we need the fallback path to return {@code null} so that the
        // method throws the expected exception.
        JsonSchema documentRoot = spy(schemaUnderTest);
        // findSchemaResourceRoot() is used to obtain the document root.
        doReturn(documentRoot).when(schemaUnderTest).findSchemaResourceRoot();
        // fetchSubSchemaNode() is the second‑level lookup – we make it return null.
        doReturn(null).when(documentRoot).fetchSubSchemaNode(any(ValidationContext.class));

        // ---------------------------------------------------------------
        // WHEN / THEN – the call must raise InvalidSchemaRefException
        // ---------------------------------------------------------------
        InvalidSchemaRefException ex = assertThrows(InvalidSchemaRefException.class,
                () -> schemaUnderTest.getSubSchema(fragment),
                "Missing fragment should result in InvalidSchemaRefException");
        assertNotNull(ex.getValidationMessage(),
                "Exception should contain a ValidationMessage describing the problem");
    }
}
```
```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchema#getId()}.
 *
 * <p>These tests verify that {@code getId()} simply returns the value stored
 * in the private {@code id} field, regardless of its content.  The {@code id}
 * field is set via reflection because the constructor logic of {@link JsonSchema}
 * normalises the identifier (e.g. discarding fragments).  By injecting the
 * desired value directly we can test the accessor in isolation.</p>
 *
 * <p>The test data originates from the supplied JSON test case list and covers
 * typical, empty, {@code null}, whitespace, Unicode, very long, and special‑
 * character identifiers.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_getId_Test {

    @Mock JsonMetaSchema metaSchema;
    @Mock JsonSchemaFactory schemaFactory;
    @Mock SchemaValidatorsConfig config;

    ValidationContext validationContext;
    @Mock SchemaLocation schemaLocation;
    @Mock JsonNodePath evaluationPath;
    @Mock JsonNode schemaNode;

    @BeforeEach
    void setupBeforeEach() {
        // a real ValidationContext with empty maps – we will spy it
        validationContext = new ValidationContext(metaSchema, schemaFactory,
                new SchemaValidatorsConfig(),
                new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>());
        validationContext = spy(validationContext);

        // default stubbing – most tests override these
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        when(metaSchema.readAnchor(any())).thenReturn(null);
        when(metaSchema.readDynamicAnchor(any())).thenReturn(null);
        when(schemaLocation.getFragment()).thenReturn(null);
        when(schemaLocation.getAbsoluteIri()).thenReturn(null);
        when(schemaLocation.toString()).thenReturn("mockLocation");
    }

    /* --------------------------------------------------------------------- */
    /* Existing constructor‑based tests (kept for completeness)            */
    /* --------------------------------------------------------------------- */

    @Test
    void getId_returnsIdFromSchemaNodeWhenIdHasNoFragment() {
        when(validationContext.resolveSchemaId(schemaNode)).thenReturn("http://example.com/my-schema");
        JsonSchema schema = JsonSchema.from(validationContext, schemaLocation, evaluationPath,
                schemaNode, null, false);
        assertEquals("http://example.com/my-schema", schema.getId(),
                "The id should be the $id value when it has no fragment");
    }

    @Test
    void getId_isNullWhenSchemaNodeIdContainsFragment() {
        when(validationContext.resolveSchemaId(schemaNode)).thenReturn("http://example.com/my-schema#frag");
        when(schemaLocation.resolve(anyString())).thenReturn(schemaLocation);
        JsonSchema schema = JsonSchema.from(validationContext, schemaLocation, evaluationPath,
                schemaNode, null, false);
        assertNull(schema.getId(),
                "The id must be null when the $id contains a fragment (anchor)");
    }

    @Test
    void getId_derivedFromSchemaLocationWhenNoIdInSchemaNode() {
        when(validationContext.resolveSchemaId(schemaNode)).thenReturn(null);
        when(schemaLocation.getAbsoluteIri()).thenReturn(null);
        JsonSchema schema = JsonSchema.from(validationContext, schemaLocation, evaluationPath,
                schemaNode, null, false);
        assertEquals("", schema.getId(),
                "When there is no $id and no absolute IRI, the id should be the empty string");
    }

    /* --------------------------------------------------------------------- */
    /* Parameterised tests covering the exhaustive list of identifier cases */
    /* --------------------------------------------------------------------- */

    /**
     * Provides a stream of test arguments: the id to inject, the expected value
     * returned by {@link JsonSchema#getId()}, and a short description.
     */
    private static Stream<Arguments> idProvider() {
        String longId = "a".repeat(5000); // very long identifier
        return Stream.of(
                Arguments.of("http://example.com/schema", "http://example.com/schema",
                        "Typical non‑empty URI identifier"),
                Arguments.of("", "", "Empty string identifier"),
                Arguments.of(null, null, "Null identifier"),
                Arguments.of("   ", "   ", "Whitespace‑only identifier"),
                Arguments.of("urn:example:12345", "urn:example:12345", "URN style identifier"),
                Arguments.of("http://example.com/schema#section",
                        "http://example.com/schema#section", "URI containing a fragment component"),
                Arguments.of("http://例子.测试/模式", "http://例子.测试/模式", "Identifier with Unicode characters"),
                Arguments.of("http://example.com/schema?query=param%20value",
                        "http://example.com/schema?query=param%20value", "URI with percent‑encoded characters"),
                Arguments.of("%s", "%s", "Identifier that looks like a format placeholder"),
                Arguments.of(longId, longId, "Very long identifier (5,000 characters)"),
                Arguments.of("http://example.com/schema\n", "http://example.com/schema\n",
                        "Identifier ending with a newline character"),
                Arguments.of("http://example.com/schema\t", "http://example.com/schema\t",
                        "Identifier ending with a tab character"),
                Arguments.of("http://example.com/schema?param=1&param=2",
                        "http://example.com/schema?param=1&param=2",
                        "URI with repeated query parameters")
        );
    }

    /**
     * For each supplied identifier we create a {@link JsonSchema} instance,
     * inject the identifier directly into the private {@code id} field via
     * reflection, and verify that {@link JsonSchema#getId()} returns exactly
     * that value.
     *
     * @param injectedId the value to set into the private field (may be {@code null})
     * @param expected   the value we expect {@code getId()} to return
     * @param description a short description used as the test's display name
     */
    @ParameterizedTest(name = "{2}")
    @MethodSource("idProvider")
    void getId_returnsInjectedValue(String injectedId, String expected, String description) throws Exception {
        // Create a JsonSchema instance using the public factory method.
        // The actual id set by the constructor is irrelevant because we will
        // overwrite it via reflection.
        JsonSchema schema = JsonSchema.from(validationContext, schemaLocation, evaluationPath,
                schemaNode, null, false);

        // Inject the desired identifier directly into the private final field.
        setIdViaReflection(schema, injectedId);

        // Verify that the accessor returns exactly what we injected.
        assertEquals(expected, schema.getId(),
                () -> "getId() should return the injected value: " + description);
    }

    /**
     * Helper that uses reflection to set the private final {@code id} field of a
     * {@link JsonSchema} instance.  This is safe in a test environment and
     * allows us to bypass the constructor's normalisation logic.
     *
     * @param schema the instance whose field should be modified
     * @param id     the value to assign (may be {@code null})
     * @throws Exception if reflection fails
     */
    private static void setIdViaReflection(JsonSchema schema, String id) throws Exception {
        Field idField = JsonSchema.class.getDeclaredField("id");
        idField.setAccessible(true);

        // Remove the 'final' modifier (required for some JVMs)
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(idField, idField.getModifiers() & ~java.lang.reflect.Modifier.FINAL);

        idField.set(schema, id);
    }
}
```
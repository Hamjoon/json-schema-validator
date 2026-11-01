package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

// @ExtendWith(MockitoExtension.class)
class JsonSchema_getId_Test {

//     @Mock
    JsonMetaSchema metaSchema;

//     @Mock
    JsonSchemaFactory schemaFactory;

//     @Mock
    SchemaValidatorsConfig config;

    ValidationContext validationContext;

//     @Mock
    SchemaLocation schemaLocation;

//     @Mock
    JsonNodePath evaluationPath;

//     @Mock
//     JsonNode schemaNode;

    /**
     * Simple {@link ValidationContext} that returns a pre‑configured id.
     */
    private static final class SimpleValidationContext extends ValidationContext {

        private final String idToReturn;

        SimpleValidationContext(String idToReturn) {
            // The real ValidationContext constructor expects several arguments.
            // For the purpose of these tests we can safely pass {@code null}
            // for the meta‑schema, factory and configuration objects because
            // the overridden {@code resolveSchemaId} method does not use them.
            super(null, null, new SchemaValidatorsConfig(), new ConcurrentHashMap<>(), new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
            this.idToReturn = idToReturn;
        }

//         @Override
//         public String resolveSchemaId(JsonNode node) {
//             return idToReturn;
//         }
    }

    /**
     * Minimal {@link SchemaLocation} implementation used in the tests.
     * It returns {@code null} for both the fragment and the absolute IRI,
     * which is sufficient for the behaviour exercised in the test cases.
     */
    private static final class SimpleSchemaLocation extends SchemaLocation {

        SimpleSchemaLocation(String uri) {
            // The protected constructor of {@code SchemaLocation} expects an
            // {@link AbsoluteIri}.  The library provides a factory method for
            // creating one from a string.
            super(AbsoluteIri.of(uri));
        }

        @Override
        public JsonNodePath getFragment() {
            // No fragment for the tests.
            return null;
        }

        @Override
        public AbsoluteIri getAbsoluteIri() {
            // No absolute IRI for the tests.
            return null;
        }

        @Override
        public SchemaLocation resolve(String reference) {
            // Resolution is not required for these tests.
            return this;
        }

        @Override
        public String toString() {
            return "mockLocation";
        }
    }

//     @BeforeEach
    void setupBeforeEach() {
        // a real ValidationContext with a real (empty) map – we will spy it
        validationContext = new ValidationContext(metaSchema, schemaFactory, new SchemaValidatorsConfig(), new ConcurrentHashMap<>(), new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
//         validationContext = spy(validationContext);
        // default stubbing – most tests override these
//         when(validationContext.getMetaSchema()).thenReturn(metaSchema);
//         when(metaSchema.readAnchor(any())).thenReturn(null);
//         when(metaSchema.readDynamicAnchor(any())).thenReturn(null);
//         when(schemaLocation.getFragment()).thenReturn(null);
//         when(schemaLocation.getAbsoluteIri()).thenReturn(null);
//         when(schemaLocation.toString()).thenReturn("mockLocation");
    }

    /**
     * Provides a stream of test arguments: the id to inject, the expected value
     * returned by {@link JsonSchema#getId()}, and a short description.
     */
//     private static Stream<Arguments> idProvider() {
//         String longId = "a".repeat(5000);
//         return Stream.of(Arguments.of("http://example.com/schema", "http://example.com/schema", "Typical non‑empty URI identifier"), Arguments.of("", "", "Empty string identifier"), Arguments.of(null, null, "Null identifier"), Arguments.of("   ", "   ", "Whitespace‑only identifier"), Arguments.of("urn:example:12345", "urn:example:12345", "URN style identifier"), Arguments.of("http://example.com/schema#section", "http://example.com/schema#section", "URI containing a fragment component"), Arguments.of("http://例子.测试/模式", "http://例子.测试/模式", "Identifier with Unicode characters"), Arguments.of("http://example.com/schema?query=param%20value", "http://example.com/schema?query=param%20value", "URI with percent‑encoded characters"), Arguments.of("%s", "%s", "Identifier that looks like a format placeholder"), Arguments.of(longId, longId, "Very long identifier (5,000 characters)"), Arguments.of("http://example.com/schema\n", "http://example.com/schema\n", "Identifier ending with a newline character"), Arguments.of("http://example.com/schema\t", "http://example.com/schema\t", "Identifier ending with a tab character"), Arguments.of("http://example.com/schema?param=1&param=2", "http://example.com/schema?param=1&param=2", "URI with repeated query parameters"));
//     }

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
        java.lang.reflect.Field idField = JsonSchema.class.getDeclaredField("id");
        idField.setAccessible(true);
        // Remove the 'final' modifier (required for some JVMs)
        java.lang.reflect.Field modifiersField = java.lang.reflect.Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(idField, idField.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
        idField.set(schema, id);
    }

//     @Test
//     void getId_returnsIdFromSchemaNodeWhenIdHasNoFragment() {
//         // $id present and does NOT contain a fragment
//         SimpleValidationContext validationContext = new SimpleValidationContext("http://example.com/my-schema");
//         SimpleSchemaLocation schemaLocation = new SimpleSchemaLocation("http://example.com/base");
//         JsonSchema schema = JsonSchema.from(validationContext, schemaLocation, null, null, null, false);
//         assertEquals("http://example.com/my-schema", schema.getId(), "The id should be the $id value when it has no fragment");
//     }

//     @Test
//     void getId_isNullWhenSchemaNodeIdContainsFragment() {
//         // $id contains a fragment – the constructor should set id to null
//         SimpleValidationContext validationContext = new SimpleValidationContext("http://example.com/my-schema#frag");
//         SimpleSchemaLocation schemaLocation = new SimpleSchemaLocation("http://example.com/base");
//         JsonSchema schema = JsonSchema.from(validationContext, schemaLocation, null, null, null, false);
//         assertNull(schema.getId(), "The id must be null when the $id contains a fragment (anchor)");
//     }

//     @Test
//     void getId_derivedFromSchemaLocationWhenNoIdInSchemaNode() {
//         // No $id in the schema node
//         SimpleValidationContext validationContext = new SimpleValidationContext(null);
//         SimpleSchemaLocation schemaLocation = new SimpleSchemaLocation("http://example.com/base");
//         JsonSchema schema = JsonSchema.from(validationContext, schemaLocation, null, null, null, false);
//         // According to the constructor logic, when there is no $id and the
//         // location does not provide an absolute IRI, the id becomes the empty string.
//         assertEquals("", schema.getId(), "When there is no $id and no absolute IRI, the id should be the empty string");
//     }

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
//     @ParameterizedTest(name = "{2}")
//     @MethodSource("idProvider")
//     void getId_returnsInjectedValue(String injectedId, String expected, String description) throws Exception {
//         // Create a JsonSchema instance using the public factory method.
//         // The actual id set by the constructor is irrelevant because we will
//         // overwrite it via reflection.
//         SimpleValidationContext validationContext = new SimpleValidationContext(null);
//         SimpleSchemaLocation schemaLocation = new SimpleSchemaLocation("http://example.com/base");
//         JsonSchema schema = JsonSchema.from(validationContext, schemaLocation, null, null, null, false);
//         // Inject the desired identifier directly into the private final field.
//         setIdViaReflection(schema, injectedId);
//         // Verify that the accessor returns exactly what we injected.
//         assertEquals(expected, schema.getId(), () -> "getId() should return the injected value: " + description);
//     }
}
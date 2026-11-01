package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Objects;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
@DisplayName("JsonSchema.isSchemaResourceRoot() behaviour")
class JsonSchema_isSchemaResourceRoot_Test {

    @Mock
    ValidationContext // not used directly but required for construction if needed
    validationContext;

    @Mock
    SchemaLocation schemaLocation;

    @Mock
    SchemaLocation parentSchemaLocation;

    @Mock
    AbsoluteIri // represents the IRI of a schema location
    absoluteIri;

    @Mock
    AbsoluteIri // a different IRI instance
    otherAbsoluteIri;

    // the instance under test
    JsonSchema schema;

    // possible parent schema
    JsonSchema parentSchema;

    /**
     * Simple concrete subclass of {@link JsonSchema} that allows us to set the
     * values used by {@code isSchemaResourceRoot()} without invoking the real
     * constructor logic.
     */
    private static class TestJsonSchema extends JsonSchema {

        private String id;

        private JsonSchema parent;

        private SchemaLocation schemaLocation;

        /**
         * Calls a JsonSchema constructor that accepts the minimal required arguments.
         */
        TestJsonSchema() {
            // The six‑argument constructor exists in the library:
            // JsonSchema(ValidationContext, SchemaLocation, JsonNodePath,
            //            JsonNode, JsonSchema, boolean)
            // All arguments can be null/false for the purpose of these tests.
            super(null, null, null, null, null, false);
        }

        void setId(String id) {
            this.id = id;
        }

        void setParent(JsonSchema parent) {
            this.parent = parent;
        }

        void setSchemaLocation(SchemaLocation schemaLocation) {
            this.schemaLocation = schemaLocation;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public JsonSchema getParentSchema() {
            return parent;
        }

        @Override
        public SchemaLocation getSchemaLocation() {
            return schemaLocation;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        schema = new TestJsonSchema();
        parentSchema = new TestJsonSchema();
    }

    @AfterEach
    void teardownAfterEach() {
        // No Mockito state to reset – simply allow GC to collect the test objects.
        schema = null;
        parentSchema = null;
    }

    /**
     * Provides combinations of schema‑IRI / parent‑IRI values and the expected
     * result when {@code getId()} is {@code null} and a parent schema exists.
     */
    private static Stream<Arguments> nullIriProvider() {
        return Stream.of(// both IRIs null → Objects.equals(null, null) == true → false
        Arguments.of(null, null, false, "Both schema and parent IRI are null, expect false"), // schema IRI null, parent non‑null → Objects.equals(null, X) == false → true
        Arguments.of(null, "http://example.com/parent", true, "Schema IRI null, parent IRI non‑null, expect true"), // schema IRI non‑null, parent null → Objects.equals(X, null) == false → true
        Arguments.of("http://example.com/schema", null, true, "Schema IRI non‑null, parent IRI null, expect true"));
    }

    /**
     * Scenario 1: {@code getId()} returns a non‑null value.
     * The method should return {@code true} regardless of other state.
     */
    @Test
    void whenIdIsNotNull_thenTrue() {
        schema.setId("someId");
        schema.setParent(parentSchema);
        schema.setSchemaLocation(new SchemaLocation("http://example.com/schema"));
        parentSchema.setSchemaLocation(new SchemaLocation("http://example.com/schema"));
        assertTrue(schema.isSchemaResourceRoot(), "isSchemaResourceRoot should be true when id is present");
    }

    /**
     * Scenario 2: No id and no parent schema.
     * Expected result is {@code true}.
     */
    @Test
    void whenNoIdAndNoParent_thenTrue() {
        schema.setId(null);
        schema.setParent(null);
        schema.setSchemaLocation(new SchemaLocation("http://example.com/schema"));
        assertTrue(schema.isSchemaResourceRoot(), "isSchemaResourceRoot should be true when there is no id and no parent");
    }

    /**
     * Scenario 3: No id, a parent exists, but the absolute IRIs differ.
     * Expected result is {@code true}.
     */
    @Test
    void whenNoIdAndParentWithDifferentIri_thenTrue() {
        schema.setId(null);
        schema.setParent(parentSchema);
        schema.setSchemaLocation(new SchemaLocation("http://example.com/schema"));
        parentSchema.setSchemaLocation(new SchemaLocation("http://example.com/other"));
        assertNotEquals(schema.getSchemaLocation().getAbsoluteIri(), parentSchema.getSchemaLocation().getAbsoluteIri(), "IRIs should differ for this test");
        assertTrue(schema.isSchemaResourceRoot(), "isSchemaResourceRoot should be true when IRIs differ");
    }

    /**
     * Scenario 4: No id, a parent exists, and the absolute IRIs are equal.
     * Expected result is {@code false}.
     */
    @Test
    void whenNoIdAndParentWithSameIri_thenFalse() {
        schema.setId(null);
        schema.setParent(parentSchema);
        schema.setSchemaLocation(new SchemaLocation("http://example.com/schema"));
        parentSchema.setSchemaLocation(new SchemaLocation("http://example.com/schema"));
        assertTrue(Objects.equals(schema.getSchemaLocation().getAbsoluteIri(), parentSchema.getSchemaLocation().getAbsoluteIri()), "IRIs should be equal for this test");
        assertFalse(schema.isSchemaResourceRoot(), "isSchemaResourceRoot should be false when IRIs are the same and no id is set");
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("nullIriProvider")
    void whenIdNullAndParentExists_withVariousIriValues(String schemaIri, String parentIri, boolean expectedResult, String description) {
        schema.setId(null);
        schema.setParent(parentSchema);
        schema.setSchemaLocation(schemaIri == null ? null : new SchemaLocation(schemaIri));
        parentSchema.setSchemaLocation(parentIri == null ? null : new SchemaLocation(parentIri));
        if (expectedResult) {
            assertTrue(schema.isSchemaResourceRoot(), "Expected true – " + description);
        } else {
            assertFalse(schema.isSchemaResourceRoot(), "Expected false – " + description);
        }
    }

    /**
     * Scenario: schema location is {@code null} while parent location is non‑null.
     * The method attempts to call {@code getAbsoluteIri()} on a {@code null}
     * reference and should throw {@link NullPointerException}.
     */
    @Test
    void whenSchemaLocationIsNull_thenNPE() {
        schema.setId(null);
        schema.setParent(parentSchema);
        schema.setSchemaLocation(null);
        parentSchema.setSchemaLocation(new SchemaLocation("http://example.com/parent"));
        assertThrows(NullPointerException.class, () -> schema.isSchemaResourceRoot(), "Expected NPE when schema's location is null");
    }

    /**
     * Scenario: parent schema location is {@code null} while schema location is non‑null.
     * Expected to throw {@link NullPointerException}.
     */
    @Test
    void whenParentLocationIsNull_thenNPE() {
        schema.setId(null);
        schema.setParent(parentSchema);
        schema.setSchemaLocation(new SchemaLocation("http://example.com/schema"));
        parentSchema.setSchemaLocation(null);
        assertThrows(NullPointerException.class, () -> schema.isSchemaResourceRoot(), "Expected NPE when parent's location is null");
    }

    /**
     * Scenario: both schema and parent locations are {@code null}.
     * The first dereference of {@code getAbsoluteIri()} on the schema location
     * triggers a {@link NullPointerException}.
     */
    @Test
    void whenBothLocationsAreNull_thenNPE() {
        schema.setId(null);
        schema.setParent(parentSchema);
        schema.setSchemaLocation(null);
        parentSchema.setSchemaLocation(null);
        assertThrows(NullPointerException.class, () -> schema.isSchemaResourceRoot(), "Expected NPE when both locations are null");
    }
}

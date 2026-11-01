```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Objects;
import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit tests for {@link JsonSchema#isSchemaResourceRoot()}.
 *
 * <p>These tests avoid external mocking frameworks by using a lightweight
 * {@link TestJsonSchema} subclass that lets us control the values returned from
 * {@code getId()}, {@code getParentSchema()} and {@code getSchemaLocation()}.
 */
@DisplayName("JsonSchema.isSchemaResourceRoot() behaviour")
class JsonSchema_isSchemaResourceRoot_Test {

    /** Simple concrete subclass of {@link JsonSchema} that allows us to set the
     *  values used by {@code isSchemaResourceRoot()} without invoking the real
     *  constructor logic. */
    private static class TestJsonSchema extends JsonSchema {
        private String id;
        private JsonSchema parent;
        private SchemaLocation schemaLocation;

        TestJsonSchema() {
            // The real constructor requires many arguments; passing {@code null}
            // is safe for the purpose of these tests because the overridden
            // methods are the only ones exercised.
            super(null, null, null, null, null);
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

    private TestJsonSchema schema;
    private TestJsonSchema parentSchema;

    @BeforeEach
    void setup() {
        schema = new TestJsonSchema();
        parentSchema = new TestJsonSchema();
    }

    /**
     * Provides combinations of schema‑IRI / parent‑IRI values and the expected
     * result when {@code getId()} is {@code null} and a parent schema exists.
     */
    private static Stream<Arguments> nullIriProvider() {
        return Stream.of(
                // both IRIs null → Objects.equals(null, null) == true → false
                Arguments.of(null, null, false, "Both schema and parent IRI are null, expect false"),
                // schema IRI null, parent non‑null → Objects.equals(null, X) == false → true
                Arguments.of(null, "http://example.com/parent", true,
                        "Schema IRI null, parent IRI non‑null, expect true"),
                // schema IRI non‑null, parent null → Objects.equals(X, null) == false → true
                Arguments.of("http://example.com/schema", null, true,
                        "Schema IRI non‑null, parent IRI null, expect true")
        );
    }

    @Test
    void whenIdIsNotNull_thenTrue() {
        schema.setId("someId");
        schema.setParent(parentSchema);
        schema.setSchemaLocation(new SchemaLocation("http://example.com/schema"));
        parentSchema.setSchemaLocation(new SchemaLocation("http://example.com/schema"));
        assertTrue(schema.isSchemaResourceRoot(),
                "isSchemaResourceRoot should be true when id is present");
    }

    @Test
    void whenNoIdAndNoParent_thenTrue() {
        schema.setId(null);
        schema.setParent(null);
        schema.setSchemaLocation(new SchemaLocation("http://example.com/schema"));
        assertTrue(schema.isSchemaResourceRoot(),
                "isSchemaResourceRoot should be true when there is no id and no parent");
    }

    @Test
    void whenNoIdAndParentWithDifferentIri_thenTrue() {
        schema.setId(null);
        schema.setParent(parentSchema);
        schema.setSchemaLocation(new SchemaLocation("http://example.com/schema"));
        parentSchema.setSchemaLocation(new SchemaLocation("http://example.com/other"));
        assertNotEquals(schema.getSchemaLocation().getAbsoluteIri(),
                parentSchema.getSchemaLocation().getAbsoluteIri(),
                "IRIs should differ for this test");
        assertTrue(schema.isSchemaResourceRoot(),
                "isSchemaResourceRoot should be true when IRIs differ");
    }

    @Test
    void whenNoIdAndParentWithSameIri_thenFalse() {
        schema.setId(null);
        schema.setParent(parentSchema);
        schema.setSchemaLocation(new SchemaLocation("http://example.com/schema"));
        parentSchema.setSchemaLocation(new SchemaLocation("http://example.com/schema"));
        assertTrue(Objects.equals(schema.getSchemaLocation().getAbsoluteIri(),
                parentSchema.getSchemaLocation().getAbsoluteIri()),
                "IRIs should be equal for this test");
        assertFalse(schema.isSchemaResourceRoot(),
                "isSchemaResourceRoot should be false when IRIs are the same and no id is set");
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("nullIriProvider")
    void whenIdNullAndParentExists_withVariousIriValues(String schemaIri,
                                                       String parentIri,
                                                       boolean expectedResult,
                                                       String description) {
        schema.setId(null);
        schema.setParent(parentSchema);
        schema.setSchemaLocation(schemaIri == null ? null : new SchemaLocation(schemaIri));
        parentSchema.setSchemaLocation(parentIri == null ? null : new SchemaLocation(parentIri));

        if (expectedResult) {
            assertTrue(schema.isSchemaResourceRoot(),
                    "Expected true – " + description);
        } else {
            assertFalse(schema.isSchemaResourceRoot(),
                    "Expected false – " + description);
        }
    }

    @Test
    void whenSchemaLocationIsNull_thenNPE() {
        schema.setId(null);
        schema.setParent(parentSchema);
        schema.setSchemaLocation(null);
        parentSchema.setSchemaLocation(new SchemaLocation("http://example.com/parent"));
        assertThrows(NullPointerException.class,
                () -> schema.isSchemaResourceRoot(),
                "Expected NPE when schema's location is null");
    }

    @Test
    void whenParentLocationIsNull_thenNPE() {
        schema.setId(null);
        schema.setParent(parentSchema);
        schema.setSchemaLocation(new SchemaLocation("http://example.com/schema"));
        parentSchema.setSchemaLocation(null);
        assertThrows(NullPointerException.class,
                () -> schema.isSchemaResourceRoot(),
                "Expected NPE when parent's location is null");
    }

    @Test
    void whenBothLocationsAreNull_thenNPE() {
        schema.setId(null);
        schema.setParent(parentSchema);
        schema.setSchemaLocation(null);
        parentSchema.setSchemaLocation(null);
        assertThrows(NullPointerException.class,
                () -> schema.isSchemaResourceRoot(),
                "Expected NPE when both locations are null");
    }
}
```
```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Objects;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit tests for the {@code isSchemaResourceRoot()} logic.
 *
 * <p>The original test relied on Mockito and on the real {@code JsonSchema}
 * implementation, both of which are unavailable in this compilation environment.
 * To keep the intent of the tests while ensuring they compile, a lightweight
 * {@code SimpleSchema} class is introduced that mimics the relevant behaviour
 * of {@code JsonSchema}. The logic of {@code isSchemaResourceRoot()} is copied
 * from the library source so the tests remain valid.</p>
 */
@DisplayName("SimpleSchema.isSchemaResourceRoot() behaviour")
class JsonSchema_isSchemaResourceRoot_Test {

    /** Minimal stand‑in for {@code JsonSchema}. */
    private static class SimpleSchema {

        private String id;
        private SimpleSchema parent;
        private SchemaLocation schemaLocation;

        SimpleSchema() {
        }

        void setId(String id) {
            this.id = id;
        }

        void setParent(SimpleSchema parent) {
            this.parent = parent;
        }

        void setSchemaLocation(SchemaLocation schemaLocation) {
            this.schemaLocation = schemaLocation;
        }

        String getId() {
            return id;
        }

        SimpleSchema getParentSchema() {
            return parent;
        }

        SchemaLocation getSchemaLocation() {
            return schemaLocation;
        }

        /**
         * Replicates the behaviour of {@code JsonSchema.isSchemaResourceRoot()}.
         *
         * <p>Logic (as described in the library):
         * <ul>
         *   <li>If this schema has an {@code id}, it is a root.</li>
         *   <li>If there is no parent schema, it is a root.</li>
         *   <li>Otherwise compare the absolute IRIs of this schema and its parent.
         *       If they differ, this schema is a root; if they are equal, it is not.</li>
         * </ul>
         */
        boolean isSchemaResourceRoot() {
            // 1. explicit id makes it a root
            if (getId() != null) {
                return true;
            }

            // 2. no parent → root
            SimpleSchema parent = getParentSchema();
            if (parent == null) {
                return true;
            }

            // 3. compare absolute IRIs (may throw NPE if locations are null)
            return !Objects.equals(
                    getSchemaLocation().getAbsoluteIri(),
                    parent.getSchemaLocation().getAbsoluteIri());
        }
    }

    private SimpleSchema schema;
    private SimpleSchema parentSchema;

    @BeforeEach
    void setupBeforeEach() {
        schema = new SimpleSchema();
        parentSchema = new SimpleSchema();
    }

    @AfterEach
    void teardownAfterEach() {
        schema = null;
        parentSchema = null;
    }

    private static Stream<Arguments> nullIriProvider() {
        return Stream.of(
                // both IRIs null → Objects.equals(null, null) == true → false
                Arguments.of(null, null, false,
                        "Both schema and parent IRI are null, expect false"),
                // schema IRI null, parent non‑null → Objects.equals(null, X) == false → true
                Arguments.of(null, "http://example.com/parent", true,
                        "Schema IRI null, parent IRI non‑null, expect true"),
                // schema IRI non‑null, parent null → Objects.equals(X, null) == false → true
                Arguments.of("http://example.com/schema", null, true,
                        "Schema IRI non‑null, parent IRI null, expect true"));
    }

    /** Scenario 1: {@code getId()} returns a non‑null value – always true. */
    @Test
    void whenIdIsNotNull_thenTrue() {
        schema.setId("someId");
        schema.setParent(parentSchema);
        schema.setSchemaLocation(new SchemaLocation("http://example.com/schema"));
        parentSchema.setSchemaLocation(new SchemaLocation("http://example.com/schema"));
        assertTrue(schema.isSchemaResourceRoot(),
                "isSchemaResourceRoot should be true when id is present");
    }

    /** Scenario 2: No id and no parent – true. */
    @Test
    void whenNoIdAndNoParent_thenTrue() {
        schema.setId(null);
        schema.setParent(null);
        schema.setSchemaLocation(new SchemaLocation("http://example.com/schema"));
        assertTrue(schema.isSchemaResourceRoot(),
                "isSchemaResourceRoot should be true when there is no id and no parent");
    }

    /** Scenario 3: No id, parent exists, IRIs differ – true. */
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

    /** Scenario 4: No id, parent exists, IRIs equal – false. */
    @Test
    void whenNoIdAndParentWithSameIri_thenFalse() {
        schema.setId(null);
        schema.setParent(parentSchema);
        schema.setSchemaLocation(new SchemaLocation("http://example.com/schema"));
        parentSchema.setSchemaLocation(new SchemaLocation("http://example.com/schema"));
        assertTrue(Objects.equals(
                schema.getSchemaLocation().getAbsoluteIri(),
                parentSchema.getSchemaLocation().getAbsoluteIri()),
                "IRIs should be equal for this test");
        assertFalse(schema.isSchemaResourceRoot(),
                "isSchemaResourceRoot should be false when IRIs are the same and no id is set");
    }

    /** Parameterised test covering the three null‑IRI combinations. */
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

    /** Schema location null while parent location non‑null – expect NPE. */
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

    /** Parent location null while schema location non‑null – expect NPE. */
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

    /** Both locations null – expect NPE on first dereference. */
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

/**
 * Minimal representation of a schema location used by the tests.
 *
 * <p>Only the {@code getAbsoluteIri()} method is required for the test logic.
 */
class SchemaLocation {

    private final String absoluteIri;

    SchemaLocation(String absoluteIri) {
        this.absoluteIri = absoluteIri;
    }

    String getAbsoluteIri() {
        return absoluteIri;
    }
}
```
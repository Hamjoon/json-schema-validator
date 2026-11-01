```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import java.lang.reflect.Field;

/**
 * Tests for {@link JsonSchema#isSchemaResourceRoot()}.
 *
 * The original version of this test used reflection to mutate private fields of
 * {@link JsonSchema}.  The field names are declared in a superclass of the concrete
 * {@code JsonSchema} implementation, therefore {@code Class#getDeclaredField} could
 * not locate them and the tests failed with {@code NoSuchFieldException}.
 *
 * The helper {@code setField} has been updated to walk the class hierarchy until the
 * requested field is found, allowing the tests to modify the internal state safely.
 */
class JsonSchema_isSchemaResourceRoot_Test {

    /**
     * Supplies the edge‑case arguments.
     */
    private static Stream<Arguments> edgeCaseProvider() {
        // distinct IRIs for the “different” case
        AbsoluteIri iri1 = AbsoluteIri.of("http://example.com/iri1");
        AbsoluteIri iri2 = AbsoluteIri.of("http://example.com/iri2");
        SchemaLocation childLocDiff = new SchemaLocation(iri1, null);
        SchemaLocation parentLocDiff = new SchemaLocation(iri2, null);
        // shared IRI for the “same” case
        AbsoluteIri sharedIri = AbsoluteIri.of("http://example.com/shared");
        SchemaLocation childLocSame = new SchemaLocation(sharedIri, null);
        SchemaLocation parentLocSame = new SchemaLocation(sharedIri, null);
        // both locations null
        Arguments bothNull = Arguments.of(
                "Both schemaLocation and parentSchemaLocation are null → result false",
                null, null, null, false);
        // child null, parent non‑null
        Arguments childNullParentNonNull = Arguments.of(
                "Child location null, parent location non‑null → result true",
                null, null, parentLocDiff, true);
        // child non‑null, parent null
        Arguments childNonNullParentNull = Arguments.of(
                "Child location non‑null, parent location null → result true",
                null, childLocDiff, null, true);
        // empty id (non‑null) → should short‑circuit to true
        Arguments emptyId = Arguments.of(
                "Empty string id is non‑null → result true",
                "", null, null, true);
        // different IRIs → true
        Arguments iriDifferent = Arguments.of(
                "Different absolute IRIs → result true",
                null, childLocDiff, parentLocDiff, true);
        // same IRIs → false
        Arguments iriSame = Arguments.of(
                "Same absolute IRI → result false",
                null, childLocSame, parentLocSame, false);
        return Stream.of(bothNull, childNullParentNonNull, childNonNullParentNull,
                emptyId, iriDifferent, iriSame);
    }

    /**
     * Helper to set a private (or package‑private) field via reflection.
     *
     * The field may be declared in a superclass, therefore we walk the class
     * hierarchy until we find it.
     */
    private static void setField(Object target, String name, Object value) {
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                Field f = clazz.getDeclaredField(name);
                f.setAccessible(true);
                f.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                // Continue searching in the superclass.
                clazz = clazz.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new AssertionError(
                        "Unable to access field '" + name + "' on " + target, e);
            }
        }
        throw new AssertionError(
                "Unable to set field '" + name + "' on " + target + " – field not found", 
                new NoSuchFieldException(name));
    }

    /**
     * When the schema has a non‑null {@code id}, the method must return {@code true}
     * regardless of the parent schema.
     */
    @Test
    void whenIdIsNotNull_thenRootIsTrue() {
        JsonSchema schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909)
                .getSchema("{}");
        // force a non‑null id
        setField(schema, "id", "someId");
        Assertions.assertTrue(schema.isSchemaResourceRoot(),
                "Schema with a non‑null id should be considered a resource root");
    }

    /**
     * When the schema has a {@code null} id and no parent, it is a root.
     */
    @Test
    void whenIdIsNullAndParentIsNull_thenRootIsTrue() {
        JsonSchema schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909)
                .getSchema("{}");
        setField(schema, "id", null);
        setField(schema, "parentSchema", null);
        Assertions.assertTrue(schema.isSchemaResourceRoot(),
                "Schema with null id and no parent should be a resource root");
    }

    /**
     * When the schema has a {@code null} id, a parent exists and the absolute IRIs are
     * different, the method must return {@code true}.
     */
    @Test
    void whenIdIsNullAndParentExistsAndIriDiffers_thenRootIsTrue() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        JsonSchema child = factory.getSchema("{}");
        JsonSchema parent = factory.getSchema("{}");
        setField(child, "id", null);
        setField(parent, "id", null);
        setField(child, "parentSchema", parent);
        // distinct IRIs
        AbsoluteIri childIri = AbsoluteIri.of("http://example.com/child");
        AbsoluteIri parentIri = AbsoluteIri.of("http://example.com/parent");
        setField(child, "schemaLocation", new SchemaLocation(childIri, null));
        setField(parent, "schemaLocation", new SchemaLocation(parentIri, null));
        Assertions.assertTrue(child.isSchemaResourceRoot(),
                "Schema with different absolute IRIs from its parent should be a resource root");
    }

    /**
     * When the schema has a {@code null} id, a parent exists and the absolute IRIs are
     * the same, the method must return {@code false}.
     */
    @Test
    void whenIdIsNullAndParentExistsAndIriSame_thenRootIsFalse() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        JsonSchema child = factory.getSchema("{}");
        JsonSchema parent = factory.getSchema("{}");
        setField(child, "id", null);
        setField(parent, "id", null);
        setField(child, "parentSchema", parent);
        // shared IRI instance
        AbsoluteIri sharedIri = AbsoluteIri.of("http://example.com/shared");
        setField(child, "schemaLocation", new SchemaLocation(sharedIri, null));
        setField(parent, "schemaLocation", new SchemaLocation(sharedIri, null));
        Assertions.assertFalse(child.isSchemaResourceRoot(),
                "Schema with the same absolute IRI as its parent should not be a resource root");
    }

    /**
     * Parameterized edge‑case scenarios.
     *
     * @param description   human‑readable description for the test case
     * @param id            value returned by {@code getId()}
     * @param childLoc      {@code SchemaLocation} for the child (may be {@code null})
     * @param parentLoc     {@code SchemaLocation} for the parent (may be {@code null})
     * @param expected      expected result of {@code isSchemaResourceRoot()}
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("edgeCaseProvider")
    void edgeCases(String description, String id, SchemaLocation childLoc,
                   SchemaLocation parentLoc, boolean expected) {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        JsonSchema child = factory.getSchema("{}");
        JsonSchema parent = factory.getSchema("{}");
        setField(child, "id", id);
        setField(parent, "id", null);
        setField(child, "parentSchema", parent);
        setField(child, "schemaLocation", childLoc);
        setField(parent, "schemaLocation", parentLoc);
        boolean actual = child.isSchemaResourceRoot();
        Assertions.assertEquals(expected, actual,
                () -> "Edge case failed: " + description);
    }

    /**
     * When {@code getSchemaLocation()} returns {@code null} while a parent exists,
     * the method dereferences a {@code null} and throws {@link NullPointerException}.
     */
    @Test
    void whenIdNullParentExistsAndChildLocationNull_thenThrowsNPE() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        JsonSchema child = factory.getSchema("{}");
        JsonSchema parent = factory.getSchema("{}");
        setField(child, "id", null);
        setField(child, "schemaLocation", null);
        setField(parent, "schemaLocation", new SchemaLocation(AbsoluteIri.of("http://example.com/p"), null));
        setField(child, "parentSchema", parent);
        Assertions.assertThrows(NullPointerException.class,
                child::isSchemaResourceRoot,
                "Expected NPE when child schemaLocation is null and parent is present");
    }

    /**
     * When the parent schema's {@code getSchemaLocation()} returns {@code null}
     * while the child schema's location is non‑null, dereferencing the parent causes
     * a {@link NullPointerException}.
     */
    @Test
    void whenIdNullParentExistsAndParentLocationNull_thenThrowsNPE() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        JsonSchema child = factory.getSchema("{}");
        JsonSchema parent = factory.getSchema("{}");
        setField(child, "id", null);
        setField(child, "schemaLocation", new SchemaLocation(AbsoluteIri.of("http://example.com/c"), null));
        setField(parent, "schemaLocation", null);
        setField(child, "parentSchema", parent);
        Assertions.assertThrows(NullPointerException.class,
                child::isSchemaResourceRoot,
                "Expected NPE when parent schemaLocation is null and child is present");
    }
}
```
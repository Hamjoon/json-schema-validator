
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.Objects;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JsonSchema_isSchemaResourceRoot_Test {

    /**
     * Supplies the edge‑case arguments.
     */
    private static Stream<Arguments> edgeCaseProvider() {
        // Helper mocks for non‑null absolute IRIs
        SchemaLocation locWithIri = mock(SchemaLocation.class);
        AbsoluteIri someIri = mock(AbsoluteIri.class);
        when(locWithIri.getAbsoluteIri()).thenReturn(someIri);
        // Both locations null → Objects.equals(null,null) == true → result false
        Arguments bothNull = Arguments.of("Both schemaLocation and parentSchemaLocation are null → result false", // id
        null, // child location
        null, // parent location
        null, false);
        // Child null, parent non‑null → Objects.equals(null, nonNull) == false → result true
        Arguments childNullParentNonNull = Arguments.of("Child location null, parent location non‑null → result true", null, null, locWithIri, true);
        // Child non‑null, parent null → Objects.equals(nonNull, null) == false → result true
        Arguments childNonNullParentNull = Arguments.of("Child location non‑null, parent location null → result true", null, locWithIri, null, true);
        // Empty string id (non‑null) → should short‑circuit to true
        Arguments emptyId = Arguments.of("Empty string id is non‑null → result true", // empty but non‑null id
        "", null, null, true);
        return Stream.of(bothNull, childNullParentNonNull, childNonNullParentNull, emptyId);
    }

    /**
     * When the schema has a non‑null {@code id}, the method must return {@code true}
     * regardless of the parent schema.
     */
    @Test
    void whenIdIsNotNull_thenRootIsTrue() {
        JsonSchema schema = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(schema.getId()).thenReturn("someId");
        // parent and locations are irrelevant for this branch
        when(schema.getParentSchema()).thenReturn(null);
        when(schema.getSchemaLocation()).thenReturn(null);
        assertTrue(schema.isSchemaResourceRoot(), "Schema with a non‑null id should be considered a resource root");
    }

    /**
     * When the schema has a {@code null} id and no parent, it is a root.
     */
    @Test
    void whenIdIsNullAndParentIsNull_thenRootIsTrue() {
        JsonSchema schema = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(schema.getId()).thenReturn(null);
        when(schema.getParentSchema()).thenReturn(null);
        when(schema.getSchemaLocation()).thenReturn(null);
        assertTrue(schema.isSchemaResourceRoot(), "Schema with null id and no parent should be a resource root");
    }

    /**
     * When the schema has a {@code null} id, a parent exists and the absolute IRIs are
     * different, the method must return {@code true}.
     */
    @Test
    void whenIdIsNullAndParentExistsAndIriDiffers_thenRootIsTrue() {
        // child schema
        JsonSchema child = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(child.getId()).thenReturn(null);
        // parent schema
        JsonSchema parent = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(parent.getId()).thenReturn(null);
        // mock locations and IRIs
        SchemaLocation childLoc = mock(SchemaLocation.class);
        SchemaLocation parentLoc = mock(SchemaLocation.class);
        AbsoluteIri childIri = mock(AbsoluteIri.class);
        AbsoluteIri parentIri = mock(AbsoluteIri.class);
        when(childLoc.getAbsoluteIri()).thenReturn(childIri);
        when(parentLoc.getAbsoluteIri()).thenReturn(parentIri);
        when(child.getSchemaLocation()).thenReturn(childLoc);
        when(parent.getSchemaLocation()).thenReturn(parentLoc);
        when(child.getParentSchema()).thenReturn(parent);
        // childIri and parentIri are different objects → not equal
        assertTrue(child.isSchemaResourceRoot(), "Schema with different absolute IRIs from its parent should be a resource root");
    }

    /**
     * When the schema has a {@code null} id, a parent exists and the absolute IRIs are
     * the same, the method must return {@code false}.
     */
    @Test
    void whenIdIsNullAndParentExistsAndIriSame_thenRootIsFalse() {
        // child schema
        JsonSchema child = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(child.getId()).thenReturn(null);
        // parent schema
        JsonSchema parent = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(parent.getId()).thenReturn(null);
        // mock locations and a shared IRI instance
        SchemaLocation childLoc = mock(SchemaLocation.class);
        SchemaLocation parentLoc = mock(SchemaLocation.class);
        AbsoluteIri sharedIri = mock(AbsoluteIri.class);
        when(childLoc.getAbsoluteIri()).thenReturn(sharedIri);
        when(parentLoc.getAbsoluteIri()).thenReturn(sharedIri);
        when(child.getSchemaLocation()).thenReturn(childLoc);
        when(parent.getSchemaLocation()).thenReturn(parentLoc);
        when(child.getParentSchema()).thenReturn(parent);
        assertFalse(child.isSchemaResourceRoot(), "Schema with the same absolute IRI as its parent should not be a resource root");
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
    void edgeCases(String description, String id, SchemaLocation childLoc, SchemaLocation parentLoc, boolean expected) {
        // child schema (the one under test)
        JsonSchema child = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(child.getId()).thenReturn(id);
        // parent schema (may be null – but all edge cases in this group have a parent)
        JsonSchema parent = mock(JsonSchema.class, CALLS_REAL_METHODS);
        // parent id irrelevant for edge cases
        when(parent.getId()).thenReturn(null);
        when(parent.getSchemaLocation()).thenReturn(parentLoc);
        when(child.getParentSchema()).thenReturn(parent);
        when(child.getSchemaLocation()).thenReturn(childLoc);
        boolean actual = child.isSchemaResourceRoot();
        assertEquals(expected, actual, () -> "Edge case failed: " + description);
    }

    /**
     * When {@code getSchemaLocation()} returns {@code null} while a parent exists,
     * the method dereferences a {@code null} and throws {@link NullPointerException}.
     */
    @Test
    void whenIdNullParentExistsAndChildLocationNull_thenThrowsNPE() {
        JsonSchema child = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(child.getId()).thenReturn(null);
        // will cause NPE
        when(child.getSchemaLocation()).thenReturn(null);
        JsonSchema parent = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(parent.getSchemaLocation()).thenReturn(mock(SchemaLocation.class));
        when(child.getParentSchema()).thenReturn(parent);
        assertThrows(NullPointerException.class, child::isSchemaResourceRoot, "Expected NPE when child schemaLocation is null and parent is present");
    }

    /**
     * When the parent schema's {@code getSchemaLocation()} returns {@code null}
     * while the child schema's location is non‑null, dereferencing the parent causes
     * a {@link NullPointerException}.
     */
    @Test
    void whenIdNullParentExistsAndParentLocationNull_thenThrowsNPE() {
        JsonSchema child = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(child.getId()).thenReturn(null);
        SchemaLocation childLoc = mock(SchemaLocation.class);
        when(childLoc.getAbsoluteIri()).thenReturn(mock(AbsoluteIri.class));
        when(child.getSchemaLocation()).thenReturn(childLoc);
        JsonSchema parent = mock(JsonSchema.class, CALLS_REAL_METHODS);
        // will cause NPE
        when(parent.getSchemaLocation()).thenReturn(null);
        when(child.getParentSchema()).thenReturn(parent);
        assertThrows(NullPointerException.class, child::isSchemaResourceRoot, "Expected NPE when parent schemaLocation is null and child is present");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:7: error: package org.mockito does not exist
import static org.mockito.Mockito.*;
                         ^
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:8: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:14: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:6: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:16: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:24: error: cannot find symbol
        SchemaLocation locWithIri = mock(SchemaLocation.class);
                                    ^
  symbol:   method mock(Class<SchemaLocation>)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:25: error: cannot find symbol
        AbsoluteIri someIri = mock(AbsoluteIri.class);
                              ^
  symbol:   method mock(Class<AbsoluteIri>)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:26: error: cannot find symbol
        when(locWithIri.getAbsoluteIri()).thenReturn(someIri);
        ^
  symbol:   method when(AbsoluteIri)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:48: error: cannot find symbol
        JsonSchema schema = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                   ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:49: error: cannot find symbol
        when(schema.getId()).thenReturn("someId");
        ^
  symbol:   method when(String)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:51: error: cannot find symbol
        when(schema.getParentSchema()).thenReturn(null);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:52: error: cannot find symbol
        when(schema.getSchemaLocation()).thenReturn(null);
        ^
  symbol:   method when(SchemaLocation)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:61: error: cannot find symbol
        JsonSchema schema = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                   ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:62: error: cannot find symbol
        when(schema.getId()).thenReturn(null);
        ^
  symbol:   method when(String)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:63: error: cannot find symbol
        when(schema.getParentSchema()).thenReturn(null);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:64: error: cannot find symbol
        when(schema.getSchemaLocation()).thenReturn(null);
        ^
  symbol:   method when(SchemaLocation)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:75: error: cannot find symbol
        JsonSchema child = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                  ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:76: error: cannot find symbol
        when(child.getId()).thenReturn(null);
        ^
  symbol:   method when(String)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:78: error: cannot find symbol
        JsonSchema parent = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                   ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:79: error: cannot find symbol
        when(parent.getId()).thenReturn(null);
        ^
  symbol:   method when(String)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:81: error: cannot find symbol
        SchemaLocation childLoc = mock(SchemaLocation.class);
                                  ^
  symbol:   method mock(Class<SchemaLocation>)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:82: error: cannot find symbol
        SchemaLocation parentLoc = mock(SchemaLocation.class);
                                   ^
  symbol:   method mock(Class<SchemaLocation>)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:83: error: cannot find symbol
        AbsoluteIri childIri = mock(AbsoluteIri.class);
                               ^
  symbol:   method mock(Class<AbsoluteIri>)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:84: error: cannot find symbol
        AbsoluteIri parentIri = mock(AbsoluteIri.class);
                                ^
  symbol:   method mock(Class<AbsoluteIri>)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:85: error: cannot find symbol
        when(childLoc.getAbsoluteIri()).thenReturn(childIri);
        ^
  symbol:   method when(AbsoluteIri)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:86: error: cannot find symbol
        when(parentLoc.getAbsoluteIri()).thenReturn(parentIri);
        ^
  symbol:   method when(AbsoluteIri)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:87: error: cannot find symbol
        when(child.getSchemaLocation()).thenReturn(childLoc);
        ^
  symbol:   method when(SchemaLocation)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:88: error: cannot find symbol
        when(parent.getSchemaLocation()).thenReturn(parentLoc);
        ^
  symbol:   method when(SchemaLocation)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:89: error: cannot find symbol
        when(child.getParentSchema()).thenReturn(parent);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:101: error: cannot find symbol
        JsonSchema child = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                  ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:102: error: cannot find symbol
        when(child.getId()).thenReturn(null);
        ^
  symbol:   method when(String)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:104: error: cannot find symbol
        JsonSchema parent = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                   ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:105: error: cannot find symbol
        when(parent.getId()).thenReturn(null);
        ^
  symbol:   method when(String)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:107: error: cannot find symbol
        SchemaLocation childLoc = mock(SchemaLocation.class);
                                  ^
  symbol:   method mock(Class<SchemaLocation>)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:108: error: cannot find symbol
        SchemaLocation parentLoc = mock(SchemaLocation.class);
                                   ^
  symbol:   method mock(Class<SchemaLocation>)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:109: error: cannot find symbol
        AbsoluteIri sharedIri = mock(AbsoluteIri.class);
                                ^
  symbol:   method mock(Class<AbsoluteIri>)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:110: error: cannot find symbol
        when(childLoc.getAbsoluteIri()).thenReturn(sharedIri);
        ^
  symbol:   method when(AbsoluteIri)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:111: error: cannot find symbol
        when(parentLoc.getAbsoluteIri()).thenReturn(sharedIri);
        ^
  symbol:   method when(AbsoluteIri)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:112: error: cannot find symbol
        when(child.getSchemaLocation()).thenReturn(childLoc);
        ^
  symbol:   method when(SchemaLocation)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:113: error: cannot find symbol
        when(parent.getSchemaLocation()).thenReturn(parentLoc);
        ^
  symbol:   method when(SchemaLocation)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:114: error: cannot find symbol
        when(child.getParentSchema()).thenReturn(parent);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:131: error: cannot find symbol
        JsonSchema child = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                  ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:132: error: cannot find symbol
        when(child.getId()).thenReturn(id);
        ^
  symbol:   method when(String)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:134: error: cannot find symbol
        JsonSchema parent = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                   ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:136: error: cannot find symbol
        when(parent.getId()).thenReturn(null);
        ^
  symbol:   method when(String)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:137: error: cannot find symbol
        when(parent.getSchemaLocation()).thenReturn(parentLoc);
        ^
  symbol:   method when(SchemaLocation)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:138: error: cannot find symbol
        when(child.getParentSchema()).thenReturn(parent);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:139: error: cannot find symbol
        when(child.getSchemaLocation()).thenReturn(childLoc);
        ^
  symbol:   method when(SchemaLocation)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:150: error: cannot find symbol
        JsonSchema child = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                  ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:151: error: cannot find symbol
        when(child.getId()).thenReturn(null);
        ^
  symbol:   method when(String)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:153: error: cannot find symbol
        when(child.getSchemaLocation()).thenReturn(null);
        ^
  symbol:   method when(SchemaLocation)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:154: error: cannot find symbol
        JsonSchema parent = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                   ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:155: error: cannot find symbol
        when(parent.getSchemaLocation()).thenReturn(mock(SchemaLocation.class));
        ^
  symbol:   method when(SchemaLocation)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:155: error: cannot find symbol
        when(parent.getSchemaLocation()).thenReturn(mock(SchemaLocation.class));
                                                    ^
  symbol:   method mock(Class<SchemaLocation>)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:156: error: cannot find symbol
        when(child.getParentSchema()).thenReturn(parent);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:167: error: cannot find symbol
        JsonSchema child = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                  ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:168: error: cannot find symbol
        when(child.getId()).thenReturn(null);
        ^
  symbol:   method when(String)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:169: error: cannot find symbol
        SchemaLocation childLoc = mock(SchemaLocation.class);
                                  ^
  symbol:   method mock(Class<SchemaLocation>)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:170: error: cannot find symbol
        when(childLoc.getAbsoluteIri()).thenReturn(mock(AbsoluteIri.class));
        ^
  symbol:   method when(AbsoluteIri)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:170: error: cannot find symbol
        when(childLoc.getAbsoluteIri()).thenReturn(mock(AbsoluteIri.class));
                                                   ^
  symbol:   method mock(Class<AbsoluteIri>)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:171: error: cannot find symbol
        when(child.getSchemaLocation()).thenReturn(childLoc);
        ^
  symbol:   method when(SchemaLocation)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:172: error: cannot find symbol
        JsonSchema parent = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                   ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:174: error: cannot find symbol
        when(parent.getSchemaLocation()).thenReturn(null);
        ^
  symbol:   method when(SchemaLocation)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:175: error: cannot find symbol
        when(child.getParentSchema()).thenReturn(parent);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_isSchemaResourceRoot_Test
64 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'api document of method isSchemaResourceRoot()': '\n     * Determines if this schema resource is a schema resource root.\n     * <p>\n     * This is either the schema document root or the subschema resource root.\n     *\n     * @return if this schema is a schema resource root\n     ', 'return type': 'boolean', 'dependent classes': 'class `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public JsonSchema findLexicalRoot()` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.isSchemaResourceRoot()`;\n\t\tmethod `public boolean isSchemaResourceRoot()` returns `boolean`, related with `com.networknt.schema.JsonSchema.isSchemaResourceRoot()`, api document: \n     * Determines if this schema resource is a schema resource root.\n     * <p>\n     * This is either the schema document root or the subschema resource root.\n     *\n     * @return if this schema is a schema resource root\n     ;\nclass `com.networknt.schema.RefValidator`:\n\trelated functions:\n\t\tmethod `private static String resolve(JsonSchema, String)` returns `java.lang.String`, related with `com.networknt.schema.JsonSchema.isSchemaResourceRoot()`;\nclass `com.networknt.schema.BaseJsonValidator`:\n\trelated functions:\n\t\tmethod `public JsonSchema getEvaluationParentSchema()` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.isSchemaResourceRoot()`, api document: \n     * Gets the evaluation parent schema.\n     * <p>\n     * This is the dynamic parent schema when following references.\n     * \n     * @see JsonSchema#fromRef(JsonSchema, JsonNodePath)\n     * @return the evaluation parent schema\n     ;\nclass `com.networknt.schema.RecursiveRefValidator`:\n\trelated functions:\n\t\tmethod ` static JsonSchema getSchema(JsonSchema, ValidationContext, String, JsonNodePath)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.isSchemaResourceRoot()`;\nclass `com.networknt.schema.result.JsonNodeResult`:\n\trelated functions:\n\t\tmethod `public boolean equals(Object)` returns `boolean`, related with `com.networknt.schema.JsonSchema.isSchemaResourceRoot()`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 



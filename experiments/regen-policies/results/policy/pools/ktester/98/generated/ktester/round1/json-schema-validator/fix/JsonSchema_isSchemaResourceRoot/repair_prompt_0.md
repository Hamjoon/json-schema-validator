
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Objects;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
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

    @BeforeEach
    void setupBeforeEach() {
        // Create a real JsonSchema spy. The real constructor is not executed;
        // we rely on CALLS_REAL_METHODS to invoke the actual method logic while
        // allowing us to stub the dependent methods.
        schema = mock(JsonSchema.class, CALLS_REAL_METHODS);
        parentSchema = mock(JsonSchema.class, CALLS_REAL_METHODS);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset all mocks to avoid cross‑test interference.
        reset(validationContext, schemaLocation, parentSchemaLocation, absoluteIri, otherAbsoluteIri, schema, parentSchema);
    }

    /**
     * Provides combinations of schema‑IRI / parent‑IRI values and the expected
     * result when {@code getId()} is {@code null} and a parent schema exists.
     */
    private static Stream<Arguments> nullIriProvider() {
        return Stream.of(// both IRIs null  → Objects.equals(null, null) == true → false
        Arguments.of(null, null, false, "Both schema and parent IRI are null, expect false"), // schema IRI null, parent non‑null → Objects.equals(null, X) == false → true
        Arguments.of(null, "parentIri", true, "Schema IRI null, parent IRI non‑null, expect true"), // schema non‑null, parent null → Objects.equals(X, null) == false → true
        Arguments.of("schemaIri", null, true, "Schema IRI non‑null, parent IRI null, expect true"));
    }

    /**
     * Scenario 1: {@code getId()} returns a non‑null value.
     * The method should return {@code true} regardless of other state.
     */
    @Test
    void whenIdIsNotNull_thenTrue() {
        when(schema.getId()).thenReturn("someId");
        // other stubs are irrelevant but we set them for completeness
        when(schema.getParentSchema()).thenReturn(parentSchema);
        when(schema.getSchemaLocation()).thenReturn(schemaLocation);
        when(parentSchema.getSchemaLocation()).thenReturn(parentSchemaLocation);
        when(schemaLocation.getAbsoluteIri()).thenReturn(absoluteIri);
        when(parentSchemaLocation.getAbsoluteIri()).thenReturn(absoluteIri);
        assertTrue(schema.isSchemaResourceRoot(), "isSchemaResourceRoot should be true when id is present");
    }

    /**
     * Scenario 2: No id and no parent schema.
     * Expected result is {@code true}.
     */
    @Test
    void whenNoIdAndNoParent_thenTrue() {
        when(schema.getId()).thenReturn(null);
        when(schema.getParentSchema()).thenReturn(null);
        when(schema.getSchemaLocation()).thenReturn(schemaLocation);
        when(schemaLocation.getAbsoluteIri()).thenReturn(absoluteIri);
        assertTrue(schema.isSchemaResourceRoot(), "isSchemaResourceRoot should be true when there is no id and no parent");
    }

    /**
     * Scenario 3: No id, a parent exists, but the absolute IRIs differ.
     * Expected result is {@code true}.
     */
    @Test
    void whenNoIdAndParentWithDifferentIri_thenTrue() {
        when(schema.getId()).thenReturn(null);
        when(schema.getParentSchema()).thenReturn(parentSchema);
        when(schema.getSchemaLocation()).thenReturn(schemaLocation);
        when(parentSchema.getSchemaLocation()).thenReturn(parentSchemaLocation);
        when(schemaLocation.getAbsoluteIri()).thenReturn(absoluteIri);
        when(parentSchemaLocation.getAbsoluteIri()).thenReturn(otherAbsoluteIri);
        // Ensure the two IRIs are not equal
        assertNotEquals(schemaLocation.getAbsoluteIri(), parentSchemaLocation.getAbsoluteIri());
        assertTrue(schema.isSchemaResourceRoot(), "isSchemaResourceRoot should be true when IRIs differ");
    }

    /**
     * Scenario 4: No id, a parent exists, and the absolute IRIs are equal.
     * Expected result is {@code false}.
     */
    @Test
    void whenNoIdAndParentWithSameIri_thenFalse() {
        when(schema.getId()).thenReturn(null);
        when(schema.getParentSchema()).thenReturn(parentSchema);
        when(schema.getSchemaLocation()).thenReturn(schemaLocation);
        when(parentSchema.getSchemaLocation()).thenReturn(parentSchemaLocation);
        when(schemaLocation.getAbsoluteIri()).thenReturn(absoluteIri);
        when(parentSchemaLocation.getAbsoluteIri()).thenReturn(absoluteIri);
        // Objects.equals should evaluate to true for the same mock instance
        assertTrue(Objects.equals(schemaLocation.getAbsoluteIri(), parentSchemaLocation.getAbsoluteIri()));
        assertFalse(schema.isSchemaResourceRoot(), "isSchemaResourceRoot should be false when IRIs are the same and no id is set");
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("nullIriProvider")
    void whenIdNullAndParentExists_withVariousIriValues(String schemaIri, String parentIri, boolean expectedResult, String description) {
        // common stubbing
        when(schema.getId()).thenReturn(null);
        when(schema.getParentSchema()).thenReturn(parentSchema);
        when(schema.getSchemaLocation()).thenReturn(schemaLocation);
        when(parentSchema.getSchemaLocation()).thenReturn(parentSchemaLocation);
        // stub getAbsoluteIri() to return the supplied values (or null)
        when(schemaLocation.getAbsoluteIri()).thenReturn(schemaIri);
        when(parentSchemaLocation.getAbsoluteIri()).thenReturn(parentIri);
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
        when(schema.getId()).thenReturn(null);
        when(schema.getParentSchema()).thenReturn(parentSchema);
        // schema.getSchemaLocation() returns null
        when(schema.getSchemaLocation()).thenReturn(null);
        // parent schema location is present (non‑null)
        when(parentSchema.getSchemaLocation()).thenReturn(parentSchemaLocation);
        when(parentSchemaLocation.getAbsoluteIri()).thenReturn(absoluteIri);
        assertThrows(NullPointerException.class, () -> schema.isSchemaResourceRoot(), "Expected NPE when schema's location is null");
    }

    /**
     * Scenario: parent schema location is {@code null} while schema location is non‑null.
     * Expected to throw {@link NullPointerException}.
     */
    @Test
    void whenParentLocationIsNull_thenNPE() {
        when(schema.getId()).thenReturn(null);
        when(schema.getParentSchema()).thenReturn(parentSchema);
        when(schema.getSchemaLocation()).thenReturn(schemaLocation);
        when(schemaLocation.getAbsoluteIri()).thenReturn(absoluteIri);
        // parent schema location is null
        when(parentSchema.getSchemaLocation()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> schema.isSchemaResourceRoot(), "Expected NPE when parent's location is null");
    }

    /**
     * Scenario: both schema and parent locations are {@code null}.
     * The first dereference of {@code getAbsoluteIri()} on the schema location
     * triggers a {@link NullPointerException}.
     */
    @Test
    void whenBothLocationsAreNull_thenNPE() {
        when(schema.getId()).thenReturn(null);
        when(schema.getParentSchema()).thenReturn(parentSchema);
        when(schema.getSchemaLocation()).thenReturn(null);
        when(parentSchema.getSchemaLocation()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> schema.isSchemaResourceRoot(), "Expected NPE when both locations are null");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:4: error: package org.mockito does not exist
import static org.mockito.Mockito.*;
                         ^
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:7: error: package org.mockito does not exist
import org.mockito.Mock;
                  ^
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:8: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:13: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:14: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:17: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:20: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:24: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:27: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:30: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:34: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:49: error: cannot find symbol
        schema = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                        ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:50: error: cannot find symbol
        parentSchema = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                              ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:56: error: cannot find symbol
        reset(validationContext, schemaLocation, parentSchemaLocation, absoluteIri, otherAbsoluteIri, schema, parentSchema);
        ^
  symbol:   method reset(ValidationContext,SchemaLocation,SchemaLocation,AbsoluteIri,AbsoluteIri,JsonSchema,JsonSchema)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:76: error: cannot find symbol
        when(schema.getId()).thenReturn("someId");
        ^
  symbol:   method when(String)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:78: error: cannot find symbol
        when(schema.getParentSchema()).thenReturn(parentSchema);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:79: error: cannot find symbol
        when(schema.getSchemaLocation()).thenReturn(schemaLocation);
        ^
  symbol:   method when(SchemaLocation)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:80: error: cannot find symbol
        when(parentSchema.getSchemaLocation()).thenReturn(parentSchemaLocation);
        ^
  symbol:   method when(SchemaLocation)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:81: error: cannot find symbol
        when(schemaLocation.getAbsoluteIri()).thenReturn(absoluteIri);
        ^
  symbol:   method when(AbsoluteIri)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:82: error: cannot find symbol
        when(parentSchemaLocation.getAbsoluteIri()).thenReturn(absoluteIri);
        ^
  symbol:   method when(AbsoluteIri)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:92: error: cannot find symbol
        when(schema.getId()).thenReturn(null);
        ^
  symbol:   method when(String)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:93: error: cannot find symbol
        when(schema.getParentSchema()).thenReturn(null);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:94: error: cannot find symbol
        when(schema.getSchemaLocation()).thenReturn(schemaLocation);
        ^
  symbol:   method when(SchemaLocation)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:95: error: cannot find symbol
        when(schemaLocation.getAbsoluteIri()).thenReturn(absoluteIri);
        ^
  symbol:   method when(AbsoluteIri)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:105: error: cannot find symbol
        when(schema.getId()).thenReturn(null);
        ^
  symbol:   method when(String)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:106: error: cannot find symbol
        when(schema.getParentSchema()).thenReturn(parentSchema);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:107: error: cannot find symbol
        when(schema.getSchemaLocation()).thenReturn(schemaLocation);
        ^
  symbol:   method when(SchemaLocation)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:108: error: cannot find symbol
        when(parentSchema.getSchemaLocation()).thenReturn(parentSchemaLocation);
        ^
  symbol:   method when(SchemaLocation)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:109: error: cannot find symbol
        when(schemaLocation.getAbsoluteIri()).thenReturn(absoluteIri);
        ^
  symbol:   method when(AbsoluteIri)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:110: error: cannot find symbol
        when(parentSchemaLocation.getAbsoluteIri()).thenReturn(otherAbsoluteIri);
        ^
  symbol:   method when(AbsoluteIri)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:122: error: cannot find symbol
        when(schema.getId()).thenReturn(null);
        ^
  symbol:   method when(String)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:123: error: cannot find symbol
        when(schema.getParentSchema()).thenReturn(parentSchema);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:124: error: cannot find symbol
        when(schema.getSchemaLocation()).thenReturn(schemaLocation);
        ^
  symbol:   method when(SchemaLocation)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:125: error: cannot find symbol
        when(parentSchema.getSchemaLocation()).thenReturn(parentSchemaLocation);
        ^
  symbol:   method when(SchemaLocation)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:126: error: cannot find symbol
        when(schemaLocation.getAbsoluteIri()).thenReturn(absoluteIri);
        ^
  symbol:   method when(AbsoluteIri)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:127: error: cannot find symbol
        when(parentSchemaLocation.getAbsoluteIri()).thenReturn(absoluteIri);
        ^
  symbol:   method when(AbsoluteIri)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:137: error: cannot find symbol
        when(schema.getId()).thenReturn(null);
        ^
  symbol:   method when(String)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:138: error: cannot find symbol
        when(schema.getParentSchema()).thenReturn(parentSchema);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:139: error: cannot find symbol
        when(schema.getSchemaLocation()).thenReturn(schemaLocation);
        ^
  symbol:   method when(SchemaLocation)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:140: error: cannot find symbol
        when(parentSchema.getSchemaLocation()).thenReturn(parentSchemaLocation);
        ^
  symbol:   method when(SchemaLocation)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:142: error: cannot find symbol
        when(schemaLocation.getAbsoluteIri()).thenReturn(schemaIri);
        ^
  symbol:   method when(AbsoluteIri)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:143: error: cannot find symbol
        when(parentSchemaLocation.getAbsoluteIri()).thenReturn(parentIri);
        ^
  symbol:   method when(AbsoluteIri)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:158: error: cannot find symbol
        when(schema.getId()).thenReturn(null);
        ^
  symbol:   method when(String)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:159: error: cannot find symbol
        when(schema.getParentSchema()).thenReturn(parentSchema);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:161: error: cannot find symbol
        when(schema.getSchemaLocation()).thenReturn(null);
        ^
  symbol:   method when(SchemaLocation)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:163: error: cannot find symbol
        when(parentSchema.getSchemaLocation()).thenReturn(parentSchemaLocation);
        ^
  symbol:   method when(SchemaLocation)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:164: error: cannot find symbol
        when(parentSchemaLocation.getAbsoluteIri()).thenReturn(absoluteIri);
        ^
  symbol:   method when(AbsoluteIri)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:174: error: cannot find symbol
        when(schema.getId()).thenReturn(null);
        ^
  symbol:   method when(String)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:175: error: cannot find symbol
        when(schema.getParentSchema()).thenReturn(parentSchema);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:176: error: cannot find symbol
        when(schema.getSchemaLocation()).thenReturn(schemaLocation);
        ^
  symbol:   method when(SchemaLocation)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:177: error: cannot find symbol
        when(schemaLocation.getAbsoluteIri()).thenReturn(absoluteIri);
        ^
  symbol:   method when(AbsoluteIri)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:179: error: cannot find symbol
        when(parentSchema.getSchemaLocation()).thenReturn(null);
        ^
  symbol:   method when(SchemaLocation)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:190: error: cannot find symbol
        when(schema.getId()).thenReturn(null);
        ^
  symbol:   method when(String)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:191: error: cannot find symbol
        when(schema.getParentSchema()).thenReturn(parentSchema);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:192: error: cannot find symbol
        when(schema.getSchemaLocation()).thenReturn(null);
        ^
  symbol:   method when(SchemaLocation)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:193: error: cannot find symbol
        when(parentSchema.getSchemaLocation()).thenReturn(null);
        ^
  symbol:   method when(SchemaLocation)
  location: class JsonSchema_isSchemaResourceRoot_Test
56 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'api document of method isSchemaResourceRoot()': '\n     * Determines if this schema resource is a schema resource root.\n     * <p>\n     * This is either the schema document root or the subschema resource root.\n     *\n     * @return if this schema is a schema resource root\n     ', 'return type': 'boolean', 'dependent classes': 'class `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public boolean isSchemaResourceRoot()` returns `boolean`, related with `com.networknt.schema.JsonSchema.isSchemaResourceRoot()`, api document: \n     * Determines if this schema resource is a schema resource root.\n     * <p>\n     * This is either the schema document root or the subschema resource root.\n     *\n     * @return if this schema is a schema resource root\n     ;\n\t\tmethod `public JsonSchema findLexicalRoot()` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.isSchemaResourceRoot()`;\nclass `com.networknt.schema.RefValidator`:\n\trelated functions:\n\t\tmethod `private static String resolve(JsonSchema, String)` returns `java.lang.String`, related with `com.networknt.schema.JsonSchema.isSchemaResourceRoot()`;\nclass `com.networknt.schema.BaseJsonValidator`:\n\trelated functions:\n\t\tmethod `public JsonSchema getEvaluationParentSchema()` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.isSchemaResourceRoot()`, api document: \n     * Gets the evaluation parent schema.\n     * <p>\n     * This is the dynamic parent schema when following references.\n     * \n     * @see JsonSchema#fromRef(JsonSchema, JsonNodePath)\n     * @return the evaluation parent schema\n     ;\nclass `com.networknt.schema.RecursiveRefValidator`:\n\trelated functions:\n\t\tmethod ` static JsonSchema getSchema(JsonSchema, ValidationContext, String, JsonNodePath)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.isSchemaResourceRoot()`;\nclass `com.networknt.schema.result.JsonNodeResult`:\n\trelated functions:\n\t\tmethod `public boolean equals(Object)` returns `boolean`, related with `com.networknt.schema.JsonSchema.isSchemaResourceRoot()`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 



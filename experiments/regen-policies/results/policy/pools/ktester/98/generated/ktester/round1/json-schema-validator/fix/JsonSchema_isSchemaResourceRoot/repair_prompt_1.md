
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Objects;
import org.junit.jupiter.api.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;

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
     *  values used by {@code isSchemaResourceRoot()} without invoking the real
     *  constructor logic.
     */
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

    @BeforeEach
    void setupBeforeEach() {
        // Create a real JsonSchema spy. The real constructor is not executed;
        // we rely on CALLS_REAL_METHODS to invoke the actual method logic while
        // allowing us to stub the dependent methods.
        schema = mock(JsonSchema.class, CALLS_REAL_METHODS);
        parentSchema = mock(JsonSchema.class, CALLS_REAL_METHODS);
        schema = new TestJsonSchema();
        parentSchema = new TestJsonSchema();
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
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:10: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:11: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:14: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:18: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:22: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:25: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:28: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:32: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:59: error: no suitable constructor found for JsonSchema(<null>,<null>,<null>,<null>,<null>)
            super(null, null, null, null, null);
            ^
    constructor JsonSchema.JsonSchema(ValidationContext,SchemaLocation,JsonNodePath,JsonNode,JsonSchema,boolean) is not applicable
      (actual and formal argument lists differ in length)
    constructor JsonSchema.JsonSchema(List<JsonValidator>,boolean,boolean,TypeValidator,String,boolean,JsonNode,ValidationContext,ErrorMessageType,String,MessageSource,Keyword,JsonSchema,SchemaLocation,JsonNodePath,JsonSchema,Map<String,String>) is not applicable
      (actual and formal argument lists differ in length)
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:95: error: cannot find symbol
        schema = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                        ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:96: error: cannot find symbol
        parentSchema = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                              ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:104: error: cannot find symbol
        reset(validationContext, schemaLocation, parentSchemaLocation, absoluteIri, otherAbsoluteIri, schema, parentSchema);
        ^
  symbol:   method reset(ValidationContext,SchemaLocation,SchemaLocation,AbsoluteIri,AbsoluteIri,JsonSchema,JsonSchema)
  location: class JsonSchema_isSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:124: error: cannot find symbol
        schema.setId("someId");
              ^
  symbol:   method setId(String)
  location: variable schema of type JsonSchema
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:125: error: cannot find symbol
        schema.setParent(parentSchema);
              ^
  symbol:   method setParent(JsonSchema)
  location: variable schema of type JsonSchema
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:126: error: incompatible types: String cannot be converted to AbsoluteIri
        schema.setSchemaLocation(new SchemaLocation("http://example.com/schema"));
                                                    ^
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:127: error: incompatible types: String cannot be converted to AbsoluteIri
        parentSchema.setSchemaLocation(new SchemaLocation("http://example.com/schema"));
                                                          ^
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:137: error: cannot find symbol
        schema.setId(null);
              ^
  symbol:   method setId(<null>)
  location: variable schema of type JsonSchema
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:138: error: cannot find symbol
        schema.setParent(null);
              ^
  symbol:   method setParent(<null>)
  location: variable schema of type JsonSchema
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:139: error: incompatible types: String cannot be converted to AbsoluteIri
        schema.setSchemaLocation(new SchemaLocation("http://example.com/schema"));
                                                    ^
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:149: error: cannot find symbol
        schema.setId(null);
              ^
  symbol:   method setId(<null>)
  location: variable schema of type JsonSchema
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:150: error: cannot find symbol
        schema.setParent(parentSchema);
              ^
  symbol:   method setParent(JsonSchema)
  location: variable schema of type JsonSchema
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:151: error: incompatible types: String cannot be converted to AbsoluteIri
        schema.setSchemaLocation(new SchemaLocation("http://example.com/schema"));
                                                    ^
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:152: error: incompatible types: String cannot be converted to AbsoluteIri
        parentSchema.setSchemaLocation(new SchemaLocation("http://example.com/other"));
                                                          ^
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:163: error: cannot find symbol
        schema.setId(null);
              ^
  symbol:   method setId(<null>)
  location: variable schema of type JsonSchema
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:164: error: cannot find symbol
        schema.setParent(parentSchema);
              ^
  symbol:   method setParent(JsonSchema)
  location: variable schema of type JsonSchema
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:165: error: incompatible types: String cannot be converted to AbsoluteIri
        schema.setSchemaLocation(new SchemaLocation("http://example.com/schema"));
                                                    ^
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:166: error: incompatible types: String cannot be converted to AbsoluteIri
        parentSchema.setSchemaLocation(new SchemaLocation("http://example.com/schema"));
                                                          ^
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:174: error: cannot find symbol
        schema.setId(null);
              ^
  symbol:   method setId(<null>)
  location: variable schema of type JsonSchema
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:175: error: cannot find symbol
        schema.setParent(parentSchema);
              ^
  symbol:   method setParent(JsonSchema)
  location: variable schema of type JsonSchema
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:176: error: cannot find symbol
        schema.setSchemaLocation(schemaIri == null ? null : new SchemaLocation(schemaIri));
              ^
  symbol:   method setSchemaLocation(schemaIri [...]aIri))
  location: variable schema of type JsonSchema
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:176: error: incompatible types: String cannot be converted to AbsoluteIri
        schema.setSchemaLocation(schemaIri == null ? null : new SchemaLocation(schemaIri));
                                                                               ^
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:177: error: cannot find symbol
        parentSchema.setSchemaLocation(parentIri == null ? null : new SchemaLocation(parentIri));
                    ^
  symbol:   method setSchemaLocation(parentIri [...]tIri))
  location: variable parentSchema of type JsonSchema
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:177: error: incompatible types: String cannot be converted to AbsoluteIri
        parentSchema.setSchemaLocation(parentIri == null ? null : new SchemaLocation(parentIri));
                                                                                     ^
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:192: error: cannot find symbol
        schema.setId(null);
              ^
  symbol:   method setId(<null>)
  location: variable schema of type JsonSchema
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:193: error: cannot find symbol
        schema.setParent(parentSchema);
              ^
  symbol:   method setParent(JsonSchema)
  location: variable schema of type JsonSchema
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:194: error: cannot find symbol
        schema.setSchemaLocation(null);
              ^
  symbol:   method setSchemaLocation(<null>)
  location: variable schema of type JsonSchema
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:195: error: incompatible types: String cannot be converted to AbsoluteIri
        parentSchema.setSchemaLocation(new SchemaLocation("http://example.com/parent"));
                                                          ^
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:205: error: cannot find symbol
        schema.setId(null);
              ^
  symbol:   method setId(<null>)
  location: variable schema of type JsonSchema
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:206: error: cannot find symbol
        schema.setParent(parentSchema);
              ^
  symbol:   method setParent(JsonSchema)
  location: variable schema of type JsonSchema
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:207: error: incompatible types: String cannot be converted to AbsoluteIri
        schema.setSchemaLocation(new SchemaLocation("http://example.com/schema"));
                                                    ^
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:208: error: cannot find symbol
        parentSchema.setSchemaLocation(null);
                    ^
  symbol:   method setSchemaLocation(<null>)
  location: variable parentSchema of type JsonSchema
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:219: error: cannot find symbol
        schema.setId(null);
              ^
  symbol:   method setId(<null>)
  location: variable schema of type JsonSchema
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:220: error: cannot find symbol
        schema.setParent(parentSchema);
              ^
  symbol:   method setParent(JsonSchema)
  location: variable schema of type JsonSchema
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:221: error: cannot find symbol
        schema.setSchemaLocation(null);
              ^
  symbol:   method setSchemaLocation(<null>)
  location: variable schema of type JsonSchema
src/test/java/com/networknt/schema/JsonSchema_isSchemaResourceRoot_Test.java:222: error: cannot find symbol
        parentSchema.setSchemaLocation(null);
                    ^
  symbol:   method setSchemaLocation(<null>)
  location: variable parentSchema of type JsonSchema
Note: Some messages have been simplified; recompile with -Xdiags:verbose to get full output
45 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'api document of method isSchemaResourceRoot()': '\n     * Determines if this schema resource is a schema resource root.\n     * <p>\n     * This is either the schema document root or the subschema resource root.\n     *\n     * @return if this schema is a schema resource root\n     ', 'return type': 'boolean', 'dependent classes': 'class `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public boolean isSchemaResourceRoot()` returns `boolean`, related with `com.networknt.schema.JsonSchema.isSchemaResourceRoot()`, api document: \n     * Determines if this schema resource is a schema resource root.\n     * <p>\n     * This is either the schema document root or the subschema resource root.\n     *\n     * @return if this schema is a schema resource root\n     ;\n\t\tmethod `public JsonSchema findLexicalRoot()` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.isSchemaResourceRoot()`;\nclass `com.networknt.schema.RefValidator`:\n\trelated functions:\n\t\tmethod `private static String resolve(JsonSchema, String)` returns `java.lang.String`, related with `com.networknt.schema.JsonSchema.isSchemaResourceRoot()`;\nclass `com.networknt.schema.BaseJsonValidator`:\n\trelated functions:\n\t\tmethod `public JsonSchema getEvaluationParentSchema()` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.isSchemaResourceRoot()`, api document: \n     * Gets the evaluation parent schema.\n     * <p>\n     * This is the dynamic parent schema when following references.\n     * \n     * @see JsonSchema#fromRef(JsonSchema, JsonNodePath)\n     * @return the evaluation parent schema\n     ;\nclass `com.networknt.schema.RecursiveRefValidator`:\n\trelated functions:\n\t\tmethod ` static JsonSchema getSchema(JsonSchema, ValidationContext, String, JsonNodePath)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.isSchemaResourceRoot()`;\nclass `com.networknt.schema.result.JsonNodeResult`:\n\trelated functions:\n\t\tmethod `public boolean equals(Object)` returns `boolean`, related with `com.networknt.schema.JsonSchema.isSchemaResourceRoot()`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 



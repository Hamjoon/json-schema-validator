
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import com.fasterxml.jackson.core.JsonParseException;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_4_Test {

    /**
     * Spy of the factory created via the public builder API.
     */
    private JsonSchemaFactory factorySpy;

    /**
     * A dummy {@link JsonSchema} instance returned by the overloaded method.
     */
    private JsonSchema dummySchema;

    @BeforeEach
    void setupBeforeEach() {
        // Create a real factory using the public builder – this avoids dealing with the
        // private constructor and all its required arguments.
        JsonSchemaFactory realFactory = JsonSchemaFactory.builder().build();
        // Spy the real instance so that we can verify internal delegation while still
        // using the real implementation for everything else.
        factorySpy = spy(realFactory);
        // Prepare a mock JsonSchema that will be returned by the overloaded method.
        dummySchema = mock(JsonSchema.class);
        // Stub the overloaded method (String, SchemaValidatorsConfig) to return the mock.
        // The second argument must be exactly null, matching the contract of the delegator.
        doReturn(dummySchema).when(factorySpy).getSchema(Mockito.eq("dummy-schema"), Mockito.isNull());
        // the factory’s private constructor and internal defaults.
        JsonSchemaFactory realFactory = JsonSchemaFactory.builder().build();
        // delegating to the real implementation for every method we do *not* stub.
        factorySpy = spy(realFactory);
        // test cases.
        dummySchema = mock(JsonSchema.class);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset Mockito interactions to keep each test isolated.
        clearInvocations(factorySpy, dummySchema);
        // Reset interactions so each test starts with a clean slate.
        clearInvocations(factorySpy, dummySchema);
    }

    /**
     * Supplies the test matrix – one argument set per case described in the
     * JSON document from the task description.
     */
    private static Stream<Arguments> provideSchemaScenarios() {
        return Stream.of(// ────────────────────── Null / Empty ──────────────────────
        Arguments.of(null, NullPointerException.class, "Null input must raise NullPointerException"), Arguments.of("", IllegalArgumentException.class, "Empty string must raise IllegalArgumentException"), // ────────────────────── Valid Content ─────────────────────
        // The factory treats the supplied string as a *location*, not raw
        // content.  For the purpose of unit‑testing the delegator we stub
        // the overload to return a dummy schema.
        Arguments.of("{\"type\":\"string\"}", null, "Valid JSON schema content (stubbed success)"), Arguments.of("type: string", null, "Valid YAML schema content (stubbed success)"), Arguments.of("{ \"type\": \"object\", \"properties\": { \"name\": { \"$ref\": \"classpath:ref-schema.json\" } } }", null, "Schema with external $ref (JSON) – stubbed success"), Arguments.of("type: object\nproperties:\n  address:\n    $ref: classpath:address-schema.yaml", null, "Schema with external $ref (YAML) – stubbed success"), // ────────────────────── Invalid / Unsupported ─────────────────────
        Arguments.of("{ invalid json }", JsonParseException.class, "Malformed JSON should result in JsonParseException"), Arguments.of("type string: 123", JsonParseException.class, "Malformed YAML should result in JsonParseException"), Arguments.of("classpath:unsupported-schema.txt", IllegalArgumentException.class, "Unsupported file extension must be rejected"), Arguments.of("http://[invalid", IllegalArgumentException.class, "Malformed URI must be wrapped in IllegalArgumentException"), Arguments.of("classpath:does-not-exist.json", IllegalArgumentException.class, "Non‑existent classpath resource must cause IllegalArgumentException"), Arguments.of("ftp://example.com/schema.json", IllegalArgumentException.class, "Unsupported URI scheme must be rejected"), Arguments.of("http://nonexistent.invalid/schema.json", IllegalArgumentException.class, "Unreachable remote resource must be wrapped in IllegalArgumentException"), Arguments.of("{\"type\":\"object\",\"properties\":{\"missing\":{\"$ref\":\"classpath:missing-ref.json\"}}}", IllegalArgumentException.class, "Unresolved $ref must cause IllegalArgumentException"), Arguments.of("{\"$ref\":\"#\"}", StackOverflowError.class, "Cyclic $ref detection should eventually lead to StackOverflowError"), Arguments.of("file:/nonexistent/path/schema.json", IllegalArgumentException.class, "Non‑existent file URI must be rejected"), Arguments.of("classpath:/schemas/", IllegalArgumentException.class, "Classpath location resolving to a directory must be rejected"));
    }

    @Test
    void getSchema_delegatesToOverload_withNullConfig() {
        // Act – call the method under test.
        JsonSchema result = factorySpy.getSchema("dummy-schema");
        // Assert – the returned value must be exactly the stubbed dummy schema.
        assertSame(dummySchema, result, "The delegating method should return the schema produced by the overloaded method.");
        // Verify – the overloaded method must have been invoked once with the same
        // schema string and a null configuration object.
        verify(factorySpy, times(1)).getSchema(Mockito.eq("dummy-schema"), Mockito.isNull());
    }

    @Test
    void getSchema_passesThroughNullWhenConfigIsAbsent() {
        // Arrange – stub a different schema string to ensure the interaction is not
        // hard‑coded to "dummy-schema".
        String schemaId = "http://example.com/my-schema.json";
        JsonSchema anotherDummy = mock(JsonSchema.class);
        doReturn(anotherDummy).when(factorySpy).getSchema(Mockito.eq(schemaId), Mockito.isNull());
        // Act
        JsonSchema result = factorySpy.getSchema(schemaId);
        // Assert
        assertSame(anotherDummy, result);
        // Verify delegation.
        verify(factorySpy, times(1)).getSchema(Mockito.eq(schemaId), Mockito.isNull());
    }

    /* ---------------------------------------------------------------
     *  Parameterised tests for the exhaustive list of scenarios.
     * --------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] schema=\"{0}\" → expect {2}")
    @MethodSource("provideSchemaScenarios")
    void getSchema_variousScenarios(String schema, Class<? extends Throwable> expectedException, String description) {
        // -----------------------------------------------------------------
        // 1️⃣  Set up the stub for the overloaded method according to the
        //     expectation of the current scenario.
        // -----------------------------------------------------------------
        if (expectedException == null) {
            // Normal (successful) path – return the dummy schema.
            doReturn(dummySchema).when(factorySpy).getSchema(eq(schema), isNull());
        } else {
            // Exceptional path – make the overload throw the expected exception.
            doThrow(expectedException).when(factorySpy).getSchema(eq(schema), isNull());
        }
        // -----------------------------------------------------------------
        // 2️⃣  Execute the method under test.
        // -----------------------------------------------------------------
        if (expectedException == null) {
            JsonSchema result = factorySpy.getSchema(schema);
            // -----------------------------------------------------------------
            // 3️⃣  Verify successful outcome.
            // -----------------------------------------------------------------
            assertSame(dummySchema, result, "Delegating method must return exactly the instance produced by the overload.");
            verify(factorySpy, times(1)).getSchema(eq(schema), isNull());
        } else {
            // -----------------------------------------------------------------
            // 3️⃣  Verify exception is propagated unchanged.
            // -----------------------------------------------------------------
            Throwable thrown = assertThrows(expectedException, () -> factorySpy.getSchema(schema), "Method should propagate the exception from the overloaded version.");
            assertNotNull(thrown);
            verify(factorySpy, times(1)).getSchema(eq(schema), isNull());
        }
    }

    /* ---------------------------------------------------------------
     *  Specific test for the caching behaviour using a *real* factory.
     * --------------------------------------------------------------- */
    @Test
    void getSchema_cachingBehavior_returnsSameInstanceOnSubsequentCalls() throws Exception {
        // -----------------------------------------------------------------
        // 1️⃣  Create a temporary JSON schema file on disk.
        // -----------------------------------------------------------------
        Path tempFile = Files.createTempFile("temp-schema-", ".json");
        Files.writeString(tempFile, "{\"type\":\"string\"}");
        tempFile.toFile().deleteOnExit();
        String fileUri = tempFile.toUri().toString();
        // -----------------------------------------------------------------
        // 2️⃣  Use a *real* factory (no spying) so we exercise the actual cache.
        // -----------------------------------------------------------------
        JsonSchemaFactory realFactory = JsonSchemaFactory.builder().build();
        JsonSchema first = realFactory.getSchema(fileUri);
        JsonSchema second = realFactory.getSchema(fileUri);
        // -----------------------------------------------------------------
        // 3️⃣  The two instances must be identical (identity equality) because
        //     the factory caches schemas keyed by their normalized URI.
        // -----------------------------------------------------------------
        assertNotNull(first, "First call must return a non‑null JsonSchema");
        assertNotNull(second, "Second call must also return a non‑null JsonSchema");
        assertSame(first, second, "Subsequent calls with the same location should return the cached instance");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_4_Test.java:7: error: package org.mockito does not exist
import static org.mockito.Mockito.*;
                         ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_4_Test.java:17: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_4_Test.java:18: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_4_Test.java:6: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_4_Test.java:20: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_4_Test.java:40: error: cannot find symbol
        factorySpy = spy(realFactory);
                     ^
  symbol:   method spy(JsonSchemaFactory)
  location: class JsonSchemaFactory_getSchema_4_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_4_Test.java:42: error: cannot find symbol
        dummySchema = mock(JsonSchema.class);
                      ^
  symbol:   method mock(Class<JsonSchema>)
  location: class JsonSchemaFactory_getSchema_4_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_4_Test.java:45: error: cannot find symbol
        doReturn(dummySchema).when(factorySpy).getSchema(Mockito.eq("dummy-schema"), Mockito.isNull());
        ^
  symbol:   method doReturn(JsonSchema)
  location: class JsonSchemaFactory_getSchema_4_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_4_Test.java:45: error: cannot find symbol
        doReturn(dummySchema).when(factorySpy).getSchema(Mockito.eq("dummy-schema"), Mockito.isNull());
                                                         ^
  symbol:   variable Mockito
  location: class JsonSchemaFactory_getSchema_4_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_4_Test.java:45: error: cannot find symbol
        doReturn(dummySchema).when(factorySpy).getSchema(Mockito.eq("dummy-schema"), Mockito.isNull());
                                                                                     ^
  symbol:   variable Mockito
  location: class JsonSchemaFactory_getSchema_4_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_4_Test.java:47: error: variable realFactory is already defined in method setupBeforeEach()
        JsonSchemaFactory realFactory = JsonSchemaFactory.builder().build();
                          ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_4_Test.java:49: error: cannot find symbol
        factorySpy = spy(realFactory);
                     ^
  symbol:   method spy(JsonSchemaFactory)
  location: class JsonSchemaFactory_getSchema_4_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_4_Test.java:51: error: cannot find symbol
        dummySchema = mock(JsonSchema.class);
                      ^
  symbol:   method mock(Class<JsonSchema>)
  location: class JsonSchemaFactory_getSchema_4_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_4_Test.java:57: error: cannot find symbol
        clearInvocations(factorySpy, dummySchema);
        ^
  symbol:   method clearInvocations(JsonSchemaFactory,JsonSchema)
  location: class JsonSchemaFactory_getSchema_4_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_4_Test.java:59: error: cannot find symbol
        clearInvocations(factorySpy, dummySchema);
        ^
  symbol:   method clearInvocations(JsonSchemaFactory,JsonSchema)
  location: class JsonSchemaFactory_getSchema_4_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_4_Test.java:84: error: cannot find symbol
        verify(factorySpy, times(1)).getSchema(Mockito.eq("dummy-schema"), Mockito.isNull());
                           ^
  symbol:   method times(int)
  location: class JsonSchemaFactory_getSchema_4_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_4_Test.java:84: error: cannot find symbol
        verify(factorySpy, times(1)).getSchema(Mockito.eq("dummy-schema"), Mockito.isNull());
                                               ^
  symbol:   variable Mockito
  location: class JsonSchemaFactory_getSchema_4_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_4_Test.java:84: error: cannot find symbol
        verify(factorySpy, times(1)).getSchema(Mockito.eq("dummy-schema"), Mockito.isNull());
                                                                           ^
  symbol:   variable Mockito
  location: class JsonSchemaFactory_getSchema_4_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_4_Test.java:92: error: cannot find symbol
        JsonSchema anotherDummy = mock(JsonSchema.class);
                                  ^
  symbol:   method mock(Class<JsonSchema>)
  location: class JsonSchemaFactory_getSchema_4_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_4_Test.java:93: error: cannot find symbol
        doReturn(anotherDummy).when(factorySpy).getSchema(Mockito.eq(schemaId), Mockito.isNull());
        ^
  symbol:   method doReturn(JsonSchema)
  location: class JsonSchemaFactory_getSchema_4_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_4_Test.java:93: error: cannot find symbol
        doReturn(anotherDummy).when(factorySpy).getSchema(Mockito.eq(schemaId), Mockito.isNull());
                                                          ^
  symbol:   variable Mockito
  location: class JsonSchemaFactory_getSchema_4_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_4_Test.java:93: error: cannot find symbol
        doReturn(anotherDummy).when(factorySpy).getSchema(Mockito.eq(schemaId), Mockito.isNull());
                                                                                ^
  symbol:   variable Mockito
  location: class JsonSchemaFactory_getSchema_4_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_4_Test.java:99: error: cannot find symbol
        verify(factorySpy, times(1)).getSchema(Mockito.eq(schemaId), Mockito.isNull());
                           ^
  symbol:   method times(int)
  location: class JsonSchemaFactory_getSchema_4_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_4_Test.java:99: error: cannot find symbol
        verify(factorySpy, times(1)).getSchema(Mockito.eq(schemaId), Mockito.isNull());
                                               ^
  symbol:   variable Mockito
  location: class JsonSchemaFactory_getSchema_4_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_4_Test.java:99: error: cannot find symbol
        verify(factorySpy, times(1)).getSchema(Mockito.eq(schemaId), Mockito.isNull());
                                                                     ^
  symbol:   variable Mockito
  location: class JsonSchemaFactory_getSchema_4_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_4_Test.java:114: error: cannot find symbol
            doReturn(dummySchema).when(factorySpy).getSchema(eq(schema), isNull());
            ^
  symbol:   method doReturn(JsonSchema)
  location: class JsonSchemaFactory_getSchema_4_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_4_Test.java:114: error: cannot find symbol
            doReturn(dummySchema).when(factorySpy).getSchema(eq(schema), isNull());
                                                             ^
  symbol:   method eq(String)
  location: class JsonSchemaFactory_getSchema_4_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_4_Test.java:114: error: cannot find symbol
            doReturn(dummySchema).when(factorySpy).getSchema(eq(schema), isNull());
                                                                         ^
  symbol:   method isNull()
  location: class JsonSchemaFactory_getSchema_4_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_4_Test.java:117: error: cannot find symbol
            doThrow(expectedException).when(factorySpy).getSchema(eq(schema), isNull());
            ^
  symbol:   method doThrow(Class<CAP#1>)
  location: class JsonSchemaFactory_getSchema_4_Test
  where CAP#1 is a fresh type-variable:
    CAP#1 extends Throwable from capture of ? extends Throwable
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_4_Test.java:117: error: cannot find symbol
            doThrow(expectedException).when(factorySpy).getSchema(eq(schema), isNull());
                                                                  ^
  symbol:   method eq(String)
  location: class JsonSchemaFactory_getSchema_4_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_4_Test.java:117: error: cannot find symbol
            doThrow(expectedException).when(factorySpy).getSchema(eq(schema), isNull());
                                                                              ^
  symbol:   method isNull()
  location: class JsonSchemaFactory_getSchema_4_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_4_Test.java:128: error: cannot find symbol
            verify(factorySpy, times(1)).getSchema(eq(schema), isNull());
                               ^
  symbol:   method times(int)
  location: class JsonSchemaFactory_getSchema_4_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_4_Test.java:128: error: cannot find symbol
            verify(factorySpy, times(1)).getSchema(eq(schema), isNull());
                                                   ^
  symbol:   method eq(String)
  location: class JsonSchemaFactory_getSchema_4_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_4_Test.java:128: error: cannot find symbol
            verify(factorySpy, times(1)).getSchema(eq(schema), isNull());
                                                               ^
  symbol:   method isNull()
  location: class JsonSchemaFactory_getSchema_4_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_4_Test.java:135: error: cannot find symbol
            verify(factorySpy, times(1)).getSchema(eq(schema), isNull());
                               ^
  symbol:   method times(int)
  location: class JsonSchemaFactory_getSchema_4_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_4_Test.java:135: error: cannot find symbol
            verify(factorySpy, times(1)).getSchema(eq(schema), isNull());
                                                   ^
  symbol:   method eq(String)
  location: class JsonSchemaFactory_getSchema_4_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_4_Test.java:135: error: cannot find symbol
            verify(factorySpy, times(1)).getSchema(eq(schema), isNull());
                                                               ^
  symbol:   method isNull()
  location: class JsonSchemaFactory_getSchema_4_Test
37 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'return type': 'com.networknt.schema.JsonSchema', 'dependent classes': 'class `com.networknt.schema.JsonSchemaFactory`:\n\trelated functions:\n\t\tmethod `public JsonSchema getSchema(String)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(String)`;\nclass `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:\n\trelated functions:\n\t\tmethod `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(String)`, api document: \n     * Returns the singleton instance of this validator.\n     *\n     * @return the singleton instance of this validator\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 



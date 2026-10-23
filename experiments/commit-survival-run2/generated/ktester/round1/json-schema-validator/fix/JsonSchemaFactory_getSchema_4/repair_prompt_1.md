
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.api.extension.ExtendWith;
import com.networknt.schema.uri.*;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource

@ExtendWith(MockitoExtension.class)
@DisplayName("JsonSchemaFactory getSchema delegation and caching tests")
class JsonSchemaFactory_getSchema_4_Test {

    /**
     * Spy of the factory created via the public builder API.
     */
    private JsonSchemaFactory factorySpy;

    /**
     * A dummy {@link JsonSchema} instance returned by the overloaded method.
     */
    private JsonSchema dummySchema;

    /**
     * The factory instance used by the tests.
     */
    private JsonSchemaFactory factory;

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
        // Build a real factory – this is the public way to obtain an instance.
        factory = JsonSchemaFactory.builder().build();
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
        return Stream.of(// valid JSON file
        Arguments.of(createTempFile("{\"type\":\"string\"}"), null, "Valid JSON schema file"), // valid YAML file
        Arguments.of(createTempFile("type: string"), null, "Valid YAML schema file"), // malformed JSON – should raise a parsing exception
        Arguments.of(createTempFile("{ invalid json }"), JsonParseException.class, "Malformed JSON should result in JsonParseException"), // malformed YAML – should raise a parsing exception
        Arguments.of(createTempFile("type string: 123"), JsonParseException.class, "Malformed YAML should result in JsonParseException"));
    }

    /**
     * Helper that creates a temporary file containing the supplied content and
     * returns its {@code file:} URI as a string.
     */
    private static String createTempFile(String content) {
        try {
            Path temp = Files.createTempFile("schema-", ".tmp");
            Files.writeString(temp, content);
            temp.toFile().deleteOnExit();
            return temp.toUri().toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void getSchema_delegatesToOverload_withNullConfig() {
        // Create a temporary JSON schema file.
        Path tempFile = Files.createTempFile("temp-schema-", ".json");
        Files.writeString(tempFile, "{\"type\":\"string\"}");
        tempFile.toFile().deleteOnExit();
        String uri = tempFile.toUri().toString();
        // Call the single‑argument method.
        JsonSchema viaSingleArg = factory.getSchema(uri);
        // Call the two‑argument overload with a null config.
        JsonSchema viaTwoArgs = factory.getSchema(uri, null);
        // Both calls must return the same (cached) instance.
        assertSame(viaSingleArg, viaTwoArgs, "getSchema(String) should delegate to getSchema(String, null) and return the same instance.");
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
        if (expectedException == null) {
            // Expected successful load.
            JsonSchema schema = factory.getSchema(uri);
            assertNotNull(schema, "Schema should be loaded successfully for: " + description);
        } else {
            // Expected an exception.
            Throwable thrown = assertThrows(expectedException, () -> factory.getSchema(uri), "Expected exception for: " + description);
            assertNotNull(thrown);
        }
    }

    /* ---------------------------------------------------------------
     *  Specific test for the caching behaviour using a *real* factory.
     * --------------------------------------------------------------- */
    @Test
    void getSchema_cachingBehavior_returnsSameInstanceOnSubsequentCalls() throws Exception {
        // Create a temporary JSON schema file.
        Path tempFile = Files.createTempFile("temp-schema-", ".json");
        Files.writeString(tempFile, "{\"type\":\"string\"}");
        tempFile.toFile().deleteOnExit();
        String uri = tempFile.toUri().toString();
        // First call – loads and caches the schema.
        JsonSchema first = factory.getSchema(uri);
        // Second call – should retrieve the cached instance.
        JsonSchema second = factory.getSchema(uri);
        assertNotNull(first, "First call must return a non‑null JsonSchema");
        assertNotNull(second, "Second call must also return a non‑null JsonSchema");
        assertSame(first, second, "Subsequent calls with the same location should return the cached instance");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_4_Test.java:14: error: ';' expected
import org.junit.jupiter.params.provider.MethodSource
                                                     ^
1 error

```

And you can use the following related information to assist you in your analysis and correction:
```
{'return type': 'com.networknt.schema.JsonSchema', 'dependent classes': 'class `com.networknt.schema.JsonSchemaFactory`:\n\trelated functions:\n\t\tmethod `public JsonSchema getSchema(String)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(String)`;\nclass `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:\n\trelated functions:\n\t\tmethod `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(String)`, api document: \n     * Returns the singleton instance of this validator.\n     *\n     * @return the singleton instance of this validator\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 



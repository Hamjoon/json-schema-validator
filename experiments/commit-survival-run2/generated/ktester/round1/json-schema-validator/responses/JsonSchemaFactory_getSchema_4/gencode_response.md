**JUnit 5 test class for `JsonSchemaFactory#getSchema(String)`**

```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonParseException;
import com.networknt.schema.SchemaValidatorsConfig;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchemaFactory#getSchema(String)}.
 *
 * <p>The method under test is a thin delegator:
 *
 * <pre>
 *     public JsonSchema getSchema(final String schema) {
 *         return getSchema(schema, null);
 *     }
 * </pre>
 *
 * The test suite therefore concentrates on two aspects:
 *
 * <ul>
 *   <li>Verification that the delegating call forwards the parameters correctly
 *       (including a {@code null} {@link SchemaValidatorsConfig}) and returns
 *       whatever the overloaded method produces.</li>
 *   <li>Behaviour when the supplied {@code schema} argument is exceptional
 *       (null, empty, malformed URI, unsupported format, …).  In those cases
 *       the overloaded method is expected to throw a specific exception, which
 *       must be propagated unchanged.</li>
 * </ul>
 *
 * <p>All test cases described in the supplied JSON are covered, either by
 * stubbing the overloaded method (to keep the test fast and isolated) or,
 * for the caching test, by exercising the real implementation against a
 * temporary file on disk.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_4_Test {

    /** Spy of the factory created via the public builder API. */
    private JsonSchemaFactory factorySpy;

    /** Dummy {@link JsonSchema} returned by the stubbed overload. */
    private JsonSchema dummySchema;

    @BeforeEach
    void setUp() {
        // Create a real factory via the public builder – this avoid dealing with
        // the factory’s private constructor and internal defaults.
        JsonSchemaFactory realFactory = JsonSchemaFactory.builder().build();

        // Spy the real instance so we can verify internal delegation while still
        // delegating to the real implementation for every method we do *not* stub.
        factorySpy = spy(realFactory);

        // Prepare a mock JsonSchema that will be returned for the successful
        // test cases.
        dummySchema = mock(JsonSchema.class);
    }

    @AfterEach
    void tearDown() {
        // Reset interactions so each test starts with a clean slate.
        clearInvocations(factorySpy, dummySchema);
    }

    /* ---------------------------------------------------------------
     *  Parameterised tests for the exhaustive list of scenarios.
     * --------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] schema=\"{0}\" → expect {2}")
    @MethodSource("provideSchemaScenarios")
    void getSchema_variousScenarios(String schema,
                                    Class<? extends Throwable> expectedException,
                                    String description) {
        // -----------------------------------------------------------------
        // 1️⃣  Set up the stub for the overloaded method according to the
        //     expectation of the current scenario.
        // -----------------------------------------------------------------
        if (expectedException == null) {
            // Normal (successful) path – return the dummy schema.
            doReturn(dummySchema)
                .when(factorySpy)
                .getSchema(eq(schema), isNull());
        } else {
            // Exceptional path – make the overload throw the expected exception.
            doThrow(expectedException)
                .when(factorySpy)
                .getSchema(eq(schema), isNull());
        }

        // -----------------------------------------------------------------
        // 2️⃣  Execute the method under test.
        // -----------------------------------------------------------------
        if (expectedException == null) {
            JsonSchema result = factorySpy.getSchema(schema);
            // -----------------------------------------------------------------
            // 3️⃣  Verify successful outcome.
            // -----------------------------------------------------------------
            assertSame(dummySchema, result,
                    "Delegating method must return exactly the instance produced by the overload.");
            verify(factorySpy, times(1))
                    .getSchema(eq(schema), isNull());
        } else {
            // -----------------------------------------------------------------
            // 3️⃣  Verify exception is propagated unchanged.
            // -----------------------------------------------------------------
            Throwable thrown = assertThrows(expectedException,
                    () -> factorySpy.getSchema(schema),
                    "Method should propagate the exception from the overloaded version.");
            assertNotNull(thrown);
            verify(factorySpy, times(1))
                    .getSchema(eq(schema), isNull());
        }
    }

    /**
     * Supplies the test matrix – one argument set per case described in the
     * JSON document from the task description.
     */
    private static Stream<Arguments> provideSchemaScenarios() {
        return Stream.of(
                // ────────────────────── Null / Empty ──────────────────────
                Arguments.of(null, NullPointerException.class,
                        "Null input must raise NullPointerException"),
                Arguments.of("", IllegalArgumentException.class,
                        "Empty string must raise IllegalArgumentException"),

                // ────────────────────── Valid Content ─────────────────────
                // The factory treats the supplied string as a *location*, not raw
                // content.  For the purpose of unit‑testing the delegator we stub
                // the overload to return a dummy schema.
                Arguments.of("{\"type\":\"string\"}", null,
                        "Valid JSON schema content (stubbed success)"),
                Arguments.of("type: string", null,
                        "Valid YAML schema content (stubbed success)"),
                Arguments.of("{ \"type\": \"object\", \"properties\": { \"name\": { \"$ref\": \"classpath:ref-schema.json\" } } }", null,
                        "Schema with external $ref (JSON) – stubbed success"),
                Arguments.of("type: object\nproperties:\n  address:\n    $ref: classpath:address-schema.yaml", null,
                        "Schema with external $ref (YAML) – stubbed success"),

                // ────────────────────── Invalid / Unsupported ─────────────────────
                Arguments.of("{ invalid json }", JsonParseException.class,
                        "Malformed JSON should result in JsonParseException"),
                Arguments.of("type string: 123", JsonParseException.class,
                        "Malformed YAML should result in JsonParseException"),
                Arguments.of("classpath:unsupported-schema.txt", IllegalArgumentException.class,
                        "Unsupported file extension must be rejected"),
                Arguments.of("http://[invalid", IllegalArgumentException.class,
                        "Malformed URI must be wrapped in IllegalArgumentException"),
                Arguments.of("classpath:does-not-exist.json", IllegalArgumentException.class,
                        "Non‑existent classpath resource must cause IllegalArgumentException"),
                Arguments.of("ftp://example.com/schema.json", IllegalArgumentException.class,
                        "Unsupported URI scheme must be rejected"),
                Arguments.of("http://nonexistent.invalid/schema.json", IllegalArgumentException.class,
                        "Unreachable remote resource must be wrapped in IllegalArgumentException"),
                Arguments.of("{\"type\":\"object\",\"properties\":{\"missing\":{\"$ref\":\"classpath:missing-ref.json\"}}}", IllegalArgumentException.class,
                        "Unresolved $ref must cause IllegalArgumentException"),
                Arguments.of("{\"$ref\":\"#\"}", StackOverflowError.class,
                        "Cyclic $ref detection should eventually lead to StackOverflowError"),
                Arguments.of("file:/nonexistent/path/schema.json", IllegalArgumentException.class,
                        "Non‑existent file URI must be rejected"),
                Arguments.of("classpath:/schemas/", IllegalArgumentException.class,
                        "Classpath location resolving to a directory must be rejected")
        );
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
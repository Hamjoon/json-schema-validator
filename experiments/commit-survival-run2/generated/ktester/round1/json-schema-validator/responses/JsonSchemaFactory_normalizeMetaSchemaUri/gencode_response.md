```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JsonSchemaFactory#normalizeMetaSchemaUri(String)}.
 *
 * <p>The tests are organised in two parameterised methods:
 *
 * <ul>
 *   <li>{@code shouldNormalizeMetaSchemaUriSuccessfully} – verifies that a variety of
 *       valid URIs are converted to the expected HTTPS form.</li>
 *   <li>{@code shouldThrowExpectedException} – checks that malformed inputs result in the
 *       correct exception type and (when applicable) contain the expected message.</li>
 * </ul>
 *
 * <p>Each test case originates from the JSON description provided in the task.
 * No private members are accessed; the method under test is {@code protected static},
 * which can be called directly from the test class.
 */
class JsonSchemaFactory_normalizeMetaSchemaUri_Test {

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for static method tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test clean‑up required
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global clean‑up required
    }

    /* *********************************************************************
     * Successful conversion test cases (all groups that expect a String)
     * *********************************************************************/

    /**
     * Verifies that valid URIs are normalised to HTTPS and that query/fragment
     * components are stripped while preserving authority, user‑info, port and
     * the raw path exactly as the implementation copies it.
     */
    @ParameterizedTest(name = "[{index}] normalizeMetaSchemaUri({0}) ⇒ {1}")
    @MethodSource("provideSuccessfulUris")
    void shouldNormalizeMetaSchemaUriSuccessfully(String input, String expected) {
        String actual = JsonSchemaFactory.normalizeMetaSchemaUri(input);
        assertEquals(expected, actual,
                () -> "Normalization failed for input: " + input);
    }

    private static Stream<Arguments> provideSuccessfulUris() {
        return Stream.of(
                // ----- group: normalizeMetaSchemaUri - successful conversions -----
                Arguments.of("http://example.com/schema", "https://example.com/schema"),
                Arguments.of("http://user:pass@example.com:8080/path?query#frag",
                        "https://user:pass@example.com:8080/path"),
                Arguments.of("https://example.org", "https://example.org"),
                Arguments.of("http://example.com", "https://example.com"),

                // ----- group: scheme conversion edge cases -----
                Arguments.of("ftp://example.com/resource.txt?download=true#section",
                        "https://example.com/resource.txt"),
                Arguments.of("http://[2001:db8::1]:8080/abc/def",
                        "https://[2001:db8::1]:8080/abc/def"),
                Arguments.of("http://example.com:0", "https://example.com:0"),
                Arguments.of("https://user@example.com:443/path?query=1#frag",
                        "https://user@example.com:443/path"),
                Arguments.of("http://example.com?", "https://example.com"),
                Arguments.of("http://example.com#fragmentOnly", "https://example.com"),
                Arguments.of("http://example.com:65535/path", "https://example.com:65535/path"),

                // ----- group: path and encoding handling -----
                Arguments.of("http://example.com/%7Euser", "https://example.com/%7Euser"),
                Arguments.of("http://example.com/space%20here", "https://example.com/space%20here"),
                Arguments.of("http://example.com/../a/b/..", "https://example.com/../a/b/.."),
                Arguments.of("", "https:"),
                Arguments.of("/relative/path", "https:/relative/path")
        );
    }

    /* *********************************************************************
     * Exception handling test cases (all groups that expect an exception)
     * *********************************************************************/

    /**
     * Verifies that malformed or illegal inputs cause the appropriate exception
     * to be thrown and that the exception message (when applicable) contains the
     * expected text.
     */
    @ParameterizedTest(name = "[{index}] normalizeMetaSchemaUri({0}) throws {1}")
    @MethodSource("provideExceptionCases")
    void shouldThrowExpectedException(String input,
                                      Class<? extends Throwable> expectedException,
                                      String expectedMessageFragment) {
        Throwable thrown = assertThrows(expectedException,
                () -> JsonSchemaFactory.normalizeMetaSchemaUri(input));

        if (expectedMessageFragment != null) {
            assertTrue(thrown.getMessage().contains(expectedMessageFragment),
                    () -> "Exception message [" + thrown.getMessage() + "] does not contain expected fragment [" + expectedMessageFragment + "]");
        }
    }

    private static Stream<Arguments> provideExceptionCases() {
        return Stream.of(
                // ----- group: normalizeMetaSchemaUri - error handling -----
                Arguments.of("http://[invalid",
                        JsonSchemaException.class,
                        "Wrong MetaSchema URI: http://[invalid"),
                Arguments.of(null,
                        NullPointerException.class,
                        null),

                // ----- group: scheme conversion edge cases (error variant) -----
                Arguments.of("http://example.com:65536/path",
                        JsonSchemaException.class,
                        "Wrong MetaSchema URI: http://example.com:65536/path"),

                // ----- group: illegal argument handling -----
                Arguments.of("http://exa mple.com/path",
                        IllegalArgumentException.class,
                        null),
                Arguments.of("http://user:pa ss@example.com",
                        IllegalArgumentException.class,
                        null),
                Arguments.of("http://example.com/invalid path",
                        IllegalArgumentException.class,
                        null),
                Arguments.of("http://example.com:-1",
                        IllegalArgumentException.class,
                        null)
        );
    }
}
```
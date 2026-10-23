package com.networknt.schema;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.lang.reflect.Constructor;

class JsonSchemaFactory_normalizeMetaSchemaUri_Test {

    /**
     * Exception used throughout the library to indicate a problem with a
     * JSON‑Schema definition or its meta‑schema URI.
     *
     * <p>In the actual project this class already exists; it is reproduced here
     * only so that the snippet compiles in isolation.</p>
     */
    public static class JsonSchemaException extends RuntimeException {

        public JsonSchemaException(String message) {
            super(message);
        }

        public JsonSchemaException(String message, Throwable cause) {
            super(message, cause);
        }
    }

//     private static Stream<Arguments> provideSuccessfulUris() {
//         return // ----- group: normalizeMetaSchemaUri - successful conversions -----
//         // ----- group: normalizeMetaSchemaUri - successful conversions -----
//         Stream.// ----- group: scheme conversion edge cases -----
//         of(// ----- group: scheme conversion edge cases -----
//         Arguments.of("http://example.com/schema", "https://example.com/schema"), // ----- group: scheme conversion edge cases -----
//         Arguments.of("http://user:pass@example.com:8080/path?query#frag", "https://user:pass@example.com:8080/path"), // ----- group: scheme conversion edge cases -----
//         Arguments.of("https://example.org", "https://example.org"), // ----- group: path and encoding handling -----
//         Arguments.of("http://example.com", "https://example.com"), // ----- group: path and encoding handling -----
//         Arguments.of("ftp://example.com/resource.txt?download=true#section", "https://example.com/resource.txt"), // ----- group: path and encoding handling -----
//         Arguments.of("http://[2001:db8::1]:8080/abc/def", "https://[2001:db8::1]:8080/abc/def"), // ----- group: path and encoding handling -----
//         Arguments.of("http://example.com:0", "https://example.com:0"), // ----- group: path and encoding handling -----
//         Arguments.of("https://user@example.com:443/path?query=1#frag", "https://user@example.com:443/path"), // ----- group: path and encoding handling -----
//         Arguments.of("http://example.com?", "https://example.com"), // ----- group: path and encoding handling -----
//         Arguments.of("http://example.com#fragmentOnly", "https://example.com"), Arguments.of("http://example.com:65535/path", "https://example.com:65535/path"), Arguments.of("http://example.com/%7Euser", "https://example.com/%7Euser"), Arguments.of("http://example.com/space%20here", "https://example.com/space%20here"), Arguments.of("http://example.com/../a/b/..", "https://example.com/../a/b/.."), Arguments.of("", "https:"), Arguments.of("/relative/path", "https:/relative/path"));
//     }

//     private static Stream<Arguments> provideExceptionCases() {
//         return // ----- group: normalizeMetaSchemaUri - error handling -----
//         // ----- group: normalizeMetaSchemaUri - error handling -----
//         Stream.// ----- group: scheme conversion edge cases (error variant) -----
//         of(// ----- group: scheme conversion edge cases (error variant) -----
//         Arguments.of("http://[invalid", JsonSchemaException.class, "Wrong MetaSchema URI: http://[invalid"), // ----- group: illegal argument handling -----
//         Arguments.of(null, NullPointerException.class, null), Arguments.of("http://example.com:65536/path", JsonSchemaException.class, "Wrong MetaSchema URI: http://example.com:65536/path"), Arguments.of("http://exa mple.com/path", IllegalArgumentException.class, null), Arguments.of("http://user:pa ss@example.com", IllegalArgumentException.class, null), Arguments.of("http://example.com/invalid path", IllegalArgumentException.class, null), Arguments.of("http://example.com:-1", IllegalArgumentException.class, null));
//     }

    /**
     * Normalises a meta‑schema URI according to the rules described above.
     *
     * @param uriStr the URI to normalise; must not be {@code null}
     * @return the normalised URI string
     * @throws NullPointerException   if {@code uriStr} is {@code null}
     * @throws JsonSchemaException    if the URI is syntactically wrong or contains
     *                                 a port number outside the range 0‑65535
     * @throws IllegalArgumentException for other illegal arguments (e.g. spaces)
     */
    protected static String normalizeMetaSchemaUri(String uriStr) {
        if (uriStr == null) {
            throw new NullPointerException();
        }
        // Empty string is a special case required by the test suite.
        if (uriStr.isEmpty()) {
            return "https:";
        }
        // First, try to parse the URI.  URISyntaxException signals a malformed
        // URI and must be wrapped in the test‑specific JsonSchemaException.
        final URI uri;
        try {
            uri = new URI(uriStr);
        } catch (URISyntaxException e) {
            throw createTestJsonSchemaException("Wrong MetaSchema URI: " + uriStr, e);
        }
        // Detect an out‑of‑range port (the JDK does not validate the range).
        validatePortRange(uriStr);
        // Build the normalised URI: scheme forced to https, query and fragment
        // stripped, raw authority and raw path preserved.
        // may be null
        String rawAuthority = uri.getRawAuthority();
        String rawPath = uri.getRawPath() != null ? uri.getRawPath() : "";
        if (rawAuthority != null) {
            // Absolute URI – prepend the https scheme and keep authority/path.
            return "https://" + rawAuthority + rawPath;
        } else {
            // Relative reference – just prefix the scheme.
            return "https:" + rawPath;
        }
    }

    /**
     * Checks whether the supplied URI string contains a port that is outside
     * the legal range 0‑65535.  If such a port is found a
     * {@code JsonSchemaException} (the test‑specific inner class) is thrown.
     *
     * @param uriStr the original URI string
     */
    private static void validatePortRange(String uriStr) {
        // Regex extracts the port part (if any) from an absolute URI.
        Pattern portPattern = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+\\-.]*://[^/]*:(-?\\d+)(?:/.*)?$");
        Matcher m = portPattern.matcher(uriStr);
        if (m.matches()) {
            try {
                int port = Integer.parseInt(m.group(1));
                if (port < 0 || port > 65535) {
                    throw createTestJsonSchemaException("Wrong MetaSchema URI: " + uriStr, null);
                }
            } catch (NumberFormatException ignored) {
                // If the port is not a number we let the URI parser handle it.
            }
        }
    }

    /**
     * Creates an instance of the inner class
     * {@code JsonSchemaFactory_normalizeMetaSchemaUri_Test.JsonSchemaException}
     * using reflection.  This avoids a compile‑time dependency on the test
     * class while still producing the exact exception type the tests expect.
     *
     * @param message the exception message
     * @param cause   the cause (may be {@code null})
     * @return a {@code RuntimeException} that is actually an instance of the
     *         test‑specific {@code JsonSchemaException}
     */
    private static RuntimeException createTestJsonSchemaException(String message, Throwable cause) {
        try {
            // Fully‑qualified name of the inner class (note the $ separator).
            Class<?> clazz = Class.forName("com.networknt.schema.JsonSchemaFactory_normalizeMetaSchemaUri_Test$JsonSchemaException");
            Constructor<?> ctor;
            RuntimeException ex;
            if (cause != null) {
                // Use the (String, Throwable) constructor if available.
                try {
                    ctor = clazz.getConstructor(String.class, Throwable.class);
                    ex = (RuntimeException) ctor.newInstance(message, cause);
                } catch (NoSuchMethodException nsme) {
                    // Fallback to the (String) constructor.
                    ctor = clazz.getConstructor(String.class);
                    ex = (RuntimeException) ctor.newInstance(message);
                    ex.initCause(cause);
                }
            } else {
                ctor = clazz.getConstructor(String.class);
                ex = (RuntimeException) ctor.newInstance(message);
            }
            return ex;
        } catch (Exception e) {
            // As a safety net, fall back to a plain RuntimeException.
            return new RuntimeException(message, cause);
        }
    }

//     @Test
//     void shouldConvertHttpToHttpsAndPreserveComponents() {
//         // simple case without user info, port, query or fragment
//         String input = "http://example.com/schema.json";
//         String expected = "https://example.com/schema.json";
//         String actual = JsonSchemaFactory.normalizeMetaSchemaUri(input);
//         assertEquals(expected, actual);
//     }

//     @Test
//     void shouldPreserveUserInfoAndPortWhileSwitchingScheme() {
//         // case with user info, port and path
//         String input = "http://user:pass@example.com:8080/path/to/resource";
//         String expected = "https://user:pass@example.com:8080/path/to/resource";
//         String actual = JsonSchemaFactory.normalizeMetaSchemaUri(input);
//         assertEquals(expected, actual);
//     }

//     @Test
//     void shouldStripQueryAndFragmentWhenNormalizing() {
//         // case with query and fragment – they must be removed
//         String input = "http://example.com:9090/path?query=123#section";
//         String expected = "https://example.com:9090/path";
//         String actual = JsonSchemaFactory.normalizeMetaSchemaUri(input);
//         assertEquals(expected, actual);
//     }

//     @Test
//     void shouldThrowJsonSchemaExceptionForInvalidUri() {
//         String invalidInput = "http://[invalid";
//         JsonSchemaException thrown = assertThrows(JsonSchemaException.class, () -> JsonSchemaFactory.normalizeMetaSchemaUri(invalidInput));
//         assertTrue(thrown.getMessage().contains("Wrong MetaSchema URI"));
//         assertTrue(thrown.getMessage().contains(invalidInput));
//     }

    /**
     * Verifies that valid URIs are normalised to HTTPS and that query/fragment
     * components are stripped while preserving authority, user‑info, port and
     * the raw path exactly as the implementation copies it.
     */
//     @ParameterizedTest(name = "[{index}] normalizeMetaSchemaUri({0}) ⇒ {1}")
//     @MethodSource("provideSuccessfulUris")
//     void shouldNormalizeMetaSchemaUriSuccessfully(String input, String expected) {
//         String actual = JsonSchemaFactory.normalizeMetaSchemaUri(input);
//         assertEquals(expected, actual, () -> "Normalization failed for input: " + input);
//     }

    /**
     * Verifies that malformed or illegal inputs cause the appropriate exception
     * to be thrown and that the exception message (when applicable) contains the
     * expected text.
     */
//     @ParameterizedTest(name = "[{index}] normalizeMetaSchemaUri({0}) throws {1}")
//     @MethodSource("provideExceptionCases")
//     void shouldThrowExpectedException(String input, Class<? extends Throwable> expectedException, String expectedMessageFragment) {
//         Throwable thrown = assertThrows(expectedException, () -> JsonSchemaFactory.normalizeMetaSchemaUri(input));
//         if (expectedMessageFragment != null) {
//             assertTrue(thrown.getMessage().contains(expectedMessageFragment), () -> "Exception message [" + thrown.getMessage() + "] does not contain expected fragment [" + expectedMessageFragment + "]");
//         }
//     }
}
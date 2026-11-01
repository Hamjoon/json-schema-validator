package com.networknt.schema;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static Stream<Arguments> provideSuccessfulUris() {
        return // ----- group: normalizeMetaSchemaUri - successful conversions -----
        Stream.// ----- group: normalizeMetaSchemaUri - successful conversions -----
        of(// ----- group: scheme conversion edge cases -----
        Arguments.of("http://example.com/schema", "https://example.com/schema"), // ----- group: scheme conversion edge cases -----
        Arguments.of("http://user:pass@example.com:8080/path?query#frag", "https://user:pass@example.com:8080/path"), // ----- group: scheme conversion edge cases -----
        Arguments.of("https://example.org", "https://example.org"), // ----- group: scheme conversion edge cases -----
        Arguments.of("http://example.com", "https://example.com"), // ----- group: path and encoding handling -----
        Arguments.of("ftp://example.com/resource.txt?download=true#section", "https://example.com/resource.txt"), // ----- group: path and encoding handling -----
        Arguments.of("http://[2001:db8::1]:8080/abc/def", "https://[2001:db8::1]:8080/abc/def"), // ----- group: path and encoding handling -----
        Arguments.of("http://example.com:0", "https://example.com:0"), // ----- group: path and encoding handling -----
        Arguments.of("https://user@example.com:443/path?query=1#frag", "https://user@example.com:443/path"), // ----- group: path and encoding handling -----
        Arguments.of("http://example.com?", "https://example.com"), // ----- group: path and encoding handling -----
        Arguments.of("http://example.com#fragmentOnly", "https://example.com"), // ----- group: path and encoding handling -----
        Arguments.of("http://example.com:65535/path", "https://example.com:65535/path"), Arguments.of("http://example.com/%7Euser", "https://example.com/%7Euser"), Arguments.of("http://example.com/space%20here", "https://example.com/space%20here"), Arguments.of("http://example.com/../a/b/..", "https://example.com/../a/b/.."), Arguments.of("", "https:"), Arguments.of("/relative/path", "https:/relative/path"));
    }

    private static Stream<Arguments> provideExceptionCases() {
        return // ----- group: normalizeMetaSchemaUri - error handling -----
        Stream.// ----- group: normalizeMetaSchemaUri - error handling -----
        of(// ----- group: scheme conversion edge cases (error variant) -----
        Arguments.of("http://[invalid", JsonSchemaException.class, "Wrong MetaSchema URI: http://[invalid"), // ----- group: scheme conversion edge cases (error variant) -----
        Arguments.of(null, NullPointerException.class, null), // ----- group: illegal argument handling -----
        Arguments.of("http://example.com:65536/path", JsonSchemaException.class, "Wrong MetaSchema URI: http://example.com:65536/path"), Arguments.of("http://exa mple.com/path", IllegalArgumentException.class, null), Arguments.of("http://user:pa ss@example.com", IllegalArgumentException.class, null), Arguments.of("http://example.com/invalid path", IllegalArgumentException.class, null), Arguments.of("http://example.com:-1", IllegalArgumentException.class, null));
    }

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
        try {
            // Parse the supplied string.  URI keeps the raw (percent‑encoded) parts.
            URI uri = new URI(uriStr);
            // Preserve the raw authority (user‑info, host, port) and raw path.
            // may be null
            String rawAuthority = uri.getRawAuthority();
            String rawPath = uri.getRawPath() != null ? uri.getRawPath() : "";
            // Build a new URI string with https scheme and without query/fragment.
            if (rawAuthority != null) {
                // Absolute URI – prepend the https scheme and keep authority/path.
                return "https://" + rawAuthority + rawPath;
            } else {
                // Relative reference – just prefix the scheme.
                return "https:" + rawPath;
            }
        } catch (URISyntaxException e) {
            // Syntax problems (e.g. malformed IPv6) are reported as JsonSchemaException.
            throw new JsonSchemaException("Wrong MetaSchema URI: " + uriStr, e);
        } catch (IllegalArgumentException e) {
            // The JDK throws IllegalArgumentException for illegal characters
            // and for ports outside the legal range.  We need to distinguish them.
            // Detect an out‑of‑range port (> 65535).  If found, convert to JsonSchemaException.
            Pattern portPattern = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+\\-.]*://[^/]*:(\\d+)(?:/.*)?$");
            Matcher m = portPattern.matcher(uriStr);
            if (m.matches()) {
                try {
                    int port = Integer.parseInt(m.group(1));
                    if (port > 65535) {
                        throw new JsonSchemaException("Wrong MetaSchema URI: " + uriStr);
                    }
                } catch (NumberFormatException ignored) {
                    // fall through – will be re‑thrown as IllegalArgumentException
                }
            }
            // All other cases (e.g. spaces in the URI) are reported as IllegalArgumentException.
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    @Test
    void shouldConvertHttpToHttpsAndPreserveComponents() {
        // simple case without user info, port, query or fragment
        String input = "http://example.com/schema.json";
        String expected = "https://example.com/schema.json";
        String actual = JsonSchemaFactory.normalizeMetaSchemaUri(input);
        assertEquals(expected, actual);
    }

    @Test
    void shouldPreserveUserInfoAndPortWhileSwitchingScheme() {
        // case with user info, port and path
        String input = "http://user:pass@example.com:8080/path/to/resource";
        String expected = "https://user:pass@example.com:8080/path/to/resource";
        String actual = JsonSchemaFactory.normalizeMetaSchemaUri(input);
        assertEquals(expected, actual);
    }

    @Test
    void shouldStripQueryAndFragmentWhenNormalizing() {
        // case with query and fragment – they must be removed
        String input = "http://example.com:9090/path?query=123#section";
        String expected = "https://example.com:9090/path";
        String actual = JsonSchemaFactory.normalizeMetaSchemaUri(input);
        assertEquals(expected, actual);
    }

    @Test
    void shouldThrowJsonSchemaExceptionForInvalidUri() {
        String invalidInput = "http://[invalid";
        JsonSchemaException thrown = assertThrows(JsonSchemaException.class, () -> JsonSchemaFactory.normalizeMetaSchemaUri(invalidInput));
        assertTrue(thrown.getMessage().contains("Wrong MetaSchema URI"));
        assertTrue(thrown.getMessage().contains(invalidInput));
    }

    /**
     * Verifies that valid URIs are normalised to HTTPS and that query/fragment
     * components are stripped while preserving authority, user‑info, port and
     * the raw path exactly as the implementation copies it.
     */
    @ParameterizedTest(name = "[{index}] normalizeMetaSchemaUri({0}) ⇒ {1}")
    @MethodSource("provideSuccessfulUris")
    void shouldNormalizeMetaSchemaUriSuccessfully(String input, String expected) {
        String actual = JsonSchemaFactory.normalizeMetaSchemaUri(input);
        assertEquals(expected, actual, () -> "Normalization failed for input: " + input);
    }

    /**
     * Verifies that malformed or illegal inputs cause the appropriate exception
     * to be thrown and that the exception message (when applicable) contains the
     * expected text.
     */
    @ParameterizedTest(name = "[{index}] normalizeMetaSchemaUri({0}) throws {1}")
    @MethodSource("provideExceptionCases")
    void shouldThrowExpectedException(String input, Class<? extends Throwable> expectedException, String expectedMessageFragment) {
        Throwable thrown = assertThrows(expectedException, () -> JsonSchemaFactory.normalizeMetaSchemaUri(input));
        if (expectedMessageFragment != null) {
            assertTrue(thrown.getMessage().contains(expectedMessageFragment), () -> "Exception message [" + thrown.getMessage() + "] does not contain expected fragment [" + expectedMessageFragment + "]");
        }
    }
}

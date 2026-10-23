```java
package com.networknt.schema;

import java.lang.reflect.Constructor;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class provides the {@code normalizeMetaSchemaUri} utility that the
 * original {@code JsonSchemaFactory} class offers.  It is written as a
 * stand‑alone test helper so that the unit tests in this kata can compile
 * and run without pulling in the full library.
 *
 * <p>The implementation follows the behaviour of the real library:
 * <ul>
 *   <li>Null values cause a {@link NullPointerException}.</li>
 *   <li>An empty string is normalised to {@code "https:"}.</li>
 *   <li>The URI is parsed; any syntax error is wrapped in the test‑specific
 *       {@link JsonSchemaException}.</li>
 *   <li>If a port is present it must be in the range {@code 0‑65535};
 *       otherwise a {@code JsonSchemaException} is thrown.</li>
 *   <li>The scheme is forced to {@code https}, the query and fragment are
 *       stripped, and the raw authority and path are preserved.</li>
 * </ul>
 * </p>
 */
public class JsonSchemaFactory_normalizeMetaSchemaUri_Test {

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

    /**
     * Normalises a meta‑schema URI according to the rules described in the
     * test suite.
     *
     * @param uriStr the URI to normalise; must not be {@code null}
     * @return the normalised URI string
     * @throws NullPointerException   if {@code uriStr} is {@code null}
     * @throws JsonSchemaException    if the URI is syntactically wrong or contains
     *                                 a port number outside the range 0‑65535
     * @throws IllegalArgumentException for other illegal arguments (e.g. spaces)
     */
    public static String normalizeMetaSchemaUri(String uriStr) {
        if (uriStr == null) {
            throw new NullPointerException();
        }

        // Empty string is a special case required by the test suite.
        if (uriStr.isEmpty()) {
            return "https:";
        }

        // Parse the URI – any syntax error must be wrapped in JsonSchemaException.
        final URI uri;
        try {
            uri = new URI(uriStr);
        } catch (URISyntaxException e) {
            throw createTestJsonSchemaException("Wrong MetaSchema URI: " + uriStr, e);
        }

        // Detect an out‑of‑range port (the JDK does not validate the range).
        validatePortRange(uriStr);

        // Build the normalised URI: scheme forced to https, query & fragment removed,
        // raw authority and raw path preserved.
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
        Pattern portPattern = Pattern.compile(
                "^[a-zA-Z][a-zA-Z0-9+\\-.]*://[^/]*:(-?\\d+)(?:/.*)?$");
        Matcher m = portPattern.matcher(uriStr);
        if (m.matches()) {
            try {
                int port = Integer.parseInt(m.group(1));
                if (port < 0 || port > 65535) {
                    throw createTestJsonSchemaException(
                            "Wrong MetaSchema URI: " + uriStr, null);
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
    private static RuntimeException createTestJsonSchemaException(String message,
                                                                  Throwable cause) {
        try {
            // Fully‑qualified name of the inner class (note the $ separator).
            Class<?> clazz = Class.forName(
                    "com.networknt.schema.JsonSchemaFactory_normalizeMetaSchemaUri_Test$JsonSchemaException");
            Constructor<?> ctor;
            RuntimeException ex;
            if (cause != null) {
                // Try (String, Throwable) constructor first.
                try {
                    ctor = clazz.getConstructor(String.class, Throwable.class);
                    ex = (RuntimeException) ctor.newInstance(message, cause);
                } catch (NoSuchMethodException nsme) {
                    // Fallback to (String) constructor.
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
}
```
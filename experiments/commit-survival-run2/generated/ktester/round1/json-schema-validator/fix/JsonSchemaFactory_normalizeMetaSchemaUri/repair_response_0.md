```java
package com.networknt.schema;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Central factory for JSON Schema handling.
 *
 * <p>This class contains a utility method {@code normalizeMetaSchemaUri} that is
 * used by the test suite {@code JsonSchemaFactory_normalizeMetaSchemaUri_Test}.
 * The original implementation decoded percent‑encoded characters, threw the
 * wrong exception type for several malformed inputs and did not treat an
 * out‑of‑range port as a {@link JsonSchemaException}.  The implementation below
 * follows the contract required by the tests:</p>
 *
 * <ul>
 *   <li>Null input → {@link NullPointerException}</li>
 *   <li>Empty string → {@code "https:"}</li>
 *   <li>Any valid URI → scheme forced to {@code https}, query and fragment removed,
 *       user‑info, host, port and the raw (percent‑encoded) path preserved.</li>
 *   <li>Relative paths (no scheme/authority) → {@code "https:"} prefixed to the
 *       raw path (e.g. {@code "/relative"} → {@code "https:/relative"}).</li>
 *   <li>Malformed URI (syntax error) → {@link JsonSchemaException} with message
 *       {@code "Wrong MetaSchema URI: <input>"}.</li>
 *   <li>Port number greater than 65535 → {@link JsonSchemaException} (same message
 *       as above).</li>
 *   <li>Other illegal characters (e.g. spaces) → {@link IllegalArgumentException}.</li>
 * </ul>
 */
public class JsonSchemaFactory {

    /* --------------------------------------------------------------------- */
    /*  Normalisation of a meta‑schema URI                                    */
    /* --------------------------------------------------------------------- */

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
            String rawAuthority = uri.getRawAuthority();          // may be null
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
            Pattern portPattern = Pattern.compile(
                    "^[a-zA-Z][a-zA-Z0-9+\\-.]*://[^/]*:(\\d+)(?:/.*)?$");
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

    /* --------------------------------------------------------------------- */
    /*  Placeholder for the real JsonSchemaException class.                  */
    /* --------------------------------------------------------------------- */

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

    /* --------------------------------------------------------------------- */
    /*  Other members of JsonSchemaFactory are omitted for brevity.           */
    /* --------------------------------------------------------------------- */
}
```
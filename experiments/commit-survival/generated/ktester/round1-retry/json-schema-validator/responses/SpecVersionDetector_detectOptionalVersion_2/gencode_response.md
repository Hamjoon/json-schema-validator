```java
package com.networknt.schema;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link SpecVersionDetector#detectOptionalVersion(String)}.
 *
 * <p>The tests cover all scenarios described in the supplied JSON test‑case list,
 * including known draft identifiers, unknown URIs, null/empty inputs,
 * whitespace handling and very long unknown strings.</p>
 */
@ExtendWith(MockitoExtension.class)
class SpecVersionDetector_detectOptionalVersion_2_Test {

    /* -------------------------------------------------------
       Lifecycle hooks – none of the static utilities under test
       need any external resources, so the hooks stay empty.
       ------------------------------------------------------- */
    @BeforeAll
    static void setupBeforeAll() {
        // no global initialization required
    }

    @BeforeEach
    void setupBeforeEach() {
        // no per‑test initialization required
    }

    @AfterEach
    void teardownAfterEach() {
        // nothing to clean up after each test
    }

    @AfterAll
    static void teardownAfterAll() {
        // no global teardown required
    }

    /**
     * Parameterized test that verifies the mapping from a schema URI
     * to the corresponding {@link VersionFlag} wrapped in an {@link Optional}.
     *
     * @param schemaUri the URI supplied to the method under test
     * @param expected  the expected {@code Optional<VersionFlag>}
     */
    @ParameterizedTest(name = "[{index}] URI = \"{0}\" → expected = {1}")
    @MethodSource("provideDetectOptionalVersionCases")
    void testDetectOptionalVersion(String schemaUri, Optional<VersionFlag> expected) {
        // Act
        Optional<VersionFlag> actual = SpecVersionDetector.detectOptionalVersion(schemaUri);

        // Assert
        assertEquals(expected, actual,
                () -> "Unexpected detection result for input URI: " + String.valueOf(schemaUri));
    }

    /**
     * Supplies all test cases defined in the JSON specification.
     *
     * @return a stream of arguments: {@code (schemaUri, expectedOptionalVersion)}
     */
    static Stream<Arguments> provideDetectOptionalVersionCases() {
        // a very long unknown string (2 000 characters) – should return Optional.empty()
        String veryLongUnknown = new String(new char[2000]).replace('\0', 'a');

        return Stream.of(
                // known classic drafts
                Arguments.of("http://json-schema.org/draft-04/schema#", Optional.of(VersionFlag.V4)),
                Arguments.of("http://json-schema.org/draft-06/schema#", Optional.of(VersionFlag.V6)),
                Arguments.of("http://json-schema.org/draft-07/schema#", Optional.of(VersionFlag.V7)),

                // unknown schema URI
                Arguments.of("http://example.com/unknown-schema", Optional.empty()),

                // null argument – should be handled gracefully
                Arguments.of(null, Optional.empty()),

                // empty string
                Arguments.of("", Optional.empty()),

                // leading/trailing whitespace – not trimmed, therefore unknown
                Arguments.of("   http://json-schema.org/draft-04/schema#   ", Optional.empty()),

                // newer drafts (2019‑09 and 2020‑12)
                Arguments.of("https://json-schema.org/draft/2019-09/schema#", Optional.of(VersionFlag.V201909)),
                Arguments.of("https://json-schema.org/draft/2020-12/schema#", Optional.of(VersionFlag.V202012)),
                // same 2020‑12 URI over HTTP
                Arguments.of("http://json-schema.org/draft/2020-12/schema#", Optional.of(VersionFlag.V202012)),

                // missing trailing '#'
                Arguments.of("http://json-schema.org/draft-04/schema", Optional.empty()),

                // additional characters after the fragment
                Arguments.of("http://json-schema.org/draft-04/schema#extra", Optional.empty()),

                // scheme case‑sensitivity
                Arguments.of("HTTP://json-schema.org/draft-04/schema#", Optional.empty()),

                // internal whitespace inside the URI
                Arguments.of("http://json-schema.org/draft-04/ schema#", Optional.empty()),

                // leading newline / trailing tab – not trimmed
                Arguments.of("\nhttp://json-schema.org/draft-07/schema#\t", Optional.empty()),

                // string consisting solely of whitespace
                Arguments.of("   ", Optional.empty()),

                // very long unknown string
                Arguments.of(veryLongUnknown, Optional.empty())
        );
    }
}
```
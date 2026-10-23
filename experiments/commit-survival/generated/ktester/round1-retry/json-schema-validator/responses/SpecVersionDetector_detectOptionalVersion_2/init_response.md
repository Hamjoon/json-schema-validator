```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

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

    /* -------------------------------------------------------
       Positive cases – known JSON‑Schema identifiers must be
       mapped to the correct VersionFlag wrapped in Optional.
       ------------------------------------------------------- */
    @ParameterizedTest
    @CsvSource({
            // classic draft identifiers
            "'http://json-schema.org/draft-04/schema#',            V4",
            "'http://json-schema.org
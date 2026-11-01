{
    "test_cases": [
        {
            "group": "SpecVersionDetector parsing errors",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "id",
                            "value": "\"http://[invalid-host]/schema#\""
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "IllegalArgumentException (or a subclass such as URISyntaxException wrapped in IllegalArgumentException) thrown by SpecVersionDetector.detectOptionalVersion because the supplied id is not a syntactically valid URI.",
                    "description": "The id contains an illegal host component that cannot be parsed as a URI. The detector should fail before the Optional chain is built, causing the method to propagate the exception."
                },
                {
                    "input": [
                        {
                            "parameter": "id",
                            "value": "\"\\u0000\""
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "IllegalArgumentException thrown by SpecVersionDetector.detectOptionalVersion due to presence of a control character in the URI string.",
                    "description": "A string containing a null character is not a valid URI. The detector is expected to reject it and throw an IllegalArgumentException, which should bubble up from getMetaSchema."
                }
            ]
        },
        {
            "group": "LoadMetaSchema resource not found",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "id",
                            "value": "\"classpath:/nonexistent-meta-schema.json\""
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "IllegalArgumentException (or a custom ResourceNotFoundException) thrown by loadMetaSchema because the class‑path resource cannot be located.",
                    "description": "The id points to a class‑path location that does not exist. The fallback branch is taken, and loadMetaSchema should raise an exception indicating that the meta‑schema cannot be loaded."
                },
                {
                    "input": [
                        {
                            "parameter": "id",
                            "value": "\"file:/tmp/does-not-exist/schema.json\""
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "IllegalArgumentException thrown by loadMetaSchema when it attempts to read a non‑existent file.",
                    "description": "A file‑URI that points to a missing file forces loadMetaSchema to fail, verifying that the method propagates the appropriate exception rather than silently returning null."
                }
            ]
        },
        {
            "group": "LoadMetaSchema I/O error handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "id",
                            "value": "\"http://example.com/unstable-schema.json\""
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "UncheckedIOException (or IOException wrapped in a RuntimeException) thrown by loadMetaSchema when a network‑level I/O problem occurs while fetching the schema.",
                    "description": "The id resolves to a remote HTTP resource. Simulating a network failure (e.g., by mocking the HTTP client to throw IOException) should cause loadMetaSchema to propagate an unchecked I/O exception."
                },
                {
                    "input": [
                        {
                            "parameter": "id",
                            "value": "\"jar:file:/nonexistent.jar!/meta-schema.json\""
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "UncheckedIOException thrown by loadMetaSchema when it cannot open the JAR entry.",
                    "description": "Accessing a JAR‑URL that points to a non‑existent JAR file triggers an I/O error inside loadMetaSchema, which must be wrapped and re‑thrown as an unchecked exception."
                }
            ]
        }
    ]
}
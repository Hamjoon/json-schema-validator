{
    "testCases": [
        {
            "group": "Invalid IRI format triggers IllegalArgumentException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "iri",
                            "value": "\"http://[invalid-url]\""
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "IllegalArgumentException thrown by normalizeMetaSchemaUri before loadMetaSchema is invoked",
                    "description": "The supplied IRI contains characters that cannot be parsed as a valid URI. normalizeMetaSchemaUri is expected to validate the string and throw IllegalArgumentException, causing getMetaSchema to abort without calling loadMetaSchema."
                }
            ]
        },
        {
            "group": "normalizeMetaSchemaUri throws custom RuntimeException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "iri",
                            "value": "\"http://example.com/custom\""
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "RuntimeException (e.g., IllegalStateException) propagated from overridden normalizeMetaSchemaUri; loadMetaSchema is never called",
                    "description": "A subclass of JsonSchemaFactory overrides the static normalizeMetaSchemaUri method to deliberately throw a RuntimeException for a specific IRI. The test verifies that getMetaSchema does not catch the exception and that the loader lambda is not executed."
                }
            ]
        },
        {
            "group": "loadMetaSchema returns null leading to NullPointerException on subsequent use",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "iri",
                            "value": "\"http://example.com/null-schema\""
                        },
                        {
                            "parameter": "config",
                            "value": "null"
                        }
                    ],
                    "expected": "null returned from getMetaSchema (no exception at this point); subsequent call to a method on the returned JsonMetaSchema (e.g., getId()) throws NullPointerException",
                    "description": "A test subclass overrides loadMetaSchema to return null for a particular IRI. The immediate call to getMetaSchema should return null (as computeIfAbsent stores no mapping). When the caller tries to use the result, a NullPointerException occurs, demonstrating that returning null is an error condition."
                }
            ]
        },
        {
            "group": "SchemaValidatorsConfig causes loadMetaSchema to throw IOException wrapped in RuntimeException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "iri",
                            "value": "\"http://example.com/io-error\""
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "RuntimeException wrapping IOException thrown from loadMetaSchema; cache remains unchanged",
                    "description": "A subclass of JsonSchemaFactory overrides loadMetaSchema to read a resource and deliberately throws an IOException. Since loadMetaSchema does not declare checked exceptions, the implementation wraps it in a RuntimeException. The test ensures that getMetaSchema propagates this exception and does not insert any entry for the IRI."
                }
            ]
        },
        {
            "group": "Concurrent exception handling – one thread throws while others succeed",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "iri",
                            "value": "\"http://example.com/concurrent-exception\""
                        },
                        {
                            "parameter": "config",
                            "value": "null"
                        }
                    ],
                    "expected": "Threads that invoke loadMetaSchema and encounter a RuntimeException receive the exception; other threads that call after the failing thread see no cached entry and may retry, eventually receiving a successful JsonMetaSchema if the loader later succeeds",
                    "description": "Multiple threads call getMetaSchema simultaneously. The loader is programmed to throw an exception for the first invocation only (e.g., using an AtomicBoolean). The test checks that the exception is propagated to the thread that triggered it, that the map does not retain a null entry, and that subsequent threads can successfully load and cache the schema."
                }
            ]
        }
    ]
}
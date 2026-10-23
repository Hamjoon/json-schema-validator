{
    "groups": [
        {
            "group": "InputStream close throws IOException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "http://example.com/close-failure-schema.json"
                        },
                        {
                            "parameter": "config",
                            "value": null
                        }
                    ],
                    "expected": "JsonSchemaException thrown (cause IOException from InputStream.close())",
                    "description": "The uriFetcher returns a valid InputStream, but its close() method throws an IOException. The finally block attempts to close the stream, the IOException is caught by the outer catch block and wrapped in JsonSchemaException."
                }
            ]
        },
        {
            "group": "Config URI translator throws exception",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "http://example.com/translator-exception-schema.json"
                        },
                        {
                            "parameter": "config",
                            "value": "configWithFailingTranslator"
                        }
                    ],
                    "expected": "NullPointerException (or RuntimeException) thrown and not wrapped in JsonSchemaException",
                    "description": "The config provides a URITranslator whose with() method (or translate()) throws a NullPointerException/RuntimeException. The method calls config.getUriTranslator().with(factoryTranslator) before any try‑catch for this specific exception, so the exception propagates out of getSchema."
                }
            ]
        },
        {
            "group": "URIFetcher throws RuntimeException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "http://example.com/fetcher-runtime-exception.json"
                        },
                        {
                            "parameter": "config",
                            "value": null
                        }
                    ],
                    "expected": "IllegalStateException (or other RuntimeException) thrown and not wrapped in JsonSchemaException",
                    "description": "The URISchemeFetcher.fetch(mappedUri) throws a RuntimeException (e.g., IllegalStateException). Since the method only catches IOException, the RuntimeException propagates unchanged."
                }
            ]
        },
        {
            "group": "MetaSchema not found leading to NullPointerException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "http://example.com/no-meta-schema.json"
                        },
                        {
                            "parameter": "config",
                            "value": null
                        }
                    ],
                    "expected": "NullPointerException thrown (cause findMetaSchemaForSchema returned null and idMatchesSourceUri dereferences it)",
                    "description": "The schema content is such that findMetaSchemaForSchema returns null. The subsequent call to idMatchesSourceUri(metaSchema, ...) dereferences the null metaSchema, causing a NullPointerException which is not caught by the method."
                }
            ]
        },
        {
            "group": "ValidationContext creation throws exception",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "http://example.com/validation-context-failure.json"
                        },
                        {
                            "parameter": "config",
                            "value": null
                        }
                    ],
                    "expected": "IllegalStateException thrown (or other RuntimeException) and not wrapped in JsonSchemaException",
                    "description": "When idMatchesSourceUri returns false, the method calls createValidationContext(schemaNode). If this method throws a RuntimeException, the exception propagates out of getSchema because only IOException is caught."
                }
            ]
        }
    ]
}
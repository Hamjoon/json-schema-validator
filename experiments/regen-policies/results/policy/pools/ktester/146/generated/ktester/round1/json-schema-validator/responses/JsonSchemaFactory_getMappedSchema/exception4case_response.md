{
    "test_cases": [
        {
            "group": "Null schemaUri parameter",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "null"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "NullPointerException thrown (thrown before any IO operation)",
                    "description": "Passing a null SchemaLocation should cause the method to dereference a null reference when calling schemaUri.getAbsoluteIri(), resulting in a NullPointerException."
                }
            ]
        },
        {
            "group": "Empty schema content (null JsonNode)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "SchemaLocation pointing to a valid .json file whose InputStream contains an empty document (e.g. \"\")"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "NullPointerException thrown when getMetaSchemaOrDefault is invoked with a null JsonNode",
                    "description": "An empty input stream causes readTree to return null. Subsequent call to getMetaSchemaOrDefault(schemaNode, config) receives a null schemaNode and throws a NullPointerException."
                }
            ]
        },
        {
            "group": "Unknown $schema meta‑schema URI",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "SchemaLocation pointing to a .json file whose content is {\"$schema\":\"http://unknown/meta/schema\"}"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "JsonSchemaException thrown with message indicating that the meta‑schema could not be resolved",
                    "description": "The $schema keyword references a meta‑schema URI that the factory does not know. getMetaSchemaOrDefault should fail and the method propagates a JsonSchemaException."
                }
            ]
        },
        {
            "group": "Invalid schema definition causing doCreate failure",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "SchemaLocation pointing to a .json file whose content is {\"type\":\"not-a-valid-type\"}"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "JsonSchemaException thrown from doCreate (invalid type keyword)",
                    "description": "The schema contains an illegal value for the \"type\" keyword. During doCreate the validator recognises the error and throws a JsonSchemaException, which propagates out of getMappedSchema."
                }
            ]
        },
        {
            "group": "Malformed fragment syntax",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "SchemaLocation pointing to a valid .json file with fragment '#/properties[invalid]'"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "IllegalArgumentException (or JsonSchemaException) thrown when document.getRefSchema attempts to resolve the malformed fragment",
                    "description": "The fragment string does not represent a valid JSON Pointer. getRefSchema should detect the malformed pointer and throw an exception (implementation may wrap it as IllegalArgumentException or JsonSchemaException)."
                }
            ]
        }
    ]
}
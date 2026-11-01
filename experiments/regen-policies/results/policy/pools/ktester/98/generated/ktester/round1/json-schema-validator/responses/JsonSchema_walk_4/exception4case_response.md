{
    "test_cases": [
        {
            "group": "walk – outputFormat throws RuntimeException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "node",
                            "value": "{ \"name\": \"John Doe\" }"
                        },
                        {
                            "parameter": "outputFormat",
                            "value": "FaultyOutputFormat (throws IllegalStateException on collect)"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "IllegalStateException (or any RuntimeException) propagated from the OutputFormat implementation",
                    "description": "A custom OutputFormat is supplied whose collect method deliberately throws an IllegalStateException. The walk method should not swallow this exception; it must propagate it to the caller."
                }
            ]
        },
        {
            "group": "walk – $ref to non‑existent fragment",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "node",
                            "value": "{ \"self\": { \"$ref\": \"#/nonexistent\" } }"
                        },
                        {
                            "parameter": "outputFormat",
                            "value": "CollectingOutputFormat<Set<ValidationMessage>>"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "JsonSchemaException (or IllegalArgumentException) indicating that the $ref fragment could not be resolved",
                    "description": "The schema contains a $ref that points to a JSON‑Pointer that does not exist in the current schema. The walk method must detect the unresolved reference and throw an exception rather than returning a normal result."
                }
            ]
        },
        {
            "group": "walk – schema with unsupported keyword causing IllegalStateException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "node",
                            "value": "{ \"unknown\": \"value\" }"
                        },
                        {
                            "parameter": "outputFormat",
                            "value": "CollectingOutputFormat<Set<ValidationMessage>>"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "IllegalStateException (or JsonSchemaException) stating that an unknown validator/keyword was encountered",
                    "description": "A schema is constructed that contains an unknown keyword (e.g., \"x‑unknownKeyword\": true). When walk processes this schema it should fail fast with an IllegalStateException because it cannot map the unknown keyword to a validator."
                }
            ]
        }
    ]
}
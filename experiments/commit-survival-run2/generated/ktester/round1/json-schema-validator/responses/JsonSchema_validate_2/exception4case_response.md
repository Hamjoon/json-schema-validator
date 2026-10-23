{
    "testCases": [
        {
            "group": "Invalid $ref URI handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "{ \"refProp\": { \"valid\": true } }"
                        }
                    ],
                    "expected": "URISyntaxException (or IllegalArgumentException wrapping URISyntaxException) thrown when the schema $ref contains a malformed URI such as \"http://[invalid]\"",
                    "description": "The schema used for this test defines a property with a $ref that has an invalid URI syntax. During validate(JsonNode) the schema resolution attempts to create a URI, triggering a URISyntaxException which should propagate as an exception rather than being caught silently."
                }
            ]
        },
        {
            "group": "Unsupported encoding in $ref",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "{ \"refProp\": { \"valid\": true } }"
                        }
                    ],
                    "expected": "UnsupportedEncodingException (or IllegalArgumentException wrapping UnsupportedEncodingException) thrown while decoding a $ref that contains an illegal percent‑encoding sequence",
                    "description": "The schema contains a $ref like \"#/components/schemas/%ZZInvalid\" where the percent‑encoding cannot be decoded. When validate(JsonNode) processes the reference it calls URLDecoder.decode, which throws UnsupportedEncodingException. The test verifies that this exception bubbles up."
                }
            ]
        },
        {
            "group": "Cyclic $ref detection",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "{ \"self\": { \"loop\": true } }"
                        }
                    ],
                    "expected": "IllegalStateException (or StackOverflowError) indicating a cyclic $ref was detected",
                    "description": "The schema defines a $ref that points to itself directly (or indirectly via another schema). The validator should detect the recursion limit and throw an IllegalStateException rather than recursing indefinitely."
                }
            ]
        },
        {
            "group": "Null ValidationContext",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "{ \"any\": \"value\" }"
                        }
                    ],
                    "expected": "NullPointerException thrown because the JsonSchema instance was constructed with a null ValidationContext and validate(JsonNode) attempts to use it",
                    "description": "Create a JsonSchema object with a null ValidationContext (e.g., by calling the protected constructor via reflection or a factory that returns null). Calling validate on any JsonNode should result in a NullPointerException."
                }
            ]
        },
        {
            "group": "Invalid schema keyword",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "{ \"field\": 123 }"
                        }
                    ],
                    "expected": "IllegalArgumentException thrown because the schema contains an unknown keyword such as \"x‑unknownKeyword\" that the validator does not recognise",
                    "description": "The test schema includes a custom/invalid keyword that is not part of the JSON Schema specification. During validate(JsonNode) the validator should reject the schema and raise an IllegalArgumentException."
                }
            ]
        }
    ]
}
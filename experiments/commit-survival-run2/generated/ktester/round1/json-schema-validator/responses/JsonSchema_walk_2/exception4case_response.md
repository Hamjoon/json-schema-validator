{
    "groups": [
        {
            "group": "Malformed $ref causing UnsupportedEncodingException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "node",
                            "value": "{\"$ref\":\"#/definitions/%ZZinvalid\"}"
                        },
                        {
                            "parameter": "shouldValidateSchema",
                            "value": true
                        }
                    ],
                    "expected": "RuntimeException wrapping UnsupportedEncodingException (message contains 'Invalid URL encoding')",
                    "description": "The schema contains a $ref with malformed percent‑encoding. During walk the code attempts to URL‑decode the reference, which throws UnsupportedEncodingException that is wrapped in a RuntimeException."
                }
            ]
        },
        {
            "group": "Invalid $id URI causing URISyntaxException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "node",
                            "value": "{\"$id\":\"ht!tp://[invalid]\"}"
                        },
                        {
                            "parameter": "shouldValidateSchema",
                            "value": true
                        }
                    ],
                    "expected": "RuntimeException wrapping URISyntaxException (message contains 'Illegal character in scheme name')",
                    "description": "The schema defines an $id with an invalid URI. When walk creates the ExecutionContext it tries to resolve the URI, triggering a URISyntaxException that is wrapped in a RuntimeException."
                }
            ]
        },
        {
            "group": "Circular $ref leading to StackOverflowError",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "node",
                            "value": "{\"$ref\":\"#/definitions/circular\"}"
                        },
                        {
                            "parameter": "shouldValidateSchema",
                            "value": true
                        }
                    ],
                    "expected": "StackOverflowError (or ValidationException indicating recursion limit exceeded)",
                    "description": "The schema contains a $ref that ultimately references itself, creating an infinite resolution loop. The walk implementation detects this and throws a StackOverflowError (or a custom ValidationException if recursion depth is limited)."
                }
            ]
        },
        {
            "group": "Unsupported schema node type (number) causing IllegalArgumentException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "node",
                            "value": "123"
                        },
                        {
                            "parameter": "shouldValidateSchema",
                            "value": true
                        }
                    ],
                    "expected": "IllegalArgumentException (message contains 'Schema node must be an object or boolean')",
                    "description": "The root schema node is a numeric literal instead of an object or boolean. walk validates the schema structure before processing the instance and throws IllegalArgumentException."
                }
            ]
        },
        {
            "group": "Null schema node leading to NullPointerException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "node",
                            "value": "{\"name\":\"John\"}"
                        },
                        {
                            "parameter": "shouldValidateSchema",
                            "value": true
                        }
                    ],
                    "expected": "NullPointerException (message contains 'schemaNode is null')",
                    "description": "A JsonSchema instance is created with a null schemaNode (e.g., via JsonSchema.from(..., null, ...)). When walk is invoked the internal code dereferences the schemaNode, resulting in a NullPointerException."
                }
            ]
        }
    ]
}
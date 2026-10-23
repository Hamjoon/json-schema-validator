{
    "result": [
        {
            "group": "walk_malformedRefUri",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "ExecutionContext created for schema {\"$ref\":\"ht!tp://[invalid]\"}"
                        },
                        {
                            "parameter": "node",
                            "value": "JsonNode representing {} (empty object)"
                        },
                        {
                            "parameter": "shouldValidateSchema",
                            "value": "true"
                        }
                    ],
                    "expected": "URISyntaxException (or IllegalArgumentException) is thrown due to malformed URI in $ref",
                    "description": "The schema contains a $ref with an invalid URI syntax; walk should fail while trying to resolve the reference."
                }
            ]
        },
        {
            "group": "walk_unresolvableExternalRef",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "ExecutionContext created for schema {\"$ref\":\"http://nonexistent.example.com/schema.json\"}"
                        },
                        {
                            "parameter": "node",
                            "value": "JsonNode representing {} (empty object)"
                        },
                        {
                            "parameter": "shouldValidateSchema",
                            "value": "true"
                        }
                    ],
                    "expected": "JsonSchemaException (or IllegalArgumentException) is thrown because the external $ref cannot be retrieved",
                    "description": "The $ref points to an external resource that does not exist; walk should raise an exception indicating that the reference cannot be resolved."
                }
            ]
        },
        {
            "group": "walk_circularRefDetection",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "ExecutionContext created for schema {\"definitions\":{\"A\":{\"$ref\":\"#/definitions/B\"},\"B\":{\"$ref\":\"#/definitions/A\"}},\"$ref\":\"#/definitions/A\"}"
                        },
                        {
                            "parameter": "node",
                            "value": "JsonNode representing {} (empty object)"
                        },
                        {
                            "parameter": "shouldValidateSchema",
                            "value": "true"
                        }
                    ],
                    "expected": "IllegalStateException (or StackOverflowError) is thrown due to circular $ref resolution",
                    "description": "The schema contains mutually referencing definitions that create a circular reference; walk should detect the cycle and abort with an exception."
                }
            ]
        },
        {
            "group": "walk_unsupportedKeyword",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "ExecutionContext created for schema {\"type\":\"object\",\"properties\":{\"age\":{\"type\":\"integer\"}},\"unknownKeyword\":123}"
                        },
                        {
                            "parameter": "node",
                            "value": "JsonNode representing {\"age\":30}"
                        },
                        {
                            "parameter": "shouldValidateSchema",
                            "value": "true"
                        }
                    ],
                    "expected": "JsonSchemaException (or IllegalArgumentException) is thrown because the schema contains an unsupported keyword",
                    "description": "The schema includes a keyword that the validator does not recognize; walk should raise an exception indicating the unsupported keyword."
                }
            ]
        },
        {
            "group": "walk_schemaNotObject",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "ExecutionContext created for schema that is a JSON array: [{\"type\":\"string\"}]"
                        },
                        {
                            "parameter": "node",
                            "value": "JsonNode representing \"some string\""
                        },
                        {
                            "parameter": "shouldValidateSchema",
                            "value": "true"
                        }
                    ],
                    "expected": "IllegalArgumentException is thrown because the root schema node is not a JSON object",
                    "description": "The supplied schema is an array rather than an object; walk expects an object schema and should reject this with an IllegalArgumentException."
                }
            ]
        }
    ]
}
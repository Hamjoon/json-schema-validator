{
    "groups": [
        {
            "group": "Non‑object schemaNode",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaNode",
                            "value": "[\"item1\", \"item2\"]"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "IllegalArgumentException (or NullPointerException) thrown because the schemaNode is not a JSON object",
                    "description": "The factory expects the root schema node to be an object; providing an array should trigger an argument‑validation exception."
                },
                {
                    "input": [
                        {
                            "parameter": "schemaNode",
                            "value": "42"
                        },
                        {
                            "parameter": "config",
                            "value": "null"
                        }
                    ],
                    "expected": "IllegalArgumentException (or NullPointerException) thrown because the schemaNode is a primitive value",
                    "description": "Supplying a numeric primitive instead of an object should be rejected by createValidationContext."
                }
            ]
        },
        {
            "group": "Non‑text $schema value",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaNode",
                            "value": "{\"$schema\":123,\"type\":\"object\"}"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "IllegalArgumentException thrown when $schema field is not a text node",
                    "description": "The $schema attribute must be a string; providing a numeric value should cause the method to raise an exception during meta‑schema resolution."
                },
                {
                    "input": [
                        {
                            "parameter": "schemaNode",
                            "value": "{\"$schema\":true,\"type\":\"object\"}"
                        },
                        {
                            "parameter": "config",
                            "value": "null"
                        }
                    ],
                    "expected": "IllegalArgumentException thrown when $schema field is a boolean",
                    "description": "A boolean $schema value is invalid and should be detected, resulting in an exception."
                },
                {
                    "input": [
                        {
                            "parameter": "schemaNode",
                            "value": "{\"$schema\":{},\"type\":\"object\"}"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "IllegalArgumentException thrown when $schema field is an object",
                    "description": "When $schema is a JSON object instead of a string, the factory cannot resolve a meta‑schema and must fail."
                }
            ]
        },
        {
            "group": "Malformed $schema URI",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaNode",
                            "value": "{\"$schema\":\"http://[invalid]/schema#\",\"type\":\"object\"}"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "IllegalArgumentException thrown because the $schema URI is syntactically invalid",
                    "description": "A $schema value that does not conform to URI syntax should cause the factory to raise an exception rather than silently falling back."
                },
                {
                    "input": [
                        {
                            "parameter": "schemaNode",
                            "value": "{\"$schema\":\"ht!tp://bad^uri\",\"type\":\"object\"}"
                        },
                        {
                            "parameter": "config",
                            "value": "null"
                        }
                    ],
                    "expected": "IllegalArgumentException thrown due to malformed URI in $schema",
                    "description": "Even when config is null, a malformed $schema string must be detected and result in an exception."
                }
            ]
        }
    ]
}
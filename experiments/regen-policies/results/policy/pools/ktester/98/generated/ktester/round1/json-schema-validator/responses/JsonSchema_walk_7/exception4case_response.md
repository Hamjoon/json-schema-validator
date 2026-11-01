{
    "groups": [
        {
            "group": "Null Input String",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "new ExecutionContext()"
                        },
                        {
                            "parameter": "input",
                            "value": "null"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "validate",
                            "value": "true"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "When the supplied JSON string is null the method should fail early because deserialize cannot operate on a null reference."
                }
            ]
        },
        {
            "group": "Empty Input String",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "new ExecutionContext()"
                        },
                        {
                            "parameter": "input",
                            "value": "\"\""
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "validate",
                            "value": "true"
                        }
                    ],
                    "expected": "IOException (or JsonProcessingException) wrapped in JsonSchemaException",
                    "description": "An empty JSON string cannot be parsed; deserialize throws an IOException which should be propagated/wrapped by walk."
                }
            ]
        },
        {
            "group": "Invalid Percent Encoding – UnsupportedEncodingException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "new ExecutionContext()"
                        },
                        {
                            "parameter": "input",
                            "value": "\"%ZZ\""
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "validate",
                            "value": "true"
                        }
                    ],
                    "expected": "UnsupportedEncodingException wrapped in JsonSchemaException",
                    "description": "If the input string contains malformed percent‑encoding, URLDecoder.decode (used inside deserialize) throws UnsupportedEncodingException; the method must surface this as a wrapped exception."
                }
            ]
        },
        {
            "group": "Unsupported InputFormat Value",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "new ExecutionContext()"
                        },
                        {
                            "parameter": "input",
                            "value": "\"<xml></xml>\""
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.XML"
                        },
                        {
                            "parameter": "validate",
                            "value": "true"
                        }
                    ],
                    "expected": "IllegalArgumentException (or custom SchemaException) indicating unsupported format",
                    "description": "Passing an InputFormat value that the library does not support (e.g., XML) should trigger the error handling path in deserialize."
                }
            ]
        }
    ]
}
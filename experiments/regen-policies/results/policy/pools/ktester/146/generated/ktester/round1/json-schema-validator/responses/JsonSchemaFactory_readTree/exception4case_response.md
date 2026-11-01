{
    "test_cases": [
        {
            "group": "jsonNodeReader is null – unsupported InputFormat",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "content",
                            "value": "{\"key\":\"value\"}"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.XML"
                        }
                    ],
                    "expected": "IllegalArgumentException thrown by getObjectMapper because the InputFormat is not supported",
                    "description": "When jsonNodeReader is null and an InputFormat value that the factory does not recognise (e.g., XML) is supplied, getObjectMapper should reject it and throw IllegalArgumentException, which must propagate out of readTree."
                }
            ]
        },
        {
            "group": "jsonNodeReader is set – unchecked exception propagation",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "content",
                            "value": "{\"id\":1}"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        }
                    ],
                    "expected": "RuntimeException (e.g., IllegalStateException) thrown by JsonNodeReader.readTree propagates unchanged",
                    "description": "When a custom JsonNodeReader is injected, if its readTree method throws a runtime exception (not declared in the signature), readTree must let that exception bubble up unchanged."
                },
                {
                    "input": [
                        {
                            "parameter": "content",
                            "value": null
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.YAML"
                        }
                    ],
                    "expected": "NullPointerException thrown by JsonNodeReader.readTree propagates unchanged",
                    "description": "Even though the method signature declares IOException, a JsonNodeReader implementation may throw a NullPointerException for a null content argument; the factory must not catch it and should let it propagate."
                }
            ]
        }
    ]
}
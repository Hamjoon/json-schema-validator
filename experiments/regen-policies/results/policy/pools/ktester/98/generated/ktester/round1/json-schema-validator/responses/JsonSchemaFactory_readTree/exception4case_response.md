{
    "test_cases": [
        {
            "group": "jsonNodeReader is non‑null – null content handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "content",
                            "value": null
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "JSON"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "When a custom JsonNodeReader is set, passing a null content string should cause the reader to throw NullPointerException, which must be propagated by readTree."
                }
            ]
        },
        {
            "group": "jsonNodeReader is non‑null – null InputFormat handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "content",
                            "value": "{\"key\":\"value\"}"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": null
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "When a custom JsonNodeReader is set, a null InputFormat argument should result in NullPointerException (or IllegalArgumentException) from the reader, which must be propagated."
                }
            ]
        },
        {
            "group": "jsonNodeReader is non‑null – custom reader runtime exception propagation",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "content",
                            "value": "{\"trigger\":\"runtime\"}"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "JSON"
                        }
                    ],
                    "expected": "IllegalStateException",
                    "description": "If the custom JsonNodeReader throws a runtime exception such as IllegalStateException, readTree must not catch it and should let it propagate to the caller."
                }
            ]
        },
        {
            "group": "jsonNodeReader is non‑null – empty content handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "content",
                            "value": ""
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "JSON"
                        }
                    ],
                    "expected": "IOException",
                    "description": "When a custom JsonNodeReader receives an empty string, it should behave like the default ObjectMapper and throw an IOException, which must be propagated."
                }
            ]
        }
    ]
}
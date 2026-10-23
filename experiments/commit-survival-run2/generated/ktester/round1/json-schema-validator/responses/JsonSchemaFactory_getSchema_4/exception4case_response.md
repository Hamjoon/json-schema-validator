{
    "test_cases": [
        {
            "group": "Unsupported URI Scheme",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schema",
                            "value": "ftp://example.com/schema.json"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "The factory should reject URIs with unsupported schemes (e.g., ftp) and throw an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "Unreachable Remote Resource",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schema",
                            "value": "http://nonexistent.invalid/schema.json"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "When the URI points to a remote resource that cannot be reached, the underlying I/O error should be wrapped and reported as an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "Unresolved $ref Reference",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schema",
                            "value": "{\"type\":\"object\",\"properties\":{\"missing\":{\"$ref\":\"classpath:missing-ref.json\"}}}"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "A schema containing a $ref to a non‑existent classpath resource must cause the factory to throw an IllegalArgumentException indicating the reference cannot be resolved."
                }
            ]
        },
        {
            "group": "Cyclic $ref Detection",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schema",
                            "value": "{\"$ref\":\"#\"}"
                        }
                    ],
                    "expected": "StackOverflowError",
                    "description": "A schema that references itself directly ($ref to '#') creates a cyclic reference; the factory should eventually overflow the call stack, resulting in a StackOverflowError."
                }
            ]
        },
        {
            "group": "Non‑existent File URI",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schema",
                            "value": "file:/nonexistent/path/schema.json"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "A file URI that points to a location that does not exist must be rejected with an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "Directory Resource on Classpath",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schema",
                            "value": "classpath:/schemas/"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "When the classpath location resolves to a directory rather than a file, the factory should throw an IllegalArgumentException because a schema document cannot be read from a directory."
                }
            ]
        }
    ]
}
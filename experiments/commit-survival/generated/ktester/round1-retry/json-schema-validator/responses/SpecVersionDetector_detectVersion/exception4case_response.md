{
    "groups": [
        {
            "group": "SpecVersionDetector.detectVersion - Null jsonNode causes NullPointerException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "jsonNode",
                            "value": "null"
                        },
                        {
                            "parameter": "specification",
                            "value": "/tmp/schemas/draft-07/schema.json"
                        },
                        {
                            "parameter": "defaultVersion",
                            "value": "VersionFlag.V4"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "When jsonNode is null the internal call to detectOptionalVersion is expected to throw a NullPointerException, which propagates out of detectVersion."
                }
            ]
        },
        {
            "group": "SpecVersionDetector.detectVersion - Null specification causes NullPointerException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "jsonNode",
                            "value": "{ \"\\$schema\": \"http://json-schema.org/draft-07/schema#\" }"
                        },
                        {
                            "parameter": "specification",
                            "value": "null"
                        },
                        {
                            "parameter": "defaultVersion",
                            "value": "VersionFlag.V4"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "When specification (Path) is null the internal call to detectVersionFromPath is expected to throw a NullPointerException, which propagates out of detectVersion."
                }
            ]
        },
        {
            "group": "SpecVersionDetector.detectVersion - Invalid Path throws IllegalArgumentException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "jsonNode",
                            "value": "{}"
                        },
                        {
                            "parameter": "specification",
                            "value": ":::invalid_path:::"
                        },
                        {
                            "parameter": "defaultVersion",
                            "value": "VersionFlag.V4"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "A malformed string that cannot be converted to a valid Path should cause detectVersionFromPath to throw an IllegalArgumentException, which propagates out of detectVersion."
                }
            ]
        },
        {
            "group": "SpecVersionDetector.detectVersion - detectOptionalVersion throws RuntimeException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "jsonNode",
                            "value": "{ \"invalid\": \"@@@\" }"
                        },
                        {
                            "parameter": "specification",
                            "value": "/tmp/schemas/draft-07/schema.json"
                        },
                        {
                            "parameter": "defaultVersion",
                            "value": "VersionFlag.V4"
                        }
                    ],
                    "expected": "RuntimeException",
                    "description": "If the JSON node is malformed in a way that detectOptionalVersion cannot process (e.g., unexpected token), it may throw a RuntimeException. The test verifies that such an exception propagates from detectVersion."
                }
            ]
        }
    ]
}
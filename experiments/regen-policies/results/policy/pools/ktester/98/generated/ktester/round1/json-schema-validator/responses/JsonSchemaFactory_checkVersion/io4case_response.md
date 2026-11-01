{
    "testCases": [
        {
            "group": "Instance uniqueness",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "V202012"
                        }
                    ],
                    "expected": "different instance on each call",
                    "description": "Calling checkVersion twice with V202012 should return two distinct Version202012 objects (object identity must differ)."
                },
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "V201909"
                        }
                    ],
                    "expected": "different instance on each call",
                    "description": "Calling checkVersion twice with V201909 should return two distinct Version201909 objects."
                },
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "V7"
                        }
                    ],
                    "expected": "different instance on each call",
                    "description": "Calling checkVersion twice with V7 should return two distinct Version7 objects."
                },
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "V6"
                        }
                    ],
                    "expected": "different instance on each call",
                    "description": "Calling checkVersion twice with V6 should return two distinct Version6 objects."
                },
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "V4"
                        }
                    ],
                    "expected": "different instance on each call",
                    "description": "Calling checkVersion twice with V4 should return two distinct Version4 objects."
                }
            ]
        },
        {
            "group": "Exception message detail",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "UNSUPPORTED"
                        }
                    ],
                    "expected": "IllegalArgumentException with message \"Unsupported valueUNSUPPORTED\"",
                    "description": "When an enum constant not covered by the switch (e.g., UNSUPPORTED) is supplied, the method must throw IllegalArgumentException and the message must contain the exact concatenated text \"Unsupported value\" followed by the enum name."
                }
            ]
        },
        {
            "group": "Return type compliance",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "V202012"
                        }
                    ],
                    "expected": "instance of com.networknt.schema.JsonSchemaVersion",
                    "description": "The object returned for V202012 must implement the JsonSchemaVersion interface."
                },
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "V201909"
                        }
                    ],
                    "expected": "instance of com.networknt.schema.JsonSchemaVersion",
                    "description": "The object returned for V201909 must implement the JsonSchemaVersion interface."
                },
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "V7"
                        }
                    ],
                    "expected": "instance of com.networknt.schema.JsonSchemaVersion",
                    "description": "The object returned for V7 must implement the JsonSchemaVersion interface."
                },
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "V6"
                        }
                    ],
                    "expected": "instance of com.networknt.schema.JsonSchemaVersion",
                    "description": "The object returned for V6 must implement the JsonSchemaVersion interface."
                },
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "V4"
                        }
                    ],
                    "expected": "instance of com.networknt.schema.JsonSchemaVersion",
                    "description": "The object returned for V4 must implement the JsonSchemaVersion interface."
                }
            ]
        }
    ]
}
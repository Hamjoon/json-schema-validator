{
    "testCases": [
        {
            "group": "checkVersion_validInstances",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "V202012"
                        }
                    ],
                    "expected": "instance of class Version202012",
                    "description": "When versionFlag is V202012 the method should return a non‑null object whose runtime class is com.networknt.schema.Version202012."
                },
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "V201909"
                        }
                    ],
                    "expected": "instance of class Version201909",
                    "description": "When versionFlag is V201909 the method should return a non‑null object whose runtime class is com.networknt.schema.Version201909."
                },
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "V7"
                        }
                    ],
                    "expected": "instance of class Version7",
                    "description": "When versionFlag is V7 the method should return a non‑null object whose runtime class is com.networknt.schema.Version7."
                },
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "V6"
                        }
                    ],
                    "expected": "instance of class Version6",
                    "description": "When versionFlag is V6 the method should return a non‑null object whose runtime class is com.networknt.schema.Version6."
                },
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "V4"
                        }
                    ],
                    "expected": "instance of class Version4",
                    "description": "When versionFlag is V4 the method should return a non‑null object whose runtime class is com.networknt.schema.Version4."
                }
            ]
        },
        {
            "group": "checkVersion_instanceUniqueness",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "V202012"
                        }
                    ],
                    "expected": "two distinct objects",
                    "description": "Calling checkVersion(V202012) twice must produce two different instances (no caching), i.e., the references are not equal."
                },
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "V201909"
                        }
                    ],
                    "expected": "two distinct objects",
                    "description": "Calling checkVersion(V201909) twice must produce two different instances."
                },
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "V7"
                        }
                    ],
                    "expected": "two distinct objects",
                    "description": "Calling checkVersion(V7) twice must produce two different instances."
                },
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "V6"
                        }
                    ],
                    "expected": "two distinct objects",
                    "description": "Calling checkVersion(V6) twice must produce two different instances."
                },
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "V4"
                        }
                    ],
                    "expected": "two distinct objects",
                    "description": "Calling checkVersion(V4) twice must produce two different instances."
                }
            ]
        },
        {
            "group": "checkVersion_unsupportedExceptionMessage",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "UNSUPPORTED"
                        }
                    ],
                    "expected": "IllegalArgumentException with message containing 'UNSUPPORTED'",
                    "description": "When an enum constant that is not handled by the switch (e.g., a mock constant named UNSUPPORTED) is supplied, the method must throw IllegalArgumentException and the exception message must include the string representation of the unsupported value."
                }
            ]
        }
    ]
}
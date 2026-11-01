{
    "testCases": [
        {
            "group": "Null handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": null
                        }
                    ],
                    "expected": null,
                    "description": "When the supplied versionFlag is null the method should return null."
                }
            ]
        },
        {
            "group": "Supported version flags",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "V202012"
                        }
                    ],
                    "expected": "instance of com.networknt.schema.Version202012",
                    "description": "V202012 flag must map to a new Version202012 object."
                },
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "V201909"
                        }
                    ],
                    "expected": "instance of com.networknt.schema.Version201909",
                    "description": "V201909 flag must map to a new Version201909 object."
                },
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "V7"
                        }
                    ],
                    "expected": "instance of com.networknt.schema.Version7",
                    "description": "V7 flag must map to a new Version7 object."
                },
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "V6"
                        }
                    ],
                    "expected": "instance of com.networknt.schema.Version6",
                    "description": "V6 flag must map to a new Version6 object."
                },
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "V4"
                        }
                    ],
                    "expected": "instance of com.networknt.schema.Version4",
                    "description": "V4 flag must map to a new Version4 object."
                }
            ]
        },
        {
            "group": "Unsupported version flag (default branch)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "UNSUPPORTED"
                        }
                    ],
                    "expected": "IllegalArgumentException with message containing \"Unsupported value\"",
                    "description": "When a versionFlag value that is not handled by the switch is supplied, the method must throw IllegalArgumentException."
                }
            ]
        }
    ]
}
{
    "testCases": [
        {
            "group": "checkVersion - supported flags",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "SpecVersion.VersionFlag.V202012"
                        }
                    ],
                    "expected": "Version202012",
                    "description": "Supplies V202012 flag – method should return a new instance of Version202012."
                },
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "SpecVersion.VersionFlag.V201909"
                        }
                    ],
                    "expected": "Version201909",
                    "description": "Supplies V201909 flag – method should return a new instance of Version201909."
                },
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "SpecVersion.VersionFlag.V7"
                        }
                    ],
                    "expected": "Version7",
                    "description": "Supplies V7 flag – method should return a new instance of Version7."
                },
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "SpecVersion.VersionFlag.V6"
                        }
                    ],
                    "expected": "Version6",
                    "description": "Supplies V6 flag – method should return a new instance of Version6."
                },
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "SpecVersion.VersionFlag.V4"
                        }
                    ],
                    "expected": "Version4",
                    "description": "Supplies V4 flag – method should return a new instance of Version4."
                }
            ]
        },
        {
            "group": "checkVersion - illegal argument for unknown flag",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "UNKNOWN_ENUM_VALUE"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "Passes a value that is not part of the defined enum (simulated via reflection or a mock) – method should throw IllegalArgumentException."
                }
            ]
        }
    ]
}
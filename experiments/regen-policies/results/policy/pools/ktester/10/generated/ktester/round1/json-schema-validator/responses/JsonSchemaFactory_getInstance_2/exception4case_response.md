{
    "testCases": [
        {
            "group": "customizer throws NullPointerException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "SpecVersion.VersionFlag.V4"
                        },
                        {
                            "parameter": "customizer",
                            "value": "builder -> { throw new NullPointerException(\"customizer NPE\"); }"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "The customizer lambda throws a NullPointerException; getInstance should propagate this exception."
                }
            ]
        },
        {
            "group": "customizer throws IllegalArgumentException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "SpecVersion.VersionFlag.V6"
                        },
                        {
                            "parameter": "customizer",
                            "value": "builder -> { throw new IllegalArgumentException(\"customizer illegal arg\"); }"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "The customizer lambda throws an IllegalArgumentException; getInstance must not catch it and should let it propagate."
                }
            ]
        },
        {
            "group": "customizer throws custom RuntimeException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "SpecVersion.VersionFlag.V7"
                        },
                        {
                            "parameter": "customizer",
                            "value": "builder -> { throw new RuntimeException(\"customizer failure\"); }"
                        }
                    ],
                    "expected": "RuntimeException",
                    "description": "A customizer that throws a generic RuntimeException should cause getInstance to abort and propagate the same exception."
                }
            ]
        }
    ]
}
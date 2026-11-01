{
    "testCases": [
        {
            "group": "Customizer throws IllegalArgumentException - should propagate",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "SpecVersion.VersionFlag.V7"
                        },
                        {
                            "parameter": "customizer",
                            "value": "builder -> { throw new IllegalArgumentException(\"customizer error\"); }"
                        }
                    ],
                    "expected": "IllegalArgumentException with message \"customizer error\"",
                    "description": "When a non‑null customizer throws an IllegalArgumentException, getInstance(versionFlag, customizer) must propagate that exception unchanged."
                }
            ]
        },
        {
            "group": "Customizer throws RuntimeException - should propagate",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "SpecVersion.VersionFlag.V6"
                        },
                        {
                            "parameter": "customizer",
                            "value": "builder -> { throw new RuntimeException(\"unexpected failure\"); }"
                        }
                    ],
                    "expected": "RuntimeException with message \"unexpected failure\"",
                    "description": "If the customizer lambda throws a generic RuntimeException, the method must not catch it and should let it bubble up to the caller."
                }
            ]
        },
        {
            "group": "Customiser sets null version flag - should raise IllegalArgumentException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "SpecVersion.VersionFlag.V4"
                        },
                        {
                            "parameter": "customizer",
                            "value": "builder -> { builder.versionFlag(null); }"
                        }
                    ],
                    "expected": "IllegalArgumentException with message containing \"versionFlag must not be null\"",
                    "description": "A customizer that attempts to set a null version flag on the builder must cause an IllegalArgumentException, mirroring the validation performed in the factory method."
                }
            ]
        },
        {
            "group": "Internal meta‑schema loading failure - should throw IllegalStateException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "SpecVersion.VersionFlag.DRAFT_2020_12"
                        },
                        {
                            "parameter": "customizer",
                            "value": "builder -> { /* mock JsonSchemaFactory.loadMetaSchema to throw IOException */ }"
                        }
                    ],
                    "expected": "IllegalStateException with cause IOException",
                    "description": "If loading the meta‑schema (e.g., reading a resource file) fails with an IOException, the factory should wrap it in an IllegalStateException. The test uses a mocking framework to make loadMetaSchema throw IOException and verifies the wrapper exception."
                }
            ]
        }
    ]
}
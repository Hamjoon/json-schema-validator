{
    "testCases": [
        {
            "group": "Valid version flags - should return a non‑null factory",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "SpecVersion.VersionFlag.V4"
                        }
                    ],
                    "expected": "non‑null JsonSchemaFactory instance whose meta‑schema id is \"http://json-schema.org/draft-04/schema#\"",
                    "description": "When a supported Draft‑04 flag is supplied, getInstance should create a factory for Draft‑04 and never return null."
                },
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "SpecVersion.VersionFlag.V6"
                        }
                    ],
                    "expected": "non‑null JsonSchemaFactory instance whose meta‑schema id is \"http://json-schema.org/draft-06/schema#\"",
                    "description": "When a supported Draft‑06 flag is supplied, the method must return a factory configured for Draft‑06."
                },
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "SpecVersion.VersionFlag.V7"
                        }
                    ],
                    "expected": "non‑null JsonSchemaFactory instance whose meta‑schema id is \"http://json-schema.org/draft-07/schema#\"",
                    "description": "When a supported Draft‑07 flag is supplied, the method must return a factory configured for Draft‑07."
                },
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "SpecVersion.VersionFlag.DRAFT_2019_09"
                        }
                    ],
                    "expected": "non‑null JsonSchemaFactory instance whose meta‑schema id is \"https://json-schema.org/draft/2019-09/schema#\"",
                    "description": "When a supported 2019‑09 flag is supplied, the method must return a factory configured for that version."
                }
            ]
        },
        {
            "group": "Invalid inputs - should throw IllegalArgumentException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "null"
                        }
                    ],
                    "expected": "IllegalArgumentException with message containing \"versionFlag must not be null\"",
                    "description": "Passing a null version flag must be rejected and result in an IllegalArgumentException."
                },
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "SpecVersion.VersionFlag.UNKNOWN"
                        }
                    ],
                    "expected": "IllegalArgumentException with message containing \"Unsupported version flag\"",
                    "description": "If an enum constant that is not mapped to a known JSON‑Schema draft is supplied, the method must fail fast with an IllegalArgumentException."
                }
            ]
        }
    ]
}
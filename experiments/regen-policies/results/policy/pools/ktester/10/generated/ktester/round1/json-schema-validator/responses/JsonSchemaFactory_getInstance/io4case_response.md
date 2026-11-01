{
    "testCases": [
        {
            "group": "Additional supported version flags - should return a non‑null factory",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "SpecVersion.VersionFlag.DRAFT_2020_12"
                        }
                    ],
                    "expected": "non‑null JsonSchemaFactory instance whose meta‑schema id is \"https://json-schema.org/draft/2020-12/schema#\"",
                    "description": "When the Draft‑2020‑12 flag is supplied, getInstance must create a factory for Draft‑2020‑12 and never return null."
                }
            ]
        },
        {
            "group": "Factory instance uniqueness - repeated calls with same flag",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "SpecVersion.VersionFlag.V7"
                        }
                    ],
                    "expected": "two distinct JsonSchemaFactory instances (different object references) returned on successive calls",
                    "description": "Calling getInstance with the same supported flag multiple times should produce separate factory objects (no caching of a singleton)."
                }
            ]
        },
        {
            "group": "Custom builder interaction via overload - null customizer should behave like simple call",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "versionFlag",
                            "value": "SpecVersion.VersionFlag.V6"
                        }
                    ],
                    "expected": "non‑null JsonSchemaFactory instance identical in configuration to the one returned by getInstance(versionFlag) without customizer",
                    "description": "When the overload getInstance(versionFlag, null) is used internally, it must produce the same result as the single‑argument version; this case verifies that the factory’s meta‑schema id matches the expected Draft‑06 identifier."
                }
            ]
        }
    ]
}
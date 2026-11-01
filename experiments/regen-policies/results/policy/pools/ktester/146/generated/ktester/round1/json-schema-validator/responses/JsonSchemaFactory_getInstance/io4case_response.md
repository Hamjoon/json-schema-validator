{
    "group": "JsonSchemaFactory.getInstance(SpecVersion.VersionFlag) - edge cases and caching",
    "cases": [
        {
            "input": [
                {
                    "parameter": "versionFlag",
                    "value": "null"
                }
            ],
            "expected": "Throws NullPointerException (or IllegalArgumentException) because a null version flag is not allowed.",
            "description": "Verify that the factory method validates its argument and fails fast when a null VersionFlag is supplied."
        },
        {
            "input": [
                {
                    "parameter": "versionFlag",
                    "value": "SpecVersion.VersionFlag.V7"
                }
            ],
            "expected": "Two successive calls with the same flag return the identical JsonSchemaFactory instance (reference equality).",
            "description": "Check that the factory caches instances per version flag, so repeated calls with V7 yield the same object."
        },
        {
            "input": [
                {
                    "parameter": "versionFlag",
                    "value": "SpecVersion.VersionFlag.V4"
                }
            ],
            "expected": "Two factories created with different flags (V4 vs V6) are not the same instance (reference inequality).",
            "description": "Ensure that factories for different draft versions are distinct objects."
        },
        {
            "input": [
                {
                    "parameter": "versionFlag",
                    "value": "SpecVersion.VersionFlag.DRAFT_2020_12"
                }
            ],
            "expected": "When invoked concurrently from multiple threads, all calls return the same JsonSchemaFactory instance (reference equality).",
            "description": "Validate thread‑safety of the caching mechanism by spawning several threads that each call getInstance(DRAFT_2020_12) and asserting that the returned references are identical."
        }
    ]
}
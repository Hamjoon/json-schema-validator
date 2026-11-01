{
    "group": "checkVersion_additional",
    "cases": [
        {
            "input": [
                {
                    "parameter": "versionFlag",
                    "value": "SpecVersion.VersionFlag.V4"
                }
            ],
            "expected": "new instance each call (different object references)",
            "description": "Calling checkVersion twice with the same supported flag (V4) should return two distinct objects, confirming that the method does not cache instances."
        },
        {
            "input": [
                {
                    "parameter": "versionFlag",
                    "value": "SpecVersion.VersionFlag.V7"
                }
            ],
            "expected": "instance implements com.networknt.schema.JsonSchemaVersion",
            "description": "Even though the return type is JsonSchemaVersion, verify at runtime that the created object indeed implements this interface for a supported flag (V7)."
        },
        {
            "input": [
                {
                    "parameter": "versionFlag",
                    "value": "SpecVersion.VersionFlag.UNSUPPORTED"
                }
            ],
            "expected": "IllegalArgumentException with exact message \"Unsupported valueUNSUPPORTED\"",
            "description": "Validate that the IllegalArgumentException thrown for an unsupported enum constant contains the exact expected message (no extra whitespace)."
        },
        {
            "input": [
                {
                    "parameter": "versionFlag",
                    "value": "SpecVersion.VersionFlag.V202012"
                }
            ],
            "expected": "instance of com.networknt.schema.Version202012 and not null",
            "description": "Confirm that a supported flag (V202012) returns a non‑null object of the correct concrete class."
        },
        {
            "input": [
                {
                    "parameter": "versionFlag",
                    "value": "SpecVersion.VersionFlag.V6"
                }
            ],
            "expected": "instance of com.networknt.schema.Version6 and not null",
            "description": "Confirm that a supported flag (V6) returns a non‑null object of the correct concrete class."
        }
    ]
}
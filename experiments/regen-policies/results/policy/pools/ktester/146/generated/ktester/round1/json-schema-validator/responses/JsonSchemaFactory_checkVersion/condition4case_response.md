{
    "group": "checkVersion",
    "cases": [
        {
            "input": [
                {
                    "parameter": "versionFlag",
                    "value": null
                }
            ],
            "expected": "null",
            "description": "When the versionFlag argument is null the method should return null."
        },
        {
            "input": [
                {
                    "parameter": "versionFlag",
                    "value": "SpecVersion.VersionFlag.V202012"
                }
            ],
            "expected": "instance of com.networknt.schema.Version202012",
            "description": "VersionFlag V202012 should map to a new Version202012 object."
        },
        {
            "input": [
                {
                    "parameter": "versionFlag",
                    "value": "SpecVersion.VersionFlag.V201909"
                }
            ],
            "expected": "instance of com.networknt.schema.Version201909",
            "description": "VersionFlag V201909 should map to a new Version201909 object."
        },
        {
            "input": [
                {
                    "parameter": "versionFlag",
                    "value": "SpecVersion.VersionFlag.V7"
                }
            ],
            "expected": "instance of com.networknt.schema.Version7",
            "description": "VersionFlag V7 should map to a new Version7 object."
        },
        {
            "input": [
                {
                    "parameter": "versionFlag",
                    "value": "SpecVersion.VersionFlag.V6"
                }
            ],
            "expected": "instance of com.networknt.schema.Version6",
            "description": "VersionFlag V6 should map to a new Version6 object."
        },
        {
            "input": [
                {
                    "parameter": "versionFlag",
                    "value": "SpecVersion.VersionFlag.V4"
                }
            ],
            "expected": "instance of com.networknt.schema.Version4",
            "description": "VersionFlag V4 should map to a new Version4 object."
        },
        {
            "input": [
                {
                    "parameter": "versionFlag",
                    "value": "SpecVersion.VersionFlag.UNSUPPORTED"
                }
            ],
            "expected": "IllegalArgumentException with message containing \"Unsupported valueUNSUPPORTED\"",
            "description": "Any VersionFlag value that is not explicitly handled should cause an IllegalArgumentException."
        }
    ]
}
{
    "group": "JsonSchemaFactory.getInstance(SpecVersion.VersionFlag, Consumer) - customizer exception propagation",
    "cases": [
        {
            "input": [
                {
                    "parameter": "versionFlag",
                    "value": "SpecVersion.VersionFlag.V7"
                },
                {
                    "parameter": "customizer",
                    "value": "consumer -> { throw new IllegalStateException(\"customizer failure\"); }"
                }
            ],
            "expected": "IllegalStateException with message \"customizer failure\" is thrown and propagates out of getInstance.",
            "description": "Verify that a runtime exception thrown inside the customizer Consumer is not swallowed by getInstance and is re‑thrown to the caller."
        },
        {
            "input": [
                {
                    "parameter": "versionFlag",
                    "value": "SpecVersion.VersionFlag.V7"
                },
                {
                    "parameter": "customizer",
                    "value": "consumer -> { throw new NullPointerException(\"null inside customizer\"); }"
                }
            ],
            "expected": "NullPointerException with message \"null inside customizer\" is thrown.",
            "description": "Ensure that any exception type (including NullPointerException) raised by the customizer is propagated unchanged."
        }
    ]
}
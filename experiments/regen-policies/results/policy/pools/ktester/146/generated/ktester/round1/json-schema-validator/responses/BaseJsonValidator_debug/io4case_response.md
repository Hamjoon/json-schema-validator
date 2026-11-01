{
    "group": "debug method - execution config debug disabled",
    "cases": [
        {
            "input": [
                {
                    "parameter": "logger",
                    "value": "mock Logger where isDebugEnabled() returns true and captures debug(String) calls"
                },
                {
                    "parameter": "executionContext",
                    "value": "mock ExecutionContext where getExecutionConfig().isDebugEnabled() returns false"
                },
                {
                    "parameter": "node",
                    "value": "mock JsonNode where toString() returns \"{\\\"field\\\":1}\""
                },
                {
                    "parameter": "rootNode",
                    "value": "mock JsonNode where toString() returns \"{\\\"root\\\":true}\""
                },
                {
                    "parameter": "instanceLocation",
                    "value": "mock JsonNodePath where toString() returns \"/data/item\""
                }
            ],
            "expected": "logger.debug is NOT invoked (zero interactions)",
            "description": "When the execution configuration disables debug, the method must skip logging even if the logger itself is debug‑enabled."
        }
    ]
}
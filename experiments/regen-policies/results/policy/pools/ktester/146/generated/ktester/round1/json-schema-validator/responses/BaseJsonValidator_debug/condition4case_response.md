{
    "group": "debug method - debug enabled",
    "cases": [
        {
            "input": [
                {
                    "parameter": "logger",
                    "value": "mock Logger where isDebugEnabled() returns true and captures debug(String) calls"
                },
                {
                    "parameter": "executionContext",
                    "value": "mock ExecutionContext where getExecutionConfig().isDebugEnabled() returns true"
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
            "expected": "logger.debug is invoked exactly once with the message: \"validate( {\\\"field\\\":1}, {\\\"root\\\":true}, /data/item)\"",
            "description": "When both execution config debug flag and logger debug flag are true, the method should build the expected string and forward it to logger.debug."
        }
    ]
}
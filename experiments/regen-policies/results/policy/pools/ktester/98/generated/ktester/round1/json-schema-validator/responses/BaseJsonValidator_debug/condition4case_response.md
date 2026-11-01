{
    "group": "BaseJsonValidator.debug logging behavior",
    "cases": [
        {
            "input": [
                { "parameter": "logger", "value": "mockLogger(debugEnabled=true)" },
                { "parameter": "executionContext", "value": "mockExecutionContext(debugEnabled=true)" },
                { "parameter": "node", "value": "mockJsonNode(toString=\"{\\\"a\\\":1}\")" },
                { "parameter": "rootNode", "value": "mockJsonNode(toString=\"{\\\"root\\\":true}\")" },
                { "parameter": "instanceLocation", "value": "mockJsonNodePath(toString=\"/data/item\")" }
            ],
            "expected": "logger.debug called once with string \"validate( {\\\"a\\\":1}, {\\\"root\\\":true}, /data/item)\"",
            "description": "Both execution config and logger are debug‑enabled, so the method should build the message and invoke logger.debug with the correctly formatted string."
        },
        {
            "input": [
                { "parameter": "logger", "value": "mockLogger(debugEnabled=false)" },
                { "parameter": "executionContext", "value": "mockExecutionContext(debugEnabled=true)" },
                { "parameter": "node", "value": "mockJsonNode(toString=\"node\")" },
                { "parameter": "rootNode", "value": "mockJsonNode(toString=\"root\")" },
                { "parameter": "instanceLocation", "value": "mockJsonNodePath(toString=\"/path\")" }
            ],
            "expected": "logger.debug never called",
            "description": "Execution config is debug‑enabled but the logger itself is not; the method must skip logging."
        },
        {
            "input": [
                { "parameter": "logger", "value": "mockLogger(debugEnabled=true)" },
                { "parameter": "executionContext", "value": "mockExecutionContext(debugEnabled=false)" },
                { "parameter": "node", "value": "mockJsonNode(toString=\"node\")" },
                { "parameter": "rootNode", "value": "mockJsonNode(toString=\"root\")" },
                { "parameter": "instanceLocation", "value": "mockJsonNodePath(toString=\"/path\")" }
            ],
            "expected": "logger.debug never called",
            "description": "Logger is debug‑enabled but the execution config disables debug; the method must not log."
        },
        {
            "input": [
                { "parameter": "logger", "value": "mockLogger(debugEnabled=false)" },
                { "parameter": "executionContext", "value": "mockExecutionContext(debugEnabled=false)" },
                { "parameter": "node", "value": "mockJsonNode(toString=\"node\")" },
                { "parameter": "rootNode", "value": "mockJsonNode(toString=\"root\")" },
                { "parameter": "instanceLocation", "value": "mockJsonNodePath(toString=\"/path\")" }
            ],
            "expected": "logger.debug never called",
            "description": "Both execution config and logger have debug disabled; the method must skip logging."
        }
    ]
}
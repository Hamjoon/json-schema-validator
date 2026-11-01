{
    "group": "BaseJsonValidator.debug null execution config handling",
    "cases": [
        {
            "input": [
                {
                    "parameter": "logger",
                    "value": "mockLogger(debugEnabled=true)"
                },
                {
                    "parameter": "executionContext",
                    "value": "mockExecutionContext(executionConfig=null, debugEnabled=true)"
                },
                {
                    "parameter": "node",
                    "value": "mockJsonNode(toString=\"{}\")"
                },
                {
                    "parameter": "rootNode",
                    "value": "mockJsonNode(toString=\"{}\")"
                },
                {
                    "parameter": "instanceLocation",
                    "value": "mockJsonNodePath(toString=\"/path\")"
                }
            ],
            "expected": "NullPointerException",
            "description": "ExecutionContext provides a null ExecutionConfig; the method should throw a NullPointerException when attempting to call isDebugEnabled() on the null config."
        }
    ]
}
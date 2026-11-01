{
    "group": "JsonSchema.toString - schema node toString throws exception",
    "cases": [
        {
            "input": [],
            "expected": "java.lang.IllegalStateException",
            "description": "When getSchemaNode() returns a JsonNode whose toString() method throws an unchecked exception (e.g., IllegalStateException), the toString() implementation of JsonSchema propagates that exception. The test uses a spy or mock of JsonSchema that returns a valid evaluation path and a mocked JsonNode whose toString() is stubbed to throw IllegalStateException."
        }
    ]
}
{
    "group": "BaseJsonValidator.toString() - evaluation path handling",
    "cases": [
        {
            "input": [],
            "expected": "leaf",
            "description": "When the evaluation path contains several segments (e.g., [\"root\", \"child\", \"leaf\"]) the toString() method should return the last segment, i.e., \"leaf\"."
        },
        {
            "input": [],
            "expected": "only",
            "description": "When the evaluation path consists of a single segment (e.g., [\"only\"]) the toString() method should return that single segment."
        },
        {
            "input": [],
            "expected": "",
            "description": "When the evaluation path is empty, getName(-1) is expected to return an empty string (or null – the test asserts the actual contract of JsonNodePath). The toString() method should therefore return an empty string."
        },
        {
            "input": [],
            "expected": "java.lang.NullPointerException",
            "description": "If getEvaluationPath() returns null, toString() will attempt to invoke getName(-1) on a null reference and should throw a NullPointerException."
        }
    ]
}
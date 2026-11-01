{
    "group": "BaseJsonValidator.toString",
    "cases": [
        {
            "input": [],
            "expected": "leaf",
            "description": "Normal case – the evaluation path contains several elements ['root','child','leaf']; toString() should return the last element 'leaf' (getName(-1))."
        },
        {
            "input": [],
            "expected": "only",
            "description": "Single‑element path – when the evaluation path consists of a single name ['only'], getName(-1) returns that name, so toString() should return 'only'."
        },
        {
            "input": [],
            "expected": null,
            "description": "Empty path – a JsonNodePath that holds no names returns null (or an empty string) for getName(-1). The test verifies that toString() propagates this null result."
        },
        {
            "input": [],
            "expected": "java.lang.NullPointerException",
            "description": "Null evaluation path – if getEvaluationPath() returns null, toString() will throw a NullPointerException. The test asserts that this exception is thrown."
        },
        {
            "input": [],
            "expected": "java.lang.IndexOutOfBoundsException",
            "description": "Path implementation throws IndexOutOfBoundsException for getName(-1) when the internal list is empty. The test confirms that toString() does not swallow the exception and propagates it."
        }
    ]
}
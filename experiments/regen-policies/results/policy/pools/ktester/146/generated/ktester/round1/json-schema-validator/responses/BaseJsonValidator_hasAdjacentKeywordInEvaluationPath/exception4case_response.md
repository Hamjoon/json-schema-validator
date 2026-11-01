{
    "groups": [
        {
            "group": "Validators collection is null leads to NullPointerException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "keyword",
                            "value": "type"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "The evaluation parent schema returns a non‑null validators collection that is actually `null`. When the method attempts to iterate over it, a NullPointerException is thrown."
                }
            ]
        },
        {
            "group": "Validator in collection is null leads to NullPointerException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "keyword",
                            "value": "minimum"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "The evaluation parent schema provides a validators collection containing a `null` element. The method dereferences the element to call `getKeyword()`, causing a NullPointerException."
                }
            ]
        },
        {
            "group": "EvaluationPath is null leads to NullPointerException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "keyword",
                            "value": "maxLength"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "The evaluation parent schema returns a non‑null validators collection (empty or with valid validators) but its `getEvaluationPath()` method returns `null`. The subsequent call to `getElement(-1)` triggers a NullPointerException."
                }
            ]
        },
        {
            "group": "Empty EvaluationPath causes IndexOutOfBoundsException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "keyword",
                            "value": "pattern"
                        }
                    ],
                    "expected": "IndexOutOfBoundsException",
                    "description": "The evaluation parent schema provides a valid (non‑null) evaluation path that contains no elements. Calling `getElement(-1)` attempts to access a non‑existent element and throws an IndexOutOfBoundsException (or the specific exception defined by the path implementation)."
                }
            ]
        }
    ]
}
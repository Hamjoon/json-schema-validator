{
    "groups": [
        {
            "group": "canShortCircuit - parent schema is null",
            "cases": [
                {
                    "input": [],
                    "expected": "NullPointerException",
                    "description": "When `getEvaluationParentSchema()` returns null, the method attempts to call `getValidators()` on a null reference, causing a NullPointerException."
                }
            ]
        },
        {
            "group": "canShortCircuit - validators list is null",
            "cases": [
                {
                    "input": [],
                    "expected": "NullPointerException",
                    "description": "When the parent schema exists but its `getValidators()` method returns null, iterating over a null collection triggers a NullPointerException."
                }
            ]
        },
        {
            "group": "canShortCircuit - validator element is null",
            "cases": [
                {
                    "input": [],
                    "expected": "NullPointerException",
                    "description": "When the validators list contains a `null` element, the loop attempts to invoke `validator.getKeyword()` on a null reference, resulting in a NullPointerException."
                }
            ]
        },
        {
            "group": "canShortCircuit - validator.getKeyword() throws RuntimeException",
            "cases": [
                {
                    "input": [],
                    "expected": "RuntimeException",
                    "description": "If a validator's `getKeyword()` implementation throws a RuntimeException (e.g., due to internal errors), the method propagates that exception without handling it."
                }
            ]
        }
    ]
}
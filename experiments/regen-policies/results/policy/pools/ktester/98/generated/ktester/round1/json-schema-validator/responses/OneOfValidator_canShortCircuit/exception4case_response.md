{
    "test_cases": [
        {
            "group": "canShortCircuit_nullParentSchema",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "parentSchema",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "When `getEvaluationParentSchema()` returns null, the method attempts to call `getValidators()` on a null reference, resulting in a NullPointerException."
                }
            ]
        },
        {
            "group": "canShortCircuit_nullValidatorsList",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "parentSchema",
                            "value": "mock"
                        },
                        {
                            "parameter": "validatorsList",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "When the parent schema's `getValidators()` method returns null, the enhanced for‑loop tries to iterate over a null collection, causing a NullPointerException."
                }
            ]
        },
        {
            "group": "canShortCircuit_nullValidatorInList",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "parentSchema",
                            "value": "mock"
                        },
                        {
                            "parameter": "validatorsList",
                            "value": "[null]"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "When the validators list contains a null element, the loop dereferences the null `validator` to call `getKeyword()`, which throws a NullPointerException."
                }
            ]
        },
        {
            "group": "canShortCircuit_validatorKeywordThrows",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "parentSchema",
                            "value": "mock"
                        },
                        {
                            "parameter": "validatorsList",
                            "value": "[validatorThatThrows]"
                        }
                    ],
                    "expected": "RuntimeException",
                    "description": "If a validator's `getKeyword()` method itself throws a RuntimeException, the exception propagates out of `canShortCircuit()`."
                }
            ]
        }
    ]
}
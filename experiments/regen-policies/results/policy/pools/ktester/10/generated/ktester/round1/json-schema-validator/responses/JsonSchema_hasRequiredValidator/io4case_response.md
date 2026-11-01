{
    "test_cases": [
        {
            "group": "hasRequiredValidator - required keyword present",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaJson",
                            "value": "{ \"type\": \"object\", \"required\": [\"name\"] }"
                        }
                    ],
                    "expected": true,
                    "description": "A JsonSchema built from a schema that contains the `required` keyword should initialise a RequiredValidator. After construction (or after calling `initializeValidators()`), `hasRequiredValidator()` must return true."
                }
            ]
        },
        {
            "group": "hasRequiredValidator - required keyword absent",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaJson",
                            "value": "{ \"type\": \"object\", \"properties\": { \"age\": { \"type\": \"integer\" } } }"
                        }
                    ],
                    "expected": false,
                    "description": "A JsonSchema built from a schema that does **not** contain the `required` keyword should not create a RequiredValidator. `hasRequiredValidator()` must therefore return false."
                }
            ]
        },
        {
            "group": "hasRequiredValidator - manually injected validator",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaJson",
                            "value": "{ \"type\": \"object\" }"
                        },
                        {
                            "parameter": "injectValidator",
                            "value": true
                        }
                    ],
                    "expected": true,
                    "description": "Even if the schema does not contain a `required` keyword, a test can force a RequiredValidator into the private field (e.g., via reflection). After injection, `hasRequiredValidator()` should report true."
                }
            ]
        },
        {
            "group": "hasRequiredValidator - validator cleared after initialization",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaJson",
                            "value": "{ \"type\": \"object\", \"required\": [\"id\"] }"
                        },
                        {
                            "parameter": "clearValidator",
                            "value": true
                        }
                    ],
                    "expected": false,
                    "description": "A schema that initially creates a RequiredValidator (so the method would return true) is then cleared (e.g., by setting the private field to null). After clearing, `hasRequiredValidator()` must return false."
                }
            ]
        },
        {
            "group": "hasRequiredValidator - after withConfig that does not affect required validator",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaJson",
                            "value": "{ \"type\": \"object\", \"required\": [\"code\"] }"
                        },
                        {
                            "parameter": "configChange",
                            "value": "{ \"typeLoose\": true }"
                        }
                    ],
                    "expected": true,
                    "description": "Calling `withConfig` with a configuration that does not disable the required keyword must leave the RequiredValidator intact; `hasRequiredValidator()` should still return true."
                }
            ]
        }
    ]
}
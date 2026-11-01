{
    "groups": [
        {
            "group": "NullPointerException propagation from getValidators",
            "cases": [
                {
                    "input": [],
                    "expected": "NullPointerException thrown from getValidators() propagates out of getTypeValidator()",
                    "description": "Configure a JsonSchema spy so that its getValidators() method throws a NullPointerException (e.g., new NullPointerException(\"null boom\")). Invoke getTypeValidator() and assert that the same NullPointerException is thrown to the caller."
                }
            ]
        },
        {
            "group": "Error propagation from getValidators",
            "cases": [
                {
                    "input": [],
                    "expected": "AssertionError thrown from getValidators() propagates out of getTypeValidator()",
                    "description": "Stub getValidators() to throw an AssertionError (or any subclass of java.lang.Error). Call getTypeValidator() and verify that the Error is not caught and propagates unchanged."
                }
            ]
        }
    ]
}
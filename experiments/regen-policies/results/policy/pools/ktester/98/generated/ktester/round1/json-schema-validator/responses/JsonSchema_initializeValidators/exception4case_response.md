{
    "group": "initializeValidators - getValidators throws exception",
    "cases": [
        {
            "input": [],
            "expected": "RuntimeException (e.g., IllegalStateException) is propagated; validatorsLoaded flag remains false; no preloadJsonSchema() calls are made",
            "description": "When getValidators() itself throws an exception before returning a collection, initializeValidators() should let the exception bubble up and must not set validatorsLoaded to true nor invoke any validator."
        }
    ]
}
{
    "group": "Preload with list that throws UnsupportedOperationException on modification",
    "cases": [
        {
            "input": [],
            "expected": "UnsupportedOperationException thrown from preloadJsonSchema() and propagated to the caller",
            "description": "Provide a tupleSchema implemented as a custom List that throws UnsupportedOperationException when a mutating method (e.g., add) is called. If preloadJsonSchemas attempts to modify the list, the exception should bubble up."
        }
    ]
}
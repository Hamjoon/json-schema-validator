{
    "group": "Mutable evaluated properties collection",
    "cases": [
        {
            "input": [],
            "expected": "Collection size is 0 initially, after adding \"propA\" size becomes 1 and collection contains \"propA\"",
            "description": "Create a CollectorContext with unevaluatedProperties enabled (default). Enter a dynamic scope, retrieve evaluated properties via getEvaluatedProperties(), verify the collection is mutable by adding a property and checking the size and content."
        },
        {
            "input": [],
            "expected": "Calling getEvaluatedProperties() multiple times returns the same collection instance (identity equality)",
            "description": "Enter a dynamic scope, call getEvaluatedProperties() twice and assert that both references are identical, confirming that the method forwards the same underlying collection."
        }
    ]
}
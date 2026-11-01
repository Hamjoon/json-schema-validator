{
    "test_cases": [
        {
            "group": "OneOfValidator.canShortCircuit() - exception scenarios",
            "cases": [
                {
                    "input": [],
                    "expected": "NullPointerException",
                    "description": "Invocation when `getEvaluationParentSchema()` returns null. The method attempts to call `.getValidators()` on a null reference, causing a NullPointerException."
                },
                {
                    "input": [],
                    "expected": "NullPointerException",
                    "description": "Invocation when the parent schema's validators collection contains a null element. The enhanced for‑loop dereferences the null validator to call `getKeyword()`, resulting in a NullPointerException."
                },
                {
                    "input": [],
                    "expected": "IllegalArgumentException",
                    "description": "Invocation when a validator in the collection throws an IllegalArgumentException from its `getKeyword()` implementation. The exception propagates out of `canShortCircuit()`."
                },
                {
                    "input": [],
                    "expected": "ConcurrentModificationException",
                    "description": "Invocation when the validators collection is modified concurrently during iteration (e.g., a `List` that has an element added inside the loop). The iterator detects the structural change and throws a ConcurrentModificationException."
                }
            ]
        }
    ]
}
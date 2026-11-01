{
    "groups": [
        {
            "group": "AnyOfValidator.canShortCircuit() - null parent schema",
            "cases": [
                {
                    "input": [],
                    "expected": "java.lang.NullPointerException",
                    "description": "The validator's evaluation parent schema is null. The method attempts to call getValidators() on a null reference, causing a NullPointerException. This verifies that the method fails fast when the parent schema is not set."
                }
            ]
        },
        {
            "group": "AnyOfValidator.canShortCircuit() - null validator element in list",
            "cases": [
                {
                    "input": [],
                    "expected": "java.lang.NullPointerException",
                    "description": "The parent schema returns a non‑null list of validators, but the list contains a null entry. When the for‑each loop reaches the null element and invokes validator.getKeyword(), a NullPointerException is thrown. This tests handling of malformed validator collections."
                }
            ]
        },
        {
            "group": "AnyOfValidator.canShortCircuit() - concurrent modification during iteration",
            "cases": [
                {
                    "input": [],
                    "expected": "java.util.ConcurrentModificationException",
                    "description": "The validators list is a mutable collection that is modified (e.g., an element is added or removed) by another thread while canShortCircuit() is iterating over it. The iterator detects the structural change and throws a ConcurrentModificationException, confirming that the method does not guard against concurrent modifications."
                }
            ]
        }
    ]
}
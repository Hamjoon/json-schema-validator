{
    "group": "CollectorContext.add(String, Collector<E>) - additional edge cases and robustness",
    "cases": [
        {
            "input": [
                {
                    "parameter": "name",
                    "value": "\"\""
                },
                {
                    "parameter": "collector",
                    "value": "new SimpleCollector<>()"
                }
            ],
            "expected": "CollectorContext.get(\"\") returns the SimpleCollector instance that was added",
            "description": "Verify that an empty string is a valid key and can be used to store and retrieve a collector."
        },
        {
            "input": [
                {
                    "parameter": "name",
                    "value": "\"   \""
                },
                {
                    "parameter": "collector",
                    "value": "new SimpleCollector<>()"
                }
            ],
            "expected": "CollectorContext.get(\"   \") returns the SimpleCollector instance that was added",
            "description": "Check that a name consisting only of whitespace characters is accepted as a map key."
        },
        {
            "input": [
                {
                    "parameter": "name",
                    "value": "\"!@#$%^&*()_+|~=`{}[]:\\\\;'<>,.?/\""
                },
                {
                    "parameter": "collector",
                    "value": "new SimpleCollector<>()"
                }
            ],
            "expected": "CollectorContext.get(\"!@#$%^&*()_+|~=`{}[]:\\\\;'<>,.?/\") returns the SimpleCollector instance",
            "description": "Ensure that special characters in the name do not affect storage/retrieval."
        },
        {
            "input": [
                {
                    "parameter": "name",
                    "value": "String.join(\"\", java.util.Collections.nCopies(5000, \"a\"))"
                },
                {
                    "parameter": "collector",
                    "value": "new SimpleCollector<>()"
                }
            ],
            "expected": "CollectorContext.get(veryLongName) returns the SimpleCollector instance without throwing any exception",
            "description": "Stress test with a very long (5,000‑character) name to confirm the method handles large keys gracefully."
        },
        {
            "input": [
                {
                    "parameter": "name",
                    "value": "\"subCollector\""
                },
                {
                    "parameter": "collector",
                    "value": "new CustomCollector<>()"
                }
            ],
            "expected": "CollectorContext.get(\"subCollector\") returns the same CustomCollector instance",
            "description": "Validate that a collector of a subclass type (CustomCollector extends SimpleCollector) is stored and retrieved correctly."
        },
        {
            "input": [
                {
                    "parameter": "name",
                    "value": "\"nullThenReal\""
                },
                {
                    "parameter": "collector",
                    "value": "null"
                }
            ],
            "expected": "After adding null, CollectorContext.get(\"nullThenReal\") returns null; after a second add with a non‑null collector, get returns the new collector and getAll().size() remains 1",
            "description": "Test overwriting a previously stored null value with a real collector; map size should not increase."
        },
        {
            "input": [
                {
                    "parameter": "name",
                    "value": "\"sameInstance\""
                },
                {
                    "parameter": "collector",
                    "value": "new SimpleCollector<>()"
                }
            ],
            "expected": "After adding the same collector instance a second time with the same name, CollectorContext.get(\"sameInstance\") still returns that instance and getAll().size() stays 1",
            "description": "Confirm that adding the identical instance under an existing key does not change the map size or the stored reference."
        },
        {
            "input": [
                {
                    "parameter": "name",
                    "value": "\"ユニコード\""
                },
                {
                    "parameter": "collector",
                    "value": "new SimpleCollector<>()"
                }
            ],
            "expected": "CollectorContext.get(\"ユニコード\") returns the SimpleCollector instance",
            "description": "Check that Unicode characters in the name are handled correctly."
        },
        {
            "input": [
                {
                    "parameter": "name",
                    "value": "new String(\"duplicateKey\")"
                },
                {
                    "parameter": "collector",
                    "value": "new SimpleCollector<>(\"first\")"
                }
            ],
            "expected": "After a second add with name = new String(\"duplicateKey\") and collector = new SimpleCollector<>(\"second\"), CollectorContext.get(\"duplicateKey\") returns the second collector and getAll().size() remains 1",
            "description": "Verify that map key equality is based on String.equals, not object identity; a different String object with the same content overwrites the previous entry."
        }
    ]
}
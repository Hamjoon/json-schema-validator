{
    "group": "Validator list contains non‑JsonValidator element (ClassCastException)",
    "cases": [
        {
            "input": [
                {
                    "parameter": "keyword",
                    "value": "anyKeyword"
                }
            ],
            "expected": "ClassCastException",
            "description": "The evaluation‑parent schema's getValidators() returns a collection that includes an object which does not implement JsonValidator. During the enhanced for‑loop the JVM attempts to cast the element to JsonValidator, resulting in a ClassCastException."
        }
    ]
}
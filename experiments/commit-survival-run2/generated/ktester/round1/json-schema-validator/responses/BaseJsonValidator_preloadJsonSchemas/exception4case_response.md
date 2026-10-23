{
    "group": "preloadJsonSchemas - raw collection with wrong element type",
    "cases": [
        {
            "input": [
                {
                    "parameter": "schemas",
                    "value": "Raw List containing a String element \"not a schema\" (e.g., List rawList = new ArrayList(); rawList.add(\"not a schema\");) passed as Collection<JsonSchema> via unchecked cast"
                }
            ],
            "expected": "ClassCastException is thrown when the for‑each loop attempts to cast the String element to JsonSchema",
            "description": "Verifies that passing a raw collection containing objects that are not instances of JsonSchema results in a ClassCastException during iteration."
        }
    ]
}
{
    "testCases": [
        {
            "group": "Identity of returned map",
            "cases": [
                {
                    "input": [],
                    "expected": "Two consecutive calls to getSchemas() return the exact same Map instance (reference equality)",
                    "description": "Verify that the getter does not create a new map each time but returns the internal live map instance."
                }
            ]
        },
        {
            "group": "Insertion order preservation",
            "cases": [
                {
                    "input": [],
                    "expected": "The iteration order of the map matches the order in which entries were added (e.g., keys \"a\", \"b\", \"c\" appear in that sequence)",
                    "description": "Add several entries to the map returned by getSchemas() in a known order and then iterate over keySet() to confirm that LinkedHashMap preserves insertion order."
                }
            ]
        },
        {
            "group": "Removal of entries via returned map",
            "cases": [
                {
                    "input": [],
                    "expected": "After removing a key from the map obtained from getSchemas(), the internal map no longer contains that key and its size decreases accordingly",
                    "description": "Insert a mock JsonSchema under key \"temp\", call getSchemas().remove(\"temp\"), then verify that a subsequent call to getSchemas() reports the key absent."
                }
            ]
        },
        {
            "group": "Handling of null key",
            "cases": [
                {
                    "input": [],
                    "expected": "Putting a null key with a JsonSchema value into the map returned by getSchemas() succeeds and the map reports containsKey(null) == true",
                    "description": "LinkedHashMap permits a single null key; this test ensures that getSchemas() exposes that behavior and does not guard against null keys."
                }
            ]
        },
        {
            "group": "Overwrite existing key",
            "cases": [
                {
                    "input": [],
                    "expected": "Inserting a second JsonSchema under an existing key replaces the previous value; the map size remains unchanged and getSchemas().get(key) returns the new instance",
                    "description": "Add an entry with key \"duplicate\", then add another entry with the same key but a different mock JsonSchema. Verify that the map size stays the same and the value is the latest one."
                }
            ]
        }
    ]
}
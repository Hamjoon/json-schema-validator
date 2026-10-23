{
    "testCases": [
        {
            "group": "Initial state of schemas map",
            "cases": [
                {
                    "input": [],
                    "expected": "An empty map (size 0) is returned",
                    "description": "When a new PropertiesValidator instance is created, getSchemas() should return the internal map which is initially empty."
                }
            ]
        },
        {
            "group": "Schemas map after adding entries via reflection",
            "cases": [
                {
                    "input": [],
                    "expected": "Map contains the added entry with key \"user\" and the corresponding JsonSchema instance",
                    "description": "Using reflection, insert a mock JsonSchema object into the private `schemas` field. getSchemas() must return a map that reflects this entry, proving the getter returns the actual internal map."
                }
            ]
        },
        {
            "group": "Mutability of the returned map",
            "cases": [
                {
                    "input": [],
                    "expected": "Modifying the map returned by getSchemas() (e.g., put a new entry) also changes the internal state of the validator",
                    "description": "Obtain the map via getSchemas(), add a new entry directly to this map, and then call getSchemas() again. The second call should show the newly added entry, confirming that the getter does not return a defensive copy."
                }
            ]
        },
        {
            "group": "Thread‑safety sanity check",
            "cases": [
                {
                    "input": [],
                    "expected": "Concurrent modifications to the map returned by getSchemas() are reflected without throwing ConcurrentModificationException during read",
                    "description": "Create two threads: one repeatedly adds entries to the map obtained from getSchemas(), the other repeatedly reads the map size. The test verifies that no unexpected exceptions are thrown and that the size observed is consistent with the number of insertions, demonstrating that getSchemas() returns the live map."
                }
            ]
        }
    ]
}
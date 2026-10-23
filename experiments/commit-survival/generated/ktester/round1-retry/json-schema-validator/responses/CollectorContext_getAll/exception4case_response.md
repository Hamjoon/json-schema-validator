{
    "test_cases": [
        {
            "group": "CollectorContext.getAll() - internal map null handling",
            "cases": [
                {
                    "input": [],
                    "expected": "NullPointerException",
                    "description": "Using reflection, set the private field 'collectorMap' to null before invoking getAll(). The method should throw NullPointerException when it attempts to putAll from a null map."
                },
                {
                    "input": [],
                    "expected": "NullPointerException",
                    "description": "Using reflection, set the private field 'collectorLoadMap' to null before invoking getAll(). The method should throw NullPointerException when it attempts to putAll from a null map."
                },
                {
                    "input": [],
                    "expected": "NullPointerException",
                    "description": "Using reflection, set both 'collectorMap' and 'collectorLoadMap' to null before invoking getAll(). The first putAll call should trigger a NullPointerException."
                }
            ]
        },
        {
            "group": "CollectorContext.getAll() - faulty map implementation",
            "cases": [
                {
                    "input": [],
                    "expected": "RuntimeException",
                    "description": "Replace the private field 'collectorMap' via reflection with a custom Map implementation whose entrySet iterator throws a RuntimeException. Calling getAll() should propagate that RuntimeException."
                },
                {
                    "input": [],
                    "expected": "RuntimeException",
                    "description": "Replace the private field 'collectorLoadMap' via reflection with a custom Map implementation whose entrySet iterator throws a RuntimeException. Calling getAll() should propagate that RuntimeException."
                }
            ]
        }
    ]
}
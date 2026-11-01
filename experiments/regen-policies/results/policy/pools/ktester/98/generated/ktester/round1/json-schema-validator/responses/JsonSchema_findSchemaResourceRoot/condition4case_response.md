{
    "testCases": [
        {
            "group": "Current schema is already the resource root",
            "cases": [
                {
                    "input": [],
                    "expected": "returns the same JsonSchema instance that invoked findSchemaResourceRoot()",
                    "description": "The schema instance reports true for isSchemaResourceRoot() and therefore the while‑loop never iterates. The method should immediately return the original instance."
                }
            ]
        },
        {
            "group": "Traverse ancestors until the resource root is found",
            "cases": [
                {
                    "input": [],
                    "expected": "returns the ancestor JsonSchema instance whose isSchemaResourceRoot() returns true",
                    "description": "Create a chain of three JsonSchema objects (child → middle → root). Only the topmost object reports true for isSchemaResourceRoot(). The method must walk up the chain via getParentSchema() and return the root object."
                }
            ]
        },
        {
            "group": "No resource‑root in the hierarchy (null parent leads to NPE)",
            "cases": [
                {
                    "input": [],
                    "expected": "throws NullPointerException",
                    "description": "Configure a JsonSchema instance that returns false for isSchemaResourceRoot() and whose getParentSchema() returns null. The while‑loop will attempt to call isSchemaResourceRoot() on a null reference, resulting in a NullPointerException. The test verifies that this exceptional path is exercised."
                }
            ]
        }
    ]
}
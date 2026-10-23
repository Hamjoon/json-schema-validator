{
    "group": "findAncestor_additional",
    "cases": [
        {
            "input": [],
            "expected": "returns the same JsonSchema instance after multiple calls (idempotent)",
            "description": "Idempotence: invoke findAncestor() twice on a root schema (no parent) and verify that both calls return the identical instance, confirming that the method does not alter internal state."
        },
        {
            "input": [],
            "expected": "returns the top‑most ancestor JsonSchema instance for a ten‑level chain",
            "description": "Deep hierarchy: construct a chain of ten schemas where each schema's getParentSchema() points to the next higher level. Calling findAncestor() on the leaf should recursively traverse all ten levels and return the original root schema, testing recursion depth beyond the simple three‑level case."
        },
        {
            "input": [],
            "expected": "throws StackOverflowError due to a cyclic parent reference",
            "description": "Cyclic reference: create two JsonSchema objects where schemaA.getParentSchema() returns schemaB and schemaB.getParentSchema() returns schemaA, forming a loop. Invoking findAncestor() on either schema should result in a StackOverflowError, demonstrating that the method does not handle cycles and that such a configuration is unsafe."
        }
    ]
}
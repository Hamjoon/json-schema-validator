{
    "group": "createSchemaValidatorsConfig - concurrent calls produce distinct instances",
    "cases": [
        {
            "input": [],
            "expected": "All instances returned from 10 parallel threads are non‑null, of type com.networknt.schema.SchemaValidatorsConfig, have identical default field values, and each instance has a unique reference (no two threads receive the same object)",
            "description": "Spawn multiple threads that each invoke the protected createSchemaValidatorsConfig() via a subclass exposing the method. Verify thread‑safety: every call yields a fresh configuration object with default settings and no shared references across threads."
        }
    ]
}
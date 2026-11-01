{
    "group": "Discriminator Disabled - Walk=True, All Schemas Invalid",
    "cases": [
        {
            "input": [
                { "parameter": "executionContext", "value": "mock ExecutionContext with failFast=false, discriminator disabled" },
                { "parameter": "node", "value": "JsonNode of type string \"invalid\"" },
                { "parameter": "rootNode", "value": "same as node" },
                { "parameter": "instanceLocation", "value": "JsonNodePath root" },
                { "parameter": "walk", "value": true }
            ],
            "expected": "Set containing validation messages from each schema (allErrors) because walk=true forces each schema to be evaluated via walk() and all return non‑empty error sets; short‑circuit is false.",
            "description": "Discriminator disabled, walk flag true, every subschema returns errors via walk(); the validator accumulates all errors and returns them."
        }
    ]
}
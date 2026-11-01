{
    "group": "registerAndMergeDiscriminator_noSchemaDiscriminator",
    "cases": [
        {
            "input": [
                {
                    "parameter": "currentDiscriminatorContext",
                    "value": "mock DiscriminatorContext where getDiscriminatorForPath(any) returns null"
                },
                {
                    "parameter": "discriminator",
                    "value": "empty ObjectNode"
                },
                {
                    "parameter": "schema",
                    "value": "JsonSchema whose schemaNode does NOT contain a \"discriminator\" field"
                },
                {
                    "parameter": "instanceLocation",
                    "value": "\"/root\" (JsonNodePath instance)"
                }
            ],
            "expected": "No exception thrown; currentDiscriminatorContext.registerDiscriminator called once with schema.schemaLocation and the original discriminator (unchanged)",
            "description": "When the schema does not define a discriminator, the method should simply register the supplied discriminator without attempting any merge."
        }
    ]
}
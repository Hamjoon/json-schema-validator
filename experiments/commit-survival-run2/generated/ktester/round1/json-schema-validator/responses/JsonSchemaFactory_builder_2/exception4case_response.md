{
    "cases": [
        {
            "group": "builder(JsonSchemaFactory) - null blueprint",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "blueprint",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "Calling builder(null) should throw a NullPointerException because the method dereferences the blueprint to copy its internal state."
                }
            ]
        },
        {
            "group": "builder(JsonSchemaFactory) - blueprint with null jsonMetaSchemas map",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "blueprint",
                            "value": "JsonSchemaFactory instance whose internal jsonMetaSchemas field is set to null via reflection"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "The method accesses blueprint.jsonMetaSchemas.values(); if the map is null a NullPointerException must be thrown."
                }
            ]
        },
        {
            "group": "builder(JsonSchemaFactory) - blueprint with null uriFactory",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "blueprint",
                            "value": "JsonSchemaFactory instance whose internal uriFactory field is set to null via reflection"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "When iterating over blueprint.uriFactory.getURIFactories() the method will dereference a null uriFactory, resulting in a NullPointerException."
                }
            ]
        },
        {
            "group": "builder(JsonSchemaFactory) - blueprint with null uriFetcher",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "blueprint",
                            "value": "JsonSchemaFactory instance whose internal uriFetcher field is set to null via reflection"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "The method calls blueprint.uriFetcher.getURIFetchers(); a null uriFetcher causes a NullPointerException."
                }
            ]
        },
        {
            "group": "builder(JsonSchemaFactory) - blueprint with null uriMap",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "blueprint",
                            "value": "JsonSchemaFactory instance whose internal uriMap field is set to null via reflection"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "The builder adds the blueprint's uriMap via addUriMappings; passing a null map leads to a NullPointerException."
                }
            ]
        }
    ]
}
You are tasked with creating a framework for a test class in Java. Now analyze the following information carefully:

1. The method that needs to be tested and its parameters.
2. The constructor of the target class and its parameters.
3. Any dependencies or external resources used by the target class.

Based on this analysis, complete the test class template in the provided structure:

1. Initialize necessary fields and dependencies within setupBeforeAll() and setupBeforeEach().
2. Handle any required cleanup in teardownAfterEach() and teardownAfterAll().
3. Make sure not to invoke any private methods or fields of the target class and dependent class.
4. Refer to the "given" phase in the existing test class for guidance.
5. Add necessary import statements and ensure there are no syntax errors in the generated code.

@input{focal method and target class}
Here is the code of focal method, `detect(JsonNode)`, with the focal class `SpecVersionDetector_detect_Test`:
```Java
package com.networknt.schema;
import com.networknt.schema.SpecVersion.VersionFlag;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.stream.StreamSupport;
import java.util.Optional;
import java.util.Objects;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.stream.Stream;
import java.util.Map;
public final class SpecVersionDetector {
    protected static final Map<String, VersionFlag> supportedVersions = new HashMap<>();
    private static final String SCHEMA_TAG = "$schema";
    public static VersionFlag detect(JsonNode jsonNode) {
        return detectOptionalVersion(jsonNode).orElseThrow(() -> new JsonSchemaException(("'" + SpecVersionDetector.SCHEMA_TAG) + "' tag is not present"));
    }
public static Optional<VersionFlag> detectOptionalVersion(JsonNode jsonNode);
    public static Optional<VersionFlag> detectOptionalVersion(String schemaUri);
    public static VersionFlag detectVersion(JsonNode jsonNode, Path specification, VersionFlag defaultVersion);
    public static Optional<VersionFlag> detectVersionFromPath(Path path);
}
```


@input{context information}

api document of class com.networknt.schema.SpecVersionDetector: 

 * This class is used to detect schema version
 *
 * @author Subhajitdas298
 * @since 25/06/20
 

constructors for class `com.networknt.schema.SpecVersionDetector`: 
params: 
body:
```java
    private SpecVersionDetector() {
        // Prevent instantiation of this utility class
    }

```

existing test class: 
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SpecVersionDetectorTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    @ParameterizedTest
    @CsvSource({
            "draft4,       V4",
            "draft6,       V6",
            "draft7,       V7",
            "draft2019-09, V201909",
            "draft2020-12, V202012"
    })
    void detectVersion(String resourceDirectory, SpecVersion.VersionFlag expectedFlag) throws IOException {
        InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(resourceDirectory + "/schemaTag.json");
        JsonNode node = mapper.readTree(in);
        SpecVersion.VersionFlag flag = SpecVersionDetector.detect(node);
        assertEquals(expectedFlag, flag);
    }

    @ParameterizedTest
    @CsvSource({
            "data/schemaTag.json,        'http://json-schema.org/draft-03/schema#' is unrecognizable schema",
            "data/schemaTagMissing.json, '$schema' tag is not present"
    })
    void detectInvalidSchemaVersion(String schemaPath, String expectedError) throws IOException {
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(schemaPath);
        JsonNode node = mapper.readTree(in);
        JsonSchemaException exception = assertThrows(JsonSchemaException.class, () -> SpecVersionDetector.detect(node));
        assertEquals(expectedError, exception.getMessage());
    }

    @Test
    void detectOptionalSpecVersion() throws IOException {
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(
                "data/schemaTagMissing.json");
        JsonNode node = mapper.readTree(in);
        Optional<SpecVersion.VersionFlag> flag = SpecVersionDetector.detectOptionalVersion(node);
        assertEquals(Optional.empty(), flag);
    }
}

```



@input{test class template}
```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
// Add other imports if necessary

@ExtendWith(MockitoExtension.class)
class SpecVersionDetector_detect_Test {
    @BeforeAll
    static void setupBeforeAll() {
    }

    @BeforeEach
    void setupBeforeEach() {
    }

    @AfterEach
    void teardownAfterEach() {
    }

    @AfterAll
    static void teardownAfterAll() {
    }
}
```

@output{initial test class}: now consider the invoke examples output the code wrapped in a Markdown code block ```java at the beginning and ``` at the end.
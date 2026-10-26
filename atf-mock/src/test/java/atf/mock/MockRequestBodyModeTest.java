package atf.mock;
import atf.testkit.AtfComponentTest;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AtfComponentTest

class MockRequestBodyModeTest {
    @Test
    void preservesTheYamlVocabularyAndDefaultsBlankValuesToJson() {
        assertAll(
                () -> assertEquals(MockRequestBodyMode.JSON,
                        MockRequestBodyMode.from(MockRequestBodyMode.JSON.value())),
                () -> assertEquals(MockRequestBodyMode.WRONG_CONTENT_TYPE,
                        MockRequestBodyMode.from(MockRequestBodyMode.WRONG_CONTENT_TYPE.value())),
                () -> assertEquals(MockRequestBodyMode.JSON.value(), MockRequestBodyMode.normalize(" ")));
    }

    @Test
    void rejectsValuesOutsideTheClosedMockContract() {
        assertThrows(IllegalArgumentException.class,
                () -> MockRequestBodyMode.from("form-data"));
    }
}

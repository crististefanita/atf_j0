package atf.autoex.tests.extension;

import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Component test marker that also publishes an Extent result for each test method. */
@AtfComponentTest
@ExtendWith(AutoExExtentTestExtension.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoExComponentTest { }

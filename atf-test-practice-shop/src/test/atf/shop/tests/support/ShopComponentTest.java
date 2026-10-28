package atf.shop.tests.support;

import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Component marker that publishes one configured Extent artifact per test method. */
@AtfComponentTest
@ExtendWith(ShopExtentTestExtension.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ShopComponentTest { }

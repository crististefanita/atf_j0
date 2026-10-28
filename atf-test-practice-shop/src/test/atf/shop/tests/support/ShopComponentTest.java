package atf.shop.tests.support;

import atf.shop.tests.support.reporting.ShopExtentTestExtension;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Audits each invocation into one report per class, or per explicitly scoped suite. */
@AtfComponentTest
@ExtendWith(ShopExtentTestExtension.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ShopComponentTest { }

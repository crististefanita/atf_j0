package atf.reporting.test;

import atf.core.config.module.AtfModule;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Declares the runtime modules required by one reporting test composition root. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(AtfModuleTestCondition.class)
public @interface AtfModuleTest {
    AtfModule[] requires();
}

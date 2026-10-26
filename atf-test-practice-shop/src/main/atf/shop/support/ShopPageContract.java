package atf.shop.support;

import atf.shop.config.ShopScreen;
import atf.shop.locators.ShopControl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Declarative contract for one navigable Practice Shop page. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ShopPageContract {
    ShopScreen screen();
    /** Exact route or a route prefix ending in {@code /*}. */
    String route();
    ShopControl[] requiredControls();
    String bodyMarker() default "";
}

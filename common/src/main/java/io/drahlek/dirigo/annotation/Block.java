package io.drahlek.dirigo.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Block {
    String id() default ""; // optional registry ID
    String creativeTab() default "";
    boolean registerItem() default true;

    /**
     * Extra block entity types this block should be treated as valid for.
     * <p>
     * Values without a namespace are resolved against the owning mod id. Use a fully qualified id
     * for vanilla or cross-mod block entity types, for example {@code "minecraft:campfire"}.
     */
    String[] validBlockEntityTypes() default {};
}

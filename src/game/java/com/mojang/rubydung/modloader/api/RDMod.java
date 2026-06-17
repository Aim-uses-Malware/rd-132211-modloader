package com.mojang.rubydung.modloader.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation required on every rdLoader mod class.
 *
 * Usage:
 *   @RDMod(id = "mymod", name = "My Mod", version = "1.0", author = "AimRite2")
 *   public class MyMod implements IMod { ... }
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RDMod {
    /** Unique mod ID (lowercase, no spaces) */
    String id();

    /** Display name */
    String name();

    /** Version string */
    String version() default "1.0.0";

    /** Author(s) */
    String author() default "unknown";

    /** Short description */
    String description() default "";

    /** Mod IDs this mod depends on (must be loaded first) */
    String[] dependencies() default {};
}

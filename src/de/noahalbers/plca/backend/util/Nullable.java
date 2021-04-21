package de.noahalbers.plca.backend.util;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Nullable {
	// If the value is a string if it can be empty
	boolean ifEmptyStringAutoToNull() default true;
}

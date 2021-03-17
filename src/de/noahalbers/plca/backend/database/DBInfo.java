package de.noahalbers.plca.backend.database;

import static java.lang.annotation.ElementType.FIELD;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/*
 * Sets the name for importing and exporting the value
 * */

@Retention(RetentionPolicy.RUNTIME)
@Target(FIELD)
public @interface DBInfo {
	// The name inside of the database
	public String value();
	// If the field can be imported
	public boolean importable() default true;
	// If the field can be exported
	public boolean exportable() default true;
}

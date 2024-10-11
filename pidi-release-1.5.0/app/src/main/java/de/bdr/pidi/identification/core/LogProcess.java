/*
 * Copyright Bundesdruckerei 2024. Licensed under EUPL-1.2, see the accompanying license file.
 */
package de.bdr.pidi.identification.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface LogProcess {

    /**
     * Name of the process step that will be added to subsequent log entries.
     * If empty, the method name will be taken.
     */
    String value();

    /**
     * A descriptive log message for the process step.
     */
    String message();
}

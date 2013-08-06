package de.hpi.uni.potsdam.test.bpmn_to_sql.util;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.camunda.bpm.engine.test.Deployment;

/**
 * Use this annotation to specify sql scripts that setup the initial database setting for the test.
 * The default value is the full qualified path to the test method, similar to the {@link Deployment} annotation.
 * @author Thorben Lindhauer
 *
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface DatabaseSetup {

  public String[] resources() default {};
}

package org.camunda.bpm.tasklist;

import org.camunda.bpm.engine.rest.impl.ProcessDefinitionServiceImpl;
import org.camunda.bpm.engine.rest.impl.ProcessInstanceServiceImpl;
import org.camunda.bpm.engine.rest.impl.TaskRestServiceImpl;
import org.camunda.bpm.engine.rest.mapper.EngineQueryDtoGetReader;
import org.camunda.bpm.engine.rest.mapper.JacksonConfigurator;
import org.camunda.bpm.tasklist.resources.AuthenticationResource;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;

import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

import org.camunda.bpm.engine.rest.exception.ExceptionHandler;

/**
 * @author: drobisch
 */
public class EngineApiApplication extends Application {

  @Override
  public Set<Class<?>> getClasses() {
    Set<Class<?>> classes = new HashSet<Class<?>>();

    classes.add(ProcessDefinitionServiceImpl.class);
    classes.add(ProcessInstanceServiceImpl.class);
    classes.add(TaskRestServiceImpl.class);

    classes.add(EngineQueryDtoGetReader.class);
    classes.add(JacksonConfigurator.class);
    classes.add(JacksonJsonProvider.class);
    classes.add(ExceptionHandler.class);

    return classes;
  }
}
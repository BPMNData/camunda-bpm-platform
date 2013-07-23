package de.hpi.uni.potsdam.bpmn_to_sql;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

@ApplicationPath("/")
public class BpmnDataApplication extends Application {

  @Override
  public Set<Class<?>> getClasses() {
    Set<Class<?>> classes = new HashSet<Class<?>>();
    classes.add(BpmnDataMessageEndpoint.class);
    
    return classes;
  }
}

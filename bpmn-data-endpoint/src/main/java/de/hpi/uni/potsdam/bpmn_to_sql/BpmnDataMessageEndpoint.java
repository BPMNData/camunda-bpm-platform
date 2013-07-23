package de.hpi.uni.potsdam.bpmn_to_sql;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

import org.camunda.bpm.BpmPlatform;
import org.camunda.bpm.engine.ProcessEngine;

@Path("/message")
public class BpmnDataMessageEndpoint {

  @POST
  @Consumes(MediaType.APPLICATION_XML)
  public void deliverMessage(String message) {
    ProcessEngine engine = BpmPlatform.getDefaultProcessEngine();
    engine.getRuntimeService().correlateBpmnDataMessage(message);
  }
}

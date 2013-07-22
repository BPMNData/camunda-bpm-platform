package de.hpi.uni.potsdam.bpmn_to_sql.behavior;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.impl.bpmn.behavior.AbstractBpmnActivityBehavior;
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;

import de.hpi.uni.potsdam.bpmn_to_sql.bpmn.CorrelationKey;
import de.hpi.uni.potsdam.bpmn_to_sql.bpmn.CorrelationProperty;
import de.hpi.uni.potsdam.bpmn_to_sql.bpmn.MessageFlow;

public class BpmnDataSendTaskBehavior extends AbstractBpmnActivityBehavior {

  private static final String ENDPOINT_VARIABLE_NAME = "bpmn_data_endpoint";
  private static final String MESSAGE_INPUT_VARIABLE_NAME = "messageInput";
  
  @Override
  public void execute(ActivityExecution execution) throws Exception {
    String endPointAddress = (String) execution.getVariable(ENDPOINT_VARIABLE_NAME);
    String messageContent = (String) execution.getVariable(MESSAGE_INPUT_VARIABLE_NAME);
    
    DefaultHttpClient client = new DefaultHttpClient();
    HttpPost post =  new HttpPost(endPointAddress);
    post.setHeader("Content-Type", "application/xml");
    HttpEntity body = new StringEntity(messageContent);
    post.setEntity(body);
    HttpResponse response = client.execute(post);
    
    int statusCode = response.getStatusLine().getStatusCode();
    
    if (statusCode / 100 != 2) {
      throw new ProcessEngineException("Could not make reques to " + endPointAddress +". Got status " + statusCode);
    }
    
    updateCorrelationKeys(execution, messageContent);

    leave(execution);
  }

  private void updateCorrelationKeys(ActivityExecution execution, String message) {
    // TODO set the keys on the correlation scope here (i.e. subprocess or process instance)
    MessageFlow messageFlow = ((ActivityImpl) execution.getActivity()).getOutgoingMessageFlow();
    
    if (messageFlow == null) {
      throw new ProcessEngineException("Send task " + execution.getActivity().getId() + " has no outgoing message flow");
    }
    
    for (CorrelationKey correlationKey : messageFlow.getCorrelationKeys()) {
      for (CorrelationProperty property : correlationKey.getCorrelationProperties()) {
        populatePropertyFromMessage(execution, property, message);
      }
    }
  }
  
  protected void populatePropertyFromMessage(ActivityExecution execution, CorrelationProperty correlationProperty, String message) {
    // TODO evaluate property expression message here
    execution.setVariable(correlationProperty.getId(), "some correlation key");
  }
}

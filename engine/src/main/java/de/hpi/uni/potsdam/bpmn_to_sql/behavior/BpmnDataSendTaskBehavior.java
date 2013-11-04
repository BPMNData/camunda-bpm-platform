package de.hpi.uni.potsdam.bpmn_to_sql.behavior;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.delegate.Expression;
import org.camunda.bpm.engine.impl.bpmn.behavior.AbstractBpmnActivityBehavior;
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;

import de.hpi.uni.potsdam.bpmn_to_sql.bpmn.MessageFlow;
import de.hpi.uni.potsdam.bpmn_to_sql.correlation.CorrelationHelper;
import de.hpi.uni.potsdam.bpmn_to_sql.correlation.MessageInstance;

/**
 * Sends the message, provided by a variable named 'dataInput' and updates the local correlation properties.
 * @author Thorben
 *
 */
public class BpmnDataSendTaskBehavior extends AbstractBpmnActivityBehavior {

  private static final String MESSAGE_INPUT_VARIABLE_NAME = "dataInput";
  
  @Override
  public void execute(ActivityExecution execution) throws Exception {
    String messageContent = (String) execution.getVariable(MESSAGE_INPUT_VARIABLE_NAME);
    
    if(messageContent == null || messageContent.trim().isEmpty()) {
      throw new ProcessEngineException(execution.getActivity().toString() + ": message content is empty");
    }
    
    MessageFlow messageFlow = ((ActivityImpl) execution.getActivity()).getOutgoingMessageFlow();
    
    if (messageFlow == null) {
      throw new ProcessEngineException("activity " + execution.getActivity().getId() + " has no outgoing message flow.");
    }
    
    Expression endPointAddressExpression = messageFlow.getEndpointAddressExpression();
    
    if (endPointAddressExpression == null) {
      throw new ProcessEngineException("no endpoint specified for participant addressed by message flow " + messageFlow.getId());
    }
    
    String endPointAddress = (String) endPointAddressExpression.getValue(execution);
    
    DefaultHttpClient client = new DefaultHttpClient();
    HttpPost post =  new HttpPost(endPointAddress);
    post.setHeader("Content-Type", "application/xml");
    HttpEntity body = new StringEntity(messageContent);
    post.setEntity(body);
    HttpResponse response = client.execute(post);
    
    int statusCode = response.getStatusLine().getStatusCode();
    
    if (statusCode / 100 != 2) {
      throw new ProcessEngineException("Could not make request to " + endPointAddress +". Got status " + statusCode);
    }
    
    updateCorrelationKeys(execution, messageContent);

    leave(execution);
  }

  private void updateCorrelationKeys(ActivityExecution execution, String message) {
    MessageFlow messageFlow = ((ActivityImpl) execution.getActivity()).getOutgoingMessageFlow();
    
    if (messageFlow == null) {
      throw new ProcessEngineException("Send task " + execution.getActivity().getId() + " has no outgoing message flow");
    }
    
    MessageInstance messageInstance = new MessageInstance();
    messageInstance.setContent(message);
    messageInstance.setMessageDefinition(messageFlow.getMessage());
    
    CorrelationHelper.populateCorrelationKeysInScope(execution, messageInstance, messageFlow.getCorrelationKeys());
  }
}

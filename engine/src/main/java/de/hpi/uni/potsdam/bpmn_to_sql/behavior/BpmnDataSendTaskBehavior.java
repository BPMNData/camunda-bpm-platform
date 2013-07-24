package de.hpi.uni.potsdam.bpmn_to_sql.behavior;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.impl.bpmn.behavior.AbstractBpmnActivityBehavior;
import org.camunda.bpm.engine.impl.bpmn.helper.ScopeUtil;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;

import de.hpi.uni.potsdam.bpmn_to_sql.bpmn.CorrelationKey;
import de.hpi.uni.potsdam.bpmn_to_sql.bpmn.CorrelationProperty;
import de.hpi.uni.potsdam.bpmn_to_sql.bpmn.MessageFlow;
import de.hpi.uni.potsdam.bpmn_to_sql.xquery.XQueryHandler;

public class BpmnDataSendTaskBehavior extends AbstractBpmnActivityBehavior {

  private static final String MESSAGE_INPUT_VARIABLE_NAME = "messageInput";
//  private static final String SUB_PROCESS_TYPE = "subProcess";
  
  @Override
  public void execute(ActivityExecution execution) throws Exception {
    String messageContent = (String) execution.getVariable(MESSAGE_INPUT_VARIABLE_NAME);
    
    if(messageContent == null || messageContent.trim().isEmpty()) {
      throw new ProcessEngineException(execution.getActivity().toString() + ": message content is empty");
    }
    
    MessageFlow messageFlow = ((ActivityImpl) execution.getActivity()).getOutgoingMessageFlow();
    String endPointAddress = messageFlow.getEndpointAddress();
    
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
    
    // determine the correlation scope execution
    ExecutionEntity scopeExecution = determineCorrelationScopeExecution(execution); 
    
    
    
    for (CorrelationKey correlationKey : messageFlow.getCorrelationKeys()) {
      for (CorrelationProperty property : correlationKey.getCorrelationProperties()) {
        populatePropertyFromMessage(scopeExecution, property, message);
      }
    }
  }
  
  protected ExecutionEntity determineCorrelationScopeExecution(ActivityExecution execution) {
    ExecutionEntity correlationScopeExecution = (ExecutionEntity) ScopeUtil.findScopeExecution(execution);
    
    return correlationScopeExecution;
  }
  
//  protected boolean isSubprocessActivity(ActivityImpl activity) {
//    Class<?> subprocessBehaviorClass = SubProcessActivityBehavior.class;
//    Class<?> multInstanceBehaviorClass = MultiInstanceActivityBehavior.class;
//    
//    ActivityBehavior behavior = activity.getActivityBehavior();
//    
//    if (subprocessBehaviorClass.isAssignableFrom(behavior.getClass())) {
//      return true;
//    }
//    if (multInstanceBehaviorClass.isAssignableFrom(behavior.getClass())) {
//      MultiInstanceActivityBehavior miBehavior = (MultiInstanceActivityBehavior) behavior;
//      if (subprocessBehaviorClass.isAssignableFrom(miBehavior.getInnerActivityBehavior().getClass())) {
//        return true;
//      }
//    }
//    
//    return false;
//  }

  protected void populatePropertyFromMessage(ActivityExecution correlationScopeExecution, CorrelationProperty correlationProperty, String message) {
    XQueryHandler handler = new XQueryHandler();
    String property = handler.runXPath(message, correlationProperty.getRetrievalExpression());
    if (property != null && !property.trim().equals("")) {
      correlationScopeExecution.setVariableLocal(correlationProperty.getId(), property);
    }
  }
}

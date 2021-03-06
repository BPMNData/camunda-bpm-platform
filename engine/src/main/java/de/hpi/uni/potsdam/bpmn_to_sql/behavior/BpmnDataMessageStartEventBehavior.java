package de.hpi.uni.potsdam.bpmn_to_sql.behavior;

import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.impl.bpmn.behavior.FlowNodeActivityBehavior;
import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;

import de.hpi.uni.potsdam.bpmn_to_sql.bpmn.MessageFlow;
import de.hpi.uni.potsdam.bpmn_to_sql.correlation.CorrelationHelper;
import de.hpi.uni.potsdam.bpmn_to_sql.correlation.MessageInstance;

/**
 * Expects a variable 'startMessage' that contains the message's xml payload.
 * Populates correlation properties accordingly and provides the message in the 'dataOutput' variable. 
 */
public class BpmnDataMessageStartEventBehavior extends FlowNodeActivityBehavior {

  @Override
  public void execute(ActivityExecution execution) throws Exception {

    if (Context.getProcessEngineConfiguration().isBpmnDataAware()) {
      String message = (String) execution.getVariable("startMessage");
      updateCorrelationKeys(execution, message);
      execution.setVariableLocal("dataOutput", message);
    }
    
    super.execute(execution);
  }
  
  protected void updateCorrelationKeys(ActivityExecution execution, String message) {
    MessageFlow messageFlow = ((ActivityImpl) execution.getActivity()).getIncomingMessageFlow();
    
    if (messageFlow == null) {
      throw new ProcessEngineException("Start event " + execution.getActivity().getId() + " has no incoming message flow");
    }
    
    MessageInstance messageInstance = new MessageInstance();
    messageInstance.setContent(message);
    messageInstance.setMessageDefinition(messageFlow.getMessage());
    
    CorrelationHelper.populateCorrelationKeysInScope(execution, messageInstance, messageFlow.getCorrelationKeys());
  }
}

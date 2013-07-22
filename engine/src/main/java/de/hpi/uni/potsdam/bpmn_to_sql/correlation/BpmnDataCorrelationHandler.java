package de.hpi.uni.potsdam.bpmn_to_sql.correlation;

import java.util.ArrayList;
import java.util.List;

import org.camunda.bpm.engine.MismatchingMessageCorrelationException;
import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.impl.ExecutionQueryImpl;
import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;
import org.camunda.bpm.engine.impl.runtime.CorrelationSet;
import org.camunda.bpm.engine.impl.runtime.DefaultCorrelationHandler;
import org.camunda.bpm.engine.runtime.Execution;

import de.hpi.uni.potsdam.bpmn_to_sql.bpmn.CorrelationKey;
import de.hpi.uni.potsdam.bpmn_to_sql.bpmn.CorrelationProperty;
import de.hpi.uni.potsdam.bpmn_to_sql.bpmn.MessageFlow;
import de.hpi.uni.potsdam.bpmn_to_sql.xquery.XQueryHandler;

public class BpmnDataCorrelationHandler extends DefaultCorrelationHandler {

  public Execution correlateMessageToExecution(CommandContext commandContext, String messageName, CorrelationSet correlationSet) {
    String messageContent = (String) correlationSet.getCorrelationKeys().get("message");
    
    XQueryHandler xQueryHandler = new XQueryHandler();
    
    ExecutionQueryImpl executionQuery = new ExecutionQueryImpl();
    executionQuery.messageEventSubscriptionName(messageName);
    List<Execution> waitingExecutions = executionQuery.executeList(commandContext, null);
    
    List<Execution> matchingExecutions = new ArrayList<Execution>();
    
    for (Execution waitingExecution : waitingExecutions) {
      ExecutionEntity executionEntity = ((ExecutionEntity) waitingExecution);
      ActivityImpl activity = executionEntity.getActivity();
      MessageFlow incomingMessageFlow = activity.getIncomingMessageFlow();
      
      if (incomingMessageFlow == null) {
        throw new ProcessEngineException(waitingExecution.toString() + " waits in activity " 
            + activity.getId() + " without incoming message flow");
      }
      
      for (CorrelationKey correlationKey : incomingMessageFlow.getCorrelationKeys()) {
        boolean matches = true;
        for (CorrelationProperty property : correlationKey.getCorrelationProperties()) {
          String retrievalExpression = property.getRetrievalExpression();
          String resolvedProperty = xQueryHandler.runXPath(messageContent, retrievalExpression);
          if (!executionEntity.hasVariable(property.getId()) || !resolvedProperty.equals(executionEntity.getVariable(property.getId()))) {
            matches = false;
            break;
          }
        }
        
        if (matches) {
          matchingExecutions.add(waitingExecution);
        }
      }
    }
    
    // TODO assure that all matching executions belong to the same process instance, 
    // otherwise an exception should be thrown
    
    if (matchingExecutions.isEmpty()) {
      throw new MismatchingMessageCorrelationException(messageName, "No matching execution found");
    }
    
    return matchingExecutions.get(0);
  }

}

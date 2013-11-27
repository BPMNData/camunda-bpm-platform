package de.hpi.uni.potsdam.bpmn_to_sql.correlation;

import java.util.ArrayList;
import java.util.List;

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

/**
 * Correlates a message to an execution by evaluating correlation keys and properties. 
 * For correlation with process definitions, the behavior from DefaultCorrelationHandler is reused, which
 * matches by the message's name.
 */
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
      
      List<CorrelationResult> correlationResults = new ArrayList<CorrelationResult>();
      for (CorrelationKey correlationKey : incomingMessageFlow.getCorrelationKeys()) {
        CorrelationResult result = new CorrelationResult(correlationKey.getId());
        for (CorrelationProperty property : correlationKey.getCorrelationProperties()) {
          String retrievalExpression = property.getRetrievalExpression(messageName);
          if (retrievalExpression == null) {
            result.keyDefinedForMessage = false;
            result.keyMatches = false;
            break;
          }
          
          if (!executionEntity.hasVariable(property.getId())) {
            result.keyDefinedForScope = false;
            result.keyMatches = false;
            break;
          }
          
          String resolvedProperty = xQueryHandler.runXPath(messageContent, retrievalExpression);
          
          if (!resolvedProperty.equals(executionEntity.getVariable(property.getId()))) {
            result.keyMatches = false;
            break;
          }
        }
        correlationResults.add(result);
      }
      
      if (fulfillsCorrelationCondition(correlationResults)) {
        matchingExecutions.add(waitingExecution);
      }
    }
    
    // TODO assure that all matching executions belong to the same process instance, 
    // otherwise an exception should be thrown
    
    if (matchingExecutions.isEmpty()) {
      return null;
    }
    
    return matchingExecutions.get(0);
  }
  
  /**
   * Checks that at least one key matches. For all the non-matching keys, 
   * it has to hold that they are either undefined for the message or the scope.
   */
  private boolean fulfillsCorrelationCondition(List<CorrelationResult> correlationResults) {
    boolean oneMatchingKeyOrCanBeSet = false;
    boolean allKeysMatchOrUndefined = true;
    for (CorrelationResult correlationResult : correlationResults) {
      if (correlationResult.keyMatches || (!correlationResult.keyMatches && !correlationResult.keyDefinedForScope && correlationResult.keyDefinedForMessage)) {
        oneMatchingKeyOrCanBeSet = true;
      }
      if (!correlationResult.keyMatches && correlationResult.keyDefinedForScope && correlationResult.keyDefinedForMessage) {
        allKeysMatchOrUndefined = false;
      }
    }
    
    return oneMatchingKeyOrCanBeSet && allKeysMatchOrUndefined;
  }

  private class CorrelationResult {
    
    public CorrelationResult(String correlationKeyId) {
      this.correlationKeyId = correlationKeyId;
    }
    
    String correlationKeyId;
    boolean keyMatches = true;
    boolean keyDefinedForMessage = true;
    boolean keyDefinedForScope = true;
  }

}

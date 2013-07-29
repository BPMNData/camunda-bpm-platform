package de.hpi.uni.potsdam.bpmn_to_sql.correlation;

import java.util.HashMap;
import java.util.Map;

import org.camunda.bpm.engine.impl.cmd.CorrelateMessageCmd;
import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.runtime.Execution;

import de.hpi.uni.potsdam.bpmn_to_sql.xquery.XQueryHandler;

public class CorrelateBpmnDataMessageCmd extends CorrelateMessageCmd {

  protected static final String MESSAGE_NAME_XPATH_EXPRESSION = "/message/@name";
  
  protected String messageContent;
  
  public CorrelateBpmnDataMessageCmd(String message) {
    super(null, null, null, null);
    this.messageName = extractMessageName(message);
    this.messageContent = message;
    Map<String, Object> correlationKeys = new HashMap<String, Object>();
    correlationKeys.put("message", messageContent);
    this.correlationKeys = correlationKeys;
    this.processVariables = correlationKeys;
  }


  private String extractMessageName(String message) {
    XQueryHandler handler = new XQueryHandler();
    return handler.runXPath(message, MESSAGE_NAME_XPATH_EXPRESSION);
  }


  protected void triggerExecution(CommandContext commandContext, Execution matchingExecution) {
    // ensure that the message payload is only set locally on the execution and not propagated upwards
    ((ExecutionEntity) matchingExecution).setVariablesLocal(correlationKeys);
    super.triggerExecution(commandContext, matchingExecution);
  }
  
}

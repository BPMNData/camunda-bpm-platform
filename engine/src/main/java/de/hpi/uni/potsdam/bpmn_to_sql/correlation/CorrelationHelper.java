package de.hpi.uni.potsdam.bpmn_to_sql.correlation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.engine.impl.bpmn.helper.ScopeUtil;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution;

import de.hpi.uni.potsdam.bpmn_to_sql.bpmn.CorrelationKey;
import de.hpi.uni.potsdam.bpmn_to_sql.bpmn.CorrelationProperty;
import de.hpi.uni.potsdam.bpmn_to_sql.xquery.XQueryHandler;

public class CorrelationHelper {

  /**
   * Populates all correlation keys that are applicable to this message (i.e. have correlation prop retrieval expressions)
   */
  public static void populateCorrelationKeysInScope(ActivityExecution execution, MessageInstance message, 
      List<CorrelationKey> correlationKeys) {
    // determine the correlation scope execution
    ExecutionEntity scopeExecution = determineCorrelationScopeExecution(execution); 
    
    Map<String, Object> resolvedProperties = extractCorrelationProperties(message, correlationKeys);
    scopeExecution.setVariablesLocal(resolvedProperties);
  }
  
  protected static ExecutionEntity determineCorrelationScopeExecution(ActivityExecution execution) {
    ExecutionEntity correlationScopeExecution = (ExecutionEntity) ScopeUtil.findScopeExecution(execution);
    
    return correlationScopeExecution;
  }
  
  
  public static Map<String, Object> extractCorrelationProperties(MessageInstance message, 
      List<CorrelationKey> correlationKeys) {
    Map<String, Object> extractedProperties = new HashMap<String, Object>();
    
    for (CorrelationKey key : correlationKeys) {
      for (CorrelationProperty property : key.getCorrelationProperties()) {
        String resolvedProperty = resolvePropertyFromMessage(property, message);
        if (resolvedProperty != null && !resolvedProperty.trim().isEmpty()) {
          extractedProperties.put(property.getId(), resolvedProperty);
        }
      }
    }
    
    return extractedProperties;
  }
  
  /**
   * Returns a correlation key's property value from a message or null, if the property has no retrieval expression for this message
   */
  protected static String resolvePropertyFromMessage(CorrelationProperty correlationProperty, MessageInstance message) {
    XQueryHandler handler = new XQueryHandler();
    String messageDefinitionId = message.getMessageDefinition().getOriginalId();
    String retrievalExpression = correlationProperty.getRetrievalExpression(messageDefinitionId);
    
    if (retrievalExpression == null) {
      return null;
    }
    
    String property = handler.runXPath(message.getContent(), retrievalExpression);
    return property;
  }

}

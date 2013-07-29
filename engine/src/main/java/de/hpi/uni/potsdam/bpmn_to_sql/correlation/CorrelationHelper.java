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

  public static void populateCorrelationKeysInScope(ActivityExecution execution, String message, 
      List<CorrelationKey> correlationKeys) {
    // determine the correlation scope execution
    ExecutionEntity scopeExecution = determineCorrelationScopeExecution(execution); 
    
    Map<String, Object> resolvedProperties = extractCorrelationProperties(message, correlationKeys);
    scopeExecution.setVariablesLocal(resolvedProperties);
    
//    for (CorrelationKey correlationKey : correlationKeys) {
//      for (CorrelationProperty property : correlationKey.getCorrelationProperties()) {
//        populatePropertyFromMessage(scopeExecution, property, message);
//      }
//    }
  }
  
  protected static ExecutionEntity determineCorrelationScopeExecution(ActivityExecution execution) {
    ExecutionEntity correlationScopeExecution = (ExecutionEntity) ScopeUtil.findScopeExecution(execution);
    
    return correlationScopeExecution;
  }
  
//  protected static void populatePropertyFromMessage(ActivityExecution correlationScopeExecution, CorrelationProperty correlationProperty, String message) {
//    XQueryHandler handler = new XQueryHandler();
//    String property = handler.runXPath(message, correlationProperty.getRetrievalExpression());
//    if (property != null && !property.trim().equals("")) {
//      correlationScopeExecution.setVariableLocal(correlationProperty.getId(), property);
//    }
//  }
  
  public static Map<String, Object> extractCorrelationProperties(String message, 
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
  
  protected static String resolvePropertyFromMessage(CorrelationProperty correlationProperty, String message) {
    XQueryHandler handler = new XQueryHandler();
    String property = handler.runXPath(message, correlationProperty.getRetrievalExpression());
    return property;
  }
}

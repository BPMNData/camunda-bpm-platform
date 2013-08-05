package de.hpi.uni.potsdam.bpmn_to_sql.bpmn;

import java.util.ArrayList;
import java.util.List;

import org.camunda.bpm.engine.delegate.Expression;
import org.camunda.bpm.engine.impl.bpmn.webservice.MessageDefinition;

public class MessageFlow {

  protected String id;
  protected List<CorrelationKey> correlationKeys = new ArrayList<CorrelationKey>();
  protected MessageDefinition message;
  protected Expression endpointAddressExpression;
  
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }
  public List<CorrelationKey> getCorrelationKeys() {
    return correlationKeys;
  }
  
  public void addCorrelationKey(CorrelationKey key) {
    this.correlationKeys.add(key);
  }
  
  public MessageDefinition getMessage() {
    return message;
  }
  public void setMessage(MessageDefinition message) {
    this.message = message;
  }
  public Expression getEndpointAddressExpression() {
    return endpointAddressExpression;
  }
  public void setEndpointAddressExpression(Expression endpointAddressExpression) {
    this.endpointAddressExpression = endpointAddressExpression;
  }
}

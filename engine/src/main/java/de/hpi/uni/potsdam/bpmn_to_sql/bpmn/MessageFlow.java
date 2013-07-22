package de.hpi.uni.potsdam.bpmn_to_sql.bpmn;

import java.util.List;

public class MessageFlow {

  protected String id;
  protected List<CorrelationKey> correlationKeys;
  protected BpmnMessage message;
  
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }
  public List<CorrelationKey> getCorrelationKeys() {
    return correlationKeys;
  }
  public void setCorrelationKeys(List<CorrelationKey> correlationKeys) {
    this.correlationKeys = correlationKeys;
  }
  public BpmnMessage getMessage() {
    return message;
  }
  public void setMessage(BpmnMessage message) {
    this.message = message;
  }
}

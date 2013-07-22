package de.hpi.uni.potsdam.bpmn_to_sql.bpmn;

import java.util.List;

public class CorrelationKey {

  protected String id;
  protected List<CorrelationProperty> correlationProperties;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public List<CorrelationProperty> getCorrelationProperties() {
    return correlationProperties;
  }

  public void setCorrelationProperties(List<CorrelationProperty> correlationProperties) {
    this.correlationProperties = correlationProperties;
  }
}

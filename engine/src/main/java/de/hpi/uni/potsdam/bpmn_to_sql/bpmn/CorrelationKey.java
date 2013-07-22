package de.hpi.uni.potsdam.bpmn_to_sql.bpmn;

import java.util.List;

public class CorrelationKey {

  protected List<CorrelationProperty> correlationProperties;

  public List<CorrelationProperty> getCorrelationProperties() {
    return correlationProperties;
  }

  public void setCorrelationProperties(List<CorrelationProperty> correlationProperties) {
    this.correlationProperties = correlationProperties;
  }
}

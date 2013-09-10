package de.hpi.uni.potsdam.bpmn_to_sql.bpmn;

import java.util.HashMap;
import java.util.Map;

public class CorrelationProperty {

  protected String id;
  protected String name;
  protected Map<String, String> retrievalExpressions;
  
  public CorrelationProperty() {
    this.retrievalExpressions = new HashMap<String, String>();
  }
  
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }
//  public String getName() {
//    return name;
//  }
  public void setName(String name) {
    this.name = name;
  }
  public String getRetrievalExpression(String messageName) {
    return retrievalExpressions.get(messageName);
  }
  public void addRetrievalExpression(String messageName, String retrievalExpression) {
    retrievalExpressions.put(messageName, retrievalExpression);
  }  
}

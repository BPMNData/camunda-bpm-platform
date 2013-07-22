package de.hpi.uni.potsdam.bpmn_to_sql.bpmn;

public class CorrelationProperty {

  protected String id;
  protected String retrievalExpression;
  
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }
  public String getRetrievalExpression() {
    return retrievalExpression;
  }
  public void setRetrievalExpression(String retrievalExpression) {
    this.retrievalExpression = retrievalExpression;
  }  
}

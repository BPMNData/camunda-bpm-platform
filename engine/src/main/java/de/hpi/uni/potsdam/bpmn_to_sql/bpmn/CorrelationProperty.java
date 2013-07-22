package de.hpi.uni.potsdam.bpmn_to_sql.bpmn;

public class CorrelationProperty {

  protected String id;
  protected String name;
  protected String retrievalExpression;
  
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
  public String getRetrievalExpression() {
    return retrievalExpression;
  }
  public void setRetrievalExpression(String retrievalExpression) {
    this.retrievalExpression = retrievalExpression;
  }  
}

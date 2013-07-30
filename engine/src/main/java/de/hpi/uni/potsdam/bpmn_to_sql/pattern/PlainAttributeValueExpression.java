package de.hpi.uni.potsdam.bpmn_to_sql.pattern;

public class PlainAttributeValueExpression {

  private String[] values;
  private PlainAttributeValueExpression(String[] values) {
    this.values = values;
  }
  
  public static PlainAttributeValueExpression values(String... values) {
    PlainAttributeValueExpression expression = new PlainAttributeValueExpression(values);
    return expression;
  }
  
  public static PlainAttributeValueExpression nullValue() {
    PlainAttributeValueExpression expression = new PlainAttributeValueExpression(null);
    return expression;
  }

  public String[] getAttributeValues() {
    return values;
  }
  
  
}

package de.hpi.uni.potsdam.bpmn_to_sql.pattern;

import static de.hpi.uni.potsdam.bpmn_to_sql.pattern.PlainAttributeValueExpression.values;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.AttributeSetClause;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.SqlHelper;

public class AttributeUpdate {

  private String attributeName;
  private AttributeValueExpression attributeValue;
  
  public AttributeUpdate(String attributeName, AttributeValueExpression attributeValue) {
    this.attributeName = attributeName;
    this.attributeValue = attributeValue;
  }
  
  public AttributeUpdate(String attributeName, String attributeValue) {
    this(attributeName, values(attributeValue));
  }
  
  public String getAttributeName() {
    return attributeName;
  }
  public void setAttributeName(String attributeName) {
    this.attributeName = attributeName;
  }
  public AttributeValueExpression getAttributeValue() {
    return attributeValue;
  }
  public void setAttributeValue(AttributeValueExpression attributeValue) {
    this.attributeValue = attributeValue;
  }
  
  public AttributeSetClause toSetSubClause() {
    String fullQualifiedAttributeName = SqlHelper.escapeIdentifier(attributeName);
    return new AttributeSetClause(fullQualifiedAttributeName, attributeValue.toValueSelection());
  }
}

package de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql;

public class AttributeSetClause implements SqlClause {

  private String attributeName;
  private String attributeValue;
  
  public AttributeSetClause(String attributeName, String attributeValue) {
    this.attributeName = attributeName;
    this.attributeValue = attributeValue;
  }

  public String toSqlString() {
    StringBuilder statement = new StringBuilder();
    statement.append(attributeName);
    statement.append(" = ");
    statement.append(attributeValue);
    return statement.toString();
  }

}

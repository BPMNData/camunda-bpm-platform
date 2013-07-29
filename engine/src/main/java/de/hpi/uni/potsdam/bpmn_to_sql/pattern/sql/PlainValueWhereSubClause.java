package de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql;

public class PlainValueWhereSubClause implements WhereSubClause {

  private String attributeName;
  private String[] attributeValues;
  
  public PlainValueWhereSubClause(String attributeName, String attributeValue) {
    this.attributeName = attributeName;
    this.attributeValues = new String[]{ attributeValue };
  }
  
  public PlainValueWhereSubClause(String attributeName, String[] attributeValues) {
    this.attributeName = attributeName;
    this.attributeValues = attributeValues;
  }
  
  public String toSqlString() {
    StringBuilder sqlClause = new StringBuilder();
    sqlClause.append(attributeName);
    
    if (attributeValues == null || attributeName.length() == 0) {
      sqlClause.append(" IS NULL");
      return sqlClause.toString();
    }
    
    sqlClause.append(" = ");
    
    if (attributeValues.length > 1) {
      sqlClause.append("(");
    }
    
    for (int i = 0; i < attributeValues.length; i++) {
      sqlClause.append(attributeValues[i]);
      
      if (i < attributeValues.length - 1) {
        sqlClause.append(" OR ");
      }
    }
    
    if (attributeValues.length > 1) {
      sqlClause.append(")");
    }
    
    return sqlClause.toString();
  }
}

package de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql;

import org.camunda.bpm.engine.ProcessEngineException;

public class PlainValueWhereSubClause implements WhereSubClause {

  private String attributeName;
  private String[] attributeValues;
  
  public PlainValueWhereSubClause(String attributeName, String attributeValue) {
    this.attributeName = attributeName;
    if (attributeValue != null) {
      this.attributeValues = new String[]{ attributeValue };
    }
  }
  
  public PlainValueWhereSubClause(String attributeName, String[] attributeValues) {
    this.attributeName = attributeName;
    this.attributeValues = attributeValues;
  }
  
  public String toSqlString() {
    StringBuilder sqlClause = new StringBuilder();
    sqlClause.append(attributeName);
    
    if (attributeValues == null) {
      sqlClause.append(" IS NULL");
      return sqlClause.toString();
    }
    
    if (attributeValues.length == 1) {
      sqlClause.append(" = ");
      sqlClause.append(attributeValues[0]);
      return sqlClause.toString();
    } else if (attributeValues.length > 1) {
      sqlClause.append(" IN ");
      sqlClause.append("(");
      for (int i = 0; i < attributeValues.length; i++) {
        sqlClause.append(attributeValues[i]);
        
        if (i < attributeValues.length - 1) {
          sqlClause.append(", ");
        }
      }
      sqlClause.append(")");
      return sqlClause.toString();
    } else {
      throw new ProcessEngineException("Clause has to contain at least one value");
    }
    /*
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
    
    return sqlClause.toString();*/
  }
}

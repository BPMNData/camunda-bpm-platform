package de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql;

public class InsertStatement implements SqlClause {

  private String tableName;
  private String[] attributes;
  
  private ValuesClause values;
  
  public InsertStatement(String tableName, String... attributes) {
    this.tableName = tableName;
    this.attributes = attributes;
  }
  
  public void setValues(ValuesClause values) {
    this.values = values;
  }

  public String toSqlString() {
    StringBuilder statement = new StringBuilder("INSERT INTO ");
    statement.append(tableName);
    statement.append("(");
    
    for (int i = 0; i < attributes.length; i++) {
      String attribute = attributes[i];
      statement.append(attribute);
      if (i < attributes.length - 1) {
        statement.append(", ");
      }
    }
    
    statement.append(") ");
    
    statement.append(values.toSqlString());
    
    return statement.toString();
  }
  
}

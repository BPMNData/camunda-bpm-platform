package de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql;

public class ValuesSubClause implements SqlClause {

  private String[] values;
  
  public ValuesSubClause(String... values) {
    this.values = values;
  }

  public String toSqlString() {
    StringBuilder statement = new StringBuilder();
    statement.append("(");
    
    for (int i = 0; i < values.length; i++) {
      String value = values[i];
      statement.append(value);
      if (i < values.length - 1) {
        statement.append(", ");
      }
    }
    statement.append(")");
    
    return statement.toString();
  }
  
  
}

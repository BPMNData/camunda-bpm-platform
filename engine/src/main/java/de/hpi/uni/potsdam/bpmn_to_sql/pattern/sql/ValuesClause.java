package de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql;

import java.util.ArrayList;
import java.util.List;

public class ValuesClause implements SqlClause {

  private List<ValuesSubClause> values;
  
  public ValuesClause() {
    this.values = new ArrayList<ValuesSubClause>();
  }

  public void addValuesSubClause(ValuesSubClause valuesSubClause) {
    this.values.add(valuesSubClause);
  }
  
  public String toSqlString() {
    StringBuilder statement = new StringBuilder("VALUES ");
    
    for (int i = 0; i < values.size(); i++) {
      ValuesSubClause subClause = values.get(i);
      statement.append(subClause.toSqlString());
      
      if (i < values.size() - 1) {
        statement.append(", ");
      }
    }
    
    return statement.toString();
  }
  
  
}

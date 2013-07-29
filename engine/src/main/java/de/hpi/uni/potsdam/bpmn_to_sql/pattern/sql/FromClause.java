package de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql;

import java.util.ArrayList;
import java.util.List;

public class FromClause implements SqlClause {

  private List<String> tableNames;
  
  public FromClause() {
    this.tableNames = new ArrayList<String>();
  }
  
  public void addTableName(String tableName) {
    this.tableNames.add(tableName);
  }
  
  public String toSqlString() {
    if (tableNames.size() == 0) {
      throw new RuntimeException("");
    }
    
    StringBuilder statement = new StringBuilder("FROM ");
    for (int i = 0; i < tableNames.size(); i++) {
      String tableName = tableNames.get(i);
      statement.append(tableName);
      if (i < tableNames.size() - 1) {
        statement.append(", ");
      }
    }
    return statement.toString();
  }

}

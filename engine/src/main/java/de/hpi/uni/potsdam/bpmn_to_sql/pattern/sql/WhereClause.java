package de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql;

import java.util.ArrayList;
import java.util.List;

public class WhereClause implements SqlClause {

  private List<WhereSubClause> subClauses;
  
  public WhereClause() {
    subClauses = new ArrayList<WhereSubClause>();
  }
  
  public void addSubClause(WhereSubClause subClause) {
    this.subClauses.add(subClause);
  }
  
  public void addSubClauses(List<WhereSubClause> subClauses) {
    this.subClauses.addAll(subClauses);
  }
  
  public String toSqlString() {
    if (subClauses.isEmpty()) {
      return "";
    }
    
    StringBuilder statement = new StringBuilder("WHERE ");
    
    for (int i = 0; i < subClauses.size(); i++) {
      WhereSubClause subClause = subClauses.get(i);
      statement.append(subClause.toSqlString());
      if (i < subClauses.size() - 1) {
        statement.append(" AND ");
      }
    }
    return statement.toString();
  }

}

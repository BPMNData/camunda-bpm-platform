package de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql;

public class DeleteStatement implements SqlClause {

  private String tableName;
  private WhereClause whereClause;
  
  public DeleteStatement(String tableName) {
    this.tableName = tableName;
  }

  public String toSqlString() {
    StringBuilder statement = new StringBuilder("DELETE FROM ");
    statement.append(tableName);
    statement.append(" ");
    if (whereClause != null) {
      statement.append(whereClause.toSqlString());
    }
    
    return statement.toString();
  }

  public void setWhereClause(WhereClause whereClause) {
    this.whereClause = whereClause;
  }

}

package de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql;

public class UpdateStatement implements SqlClause {

  private String tableName;
  private SetClause setClause;
  private WhereClause whereClause;
  
  public UpdateStatement(String tableName) {
    this.tableName = tableName;
  }

  public String toSqlString() {
    StringBuilder statement = new StringBuilder("UPDATE ");
    statement.append(tableName);
    statement.append(" ");
    statement.append(setClause.toSqlString());
    statement.append(" ");
    
    if (whereClause != null) {
      statement.append(whereClause.toSqlString());
    }
    
    return statement.toString();
  }


  public void setSetClause(SetClause set) {
    this.setClause = set;
  }

  public void setWhereClause(WhereClause where) {
    this.whereClause = where;
  }

}

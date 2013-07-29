package de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql;

public class SelectStatement implements SqlClause {

  private SelectClause selectClause;
  private WhereClause whereClause;
  private FromClause fromClause;
  
  
  
  public void setSelectClause(SelectClause selectClause) {
    this.selectClause = selectClause;
  }

  public void setWhereClause(WhereClause whereClause) {
    this.whereClause = whereClause;
  }

  public void setFromClause(FromClause fromClause) {
    this.fromClause = fromClause;
  }

  public String toSqlString() {
    StringBuilder statement = new StringBuilder();
    statement.append(selectClause.toSqlString());
    statement.append(" ");
    statement.append(fromClause.toSqlString());
    statement.append(" ");
    statement.append(whereClause.toSqlString());
    return statement.toString();
  }
}

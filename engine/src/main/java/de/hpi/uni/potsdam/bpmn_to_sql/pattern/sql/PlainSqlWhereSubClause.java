package de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql;

/**
 * Only for compatibility with original implementation, should probably be removed as it breaks
 * the intent of having strongly-typed sql statement creation.
 * 
 * @author Thorben
 */
@Deprecated
public class PlainSqlWhereSubClause implements WhereSubClause {

  private String sql;
  
  public PlainSqlWhereSubClause(String sql) {
    this.sql = sql;
  }
  
  public String toSqlString() {
    return sql;
  }

}

package de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql;

public class SelectClause implements SqlClause {

  private String[] projections;
  
  public SelectClause(String... projections) {
    this.projections = projections;
  }
  
  public String toSqlString() {
    if (projections.length == 0) {
      throw new RuntimeException("Cannot select with empty projections");
    }
    
    StringBuilder statement = new StringBuilder("SELECT ");
    for (int i = 0; i < projections.length; i++) {
      String projection = projections[i];
      statement.append(projection);
      if (i < projections.length - 1) {
        statement.append(", ");
      }
    }
    
    return statement.toString();
  }

  
}

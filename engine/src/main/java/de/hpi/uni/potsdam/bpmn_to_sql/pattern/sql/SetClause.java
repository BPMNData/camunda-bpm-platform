package de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql;

import java.util.ArrayList;
import java.util.List;

public class SetClause implements SqlClause {

  private List<AttributeSetClause> subClauses;
  
  public SetClause() {
    this.subClauses = new ArrayList<AttributeSetClause>();
  }
  
  public String toSqlString() {
    StringBuilder statement = new StringBuilder("SET ");
    
    for (int i = 0; i < subClauses.size(); i++) {
      AttributeSetClause subClause = subClauses.get(i);
      statement.append(subClause.toSqlString());
      if (i < subClauses.size() - 1) {
        statement.append(", ");
      }
    }
    
    return statement.toString();
  }

  public void addAttributeClause(AttributeSetClause attributeClause) {
    this.subClauses.add(attributeClause);
  }

}

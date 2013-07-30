package de.hpi.uni.potsdam.bpmn_to_sql.pattern;

import java.util.ArrayList;
import java.util.List;

import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.PlainValueWhereSubClause;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.WhereSubClause;

public class DataObjectReference {
  
  private String foreignKey;
  private DataObjectSpecification referencedObject;
  
  public DataObjectReference(String foreignKey, DataObjectSpecification referencedObject) {
    this.foreignKey = foreignKey;
    this.referencedObject = referencedObject;
  }

  public List<WhereSubClause> resolveSelections(DataObjectSpecification referencingObject) {
    List<WhereSubClause> subClauses = new ArrayList<WhereSubClause>();
    String attributeName = referencingObject.getName() + "." + foreignKey;
    String attributeValue = referencedObject.getName() + "." + referencedObject.getPkAttribute();
    WhereSubClause clause = new PlainValueWhereSubClause(attributeName, attributeValue);
    subClauses.add(clause);
    subClauses.addAll(referencedObject.getWhereSubClauses());
    return subClauses;
    
  }
  
  public String resolveTableName() {
    return referencedObject.getName();
  }
}

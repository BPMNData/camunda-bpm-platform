package de.hpi.uni.potsdam.bpmn_to_sql.pattern;

import java.util.ArrayList;
import java.util.List;

import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.PlainValueWhereSubClause;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.SelectStatement;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.SqlHelper;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.WhereSubClause;

public class DataObjectReference implements AttributeValueExpression {

  private DataObjectSpecification referencedObject;
  
  public DataObjectReference(DataObjectSpecification referencedObject) {
    this.referencedObject = referencedObject;
  }
  
  public List<WhereSubClause> toWhereSubClauses(String attribute) {
    List<WhereSubClause> subClauses = new ArrayList<WhereSubClause>();
    String attributeValue = SqlHelper.escapeIdentifier(referencedObject.getName()) + "." + SqlHelper.escapeIdentifier(referencedObject.getPkAttribute());
    WhereSubClause clause = new PlainValueWhereSubClause(attribute, attributeValue);
    subClauses.add(clause);
    subClauses.addAll(referencedObject.getWhereSubClauses());
    
    return subClauses;
  }

  public String toValueSelection() {
    SelectStatement selectPk = referencedObject.getSelectPkStatement();
    
    return "(" + selectPk.toSqlString() + ")";
  }

}

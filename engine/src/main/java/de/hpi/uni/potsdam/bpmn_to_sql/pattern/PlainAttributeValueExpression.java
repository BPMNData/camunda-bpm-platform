package de.hpi.uni.potsdam.bpmn_to_sql.pattern;

import java.util.ArrayList;
import java.util.List;

import org.camunda.bpm.engine.ProcessEngineException;

import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.PlainValueWhereSubClause;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.WhereSubClause;

public class PlainAttributeValueExpression implements AttributeValueExpression {

  private String[] values;
  private PlainAttributeValueExpression(String[] values) {
    this.values = values;
  }
  
  public static PlainAttributeValueExpression values(String... values) {
    PlainAttributeValueExpression expression = new PlainAttributeValueExpression(values);
    return expression;
  }
  
  public static PlainAttributeValueExpression nullValue() {
    PlainAttributeValueExpression expression = new PlainAttributeValueExpression(null);
    return expression;
  }

  public String[] getAttributeValues() {
    return values;
  }

  public List<WhereSubClause> toWhereSubClauses(String attribute) {
    List<WhereSubClause> subClauses = new ArrayList<WhereSubClause>();
    WhereSubClause clause = new PlainValueWhereSubClause(attribute, values);
    subClauses.add(clause);
    return subClauses;
  }

  public String toValueSelection() {
    if (values.length != 1) {
      throw new ProcessEngineException("Cannot select multiple values");
    }
    
    return values[0];
  }
  
  
}

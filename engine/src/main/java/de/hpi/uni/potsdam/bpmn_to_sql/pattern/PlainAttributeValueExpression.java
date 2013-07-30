package de.hpi.uni.potsdam.bpmn_to_sql.pattern;

import java.util.ArrayList;
import java.util.List;

import org.camunda.bpm.engine.ProcessEngineException;

import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.PlainValueWhereSubClause;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.SqlHelper;
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
    PlainAttributeValueExpression expression = new PlainAttributeValueExpression(new String[]{null});
    return expression;
  }

  public String[] getAttributeValues() {
    return values;
  }

  public List<WhereSubClause> toJoinWhereSubClauses(String attribute) {
    List<WhereSubClause> subClauses = new ArrayList<WhereSubClause>();
    String[] escapedValues = new String[values.length];
    for (int i = 0; i < values.length; i++) {
      escapedValues[i] = SqlHelper.escapeStringLiteral(values[i]);
    }
    
    if (values == null || (values.length == 1 && values[0] == null)) {
      escapedValues = null;
    }
    
    WhereSubClause clause = new PlainValueWhereSubClause(attribute, escapedValues);
    subClauses.add(clause);
    return subClauses;
  }

  public String toValueSelection() {
    if (values.length != 1) {
      throw new ProcessEngineException("Cannot select multiple values");
    }
    if (values[0] == null) {
      return null;
    }
    
    return SqlHelper.escapeStringLiteral(values[0]);
  }

  public WhereSubClause toSubSelectWhereSubClause(String attribute) {
    // FIXME this allows only a single value
    return new PlainValueWhereSubClause(attribute, toValueSelection());
  }
  
  
}

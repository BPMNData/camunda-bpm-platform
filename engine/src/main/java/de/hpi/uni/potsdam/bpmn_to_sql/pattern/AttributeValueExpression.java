package de.hpi.uni.potsdam.bpmn_to_sql.pattern;

import java.util.List;

import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.WhereSubClause;

public interface AttributeValueExpression {

  List<WhereSubClause> toJoinWhereSubClauses(String attribute);
  
  WhereSubClause toSubSelectWhereSubClause(String attribute);
  
  String toValueSelection();
}

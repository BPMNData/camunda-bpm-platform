package de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql;

public class SqlHelper {

  public static String escapeIdentifier(String identifier) {
    return "`" + identifier + "`";
  }
  
  public static String escapeStringLiteral(String literal) {
    return "\"" + literal + "\"";
  }
}

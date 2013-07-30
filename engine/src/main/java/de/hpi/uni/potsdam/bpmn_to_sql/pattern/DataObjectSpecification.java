package de.hpi.uni.potsdam.bpmn_to_sql.pattern;

import static de.hpi.uni.potsdam.bpmn_to_sql.pattern.PlainAttributeValueExpression.values;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.AttributeSetClause;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.FromClause;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.PlainValueWhereSubClause;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.SelectClause;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.SelectStatement;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.SetClause;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.SqlHelper;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.UpdateStatement;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.WhereClause;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.WhereSubClause;

public class DataObjectSpecification {

  private String name;
  private String pkAttribute;
  private String pkValue;
  
  private SortedMap<String, AttributeValueExpression> attributeValues;
  
  private Set<String> tables;
  
  public DataObjectSpecification() {
    attributeValues = new TreeMap<String, AttributeValueExpression>();
    tables = new HashSet<String>();
  }
  
  public String getName() {
    return name;
  }
  
  public String getPkAttribute() {
    return pkAttribute;
  }

  public String getPkValue() {
    return pkValue;
  }
  
  public SortedSet<String> getCanonicalAttributeNames() {
    TreeSet<String> attributes = new TreeSet<String>();
    attributes.add(pkAttribute);
    attributes.addAll(attributeValues.keySet());
    return attributes;
  }

  public String getAttributeValueStatement(String attribute) {
    if (attribute.equals(pkAttribute)) {
      return SqlHelper.escapeStringLiteral(pkValue);
    }
    
    AttributeValueExpression expression = attributeValues.get(attribute);
    return expression.toValueSelection();
  }


  /**
   * Matches any object of the given type
   */
  public static DataObjectSpecification anyDataObject(String name, String pkAttribute) {
    return dataObject(name, pkAttribute, null);
  }
  

  /**
   * Matches a single object of the given type
   */
  public static DataObjectSpecification dataObject(String name, String pkAttribute, String pkValue) {
    DataObjectSpecification spec = new DataObjectSpecification();
    spec.name = name;
    spec.pkAttribute = pkAttribute;
    spec.pkValue = pkValue;
    return spec;
  }
  
  public DataObjectSpecification references(String foreignKeyAttribute, DataObjectSpecification anotherObject) {
    DataObjectReference reference = new DataObjectReference(anotherObject);
    attributeValues.put(foreignKeyAttribute, reference);
    tables.add(anotherObject.name);
    return this;
  }
  
  public DataObjectSpecification attribute(String name, String value) {
    return attribute(name, values(value));
  }
  
  public DataObjectSpecification attribute(String name, PlainAttributeValueExpression valueExpression) {
    attributeValues.put(name, valueExpression);
    return this;
  }
  
  public SelectStatement getSelectPkStatement() {
    SelectStatement statement = new SelectStatement();
    
    FromClause fromClause = buildFromClause();
    WhereClause whereClause = buildJoinWhereClause();
    
    statement.setFromClause(fromClause);
    statement.setWhereClause(whereClause);
    
    String fullQualifiedPrimaryKey = SqlHelper.escapeIdentifier(name) + "." + SqlHelper.escapeIdentifier(pkAttribute);
    SelectClause selectClause = new SelectClause(fullQualifiedPrimaryKey);
    statement.setSelectClause(selectClause);
    
    return statement;
  }
  
  public SelectStatement getSelectStarStatement() {
    SelectStatement statement = new SelectStatement();
    
    FromClause fromClause = buildFromClause();
    WhereClause whereClause = buildJoinWhereClause();
    
    statement.setFromClause(fromClause);
    statement.setWhereClause(whereClause);
    
    SelectClause selectClause = new SelectClause("*");
    statement.setSelectClause(selectClause);
    
    return statement;
  }
  
  public SelectStatement getSelectCountStatement() {
    SelectStatement statement = new SelectStatement();
    
    FromClause fromClause = buildFromClause();
    WhereClause whereClause = buildJoinWhereClause();
    
    statement.setFromClause(fromClause);
    statement.setWhereClause(whereClause);
    
    String fullQualifiedPrimaryKey = SqlHelper.escapeIdentifier(name) + "." + SqlHelper.escapeIdentifier(pkAttribute);
    SelectClause selectClause = new SelectClause("COUNT(" + fullQualifiedPrimaryKey + ")");
    statement.setSelectClause(selectClause);
    
    return statement;
  }

  public UpdateStatement getUpdateStatement(List<AttributeUpdate> updates) {
    UpdateStatement statement = new UpdateStatement(SqlHelper.escapeIdentifier(name));
    
    SetClause setClause = new SetClause();
    
    for (AttributeUpdate update : updates) {
      AttributeSetClause setSubClause = update.toSetSubClause();
      setClause.addAttributeClause(setSubClause);
    }
    statement.setSetClause(setClause);
    
    WhereClause whereClause = buildSubSelectWhereClause();
    statement.setWhereClause(whereClause);
    
    return statement;
  }
  
  private FromClause buildFromClause() {
    FromClause fromClause = new FromClause();
    fromClause.addTableName(SqlHelper.escapeIdentifier(name));
    
    for (String tableName : tables) {
      fromClause.addTableName(SqlHelper.escapeIdentifier(tableName));
    }
    
    return fromClause;
  }
  
  private WhereClause buildJoinWhereClause() {
    WhereClause whereClause = new WhereClause();
    
    whereClause.addSubClauses(getJoinWhereSubClauses());
    return whereClause;
  }
  
  private WhereClause buildSubSelectWhereClause() {
    WhereClause whereClause = new WhereClause();
    
    whereClause.addSubClauses(getSubSelectWhereSubClauses());
    return whereClause;
  }
  
  
  // TODO remove code duplication with following method
  public List<WhereSubClause> getJoinWhereSubClauses() {
    List<WhereSubClause> subClauses = new ArrayList<WhereSubClause>();
    
    if (pkValue != null) {
      String fullQualifiedAttributeName = SqlHelper.escapeIdentifier(name) + "." + SqlHelper.escapeIdentifier(pkAttribute);
      WhereSubClause pkClause = new PlainValueWhereSubClause(fullQualifiedAttributeName, SqlHelper.escapeStringLiteral(pkValue));
      subClauses.add(pkClause);
    }
    
    for (Map.Entry<String, AttributeValueExpression> attributePair : attributeValues.entrySet()) {
      String fullQualifiedAttributeName = SqlHelper.escapeIdentifier(name) + "." + SqlHelper.escapeIdentifier(attributePair.getKey());
      subClauses.addAll(attributePair.getValue().toJoinWhereSubClauses(fullQualifiedAttributeName));
    }
    
    return subClauses;
  }
  
  public List<WhereSubClause> getSubSelectWhereSubClauses() {
    List<WhereSubClause> subClauses = new ArrayList<WhereSubClause>();
    
    if (pkValue != null) {
      String fullQualifiedAttributeName = SqlHelper.escapeIdentifier(name) + "." + SqlHelper.escapeIdentifier(pkAttribute);
      WhereSubClause pkClause = new PlainValueWhereSubClause(fullQualifiedAttributeName, SqlHelper.escapeStringLiteral(pkValue));
      subClauses.add(pkClause);
    }
    
    for (Map.Entry<String, AttributeValueExpression> attributePair : attributeValues.entrySet()) {
      String fullQualifiedAttributeName = SqlHelper.escapeIdentifier(name) + "." + SqlHelper.escapeIdentifier(attributePair.getKey());
      subClauses.add(attributePair.getValue().toSubSelectWhereSubClause(fullQualifiedAttributeName));
    }
    
    return subClauses;
  }

}

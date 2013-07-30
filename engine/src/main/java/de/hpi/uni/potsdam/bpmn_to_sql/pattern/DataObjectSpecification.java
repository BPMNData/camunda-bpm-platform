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

import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.FromClause;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.PlainValueWhereSubClause;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.SelectClause;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.SelectStatement;
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
      return pkValue;
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
    WhereClause whereClause = buildWhereClause();
    
    statement.setFromClause(fromClause);
    statement.setWhereClause(whereClause);
    
    SelectClause selectClause = new SelectClause(name + "." + pkAttribute);
    statement.setSelectClause(selectClause);
    
    return statement;
  }
  
  public SelectStatement getSelectStarStatement() {
    SelectStatement statement = new SelectStatement();
    
    FromClause fromClause = buildFromClause();
    WhereClause whereClause = buildWhereClause();
    
    statement.setFromClause(fromClause);
    statement.setWhereClause(whereClause);
    
    SelectClause selectClause = new SelectClause("*");
    statement.setSelectClause(selectClause);
    
    return statement;
  }
  
  public SelectStatement getSelectCountStatement() {
    SelectStatement statement = new SelectStatement();
    
    FromClause fromClause = buildFromClause();
    WhereClause whereClause = buildWhereClause();
    
    statement.setFromClause(fromClause);
    statement.setWhereClause(whereClause);
    
    SelectClause selectClause = new SelectClause("COUNT(" + name + "." + pkAttribute + ")");
    statement.setSelectClause(selectClause);
    
    return statement;
  }
  
  private FromClause buildFromClause() {
    FromClause fromClause = new FromClause();
    fromClause.addTableName(name);
    
    for (String tableName : tables) {
      fromClause.addTableName(tableName);
    }
    
    return fromClause;
  }
  
  private WhereClause buildWhereClause() {
    WhereClause whereClause = new WhereClause();
    
    whereClause.addSubClauses(getWhereSubClauses());
    return whereClause;
  }
  
  
  
  public List<WhereSubClause> getWhereSubClauses() {
    List<WhereSubClause> subClauses = new ArrayList<WhereSubClause>();
    
    if (pkValue != null) {
      String attributeName = name + "." + pkAttribute;
      WhereSubClause pkClause = new PlainValueWhereSubClause(attributeName, pkValue);
      subClauses.add(pkClause);
    }
    
    for (Map.Entry<String, AttributeValueExpression> attributePair : attributeValues.entrySet()) {
      String fullQualifiedAttributeName = name + "." + attributePair.getKey();
      subClauses.addAll(attributePair.getValue().toWhereSubClauses(fullQualifiedAttributeName));
    }
    /*
    for (Map.Entry<String, DataObjectReference> reference : referencedObjects.entrySet()) {
      List<WhereSubClause> referenceSubClauses = new ArrayList<WhereSubClause>();
      String foreignKey = reference.getKey();
      
      String fullQualifiedAttributeName = this.name + "." + foreignKey;
      subClauses.addAll(reference.getValue().toWhereSubClauses(fullQualifiedAttributeName));
      
//      String attributeValue = referencedObject.getName() + "." + referencedObject.getPkAttribute();
//      WhereSubClause clause = new PlainValueWhereSubClause(attributeName, attributeValue);
//      subClauses.add(clause);
//      subClauses.addAll(referencedObject.getWhereSubClauses());
      subClauses.addAll(referenceSubClauses);
    }*/
    
    return subClauses;
  }
}

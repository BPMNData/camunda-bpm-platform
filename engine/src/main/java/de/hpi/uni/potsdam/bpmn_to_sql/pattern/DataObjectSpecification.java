package de.hpi.uni.potsdam.bpmn_to_sql.pattern;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.FromClause;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.PlainValueWhereSubClause;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.SelectClause;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.SelectStatement;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.WhereClause;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.WhereSubClause;

import static de.hpi.uni.potsdam.bpmn_to_sql.pattern.PlainAttributeValueExpression.values;

public class DataObjectSpecification {

  private String name;
  private String pkAttribute;
  private String pkValue;
  
  private List<DataObjectReference> referencedObjects;
  private Map<String, PlainAttributeValueExpression> attributes;
  
  
  
  public DataObjectSpecification() {
    referencedObjects = new ArrayList<DataObjectReference>();
    attributes = new HashMap<String, PlainAttributeValueExpression>();
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
    DataObjectReference reference = new DataObjectReference(foreignKeyAttribute, anotherObject);
    referencedObjects.add(reference);
    return this;
  }
  
  public DataObjectSpecification attribute(String name, String value) {
    return attribute(name, values(value));
  }
  
  public DataObjectSpecification attribute(String name, PlainAttributeValueExpression valueExpression) {
    attributes.put(name, valueExpression);
    return this;
  }
  
  public SelectStatement getSelectCountStatement() {
    SelectStatement statement = new SelectStatement();
    
    FromClause fromClause = new FromClause();
    fromClause.addTableName(name);
    
    WhereClause whereClause = new WhereClause();
    
    whereClause.addSubClauses(getWhereSubClauses());
    
    for (DataObjectReference referencedObject : referencedObjects) {
      fromClause.addTableName(referencedObject.resolveTableName());
//      List<WhereSubClause> subClauses = referencedObject.getWhereSubClauses();
//      whereClause.addSubClauses(subClauses);
    }
    
    statement.setFromClause(fromClause);
    statement.setWhereClause(whereClause);
    
    SelectClause selectClause = new SelectClause("COUNT(" + name + "." + pkAttribute + ")");
    statement.setSelectClause(selectClause);
    
    return statement;
  }
  
  public List<WhereSubClause> getWhereSubClauses() {
    List<WhereSubClause> subClauses = new ArrayList<WhereSubClause>();
    
    if (pkValue != null) {
      String attributeName = name + "." + pkAttribute;
      WhereSubClause pkClause = new PlainValueWhereSubClause(attributeName, pkValue);
      subClauses.add(pkClause);
    }
    
    for (Map.Entry<String, PlainAttributeValueExpression> attributePair : attributes.entrySet()) {
      String attributeName = name + "." + attributePair.getKey();
      String[] attributeValues = attributePair.getValue().getAttributeValues();
      WhereSubClause clause = new PlainValueWhereSubClause(attributeName, attributeValues);
      subClauses.add(clause);
    }
    
    for (DataObjectReference referencedObject : referencedObjects) {
      subClauses.addAll(referencedObject.resolveSelections(this));
    }
    
    return subClauses;
  }
}

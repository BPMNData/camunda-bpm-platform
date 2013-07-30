package de.hpi.uni.potsdam.bpmn_to_sql.pattern;

import java.util.ArrayList;
import java.util.List;

import org.camunda.bpm.engine.ProcessEngineException;

import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.InsertStatement;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.SqlHelper;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.ValuesClause;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.ValuesSubClause;

public class InsertObjectSpecification {

  private List<DataObjectSpecification> objectsToInsert;
  
  private InsertObjectSpecification() {
    this.objectsToInsert = new ArrayList<DataObjectSpecification>();
  }
  
  public static InsertObjectSpecification insert() {
    InsertObjectSpecification spec = new InsertObjectSpecification();
    return spec;
  }
  
  public InsertObjectSpecification object(DataObjectSpecification dataObject) {
    objectsToInsert.add(dataObject);
    return this;
  }
  
  public InsertStatement getStatement() {
    
    String dataObjectName = null;
    
    ValuesClause valuesClause = new ValuesClause();
    String[] attributes = null;
    
    for (DataObjectSpecification object : objectsToInsert) {
      if (dataObjectName == null) {
        dataObjectName = object.getName();
      }
      if (!dataObjectName.equals(object.getName())) {
        throw new ProcessEngineException("Can only insert objects of the same type in one statement");
      }
      if (attributes == null) {
        attributes = object.getCanonicalAttributeNames().toArray(new String[0]);
      }
      

      String[] attributeValues = new String[attributes.length];
      
      for (int i = 0; i < attributes.length; i++) {
        // TODO add validation and exception handling
        attributeValues[i] = object.getAttributeValueStatement(attributes[i]);
      }
      
      ValuesSubClause subClause = new ValuesSubClause(attributeValues);
      valuesClause.addValuesSubClause(subClause);
    }
    
    String tableName = SqlHelper.escapeIdentifier(dataObjectName);
    InsertStatement statement = new InsertStatement(tableName, escapeIdentifierArray(attributes));
    statement.setValues(valuesClause);
    return statement;
  }
  
  private String[] escapeIdentifierArray(String[] identifiers) {
    String[] escapedIdentifiers = new String[identifiers.length];
    for (int i = 0; i < identifiers.length; i++) {
      escapedIdentifiers[i] = SqlHelper.escapeIdentifier(identifiers[i]);
    }
    return escapedIdentifiers;
  }
}
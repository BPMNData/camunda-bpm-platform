package de.hpi.uni.potsdam.test.bpmn_to_sql.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.engine.ProcessEngineException;
import org.junit.Assert;

public class PersistentObjectAssertionSpecification {

  private String tableType;
  private Map<String, Object> attributeAssertions;
  private Map<String, Object> selections;
  private int numMatchingObjects;
  
  private static PersistentObjectManager poManager;
  
  private static final String SINGLE_OBJECT_ERROR_MESSAGE_FORMAT = 
      "Persistent Object %s[%s]: attribute %s does not match.";
  private static final String MISMATCHING_OBJECT_NUMBER_ERROR_MESSAGE_FORMAT = 
      "Persistent Table %s: Mismatching result set size for selections.";
  
  private PersistentObjectAssertionSpecification() {
    selections = new HashMap<String, Object>();
    attributeAssertions = new HashMap<String, Object>();
  }
  
  public static PersistentObjectAssertionSpecification dataObjects(String type, int numMatchingObjects) {
    PersistentObjectAssertionSpecification spec = new PersistentObjectAssertionSpecification();
    spec.tableType = type;
    spec.numMatchingObjects = numMatchingObjects;
    return spec;
  }
  
  public void doAssert() {
    if (poManager == null) {
      throw new ProcessEngineException("No persistent object manager set.");
    }
    
    List<Map<String, Object>> results = poManager.getPersistentObjects(tableType, selections);
    
    String sizeAssertionMessage = String.format(MISMATCHING_OBJECT_NUMBER_ERROR_MESSAGE_FORMAT, tableType);
    Assert.assertEquals(sizeAssertionMessage, numMatchingObjects, results.size());
    
    for (Map<String, Object> resultObject : results) {
      for (Map.Entry<String, Object> assertionPair : attributeAssertions.entrySet()) {
        String assertionMessage = String.format(SINGLE_OBJECT_ERROR_MESSAGE_FORMAT, 
            tableType, resultObject, assertionPair.getKey());
        Assert.assertEquals(assertionMessage, assertionPair.getValue(), resultObject.get(assertionPair.getKey()));
      }
    }
  }
  
  public PersistentObjectAssertionSpecification where(String attributeName, Object attributeValue) {
    selections.put(attributeName, attributeValue);
    return this;
  }
  
  public PersistentObjectAssertionSpecification shouldHave(String attributeName, Object value) {
    attributeAssertions.put(attributeName, value);
    return this;
  }
  
  public static void setPersitentObjectManager(PersistentObjectManager poManager) {
    PersistentObjectAssertionSpecification.poManager = poManager;
  }
}

package de.hpi.uni.potsdam.bpmn_to_sql.execution;

import static de.hpi.uni.potsdam.bpmn_to_sql.pattern.DataObjectSpecification.anyDataObject;
import static de.hpi.uni.potsdam.bpmn_to_sql.pattern.DataObjectSpecification.dataObject;
import static de.hpi.uni.potsdam.bpmn_to_sql.pattern.PlainAttributeValueExpression.nullValue;
import static de.hpi.uni.potsdam.bpmn_to_sql.pattern.PlainAttributeValueExpression.values;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.camunda.bpm.engine.impl.bpmn.data.AbstractDataAssociation;
import org.camunda.bpm.engine.impl.bpmn.parser.BpmnParse;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;

import de.hpi.uni.potsdam.bpmn_to_sql.bpmn.DataAssociation;
import de.hpi.uni.potsdam.bpmn_to_sql.bpmn.DataObject;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.DataObjectSpecification;

public class RefactoredDataInputChecker {
  
  public boolean checkDataInput(ExecutionEntity execution) {
    ActivityImpl activity = execution.getActivity();
    if (activity.getDataInputAssociations().isEmpty()){
      return true;
    }
//    if (!BpmnParse.getInputData().containsKey(execution.getActivity().getId())) {
//      return true;
//    }

    final String activityId = activity.getId();
    final String activityParentId = activity.getParent().getId();
    final String dataObjectID = execution.getEffectiveCaseObjectID();
    // final AtomicOperationActivityExecute taskBehavior = this;
    
    HashMap<String, ArrayList<DataObject>> dataObjects = getDataInputsOfActivity(activity);
    HashMap<String, Integer> queryMap = createQueryMap(dataObjects, activityId, activityParentId, dataObjectID);

    return isInputDataAvailable(queryMap);
  }
  
  private HashMap<String, ArrayList<DataObject>> getDataInputsOfActivity(ActivityImpl activity){
    HashMap<String, ArrayList<DataObject>> dataObjectMap = new HashMap<String, ArrayList<DataObject>>();
    for(AbstractDataAssociation inputDataAssociation : activity.getDataInputAssociations()){
      DataObject dataObject = ((DataAssociation)inputDataAssociation).getSourceObject();
      if(dataObject != null){
        if (dataObjectMap.containsKey(dataObject.getName())) {
          ArrayList<DataObject> al = dataObjectMap.get(dataObject.getName());
          al.add(dataObject);
          dataObjectMap.put(dataObject.getName(), al);
        } else {
          ArrayList<DataObject> al = new ArrayList<DataObject>();
          al.add(dataObject);
          dataObjectMap.put(dataObject.getName(), al);
        }        
      }
    }
    return dataObjectMap;
  }
  
  private HashMap<String, Integer> createQueryMap(HashMap<String, ArrayList<DataObject>> dataObjectMap, String activityId, String activityParentId, String dataObjectID ){
  // create SQL queries based on identified pattern
    HashMap<String, Integer> queryMap = new HashMap<String, Integer>();

    for (ArrayList<DataObject> dataObjectList : dataObjectMap.values()) {
      String q = new String();
      int r = 1;
      
      String scope = activityParentId.split(":")[0];
      
      String inputObjectName = dataObjectList.get(0).getName();
      String inputObjectPk = dataObjectList.get(0).getPkey();
      
      String caseObjName = BpmnParse.getScopeInformation().get(scope);
      String caseObjectPk = getCaseObjectPrimaryKey(scope, activityId);
      
      boolean isCaseObject = DataObjectClassification.isMainDataObject(dataObjectList.get(0), scope);
      boolean isDependentObject = !isCaseObject;
      boolean isDependentWithoutForeginKey = DataObjectClassification.isDependentDataObjectWithUnspecifiedFK(dataObjectList.get(0), scope);
      boolean isMiDependentObject = DataObjectClassification.isMIDependentDataObject(dataObjectList.get(0), scope);
      
      DataObjectSpecification caseObjectSpec = dataObject(caseObjName, caseObjectPk, dataObjectID);
      
      DataObjectSpecification inputObjectSpec = null;
      
      if (isCaseObject) {
        inputObjectSpec = caseObjectSpec;
      } else {
        inputObjectSpec = anyDataObject(inputObjectName, inputObjectPk);
        if (isDependentWithoutForeginKey) {
          inputObjectSpec.attribute(caseObjectPk, nullValue());
        } else {
          String foreignKey = dataObjectList.get(0).getFkeys().get(0);
          inputObjectSpec.references(foreignKey, caseObjectSpec);
        }
      }
      
      String[] states = extractStates(dataObjectList);
      inputObjectSpec.attribute("state", values(states));
      
      q = inputObjectSpec.getSelectCountStatement().toSqlString();
      
      if (isMiDependentObject && !isDependentWithoutForeginKey) {
        r = numberOfMultipleInstanceInTable(dataObjectList.get(0), dataObjectID, caseObjName);
      }
      
      queryMap.put(q, r);
    }
    return queryMap;    
  }

  private String getCaseObjectPrimaryKey(String scope, String activityId) {
    String caseObjPk = null;
    String caseObjName = BpmnParse.getScopeInformation().get(scope);
    for (DataObject caseObj : BpmnParse.getInputData().get(activityId)) {
      if (caseObj.getName().equalsIgnoreCase(caseObjName)) {
        caseObjPk = caseObj.getPkey();
        break;
      }
    }
    if (caseObjPk == null) {
      for (ArrayList<DataObject> temp : BpmnParse.getInputData().values()) {
        for (DataObject d : temp) {
          if (d.getName().equalsIgnoreCase(caseObjName)) {
            caseObjPk = d.getPkey();
            break;
          }
        }
        if (caseObjPk != null) {
          break;
        }
      }
    }
    return caseObjPk;
  }
  
  public boolean isInputDataAvailable(HashMap<String, Integer> queryMap) {
    boolean missingInputData = false;

    // check existence of all input data objects in the correct data states ..
    // if all satisfy the check, the activity can be executed; otherwise, the
    // check is restarted until all satisfy the check
    for (String query : queryMap.keySet()) {
      System.out.println("Running input check query: " + query);
      QueryExecutionHandler handler = QueryExecutionHandler.getInstance();
      handler.runQuery(query);
      if (Integer.parseInt(handler.getNextResult().get(0).toString()) < queryMap.get(query)) {
        missingInputData = true;
        System.out.println("waiting for " + query);
      }
    }
    
    return missingInputData == false;
  }

  private String[] extractStates(List<DataObject> dataObjects) {
    String[] states = new String[dataObjects.size()];
    
    for (int i = 0; i < dataObjects.size(); i++) {
      states[i] = dataObjects.get(i).getState();
    }
    return states;
  }

  // TODO: BPMN_SQL added
  private int numberOfMultipleInstanceInTable(DataObject dataObj, String instanceId, String caseObject) {
    int numberOfMI = 0;
    
    String dataObjectName = dataObj.getName();
    String foreignKey = dataObj.getFkeys().get(0);
    
    String query = anyDataObject(dataObjectName, dataObj.getPkey())
        .references(foreignKey, dataObject(caseObject, foreignKey, instanceId)).getSelectCountStatement().toSqlString();

    QueryExecutionHandler handler = QueryExecutionHandler.getInstance();
    handler.runQuery(query);
    numberOfMI = Integer.parseInt(handler.getNextResult().get(0).toString());

    return numberOfMI;
  }
}

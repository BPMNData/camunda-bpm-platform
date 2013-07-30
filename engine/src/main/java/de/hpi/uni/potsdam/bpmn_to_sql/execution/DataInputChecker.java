package de.hpi.uni.potsdam.bpmn_to_sql.execution;

import static de.hpi.uni.potsdam.bpmn_to_sql.pattern.DataObjectSpecification.anyDataObject;
import static de.hpi.uni.potsdam.bpmn_to_sql.pattern.DataObjectSpecification.dataObject;
import static de.hpi.uni.potsdam.bpmn_to_sql.pattern.PlainAttributeValueExpression.nullValue;
import static de.hpi.uni.potsdam.bpmn_to_sql.pattern.PlainAttributeValueExpression.values;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.camunda.bpm.engine.impl.bpmn.parser.BpmnParse;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;

import de.hpi.uni.potsdam.bpmn_to_sql.BpmnDataConfiguration;
import de.hpi.uni.potsdam.bpmn_to_sql.bpmn.DataObject;

public class DataInputChecker {

  protected BpmnDataConfiguration configuration;
  
  public DataInputChecker(BpmnDataConfiguration configuration) {
    this.configuration = configuration;
  }
  
  public boolean checkDataInput(ExecutionEntity execution) {
    if (!BpmnParse.getInputData().containsKey(execution.getActivity().getId())) {
      return true;
    }

    final String activityId = execution.getActivity().getId();
    final String instanceId = execution.getProcessInstanceId();
    final String activityParentId = execution.getActivity().getParent().getId();
    final String dataObjectID = execution.getEffectiveCaseObjectID();
    // final AtomicOperationActivityExecute taskBehavior = this;

    return isInputDataAvailable(activityId, activityParentId, dataObjectID, instanceId);
  }

  public boolean isInputDataAvailable(String activityId, String activityParentId, String dataObjectID, String instanceId) {
    boolean missingInputData = false;

    if (BpmnParse.getInputData().containsKey(activityId)) { // true if activity
                                                            // reads a data
                                                            // object
      HashMap<String, ArrayList<DataObject>> dataObjectMap = new HashMap<String, ArrayList<DataObject>>();
      // data object with same name are part of same list .. one list for each
      // different data object read by activity
      for (DataObject item : BpmnParse.getInputData().get(activityId)) {
        if (dataObjectMap.containsKey(item.getName())) {
          ArrayList<DataObject> al = dataObjectMap.get(item.getName());
          al.add(item);
          dataObjectMap.put(item.getName(), al);
        } else {
          ArrayList<DataObject> al = new ArrayList<DataObject>();
          al.add(item);
          dataObjectMap.put(item.getName(), al);
        }
      }

      // create SQL queries based on identified pattern
      HashMap<String, Integer> queryMap = new HashMap<String, Integer>();

      for (ArrayList<DataObject> dataObjectList : dataObjectMap.values()) {
        String q = new String();
        int r;
        // create SQL query with respect to type of data object (main,
        // dependent, dependent_MI, external_input)
        if (DataObjectClassification.isMainDataObject(dataObjectList.get(0), activityParentId.split(":")[0])) { // TODO: CR1; CR2
          q = createSqlQuery(dataObjectList, dataObjectID);
          r = 1;
        } else if (DataObjectClassification.isDependentDataObjectWithUnspecifiedFK(dataObjectList.get(0), activityParentId.split(":")[0])) { // TODO: D^1:1 R3; D^1:n R3; D^m:n R2; R4

          // get primary key of case object because of assumption that all
          // dependent DOs relate to main data object
          String caseObjPk = new String();
          String caseObjName = BpmnParse.getScopeInformation().get(activityParentId.split(":")[0]);
          for (DataObject caseObj : BpmnParse.getInputData().get(activityId)) {
            if (caseObj.getName().equalsIgnoreCase(caseObjName)) {
              caseObjPk = caseObj.getPkey();
              break;
            }
          }
          if (caseObjPk.isEmpty()) {
            for (ArrayList<DataObject> temp : BpmnParse.getInputData().values()) {
              for (DataObject d : temp) {
                if (d.getName().equalsIgnoreCase(caseObjName)) {
                  caseObjPk = d.getPkey();
                  break;
                }
              }
              if (!caseObjPk.isEmpty()) {
                break;
              }
            }
          }

          // provide case object of the scope to enable JOINALL; case object is
          // in a map called getScopeInformation which has as key the scope
          // (e.g., process, sub-process) name
          q = createSqlQuery(dataObjectList, dataObjectID, BpmnParse.getScopeInformation().get(activityParentId.split(":")[0]), caseObjPk,
              "dependent_WithoutFK");
          r = 1;
        } else if (DataObjectClassification.isDependentDataObject(dataObjectList.get(0), activityParentId.split(":")[0])) { // TODO: D^1:1 R1; R2
          // provide case object of the scope to enable JOINALL; case object is
          // in a map called getScopeInformation which has as key the scope
          // (e.g., process, sub-process) name
          q = createSqlQuery(dataObjectList, dataObjectID, BpmnParse.getScopeInformation().get(activityParentId.split(":")[0]));
          r = 1;
        } else if (DataObjectClassification.isMIDependentDataObject(dataObjectList.get(0), activityParentId.split(":")[0])) { // TODO: D^1:n R1; R2; D^m:n R1; R3
          q = createSqlQuery(dataObjectList, dataObjectID, BpmnParse.getScopeInformation().get(activityParentId.split(":")[0]));
          r = numberOfMultipleInstanceInTable(dataObjectList.get(0), dataObjectID, BpmnParse.getScopeInformation().get(activityParentId.split(":")[0])); // has
                                                                                                                                                         // to
                                                                                                                                                         // be
                                                                                                                                                         // defined
        } else {
          // non existent data object type identified
          q = null;
          r = 0;
        }

        queryMap.put(q, r);

      }

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
    }
    return missingInputData == false;
  }

  // TODO: BPMN_SQL added
  // main data object
  // CR1; CR2
  private String createSqlQuery(List<DataObject> dataObjectList, String instanceId) {
    // TODO our stuff
    String coName = dataObjectList.get(0).getName();
    String pk = dataObjectList.get(0).getPkey();
    
    String[] states = extractStates(dataObjectList);
    
    String query = dataObject(coName, pk, instanceId).attribute("state", values(states)).getSelectCountStatement().toSqlString();

    return query;
  }
  
  private String[] extractStates(List<DataObject> dataObjects) {
    String[] states = new String[dataObjects.size()];
    
    for (int i = 0; i < dataObjects.size(); i++) {
      states[i] = dataObjects.get(i).getState();
    }
    return states;
  }

  // TODO: BPMN_SQL added
  // dependent data object
  // D^1:1 R1; R2; D^1:n R1; R2; D^m:n R1; R3
  private String createSqlQuery(List<DataObject> dataObjectList, String instanceId, String caseObject) {
    // TODO our stuff
    String[] states = extractStates(dataObjectList);
    String dataObjectName = dataObjectList.get(0).getName();
    String foreignKey = dataObjectList.get(0).getFkeys().get(0);
    
    String query = anyDataObject(dataObjectName, dataObjectList.get(0).getPkey()).attribute("state", values(states))
        .references(foreignKey, dataObject(caseObject, foreignKey, instanceId)).getSelectCountStatement().toSqlString();

    return query;
  }

  // TODO: BPMN_SQL added
  // dependent data object with null foreign key
  // D^1:1 R3; D^1:n R3; D^m:n R2; R4
  private String createSqlQuery(List<DataObject> dataObjectList, String instanceId, String caseObject, String caseObjectPk, String type) {
    // TODO our stuff
    
    String[] states = extractStates(dataObjectList);
    String dataObjectName = dataObjectList.get(0).getName();
    
    String query = anyDataObject(dataObjectName, dataObjectList.get(0).getPkey()).attribute("state", values(states))
       .attribute(caseObjectPk, nullValue()).getSelectCountStatement().toSqlString();

    return query;
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

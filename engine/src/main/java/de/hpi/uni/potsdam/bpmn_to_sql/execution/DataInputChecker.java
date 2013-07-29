package de.hpi.uni.potsdam.bpmn_to_sql.execution;

import java.util.ArrayList;
import java.util.HashMap;
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
          q = createSqlQuery(dataObjectList, dataObjectID, BpmnParse.getScopeInformation().get(activityParentId.split(":")[0]), "dependent");
          r = 1;
        } else if (DataObjectClassification.isMIDependentDataObject(dataObjectList.get(0), activityParentId.split(":")[0])) { // TODO: D^1:n R1; R2; D^m:n R1; R3
          q = createSqlQuery(dataObjectList, dataObjectID, BpmnParse.getScopeInformation().get(activityParentId.split(":")[0]), "dependent_MI");
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
  private String createSqlQuery(ArrayList<DataObject> dataObjectList, String instanceId) {
    // TODO our stuff
    String query;
    String state = new String();

    for (DataObject dataObject : dataObjectList) {
      if (state.isEmpty()) {
        state = "\"" + dataObject.getState() + "\"";
      } else {
        state = state + (" OR " + "\"" + dataObject.getState() + "\"");
      }
    }

    query = "SELECT COUNT(`" + dataObjectList.get(0).getPkey() + "`) FROM `" + dataObjectList.get(0).getName() + "` WHERE `" + dataObjectList.get(0).getPkey()
        + "` =\"" + instanceId + "\" and `state` =(" + state + ")";

    // if (type == "main") {
    // query = "SELECT COUNT(`" + dataObjectList.get(0).getPkey() + "`) FROM `"
    // + dataObjectList.get(0).getName() + "` WHERE `" +
    // dataObjectList.get(0).getPkey() + "` =" + instanceId + " and `state` =("
    // + state + ")";
    // } else if(type == "dependent") {
    // //SELECT P.pid FROM `Product` P INNER JOIN `Order` O USING (oid) WHERE
    // O.oid = 4
    // query = "SELECT COUNT(`" + dataObjectList.get(0).getPkey() + "`) FROM `"
    // + dataObjectList.get(0).getName() + "` WHERE `" +
    // dataObjectList.get(0).getFkeys().get(0) + "` =" + instanceId +
    // " and `state` =(" + state + ")";
    // } else {
    // query = "SELECT `state` FROM `" + dataObjectList.get(0).getName() +
    // "` WHERE `" + dataObjectList.get(0).getPkey() + "` =" + instanceId;
    // }

    return query;
  }

  // TODO: BPMN_SQL added
  // dependent data object
  // D^1:1 R1; R2; D^1:n R1; R2; D^m:n R1; R3
  private String createSqlQuery(ArrayList<DataObject> dataObjectList, String instanceId, String caseObject, String type) {
    // TODO our stuff
    String query;
    String state = new String();

    for (DataObject dataObject : dataObjectList) {
      if (state.isEmpty()) {
        state = "\"" + dataObject.getState() + "\"";
      } else {
        state = state + (" OR " + "\"" + dataObject.getState() + "\"");
      }
    }

    if (type == "dependent" || type == "dependent_MI") {
      // "SELECT COUNT(`" + dataObjectList.get(0).getPkey() + "`) FROM `" +
      // dataObjectList.get(0).getName() + "` WHERE `" +
      // dataObjectList.get(0).getFkeys().get(0) + "` =" + instanceId +
      // " and `state` =(" + state + ")";
      // SELECT P.pid FROM `Product` P INNER JOIN `Order` O USING (oid) WHERE
      // O.oid = 4
      query = "SELECT COUNT(D." + dataObjectList.get(0).getFkeys().get(0) + ") FROM `" + dataObjectList.get(0).getName() + "` D INNER JOIN `" + caseObject
          + "` M USING (" + dataObjectList.get(0).getFkeys().get(0) + ") WHERE M." + dataObjectList.get(0).getFkeys().get(0) + "= \"" + instanceId
          + "\" and D.state =(" + state + ")";
      // does not work anymore as soon as several foreign keys are allowed,
      // i.e., when we extend the data object chain to more than 2 in length
    /*} else if (type == "dependent_MI") {
      query = "SELECT COUNT(D." + dataObjectList.get(0).getFkeys().get(0) + ") FROM `" + dataObjectList.get(0).getName() + "` D INNER JOIN `" + caseObject
          + "` M USING (" + dataObjectList.get(0).getFkeys().get(0) + ") WHERE M." + dataObjectList.get(0).getFkeys().get(0) + "= \"" + instanceId
          + "\" and D.state =(" + state + ")";*/
    } else { // wrong type
      query = null;
    }

    return query;
  }

  // TODO: BPMN_SQL added
  // dependent data object with null foreign key
  // D^1:1 R3; D^1:n R3; D^m:n R2; R4
  private String createSqlQuery(ArrayList<DataObject> dataObjectList, String instanceId, String caseObject, String caseObjectPk, String type) {
    // TODO our stuff
    String query;
    String state = new String();

    for (DataObject dataObject : dataObjectList) {
      if (state.isEmpty()) {
        state = "\"" + dataObject.getState() + "\"";
      } else {
        state = state + (" OR " + "\"" + dataObject.getState() + "\"");
      }
    }

    query = "SELECT COUNT(" + dataObjectList.get(0).getPkey() + ") FROM `" + dataObjectList.get(0).getName() + "` WHERE `" + caseObjectPk + "` IS NULL "
        + " and `state`= (" + state + ")";

    return query;
  }

  // TODO: BPMN_SQL added
  private int numberOfMultipleInstanceInTable(DataObject dataObj, String instanceId, String caseObject) {
    int numberOfMI = 0;
    String query;

    query = "SELECT COUNT(D." + dataObj.getFkeys().get(0) + ") FROM `" + dataObj.getName() + "` D INNER JOIN `" + caseObject + "` M USING ("
        + dataObj.getFkeys().get(0) + ") WHERE M." + dataObj.getFkeys().get(0) + "=\"" + instanceId + "\"";
    QueryExecutionHandler handler = QueryExecutionHandler.getInstance();
    handler.runQuery(query);
    numberOfMI = Integer.parseInt(handler.getNextResult().get(0).toString());

    return numberOfMI;
  }
}

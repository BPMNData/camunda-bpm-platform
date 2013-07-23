package de.hpi.uni.potsdam.bpmn_to_sql.execution;

import java.util.ArrayList;
import java.util.UUID;
import org.camunda.bpm.engine.impl.bpmn.parser.BpmnParse;
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution;

import de.hpi.uni.potsdam.bpmn_to_sql.BpmnDataConfiguration;
import de.hpi.uni.potsdam.bpmn_to_sql.bpmn.DataObject;

public class DataOutputHandler {
  
  protected BpmnDataConfiguration configuration;
  
  public DataOutputHandler(BpmnDataConfiguration configuration) {
    this.configuration = configuration;
  }
  
  public void updateOutputs(ActivityExecution execution) {
    // get id of scope or process depending whether activity is part of a scope
    // (i.e., subprocess) or the process itself
    // if(execution.getParentId()==null) {
    // ScopeInstanceID = execution.getProcessInstanceId();
    // }else if (execution.isScope()){
    // ScopeInstanceID = execution.getId();
    // } else{
    // ScopeInstanceID = execution.getParentId();
    // }
    //
    String dataObjectID = execution.getEffectiveCaseObjectID();

    if (BpmnParse.getOutputData().containsKey(execution.getActivity().getId())) { // true
                                                                                  // if
                                                                                  // activity
                                                                                  // writes
                                                                                  // a
                                                                                  // data
                                                                                  // object
      // ArrayList<DataObject> test =
      // BpmnParse.getOutputData().get(execution.getActivity().getId());
      for (DataObject item : BpmnParse.getOutputData().get(execution.getActivity().getId())) {
        String query = new String();

        ArrayList<String> stateList = new ArrayList<String>();
        // get state of the input object when it exists, that it can be used for
        // the UPDATE-statements
        // String a = execution.getActivity().getId();
        // Map<String, ArrayList<DataObject>> b = BpmnParse.getInputData();
        // Set<String> c = b.keySet();
        // if(BpmnParse.getInputData().keySet().contains(execution.getActivity().getId()))
        // {
        // String d = "abc";
        // }

        String item_state = item.getState();

        // check whether the state of the object is a process variable
        if (item_state.startsWith("$")) {
          item_state = (String) execution.getVariableLocal(item.getState().substring(1));
        }

        for (DataObject dataObj : getMatchingInputDataObject(item, execution.getActivity().getId())) {
          stateList.add(dataObj.getState());
        }

        // create SQL query with respect to type of data object (main,
        // dependent, dependent_MI, dependent_WithoutFK)
        if (DataObjectClassification.isMainDataObject(item, execution.getActivity().getParent().getId().split(":")[0])) {
          if (!getMatchingInputDataObject(item, execution.getActivity().getId()).isEmpty()) {
            // if(!stateList.isEmpty()) {
            // input data object exists
            query = createSqlQuery(item, item_state, stateList, dataObjectID);
          } else {
            // no input data object
            query = createSqlQuery(item, item_state, dataObjectID);
          }
        } else {
          // DataObject matchingInputDataObj = new DataObject();
          // matchingInputDataObj = null;
          // for (DataObject inputDo :
          // BpmnParse.getInputData().get(execution.getActivity().getId())){
          // if (inputDo.getName().equalsIgnoreCase(item.getName())){
          // matchingInputDataObj = inputDo;
          // }
          // }
          String expression = null;
          if (!getMatchingInputDataObject(item, execution.getActivity().getId()).isEmpty()) {
            // if(!stateList.isEmpty()) {
            // TODO: make available for OR statement
            if (DataObjectClassification.isDependentDataObjectWithUnspecifiedFK(getMatchingInputDataObject(item, execution.getActivity().getId()).get(0),
                execution.getActivity().getParent().getId().split(":")[0])) {
              if (item.getProcessVariable() != null) {
                expression = (String) execution.getVariable(item.getProcessVariable());
              }
              // Update-Query for data objects where the fk was not yet
              // specified in the data base, e.g. when the object was received
              // by another organization
              query = createSqlQuery(item, item_state, dataObjectID,
                  BpmnParse.getScopeInformation().get(execution.getActivity().getParent().getId().split(":")[0]), stateList, expression, "dependent_WithoutFK");
            }
          }
          if (query.isEmpty()) {
            if (DataObjectClassification.isDependentDataObject(item, execution.getActivity().getParent().getId().split(":")[0])) {
              // provide case object of the scope to enable JOINALL; case object
              // is in a map called getScopeInformation which has as key the
              // scope (e.g., process, sub-process) name
              if (!getMatchingInputDataObject(item, execution.getActivity().getId()).isEmpty()) {
                // if(!stateList.isEmpty()) {
                // input data object exists
                query = createSqlQuery(item, item_state, dataObjectID,
                    BpmnParse.getScopeInformation().get(execution.getActivity().getParent().getId().split(":")[0]), stateList, expression, "dependent");
              } else {
                // no input data object
                query = createSqlQuery(item, item_state, dataObjectID,
                    BpmnParse.getScopeInformation().get(execution.getActivity().getParent().getId().split(":")[0]), "dependent");
              }
            } else if (DataObjectClassification.isMIDependentDataObject(item, execution.getActivity().getParent().getId().split(":")[0])) {
              // provide case object of the scope to enable JOINALL; case object
              // is in a map called getScopeInformation which has as key the
              // scope (e.g., process, sub-process) name
              int numberOfItems = -1;
              if (item.getPkType().equals("new")) {
                numberOfItems = Integer.parseInt((String) execution.getVariable(item.getProcessVariable()));
              }

              if (!getMatchingInputDataObject(item, execution.getActivity().getId()).isEmpty()) {
                // if(!stateList.isEmpty()) {
                // input data object exists
                query = createSqlQuery(item, item_state, dataObjectID,
                    BpmnParse.getScopeInformation().get(execution.getActivity().getParent().getId().split(":")[0]), stateList, "dependent_MI", numberOfItems);
              } else {
                // no input data object
                query = createSqlQuery(item, item_state, dataObjectID,
                    BpmnParse.getScopeInformation().get(execution.getActivity().getParent().getId().split(":")[0]), "dependent_MI", numberOfItems);
              }
            } else {
              System.out.println("Output data object does not match the requirements for firing SQL-UPDATE Statement");
            }
          }
        }
        QueryExecutionHandler queryHandler = QueryExecutionHandler.getInstance();
        queryHandler.runUpdate(query);
      }
    }
    // TODO: BPMN_SQL end
  }

  // TODO: BPMN_SQL added
  private ArrayList<DataObject> getMatchingInputDataObject(DataObject item, String id) {
    ArrayList<DataObject> dataObjects = new ArrayList<DataObject>();
    if (BpmnParse.getInputData().get(id) != null) {
      for (DataObject inputDo : BpmnParse.getInputData().get(id)) {
        if (inputDo.getName().equalsIgnoreCase(item.getName())) {
          dataObjects.add(inputDo);
        }
      }
    }

    return dataObjects;
  }

  // TODO: BPMN_SQL added
  // Creation of SQL-Queries(insert, update, delete) only for main data object
  private String createSqlQuery(DataObject dataObj, String dataObjState, String scopeInstanceId) {
    String query = "";

    if (dataObj.getPkType().equals("new")) {
      query = "INSERT INTO `" + dataObj.getName() + "`(`" + dataObj.getPkey() + "`, `state`) VALUES (" + scopeInstanceId + ",\"" + dataObjState + "\")";
    } else if (dataObj.getPkType().equals("delete")) {
      query = "DELETE FROM `" + dataObj.getName() + "` WHERE " + dataObj.getPkey() + "=\"" + scopeInstanceId + "\"";
    } else {
      query = "UPDATE `" + dataObj.getName() + "` SET `state`=\"" + dataObjState + "\" WHERE `" + dataObj.getPkey() + "`=\"" + scopeInstanceId + "\"";
    }

    return query;
  }

  // TODO: BPMN_SQL added
  // Creation of SQL-Queries(insert, update, delete) only for main data object
  private String createSqlQuery(DataObject dataObj, String dataObjState, ArrayList<String> stateList, String scopeInstanceId) {
    String query = "";
    String state = new String();

    for (String s : stateList) {
      if (state.isEmpty()) {
        state = "\"" + s + "\"";
      } else {
        state = state + (" OR " + "\"" + s + "\"");
      }
    }

    if (dataObj.getPkType().equals("new")) {
      query = "INSERT INTO `" + dataObj.getName() + "`(`" + dataObj.getPkey() + "`, `state`) VALUES (" + scopeInstanceId + ",\"" + dataObjState + "\")";
    } else if (dataObj.getPkType().equals("delete")) {
      query = "DELETE FROM `" + dataObj.getName() + "` WHERE " + dataObj.getPkey() + "=\"" + scopeInstanceId + "\"";
    } else {
      query = "UPDATE IGNORE `" + dataObj.getName() + "` SET `state`=\"" + dataObjState + "\" WHERE `" + dataObj.getPkey() + "`=\"" + scopeInstanceId
          + "\" and `state` = (" + state + ")";
    }

    return query;
  }

  // TODO: BPMN_SQL added
  private String createSqlQuery(DataObject dataObj, String dataObjState, String scopeInstanceId, String caseObject, String type) {
    String query = "";
    QueryExecutionHandler queryHandler = QueryExecutionHandler.getInstance();
    UUID uuid = UUID.randomUUID(); // primary key for dependent data objects

    if (type == "dependent") {
      if (dataObj.getPkType().equals("new")) {
        query = "INSERT INTO `" + dataObj.getName() + "`(`" + dataObj.getPkey() + "`, `" + dataObj.getFkeys().get(0) + "`, `state`) VALUES (\"" + uuid + "\","
            + "(SELECT `" + dataObj.getFkeys().get(0) + "` FROM `" + caseObject + "` WHERE `" + dataObj.getFkeys().get(0) + "`= \"" + scopeInstanceId
            + "\"),\"" + dataObjState + "\")";

      } else if (dataObj.getPkType().equals("delete")) {
        // join in from statement not allowed
        String q = "SELECT D." + dataObj.getPkey() + " FROM `" + dataObj.getName() + "` D INNER JOIN `" + caseObject + "` M USING ("
            + dataObj.getFkeys().get(0) + ")";
        queryHandler.runQuery(q);
        String pkey = queryHandler.getNextResult().get(0).toString();
        query = "DELETE FROM `" + dataObj.getName() + "` WHERE `" + dataObj.getPkey() + "`= \"" + pkey + "\" AND `state` = \"" + dataObjState + "\""; // has
                                                                                                                                                      // to
                                                                                                                                                      // be
                                                                                                                                                      // checked
      } else {
        // join in from statement not allowed
        String q = "SELECT D." + dataObj.getPkey() + " FROM `" + dataObj.getName() + "` D INNER JOIN `" + caseObject + "` M USING ("
            + dataObj.getFkeys().get(0) + ") WHERE M." + dataObj.getFkeys().get(0) + "=\"" + scopeInstanceId + "\"";
        queryHandler.runQuery(q);
        String pkey = queryHandler.getNextResult().get(0).toString();
        query = "UPDATE `" + dataObj.getName() + "` SET `state`=\"" + dataObjState + "\" WHERE `" + dataObj.getPkey() + "`= \"" + pkey + "\"";
      }
    } else {
      System.out.println("wrong type");
    }
    return query;
  }

  // TODO: BPMN_SQL added
  private String createSqlQuery(DataObject dataObj, String dataObjState, String scopeInstanceId, String caseObject, ArrayList<String> stateList,
      String expression, String type) {
    String query = "";
    QueryExecutionHandler queryHandler = QueryExecutionHandler.getInstance();
    UUID uuid = UUID.randomUUID(); // primary key for dependent data objects
    String state = new String();

    for (String s : stateList) {
      if (state.isEmpty()) {
        state = "\"" + s + "\"";
      } else {
        state = state + (" OR " + "\"" + s + "\"");
      }
    }

    if (type == "dependent") {
      if (dataObj.getPkType().equals("new")) {
        query = "INSERT INTO `" + dataObj.getName() + "`(`" + dataObj.getPkey() + "`, `" + dataObj.getFkeys().get(0) + "`, `state`) VALUES (\"" + uuid + "\","
            + "(SELECT `" + dataObj.getFkeys().get(0) + "` FROM `" + caseObject + "` WHERE `" + dataObj.getFkeys().get(0) + "`= \"" + scopeInstanceId
            + "\"),\"" + dataObjState + "\")";

      } else if (dataObj.getPkType().equals("delete")) {
        // join in from statement not allowed
        String q = "SELECT D." + dataObj.getPkey() + " FROM `" + dataObj.getName() + "` D INNER JOIN `" + caseObject + "` M USING ("
            + dataObj.getFkeys().get(0) + ")";
        queryHandler.runQuery(q);
        String pkey = queryHandler.getNextResult().get(0).toString();
        query = "DELETE FROM `" + dataObj.getName() + "` WHERE `" + dataObj.getPkey() + "`= \"" + pkey + "\" AND `state` = \"" + dataObjState + "\""; // has
                                                                                                                                                      // to
                                                                                                                                                      // be
                                                                                                                                                      // checked
      } else {
        // join in from statement not allowed
        String q = "SELECT D." + dataObj.getPkey() + " FROM `" + dataObj.getName() + "` D INNER JOIN `" + caseObject + "` M USING ("
            + dataObj.getFkeys().get(0) + ") WHERE M." + dataObj.getFkeys().get(0) + "=\"" + scopeInstanceId + "\"";
        queryHandler.runQuery(q);
        String pkey = queryHandler.getNextResult().get(0).toString();
        query = "UPDATE IGNORE `" + dataObj.getName() + "` SET `state`=\"" + dataObjState + "\" WHERE `" + dataObj.getPkey() + "`= \"" + pkey
            + "\" and `state` = (" + state + ")";
      }
    } else if (type == "dependent_WithoutFK") {
      // TODO: has to be checked
      if (expression != null) {
        query = "UPDATE IGNORE `" + dataObj.getName() + "` SET `" + dataObj.getFkeys().get(0) + "` =" + "(SELECT `" + dataObj.getFkeys().get(0) + "` FROM `"
            + caseObject + "` WHERE `" + dataObj.getFkeys().get(0) + "`= \"" + scopeInstanceId + "\"), `state`=\"" + dataObjState + "\" WHERE `"
            + dataObj.getFkeys().get(0) + "` IS NULL and `state` = (" + state + ") AND " + expression;
      } else {
        query = "UPDATE IGNORE `" + dataObj.getName() + "` SET `" + dataObj.getFkeys().get(0) + "` =" + "(SELECT `" + dataObj.getFkeys().get(0) + "` FROM `"
            + caseObject + "` WHERE `" + dataObj.getFkeys().get(0) + "`= \"" + scopeInstanceId + "\"), `state`=\"" + dataObjState + "\" WHERE `"
            + dataObj.getFkeys().get(0) + "` IS NULL and `state` = (" + state + ")";
      }

      // SELECT COUNT( `rid` ) FROM `Receipt` WHERE `oid` IS NULL AND state =
      // "approved"
    } else {
      System.out.println("wrong type");
    }
    return query;
  }

  // TODO: BPMN_SQL added
  private String createSqlQuery(DataObject dataObj, String dataObjState, String scopeInstanceId, String caseObject, String type, int count) {
    String query = "";
    UUID uuid = UUID.randomUUID(); // primary key for dependent data objects

    if (type == "dependent_MI") {
      if (dataObj.getPkType().equals("new")) {
        query = "INSERT INTO `" + dataObj.getName() + "`(`" + dataObj.getPkey() + "`, `" + dataObj.getFkeys().get(0) + "`, `state`) VALUES ";
        for (int i = 1; i < count; i++) {
          query = query + "(\"" + uuid + "\"," + "(SELECT `" + dataObj.getFkeys().get(0) + "` FROM `" + caseObject + "` WHERE `" + dataObj.getFkeys().get(0)
              + "`= \"" + scopeInstanceId + "\"),\"" + dataObjState + "\"),";
          uuid = UUID.randomUUID(); // set new UUID for next collection data
                                    // item
        }
        query = query + "(\"" + uuid + "\"," + "(SELECT `" + dataObj.getFkeys().get(0) + "` FROM `" + caseObject + "` WHERE `" + dataObj.getFkeys().get(0)
            + "`= " + scopeInstanceId + "),\"" + dataObjState + "\")";

      } else if (dataObj.getPkType().equals("delete")) {
        query = "DELETE FROM `" + dataObj.getName() + "` WHERE `" + dataObj.getFkeys().get(0) + "` =" + "(SELECT `" + dataObj.getFkeys().get(0) + "` FROM `"
            + caseObject + "` WHERE `" + dataObj.getFkeys().get(0) + "`= " + scopeInstanceId + ") AND `state` = \"" + dataObjState + "\"";
      } else {
        query = "UPDATE `" + dataObj.getName() + "` SET `state`=\"" + dataObjState + "\" WHERE `" + dataObj.getFkeys().get(0) + "` =" + "(SELECT `"
            + dataObj.getFkeys().get(0) + "` FROM `" + caseObject + "` WHERE `" + dataObj.getFkeys().get(0) + "`= \"" + scopeInstanceId + "\")";
      }
    } else {
      System.out.println("wrong type");
    }
    return query;
  }

  // TODO: BPMN_SQL added
  private String createSqlQuery(DataObject dataObj, String dataObjState, String scopeInstanceId, String caseObject, ArrayList<String> stateList, String type,
      int count) {
    String query = "";
    UUID uuid = UUID.randomUUID(); // primary key for dependent data objects
    String state = new String();

    for (String s : stateList) {
      if (state.isEmpty()) {
        state = "\"" + s + "\"";
      } else {
        state = state + (" OR " + "\"" + s + "\"");
      }
    }

    if (type == "dependent_MI") {
      if (dataObj.getPkType().equals("new")) {
        query = "INSERT INTO `" + dataObj.getName() + "`(`" + dataObj.getPkey() + "`, `" + dataObj.getFkeys().get(0) + "`, `state`) VALUES ";
        for (int i = 1; i < count; i++) {
          query = query + "(\"" + uuid + "\"," + "(SELECT `" + dataObj.getFkeys().get(0) + "` FROM `" + caseObject + "` WHERE `" + dataObj.getFkeys().get(0)
              + "`= \"" + scopeInstanceId + "\"),\"" + dataObjState + "\"),";
          uuid = UUID.randomUUID(); // set new UUID for next collection data
                                    // item
        }
        query = query + "(\"" + uuid + "\"," + "(SELECT `" + dataObj.getFkeys().get(0) + "` FROM `" + caseObject + "` WHERE `" + dataObj.getFkeys().get(0)
            + "`= " + scopeInstanceId + "),\"" + dataObjState + "\")";

      } else if (dataObj.getPkType().equals("delete")) {
        query = "DELETE FROM `" + dataObj.getName() + "` WHERE `" + dataObj.getFkeys().get(0) + "` =" + "(SELECT `" + dataObj.getFkeys().get(0) + "` FROM `"
            + caseObject + "` WHERE `" + dataObj.getFkeys().get(0) + "`= " + scopeInstanceId + ") AND `state` = \"" + dataObjState + "\"";
      } else {
        query = "UPDATE IGNORE `" + dataObj.getName() + "` SET `state`=\"" + dataObjState + "\" WHERE `" + dataObj.getFkeys().get(0) + "` =" + "(SELECT `"
            + dataObj.getFkeys().get(0) + "` FROM `" + caseObject + "` WHERE `" + dataObj.getFkeys().get(0) + "`= \"" + scopeInstanceId + "\") AND `state` = ("
            + state + ")";
      }
    } else {
      System.out.println("wrong type");
    }
    return query;
  }
}

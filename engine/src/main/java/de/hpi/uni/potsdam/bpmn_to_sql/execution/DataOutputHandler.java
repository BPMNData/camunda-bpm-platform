package de.hpi.uni.potsdam.bpmn_to_sql.execution;

import static de.hpi.uni.potsdam.bpmn_to_sql.pattern.DataObjectSpecification.anyDataObject;
import static de.hpi.uni.potsdam.bpmn_to_sql.pattern.DataObjectSpecification.dataObject;
import static de.hpi.uni.potsdam.bpmn_to_sql.pattern.InsertObjectSpecification.insert;
import static de.hpi.uni.potsdam.bpmn_to_sql.pattern.PlainAttributeValueExpression.nullValue;
import static de.hpi.uni.potsdam.bpmn_to_sql.pattern.PlainAttributeValueExpression.values;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.camunda.bpm.engine.impl.bpmn.parser.BpmnParse;
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution;

import de.hpi.uni.potsdam.bpmn_to_sql.BpmnDataConfiguration;
import de.hpi.uni.potsdam.bpmn_to_sql.bpmn.DataObject;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.AttributeUpdate;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.DataObjectReference;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.DataObjectSpecification;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.InsertObjectSpecification;

public class DataOutputHandler {
  
  protected BpmnDataConfiguration configuration;
  
  // TODO remove config
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
          if (!getMatchingInputDataObject(item, execution.getActivity().getId()).isEmpty()) { // TODO: CU2
            // if(!stateList.isEmpty()) {
            // input data object exists
            query = createSqlQuery(item, item_state, stateList, dataObjectID);
          } else { // TODO: CC1; CC2; CU1; CD1
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
                execution.getActivity().getParent().getId().split(":")[0])) { // TODO: D^1:1 U3; D^1:n U3; D^m:n U4
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
              if (!getMatchingInputDataObject(item, execution.getActivity().getId()).isEmpty()) { // TODO: D^1:1 U2
                // if(!stateList.isEmpty()) {
                // input data object exists
                query = createSqlQuery(item, item_state, dataObjectID,
                    BpmnParse.getScopeInformation().get(execution.getActivity().getParent().getId().split(":")[0]), stateList, expression, "dependent");
              } else { // TODO: D^1:1 C1; C2; U1; D1
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

              if (!getMatchingInputDataObject(item, execution.getActivity().getId()).isEmpty()) { // TODO: D^1:n U2; D^m:n U3
                // if(!stateList.isEmpty()) {
                // input data object exists
                query = createSqlQuery(item, item_state, dataObjectID,
                    BpmnParse.getScopeInformation().get(execution.getActivity().getParent().getId().split(":")[0]), stateList, "dependent_MI", numberOfItems);
              } else { // TODO: D^1:n C1; C2; U1; D1; D^m:n C1; C2; U1; U2; D1; D2
                // no input data object
                query = createSqlQuery(item, item_state, dataObjectID,
                    BpmnParse.getScopeInformation().get(execution.getActivity().getParent().getId().split(":")[0]), "dependent_MI", numberOfItems);
              }
            } else {
              System.out.println("Output data object does not match the requirements for firing SQL-UPDATE Statement");
            }
          }
        }
//        QueryExecutionHandler queryHandler = QueryExecutionHandler.getInstance();
//        queryHandler.runUpdate(query);
        System.out.println(query);
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
  // CC1; CC2; CU1; CD1
  private String createSqlQuery(DataObject dataObj, String dataObjState, String scopeInstanceId) {
    String query = "";

    if (dataObj.getPkType().equals("new")) {
      // TODO dead branch according to tests
      query = "INSERT INTO `" + dataObj.getName() + "`(`" + dataObj.getPkey() + "`, `state`) VALUES (" + scopeInstanceId + ",\"" + dataObjState + "\")";
      throw new RuntimeException("Not tested");
    } else if (dataObj.getPkType().equals("delete")) {
      query = "DELETE FROM `" + dataObj.getName() + "` WHERE " + dataObj.getPkey() + "=\"" + scopeInstanceId + "\"";
      
      throw new RuntimeException("Not tested");
    } else {
      query = "UPDATE `" + dataObj.getName() + "` SET `state`=\"" + dataObjState + "\" WHERE `" + dataObj.getPkey() + "`=\"" + scopeInstanceId + "\"";
      throw new RuntimeException("Not tested");
    }

//    return query;
  }

  // TODO: BPMN_SQL added
  // Creation of SQL-Queries(insert, update, delete) only for main data object
  // CU2
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
      // TODO dead branch according to tests
      query = "INSERT INTO `" + dataObj.getName() + "`(`" + dataObj.getPkey() + "`, `state`) VALUES (" + scopeInstanceId + ",\"" + dataObjState + "\")";
      throw new RuntimeException("Not tested");
      
    } else if (dataObj.getPkType().equals("delete")) {
      query = "DELETE FROM `" + dataObj.getName() + "` WHERE " + dataObj.getPkey() + "=\"" + scopeInstanceId + "\"";
    } else {
      List<AttributeUpdate> updates = new ArrayList<AttributeUpdate>();
      AttributeUpdate update = new AttributeUpdate("state", dataObjState);
      updates.add(update);
      String[] validStates = stateList.toArray(new String[0]);
      query = dataObject(dataObj.getName(), dataObj.getPkey(), scopeInstanceId).attribute("state", values(validStates)).getUpdateStatement(updates).toSqlString();
      
//      String query2 = "UPDATE IGNORE `" + dataObj.getName() + "` SET `state`=\"" + dataObjState + "\" WHERE `" + dataObj.getPkey() + "`=\"" + scopeInstanceId
//          + "\" and `state` = (" + state + ")";
    }

    return query;
  }

  // TODO: BPMN_SQL added
  // D^1:1 C1; C2; U1; D1
  private String createSqlQuery(DataObject dataObj, String dataObjState, String scopeInstanceId, String caseObject, String type) {
    String query = "";
    QueryExecutionHandler queryHandler = QueryExecutionHandler.getInstance();
    UUID uuid = UUID.randomUUID(); // primary key for dependent data objects
    
    // TODO assumption is here that the foreign key equals the CO's primary key
    DataObjectSpecification caseObjectSpec = dataObject(caseObject, dataObj.getFkeys().get(0), scopeInstanceId);

    if ("dependent".equals(type)) {
      if (dataObj.getPkType().equals("new")) {
        query = insert().object(
            dataObject(dataObj.getName(), dataObj.getPkey(), uuid.toString())
              .attribute("state", dataObjState)
              .references(dataObj.getFkeys().get(0), caseObjectSpec))
            .getStatement().toSqlString();
        
//        String query2 = "INSERT INTO `" + dataObj.getName() + "`(`" + dataObj.getPkey() + "`, `" + dataObj.getFkeys().get(0) + "`, `state`) VALUES (\"" + uuid + "\","
//            + "(SELECT `" + dataObj.getFkeys().get(0) + "` FROM `" + caseObject + "` WHERE `" + dataObj.getFkeys().get(0) + "`= \"" + scopeInstanceId
//            + "\"),\"" + dataObjState + "\")";
      } else if (dataObj.getPkType().equals("delete")) {
        
        // join in from statement not allowed
        String q = "SELECT D." + dataObj.getPkey() + " FROM `" + dataObj.getName() + "` D INNER JOIN `" + caseObject + "` M USING ("
            + dataObj.getFkeys().get(0) + ")";
        queryHandler.runQuery(q);
        String pkey = queryHandler.getNextResult().get(0).toString();
        query = "DELETE FROM `" + dataObj.getName() + "` WHERE `" + dataObj.getPkey() + "`= \"" + pkey + "\" AND `state` = \"" + dataObjState + "\""; // has
                                                                                                                                                      // to
        throw new RuntimeException("not tested");                                                                                                                                              // be
                                                                                                                                                      // checked
      } else {
        // join in from statement not allowed
        String q = "SELECT D." + dataObj.getPkey() + " FROM `" + dataObj.getName() + "` D INNER JOIN `" + caseObject + "` M USING ("
            + dataObj.getFkeys().get(0) + ") WHERE M." + dataObj.getFkeys().get(0) + "=\"" + scopeInstanceId + "\"";
        queryHandler.runQuery(q);
        String pkey = queryHandler.getNextResult().get(0).toString();
        query = "UPDATE `" + dataObj.getName() + "` SET `state`=\"" + dataObjState + "\" WHERE `" + dataObj.getPkey() + "`= \"" + pkey + "\"";
      
        throw new RuntimeException("not tested");  
      }
    } else {
      System.out.println("wrong type");
    }
    return query;
  }

  // TODO: BPMN_SQL added
  // D^1:1 U3; D^1:n U3; D^m:n U4; D^1:1 U2
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

    if (type == "dependent") { // D^1:1 U2
      if (dataObj.getPkType().equals("new")) {
        // TODO dead branch according to tests
        query = "INSERT INTO `" + dataObj.getName() + "`(`" + dataObj.getPkey() + "`, `" + dataObj.getFkeys().get(0) + "`, `state`) VALUES (\"" + uuid + "\","
            + "(SELECT `" + dataObj.getFkeys().get(0) + "` FROM `" + caseObject + "` WHERE `" + dataObj.getFkeys().get(0) + "`= \"" + scopeInstanceId
            + "\"),\"" + dataObjState + "\")";

        throw new RuntimeException("not tested");  
        
      } else if (dataObj.getPkType().equals("delete")) {
        // join in from statement not allowed
        String q = "SELECT D." + dataObj.getPkey() + " FROM `" + dataObj.getName() + "` D INNER JOIN `" + caseObject + "` M USING ("
            + dataObj.getFkeys().get(0) + ")";
        queryHandler.runQuery(q);
        String pkey = queryHandler.getNextResult().get(0).toString();
        query = "DELETE FROM `" + dataObj.getName() + "` WHERE `" + dataObj.getPkey() + 
            "`= \"" + pkey + "\" AND `state` = \"" + dataObjState + "\""; // has to be checked
        
      } else {
        // join in from statement not allowed
//        String q = "SELECT D." + dataObj.getPkey() + " FROM `" + dataObj.getName() + "` D INNER JOIN `" + caseObject + "` M USING ("
//            + dataObj.getFkeys().get(0) + ") WHERE M." + dataObj.getFkeys().get(0) + "=\"" + scopeInstanceId + "\"";
//        queryHandler.runQuery(q);
//        String pkey = queryHandler.getNextResult().get(0).toString();
//        String query2 = "UPDATE IGNORE `" + dataObj.getName() + "` SET `state`=\"" + dataObjState + "\" WHERE `" + dataObj.getPkey() + "`= \"" + pkey
//            + "\" and `state` = (" + state + ")";
        
        List<AttributeUpdate> updates = new ArrayList<AttributeUpdate>();
        AttributeUpdate update = new AttributeUpdate("state", dataObjState);
        updates.add(update);
        
        // assumption is that case object pk identifier equals foreign key identifier
        DataObjectSpecification caseObjectSpec = dataObject(caseObject, dataObj.getFkeys().get(0), scopeInstanceId);
        String[] states = stateList.toArray(new String[0]);
        
        query = anyDataObject(dataObj.getName(), dataObj.getPkey()).references(dataObj.getFkeys().get(0), caseObjectSpec)
            .attribute("state", values(states)).getUpdateStatement(updates).toSqlString();
      }
    } else if (type == "dependent_WithoutFK") { // D^1:1 U3; D^1:n U3; D^m:n U4
      // TODO: has to be checked
      if (expression != null) {
        query = "UPDATE IGNORE `" + dataObj.getName() + "` SET `" + dataObj.getFkeys().get(0) + "` =" + "(SELECT `" + dataObj.getFkeys().get(0) + "` FROM `"
            + caseObject + "` WHERE `" + dataObj.getFkeys().get(0) + "`= \"" + scopeInstanceId + "\"), `state`=\"" + dataObjState + "\" WHERE `"
            + dataObj.getFkeys().get(0) + "` IS NULL and `state` = (" + state + ") AND " + expression;
        
        
        // TODO: new query cannot handle the "expression", an arbitrary snippet of sql
        /*DataObjectSpecification caseObjectSpec = dataObject(caseObject, dataObj.getFkeys().get(0), scopeInstanceId);
        List<AttributeUpdate> updates = new ArrayList<AttributeUpdate>();       
        
        AttributeUpdate update1 = new AttributeUpdate("state", dataObjState);
        AttributeUpdate update2 = new AttributeUpdate(dataObj.getFkeys().get(0), new DataObjectReference(caseObjectSpec));
        updates.add(update1);
        updates.add(update2);
        
        String[] states = stateList.toArray(new String[0]);
        
        String newQuery = anyDataObject(dataObj.getName(), dataObj.getPkey()).attribute(dataObj.getFkeys().get(0), nullValue())
            .attribute("state", values(states)).getUpdateStatement(updates).toSqlString();*/
        
      } else {
//        query = "UPDATE IGNORE `" + dataObj.getName() + "` SET `" + dataObj.getFkeys().get(0) + "` =" + "(SELECT `" + dataObj.getFkeys().get(0) + "` FROM `"
//            + caseObject + "` WHERE `" + dataObj.getFkeys().get(0) + "`= \"" + scopeInstanceId + "\"), `state`=\"" + dataObjState + "\" WHERE `"
//            + dataObj.getFkeys().get(0) + "` IS NULL and `state` = (" + state + ")";
        
        DataObjectSpecification caseObjectSpec = dataObject(caseObject, dataObj.getFkeys().get(0), scopeInstanceId);
        List<AttributeUpdate> updates = new ArrayList<AttributeUpdate>();       
        
        AttributeUpdate update1 = new AttributeUpdate("state", dataObjState);
        AttributeUpdate update2 = new AttributeUpdate(dataObj.getFkeys().get(0), new DataObjectReference(caseObjectSpec));
        updates.add(update1);
        updates.add(update2);
        
        String[] states = stateList.toArray(new String[0]);
        
        query = anyDataObject(dataObj.getName(), dataObj.getPkey()).attribute(dataObj.getFkeys().get(0), nullValue())
            .attribute("state", values(states)).getUpdateStatement(updates).toSqlString();
      }

      // SELECT COUNT( `rid` ) FROM `Receipt` WHERE `oid` IS NULL AND state =
      // "approved"
    } else {
      System.out.println("wrong type");
    }
    return query;
  }

  // TODO: BPMN_SQL added
  // D^1:n C1; C2; U1; D1; D^m:n C1; C2; U1; U2; D1; D2
  private String createSqlQuery(DataObject dataObj, String dataObjState, String scopeInstanceId, String caseObject, String type, int count) {
    String query = "";
    UUID uuid = UUID.randomUUID(); // primary key for dependent data objects

    DataObjectSpecification caseObjectSpec = dataObject(caseObject, dataObj.getFkeys().get(0), scopeInstanceId);
    
    if (type == "dependent_MI") {
      if (dataObj.getPkType().equals("new")) {
        InsertObjectSpecification insertSpec = insert();
        for (int i = 0; i < count; i++) {
          insertSpec.object(dataObject(dataObj.getName(), dataObj.getPkey(), UUID.randomUUID().toString())
              .attribute("state", dataObjState)
              .references(dataObj.getFkeys().get(0), caseObjectSpec));
        }
        query = insertSpec.getStatement().toSqlString();
        
        
//        String query2 = "INSERT INTO `" + dataObj.getName() + "`(`" + dataObj.getPkey() + "`, `" + dataObj.getFkeys().get(0) + "`, `state`) VALUES ";
//        for (int i = 1; i < count; i++) {
//          query2 = query2 + "(\"" + uuid + "\"," + "(SELECT `" + dataObj.getFkeys().get(0) + "` FROM `" + caseObject + "` WHERE `" + dataObj.getFkeys().get(0)
//              + "`= \"" + scopeInstanceId + "\"),\"" + dataObjState + "\"),";
//          uuid = UUID.randomUUID(); // set new UUID for next collection data
//                                    // item
//        }
//        query2 = query2 + "(\"" + uuid + "\"," + "(SELECT `" + dataObj.getFkeys().get(0) + "` FROM `" + caseObject + "` WHERE `" + dataObj.getFkeys().get(0)
//            + "`= " + scopeInstanceId + "),\"" + dataObjState + "\")";

      } else if (dataObj.getPkType().equals("delete")) {
        query = "DELETE FROM `" + dataObj.getName() + "` WHERE `" + dataObj.getFkeys().get(0) + "` =" + "(SELECT `" + dataObj.getFkeys().get(0) + "` FROM `"
            + caseObject + "` WHERE `" + dataObj.getFkeys().get(0) + "`= " + scopeInstanceId + ") AND `state` = \"" + dataObjState + "\"";
        throw new RuntimeException("not tested");
      } else {
        query = "UPDATE `" + dataObj.getName() + "` SET `state`=\"" + dataObjState + "\" WHERE `" + dataObj.getFkeys().get(0) + "` =" + "(SELECT `"
            + dataObj.getFkeys().get(0) + "` FROM `" + caseObject + "` WHERE `" + dataObj.getFkeys().get(0) + "`= \"" + scopeInstanceId + "\")";
        throw new RuntimeException("not tested");
      }
    } else {
      System.out.println("wrong type");
    }
    return query;
  }

  // TODO: BPMN_SQL added
  // D^1:n U2; D^m:n U3
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
        // TODO dead branch according to tests
        
        query = "INSERT INTO `" + dataObj.getName() + "`(`" + dataObj.getPkey() + "`, `" + dataObj.getFkeys().get(0) + "`, `state`) VALUES ";
        for (int i = 1; i < count; i++) {
          query = query + "(\"" + uuid + "\"," + "(SELECT `" + dataObj.getFkeys().get(0) + "` FROM `" + caseObject + "` WHERE `" + dataObj.getFkeys().get(0)
              + "`= \"" + scopeInstanceId + "\"),\"" + dataObjState + "\"),";
          uuid = UUID.randomUUID(); // set new UUID for next collection data
                                    // item
        }
        query = query + "(\"" + uuid + "\"," + "(SELECT `" + dataObj.getFkeys().get(0) + "` FROM `" + caseObject + "` WHERE `" + dataObj.getFkeys().get(0)
            + "`= " + scopeInstanceId + "),\"" + dataObjState + "\")";
        
        throw new RuntimeException("not tested");

      } else if (dataObj.getPkType().equals("delete")) {
        query = "DELETE FROM `" + dataObj.getName() + "` WHERE `" + dataObj.getFkeys().get(0) + "` =" + "(SELECT `" + dataObj.getFkeys().get(0) + "` FROM `"
            + caseObject + "` WHERE `" + dataObj.getFkeys().get(0) + "`= " + scopeInstanceId + ") AND `state` = \"" + dataObjState + "\"";
      } else {
//        query = "UPDATE IGNORE `" + dataObj.getName() + "` SET `state`=\"" + dataObjState + "\" WHERE `" + dataObj.getFkeys().get(0) + "` =" + "(SELECT `"
//            + dataObj.getFkeys().get(0) + "` FROM `" + caseObject + "` WHERE `" + dataObj.getFkeys().get(0) + "`= \"" + scopeInstanceId + "\") AND `state` = ("
//            + state + ")";
        
        DataObjectSpecification caseObjectSpec = dataObject(caseObject, dataObj.getFkeys().get(0), scopeInstanceId);
        List<AttributeUpdate> updates = new ArrayList<AttributeUpdate>();       
        
        AttributeUpdate update1 = new AttributeUpdate("state", dataObjState);
        updates.add(update1);
        
        String[] states = stateList.toArray(new String[0]);
        
        query = anyDataObject(dataObj.getName(), dataObj.getPkey()).references(dataObj.getFkeys().get(0), caseObjectSpec)
            .attribute("state", values(states)).getUpdateStatement(updates).toSqlString();
      }
    } else {
      System.out.println("wrong type");
    }
    return query;
  }
}

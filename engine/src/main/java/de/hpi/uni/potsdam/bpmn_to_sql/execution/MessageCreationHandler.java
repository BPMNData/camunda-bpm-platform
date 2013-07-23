package de.hpi.uni.potsdam.bpmn_to_sql.execution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import org.camunda.bpm.engine.impl.bpmn.parser.BpmnParse;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import de.hpi.uni.potsdam.bpmn_to_sql.BpmnDataConfiguration;
import de.hpi.uni.potsdam.bpmn_to_sql.DataObject;
import de.hpi.uni.potsdam.bpmn_to_sql.xquery.XQueryHandler;

public class MessageCreationHandler{
  
  private static XQueryHandler handler = new XQueryHandler();

  protected BpmnDataConfiguration configuration;

  public MessageCreationHandler(BpmnDataConfiguration configuration) {
    this.configuration = configuration;
  }
  
  public ArrayList<String> getXMLMessages(ExecutionEntity execution, String query){
    final String activityId = execution.getActivity().getId();
    final String instanceId = execution.getProcessInstanceId();
    final String activityParentId = execution.getActivity().getParent().getId();
    final String dataObjectID = execution.getEffectiveCaseObjectID();
    DataInputChecker dataChecker = new DataInputChecker(this.configuration);
    
    ArrayList<String> messages = new ArrayList<String>();
    
    if(dataChecker.checkDataInput(execution)){
      messages = buildXMLMessages(activityId, activityParentId, dataObjectID, instanceId, query);            
    }       
    
    return messages;    
  }

  private ArrayList<String> buildXMLMessages(String activityId, String activityParentId, String dataObjectID, String instanceId, String query) {
    ArrayList<String> messages = new ArrayList<String>();
    
    ArrayList<String>xmlData = getDataObjectsAsXML(activityId, activityParentId, dataObjectID, instanceId);
    messages = handler.runXQuery(xmlData, query);
    
    return messages;
  }

  private ArrayList<String> getDataObjectsAsXML(String activityId, String activityParentId, String dataObjectID, String instanceId) {
    HashMap<String, ArrayList<DataObject>> dataObjects = getDataInputsOfActivity(activityId);
    HashMap<String, String> queryMap = createQueryMap(dataObjects, activityId, activityParentId, dataObjectID);
    ArrayList<String> objectXMLs = new ArrayList<String>();
    
    for (Entry<String, String> sqlQuerySet : queryMap.entrySet()) {
      String dataObject = sqlQuerySet.getKey();
      String query = sqlQuerySet.getValue();
      System.out.println("Getting input for object: " + dataObject);
      QueryExecutionHandler.getInstance().runQuery(query);
      ArrayList<String> objectXML = handler.buildObjectXML(dataObject);
      objectXMLs.addAll(objectXML);
    }
    return objectXMLs;
  }
  
  private HashMap<String, ArrayList<DataObject>> getDataInputsOfActivity(String activityId){
    HashMap<String, ArrayList<DataObject>> dataObjectMap = new HashMap<String, ArrayList<DataObject>>();
    // true if activity reads a data object
    if (BpmnParse.getInputData().containsKey(activityId)) { 
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
    }
    return dataObjectMap;
  }
  
  private HashMap<String, String> createQueryMap(HashMap<String, ArrayList<DataObject>> dataObjectMap, String activityId, String activityParentId, String dataObjectID){
    // create SQL queries based on identified pattern
    HashMap<String, String> queryMap = new HashMap<String, String>();

    for (Entry<String, ArrayList<DataObject>> dataObjectSet : dataObjectMap.entrySet()) {
      ArrayList<DataObject> dataObjectList = dataObjectSet.getValue();
      String dataObject = dataObjectSet.getKey();
      String q = new String();
      // create SQL query with respect to type of data object (main,
      // dependent, dependent_MI, external_input)
      if (DataObjectClassification.isMainDataObject(dataObjectList.get(0), activityParentId.split(":")[0])) {
        q = createSqlQuery(dataObjectList, dataObjectID);
      } else if (DataObjectClassification.isDependentDataObjectWithUnspecifiedFK(dataObjectList.get(0), activityParentId.split(":")[0])) {

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
      } else if (DataObjectClassification.isDependentDataObject(dataObjectList.get(0), activityParentId.split(":")[0])) {
        // provide case object of the scope to enable JOINALL; case object is
        // in a map called getScopeInformation which has as key the scope
        // (e.g., process, sub-process) name
        q = createSqlQuery(dataObjectList, dataObjectID, BpmnParse.getScopeInformation().get(activityParentId.split(":")[0]), "dependent");
      } else if (DataObjectClassification.isMIDependentDataObject(dataObjectList.get(0), activityParentId.split(":")[0])) {
        q = createSqlQuery(dataObjectList, dataObjectID, BpmnParse.getScopeInformation().get(activityParentId.split(":")[0]), "dependent_MI");
      } else {
        // non existent data object type identified
        q = null;
      }
      queryMap.put(dataObject, q);
    }
    return queryMap;
  }
  
  //TODO: BPMN_SQL added
  // main data object
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

    query = "SELECT * " 
            + "FROM `" + dataObjectList.get(0).getName() + "` "
            + "WHERE `" + dataObjectList.get(0).getPkey() + "` =\"" + instanceId + "\" and `state` =(" + state + ")";

    return query;
  }

  // TODO: BPMN_SQL added
  // dependent data object
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
  
    if (type == "dependent") {
      query = "SELECT D.* "
              + "FROM `" + dataObjectList.get(0).getName() + "` D INNER JOIN `" + caseObject + "` M USING (" + dataObjectList.get(0).getFkeys().get(0) + ") "
              + "WHERE M." + dataObjectList.get(0).getFkeys().get(0) + "= \"" + instanceId + "\" and D.state =(" + state + ")";
      // does not work anymore as soon as several foreign keys are allowed,
      // i.e., when we extend the data object chain to more than 2 in length
    } else if (type == "dependent_MI") {
      query = "SELECT D.* "
              + "FROM `" + dataObjectList.get(0).getName() + "` D INNER JOIN `" + caseObject + "` M USING (" + dataObjectList.get(0).getFkeys().get(0) + ") "
              + "WHERE M." + dataObjectList.get(0).getFkeys().get(0) + "=\"" + instanceId + "\" and D.state =(" + state + ")";
    } else { // wrong type
      query = null;
    }
  
    return query;
  }

  // TODO: BPMN_SQL added
  // dependent data object with null foreign key
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

    query = "SELECT * "
            + "FROM `" + dataObjectList.get(0).getName() + "` "
            + "WHERE `" + caseObjectPk + "` IS NULL and `state`= (" + state + ")";

    return query;
  }  
  
}

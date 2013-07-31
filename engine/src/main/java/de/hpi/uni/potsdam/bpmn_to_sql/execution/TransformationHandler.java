package de.hpi.uni.potsdam.bpmn_to_sql.execution;

import static de.hpi.uni.potsdam.bpmn_to_sql.pattern.DataObjectSpecification.anyDataObject;
import static de.hpi.uni.potsdam.bpmn_to_sql.pattern.DataObjectSpecification.dataObject;
import static de.hpi.uni.potsdam.bpmn_to_sql.pattern.PlainAttributeValueExpression.nullValue;
import static de.hpi.uni.potsdam.bpmn_to_sql.pattern.PlainAttributeValueExpression.values;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.camunda.bpm.engine.impl.bpmn.data.AbstractDataAssociation;
import org.camunda.bpm.engine.impl.bpmn.parser.BpmnParse;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;

import de.hpi.uni.potsdam.bpmn_to_sql.BpmnDataConfiguration;
import de.hpi.uni.potsdam.bpmn_to_sql.bpmn.DataAssociation;
import de.hpi.uni.potsdam.bpmn_to_sql.bpmn.DataObject;
import de.hpi.uni.potsdam.bpmn_to_sql.xquery.XQueryHandler;

public class TransformationHandler{
  
  private static XQueryHandler handler = new XQueryHandler();

  protected BpmnDataConfiguration configuration;

  public TransformationHandler(BpmnDataConfiguration configuration) {
    this.configuration = configuration;
  }
  
  public void transformInputData(ExecutionEntity execution){
    String inputData = getTransformedInputData(execution);
    execution.setVariableLocal("dataInput", inputData);    
  }
  
  public void transformOutputData(ExecutionEntity execution){
    String outputData = (String) execution.getVariableLocal("dataOutput");
    transformDataObjects(execution, outputData);
  }
  
  private void transformDataObjects(ExecutionEntity execution, String outputData) {
    ActivityImpl activity = execution.getActivity();
    final String dataObjectID = execution.getEffectiveCaseObjectID();
    
    for(AbstractDataAssociation outputDataAssociation : activity.getDataOutputAssociations()){
      String xQuery = ((DataAssociation)outputDataAssociation).getTransformation();
      ArrayList<String> resultObjects = handler.runXQuery(outputData, xQuery);
      for(String resultObject : resultObjects){
        HashMap<String, HashMap<String, String>> object = handler.extractInformation(resultObject);
      }
    }
    
  }

  private String getTransformedInputData(ExecutionEntity execution){
    ActivityImpl activity = execution.getActivity();
    final String dataObjectID = execution.getEffectiveCaseObjectID();
    
    String xQuery = "";
    for(AbstractDataAssociation inputDataAssociation : activity.getDataInputAssociations()){
      if(xQuery.equals("")){
        xQuery = ((DataAssociation)inputDataAssociation).getTransformation();
      }
    }
    
    ArrayList<String> xmlData = getDataObjectsAsXML(activity, dataObjectID);
    
    ArrayList<String> queryResults = getTransformedData(xmlData, xQuery);
    String inputData = "";
    if(!queryResults.isEmpty()){
      inputData = getTransformedData(xmlData, xQuery).get(0);
    }      
           
    return inputData;    
  }

  private ArrayList<String> getTransformedData(ArrayList<String> xmlData, String query) {
    ArrayList<String> transformedData = new ArrayList<String>();
        
    transformedData = handler.runXQuery(xmlData, query);
    
    return transformedData;
  }

  private ArrayList<String> getDataObjectsAsXML(ActivityImpl activity, String dataObjectID) {
    HashMap<String, ArrayList<DataObject>> dataObjects = getDataInputsOfActivity(activity);
    HashMap<String, String> queryMap = createQueryMap(dataObjects, activity.getId(), activity.getParent().getId(), dataObjectID);
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
        q = createSqlQuery(dataObjectList, dataObjectID, BpmnParse.getScopeInformation().get(activityParentId.split(":")[0]));
      } else if (DataObjectClassification.isMIDependentDataObject(dataObjectList.get(0), activityParentId.split(":")[0])) {
        q = createSqlQuery(dataObjectList, dataObjectID, BpmnParse.getScopeInformation().get(activityParentId.split(":")[0]));
      } else {
        // non existent data object type identified
        q = null;
      }
      queryMap.put(dataObject, q);
    }
    return queryMap;
  }
  
  // TODO: BPMN_SQL added
  // main data object
  // CR1; CR2
  private String createSqlQuery(List<DataObject> dataObjectList, String instanceId) {
    // TODO our stuff
    String coName = dataObjectList.get(0).getName();
    String pk = dataObjectList.get(0).getPkey();
    
    String[] states = extractStates(dataObjectList);
    
    String query = dataObject(coName, pk, instanceId).attribute("state", values(states)).getSelectStarStatement().toSqlString();

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
        .references(foreignKey, dataObject(caseObject, foreignKey, instanceId)).getSelectStarStatement().toSqlString();

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
       .attribute(caseObjectPk, nullValue()).getSelectStarStatement().toSqlString();

    return query;
  }
  
}

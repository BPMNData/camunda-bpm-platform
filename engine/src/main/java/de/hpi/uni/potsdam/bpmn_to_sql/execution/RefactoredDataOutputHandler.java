package de.hpi.uni.potsdam.bpmn_to_sql.execution;

import static de.hpi.uni.potsdam.bpmn_to_sql.pattern.DataObjectSpecification.anyDataObject;
import static de.hpi.uni.potsdam.bpmn_to_sql.pattern.DataObjectSpecification.dataObject;
import static de.hpi.uni.potsdam.bpmn_to_sql.pattern.InsertObjectSpecification.insert;
import static de.hpi.uni.potsdam.bpmn_to_sql.pattern.PlainAttributeValueExpression.nullValue;
import static de.hpi.uni.potsdam.bpmn_to_sql.pattern.PlainAttributeValueExpression.values;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.camunda.bpm.engine.impl.bpmn.parser.BpmnParse;
import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution;

import de.hpi.uni.potsdam.bpmn_to_sql.bpmn.DataObject;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.AttributeUpdate;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.DataObjectReference;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.DataObjectSpecification;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.InsertObjectSpecification;

public class RefactoredDataOutputHandler {

  /**
   * Gets all input data objects of the same type
   */
  private ArrayList<DataObject> getMatchingInputDataObjects(DataObject item, String id) {
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

  
  public void updateOutputs(ActivityExecution execution) {
    String caseObjectId = execution.getEffectiveCaseObjectID();
    
    TransformationHandler transformationHandler = new TransformationHandler(Context.getProcessEngineConfiguration().getBpmnDataConfiguration());
    transformationHandler.transformInputData((ExecutionEntity)execution);

    // has outputs
    if (BpmnParse.getOutputData().containsKey(execution.getActivity().getId())) {
      
      List<DataObject> outputObjects = BpmnParse.getOutputData().get(execution.getActivity().getId());
      for (DataObject outputObject : outputObjects) {
        
        String query = new String();

        Set<String> inputStates = new HashSet<String>();

        String outputObjectState = outputObject.getState();

        // check whether the state of the object is a process variable
        if (outputObjectState.startsWith("$")) {
          outputObjectState = (String) execution.getVariableLocal(outputObject.getState().substring(1));
        }

        List<DataObject> correspondingInputObjects = getMatchingInputDataObjects(outputObject, execution.getActivity().getId());
        for (DataObject inputObject : correspondingInputObjects) {
          inputStates.add(inputObject.getState());
        }
        String[] inputStatesArray = inputStates.toArray(new String[0]);

        // create SQL query with respect to type of data object (main,
        // dependent, dependent_MI, dependent_WithoutFK)
        String scope = execution.getActivity().getParent().getId().split(":")[0];
        
        // TODO make all these extractions part of some context object
        boolean isCaseObject = DataObjectClassification.isMainDataObject(outputObject, scope);
        boolean isDependentObject = !isCaseObject;
        boolean isSingleDependentDataObject = DataObjectClassification.isDependentDataObject(outputObject, scope);
        boolean isDependentWithoutForeignKey = false;
        if (!correspondingInputObjects.isEmpty()) {
          isDependentWithoutForeignKey = DataObjectClassification.isDependentDataObjectWithUnspecifiedFK(correspondingInputObjects.get(0), scope);
        }
            
        boolean isCollectionDependentDataObject = DataObjectClassification.isMIDependentDataObject(outputObject, scope);
        
        String caseObjectName = BpmnParse.getScopeInformation().get(scope);
        
        String expression = null;
        if (outputObject.getProcessVariable() != null) {
          expression = (String) execution.getVariable(outputObject.getProcessVariable());
        }
        
        
        String statementType = outputObject.getPkType();
        if (statementType.equals("new")) {
          InsertObjectSpecification insertSpec = insert();
          int numberOfItems = 1;
          if (isCollectionDependentDataObject) {
            numberOfItems = Integer.parseInt((String) execution.getVariable(outputObject.getProcessVariable()));
          }
          for (int i = 0; i < numberOfItems; i++) {
            DataObjectSpecification objectSpec = dataObject(outputObject.getName(), outputObject.getPkey(), UUID.randomUUID().toString()).attribute("state", outputObjectState);
            objectSpec = getInsertAttributes(execution, objectSpec, outputObject).references(outputObject.getFkeys().get(0), dataObject(caseObjectName, outputObject.getFkeys().get(0), caseObjectId));
            insertSpec.object(objectSpec);
          }
          query = insertSpec.getStatement().toSqlString();
        } else if (statementType.equals("delete")) {
          DataObjectSpecification dataObjectSpec = null;
          if (isCaseObject) {
            dataObjectSpec = dataObject(caseObjectName, outputObject.getPkey(), caseObjectId);
          } else {
            dataObjectSpec = anyDataObject(outputObject.getName(), outputObject.getPkey());
            String caseObjectForeignKey = outputObject.getFkeys().get(0);
            if (isDependentWithoutForeignKey) {
              dataObjectSpec.attribute(caseObjectForeignKey, nullValue());
            }
          }
          
          dataObjectSpec.attribute("state", values(inputStatesArray));
          
          query = dataObjectSpec.getDeleteStatement().toSqlString();
        } else {
          // else is update
          DataObjectSpecification dataObjectSpec = null;
          if (isCaseObject) {
            dataObjectSpec = dataObject(caseObjectName, outputObject.getPkey(), caseObjectId);
          } else {
            dataObjectSpec = anyDataObject(outputObject.getName(), outputObject.getPkey());
            String caseObjectForeignKey = outputObject.getFkeys().get(0);
            if (isDependentWithoutForeignKey) {
              dataObjectSpec.attribute(caseObjectForeignKey, nullValue());
            }
          }
          
          // TODO if empty check needed?
          dataObjectSpec.attribute("state", values(inputStatesArray));
          
          if (expression != null) {
            dataObjectSpec.plainSqlSelection(expression);
          }
          
          List<AttributeUpdate> updates = new ArrayList<AttributeUpdate>();
          AttributeUpdate stateUpdate = new AttributeUpdate("state", outputObjectState);
          updates.add(stateUpdate);
          if (isDependentObject) {
            String caseObjectForeignKey = outputObject.getFkeys().get(0);
            AttributeUpdate foreignKeyUpdate = new AttributeUpdate(caseObjectForeignKey, new DataObjectReference(dataObject(caseObjectName, caseObjectForeignKey, caseObjectId)));
            updates.add(foreignKeyUpdate);
          }
          
          updates.addAll(getAttributeUpdates(execution,outputObject));
          
          query = dataObjectSpec.getUpdateStatement(updates).toSqlString();
        }

        System.out.println(query);
        QueryExecutionHandler queryHandler = QueryExecutionHandler.getInstance();
        queryHandler.runUpdate(query);
      }
    }
  }
  
  private List<AttributeUpdate> getAttributeUpdates(ActivityExecution execution, DataObject dataObject){
    List<AttributeUpdate> updates = new ArrayList<AttributeUpdate>();
    
    if (execution.hasVariableLocal("dataObjects")){
      HashMap<DataObject, ArrayList<HashMap<String, String>>> inputDataObjects = (HashMap<DataObject, ArrayList<HashMap<String, String>>>) execution.getVariableLocal("dataObjects");
      for (HashMap<String, String> objectUpdate : inputDataObjects.get(dataObject)){
        for (Entry<String, String> attributeUpdate : objectUpdate.entrySet()){
          if (!attributeUpdate.getValue().equals("")){
            AttributeUpdate stateUpdate = new AttributeUpdate(attributeUpdate.getKey(), attributeUpdate.getValue());
            updates.add(stateUpdate);            
          }
        }
      }
    }    
    return updates;
  }
  
  private DataObjectSpecification getInsertAttributes(ActivityExecution execution, DataObjectSpecification dataSpec, DataObject dataObject ){
    if (execution.hasVariableLocal("dataObjects")){
      HashMap<DataObject, ArrayList<HashMap<String, String>>> inputDataObjects = (HashMap<DataObject, ArrayList<HashMap<String, String>>>) execution.getVariableLocal("dataObjects");
      for (HashMap<String, String> objectUpdate : inputDataObjects.get(dataObject)){
        for (Entry<String, String> attributeUpdate : objectUpdate.entrySet()){
          if (!attributeUpdate.getValue().equals("")){
            dataSpec = dataSpec.attribute(attributeUpdate.getKey(), attributeUpdate.getValue());         
          }
        }
      }
    }
    return dataSpec;
  }
  

}
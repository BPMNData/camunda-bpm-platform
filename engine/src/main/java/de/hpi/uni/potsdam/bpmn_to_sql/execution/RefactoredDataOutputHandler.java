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
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution;

import de.hpi.uni.potsdam.bpmn_to_sql.bpmn.DataObject;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.AttributeUpdate;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.DataObjectReference;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.DataObjectSpecification;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.InsertObjectSpecification;

/**
 * Runs SQL queries to update the output data objects of an activity.
 */
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
    
    // is this the right location for this code? selection should happen before execution
//    TransformationHandler transformationHandler = new TransformationHandler();
//    transformationHandler.transformInputData((ExecutionEntity)execution);

    // has outputs
    if (BpmnParse.getOutputData().containsKey(execution.getActivity().getId())) {
      
      List<DataObject> outputObjects = BpmnParse.getOutputData().get(execution.getActivity().getId());
      for (DataObject outputObject : outputObjects) {
        
        String query = new String();

        

        List<DataObject> correspondingInputObjects = getMatchingInputDataObjects(outputObject, execution.getActivity().getId());

        // context information
        OutputObjectContext context = OutputObjectContext.fromDataObject(execution, outputObject, correspondingInputObjects);

        DataObjectSpecification caseObject = null;
        if (context.isCaseObject()) {
          caseObject = dataObject(context.getCaseObjectName(), outputObject.getPkey(), caseObjectId);
        } else {
          caseObject = dataObject(context.getCaseObjectName(), outputObject.getFkeys().get(0), caseObjectId);
        }
        
        String statementType = outputObject.getPkType();
        
        // statement creation
        if ("new".equals(statementType)) {
          InsertObjectSpecification insertSpec = insert();
          int numberOfItems = 1;
          if (context.isCollectionDependentDataObject()) {
            numberOfItems = Integer.parseInt((String) execution.getVariable(outputObject.getProcessVariable()));
          }
          for (int i = 0; i < numberOfItems; i++) {
            DataObjectSpecification outputObjectSpec = null;
            if (context.isCaseObject()) {
              outputObjectSpec = caseObject;
              execution.setCaseObjectID(caseObjectId);
            } else {
              outputObjectSpec = dataObject(outputObject.getName(), outputObject.getPkey(), UUID.randomUUID().toString())
                .references(outputObject.getFkeys().get(0), caseObject);
            }
            
            outputObjectSpec.attribute("state", context.getOutputObjectState());
            
            addInsertPayload(execution, outputObjectSpec, outputObject);
            insertSpec.object(outputObjectSpec);
          }
          query = insertSpec.getStatement().toSqlString();
        } else {
          DataObjectSpecification outputObjectSpec = null;
          if (context.isCaseObject()) {
            outputObjectSpec = caseObject;
          } else {
            outputObjectSpec = anyDataObject(outputObject.getName(), outputObject.getPkey());
            String caseObjectForeignKey = outputObject.getFkeys().get(0);
            if (context.isDependentWithoutForeignKey()) {
              outputObjectSpec.attribute(caseObjectForeignKey, nullValue());
            }
          }
          
          if (context.getInputObjectStates().length > 0) {
            outputObjectSpec.attribute("state", values(context.getInputObjectStates()));
          }
          
          if ("delete".equals(statementType)) {
            query = outputObjectSpec.getDeleteStatement().toSqlString();
          } else {
            // update
            if (outputObject.getProcessVariable() != null) {
              String expression = (String) execution.getVariable(outputObject.getProcessVariable());
              outputObjectSpec.plainSqlSelection(expression);
            }
            
            List<AttributeUpdate> updates = new ArrayList<AttributeUpdate>();
            AttributeUpdate stateUpdate = new AttributeUpdate("state", context.getOutputObjectState());
            updates.add(stateUpdate);
            if (context.isDependentObject()) {
              String caseObjectForeignKey = outputObject.getFkeys().get(0);
              AttributeUpdate foreignKeyUpdate = new AttributeUpdate(caseObjectForeignKey, new DataObjectReference(caseObject));
              updates.add(foreignKeyUpdate);
            }
            
            updates.addAll(getAttributeUpdates(execution, outputObject));
            
            query = outputObjectSpec.getUpdateStatement(updates).toSqlString();
          }
          
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
          if (!attributeUpdate.getValue().trim().isEmpty()){
            AttributeUpdate stateUpdate = new AttributeUpdate(attributeUpdate.getKey(), attributeUpdate.getValue());
            updates.add(stateUpdate);            
          }
        }
      }
    }    
    return updates;
  }
  
  private DataObjectSpecification addInsertPayload(ActivityExecution execution, DataObjectSpecification dataSpec, DataObject dataObject ){
    if (execution.hasVariableLocal("dataObjects")){
      HashMap<DataObject, ArrayList<HashMap<String, String>>> inputDataObjects = (HashMap<DataObject, ArrayList<HashMap<String, String>>>) execution.getVariableLocal("dataObjects");
      for (HashMap<String, String> objectUpdate : inputDataObjects.get(dataObject)){
        for (Entry<String, String> attributeUpdate : objectUpdate.entrySet()){
          if (!attributeUpdate.getValue().trim().isEmpty()){
            dataSpec = dataSpec.attribute(attributeUpdate.getKey(), attributeUpdate.getValue());         
          }
        }
      }
    }
    return dataSpec;
  }
  
  private static class OutputObjectContext {
    private boolean isCaseObject;
    private boolean isDependentObject;
    private boolean isSingleDependentDataObject;
    private boolean isDependentWithoutForeignKey;
    private boolean isCollectionDependentDataObject;
    private String caseObjectName;
   
    private List<DataObject> correspondingInputObjects;
    private DataObject outputObject;
    private String outputObjectState;
    
    public static OutputObjectContext fromDataObject(ActivityExecution execution, DataObject outputObject, List<DataObject> correspondingInputObjects) {
      OutputObjectContext context = new OutputObjectContext();
      context.outputObject = outputObject;
      context.correspondingInputObjects = correspondingInputObjects;

      context.outputObjectState = outputObject.getState();

      // check whether the state of the object is a process variable
      if (context.outputObjectState != null && context.outputObjectState.startsWith("$")) {
        context.outputObjectState = (String) execution.getVariableLocal(outputObject.getState().substring(1));
      }
      
      String scope = (String) execution.getActivity().getParent().getId().split(":")[0];
      context.isCaseObject = DataObjectClassification.isMainDataObject(outputObject, scope);
      context.isDependentObject = !context.isCaseObject;
      context.isSingleDependentDataObject = DataObjectClassification.isDependentDataObject(outputObject, scope);
      context.isDependentWithoutForeignKey = false;

      if (!correspondingInputObjects.isEmpty()) {
        context.isDependentWithoutForeignKey = DataObjectClassification.isDependentDataObjectWithUnspecifiedFK(correspondingInputObjects.get(0), scope);
      }
      context.isCollectionDependentDataObject = DataObjectClassification.isMIDependentDataObject(outputObject, scope);
      
      context.caseObjectName = BpmnParse.getScopeInformation().get(scope);
      
      return context;
    }

    public boolean isCaseObject() {
      return isCaseObject;
    }

    public boolean isDependentObject() {
      return isDependentObject;
    }

    public boolean isSingleDependentDataObject() {
      return isSingleDependentDataObject;
    }

    public boolean isDependentWithoutForeignKey() {
      return isDependentWithoutForeignKey;
    }

    public DataObject getOutputObject() {
      return outputObject;
    }
    
    public String getOutputObjectState() {
      return outputObjectState;
    }

    public boolean isCollectionDependentDataObject() {
      return isCollectionDependentDataObject;
    }

    public String getCaseObjectName() {
      return caseObjectName;
    }
    
    public String[] getInputObjectStates() {
      Set<String> inputStates = new HashSet<String>();
      for (DataObject inputObject : correspondingInputObjects) {
        inputStates.add(inputObject.getState());
      }
      return inputStates.toArray(new String[0]);
    }
    
    
  }
  

}
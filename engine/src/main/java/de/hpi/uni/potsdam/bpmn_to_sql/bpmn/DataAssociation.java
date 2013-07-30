package de.hpi.uni.potsdam.bpmn_to_sql.bpmn;

import org.camunda.bpm.engine.delegate.Expression;
import org.camunda.bpm.engine.impl.bpmn.data.AbstractDataAssociation;
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution;

public class DataAssociation extends AbstractDataAssociation {
  
  protected String transformation;
  
  protected DataObject sourceObject;
  protected DataObject targetObject;

  public DataAssociation(String source, String target) {
    super(source, target);
  }

  public DataAssociation(Expression sourceExpression, String target) {
    super(sourceExpression, target);
  }

  @Override
  public void evaluate(ActivityExecution execution) {

  }

  public String getTransformation() {
    return transformation;
  }

  public void setTransformation(String transformation) {
    this.transformation = transformation;
  }

  public DataObject getSourceObject() {
    return sourceObject;
  }

  public void setSourceObject(DataObject sourceObject) {
    this.sourceObject = sourceObject;
  }

  public DataObject getTargetObject() {
    return targetObject;
  }

  public void setTargetObject(DataObject targetObject) {
    this.targetObject = targetObject;
  }
  
  

}

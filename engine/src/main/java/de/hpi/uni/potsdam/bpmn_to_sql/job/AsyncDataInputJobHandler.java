package de.hpi.uni.potsdam.bpmn_to_sql.job;

import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.engine.impl.jobexecutor.AsyncContinuationJobHandler;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;

import de.hpi.uni.potsdam.bpmn_to_sql.execution.DataInputChecker;
import de.hpi.uni.potsdam.bpmn_to_sql.execution.DataInputUnavailableException;
import de.hpi.uni.potsdam.bpmn_to_sql.execution.RefactoredDataInputChecker;
import de.hpi.uni.potsdam.bpmn_to_sql.execution.TransformationHandler;

public class AsyncDataInputJobHandler extends AsyncContinuationJobHandler {

  public final static String TYPE = "async-data-input";

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public void execute(String configuration, ExecutionEntity execution, CommandContext commandContext) {
    RefactoredDataInputChecker dbInputChecker = new RefactoredDataInputChecker(Context.getProcessEngineConfiguration().getBpmnDataConfiguration());
    if (dbInputChecker.checkDataInput(execution)) {
      TransformationHandler transformationHandler = new TransformationHandler(Context.getProcessEngineConfiguration().getBpmnDataConfiguration());
      transformationHandler.transformInputData(execution);
      super.execute(configuration, execution, commandContext);
    } else {
      throw new DataInputUnavailableException("Data input for activity " + execution.getActivityId() + " unavailable.");
    }
  }
}

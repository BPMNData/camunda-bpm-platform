package de.hpi.uni.potsdam.bpmn_to_sql.job;

import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.engine.impl.jobexecutor.AsyncContinuationJobHandler;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;

import de.hpi.uni.potsdam.bpmn_to_sql.execution.DataInputChecker;
import de.hpi.uni.potsdam.bpmn_to_sql.execution.DataInputUnavailableException;

public class AsyncDataInputJobHandler extends AsyncContinuationJobHandler {

  public final static String TYPE = "async-data-input";

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public void execute(String configuration, ExecutionEntity execution, CommandContext commandContext) {

    DataInputChecker dbInputChecker = new DataInputChecker(Context.getProcessEngineConfiguration().getBpmnDataConfiguration());
    if (dbInputChecker.checkDataInput(execution)) {
      super.execute(configuration, execution, commandContext);
    } else {
      throw new DataInputUnavailableException("Data input for activity " + execution.getActivityId() + " unavailable.");
    }
  }
}
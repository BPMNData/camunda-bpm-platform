package de.hpi.uni.potsdam.test.bpmn_to_sql.util;

import java.util.ArrayList;
import java.util.List;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.ProcessEngineConfiguration;
import org.camunda.bpm.engine.impl.ProcessEngineImpl;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.test.PluggableProcessEngineTestCase;
import org.camunda.bpm.engine.runtime.Execution;
import org.camunda.bpm.engine.runtime.Job;
import org.junit.Assert;

import de.hpi.uni.potsdam.bpmn_to_sql.BpmnDataConfiguration;

public abstract class AbstractBpmnDataTestCase extends PluggableProcessEngineTestCase {

  protected static ProcessEngine cachedProcessEngine;
  protected static BpmnDataConfiguration bpmnDataConfiguration;
  
  protected void initializeBpmnDataConfiguration() {
    if (bpmnDataConfiguration == null) {
      bpmnDataConfiguration = new BpmnDataConfiguration();
      bpmnDataConfiguration.setJdbcDriver("org.mysql.jdbc.Driver");
      bpmnDataConfiguration.setJdbcUsername("testuser");
      bpmnDataConfiguration.setJdbcPassword("test623");
      bpmnDataConfiguration.setJdbcUrl("jdbc:mysql://localhost:3306/testdb");
    }
  }
  
  @Override
  protected void initializeProcessEngine() {
    if (cachedProcessEngine == null) {
      initializeBpmnDataConfiguration();
      
      cachedProcessEngine = ((ProcessEngineConfigurationImpl) ProcessEngineConfiguration
          .createProcessEngineConfigurationFromResource("activiti.cfg.xml"))
          .setBpmnDataAware(true)
          .setBpmnDataConfiguration(bpmnDataConfiguration)
          .buildProcessEngine();
    }
    processEngine = cachedProcessEngine;
  }
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    SqlTestHelper.sqlScriptDatabaseSetUp(getClass(), getName());
  }
  
  /**
   * Asserts for the given activity, that <code>numExistingInstances</code> of it exist in the database (i.e. executions waiting in that activity)
   * and subsequently executes <code>numInstancesToExecute</code> of these.
   * @param activityId
   * @param numInstancesToExecute
   * @param numExistingInstances
   */
  protected void assertAndRunDataInputJobForActivity(String activityId, int numInstancesToExecute, int numExistingInstances) {
    Assert.assertEquals(numExistingInstances, runtimeService.createExecutionQuery().activityId(activityId).count());
    
    List<Execution> executions = runtimeService.createExecutionQuery()
        .activityId(activityId).list();
    
    Assert.assertEquals("There number of waiting executions doesn't match for activity " + activityId, numExistingInstances, executions.size());
    
    List<Execution> executionsToBeProcessed = executions.subList(0, numInstancesToExecute);
    
    for (Execution execution : executionsToBeProcessed) {
      Job currentInputDataJob = managementService.createJobQuery().executionId(execution.getId()).singleResult();
      managementService.executeJob(currentInputDataJob.getId());
    }
    
  }
  
}

package de.hpi.uni.potsdam.test.bpmn_to_sql;

import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.runtime.Job;
import org.camunda.bpm.engine.test.Deployment;
import org.junit.Assert;

import de.hpi.uni.potsdam.test.bpmn_to_sql.util.AbstractBpmnDataTestCase;
import de.hpi.uni.potsdam.test.bpmn_to_sql.util.DatabaseSetup;

public class BpmnDataJobTest extends AbstractBpmnDataTestCase {

  @DatabaseSetup(resources = "de/hpi/uni/potsdam/test/bpmn_to_sql/BpmnDataJobTest.setup.sql")
  @Deployment
  public void testInfiniteJobRetries() {
    runtimeService.startBpmnDataAwareProcessInstanceByKey("jobTestProcess", "42");
    
    Job inputDataJob = managementService.createJobQuery().singleResult();
    Assert.assertEquals(3, inputDataJob.getRetries());
    
    try {
      managementService.executeJob(inputDataJob.getId());
    } catch (ProcessEngineException e) {
      // job failure is expected
      inputDataJob = managementService.createJobQuery().singleResult();
      Assert.assertEquals("Retries should not have been decremented", Integer.MAX_VALUE, inputDataJob.getRetries());
    }
  }
}

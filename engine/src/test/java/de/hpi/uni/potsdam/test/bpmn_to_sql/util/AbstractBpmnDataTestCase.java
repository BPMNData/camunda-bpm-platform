package de.hpi.uni.potsdam.test.bpmn_to_sql.util;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.ProcessEngineConfiguration;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.test.PluggableProcessEngineTestCase;

public abstract class AbstractBpmnDataTestCase extends PluggableProcessEngineTestCase {

  protected static ProcessEngine cachedProcessEngine;
  
  @Override
  protected void initializeProcessEngine() {
    cachedProcessEngine = ((ProcessEngineConfigurationImpl) ProcessEngineConfiguration
        .createProcessEngineConfigurationFromResource("activiti.cfg.xml"))
        .setBpmnDataAware(true)
        .buildProcessEngine();
  }
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    SqlTestHelper.sqlScriptDatabaseSetUp(getClass(), getName());
  }
}

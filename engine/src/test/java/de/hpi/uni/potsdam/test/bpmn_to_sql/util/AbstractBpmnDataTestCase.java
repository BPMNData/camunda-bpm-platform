package de.hpi.uni.potsdam.test.bpmn_to_sql.util;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.ProcessEngineConfiguration;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.test.PluggableProcessEngineTestCase;

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
}

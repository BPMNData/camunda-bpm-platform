package de.hpi.uni.potsdam.test.bpmn_to_sql.configuration;

import junit.framework.TestCase;

import org.camunda.bpm.engine.ProcessEngineConfiguration;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.junit.Assert;

import de.hpi.uni.potsdam.bpmn_to_sql.BpmnDataConfiguration;

public class BpmnDataProcessEngineConfigurationTest extends TestCase {

  public void testBpmnDataAwareProcessEngineConfiguraton() {
    String processEngineConfigFile = "de/hpi/uni/potsdam/test/bpmn_to_sql/configuration/bpmn_data_aware_engine.cfg.xml";
    ProcessEngineConfigurationImpl config = (ProcessEngineConfigurationImpl) 
        ProcessEngineConfiguration.createProcessEngineConfigurationFromResource(processEngineConfigFile);
    
    Assert.assertTrue(config.isBpmnDataAware());
    
    BpmnDataConfiguration bpmnDataConfig = config.getBpmnDataConfiguration();
    Assert.assertNotNull(bpmnDataConfig);
    Assert.assertEquals("com.mysql.jdbc.Driver", bpmnDataConfig.getJdbcDriver());
    Assert.assertEquals("jdbc:mysql://localhost:3306/testdb", bpmnDataConfig.getJdbcUrl());
    Assert.assertEquals("testuser", bpmnDataConfig.getJdbcUsername());
    Assert.assertEquals("test623", bpmnDataConfig.getJdbcPassword());
    
  }
}

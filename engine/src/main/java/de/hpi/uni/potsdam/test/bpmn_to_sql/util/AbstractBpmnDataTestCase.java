package de.hpi.uni.potsdam.test.bpmn_to_sql.util;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Properties;

import org.apache.ibatis.builder.xml.XMLConfigBuilder;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionManager;
import org.apache.ibatis.session.defaults.DefaultSqlSessionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.ProcessEngineConfiguration;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.test.PluggableProcessEngineTestCase;
import org.camunda.bpm.engine.impl.util.IoUtil;
import org.camunda.bpm.engine.impl.util.ReflectUtil;
import org.camunda.bpm.engine.runtime.Execution;
import org.camunda.bpm.engine.runtime.Job;
import org.junit.Assert;

import de.hpi.uni.potsdam.bpmn_to_sql.BpmnDataConfiguration;

public abstract class AbstractBpmnDataTestCase extends PluggableProcessEngineTestCase {

  protected static ProcessEngine cachedProcessEngine;
  protected static BpmnDataConfiguration bpmnDataConfiguration;
  
  protected static final String BPMN_DATA_MAPPING_FILE = "de/hpi/uni/potsdam/test/bpmn_to_sql/db/mappings.xml";
  protected static SqlSessionFactory bpmnDataSessionFactory;
  protected static PersistentObjectManager poManager;
  
  protected void initializeBpmnDataConfiguration() {
    if (bpmnDataConfiguration == null) {
      bpmnDataConfiguration = new BpmnDataConfiguration();
      bpmnDataConfiguration.setJdbcDriver("com.mysql.jdbc.Driver");
      bpmnDataConfiguration.setJdbcUsername("testuser");
      bpmnDataConfiguration.setJdbcPassword("test623");
      bpmnDataConfiguration.setJdbcUrl("jdbc:mysql://localhost:3306/testdb");
      
      initializeBpmnDataSessionFactory(bpmnDataConfiguration);
      initPersistentObjectManager();
    }
  }
  
  protected void initializeBpmnDataSessionFactory(BpmnDataConfiguration configuration) {
    if (bpmnDataSessionFactory == null) {
      PooledDataSource dataSource = 
          new PooledDataSource(ReflectUtil.getClassLoader(), configuration.getJdbcDriver(), 
              configuration.getJdbcUrl(), configuration.getJdbcUsername(), configuration.getJdbcPassword());
      Environment environment = new Environment("bpmnDataTest", new JdbcTransactionFactory(), dataSource);
      
      InputStream inputStream = null;
      try {
        inputStream = ReflectUtil.getResourceAsStream(getCustomDbMappingsFilePath());
        Reader reader = new InputStreamReader(inputStream);
        
        Properties properties = new Properties();
        XMLConfigBuilder parser = new XMLConfigBuilder(reader, "bpmnDataTest", properties);
        Configuration mybatisConfiguration = parser.getConfiguration();
        mybatisConfiguration.setEnvironment(environment);
        mybatisConfiguration = parser.parse();

        bpmnDataSessionFactory = new DefaultSqlSessionFactory(mybatisConfiguration);
      } finally {
        IoUtil.closeSilently(inputStream);
      }
      
    }
  }
  
  protected String getCustomDbMappingsFilePath() {
    return BPMN_DATA_MAPPING_FILE;
  }
  
  protected void initPersistentObjectManager() {
    if (poManager == null) {
      poManager = new PersistentObjectManager(SqlSessionManager.newInstance(bpmnDataSessionFactory));
      PersistentObjectAssertionSpecification.setPersitentObjectManager(poManager);
    }
  }

  @Override
  protected void initializeProcessEngine() {
    if (cachedProcessEngine == null) {
      initializeBpmnDataConfiguration();
      
      cachedProcessEngine = ((ProcessEngineConfigurationImpl) ProcessEngineConfiguration
          .createProcessEngineConfigurationFromResource("camunda.cfg.xml"))
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
  
  @Override
  protected void assertAndEnsureCleanDb() throws Throwable {
    super.assertAndEnsureCleanDb();
  }
}
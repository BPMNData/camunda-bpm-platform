package de.hpi.uni.potsdam.test.bpmnToSql.util;

import org.camunda.bpm.engine.impl.test.PluggableProcessEngineTestCase;

public abstract class AbstractBpmnDataTestCase extends PluggableProcessEngineTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    SqlTestHelper.sqlScriptDatabaseSetUp(getClass(), getName());
  }
}

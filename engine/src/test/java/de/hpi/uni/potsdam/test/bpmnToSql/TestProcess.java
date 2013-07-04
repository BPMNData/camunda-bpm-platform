/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.hpi.uni.potsdam.test.bpmnToSql;

import org.camunda.bpm.engine.impl.test.PluggableProcessEngineTestCase;
import org.camunda.bpm.engine.test.Deployment;

import de.hpi.uni.potsdam.test.bpmnToSql.util.DatabaseSetup;


/**
 * @author 
 */
public class TestProcess extends PluggableProcessEngineTestCase {

  @Deployment
  public void testBpmnToSql() {
    //runtimeService.startProcessInstanceByKey("testProcess");
  }
  
  @DatabaseSetup(resources = "de/hpi/uni/potsdam/test/bpmnToSql/testdb.sql")
  @Deployment
  public void testMn() {
	    runtimeService.startProcessInstanceByKey("mn");
	    waitForJobExecutorToProcessAllJobs(5000L, 500);
	  }
}

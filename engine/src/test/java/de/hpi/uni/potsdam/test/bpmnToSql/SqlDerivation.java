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

import java.util.List;

import org.camunda.bpm.engine.runtime.Job;
import org.camunda.bpm.engine.test.Deployment;

import de.hpi.uni.potsdam.test.bpmnToSql.util.AbstractBpmnDataTestCase;
import de.hpi.uni.potsdam.test.bpmnToSql.util.DatabaseSetup;


/**
 * @author 
 */
public class SqlDerivation extends AbstractBpmnDataTestCase {

  /**
   * FIXME: currently in error state due to mismatching db setup?!
   */
  @DatabaseSetup(resources = "de/hpi/uni/potsdam/test/bpmnToSql/testdb.sql")
  @Deployment
  public void testOneToN() {
    runtimeService.startProcessInstanceByKey("oneToN");
    
    waitForJobExecutorToProcessAllJobs(6000L, 500);
  }
  
  @DatabaseSetup(resources = "de/hpi/uni/potsdam/test/bpmnToSql/testdb.sql")
  @Deployment
  public void testMToN() {
    runtimeService.startProcessInstanceByKey("mToN");
    
    waitForJobExecutorToProcessAllJobs(6000L, 500);
  }
  
  public boolean areJobsAvailable() {
    List<Job> jobs = managementService
        .createJobQuery()
        .executable()
        .list();
    
    for (Job job : jobs) {
      System.out.println("Execution " + job.getExecutionId());
      System.out.println("Retries: " + job.getRetries());
    }
    
    return super.areJobsAvailable();
  }
}

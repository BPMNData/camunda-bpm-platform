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

package de.hpi.uni.potsdam.test.bpmn_to_sql;

import java.util.List;

import org.camunda.bpm.engine.runtime.Execution;
import org.camunda.bpm.engine.runtime.Job;
import org.camunda.bpm.engine.test.Deployment;
import org.junit.Assert;

import de.hpi.uni.potsdam.test.bpmn_to_sql.util.AbstractBpmnDataTestCase;
import de.hpi.uni.potsdam.test.bpmn_to_sql.util.DatabaseSetup;


/**
 * @author 
 */
public class SqlDerivation extends AbstractBpmnDataTestCase {

  /**
   * FIXME: currently in error state due to mismatching db setup?!
   */
  @DatabaseSetup(resources = "de/hpi/uni/potsdam/test/bpmn_to_sql/testdb.sql")
  @Deployment
  public void testOneToN() {
    runtimeService.startBpmnDataAwareProcessInstanceByKey("oneToN", "4");
    
    // activity A
    assertAndRunDataInputJobForActivity("A__sid-A711B8E7-258E-4F18-B9CF-B19E3D0763AB", 1, 1);
    
    // B
    assertAndRunDataInputJobForActivity("B__sid-689E0F52-A9A5-4826-BCC8-640BB31B579D", 1, 1);
    
    // C
    assertAndRunDataInputJobForActivity("C__sid-3A2CEE9F-F57C-4305-B988-EFF65ED77A74", 1, 1);
    
    // sub process 1
    assertAndRunDataInputJobForActivity("SP__sid-D9CCBBEC-5F65-4B6E-9C97-4E9E6C656D4D", 1, 1);
    
    // sub process 1 -> D
    assertAndRunDataInputJobForActivity("D__gid-689E0F52-A9A5-4826-BCC8-640BB31B579D", 1, 1);

    // sub process 1 -> G
    assertAndRunDataInputJobForActivity("G__gid-3A2CEE9F-F57C-4305-B988-EFF65ED77A74", 1, 1);
    
    // H
    assertAndRunDataInputJobForActivity("H__vsid-4C30A198-36BC-4D97-975F-08043A4CCB6E", 1, 1);
    
    // subprocess 2
    assertAndRunDataInputJobForActivity("SP-2__sid-D9CCBBEC-5F65-4B6E-9C97-4E9E6C656D4D", 1, 1);
    
    
    //sub process 2 -> I
    assertAndRunDataInputJobForActivity("I__shgid-689E0F52-A9A5-4826-BCC8-640BB31B579D", 5, 5);
    
    
    // E
    assertAndRunDataInputJobForActivity("E__sid-4C30A198-36BC-4D97-975F-08043A4CCB6E", 1, 1);
    
    // F
    assertAndRunDataInputJobForActivity("F__sid-9B5407DB-50EE-4BC7-B6E3-B47E2B25BA9E", 1, 1);

    Assert.assertEquals("The process instance should have ended.", 0, runtimeService.createProcessInstanceQuery().count());
    Assert.assertEquals("There should be no further jobs in the database", 0, managementService.createJobQuery().count());
  }
  
  @DatabaseSetup(resources = "de/hpi/uni/potsdam/test/bpmn_to_sql/testdb.sql")
  @Deployment
  public void testMToN() {
    runtimeService.startBpmnDataAwareProcessInstanceByKey("mToN", "4");
    
    // A
    assertAndRunDataInputJobForActivity("A__sid-66885BC6-9B96-45C4-B658-5A7E8EB3E0DA", 1, 1);
    
    // subprocess1
    assertAndRunDataInputJobForActivity("SP1__sid-8B31A8CE-F0F8-4D27-839A-2D3188DF398E", 1, 1);
    
    // subprocess1 -> B
    assertAndRunDataInputJobForActivity("B__sid-BE0561FF-F6E7-48CC-BAD2-83554CEF4CA3", 2, 2);
    
    // C
    assertAndRunDataInputJobForActivity("C__sid-A10E274D-5564-4CCF-9BB6-40E096CC9DBB", 1, 1);
    
    // subprocess2
    assertAndRunDataInputJobForActivity("SP2__sid-584DE683-3353-4834-9A19-05F4B314EB31", 1, 1);
    
    // subprocess2 -> D
    assertAndRunDataInputJobForActivity("D__sid-CEB030D0-3FC4-43F6-9B48-F597A3959E52", 5, 5);
    
    Assert.assertEquals("The process instance should have ended.", 0, runtimeService.createProcessInstanceQuery().count());
    Assert.assertEquals("There should be no further jobs in the database", 0, managementService.createJobQuery().count());
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

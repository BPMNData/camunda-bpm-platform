package de.hpi.uni.potsdam.test.bpmn_to_sql.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static de.hpi.uni.potsdam.test.bpmn_to_sql.util.PersistentObjectAssertionSpecification.dataObjects;

import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.junit.Assert;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import de.hpi.uni.potsdam.test.bpmn_to_sql.util.AbstractBpmnDataTestCase;
import de.hpi.uni.potsdam.test.bpmn_to_sql.util.DatabaseSetup;

// TODO write multi instance receive test (in subprocess)
public class MessageIntegrationTest extends AbstractBpmnDataTestCase {
  
  private static final String BPMN_DATA_MESSAGE = 
      "<message name=\"aMessageName\">" +
      " <correlation>" +
      "   <key name=\"Request Response Correlation Key\">" +
      "     <property name=\"request_id\">42</property>" +
      "   </key>" +
      " </correlation>" +
      " <payload>"+
      "  <response>" +
      "   <item>soccer ball</item>" +
      "   <price>123</price>" +
      "  </response>" +
      " </payload>" +
      "</message>";

  protected WireMockServer mockEndpoint = new WireMockServer(WireMockConfiguration.wireMockConfig());
  
  protected void setUp() throws Exception {
    super.setUp();
    mockEndpoint.start();
    
    // TODO assert message that is sent
    stubFor(post(urlEqualTo("/bpmn-data-endpoint/message"))
        .withHeader("Content-Type", equalTo("application/xml"))
        .willReturn(aResponse().withStatus(204)));
  }
  
  @DatabaseSetup(resources = "de/hpi/uni/potsdam/test/bpmn_to_sql/integration/setup_buyer_db.sql")
  @Deployment
  public void testSender() {
    // TODO 
    // 1. instantiate process twice with different case object (i.e. requests)
    // 2. assert status updataes
    // 3. 
    
    String caseObjectId1 = "42";
    String caseObjectId2 = "23";
    
    ProcessInstance instance1 = runtimeService.startBpmnDataAwareProcessInstanceByKey("Process_1", caseObjectId1);
    ProcessInstance instance2 = runtimeService.startBpmnDataAwareProcessInstanceByKey("Process_1", caseObjectId2);
    
    dataObjects("Request", 1).where("request_id", caseObjectId1).shouldHave("state", "created").doAssert();
    dataObjects("Request", 1).where("request_id", caseObjectId2).shouldHave("state", "created").doAssert();
    
    assertAndRunDataInputJobForActivity("SendTask_1", 2, 2);
    
    dataObjects("Request", 1).where("request_id", caseObjectId1).shouldHave("state", "sent").doAssert();
    dataObjects("Request", 1).where("request_id", caseObjectId2).shouldHave("state", "sent").doAssert();
    
    assertAndRunDataInputJobForActivity("ReceiveTask_1", 2, 2);
    
    // deliver message
    runtimeService.correlateBpmnDataMessage(BPMN_DATA_MESSAGE);
    
    // instance should have finished
    Assert.assertNull(runtimeService.createProcessInstanceQuery().processInstanceId(instance1.getId()).singleResult());
    
    // assert response
    dataObjects("Response", 1)
      .shouldHave("request_id", "42")
      .shouldHave("state", "received")
      .shouldHave("item", "soccer ball")
      .shouldHave("price", "123")
      .doAssert();
    
    // instance should not have finished
    Assert.assertNotNull(runtimeService.createProcessInstanceQuery().processInstanceId(instance2.getId()).singleResult());
    
  }
}

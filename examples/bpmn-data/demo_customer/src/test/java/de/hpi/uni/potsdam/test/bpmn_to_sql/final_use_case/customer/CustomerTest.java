package de.hpi.uni.potsdam.test.bpmn_to_sql.final_use_case.customer;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static de.hpi.uni.potsdam.test.bpmn_to_sql.util.PersistentObjectAssertionSpecification.dataObjects;

import java.util.HashMap;
import java.util.Map;

import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.test.Deployment;
import org.junit.Assert;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import de.hpi.uni.potsdam.test.bpmn_to_sql.util.AbstractBpmnDataTestCase;
import de.hpi.uni.potsdam.test.bpmn_to_sql.util.DatabaseSetup;

public class CustomerTest extends AbstractBpmnDataTestCase {

  private static final String DEMO_DATA_MAPPING_FILE = "de/hpi/uni/potsdam/test/bpmn_to_sql/db/demo_mappings.xml";
  
  private static final String OFFER_MESSAGE = 
      "<message name=\"Message_4\">" +
      " <correlation>" +
      "   <key name=\"Global_Request\">" +
      "     <property name=\"request_id\">42</property>" +
      "   </key>" +
      " </correlation>" +
      " <payload>"+
      "  <Global_Offer>" +
      "   <inboundFlightNumber>123</inboundFlightNumber>" +
      "   <outboundFlightNumber>345</outboundFlightNumber>" +
      "   <price>1000</price>" +
      "  </Global_Offer>" +
      " </payload>" +
      "</message>";
  
  protected WireMockServer mockEndpoint = new WireMockServer(WireMockConfiguration.wireMockConfig());
  
  protected String getCustomDbMappingsFilePath() {
    return DEMO_DATA_MAPPING_FILE;
  }
  
  protected void setUp() throws Exception {
    super.setUp();
    mockEndpoint.start();
    
    // TODO assert message that is sent
    stubFor(post(urlEqualTo("/bpmn-data-endpoint/message"))
        .withHeader("Content-Type", equalTo("application/xml"))
        .willReturn(aResponse().withStatus(204)));
  }
  
  @DatabaseSetup(resources = "setup_customer_db.sql")
  @Deployment(resources = "customer.compiled.bpmn")
  public void testCustomer() {
    String caseObjectId = "42";
    
    ProcessInstance instance = 
        runtimeService.startBpmnDataAwareProcessInstanceByKey("Customer", caseObjectId);
    
    Task enterDetailsTask = taskService.createTaskQuery().singleResult();
    Map<String, Object> variables = new HashMap<String, Object>();
    variables.put("departure", "Berlin");
    variables.put("destination", "London");
    variables.put("start_date", "10.1.2013");
    variables.put("return_date", "1.1.2013");
    taskService.complete(enterDetailsTask.getId(), variables);
    
    dataObjects("TravelDetails", 1).where("travelID", caseObjectId)
      .shouldHave("state", "created")
      .shouldHave("departure", "Berlin")
      .doAssert();
    
    assertAndRunDataInputJobForActivity("SendTask_1", 1, 1);
    
    runtimeService.correlateBpmnDataMessage(OFFER_MESSAGE);
    
    // instance should have finished
    Assert.assertNull(runtimeService.createProcessInstanceQuery().processInstanceId(instance.getId()).singleResult());
    
    dataObjects("TravelDetails", 1).where("travelID", caseObjectId)
      .shouldHave("state", "updated")
      .shouldHave("departure", "Berlin")
      .doAssert();
    
    dataObjects("Flights", 1)
      .shouldHave("travelID", "42")
      .shouldHave("state", "received")
      .shouldHave("inboundFlightNumber", "123")
      .shouldHave("outboundFlightNumber", "345")
      .shouldHave("price", 1000.0d)
      .doAssert();
  }
}

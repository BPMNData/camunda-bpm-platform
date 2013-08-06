package de.hpi.uni.potsdam.test.bpmn_to_sql.final_use_case.customer;

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

public class CustomerTest extends AbstractBpmnDataTestCase {

  private static final String DEMO_DATA_MAPPING_FILE = "de/hpi/uni/potsdam/test/bpmn_to_sql/db/demo_mappings.xml";
  
  private static final String OFFER_MESSAGE = 
      "<message name=\"Offer\">" +
      " <correlation>" +
      "   <key name=\"Global_Request\">" +
      "     <property name=\"request_id\">42</property>" +
      "   </key>" +
      " </correlation>" +
      " <payload>"+
      "  <Global_Offer>" +
      "   <airline>Lufthansa</airline>" +
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
  
  @DatabaseSetup(resources = "de/hpi/uni/potsdam/test/bpmn_to_sql/final_use_case/customer/setup_customer_db.sql")
  @Deployment(resources = "de/hpi/uni/potsdam/test/bpmn_to_sql/final_use_case/customer/FinalPresUseCase_Customer.bpmn")
  public void testCustomer() {
    String caseObjectId = "42";
    
    ProcessInstance instance = 
        runtimeService.startBpmnDataAwareProcessInstanceByKey("sid-FFA22F25-36EF-4BD5-A146-ED92C461BF3D", caseObjectId);
    
    dataObjects("TravelDetails", 1).where("travelID", caseObjectId)
//    .shouldHave("state", "created")
    .doAssert();
    
    assertAndRunDataInputJobForActivity("sid-DCCABAF9-7DB7-4172-AF27-7165547156AE", 1, 1);
    
    runtimeService.correlateBpmnDataMessage(OFFER_MESSAGE);
    
    // instance should have finished
    Assert.assertNull(runtimeService.createProcessInstanceQuery().processInstanceId(instance.getId()).singleResult());
    
    dataObjects("Flights", 1)
      .shouldHave("travelID", "42")
      .shouldHave("state", "received")
      .shouldHave("airline", "Lufthansa")
      .shouldHave("inboundFlightNumber", "123")
      .shouldHave("outboundFlightNumber", "345")
      .shouldHave("price", 1000.0d)
      .doAssert();
  }
}

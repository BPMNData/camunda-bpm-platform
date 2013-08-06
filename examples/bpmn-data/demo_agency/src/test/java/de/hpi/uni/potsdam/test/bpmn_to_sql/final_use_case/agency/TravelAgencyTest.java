package de.hpi.uni.potsdam.test.bpmn_to_sql.final_use_case.agency;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static de.hpi.uni.potsdam.test.bpmn_to_sql.util.PersistentObjectAssertionSpecification.dataObjects;

import java.util.List;

import org.camunda.bpm.engine.runtime.Execution;
import org.camunda.bpm.engine.test.Deployment;
import org.junit.Assert;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import de.hpi.uni.potsdam.test.bpmn_to_sql.util.AbstractBpmnDataTestCase;
import de.hpi.uni.potsdam.test.bpmn_to_sql.util.DatabaseSetup;

public class TravelAgencyTest extends AbstractBpmnDataTestCase {

  private static final String DEMO_DATA_MAPPING_FILE = "de/hpi/uni/potsdam/test/bpmn_to_sql/db/demo_mappings_agency.xml";

  private static final String REQUEST_MESSAGE = 
      "<message name=\"Travel Details\">" +
      " <correlation>" +
      "   <key name=\"Global_Request\">" +
      "     <property name=\"request_id\">42</property>" +
      "   </key>" +
      " </correlation>" +
      " <payload>"+
      "  <Global_Request>" +
      "   <request_id>42</request_id>" +
      "   <departure>Berlin</departure>" +
      "   <destination>London</destination>" +
      "   <start_date>1.1.2000</start_date>" +
      "   <return_date>7.1.2000</return_date>" +
      "  </Global_Request>" +
      " </payload>" +
      "</message>";
  
  private static final String AIRLINE_RESPONSE_MESSAGE_FORMAT = 
      "<message name=\"Offer\">" +
      " <correlation>" +
      "   <key name=\"Global_Request\">" +
      "     <property name=\"request_id\">%s</property>" +
      "   </key>" +
      " </correlation>" +
      " <payload>"+
      "  <Global_Offer>" +
      "   <inboundFlightNumber>123</inboundFlightNumber>" +
      "   <outboundFlightNumber>456</outboundFlightNumber>" +
      "   <price>1000</price>" +
      "  </Global_Offer>" +
      " </payload>" +
      "</message>";
  
  protected WireMockServer mockEndpointAirline1 = new WireMockServer(WireMockConfiguration.wireMockConfig().port(8082));
  
  protected WireMockServer mockEndpointCustomer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(8081));
  
  protected String getCustomDbMappingsFilePath() {
    return DEMO_DATA_MAPPING_FILE;
  }
  
  protected void setUp() throws Exception {
    
    super.setUp();
    mockEndpointAirline1.start();
    mockEndpointCustomer.start();
    
    // TODO assert message that is sent
    
    WireMock.configureFor("localhost", 8081);
    stubFor(post(urlEqualTo("/bpmn-data-endpoint/message"))
        .withHeader("Content-Type", equalTo("application/xml"))
        .willReturn(aResponse().withStatus(204)));
    
    WireMock.configureFor("localhost", 8082);
    stubFor(post(urlEqualTo("/bpmn-data-endpoint/message"))
        .withHeader("Content-Type", equalTo("application/xml"))
        .willReturn(aResponse().withStatus(204)));
  }
  
  @DatabaseSetup(resources = "setup_agency_db.sql")
  @Deployment(resources = "FinalPresUseCase_TravelAgency.bpmn")
  public void testTravelAgency() {
    runtimeService.correlateBpmnDataMessage(REQUEST_MESSAGE);
    
    dataObjects("TravelDetails", 1)
      .where("cu_request_id", "42")
      .where("departure", "Berlin")
      .where("destination", "London")
      .where("start_date", "1.1.2000")
      .where("return_date", "7.1.2000")
      .shouldHave("state", "received")
    .doAssert();
    
    // TODO this should be part of the task
    Execution execution = runtimeService.createExecutionQuery().singleResult();
    runtimeService.setVariable(execution.getId(), "numberOfRequests", "2");
    
    // create airline requests
    assertAndRunDataInputJobForActivity("sid-1DBDF32D-B72A-4C0A-818F-F6BBA7BDD081", 1, 1);
    
    dataObjects("AirlineRequest", 2)
      .where("departure", "Berlin")
      .where("destination", "London")
      .where("start_date", "1.1.2000")
      .where("return_date", "7.1.2000")
      .shouldHave("state", "created")
      .doAssert();
    
    // sub process
    assertAndRunDataInputJobForActivity("sid-034EC80D-1970-4BF0-B055-69D0D86DFC99", 1, 1);
    
    // send task
    assertAndRunDataInputJobForActivity("sid-62E12787-4027-4843-8276-B721BA2D2FE2", 2, 2);
    
    // manipulating the correlation property as we do not know the exact request id
    List<Execution> executions = runtimeService.createExecutionQuery().messageEventSubscriptionName("Offer").list();
    Assert.assertEquals(2, executions.size());
    
    Execution execution1 = executions.get(0);
    runtimeService.setVariableLocal(execution1.getId(), "propRequest_ID", "42");
    
    Execution execution2 = executions.get(1);
    runtimeService.setVariableLocal(execution2.getId(), "propRequest_ID", "23");
    
    
    // send first response
    runtimeService.correlateBpmnDataMessage(String.format(AIRLINE_RESPONSE_MESSAGE_FORMAT, "42"));
    
    dataObjects("AirlineRequest", 1)
      .where("inboundFlightNumber", "123")
      .where("outboundFlightNumber", "456")
      .where("price", 1000.0d)
      .shouldHave("state", "updated")
    .doAssert();
    
    // send second response
    runtimeService.correlateBpmnDataMessage(String.format(AIRLINE_RESPONSE_MESSAGE_FORMAT, "23"));
    
    dataObjects("AirlineRequest", 2)
      .where("inboundFlightNumber", "123")
      .where("outboundFlightNumber", "456")
      .where("price", 1000.0d)
      .shouldHave("state", "updated")
    .doAssert();
    
    // pick best offer
    assertAndRunDataInputJobForActivity("sid-45A7F1E4-8B24-4545-AC15-3D50752C3949", 1, 1);
    
    // refer customer to airline
    assertAndRunDataInputJobForActivity("sid-7882B220-4EDE-4D06-A53B-9B889B4D36D8", 1, 1);
    
    // instance should have finished
    Assert.assertNull(runtimeService.createProcessInstanceQuery().singleResult());
    
  }
  
  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    mockEndpointAirline1.stop();
    mockEndpointCustomer.stop();
  }
}

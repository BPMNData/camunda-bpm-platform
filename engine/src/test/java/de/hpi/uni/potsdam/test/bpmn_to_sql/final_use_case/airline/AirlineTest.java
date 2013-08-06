package de.hpi.uni.potsdam.test.bpmn_to_sql.final_use_case.airline;

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

public class AirlineTest extends AbstractBpmnDataTestCase {


  private static final String DEMO_DATA_MAPPING_FILE = "de/hpi/uni/potsdam/test/bpmn_to_sql/db/demo_mappings.xml";
  
  private static final String REQUEST_MESSAGE = 
      "<message name=\"Request\">" +
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
  
  @DatabaseSetup(resources = "de/hpi/uni/potsdam/test/bpmn_to_sql/final_use_case/airline/setup_airline_db.sql")
  @Deployment(resources = "de/hpi/uni/potsdam/test/bpmn_to_sql/final_use_case/airline/FinalPresUseCase_Airline.bpmn")
  public void testCustomer() {
    runtimeService.correlateBpmnDataMessage(REQUEST_MESSAGE);
    
    dataObjects("Request", 1)
      .where("messageRequestID", "42")
      .where("departure", "Berlin")
      .where("destination", "London")
      .where("start_date", "1.1.2000")
      .where("return_date", "7.1.2000")
	  .shouldHave("state", "created")
    .doAssert();
    
    assertAndRunDataInputJobForActivity("sid-FEC86014-FD18-4BF7-88E5-1B3F8C343264", 1, 1);
    
    dataObjects("Offer", 1)
//	  .shouldHave("requestID", "42")
//	  .shouldHave("state", "received")
//	  .shouldHave("inboundFlightNumber", "1234")
//	  .shouldHave("outboundFlightNumber", "12345")
//	  .shouldHave("price", 1000.0d)
    .doAssert();
    
    assertAndRunDataInputJobForActivity("sid-BB118ADF-5E9D-49E3-B9C2-BCE5FADD1954", 1, 1);
    
    // instance should have finished
    Assert.assertNull(runtimeService.createProcessInstanceQuery().singleResult());
    
  }
}

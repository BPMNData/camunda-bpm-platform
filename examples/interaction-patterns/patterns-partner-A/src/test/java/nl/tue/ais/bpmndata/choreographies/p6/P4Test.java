package nl.tue.ais.bpmndata.choreographies.p6;

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
import org.junit.Ignore;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import de.hpi.uni.potsdam.test.bpmn_to_sql.util.AbstractBpmnDataTestCase;
import de.hpi.uni.potsdam.test.bpmn_to_sql.util.DatabaseSetup;

@Ignore
public class P4Test extends AbstractBpmnDataTestCase {

  private static final String OFFER_MESSAGE =
	"<message name=\"p6_m_message\">" +
	"    <correlation>       " +
	"		<key name=\"Message_P6\">"+
    "	    	<property name=\"conversation_number\">42</property>"+
	"		</key>" +
	"		<key name=\"Message_P6\">"+
    "	    	<property name=\"conversation_number\">42</property>"+
	"		</key>" +
	"	 </correlation>" +
	"    <payload>"+
	"		<Message_P6>"+
	"			<message_id>5610</message_id>"+
	"			<message_text>test</message_text>"+
	"			<conversation_number>42</conversation_number>"+
	"		</Message_P6>"+
	"	</payload>"+
	"</message>";
  
  protected WireMockServer mockEndpoint = new WireMockServer(WireMockConfiguration.wireMockConfig());
  
  protected String getCustomDbMappingsFilePath() {
    return "nl/tue/ais/bpmndata/choreographies/test/db/db_mappings.xml";
  }
  
  protected void setUp() throws Exception {
    super.setUp();
    mockEndpoint.start();
    
    // TODO assert message that is sent
    stubFor(post(urlEqualTo("/bpmn-data-endpoint/message"))
        .withHeader("Content-Type", equalTo("application/xml"))
        .willReturn(aResponse().withStatus(204)));
  }
  
  @DatabaseSetup(resources = "setup_partnerA_db.sql")
  @Deployment(resources = "p6_one_from_many_receive_a.compiled.bpmn")
  public void testCustomer() {
    String caseObjectId = "42";
    
    ProcessInstance instance = 
        runtimeService.startBpmnDataAwareProcessInstanceByKey("p6_one_from_many_receive_a", caseObjectId);
    
    runtimeService.correlateBpmnDataMessage(OFFER_MESSAGE);
    runtimeService.correlateBpmnDataMessage(OFFER_MESSAGE);
    runtimeService.correlateBpmnDataMessage(OFFER_MESSAGE);
    runtimeService.correlateBpmnDataMessage(OFFER_MESSAGE);
    
    // instance should have finished
//    Assert.assertNull(runtimeService.createProcessInstanceQuery().processInstanceId(instance.getId()).singleResult());
    
//    dataObjects("TravelDetails", 1).where("travelID", caseObjectId)
//      .shouldHave("state", "updated")
//      .shouldHave("departure", "Berlin")
//      .doAssert();
//    
//    dataObjects("Flights", 1)
//      .shouldHave("travelID", "42")
//      .shouldHave("state", "received")
//      .shouldHave("inboundFlightNumber", "123")
//      .shouldHave("outboundFlightNumber", "345")
//      .shouldHave("price", 1000.0d)
//      .doAssert();
  }
}

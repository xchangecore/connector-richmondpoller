/**
 * 
 */
package com.saic.uicds.clients.em.richmond;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.apache.xmlbeans.XmlObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.ws.client.core.WebServiceOperations;

import us.va.richmond.ci.eservices.services.publicsafety.traffic.GetCORAllResponseDocument;

import com.saic.uicds.clients.em.richmond.RichmondIncidentPoller;
import com.saic.uicds.clients.sources.Incident;
import com.saic.uicds.clients.sources.IncidentPoller;
import com.saic.uicds.clients.test.MockWebServiceOperations;

/**
 * @author roger
 * 
 */
public class RichmondIncidentPollerTest {

	IncidentPoller poller;

	WebServiceOperations webServiceTemplate;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		webServiceTemplate = new MockWebServiceOperations();
		poller = new RichmondIncidentPoller();
		((RichmondIncidentPoller) poller)
				.setWebServiceTemplate(webServiceTemplate);
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		poller = null;
	}

	/**
	 * Test method for
	 * {@link com.saic.uicds.clients.em.richmond.RichmondIncidentPoller#poll()} and
	 * {@link com.saic.uicds.clients.em.richmond.RichmondIncidentPoller#getIncidents()}
	 * .
	 */
	@Test
	public void testPolling() {
		poller.poll();
		Map<String,Incident> map = poller.getIncidents();
		assertNotNull("Map is null", map);
		assertEquals("Map is not empty", 0, map.size());
	}

	@Test
	public void testPollingResponse() {
		setupInitialIncidents();
		poller.poll();
		
		Map<String,Incident> map = poller.getIncidents();
		assertNotNull("List is null", map);
		assertFalse("List is empty", map.isEmpty());
		assertEquals("Wrong number of incidents", 6, map.size());

		checkInitialIncidents(map);
	}

	@Test
	public void testPollingResponseUpdatesAndDeletes() {
		setupInitialIncidents();
		poller.poll();
	
		GetCORAllResponseDocument doc = RichmondIncidentTestData.getCORAllResonse2();
		assertNotNull(doc);
		setMockWebServiceOperationsResponse(doc);
	
		// poll to see an incident changed to deleted and an update
		poller.poll();
		Map<String,Incident> map = poller.getIncidents();
		assertNotNull("List is null", map);
		assertFalse("List is empty", map.isEmpty());
		assertEquals("Wrong number of incidents", 6, map.size());
	
		checkIncidentUpdates(map);
		
		// poll again to see the deleted incident get removed
		poller.poll();
		map = poller.getIncidents();
		assertNotNull("List is null", map);
		assertFalse("List is empty", map.isEmpty());
		assertEquals("Wrong number of incidents", 5, map.size());
		assertFalse("Incident2 is still in map",map.containsKey("37.577838-77.512054"));
		
	}

	@Test
	public void testPollingResponse3() {
		GetCORAllResponseDocument doc = RichmondIncidentTestData.getCORAllResonse3();
		assertNotNull(doc);
		setMockWebServiceOperationsResponse(doc);
	
		// poll to see an incident changed to deleted and an update
		poller.poll();
		Map<String,Incident> map = poller.getIncidents();
		assertNotNull("List is null", map);
		assertFalse("List is empty", map.isEmpty());
		assertEquals("Wrong number of incidents", 2, map.size());
	
	}
//	@Test
//	public void testPollingSite() {
//		SaajSoapMessageFactory messageFactory = new SaajSoapMessageFactory();
//		try {
//			messageFactory.afterPropertiesSet();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		WebServiceTemplate wst = new WebServiceTemplate(messageFactory);
//
//		XmlBeansMarshaller marshaller = new XmlBeansMarshaller();
//		wst.setMarshaller(marshaller);
//		wst.setUnmarshaller(marshaller);
//		
//		CommonsHttpMessageSender messageSender = new CommonsHttpMessageSender();
//		wst.setMessageSender(messageSender);
//		
//		wst.setDefaultUri("http://eservices.ci.richmond.va.us/services/PublicSafety/Traffic/newcortraffic.asmx");
//		wst.afterPropertiesSet();
//		
//		((RichmondIncidentPoller) poller).setWebServiceTemplate(wst);
//
//		poller.poll();
//		Map<String,Incident> map = poller.getIncidents();
//		assertNotNull("List is null", map);
//		assertFalse("List is empty", map.isEmpty());
//		System.out.println("num incidents: "+map.size());
//	}

	private void setupInitialIncidents() {
		GetCORAllResponseDocument doc = RichmondIncidentTestData.getCORAllResonse1();
		assertNotNull(doc);
		setMockWebServiceOperationsResponse(doc);
	}

	private void checkInitialIncidents(Map<String,Incident> incidents) {
		assertNotNull("Incidents is null", incidents);
		assertTrue("No incidents in result", incidents.size() > 0);

		Incident i = incidents.get("37.534813-77.406029");
		checkIncident(
				i, true, false, false,
				"3116 Q ST RICH / CrossStreet: N 31ST ST",
				"FIRE, HOUSE / STRUCTURE ",
				"3116 Q ST RICH / CrossStreet: N 31ST ST - NORTH",
				"On Scene 1:56 PM",
				"COMP STATED THE HOUSE WAS ON FIRE AND HER GRANDMA IS STILL INSIDE...THE GRANDMA IS WHEELCHAIR BOUND",
				"37.534813", "-77.406029");

		i = incidents.get("37.551497-77.460472");
		checkIncident(
				i, true, false, false,
				"1704 HANOVER AVE RICH / CrossStreet: N ALLEN AVE",
				"FIRE, ALARM, SMOKE/HEAT/DUCT",
				"1704 HANOVER AVE RICH / CrossStreet: N ALLEN AVE - NORTH",
				"On Scene 2:58 PM",
				"BEARD RESD/2ND FLOOR SMOKE...NO KEYHOLDER RESPONDING",
				"37.551497", "-77.460472");
	}

	private void checkIncidentUpdates(Map<String,Incident> incidents) {
		assertNotNull("Incidents is null", incidents);
		assertTrue("No incidents in result", incidents.size() > 0);

		Incident i = incidents.get("37.534813-77.406029");
		checkIncident(
				i, false, true, false,
				"3116 Q ST RICH / CrossStreet: N 31ST ST",
				"FIRE, HOUSE / STRUCTURE ",
				"3116 Q ST RICH / CrossStreet: N 31ST ST - NORTH",
				"On Scene 1:56 PM",
				"COMP STATED THE HOUSE WAS ON FIRE AND HER GRANDMA IS STILL INSIDE...THE GRANDMA IS WHEELCHAIR BOUND, GRANDMA SAFE",
				"37.534813", "-77.406029");

		i = incidents.get("37.577838-77.512054");
		checkIncident(
				i, false, false, true,
				"PATTERSON AVE/GLENBURNIE RD",
				"HIT & RUN ACCIDENT, INVESTIGATE",
				"PATTERSON AVE/GLENBURNIE RD - NORTH",
				"On Scene 2:16 PM",
				"IN PAST FEW MINUTES WHILE COMP WAS INSIDE THE POST OFFICE...SOMEONE LEFT A NOTE ON COMP CAR THAT A MERCEDES \"TRUCK\" STRUCK HER CAR AND DROVE",
				"37.577838", "-77.512054");
		
		// Only one updated incident
		int count = 0;
		for (String key : incidents.keySet()) {
			if (incidents.get(key).isUpdated()) {
				count++;
			}
		}
		assertEquals("Wrong number of updated incidents",1,count);
	}
	
	private void checkIncident(Incident incident, boolean created, boolean updated, boolean deleted,
			String name, String type, String address, String dateTime,
			String description, String latitude, String longitude) {
		assertNotNull("Unknown incident",incident);
		assertEquals("Wrong created state",created,incident.isCreated());
		assertEquals("Wrong updated state",updated,incident.isUpdated());
		assertEquals("Wrong deleted state",deleted,incident.isDeleted());
		assertEquals("Wrong name for incident",name,incident.getName());
		assertEquals("Wrong type for incident",type,incident.getType());
		assertEquals("Wrong address for incident",address,incident.getAddress());
		assertNotNull("DateTime for incident is null",incident.getDateTime());
		assertEquals("Wrong description for incident",description,incident.getDescription());
		assertEquals("Wrong latitude for incident",latitude,incident.getLatitude());
		assertEquals("Wrong longitude for incident",longitude,incident.getLongitude());
	}

	private void setMockWebServiceOperationsResponse(XmlObject response) {
		((MockWebServiceOperations) webServiceTemplate).setResponse(response);
	}

}

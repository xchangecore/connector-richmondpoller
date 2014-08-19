/**
 * 
 */
package com.saic.uicds.clients.em.richmond;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.xmlbeans.XmlObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.ws.client.core.WebServiceOperations;

import us.va.richmond.ci.eservices.services.publicsafety.traffic.GetCORAllResponseDocument;

import com.saic.uicds.clients.sources.Incident;
import com.saic.uicds.clients.sources.IncidentSourceListener;
import com.saic.uicds.clients.test.MockWebServiceOperations;

/**
 * @author roger
 * 
 */
public class RichmondIncidentSourceTest {

    class CountingIncidentSourceListener
        implements IncidentSourceListener {

        public int newCount = 0;
        public int updatedCount = 0;
        public int deletedCount = 0;

        @Override
        public boolean deletedIncident(Incident incident) {

            deletedCount++;
            return true;
        }

        @Override
        public void newIncident(Incident incident) {

            newCount++;
        }

        @Override
        public void updatedIncident(Incident incident) {

            updatedCount++;
        }

        public void reset() {

            newCount = 0;
            updatedCount = 0;
            deletedCount = 0;
        }
    }

    private RichmondIncidentSource source;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp()
        throws Exception {

        source = new RichmondIncidentSource();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown()
        throws Exception {

        source = null;
    }

    /**
     * Test method for
     * {@link com.saic.uicds.clients.em.richmond.RichmondIncidentSource#registerListener(com.saic.uicds.clients.sources.IncidentSourceListener)}
     * .
     */
    @Test
    public void testRegisterListener() {

        CountingIncidentSourceListener listener = new CountingIncidentSourceListener();
        source.registerListener(listener);
        assertTrue("No listener", source.isListening(listener));
    }

    /**
     * Test method for
     * {@link com.saic.uicds.clients.em.richmond.RichmondIncidentSource#unregisterListener(com.saic.uicds.clients.sources.IncidentSourceListener)}
     * .
     */
    @Test
    public void testUnregisterListener() {

        CountingIncidentSourceListener listener = new CountingIncidentSourceListener();
        source.registerListener(listener);
        source.unregisterListener(listener);
        assertFalse("Listener still registered", source.isListening(listener));
    }

    @Test
    public void testMultipleListeners() {

        CountingIncidentSourceListener listener1 = new CountingIncidentSourceListener();
        source.registerListener(listener1);

        CountingIncidentSourceListener listener2 = new CountingIncidentSourceListener();
        source.registerListener(listener2);

        CountingIncidentSourceListener listener3 = new CountingIncidentSourceListener();
        source.registerListener(listener3);

        assertTrue("Listner1 not found", source.isListening(listener1));
        assertTrue("Listner2 not found", source.isListening(listener2));
        assertTrue("Listner3 not found", source.isListening(listener3));

        source.unregisterListener(listener1);
        assertFalse("Listener1 still registered", source.isListening(listener1));

        source.unregisterListener(listener2);
        assertFalse("Listener2 still registered", source.isListening(listener2));

        source.unregisterListener(listener3);
        assertFalse("Listener3 still registered", source.isListening(listener3));

    }

    @Test
    public void testIncidents() {

        RichmondIncidentPoller poller = createPoller();
        setupIncident1(poller);
        source.setPoller(poller);

        CountingIncidentSourceListener listener = new CountingIncidentSourceListener();
        source.registerListener(listener);

        source.poll();

        assertEquals("No new incidents", 6, listener.newCount);
        assertEquals("Have updated incidents", 0, listener.updatedCount);
        assertEquals("Have deleted incidents", 0, listener.deletedCount);

        listener.reset();
        setupIncident2(poller);
        source.poll();
        assertEquals("Have new incidents", 0, listener.newCount);
        assertEquals("Wrong number of updated incidents", 1, listener.updatedCount);
        assertEquals("Wrong number of deleted incidents", 1, listener.deletedCount);

        listener.reset();
        source.poll();
        assertEquals("Have new incidents", 0, listener.newCount);
        assertEquals("Have updated incidents", 0, listener.updatedCount);
        assertEquals("Have deleted incidents", 0, listener.deletedCount);

    }

    private void setupIncident1(RichmondIncidentPoller poller) {

        GetCORAllResponseDocument doc = RichmondIncidentTestData.getCORAllResonse1();
        setMockWebServiceOperationsResponse(poller.getWebServiceTemplate(), doc);
    }

    private void setupIncident2(RichmondIncidentPoller poller) {

        GetCORAllResponseDocument doc = RichmondIncidentTestData.getCORAllResonse2();
        setMockWebServiceOperationsResponse(poller.getWebServiceTemplate(), doc);
    }

    private void setMockWebServiceOperationsResponse(WebServiceOperations webServiceTemplate,
        XmlObject response) {

        ((MockWebServiceOperations) webServiceTemplate).setResponse(response);
    }

    private RichmondIncidentPoller createPoller() {

        RichmondIncidentPoller poller = new RichmondIncidentPoller();

        WebServiceOperations webServiceTemplate = new MockWebServiceOperations();
        poller.setWebServiceTemplate(webServiceTemplate);

        return poller;
    }
}

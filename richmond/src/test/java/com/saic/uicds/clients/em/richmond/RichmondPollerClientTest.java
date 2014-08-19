/**
 * 
 */
package com.saic.uicds.clients.em.richmond;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import junitx.util.PrivateAccessor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.uicds.incidentManagementService.ArchiveIncidentRequestDocument;
import org.uicds.incidentManagementService.ArchiveIncidentResponseDocument;
import org.uicds.incidentManagementService.CloseIncidentRequestDocument;
import org.uicds.incidentManagementService.CloseIncidentResponseDocument;
import org.uicds.incidentManagementService.CreateIncidentRequestDocument;
import org.uicds.incidentManagementService.CreateIncidentResponseDocument;
import org.uicds.incidentManagementService.UpdateIncidentRequestDocument;
import org.uicds.incidentManagementService.UpdateIncidentResponseDocument;
import org.uicds.workProductService.GetAssociatedWorkProductListRequestDocument;
import org.uicds.workProductService.GetAssociatedWorkProductListResponseDocument;

import com.saic.precis.x2009.x06.base.IdentificationType;
import com.saic.precis.x2009.x06.base.ProcessingStateType;
import com.saic.precis.x2009.x06.structures.WorkProductDocument.WorkProduct;
import com.saic.uicds.clients.em.async.UicdsCore;
import com.saic.uicds.clients.em.async.UicdsIncident;
import com.saic.uicds.clients.sources.Incident;
import com.saic.uicds.clients.sources.PollingIncidentSource;
import com.saic.uicds.clients.util.CommonTestUtils;

/**
 * @author roger
 * 
 */
public class RichmondPollerClientTest {

    private RichmondPollerClient client;
    private PollingIncidentSource mockSource;
    private UicdsCore mockUicdsCore;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {

        client = new RichmondPollerClient();

        mockSource = createMock(PollingIncidentSource.class);
        client.setIncidentSource(mockSource);

        mockUicdsCore = createMock(UicdsCore.class);
        client.setUicdsCore(mockUicdsCore);
        expect(mockUicdsCore.initialize()).andReturn(true);
        replay(mockUicdsCore);

        client.initialize();

        reset(mockUicdsCore);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {

    }

    /**
     * Test method for
     * {@link com.saic.uicds.clients.em.richmond.RichmondPollerClient#pollIncidentSources()}.
     */
    @Test
    public void testIncidentHandling() {

        Incident incident = createCreatedIncident();

        setupMockCoreForNewIncident();

        client.newIncident(incident);

        Map<String, UicdsIncident> incidentMap = client.getIncidents();
        assertNotNull("Incident list is null", incidentMap);
        assertEquals("Wrong number of incidents", 1, incidentMap.size());
        setUicdsIncidentDocumentID(incidentMap);

        Map<String, String> idMapping = client.getIncidentMapping();
        assertNotNull("Incident mapping is null", idMapping);
        assertEquals("Wrong number of incident mappings", 1, idMapping.size());

        Incident updatedIncident = createUpdatedIncident();

        setupMockCoreForUpdatedIncident();

        client.updatedIncident(updatedIncident);

        incidentMap = client.getIncidents();
        assertNotNull("Incident list is null", incidentMap);
        assertEquals("Wrong number of incidents", 1, incidentMap.size());
        verify(mockUicdsCore);

        Incident deletedIncident = createDeletedIncident();

        setupMockCoreForDeletedIncident(false, false);

        client.deletedIncident(deletedIncident);
    }

    @Test
    public void testErrorDuringClose() {

        Incident incident = createCreatedIncident();

        setupMockCoreForNewIncident();

        client.newIncident(incident);

        Map<String, UicdsIncident> incidentMap = client.getIncidents();
        assertNotNull("Incident list is null", incidentMap);
        assertEquals("Wrong number of incidents", 1, incidentMap.size());
        setUicdsIncidentDocumentID(incidentMap);

        Map<String, String> idMapping = client.getIncidentMapping();
        assertNotNull("Incident mapping is null", idMapping);
        assertEquals("Wrong number of incident mappings", 1, idMapping.size());

        Incident updatedIncident = createUpdatedIncident();

        setupMockCoreForUpdatedIncident();

        client.updatedIncident(updatedIncident);

        incidentMap = client.getIncidents();
        assertNotNull("Incident list is null", incidentMap);
        assertEquals("Wrong number of incidents", 1, incidentMap.size());
        verify(mockUicdsCore);

        Incident deletedIncident = createDeletedIncident();

        setupMockCoreForDeletedIncident(true, false);

        client.deletedIncident(deletedIncident);
    }

    private void setUicdsIncidentDocumentID(Map<String, UicdsIncident> incidentMap) {

        for (String key : incidentMap.keySet()) {
            UicdsIncident i = incidentMap.get(key);
            IdentificationType id = IdentificationType.Factory.newInstance();
            id.addNewIdentifier().setStringValue("1");
            try {
                PrivateAccessor.setField(i, "incidentDocID", id);
            } catch (NoSuchFieldException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private void setupMockCoreForNewIncident() {

        CreateIncidentResponseDocument response = CreateIncidentResponseDocument.Factory.newInstance();
        response.addNewCreateIncidentResponse().addNewWorkProductPublicationResponse().addNewWorkProductProcessingStatus().setStatus(
            ProcessingStateType.ACCEPTED);
        response.getCreateIncidentResponse().getWorkProductPublicationResponse().addNewWorkProduct().set(
            CommonTestUtils.getDefaultIncidentWorkProduct());
        expect(mockUicdsCore.marshalSendAndReceive(isA((CreateIncidentRequestDocument.class)))).andReturn(
            response);
        GetAssociatedWorkProductListResponseDocument getAssociatedResponse = GetAssociatedWorkProductListResponseDocument.Factory.newInstance();
        getAssociatedResponse.addNewGetAssociatedWorkProductListResponse();
        expect(
            mockUicdsCore.marshalSendAndReceive(isA(GetAssociatedWorkProductListRequestDocument.class))).andReturn(
            getAssociatedResponse);
        replay(mockUicdsCore);
    }

    private void setupMockCoreForUpdatedIncident() {

        reset(mockUicdsCore);
        UpdateIncidentResponseDocument response = UpdateIncidentResponseDocument.Factory.newInstance();
        response.addNewUpdateIncidentResponse().addNewWorkProductPublicationResponse().addNewWorkProductProcessingStatus().setStatus(
            ProcessingStateType.PENDING);
        expect(mockUicdsCore.marshalSendAndReceive(isA((UpdateIncidentRequestDocument.class)))).andReturn(
            response);
        replay(mockUicdsCore);
    }

    private void setupMockCoreForDeletedIncident(boolean failDuringClose, boolean failDuringArchive) {

        reset(mockUicdsCore);
        // expect close
        WorkProduct workProduct = CommonTestUtils.getDefaultIncidentWorkProduct();
        expect(mockUicdsCore.getWorkProductFromCore(isA(IdentificationType.class))).andReturn(
            workProduct);
        if (failDuringClose) {
            expect(mockUicdsCore.marshalSendAndReceive(isA((CloseIncidentRequestDocument.class)))).andThrow(
                new ClassCastException("Archive failed"));
        } else {
            CloseIncidentResponseDocument closeResponse = CloseIncidentResponseDocument.Factory.newInstance();
            closeResponse.addNewCloseIncidentResponse().addNewWorkProductProcessingStatus().setStatus(
                ProcessingStateType.ACCEPTED);
            expect(mockUicdsCore.marshalSendAndReceive(isA((CloseIncidentRequestDocument.class)))).andReturn(
                closeResponse);
        }

        // expect archive
        ArchiveIncidentResponseDocument archiveResponse = ArchiveIncidentResponseDocument.Factory.newInstance();
        archiveResponse.addNewArchiveIncidentResponse().addNewWorkProductProcessingStatus().setStatus(
            ProcessingStateType.ACCEPTED);
        expect(mockUicdsCore.getWorkProductFromCore(isA(IdentificationType.class))).andReturn(
            workProduct);
        expect(mockUicdsCore.marshalSendAndReceive(isA((ArchiveIncidentRequestDocument.class)))).andReturn(
            archiveResponse);

        replay(mockUicdsCore);
    }

    private Incident createCreatedIncident() {

        Incident incident = new Incident();
        incident.setCreated(true);
        incident.setUpdated(false);
        incident.setDeleted(false);
        incident.setDateTime("datetime");
        incident.setDescription("description of incident");
        incident.setLatitude("37.0");
        incident.setLongitude("-77.0");
        incident.setName("IncidentName");
        incident.setType("fire");
        return incident;
    }

    private Incident createUpdatedIncident() {

        Incident incident = createCreatedIncident();
        incident.setCreated(false);
        incident.setUpdated(true);
        incident.setDeleted(false);
        incident.setDescription("more description of the incident");
        return incident;
    }

    private Incident createDeletedIncident() {

        Incident incident = createCreatedIncident();
        incident.setCreated(false);
        incident.setUpdated(false);
        incident.setDeleted(true);
        return incident;
    }

    /**
     * Test method for
     * {@link com.saic.uicds.clients.em.richmond.RichmondPollerClient#processCoreNotifications()}.
     */
    @Test
    public void testProcessCoreNotifications() {

        mockUicdsCore.processNotifications();
        replay(mockUicdsCore);
        client.processCoreNotifications();
        verify(mockUicdsCore);
    }

    private HashMap<String, Incident> createEmptyIncidentMap() {

        return new HashMap<String, Incident>();
    }

    private HashMap<String, Incident> createIncidentMap1() {

        HashMap<String, Incident> map = createEmptyIncidentMap();
        return map;
    }
}

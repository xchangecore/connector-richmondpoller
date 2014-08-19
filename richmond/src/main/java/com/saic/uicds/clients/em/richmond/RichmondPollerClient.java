/**
 * 
 */
package com.saic.uicds.clients.em.richmond;

import gov.niem.niem.niemCore.x20.ActivityType;
import gov.niem.niem.niemCore.x20.AddressFullTextDocument;
import gov.niem.niem.niemCore.x20.CircularRegionType;
import gov.niem.niem.niemCore.x20.LatitudeCoordinateType;
import gov.niem.niem.niemCore.x20.LengthMeasureType;
import gov.niem.niem.niemCore.x20.LongitudeCoordinateType;
import gov.niem.niem.niemCore.x20.MeasurePointValueDocument;
import gov.niem.niem.niemCore.x20.TwoDimensionalGeographicCoordinateType;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.uicds.incident.UICDSIncidentType;

import com.saic.precis.x2009.x06.base.IdentificationType;
import com.saic.precis.x2009.x06.base.ProcessingStateType;
import com.saic.precis.x2009.x06.base.ProcessingStatusType;
import com.saic.uicds.clients.em.async.UicdsCore;
import com.saic.uicds.clients.em.async.UicdsIncident;
import com.saic.uicds.clients.em.async.UicdsIncidentManager;
import com.saic.uicds.clients.sources.Incident;
import com.saic.uicds.clients.sources.IncidentSourceListener;
import com.saic.uicds.clients.sources.PollingIncidentSource;
import com.saic.uicds.clients.util.Common;

/**
 * @author roger
 */
public class RichmondPollerClient
    implements IncidentSourceListener {

    private static Logger logger = LoggerFactory.getLogger(RichmondPollerClient.class);

    private PollingIncidentSource incidentSource;

    private UicdsCore uicdsCore;

    private UicdsIncidentManager incidentManager;

    // key - UICDS incident id
    private Map<String, UicdsIncident> incidents;

    // key = Richmond incident id, value - UICDS incident id
    // this is ugly having to keep up two maps
    private Map<String, String> incidentMapping;

    private boolean runForever = false;
    private int counter = 0;
    private int runTimeInMinutes = 2;
    private long sleepDurationInSeconds = 60;

    public PollingIncidentSource getIncidentSource() {

        return incidentSource;
    }

    public void setIncidentSource(PollingIncidentSource incidentSource) {

        this.incidentSource = incidentSource;
        this.incidentSource.registerListener(this);
    }

    public UicdsCore getUicdsCore() {

        return uicdsCore;
    }

    public void setUicdsCore(UicdsCore uicdsCore) {

        this.uicdsCore = uicdsCore;
    }

    public Map<String, UicdsIncident> getIncidents() {

        return incidents;
    }

    public Map<String, String> getIncidentMapping() {

        return incidentMapping;
    }

    public void setIncidentMapping(Map<String, String> incidentMapping) {

        this.incidentMapping = incidentMapping;
    }

    public boolean isRunForever() {

        return runForever;
    }

    public void setRunForever(boolean runForever) {

        this.runForever = runForever;
    }

    public int getRunTimeInMinutes() {

        return runTimeInMinutes;
    }

    public void setRunTimeInMinutes(int runTimeInMinutes) {

        this.runTimeInMinutes = runTimeInMinutes;
    }

    /**
     * @return the sleepDurationInSeconds
     */
    public long getSleepDurationInSeconds() {

        return sleepDurationInSeconds;
    }

    /**
     * @param sleepDurationInSeconds the sleepDurationInSeconds to set
     */
    public void setSleepDurationInSeconds(long sleepDurationInSeconds) {

        this.sleepDurationInSeconds = sleepDurationInSeconds;
    }

    public UicdsIncidentManager getIncidentManager() {

        return incidentManager;
    }

    public void setIncidentManager(UicdsIncidentManager incidentManager) {

        this.incidentManager = incidentManager;
    }

    public void initialize() {

        // Initialize the connection to the core (setup in Spring context file
        // as init-method)
        if (!uicdsCore.initialize()) {
            logger.debug("Initialization failed.  Maybe profile does not exist?");
        }

        incidents = new HashMap<String, UicdsIncident>();
        incidentMapping = new HashMap<String, String>();
    }

    /**
     * poll the incident sources and process changes
     */
    public void pollIncidentSources() {

        logger.debug("... enter pollIncidentSources ...");
        incidentSource.poll();
        // printIncidents();
        logger.debug("... exit pollIncidentSources ...");
    }

    /**
     * process notifications from the core
     */
    public void processCoreNotifications() {

        logger.debug("... enter processCoreNotification ...");
        try {
            uicdsCore.processNotifications();
        } catch (Exception e) {
            logger.error("Error processing notfications: " + e.getMessage());
        }
        logger.debug("... exit processCoreNotification ...");
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        // Get the spring context and then the Poller object that was configured
        // in it
        ApplicationContext context = null;
        try {
            context = new FileSystemXmlApplicationContext("./richmond-context.xml");
            logger.debug("Using local richmond-context.xml file");
        } catch (BeansException e) {
            if (e.getCause() instanceof FileNotFoundException) {
                logger.debug("Local richmond-context.xml File not found so using file from jar");
            } else {
                logger.debug("Error reading local file context: " + e.getCause().getMessage());
            }
        }

        if (context == null) {
            context = new ClassPathXmlApplicationContext(
                new String[] { "contexts/richmond-context.xml" });
        }

        RichmondPollerClient client = (RichmondPollerClient) context.getBean("client");
        if (client == null) {
            logger.error("Could not instantiate client");
        }

        client.archiveOldIncidents();

        long sleepDurationInMilliseconds = client.getSleepDurationInSeconds() * 1000;
        if (sleepDurationInMilliseconds == 0) {
            sleepDurationInMilliseconds = 60000;
        }

        while (client.continueRunning()) {
            logger.debug("PROCESS");
            client.processCoreNotifications();
            client.pollIncidentSources();

            try {
                Thread.sleep(sleepDurationInMilliseconds);
            } catch (InterruptedException e) {
                e.getMessage();
                break;
            }
        }
    }

    private void archiveOldIncidents() {

        logger.info("Closing old Richmond Poller incidents on the core");
        HashMap<String, UicdsIncident> incidentList = incidentManager.getIncidents();
        for (String incidentID : incidentList.keySet()) {
            UicdsIncident uicdsIncident = incidentManager.getIncident(incidentID);
            ActivityType event = Common.getIncidentEventByCategoryAndReason(
                uicdsIncident.getIncidentDocument(), Constants.RICHMOND_TAG,
                Constants.RICHMOND_CREATED_REASON);
            if (event != null) {
                IdentificationType incidentIdentification = uicdsIncident.getIdentification();
                if (incidentIdentification == null
                    || incidentIdentification.getIdentifier() == null) {
                    logger.error("IncidentId: " + uicdsIncident.getIncidentID() + ", Name: "
                        + uicdsIncident.getName());
                    continue;
                }

                // TODO we still need to do something in the case the close or archive failed
                // because the incident will be left on the core

                boolean isDeleted = false;
                ProcessingStatusType closeStatus = uicdsIncident.closeIncident(incidentIdentification);
                if (closeStatus != null && closeStatus.getStatus() != null
                    && closeStatus.getStatus().equals(ProcessingStateType.ACCEPTED)) {
                    ProcessingStatusType archiveStatus = uicdsIncident.archiveIncident(incidentIdentification);
                    if (archiveStatus != null && archiveStatus.getStatus() != null
                        && archiveStatus.getStatus().equals(ProcessingStateType.ACCEPTED)) {
                        incidents.remove(uicdsIncident.getIncidentID());
                        if (event.sizeOfActivityIdentificationArray() > 0
                            && event.getActivityIdentificationArray(0).sizeOfIdentificationIDArray() > 0) {
                            incidentMapping.remove(event.getActivityIdentificationArray(0).getIdentificationIDArray(
                                0).getStringValue());
                        }
                        isDeleted = true;
                    } else {
                        if (archiveStatus == null || archiveStatus.getStatus() == null) {
                            logger.error("Archive status response was null for incident "
                                + incidentIdentification.getIdentifier().getStringValue());
                        } else {
                            logger.error("Archive request was not accepted for incident "
                                + incidentIdentification.getIdentifier().getStringValue());
                            logger.error("Archive status was " + archiveStatus);
                        }
                    }
                } else {
                    if (closeStatus == null || closeStatus.getStatus() == null) {
                        logger.error("Close status response was null for incident "
                            + incidentIdentification.getIdentifier().getStringValue());
                    } else {
                        logger.error("Close request was not accepted for incident "
                            + incidentIdentification.getIdentifier().getStringValue());
                        logger.error("Close request status was " + closeStatus);
                    }
                }

                logger.debug("delete Incident: " + uicdsIncident.getIncidentID()
                    + (isDeleted ? " success ..." : " faiure        ..."));
            }
        }

    }

    private boolean continueRunning() {

        if (runForever) {
            return true;
        } else {
            if (counter > runTimeInMinutes) {
                return false;
            } else {
                counter++;
                return true;
            }
        }
    }

    @Override
    public void newIncident(Incident incident) {

        UicdsIncident uicdsIncident = new UicdsIncident();
        uicdsIncident.setUicdsCore(uicdsCore);

        try {
            UICDSIncidentType incidentType = parseIncidentToUICDSIncidentType(incident);

            String wpid = uicdsIncident.createOnCore(incidentType);
            if (wpid != null) {
                logger.debug(incident.toString());
                incidents.put(uicdsIncident.getIncidentID(), uicdsIncident);
                incidentMapping.put(incident.getId(), uicdsIncident.getIncidentID());
            }
        } catch (Exception e) {
            logger.error("Cannot create Incident: " + incident.getId());
        }
    }

    @Override
    public void updatedIncident(Incident incident) {

        UicdsIncident uicdsIncident = findUicdsIncident(incident);
        if (uicdsIncident == null) {
            return;
        }
        try {
            UICDSIncidentType incidentType = parseIncidentToUICDSIncidentType(incident);

            ProcessingStatusType status = uicdsIncident.updateIncident(incidentType);
            logger.debug("Update Incident: "
                + incident.getId()
                + "/"
                + uicdsIncident.getIncidentID()
                + (status.getStatus().equals(ProcessingStateType.ACCEPTED)
                    ? " success ..."
                    : " failure ..."));
        } catch (Exception e) {
            logger.error("updatedIncident:[ " + incident.getId() + " ]: " + e.getMessage());
        }
    }

    @Override
    public boolean deletedIncident(Incident incident) {

        boolean isDeleted = false;

        UicdsIncident uicdsIncident = findUicdsIncident(incident);
        if (uicdsIncident != null) {
            IdentificationType incidentIdentification = uicdsIncident.getIdentification();
            if (incidentIdentification == null || incidentIdentification.getIdentifier() == null) {
                logger.error("IncidentId: " + uicdsIncident.getIncidentID() + ", Name: "
                    + uicdsIncident.getName());
                return false;
            }

            // TODO we still need to do something in the case the close or archive failed
            // because the incident will be left on the core

            ProcessingStatusType closeStatus = uicdsIncident.closeIncident(incidentIdentification);
            if (closeStatus != null && closeStatus.getStatus() != null
                && closeStatus.getStatus().equals(ProcessingStateType.ACCEPTED)) {
                ProcessingStatusType archiveStatus = uicdsIncident.archiveIncident(incidentIdentification);
                if (archiveStatus != null && archiveStatus.getStatus() != null
                    && archiveStatus.getStatus().equals(ProcessingStateType.ACCEPTED)) {
                    incidents.remove(uicdsIncident.getIncidentID());
                    incidentMapping.remove(incident.getId());
                    isDeleted = true;
                } else {
                    if (archiveStatus == null || archiveStatus.getStatus() == null) {
                        logger.error("Archive status response was null for incident "
                            + incidentIdentification.getIdentifier().getStringValue());
                    } else {
                        logger.error("Archive request was not accepted for incident "
                            + incidentIdentification.getIdentifier().getStringValue());
                        logger.error("Archive status was " + archiveStatus);
                    }
                }
            } else {
                if (closeStatus == null || closeStatus.getStatus() == null) {
                    logger.error("Close status response was null for incident "
                        + incidentIdentification.getIdentifier().getStringValue());
                } else {
                    logger.error("Close request was not accepted for incident "
                        + incidentIdentification.getIdentifier().getStringValue());
                    logger.error("Close request status was " + closeStatus);
                }
            }

            logger.debug("delete Incident: " + incident.getId() + "/"
                + uicdsIncident.getIncidentID() + (isDeleted ? " success ..." : " faiure 	..."));
        } else {
            if (incident != null) {
                logger.error("Richmond incident " + incident.getId() + " not found in map");
            } else {
                logger.error("asking to delete a null incident");
            }
        }
        return isDeleted;
    }

    private UICDSIncidentType parseIncidentToUICDSIncidentType(Incident incident) throws Exception {

        UICDSIncidentType incidentType = UICDSIncidentType.Factory.newInstance();
        incidentType.addNewActivityName().setStringValue(incident.getName());
        incidentType.addNewActivityDescriptionText().setStringValue(incident.getDescription());
        incidentType.addNewActivityCategoryText().setStringValue(incident.getType());
        incidentType.addNewActivityStatus().addNewStatusDescriptionText().setStringValue(
            incident.getActivityStatus());

        AddressFullTextDocument ad = AddressFullTextDocument.Factory.newInstance();
        ad.addNewAddressFullText().setStringValue(incident.getAddress());
        incidentType.addNewIncidentLocation().addNewLocationAddress().set(ad);

        incidentType.getIncidentLocationArray(0).addNewLocationArea().addNewAreaCircularRegion().set(
            createCircle(incident.getLatitude(), incident.getLongitude()));

        incidentType.addNewIncidentJurisdictionalOrganization().addNewOrganizationName().setStringValue(
            Constants.RICHMOND_LOCATION_STRING);

        Common.addIncidentEvent(incidentType, Constants.RICHMOND_CREATED_REASON,
            Constants.RICHMOND_TAG, incident.getId(), Constants.RICHMOND_ID);

        return incidentType;
    }

    private UicdsIncident findUicdsIncident(Incident incident) {

        if (incidentMapping.containsKey(incident.getId())) {
            if (incidents.containsKey(incidentMapping.get(incident.getId()))) {
                return incidents.get(incidentMapping.get(incident.getId()));
            }
        }
        return null;
    }

    private CircularRegionType createCircle(String latitude, String longitude) throws Exception {

        CircularRegionType circle = CircularRegionType.Factory.newInstance();

        circle.addNewCircularRegionCenterCoordinate().set(getCircleCenter(latitude, longitude));

        LengthMeasureType radius = circle.addNewCircularRegionRadiusLengthMeasure();
        MeasurePointValueDocument value = MeasurePointValueDocument.Factory.newInstance();
        value.addNewMeasurePointValue().setStringValue("0.0");
        radius.set(value);
        return circle;
    }

    private TwoDimensionalGeographicCoordinateType getCircleCenter(String latitude, String longitude)
        throws Exception {

        TwoDimensionalGeographicCoordinateType center = TwoDimensionalGeographicCoordinateType.Factory.newInstance();

        LatitudeCoordinateType latCoord = LatitudeCoordinateType.Factory.newInstance();
        try {
            String[] values = toDegMinSec(Double.parseDouble(latitude));
            latCoord.addNewLatitudeDegreeValue().setStringValue(values[0]);
            latCoord.addNewLatitudeMinuteValue().setStringValue(values[1]);
            latCoord.addNewLatitudeSecondValue().setStringValue(values[2]);
        } catch (NumberFormatException e) {
            logger.error("Error parsing latitude: " + e.getMessage());
            throw e;
        }
        center.setGeographicCoordinateLatitude(latCoord);

        LongitudeCoordinateType lonCoord = LongitudeCoordinateType.Factory.newInstance();
        try {
            String[] values = toDegMinSec(Double.parseDouble(longitude));
            lonCoord.addNewLongitudeDegreeValue().setStringValue(values[0]);
            lonCoord.addNewLongitudeMinuteValue().setStringValue(values[1]);
            lonCoord.addNewLongitudeSecondValue().setStringValue(values[2]);
        } catch (NumberFormatException e) {
            logger.error("Error parsing longitude: " + e.getMessage());
            throw e;
        }
        center.setGeographicCoordinateLongitude(lonCoord);

        return center;
    }

    private String[] toDegMinSec(double d) {

        int degrees = (int) d;
        d = Math.abs(d - degrees) * 60;
        int minutes = (int) d;
        double seconds = ((d - minutes) * 60) + 0.005;
        String[] ret = new String[3];
        ret[0] = String.valueOf(degrees);
        ret[1] = String.valueOf(minutes);
        ret[2] = String.valueOf(seconds).substring(0, 5);
        return ret;
    }

    private void printIncidents() {

        Map<String, Incident> incidents = incidentSource.getIncidents();

        if (incidents != null) {
            for (String key : incidents.keySet()) {
                logger.debug("Incident :" + key);
                Incident incident = incidents.get(key);
                logger.debug(incident.getName());
                logger.debug(incident.getType());
                logger.debug(incident.getDateTime());
                logger.debug(incident.getDescription());
                logger.debug(incident.getLatitude());
                logger.debug(incident.getLongitude());
            }
        }
    }

}

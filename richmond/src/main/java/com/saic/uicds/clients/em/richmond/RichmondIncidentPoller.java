/**
 * 
 */
package com.saic.uicds.clients.em.richmond;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.impl.values.XmlObjectBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ws.client.core.WebServiceOperations;
import org.springframework.ws.soap.client.core.SoapActionCallback;

import us.va.richmond.ci.eservices.services.publicsafety.traffic.GetCORAllDocument;
import us.va.richmond.ci.eservices.services.publicsafety.traffic.GetCORAllResponseDocument;
import us.va.richmond.ci.eservices.services.publicsafety.traffic.GetCORAllResponseDocument.GetCORAllResponse.GetCORAllResult;

import com.saic.uicds.clients.sources.Incident;
import com.saic.uicds.clients.sources.IncidentPoller;

/**
 * @author roger
 * 
 */
public class RichmondIncidentPoller
    implements IncidentPoller {

    private static Logger logger = LoggerFactory.getLogger(RichmondIncidentPoller.class);

    private Map<String, Incident> incidents = new HashMap<String, Incident>();

    private WebServiceOperations webServiceTemplate;

    private StringBuffer incidentXPath = new StringBuffer();

    private static final String MS_DIFFGR_NS = "urn:schemas-microsoft-com:xml-diffgram-v1";

    private String soapAction = null;

    private boolean isChanged = false;

    Pattern pattern = Pattern.compile("[\\w\\s]+ (\\d+):(\\d+)");

    public boolean isChanged() {

        return isChanged;
    }

    public WebServiceOperations getWebServiceTemplate() {

        return webServiceTemplate;
    }

    public void setWebServiceTemplate(WebServiceOperations webServiceTemplate) {

        this.webServiceTemplate = webServiceTemplate;
    }

    public String getSoapAction() {

        return soapAction;
    }

    public void setSoapAction(String soapAction) {

        this.soapAction = soapAction;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.uicds.clients.richmond.IncidentPoller#getIncidents()
     */
    public Map<String, Incident> getIncidents() {

        return incidents;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.uicds.clients.richmond.IncidentPoller#poll()
     */
    public void poll() {

        // Create a request
        GetCORAllDocument request = GetCORAllDocument.Factory.newInstance();
        request.addNewGetCORAll();

        GetCORAllResponseDocument response = null;
        try {
            if (soapAction != null) {
                response = (GetCORAllResponseDocument) webServiceTemplate.marshalSendAndReceive(
                    request, new SoapActionCallback(soapAction));
            } else {
                response = (GetCORAllResponseDocument) webServiceTemplate.marshalSendAndReceive(request);
            }
        } catch (Exception e) {
            logger.error("Exception processing a request to Richmond: " + e.getMessage());
            // logger.error(request.toString());
            response = null;
            isChanged = false;
        }

        // logger.debug(response);

        if (response != null) {
            parseResponseIntoIncidents(response);
            isChanged = true;
        }
    }

    /**
     * Parse a response from the Richmond incident web service into a client Incident object and add
     * to the current list of incidents
     * 
     * @param response
     */
    private void parseResponseIntoIncidents(GetCORAllResponseDocument response) {

        GetCORAllResult result = response.getGetCORAllResponse().getGetCORAllResult();

        // Lazy creation of the XPath to find the incident elements
        if (incidentXPath.length() == 0) {
            createIncidentXPath();
        }

        try {

            XmlObject[] incidentElements = result.selectPath(incidentXPath.toString());
            logger.debug("found " + incidentElements.length + " incident elements");

            HashMap<String, Incident> newIncidents = parseIncidents(incidentElements);
            logger.debug("size of NEW incidents: " + newIncidents.size());

            removeIncidents(newIncidents);

            incidents.putAll(newIncidents);

            logger.debug("size of incidents: " + incidents.size());

        } catch (RuntimeException e) {
            logger.error(e.getMessage());
        }
    }

    private HashMap<String, Incident> parseIncidents(XmlObject[] incidentElements) {

        HashMap<String, Incident> newIncidents = new HashMap<String, Incident>();

        for (XmlObject incident : incidentElements) {
            Incident i = parseResponseIncident(incident);
            String key = getIncidentKey(i);
            i.setId(key);
            Incident oldIncident = incidents.get(key);
            if (oldIncident != null) {
                if (oldIncident.isDeleted()) {
                    i.setCreated(true);
                    i.setUpdated(false);
                } else {
                    i.setCreated(false);
                    i.setUpdated(isUpdatedIncident(i, key));
                }
                i.setDeleted(false);
            } else {
                i.setCreated(true);
                i.setUpdated(false);
                i.setDeleted(false);

            }
            logger.debug("parsed incident: " + i.getId() + "/" + i.getName());
            newIncidents.put(key, i);
        }
        return newIncidents;
    }

    private String getDateString(String statusText) {

        SimpleDateFormat ISO8601Local = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        TimeZone timeZone = TimeZone.getDefault();
        ISO8601Local.setTimeZone(timeZone);
        Calendar now = Calendar.getInstance();

        Matcher matcher = pattern.matcher(statusText);
        if (matcher.find()) {
            String hr = matcher.group(1);
            String min = matcher.group(2);
            try {
                now.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hr));
                now.set(Calendar.MINUTE, Integer.parseInt(min));
                now.set(Calendar.SECOND, 0);
            } catch (NumberFormatException e) {
                logger.error("Error convering hours or minutes to integer");
            }
        }

        Calendar incidentDateTime = correctForDayChange(now);

        String dateTime = ISO8601Local.format(incidentDateTime.getTime());
        // logger.debug(dateTime);
        return dateTime;
    }

    private Calendar correctForDayChange(Calendar incidentDateTime) {

        // This won't work for incident that cover more than one day
        Calendar now = Calendar.getInstance();
        if (now.get(Calendar.AM_PM) == Calendar.AM
            && incidentDateTime.get(Calendar.AM_PM) == Calendar.PM) {
            incidentDateTime.set(Calendar.DAY_OF_MONTH,
                incidentDateTime.get(Calendar.DAY_OF_MONTH) - 1);
        }
        return incidentDateTime;
    }

    private boolean isUpdatedIncident(Incident newIncident, String key) {

        Incident currentIncident = incidents.get(key);
        if (!currentIncident.getDateTime().equals(newIncident.getDateTime())
            || !currentIncident.getDescription().equals(newIncident.getDescription())
            || !currentIncident.getLatitude().equals(newIncident.getLatitude())
            || !currentIncident.getLongitude().equals(newIncident.getLongitude())
            || !currentIncident.getName().equals(newIncident.getName())
            || !currentIncident.getType().equals(newIncident.getType())) {
            // logger.debug(currentIncident.getDateTime() + " => " +
            // newIncident.getDateTime());
            return true;
        }

        return false;
    }

    private String getIncidentKey(Incident incident) {

        return incident.getLatitude() + incident.getLongitude();
    }

    private void removeIncidents(HashMap<String, Incident> newIncidents) {

        Set<String> existedIncidentIDs = new HashSet<String>(incidents.keySet());
        Set<String> newIncidentIDs = newIncidents.keySet();

        // remove incidents marked as deleted on the last pass
        for (String key : existedIncidentIDs) {
            if (incidents.get(key).isDeleted()) {
                incidents.remove(key);
                logger.debug("\n... " + key + " deleted from hash ...");
            }
        }
        existedIncidentIDs.clear();
        existedIncidentIDs.addAll(incidents.keySet());

        // Remove all the incidents in the polled incident set,
        // then the rest of incidents shall be marked to be deleted
        for (String newIncidentID : newIncidentIDs) {
            existedIncidentIDs.remove(newIncidentID);
        }

        for (String incidentID : existedIncidentIDs) {
            Incident incident = incidents.remove(incidentID);
            incident.setCreated(false);
            incident.setUpdated(false);
            incident.setDeleted(true);
            logger.debug("\n... " + incident.getId() + " marked as deleted ...");
            incidents.put(incidentID, incident);
        }
    }

    private void createIncidentXPath() {

        // Create an XPath expression to get to the Incident elements from the
        // result
        incidentXPath = new StringBuffer();
        incidentXPath.append("declare namespace diffgr='");
        incidentXPath.append(MS_DIFFGR_NS);
        incidentXPath.append("'  diffgr:diffgram/TrafficPage/Incident");
    }

    private Incident parseResponseIncident(XmlObject incident) {

        Incident i = new Incident();

        // Use the diffgr:id attribute as the name
        // This id looks like it changes with each poll as incidents come and go
        // so don't use it
        // XmlObject item = incident.selectAttribute(MS_DIFFGR_NS, "id");
        // if (item != null) {
        // XmlCursor xc = incident.newCursor();
        // i.setName(xc.getAttributeText(new QName(MS_DIFFGR_NS, "id")));
        // xc.dispose();
        // }

        XmlObject[] items = incident.selectChildren("", "Location");
        if (items.length > 0) {
            i.setName(((XmlObjectBase) items[0]).getStringValue());
            i.setAddress(((XmlObjectBase) items[0]).getStringValue());
        }

        items = incident.selectChildren("", "Direction");
        if (items.length > 0) {
            i.setAddress(i.getAddress() + " - " + ((XmlObjectBase) items[0]).getStringValue());
        }

        items = incident.selectChildren("", "Type");
        if (items.length > 0) {
            i.setType(((XmlObjectBase) items[0]).getStringValue());
        }

        items = incident.selectChildren("", "Status");
        if (items.length > 0) {
            i.setActivityStatus(((XmlObjectBase) items[0]).getStringValue());

            // Status has a format like: "On Scene 2:57 PM"
            String dateTime = getDateString(((XmlObjectBase) items[0]).getStringValue());
            i.setDateTime(dateTime);
        }

        items = incident.selectChildren("", "Remarks");
        if (items.length > 0) {
            i.setDescription(((XmlObjectBase) items[0]).getStringValue());
        }

        items = incident.selectChildren("", "Lat");
        if (items.length > 0) {
            i.setLatitude(((XmlObjectBase) items[0]).getStringValue());
        }

        items = incident.selectChildren("", "Lon");
        if (items.length > 0) {
            i.setLongitude(((XmlObjectBase) items[0]).getStringValue());
        }

        return i;
    }
}

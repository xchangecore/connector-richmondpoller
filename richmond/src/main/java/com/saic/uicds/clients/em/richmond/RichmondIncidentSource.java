/**
 * 
 */
package com.saic.uicds.clients.em.richmond;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.saic.uicds.clients.sources.Incident;
import com.saic.uicds.clients.sources.IncidentSourceListener;
import com.saic.uicds.clients.sources.PollingIncidentSource;

/**
 * @author roger
 * 
 */
public class RichmondIncidentSource
    implements PollingIncidentSource {

    private static Logger logger = LoggerFactory.getLogger(RichmondIncidentSource.class);

    private Set<IncidentSourceListener> listeners = new HashSet<IncidentSourceListener>();

    private RichmondIncidentPoller poller;

    public RichmondIncidentPoller getPoller() {

        return poller;
    }

    public void setPoller(RichmondIncidentPoller poller) {

        this.poller = poller;
    }

    @Override
    public void poll() {

        poller.poll();
        if (poller.isChanged() == false) {
            // since there is no change, no action needed
            logger.info("the richmond poller failed to get any incident back, continue ...");
            return;
        }

        // make a copy of incidents so the modification will not throw exception
        Map<String, Incident> incidents = poller.getIncidents();

        for (String key : incidents.keySet()) {
            Incident incident = incidents.get(key);
            if (incident.isCreated()) {
                logger.debug("... try to create " + incident.getId() + "/" + incident.getName());
                notifyOfNewIncident(incident);
            }
            if (incident.isUpdated()) {
                logger.debug("... try to update " + incident.getId() + "/" + incident.getName());
                notifyOfUpdatedIncident(incident);
            }
            if (incident.isDeleted()) {
                logger.debug("... try to delete " + incident.getId() + "/" + incident.getName());
                notifyOfDeletedIncident(incident);
            }
        }
    }

    private void notifyOfNewIncident(Incident incident) {

        for (IncidentSourceListener listener : listeners) {
            listener.newIncident(incident);
        }
    }

    private void notifyOfUpdatedIncident(Incident incident) {

        for (IncidentSourceListener listener : listeners) {
            listener.updatedIncident(incident);
        }
    }

    private void notifyOfDeletedIncident(Incident incident) {

        for (IncidentSourceListener listener : listeners) {
            if (listener.deletedIncident(incident) == false) {
                logger.error("Cannot Close/Archive: " + incident.getId());
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.uicds.clients.sources.IncidentSource#registerListener(com.saic
     * .uicds.clients.sources .IncidentSourceListener)
     */
    @Override
    public void registerListener(IncidentSourceListener listener) {

        listeners.add(listener);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.uicds.clients.sources.IncidentSource#unregisterListener(com.
     * saic.uicds.clients.sources .IncidentSourceListener)
     */
    @Override
    public void unregisterListener(IncidentSourceListener listener) {

        listeners.remove(listener);
    }

    @Override
    public boolean isListening(IncidentSourceListener listener) {

        return listeners.contains(listener);
    }

    @Override
    public Map<String, Incident> getIncidents() {

        return poller.getIncidents();
    }

}

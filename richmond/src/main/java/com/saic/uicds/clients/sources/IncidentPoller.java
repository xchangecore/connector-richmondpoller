package com.saic.uicds.clients.sources;

import java.util.Map;

/**
 * @author roger
 *
 */
public interface IncidentPoller {
	/**
	 * Poll the Richmond web service that provides traffic incidents.
	 * 
	 */
	public void poll();
	
	/**
	 * Get a map of incidents that are being returned from the polled system.
	 * Incidents can be new, updated, or missing (removed) from the map.
	 * @return
	 */
	public Map<String,Incident> getIncidents();
}

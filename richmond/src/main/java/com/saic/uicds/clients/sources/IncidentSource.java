package com.saic.uicds.clients.sources;

public interface IncidentSource {

	public void registerListener(IncidentSourceListener listener);
	
	public void unregisterListener(IncidentSourceListener listener);
	
	public boolean isListening(IncidentSourceListener listener);
}

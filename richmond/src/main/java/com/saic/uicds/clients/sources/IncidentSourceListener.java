package com.saic.uicds.clients.sources;

public interface IncidentSourceListener {

    public void newIncident(Incident incident);

    public void updatedIncident(Incident incident);

    public boolean deletedIncident(Incident incident);
}

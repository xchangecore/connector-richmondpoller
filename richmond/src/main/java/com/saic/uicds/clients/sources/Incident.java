package com.saic.uicds.clients.sources;

public class Incident {

    private String id;
    private String name;
    private String dateTime;
    private String type;
    private String description;
    private String activityStatus;
    private String address;
    private String latitude = "37.33";
    private String longitude = "-77.29";
    private boolean created;
    private boolean updated;
    private boolean deleted;

    public String getId() {

        return id;
    }

    public void setId(String id) {

        this.id = id;
    }

    public String getName() {

        return name;
    }

    public void setName(String name) {

        this.name = name;
    }

    public String getDateTime() {

        return dateTime;
    }

    public void setDateTime(String dateTime) {

        this.dateTime = dateTime;
    }

    public String getType() {

        return type;
    }

    public void setType(String type) {

        this.type = type;
    }

    public String getDescription() {

        return description;
    }

    public void setDescription(String description) {

        this.description = description;
    }

    public String getActivityStatus() {

        return activityStatus;
    }

    public void setActivityStatus(String activityStatus) {

        this.activityStatus = activityStatus;
    }

    public String getAddress() {

        return address;
    }

    public void setAddress(String address) {

        this.address = address;
    }

    public String getLatitude() {

        return latitude;
    }

    public void setLatitude(String latitude) {

        this.latitude = latitude;
    }

    public String getLongitude() {

        return longitude;
    }

    public void setLongitude(String longitude) {

        this.longitude = longitude;
    }

    public boolean isCreated() {

        return created;
    }

    public void setCreated(boolean created) {

        this.created = created;
    }

    public boolean isUpdated() {

        return updated;
    }

    public void setUpdated(boolean updated) {

        this.updated = updated;
    }

    public boolean isDeleted() {

        return deleted;
    }

    public void setDeleted(boolean deleted) {

        this.deleted = deleted;
    }

    public String toString() {

        StringBuffer sb = new StringBuffer("\tIncident:\n");
        sb.append("\t");
        sb.append("Id: ");
        sb.append(id);
        sb.append("\n");
        sb.append("\t");
        sb.append("Name: ");
        sb.append(name);
        sb.append("\n");
        sb.append("\t");
        sb.append("Lat/Lon: ");
        sb.append(latitude);
        sb.append("/");
        sb.append(longitude);
        sb.append("\n");
        sb.append("\t");
        sb.append("Address: ");
        sb.append(address);
        sb.append("\n");
        sb.append("\t");
        sb.append("Timestamp: ");
        sb.append(dateTime);
        sb.append("\n");

        return sb.toString();
    }
}

package com.saic.uicds.clients.em.richmond;

import java.util.Date;
import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.saic.uicds.clients.sources.Incident;

public class SimplePoller {
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		// Get the spring context and then the Poller object that was configured in it
		ApplicationContext context = new ClassPathXmlApplicationContext(new String[] { "contexts/simple-context.xml" });
		RichmondIncidentPoller poller = (RichmondIncidentPoller) context.getBean("poller");
		if (poller == null) {
			System.err.println("Could not instantiate poller");
		}
		
//		WebServiceTemplate wst = (WebServiceTemplate)poller.getWebServiceTemplate();
//		WebServiceMessageSender[] messageSenders = wst.getMessageSenders();
//		if (messageSenders.length > 0) {
//			System.out.println("size: "+messageSenders.length);
//		}

		System.out.println("key,time,name,type,datetime,description,latitude,longitude");
		poller.poll();
		
		Map<String, Incident> incidents = poller.getIncidents();
		
		Date now = new Date();

        while (!incidents.isEmpty()) {

        	for (String key : incidents.keySet()) {
				System.out.print(key+",");
				System.out.print("\""+now+"\",");
				Incident incident = incidents.get(key);
				System.out.print('"'+incident.getName()+"\",");
				System.out.print('"'+incident.getType()+"\",");
				System.out.print('"'+incident.getDateTime()+"\",");
				System.out.print('"'+incident.getDescription()+"\",");
				System.out.print(incident.getLatitude()+",");
				System.out.println(incident.getLongitude());
			}

			try {
                Thread.sleep(60000);
            } catch (InterruptedException e) {
                e.getMessage();
                break;
            }
            
            poller.poll();
            incidents = poller.getIncidents();
            now = new Date();
		}
	}

}

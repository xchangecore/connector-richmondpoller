package com.saic.uicds.clients.em.richmond;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.apache.xmlbeans.XmlException;

import us.va.richmond.ci.eservices.services.publicsafety.traffic.GetCORAllResponseDocument;

public class RichmondIncidentTestData {

	public static GetCORAllResponseDocument getCORAllResonse1() {
		File file = new File(
				"src/test/resources/responses/GetCORAll-Response.1.xml");
		GetCORAllResponseDocument doc = null;
		try {
			doc = GetCORAllResponseDocument.Factory.parse(file);
		} catch (XmlException e) {
			fail(e.getMessage());
		} catch (IOException e) {
			fail(e.getMessage());
		}
		return doc;
	}

	public static GetCORAllResponseDocument getCORAllResonse2() {
		File file = new File(
				"src/test/resources/responses/GetCORAll-Response.2.xml");
		GetCORAllResponseDocument doc = null;
		try {
			doc = GetCORAllResponseDocument.Factory.parse(file);
		} catch (XmlException e) {
			fail(e.getMessage());
		} catch (IOException e) {
			fail(e.getMessage());
		}
		return doc;
	}

	public static GetCORAllResponseDocument getCORAllResonse3() {
		File file = new File(
				"src/test/resources/responses/GetCORAll-Response.3.xml");
		GetCORAllResponseDocument doc = null;
		try {
			doc = GetCORAllResponseDocument.Factory.parse(file);
		} catch (XmlException e) {
			fail(e.getMessage());
		} catch (IOException e) {
			fail(e.getMessage());
		}
		return doc;
	}

}

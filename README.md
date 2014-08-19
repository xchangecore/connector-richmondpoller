connector-richmondpoller
========================

This XchangeCore client code allows user to poll the traffic incidents of Richmond, VA and creates the incidents on the XchangeCore.  The information is accessible by the Public Safety's Web Services via SOAP request.

Dependencies:
connector-base-util
connector-base-async

To build this XchangeCore example client, use maven "mvn clean install":
1. Build connector-base-util
2. Build connector-base-async
3. Build richmond-xmlbeans
4. Build richmond
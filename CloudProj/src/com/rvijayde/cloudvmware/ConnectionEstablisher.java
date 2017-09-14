package com.rvijayde.cloudvmware;

import com.vmware.vim25.*;

import java.util.*;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.xml.ws.BindingProvider;

public class ConnectionEstablisher {

// Authentication is handled by using a TrustManager and supplying
// a host name verifier method.
private static class TrustAllTrustManager implements javax.net.ssl.TrustManager,
javax.net.ssl.X509TrustManager {

public java.security.cert.X509Certificate[] getAcceptedIssuers() {
return null;
}

public void checkServerTrusted(java.security.cert.X509Certificate[] certs,
String authType)
throws java.security.cert.CertificateException {
return;
}


public void checkClientTrusted(java.security.cert.X509Certificate[] certs,
String authType)
throws java.security.cert.CertificateException {
return;
}
}


public static Map<String, Integer> countersIdMapHelp(PerformanceMonitor pMon,ManagedObjectReference mRef,
		VimPortType vimPort,ServiceContent serviceContent){
	Map<String, Integer> counterIdMap = pMon.getMapObjForCtrId(mRef, vimPort, serviceContent);
			return counterIdMap;
}

public static Map<Integer, PerfCounterInfo> countersInfoMapHelp(PerformanceMonitor pMon,ManagedObjectReference mRef,
VimPortType vimPort,ServiceContent serviceContent){
	Map<Integer, PerfCounterInfo> ctInfMap = pMon.getCountersInfoMap(mRef,vimPort,serviceContent);
	return ctInfMap;
}

public static List<PerfMetricId> perfMetricIdHelp(PerformanceMonitor pMon,ManagedObjectReference mRef,
VimPortType vimPort,ServiceContent serviceContent){
	List<PerfMetricId> metricIdsPer=pMon.perfMetricIds(mRef, vimPort, serviceContent);
	return metricIdsPer;
}

public static void main(String[] args) {
try {

// Server URL and credentials.
String virtualMachineName = "CloudComputing02";
String userName = "vsphere.local\\CloudComputing";
String password = "CSE612@2017";
String url = "https://128.230.247.56/sdk";
String hostServer = "128.230.208.175";
int counter=0;
ManagedObjectReference mObjRefMain = new ManagedObjectReference();
VimService vimService;
VimPortType vimPort;
ServiceContent serviceContent;

// Declare a host name verifier that will automatically enable
// the connection. The host name verifier is invoked during
// the SSL handshake.

HostnameVerifier hNameVer = new HostnameVerifier() {
public boolean verify(String urlHostName, SSLSession session) {
return true;
}
};

// Create the trust manager.
javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[1];
javax.net.ssl.TrustManager tm = new TrustAllTrustManager();
trustAllCerts[0] = tm;

// Obtain SSL context instance 
javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("SSL");

// Obtain session context instance
javax.net.ssl.SSLSessionContext sslsc = sc.getServerSessionContext();

// Initialize the contexts; the session context takes the trust manager.
sslsc.setSessionTimeout(0);
sc.init(null, trustAllCerts, null);

// Create socket for connection
javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
// Set the default host name verifier to enable the connection.
HttpsURLConnection.setDefaultHostnameVerifier(hNameVer);

// Set up the manufactured managed object reference for the ServiceInstance
mObjRefMain.setType("ServiceInstance");
mObjRefMain.setValue("ServiceInstance");

// Create VimService instance to obtain VimPort binding provider.

vimService = new VimService();
vimPort = vimService.getVimPort();
Map<String, Object> ctxt = ((BindingProvider) vimPort).getRequestContext();


ctxt.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url);
ctxt.put(BindingProvider.SESSION_MAINTAIN_PROPERTY, true);

// Get ServiceContent instance and login
serviceContent = vimPort.retrieveServiceContent(mObjRefMain);
vimPort.login(serviceContent.getSessionManager(),
userName,
password,
null);

// Display product name, server type, and product version onto the console
System.out.println(serviceContent.getAbout().getFullName());
System.out.println("Server type is " + serviceContent.getAbout().getApiType());
System.out.println("API version is " + serviceContent.getAbout().getVersion());

PerformanceMonitor pMon=new PerformanceMonitor();

pMon.setVirtualmachinename(virtualMachineName);
ManagedObjectReference mObjRefVm=pMon.getManagedObjRefToVm(serviceContent, vimPort);

List<PerfMetricId> mMetrics = perfMetricIdHelp(pMon,mObjRefVm,vimPort,serviceContent);
Map<Integer, PerfCounterInfo> counters =countersInfoMapHelp(pMon,mObjRefVm,vimPort,serviceContent);

RetrieveOptions retrieveOptions = new RetrieveOptions();
Map<String, ManagedObjectReference> mapInst = pMon.mRefTypeInFold(serviceContent.getRootFolder(), "HostSystem", retrieveOptions,serviceContent,vimPort);

//Get ManagedObject reference for host
ManagedObjectReference hostMor = mapInst.get(hostServer);

//Monitoring performance of virtual machine and host

while (counter<5) {
	pMon.monitorPerformance(serviceContent.getPerfManager(), mObjRefVm, mMetrics, counters, vimPort,
			false);
	pMon.monitorPerformance(serviceContent.getPerfManager(), hostMor, mMetrics, counters, vimPort,
			true);
	System.out.println("Sleeping for 10 seconds...");
	Thread.sleep(10 * 1000);
	counter++;
}
// close client-server connection
vimPort.logout(serviceContent.getSessionManager());

// close the connection
} catch (Exception e) {
System.out.println(" Connection could not be established ");
e.printStackTrace();
}
}//end main()
}// end class TestClient
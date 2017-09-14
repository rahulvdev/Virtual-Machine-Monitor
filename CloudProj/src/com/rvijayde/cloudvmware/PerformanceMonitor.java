package com.rvijayde.cloudvmware;

import com.vmware.common.annotations.Option;
import com.vmware.connection.ConnectedVimServiceBase;
import com.vmware.connection.helpers.builders.ObjectSpecBuilder;
import com.vmware.connection.helpers.builders.PropertyFilterSpecBuilder;
import com.vmware.connection.helpers.builders.PropertySpecBuilder;
import com.vmware.connection.helpers.builders.TraversalSpecBuilder;
import com.vmware.vim25.*;

import javax.xml.ws.soap.SOAPFaultException;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;

public class PerformanceMonitor extends ConnectedVimServiceBase {
	private String virtualmachinename;

	@Option(name = "vmname", description = "name of the vm")
	public void setVirtualmachinename(String virtualmachinename) {
		this.virtualmachinename = virtualmachinename;
	}

	public void setServiceContentRef(ServiceContent serviceContent) {
		this.serviceContent = serviceContent;
	}

	/**
	 *
	 * @param listpfs
	 * @return list of object content
	 * @throws Exception
	 */
	List<ObjectContent> retrievePropertiesAllObjects(
			List<PropertyFilterSpec> propFilterSpecList, VimPortType vimPort,
			ServiceContent serviceContent) {

		RetrieveOptions propObjectRetrieveOpts = new RetrieveOptions();

		List<ObjectContent> listObjCont = new ArrayList<ObjectContent>();

		try {
			RetrieveResult rslts = vimPort.retrievePropertiesEx(
					serviceContent.getPropertyCollector(), propFilterSpecList,
					propObjectRetrieveOpts);
			if (rslts != null && rslts.getObjects() != null
					&& !rslts.getObjects().isEmpty()) {
				listObjCont.addAll(rslts.getObjects());
			}
			String token = null;
			if (rslts != null && rslts.getToken() != null) {
				token = rslts.getToken();
			}
			while (token != null && !token.isEmpty()) {
				rslts = vimPort.continueRetrievePropertiesEx(
						serviceContent.getPropertyCollector(), token);
				token = null;
				if (rslts != null) {
					token = rslts.getToken();
					if (rslts.getObjects() != null
							&& !rslts.getObjects().isEmpty()) {
						listObjCont.addAll(rslts.getObjects());
					}
				}
			}
		} catch (SOAPFaultException sfe) {
			printSoapFaultException(sfe);
		} catch (Exception e) {
			System.out.println(" : Could not get content");
			e.printStackTrace();
		}
		return listObjCont;
	}

	public Map<String, Integer> getMapObjForCtrId(ManagedObjectReference vmmor,
			VimPortType vimPort, ServiceContent serviceContent) {
		Map<String, Integer> countersIdMap = new HashMap<String, Integer>();

		List<PerfCounterInfo> perfCounters = getPerfCounters(serviceContent,
				vimPort);
		Map<Integer, PerfCounterInfo> countersInfoMap = getCountersInfoMap(
				vmmor, vimPort, serviceContent);
		for (PerfCounterInfo perfCounter : perfCounters) {

			Integer ctrId = new Integer(perfCounter.getKey());
			countersInfoMap.put(ctrId, perfCounter);
			String ctrGrp = perfCounter.getGroupInfo().getKey();
			String ctrName = perfCounter.getNameInfo().getKey();
			String ctrRollUpType = perfCounter.getRollupType().toString();
			String completeCountName = ctrGrp + "." + ctrName + "."
					+ ctrRollUpType;
			countersIdMap.put(completeCountName, ctrId);

		}
		return countersIdMap;
	}

	Map<Integer, PerfCounterInfo> getCountersInfoMap(
			ManagedObjectReference vmmor, VimPortType vimPort,
			ServiceContent serviceContent) {
		List<PerfCounterInfo> perfCounters = getPerfCounters(serviceContent,
				vimPort);
		/*
		 * Iterate through the PerfCounterInfo instances and load map object.
		 */

		Map<Integer, PerfCounterInfo> countersInfoMap = new HashMap<Integer, PerfCounterInfo>();
		for (PerfCounterInfo perfCount : perfCounters) {

			Integer ctrId = new Integer(perfCount.getKey());

			/*
			 * This map uses the counter ID to index performance counter
			 * metadata.
			 */
			countersInfoMap.put(ctrId, perfCount);
		}
		return countersInfoMap;
	}

	public void displayValues(List<PerfEntityMetricBase> entityVal,
			Map<Integer, PerfCounterInfo> counters, boolean serverType) {
		PrintWriter writeToTextfile = null;
		String writefile;
		if (serverType == false) {
			writefile = "VirtualMachineMetrics.txt";
			try {
				writeToTextfile = new PrintWriter(new BufferedWriter(
						new FileWriter(writefile, true)));
				writeToTextfile.println("Virtual Machine metrics/statistics");
				writeToTextfile.println();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			writefile = "HostMetrics.txt";
			try {
				writeToTextfile = new PrintWriter(new BufferedWriter(
						new FileWriter(writefile, true)));
				writeToTextfile.println("Host metrics/statistics");
				writeToTextfile.println();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		boolean flag = false;
		try {

			for (int i = 0; i < entityVal.size(); ++i) {
				String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
						.format(new java.util.Date());
				List<PerfMetricSeries> perfMetSeriesList = ((PerfEntityMetric) entityVal
						.get(i)).getValue();
				List<PerfSampleInfo> listinfo = ((PerfEntityMetric) entityVal
						.get(i)).getSampleInfo();

				System.out.println("Sample time range: "
						+ listinfo.get(0).getTimestamp().toString()
						+ " - "
						+ listinfo.get(listinfo.size() - 1).getTimestamp()
								.toString());
				for (int item = 0; item < perfMetSeriesList.size(); ++item) {
					PerfCounterInfo perfCounterInfo = counters
							.get(new Integer(perfMetSeriesList.get(item)
									.getId().getCounterId()));
					if (perfCounterInfo != null) {
						System.out.println(perfCounterInfo.getNameInfo()
								.getSummary());
						if (perfCounterInfo.getGroupInfo().getKey()
								.equals("cpu")
								&& flag == false) {
							flag = true;
							writeToTextfile.println(perfCounterInfo
									.getGroupInfo().getKey().toUpperCase()
									+ " usage in "
									+ perfCounterInfo.getUnitInfo().getKey()
									+ " at " + timeStamp);
							if (perfMetSeriesList.get(item) instanceof PerfMetricIntSeries) {
								PerfMetricIntSeries perfmetIntSeries = (PerfMetricIntSeries) perfMetSeriesList
										.get(item);
								List<Long> perfList = perfmetIntSeries
										.getValue();
								for (long x : perfList) {
									System.out.print(x + " ");
									writeToTextfile.print(x + " ");
								}
								System.out.println();
								writeToTextfile.println();
							}
						} else if (perfCounterInfo.getGroupInfo().getKey()
								.equals("mem")) {
							writeToTextfile.println(perfCounterInfo
									.getGroupInfo().getKey().toUpperCase()
									+ " usage in megabytes at " + timeStamp);
							if (perfMetSeriesList.get(item) instanceof PerfMetricIntSeries) {
								PerfMetricIntSeries perfmetIntSeries = (PerfMetricIntSeries) perfMetSeriesList
										.get(item);
								List<Long> metSeriesList = perfmetIntSeries
										.getValue();
								for (long x : metSeriesList) {
									int inVal = Math.toIntExact(x);
									int inValConvert = inVal / 1024;
									System.out.print(inValConvert + " ");
									writeToTextfile.print(inValConvert + " ");
									writeToTextfile.print("");
								}
								System.out.println();
								writeToTextfile.println();
							}
						}
						else if (perfCounterInfo.getGroupInfo().getKey()
								.equals("net")&& (serverType==true)) {
							writeToTextfile.println(perfCounterInfo
									.getGroupInfo().getKey().toUpperCase()
									+ " usage in megabytes/sec at " + timeStamp);
							if (perfMetSeriesList.get(item) instanceof PerfMetricIntSeries) {
								PerfMetricIntSeries perfmetIntSeries = (PerfMetricIntSeries) perfMetSeriesList
										.get(item);
								List<Long> metSeriesList = perfmetIntSeries
										.getValue();
								for (long x : metSeriesList) {
									double dVal=(double)x;
									double dValConvert=dVal/1024;
									writeToTextfile.print(dValConvert + " ");
									writeToTextfile.print("");
								}
								System.out.println();
								writeToTextfile.println();
							}
						}
					}

				}
				writeToTextfile.close();
			}
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	List<PerfMetricId> getPerfMetricList(ManagedObjectReference entityMor,
			Map<String, Integer> countersIdMap) {
		/*
		 * Use <group>.<name>.<ROLLUP-TYPE> path specification to identify
		 * counters.
		 */
		String[] countIdentifierArr = new String[] { "mem.consumed.AVERAGE",
				"cpu.usagemhz.AVERAGE","net.received.AVERAGE" };
		// "cpu.usagemhz.AVERAGE"

		/*
		 * Create the list of PerfMetricIds, one for each counter.
		 */
		List<PerfMetricId> perfMetricList = new ArrayList<PerfMetricId>();
		for (int i = 0; i < countIdentifierArr.length; i++) {

			/*
			 * Create the PerfMetricId object for the counterName.
			 */

			PerfMetricId perfMetIDObj = new PerfMetricId();
			/* Get the ID for this counter. */
			perfMetIDObj.setCounterId(countersIdMap.get(countIdentifierArr[i]));
			perfMetIDObj.setInstance("*");
			perfMetricList.add(perfMetIDObj);

		}
		/*
		 * Create the query specification for queryPerf(). Specify 5 minute
		 * rollup interval and CSV output format.
		 */
		int intervalId = 300;
		PerfQuerySpec querySpecification = new PerfQuerySpec();
		querySpecification.setEntity(entityMor);
		querySpecification.setIntervalId(intervalId);
		querySpecification.setFormat("csv");
		querySpecification.getMetricId().addAll(perfMetricList);

		List<PerfQuerySpec> pqsList = new ArrayList<PerfQuerySpec>();
		pqsList.add(querySpecification);

		return perfMetricList;
	}

	/**
	 * This method initializes all the performance counters available on the
	 * system it is connected to. The performance counters are stored in the
	 * hashmap counters with group.counter.rolluptype being the key and id being
	 * the value.
	 */

	List<PerfCounterInfo> getPerfCounters(ServiceContent serviceContent,
			VimPortType vimPort) {
		/*
		 * Create object specification to define context to obtain the
		 * PerformanceManager property.
		 */
		ObjectSpec oSpec = new ObjectSpec();
		oSpec.setObj(serviceContent.getPerfManager());
		/*
		 * Specify the property for retrieval (PerformanceManager.perfCounter is
		 * the list of counters of which the vCenter Server is aware.)
		 */
		PropertySpec pSpec = new PropertySpec();
		pSpec.setType("PerformanceManager");
		pSpec.getPathSet().add("perfCounter");
		/*
		 * Create a PropertyFilterSpec and add the object and property specs to
		 * it.
		 */
		PropertyFilterSpec fSpec = new PropertyFilterSpec();
		fSpec.getObjectSet().add(oSpec);
		fSpec.getPropSet().add(pSpec);
		/*
		 * Create a list for the filter and add specification.
		 */
		List<PropertyFilterSpec> fSpecList = new ArrayList<PropertyFilterSpec>();
		fSpecList.add(fSpec);
		List<PerfCounterInfo> perfCounters = new ArrayList<PerfCounterInfo>();
		try {
			/*
			 * Get performance counters from the server.
			 */
			RetrieveOptions ro = new RetrieveOptions();
			RetrieveResult props;

			props = vimPort.retrievePropertiesEx(
					serviceContent.getPropertyCollector(), fSpecList, ro);

			/*
			 * Turn the retrieved results into an array of PerfCounterInfo.
			 */

			if (props != null) {
				for (ObjectContent oc : props.getObjects()) {
					List<DynamicProperty> dps = oc.getPropSet();
					if (dps != null) {
						for (DynamicProperty dp : dps) {
							/*
							 * DynamicProperty.val is an xsd:anyType value to be
							 * cast to an ArrayOfPerfCounterInfo and assigned to
							 * a List<PerfCounterInfo>.
							 */
							perfCounters = ((ArrayOfPerfCounterInfo) dp
									.getVal()).getPerfCounterInfo();
						}
					}
				}
			}
		} catch (InvalidPropertyFaultMsg e) {
			e.printStackTrace();
		} catch (RuntimeFaultFaultMsg e) {
			e.printStackTrace();
		}
		return perfCounters;
	}

	public Map<String, ManagedObjectReference> mRefTypeInFold(
			final ManagedObjectReference folder, final String morefType,
			final RetrieveOptions retrieveOptions,
			ServiceContent serviceContent, VimPortType vimPort)
			throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg {
		final PropertyFilterSpec[] propFilterSpec = propertyFilterSpecs(folder,
				morefType, serviceContent, vimPort, "name");
		
		//property collector is used to iterate through results
		final ManagedObjectReference propertyCollector = serviceContent
				.getPropertyCollector();

		RetrieveResult results = vimPort.retrievePropertiesEx(
				propertyCollector, Arrays.asList(propFilterSpec),
				retrieveOptions);

		final Map<String, ManagedObjectReference> mRefReq = new HashMap<String, ManagedObjectReference>();
		while (results != null && !results.getObjects().isEmpty()) {
			assignResToMrefReq(results, mRefReq);
			final String token = results.getToken();
			results = (token != null) ? vimPort.continueRetrievePropertiesEx(
					propertyCollector, token) : null;
		}
		return mRefReq;
	}

	public PropertyFilterSpec[] propertyFilterSpecs(
			ManagedObjectReference container, String morefType,
			ServiceContent serviceContent, VimPortType vimPort,
			String... morefProperties) throws RuntimeFaultFaultMsg {
		ManagedObjectReference vwMan = serviceContent.getViewManager();
		ManagedObjectReference containerView = vimPort.createContainerView(
				vwMan, container, Arrays.asList(morefType), true);

		return new PropertyFilterSpec[] { new PropertyFilterSpecBuilder()
				.propSet(
						new PropertySpecBuilder().all(Boolean.FALSE)
								.type(morefType).pathSet(morefProperties))
				.objectSet(
						new ObjectSpecBuilder()
								.obj(containerView)
								.skip(Boolean.TRUE)
								.selectSet(
										new TraversalSpecBuilder().name("view")
												.path("view").skip(false)
												.type("ContainerView"))) };
	}

	private void assignResToMrefReq(RetrieveResult results,
			Map<String, ManagedObjectReference> mRefReq) {
		List<ObjectContent> objCount = (results != null) ? results.getObjects()
				: null;

		if (objCount != null) {
			for (ObjectContent objCon : objCount) {
				ManagedObjectReference mRef = objCon.getObj();
				String entityNm = null;
				List<DynamicProperty> dPropList = objCon.getPropSet();
				if (dPropList != null) {
					for (DynamicProperty dProp : dPropList) {
						entityNm = (String) dProp.getVal();
					}
				}
				mRefReq.put(entityNm, mRef);
			}
		}
	}

	public ManagedObjectReference getManagedObjRefToVm(
			ServiceContent serviceContent, VimPortType vimPort) {
		ManagedObjectReference retVal = null;
		ManagedObjectReference rootFolder = serviceContent.getRootFolder();
		try {
			TraversalSpec tSpec = getVirtuLMachineTravSpecification();
			// Property Specification Instance
			PropertySpec propertySpec = new PropertySpec();
			propertySpec.setAll(Boolean.FALSE);
			propertySpec.getPathSet().add("name");
			propertySpec.setType("VirtualMachine");

			// Object Specification Instance
			ObjectSpec objSpec = new ObjectSpec();
			objSpec.setObj(rootFolder);
			objSpec.setSkip(Boolean.TRUE);
			objSpec.getSelectSet().add(tSpec);

			// Create PropertyFilterSpec using the PropertySpec and ObjectPec
			// created above.
			PropertyFilterSpec propertyFilterSpec = new PropertyFilterSpec();
			propertyFilterSpec.getPropSet().add(propertySpec);
			propertyFilterSpec.getObjectSet().add(objSpec);

			List<PropertyFilterSpec> listpfs = new ArrayList<PropertyFilterSpec>(
					1);
			listpfs.add(propertyFilterSpec);
			List<ObjectContent> listobjcont = retrievePropertiesAllObjects(
					listpfs, vimPort, serviceContent);

			if (listobjcont != null) {
				for (ObjectContent objCon : listobjcont) {
					ManagedObjectReference mObjRef = objCon.getObj();
					String vmnm = null;
					List<DynamicProperty> dps = objCon.getPropSet();
					if (dps != null) {
						for (DynamicProperty dProp : dps) {
							vmnm = (String) dProp.getVal();
						}
					}
					if (vmnm != null && vmnm.equals(virtualmachinename)) {
						retVal = mObjRef;
						break;
					}
				}
			}
		} catch (SOAPFaultException sfe) {
			printSoapFaultException(sfe);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return retVal;
	}

	public TraversalSpec getVirtuLMachineTravSpecification() {

		// Traversal to get to the Virtual Machine in a VApp
		TraversalSpec vAppToVM = new TraversalSpec();
		vAppToVM.setName("vAppToVM");
		vAppToVM.setType("VirtualApp");
		vAppToVM.setPath("vm");

		// Traversal specification for VApp to VApp
		TraversalSpec vAppToVApp = new TraversalSpec();
		vAppToVApp.setName("vAppToVApp");
		vAppToVApp.setType("VirtualApp");
		vAppToVApp.setPath("resourcePool");
		// SelectionSpec for VApp to VApp recursion
		SelectionSpec vAppRecursion = new SelectionSpec();
		vAppRecursion.setName("vAppToVApp");
		// SelectionSpec to obtain virtual machine in the VApp
		SelectionSpec vmInVApp = new SelectionSpec();
		vmInVApp.setName("vAppToVM");
		// SelectionSpec for both VApp to VApp and VApp to Virtual Machine
		List<SelectionSpec> vAppToVMSS = new ArrayList<SelectionSpec>();
		vAppToVMSS.add(vAppRecursion);
		vAppToVMSS.add(vmInVApp);
		vAppToVApp.getSelectSet().addAll(vAppToVMSS);

		// This SelectionSpec is used for recursion for Folder recursion
		SelectionSpec sSpec = new SelectionSpec();
		sSpec.setName("VisitFolders");

		// Traversal to get to the vmFolder from DataCenter
		TraversalSpec dataCenterToVMFolder = new TraversalSpec();
		dataCenterToVMFolder.setName("DataCenterToVMFolder");
		dataCenterToVMFolder.setType("Datacenter");
		dataCenterToVMFolder.setPath("vmFolder");
		dataCenterToVMFolder.setSkip(false);
		dataCenterToVMFolder.getSelectSet().add(sSpec);

		// TraversalSpec to get to the DataCenter from rootFolder
		TraversalSpec trSpec = new TraversalSpec();
		trSpec.setName("VisitFolders");
		trSpec.setType("Folder");
		trSpec.setPath("childEntity");
		trSpec.setSkip(false);
		List<SelectionSpec> sSpecArr = new ArrayList<SelectionSpec>();
		sSpecArr.add(sSpec);
		sSpecArr.add(dataCenterToVMFolder);
		sSpecArr.add(vAppToVM);
		sSpecArr.add(vAppToVApp);
		trSpec.getSelectSet().addAll(sSpecArr);
		return trSpec;
	}

	List<PerfMetricId> perfMetricIds(ManagedObjectReference vmmor,
			VimPortType vimPort, ServiceContent serviceContent) {

		Map<String, Integer> ctrIdMap = getMapObjForCtrId(vmmor, vimPort,
				serviceContent);
		String[] counterNames = new String[] { "mem.consumed.AVERAGE",
				"cpu.usagemhz.AVERAGE","net.received.AVERAGE" };
		List<PerfMetricId> perfStatId = new ArrayList<PerfMetricId>();
		for (int i = 0; i < counterNames.length; i++) {
			PerfMetricId statId = new PerfMetricId();
			/* Get the ID for this counter. */
			statId.setCounterId(ctrIdMap.get(counterNames[i]));
			statId.setInstance("*");
			perfStatId.add(statId);

		}
		int intervalId = 300;
		PerfQuerySpec querySpec = new PerfQuerySpec();
		querySpec.setEntity(vmmor);
		querySpec.setIntervalId(intervalId);
		querySpec.setMaxSample(1);
		querySpec.setFormat("csv");
		querySpec.getMetricId().addAll(perfStatId);
		List<PerfQuerySpec> perfQuerySpecList = new ArrayList<PerfQuerySpec>();
		perfQuerySpecList.add(querySpec);
		return perfStatId;
	}

	public void monitorPerformance(ManagedObjectReference pmRef,
			ManagedObjectReference vmRef, List<PerfMetricId> mMetrics,
			Map<Integer, PerfCounterInfo> counters, VimPortType vimPort,
			boolean MachineType) throws RuntimeFaultFaultMsg,
			InterruptedException {
		PerfQuerySpec perfQuerySpec = new PerfQuerySpec();
		perfQuerySpec.setEntity(vmRef);
		perfQuerySpec.setMaxSample(new Integer(10));
		perfQuerySpec.getMetricId().addAll(mMetrics);
		perfQuerySpec.setIntervalId(new Integer(20));

		List<PerfQuerySpec> querySpec = new ArrayList<PerfQuerySpec>();
		querySpec.add(perfQuerySpec);
		List<PerfEntityMetricBase> listpemb = vimPort.queryPerf(pmRef,
				querySpec);
		List<PerfEntityMetricBase> pValues = listpemb;
		if (pValues != null) {
			displayValues(pValues, counters, MachineType);
		}
	}

	void printSoapFaultException(SOAPFaultException sfe) {
		System.out.println("SOAP Fault -");
		if (sfe.getFault().hasDetail()) {
			System.out.println(sfe.getFault().getDetail().getFirstChild()
					.getLocalName());
		}
		if (sfe.getFault().getFaultString() != null) {
			System.out
					.println("\n Message: " + sfe.getFault().getFaultString());
		}
	}
}

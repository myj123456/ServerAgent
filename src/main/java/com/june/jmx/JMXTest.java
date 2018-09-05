package com.june.jmx;

import java.lang.management.MemoryUsage;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

public class JMXTest {

	private static Formatter format = new Formatter();

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {

			String jmxURL = "service:jmx:rmi:///jndi/rmi://10.50.200.38:4711/jmxrmi";

			JMXServiceURL serviceURL = new JMXServiceURL(jmxURL);

			Map<String, String[]> map = new HashMap<String, String[]>();
			String[] credentials = new String[] { "monitorRole", "tomcat" };
			map.put("jmx.remote.credentials", credentials);
			// JMXConnector connector = JMXConnectorFactory.connect(serviceURL,
			// map);
			JMXConnector connector = JMXConnectorFactory.connect(serviceURL, null);

			MBeanServerConnection mbsc = connector.getMBeanServerConnection();

			// 端口最好是动态取得
			ObjectName grpObjName = new ObjectName("Catalina:type=GlobalRequestProcessor,*");
			Set<ObjectName> grps = mbsc.queryNames(grpObjName, null);
			for (ObjectName obj : grps) {
				System.out.println("名称:" + obj.getKeyProperty("name"));
				ObjectName objname = new ObjectName(obj.getCanonicalName());
				System.out.println("bytesReceived:" + mbsc.getAttribute(objname, "bytesReceived"));
				System.out.println("bytesSent:" + mbsc.getAttribute(objname, "bytesSent"));
				System.out.println("requestCount:" + mbsc.getAttribute(objname, "requestCount"));
			}

			ObjectName threadObjName = new ObjectName("Catalina:type=ThreadPool,name=\"*http*\"");
			Set<ObjectName> smbi = mbsc.queryNames(threadObjName, null);
			for (ObjectName obj : smbi) {
				System.out.println("端口名:" + obj.getKeyProperty("name"));
				ObjectName objname = new ObjectName(obj.getCanonicalName());
				System.out.println("最大线程数:" + mbsc.getAttribute(objname, "maxThreads"));
				System.out.println("当前线程数:" + mbsc.getAttribute(objname, "currentThreadCount"));
				System.out.println("繁忙线程数:" + mbsc.getAttribute(objname, "currentThreadsBusy"));
			}

			// ObjectName threadObjName1 = new
			// ObjectName("Catalina:type=ThreadPool,name=\"http-apr-8080\"");
			// MBeanInfo mbInfo = mbsc.getMBeanInfo(threadObjName1);
			//
			// String attrName = "currentThreadCount";// tomcat的线程数对应的属性值
			// MBeanAttributeInfo[] mbAttributes = mbInfo.getAttributes();
			// System.out.println("currentThreadCount:" +
			// mbsc.getAttribute(threadObjName1, attrName));

			// heap
			for (int j = 0; j < mbsc.getDomains().length; j++) {
				System.out.println("###########" + mbsc.getDomains()[j]);
			}

			Set<?> MBeanset = mbsc.queryMBeans(null, null);
			System.out.println("MBeanset.size() : " + MBeanset.size());
			Iterator<?> MBeansetIterator = MBeanset.iterator();
			while (MBeansetIterator.hasNext()) {
				ObjectInstance objectInstance = (ObjectInstance) MBeansetIterator.next();
				ObjectName objectName = objectInstance.getObjectName();
				String canonicalName = objectName.getCanonicalName();
				System.out.println("canonicalName : " + canonicalName);
				if (canonicalName.equals("Catalina:host=localhost,type=Cluster")) {
					// Get details of cluster MBeans
					System.out.println("Cluster MBeans Details:");
					System.out.println("=========================================");
					// getMBeansDetails(canonicalName);
					String canonicalKeyPropList = objectName.getCanonicalKeyPropertyListString();
					System.out.println(canonicalKeyPropList);
				}
			}
			// ------------------------ system ----------------------
			ObjectName runtimeObjName = new ObjectName("java.lang:type=Runtime");
			System.out.println("厂商:" + (String) mbsc.getAttribute(runtimeObjName, "VmVendor"));
			System.out.println("程序:" + (String) mbsc.getAttribute(runtimeObjName, "VmName"));
			System.out.println("版本:" + (String) mbsc.getAttribute(runtimeObjName, "VmVersion"));
			Date starttime = new Date((Long) mbsc.getAttribute(runtimeObjName, "StartTime"));
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			System.out.println("启动时间:" + df.format(starttime));

			Long timespan = (Long) mbsc.getAttribute(runtimeObjName, "Uptime");
			System.out.println("连续工作时间:" + JMXTest.formatTimeSpan(timespan));

			// ----------------------- JVM -------------------------
			// 堆使用率
			ObjectName heapObjName = new ObjectName("java.lang:type=Memory");
			MemoryUsage heapMemoryUsage = MemoryUsage
					.from((CompositeDataSupport) mbsc.getAttribute(heapObjName, "HeapMemoryUsage"));
			long maxMemory = heapMemoryUsage.getMax();// 堆最大
			System.out.println("heap size:" + maxMemory);
			long commitMemory = heapMemoryUsage.getCommitted();// 堆当前分配
			long usedMemory = heapMemoryUsage.getUsed();
			System.out.println("heap:" + (double) usedMemory * 100 / commitMemory + "%");// 堆使用率

			MemoryUsage nonheapMemoryUsage = MemoryUsage
					.from((CompositeDataSupport) mbsc.getAttribute(heapObjName, "NonHeapMemoryUsage"));
			long noncommitMemory = nonheapMemoryUsage.getCommitted();
			long nonusedMemory = heapMemoryUsage.getUsed();
			System.out.println("nonheap:" + (double) nonusedMemory * 100 / noncommitMemory + "%");

			ObjectName permObjName = new ObjectName("java.lang:type=MemoryPool,name=*Perm*Gen*");
			Set<ObjectName> sperm = mbsc.queryNames(permObjName, null);
			for (ObjectName obj : sperm) {
				ObjectName objname = new ObjectName(obj.getCanonicalName());
				MemoryUsage permGenUsage = MemoryUsage.from((CompositeDataSupport) mbsc.getAttribute(objname, "Usage"));
				long committed = permGenUsage.getCommitted();// 持久堆大小
				long used = heapMemoryUsage.getUsed();//
				System.out.println("perm gen:" + (double) used * 100 / committed + "%");// 持久堆使用率
			}

			// ------------------- Session ---------------
			ObjectName managerObjName = new ObjectName("Catalina:type=Manager,*");
			Set<ObjectName> s = mbsc.queryNames(managerObjName, null);
			for (ObjectName obj : s) {
				System.out.println("应用名:" + obj.getKeyProperty("path"));
				ObjectName objname = new ObjectName(obj.getCanonicalName());
				System.out.println("最大会话数:" + mbsc.getAttribute(objname, "maxActiveSessions"));
				System.out.println("会话数:" + mbsc.getAttribute(objname, "activeSessions"));
				System.out.println("活动会话数:" + mbsc.getAttribute(objname, "sessionCounter"));
			}

			// ---------------- Thread Pool ----------------
			ObjectName threadpoolObjName = new ObjectName("Catalina:type=ThreadPool,*");
			Set<ObjectName> s2 = mbsc.queryNames(threadpoolObjName, null);
			for (ObjectName obj : s2) {
				System.out.println("端口名:" + obj.getKeyProperty("name"));
				ObjectName objname = new ObjectName(obj.getCanonicalName());
				System.out.println("最大线程数:" + mbsc.getAttribute(objname, "maxThreads"));
				System.out.println("当前线程数:" + mbsc.getAttribute(objname, "currentThreadCount"));
				System.out.println("繁忙线程数:" + mbsc.getAttribute(objname, "currentThreadsBusy"));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String formatTimeSpan(long span) {
		long minseconds = span % 1000;

		span = span / 1000;
		long seconds = span % 60;

		span = span / 60;
		long mins = span % 60;

		span = span / 60;
		long hours = span % 24;

		span = span / 24;
		long days = span;
		return format.format("%1$d天 %2$02d:%3$02d:%4$02d.%5$03d", days, hours, mins, seconds, minseconds).toString();
	}
}
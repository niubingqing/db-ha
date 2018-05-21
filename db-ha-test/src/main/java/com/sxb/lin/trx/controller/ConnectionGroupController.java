package com.sxb.lin.trx.controller;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Controller;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidPooledConnection;
import com.alibaba.druid.proxy.jdbc.ConnectionProxy;
import com.google.gson.Gson;
import com.mysql.jdbc.ConnectionGroupManager;
import com.mysql.jdbc.ConnectionImpl;
import com.mysql.jdbc.LoadBalancedConnectionProxy;
import com.mysql.jdbc.jmx.LoadBalanceConnectionGroupManagerMBean;
import com.sxb.web.commons.util.RetUtil;

@Controller
@RequestMapping("/transaction/ConnectionGroup")
public class ConnectionGroupController {
	
	//@Resource(name="dataSource_r")
	private DruidDataSource druidDataSource;
	
	private List<DruidPooledConnection> druidPooledConnections = new CopyOnWriteArrayList<DruidPooledConnection>();

	class LoadBalanceConnectionGroupManager implements LoadBalanceConnectionGroupManagerMBean {

		@Override
		public int getActiveHostCount(String group) {
			return ConnectionGroupManager.getActiveHostCount(group);
		}

		@Override
		public int getTotalHostCount(String group) {
			return ConnectionGroupManager.getTotalHostCount(group);
		}

		@Override
		public long getTotalLogicalConnectionCount(String group) {
			return ConnectionGroupManager.getTotalLogicalConnectionCount(group);
		}

		@Override
		public long getActiveLogicalConnectionCount(String group) {
			return ConnectionGroupManager.getActiveLogicalConnectionCount(group);
		}

		@Override
		public long getActivePhysicalConnectionCount(String group) {
			return ConnectionGroupManager.getActivePhysicalConnectionCount(group);
		}

		@Override
		public long getTotalPhysicalConnectionCount(String group) {
			return ConnectionGroupManager.getTotalPhysicalConnectionCount(group);
		}

		@Override
		public long getTotalTransactionCount(String group) {
			return ConnectionGroupManager.getTotalTransactionCount(group);
		}

		@Override
		public void removeHost(String group, String host) throws SQLException {
			ConnectionGroupManager.removeHost(group, host, true);
		}

		@Override
		public void stopNewConnectionsToHost(String group, String host)
				throws SQLException {
			ConnectionGroupManager.removeHost(group, host);
		}

		@Override
		public void addHost(String group, String host, boolean forExisting) {
			try {
	            ConnectionGroupManager.addHost(group, host, forExisting);
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
		}

		@Override
		public String getActiveHostsList(String group) {
			return ConnectionGroupManager.getActiveHostLists(group);
		}

		@Override
		public String getRegisteredConnectionGroups() {
			return ConnectionGroupManager.getRegisteredConnectionGroups();
		}
		
	}
	
	@RequestMapping(value="/getLoadBalanceInfo.json")
	@ResponseBody
	public String getLoadBalanceInfo(){
		String group = "first";
		LoadBalanceConnectionGroupManager manager = new LoadBalanceConnectionGroupManager();
		Map<String,Object> resMap = new HashMap<String,Object>();
		resMap.put("TotalHostCount", manager.getTotalHostCount(group));
		resMap.put("TotalLogicalConnectionCount", manager.getTotalLogicalConnectionCount(group));
		resMap.put("TotalPhysicalConnectionCount", manager.getTotalPhysicalConnectionCount(group));
		resMap.put("ActiveHostCount", manager.getActiveHostCount(group));
		resMap.put("ActiveLogicalConnectionCount", manager.getActiveLogicalConnectionCount(group));
		resMap.put("ActivePhysicalConnectionCount", manager.getActivePhysicalConnectionCount(group));
		resMap.put("TotalTransactionCount", manager.getTotalTransactionCount(group));
		resMap.put("ActiveHostsList", manager.getActiveHostsList(group));
		resMap.put("RegisteredConnectionGroups", manager.getRegisteredConnectionGroups());
		return new Gson().toJson(resMap);
	}
	
	@RequestMapping(value="/removeHost.json")
	@ResponseBody
	public String removeHost(String host){
		String group = "first";
		LoadBalanceConnectionGroupManager manager = new LoadBalanceConnectionGroupManager();
		try {
			if(host == null){
				manager.removeHost(group, "localhost:3306");
			}else{
				manager.removeHost(group, host);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return new Gson().toJson(RetUtil.getRetValue(true));
	}
	
	@RequestMapping(value="/stopNewConnectionsToHost.json")
	@ResponseBody
	public String stopNewConnectionsToHost(String host){
		String group = "first";
		LoadBalanceConnectionGroupManager manager = new LoadBalanceConnectionGroupManager();
		try {
			if(host == null){
				manager.stopNewConnectionsToHost(group, "localhost:3306");
			}else{
				manager.stopNewConnectionsToHost(group, host);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return new Gson().toJson(RetUtil.getRetValue(true));
	}
	
	@RequestMapping(value="/addExistHost.json")
	@ResponseBody
	public String addExistHost(){
		String group = "first";
		LoadBalanceConnectionGroupManager manager = new LoadBalanceConnectionGroupManager();
		manager.addHost(group, "localhost:3306", true);
		return new Gson().toJson(RetUtil.getRetValue(true));
	}
	
	@RequestMapping(value="/addHost.json")
	@ResponseBody
	public String addHost(){
		String group = "first";
		LoadBalanceConnectionGroupManager manager = new LoadBalanceConnectionGroupManager();
		manager.addHost(group, "localhost:3306", false);
		return new Gson().toJson(RetUtil.getRetValue(true));
	}
	
	@RequestMapping(value="/checkConnection.json")
	@ResponseBody
	public String checkConnection() throws IllegalArgumentException, IllegalAccessException, SQLException{
		
		int poolingCount = druidDataSource.getPoolingCount();
		for(int i=0;i<poolingCount;i++){
			DruidPooledConnection druidPooledConnection = druidDataSource.getConnection();
			druidPooledConnections.add(druidPooledConnection);
			ConnectionProxy connectionProxy = (ConnectionProxy) druidPooledConnection.getConnection();
			LoadBalancedConnectionProxy loadBalancedConnectionProxy = (LoadBalancedConnectionProxy) Proxy.getInvocationHandler(connectionProxy.getRawObject());
			Field liveConnectionsField = ReflectionUtils.findField(LoadBalancedConnectionProxy.class, "liveConnections");
			liveConnectionsField.setAccessible(true);
			@SuppressWarnings("unchecked")
			Map<String, ConnectionImpl> liveConnections = (Map<String, ConnectionImpl>) liveConnectionsField.get(loadBalancedConnectionProxy);
			System.out.println(liveConnections);
			
//			Field hostListField = ReflectionUtils.findField(LoadBalancedConnectionProxy.class, "hostList");
//			hostListField.setAccessible(true);
//			@SuppressWarnings("unchecked")
//			List<String> hostList = (List<String>) hostListField.get(loadBalancedConnectionProxy);
//			System.out.println(hostList);
		}
		
		return new Gson().toJson(RetUtil.getRetValue(true));
	}
	
	@RequestMapping(value="/closeConnection.json")
	@ResponseBody
	public String closeConnection() throws SQLException {
		
		for(DruidPooledConnection druidPooledConnection : druidPooledConnections){
			druidPooledConnection.close();
		}
		
		return new Gson().toJson(RetUtil.getRetValue(true));
	}
	
	@RequestMapping(value="/getLastConnection.json")
	@ResponseBody
	public String getLastConnection() throws SQLException {
		
		DruidPooledConnection druidPooledConnection = druidDataSource.getConnection();
		DatabaseMetaData metaData = druidPooledConnection.getMetaData();
		druidPooledConnections.add(druidPooledConnection);
		
		return new Gson().toJson(RetUtil.getRetValue(metaData.getURL()));
	}
	
	@RequestMapping(value="/getConnectionInfo.json")
	@ResponseBody
	public String getConnectionInfo() throws SQLException {
		
		DruidPooledConnection druidPooledConnection = druidDataSource.getConnection();
		Properties clientInfo = druidPooledConnection.getClientInfo();
		String catalog = druidPooledConnection.getCatalog();
		DatabaseMetaData metaData = druidPooledConnection.getMetaData();
		
		System.out.println(clientInfo);
		System.out.println(catalog);
		System.out.println(metaData.getURL());
		
		druidPooledConnection.getConnection().close();
		
		druidPooledConnection.close();
		
		return new Gson().toJson(RetUtil.getRetValue(true));
	}
}

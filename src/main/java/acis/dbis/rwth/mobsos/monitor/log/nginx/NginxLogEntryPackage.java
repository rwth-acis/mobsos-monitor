package acis.dbis.rwth.mobsos.monitor.log.nginx;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;

import acis.dbis.rwth.mobsos.monitor.Monitor;
import acis.dbis.rwth.mobsos.monitor.log.LogEntryPackage;

public class NginxLogEntryPackage extends LogEntryPackage{
	
	private CSVRecord record;

	private NginxRequestLogEntry request;
	private NginxRequestQueryLogEntry query;
	private NginxRequestHeadersLogEntry headers;

	public NginxLogEntryPackage(CSVRecord data){
		super();
		this.record = data;
	}

	private void parseData(){

		// init POJOs
		request = new NginxRequestLogEntry(this);
		query = new NginxRequestQueryLogEntry(this);
		headers = new NginxRequestHeadersLogEntry(this);

		// extract POJO data from CSV record

		// first for main request record
		request.setTime(record.get(0));
		request.setIp(record.get(1));
		request.setScheme(record.get(2));
		request.setHost(record.get(3));
		request.setMethod(record.get(4));
		request.setUri(record.get(5));
		request.setStatus(record.get(6));
		request.setReferer(record.get(7));
		request.setUserAgent(record.get(8));
		request.setAccept(record.get(9));
		request.setReceivedContent(record.get(10));
		request.setSentContent(record.get(11));

		request.setRequestLength(record.get(12));
		request.setResponseLength(record.get(13));
		request.setRequestTime(record.get(14));

		/*
		Monitor.log.info("Request Time: " + request.getTime());
		Monitor.log.info("Request IP: " + request.getIp());
		Monitor.log.info("Request Scheme: " + request.getScheme());
		Monitor.log.info("Request Host: " + request.getHost());
		Monitor.log.info("Request Method: " + request.getMethod());
		Monitor.log.info("Request URI: " + request.getUri());
		Monitor.log.info("Status: " + request.getStatus());
		Monitor.log.info("Referer: " + request.getReferer());
		Monitor.log.info("Request User Agent: " + request.getUserAgent());
		Monitor.log.info("Request Accept: " + request.getAccept());
		Monitor.log.info("Received Content MIME: " + request.getReceivedContent());
		Monitor.log.info("Served Content MIME: " + request.getSentContent());
		Monitor.log.info("Request Length: " + request.getRequestLength());
		Monitor.log.info("Response Length: " + request.getResponseLength());
		Monitor.log.info("Request Processing Time: " + request.getRequestTime());
		*/
		
		// parse query parameters and put into hashtable
		// TODO:
		manageIpGeo();
		
		// parse header parameters and put into hashtable
		// TODO:
	}
	
	private void manageIpGeo(){
		try {
			PreparedStatement p = getWorker().getConnection().prepareStatement("select * from mobsos_logs.log_ipgeo where ip=?");
			p.setString(1, request.getIp());
			ResultSet rs = p.executeQuery();
			
			// only store, if ip geo data entry does not exist for given ip
			if (!rs.isBeforeFirst()){
				rs.close();
				
				CSVRecord record = retrieveIpGeo(request.getIp());
				
				String ipg = record.get(2);
				String countryCode = record.get(3);
				String countryName = record.get(4);
				String region = record.get(5);
				String city = record.get(6);
				String code = record.get(7);
				Float latitude = Float.parseFloat(record.get(8));
				Float longitude = Float.parseFloat(record.get(9));
				String timezone = record.get(10);
				
				PreparedStatement psave = getWorker().getConnection().prepareStatement("insert into mobsos_logs.log_ipgeo (ip, country_code, country_name, region_name, city_name, zip_code, lat, lon, timezone) values (?,?,?,?,?,?,?,?,?)");
				psave.setString(1, ipg);
				psave.setString(2, countryCode);
				psave.setString(3, countryName);
				psave.setString(4, region);
				psave.setString(5, city);
				psave.setString(6, code);
				psave.setFloat(7, latitude);
				psave.setFloat(8, latitude);
				psave.setString(9, timezone);
				
				int r = psave.executeUpdate();
				
				if(r<=0){
					Monitor.log.warn("IP geo for ip " + ipg + " not written!");
				}
				psave.close();
			}
		
		} catch (SQLException e) {
			Monitor.log.warn("Could not retrieve/store IP geo data!",e);
		} catch (IOException e) {
			Monitor.log.warn("Could not retrieve/store IP geo data!",e);
		}
	}

	private CSVRecord retrieveIpGeo(String ip) throws IOException{
		
		String apiKey = Monitor.conf.getProperty("ipinfodbKey");
		String url = "http://api.ipinfodb.com/v3/ip-city/?key=" + apiKey + "&ip=" + ip;  
		
		// Create an instance of HttpClient.
		HttpClient client = new HttpClient();

		// Create a method instance.
		GetMethod method = new GetMethod(url);

		// Provide custom retry handler is necessary
		method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, 
				new DefaultHttpMethodRetryHandler(3, false));

		try {
			// Execute the method.
			int statusCode = client.executeMethod(method);

			if (statusCode != HttpStatus.SC_OK) {
				Monitor.log.warn("IP geo retrieval failed: " + method.getStatusLine());
			}

			// Read the response body.
			byte[] responseBody = method.getResponseBody();
			
			BufferedReader bin = new BufferedReader(new InputStreamReader(method.getResponseBodyAsStream()));
			
			char delim = ";".charAt(0);
			CSVRecord r = null;
			for (CSVRecord record : CSVFormat.DEFAULT.withDelimiter(delim).parse(bin)) {
				r = record;
			}
			return r;

		} catch (HttpException e) {
			throw e;
		} catch (IOException e) {
			throw e;
		} finally {
			// Release the connection.
			method.releaseConnection();
		} 
	}	


	public void write() throws SQLException{

		// since we consider log entry packages as atomic units to write to the log database, we explicitly use transactions.
		// A commit only takes place if all complete entries in a log entry package could be written successfully within a transaction.
		// If an error occurs for writing one log entry, we roll back to guarantee consistency and completeness among log entries. 

		// first parse raw CSV data into ready-to-use POJOs (request, query, headers)
		parseData();

		if(this.request.isComplete()){

			// get database connection via worker assigned to write this log entry package
			Connection c = getWorker().getConnection();

			// first write base request log
			this.request.write();

			long rid = this.request.getId();

			// then write request headers log

			this.headers.setId(rid);
			//this.requestHeadersLog.write();

			// then write request query parameters log
			this.query.setId(rid);
			//this.requestQueryLog.write();

			// finally commit whole transaction for log entry package, if everything went ok
			c.commit();

			Monitor.log.info("Log entry package " + this.getId() + " successfully written and committed");	

		} else {
			Monitor.log.warn("No attempt to write log entry package " + this.getId() + " due to incomplete request log entry.");
		}
	}
}

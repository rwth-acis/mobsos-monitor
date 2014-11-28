package acis.dbis.rwth.mobsos.monitor.log.nginx;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.csv.CSVRecord;

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
		Monitor.log.info("Request Length: " + request.getResponseLength());
		Monitor.log.info("Response Length: " + request.getResponseLength());
		Monitor.log.info("Request Processing Time: " + request.getRequestTime());
		
		// parse query parameters and put into hashtable
		// TODO:
		
		// parse header parameters and put into hashtable
		// TODO:
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
				
				Monitor.log.debug("Request log entry stored under new id " + rid);
				
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

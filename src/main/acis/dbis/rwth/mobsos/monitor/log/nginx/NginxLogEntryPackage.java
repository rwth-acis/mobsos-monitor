package acis.dbis.rwth.mobsos.monitor.log.nginx;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Hashtable;

import org.apache.commons.csv.*;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;

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

	private boolean parseData(){

		// init POJOs
		request = new NginxRequestLogEntry(this);
		query = new NginxRequestQueryLogEntry(this);
		headers = new NginxRequestHeadersLogEntry(this);

		// extract POJO data from CSV record

		// first parse header parameters and put into hashtable
		String headers = record.get(18);
		Hashtable<String,String> htable = parseHeaders(headers);
		this.headers.setHeaders(htable);
		
		// drop entry, if it comes from MobSOS Monitor itself
		if(this.headers.getHeaders().get("user-agent").equals("mobsos-monitor")){
			Monitor.log.debug("Dropped, because UserAgent is " + this.headers.getHeaders().get("User-Agent"));
			return false;
		}
		
		// drop entry if it is static content
		// TODO: introduce configurable filter with regex
		if(record.get(5).endsWith(".js") || record.get(5).endsWith(".css")){
			return false;
		}

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


		// extract OIDC access token from request, if available.
		// depending on which auth flow is used, the token is transmitted 
		// in a query parameter or in a HTTP Authorization header. Precedence
		// is given to the auth header option.

		String tokenQuery = record.get(16);
		String tokenHeader = record.get(17);

		String token = null;

		if(!tokenHeader.equals("-") && tokenHeader.startsWith("Bearer ")){
			token = tokenHeader.replaceAll("Bearer ", "");
		} else if(!tokenQuery.equals("-")){
			token = tokenQuery;
		}

		manageIpGeo();

		if(token != null){
			try {
				JSONObject userInfo = retrieveUserInfo(token);
				request.setUserId((String) userInfo.get("sub"));
			} catch (IOException e) {
				Monitor.log.warn("Could not retrieve OpenID Connect user info!",e);
			}
			
			try {
				String clientId = retrieveClientId(token);
				request.setClientId(clientId);

			} catch (SQLException e) {
				Monitor.log.warn("Could not retrieve OpenID Connect client ID!",e);
			}
		}

		// parse query parameters and put into hash table
		String queryParams = record.get(19);
		Hashtable<String,String> qtable = parseQueryParams(queryParams);
		this.query.setQueryParams(qtable);

		Monitor.log.debug("Request Time: " + request.getTime());
		Monitor.log.debug("Request IP: " + request.getIp());
		Monitor.log.debug("Request Scheme: " + request.getScheme());
		Monitor.log.debug("Request Host: " + request.getHost());
		Monitor.log.debug("Request Method: " + request.getMethod());
		Monitor.log.debug("Request URI: " + request.getUri());
		Monitor.log.debug("Status: " + request.getStatus());
		Monitor.log.debug("Referer: " + request.getReferer());
		Monitor.log.debug("Request User Agent: " + request.getUserAgent());
		Monitor.log.debug("Request Accept: " + request.getAccept());
		Monitor.log.debug("Received Content MIME: " + request.getReceivedContent());
		Monitor.log.debug("Served Content MIME: " + request.getSentContent());
		Monitor.log.debug("Request Length: " + request.getRequestLength());
		Monitor.log.debug("Response Length: " + request.getResponseLength());
		Monitor.log.debug("Request Processing Time: " + request.getRequestTime());
		Monitor.log.debug("OpenID Connect User ID: " + request.getUserId());
		Monitor.log.debug("OpenID Connect Client ID: " + request.getClientId());

		// Measure length of String fields to optimize DB schema
		/*
		Monitor.log.info("Request Time: " + request.getTime().length());
		Monitor.log.info("Request IP: " + request.getIp().length());
		Monitor.log.info("Request Scheme: " + request.getScheme().length());
		Monitor.log.info("Request Host: " + request.getHost().length());
		Monitor.log.info("Request Method: " + request.getMethod().length());
		Monitor.log.info("Request URI: " + request.getUri().length());
		Monitor.log.info("Status: " + request.getStatus());
		Monitor.log.info("Referer: " + request.getReferer().length());
		Monitor.log.info("Request User Agent: " + request.getUserAgent().length());
		Monitor.log.info("Request Accept: " + request.getAccept().length());
		Monitor.log.info("Received Content MIME: " + request.getReceivedContent().length());
		Monitor.log.info("Served Content MIME: " + request.getSentContent().length());
		Monitor.log.info("Request Length: " + request.getRequestLength().length());
		Monitor.log.info("Response Length: " + request.getResponseLength().length());
		Monitor.log.info("Request Processing Time: " + request.getRequestTime());
		Monitor.log.info("OpenID Connect User ID: " + request.getUserId());
		Monitor.log.info("OpenID Connect Client ID: " + request.getClientId());
		*/
		return true;
	}

	private Hashtable<String,String> parseHeaders(String h){
		Hashtable<String,String> result = new Hashtable<String,String>();

		String[] htokens = h.split("\\\\x0A");
		for(String t: htokens){
			if(t.indexOf(":")>=0){
				String key = t.substring(0,t.indexOf(":")).trim();
				String val = t.substring(t.indexOf(":")+1).trim();
				result.put(key, val);
			}
		}

		return result;
	}

	private Hashtable<String,String> parseQueryParams(String h){
		Hashtable<String,String> result = new Hashtable<String,String>();

		String[] htokens = h.split("&");

		for(String t: htokens){
			if(t.indexOf("=")>=0){
				String[] keyval = t.split("=");
				String key = keyval[0].trim();
				String val = keyval[1].trim();
				result.put(key, val);
			}
		}

		return result;
	}

	private void manageIpGeo(){
		try {
			String dbname = this.getWorker().getManager().getDatabaseName();
			PreparedStatement p = getWorker().getConnection().prepareStatement("select * from " + dbname + ".log_ipgeo where ip=?");
			p.setString(1, request.getIp());
			ResultSet rs = p.executeQuery();
			System.out.println(request.getIp());
			// only store, if ip geo data entry does not exist for given ip
			if (!rs.isBeforeFirst()){
				rs.close();

				CityResponse record = retrieveIpGeo(request.getIp());
				String ipg = request.getIp();
				String countryCode = record.getCountry().getIsoCode();
				String countryName = record.getCountry().getName();
				String region = record.getLeastSpecificSubdivision().getName();
				String city = record.getCity().getName();
				String code = record.getPostal().getCode();
				float latitude = (float) record.getLocation().getLatitude().doubleValue();
				float longitude = (float)record.getLocation().getLongitude().doubleValue();
				String timezone = record.getLocation().getTimeZone();
				System.out.println(timezone);
				PreparedStatement psave = getWorker().getConnection().prepareStatement("insert into " + dbname + ".log_ipgeo (ip, country_code, country_name, region_name, city_name, zip_code, lat, lon, timezone) values (?,?,?,?,?,?,?,?,?)");
				psave.setString(1, ipg);
				psave.setString(2, countryCode);
				psave.setString(3, countryName);
				psave.setString(4, region);
				psave.setString(5, city);
				psave.setString(6, code);
				psave.setFloat(7, latitude);
				psave.setFloat(8, longitude);
				psave.setString(9, timezone);

				int r = psave.executeUpdate();

				if(r<=0){
					Monitor.log.warn("IP geo for ip " + ipg + " not written!");
				}
				psave.close();
			} else {
				rs.close();
			}


		} catch (SQLException e) {
			Monitor.log.warn("Could not retrieve/store IP geo data!",e);
		} catch (IOException e) {
			Monitor.log.warn("Could not retrieve/store IP geo data!",e);
		}
	}

	private String retrieveClientId(String token) throws SQLException{

		PreparedStatement p = getWorker().getConnection().prepareStatement("select c.client_id from OpenIDConnect.access_token t join OpenIDConnect.client_details c on (t.client_id = c.id) where t.token_value=?");
		p.setString(1, token);
		ResultSet rs = p.executeQuery();

		String result = null;
		while(rs.next()){
			result = rs.getString(1);
		}

		rs.close();
		p.close();
		return result;

	}

	private JSONObject retrieveClientInfo(String token) throws IOException, SQLException{

		String clientId = retrieveClientId(token);

		// set client info request to MitreID API
		// WARNING: NOT COMPLIANT WITH THE OPENID CONNECT STANDARD!
		String url = (String) Monitor.oidcProviderConfig.get("issuer")+"api/clients";

		// at this point we use a regular HttpURLConnection instead of the Apache Commons HTTP Client.
		// The Apache client showed unexpected behavior, when confronted with a bearer token in an Authorization header.
		// The respective request sent caused errors on server side attributed to invalid syntax.


		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		con.setRequestMethod("GET");
		con.setRequestProperty("Authorization", "Bearer " + token);

		BufferedReader in = new BufferedReader(
				new InputStreamReader(con.getInputStream()));

		JSONArray clients = (JSONArray) JSONValue.parse(in);
		in.close();

		if(Monitor.oidcClients == null || !Monitor.oidcClients.equals(clients)){
			Monitor.log.debug("Retrieved Clients and Monitor Clients are different.");
			Monitor.oidcClients = clients;
		}

		for(Object o: Monitor.oidcClients){
			JSONObject client = (JSONObject) o;
			if(client.get("clientId").toString().equals(clientId)){
				Monitor.log.debug("Client Name: " + client.get("clientName"));
				return client;
			}
		}

		return null;

	}

	/**
	 * Given an access token, retrieves Open ID Connect user information.
	 * @throws IOException 
	 * @throws Exception 
	 * @throws ParseException 
	 */
	private JSONObject retrieveUserInfo(String token) throws IOException {

		// send Open ID user info  request
		String url = (String) Monitor.oidcProviderConfig.get("userinfo_endpoint");

		// at this point we use a regular HttpURLConnection instead of the Apache Commons HTTP Client.
		// The Apache client showed unexpected behavior, when confronted with a bearer token in an Authorization header.
		// The respective request sent caused errors on server side attributed to invalid syntax.

		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// optional default is GET
		con.setRequestMethod("GET");

		//add request header
		con.setRequestProperty("Authorization", "Bearer " + token);
		con.setRequestProperty("User-Agent", "mobsos-monitor");

		// parse response as JSON
		BufferedReader in = new BufferedReader(
				new InputStreamReader(con.getInputStream()));

		JSONObject userinfo = (JSONObject) JSONValue.parse(in);

		in.close();

		// return JSON user info
		return userinfo;

	}

	private CityResponse retrieveIpGeo(String ip) throws IOException{
	    String dbLocation = "etc/GeoLite2-City.mmdb";
	         
	    File database = new File(dbLocation);
	    DatabaseReader dbReader = new DatabaseReader.Builder(database)
	      .build();
	    InetAddress ipAddress = InetAddress.getByName(ip);
	    CityResponse response = null;
	    
		try {
			response = dbReader.city(ipAddress);
		} catch (GeoIp2Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return response;
	}	


	public void write() throws SQLException{

		// since we consider log entry packages as atomic units to write to the log database, we explicitly use transactions.
		// A commit only takes place if all complete entries in a log entry package could be written successfully within a transaction.
		// If an error occurs for writing one log entry, we roll back to guarantee consistency and completeness among log entries. 

		// first parse raw CSV data into ready-to-use POJOs (request, query, headers)
		// then check if user agent is MobSOS Monitor itself. If so, parseData returns false.
		// in this case drop log entry.
		if(parseData()){
			persistData();
		}

	}

	private void persistData() throws SQLException{

		Monitor.log.debug("UserAgent: " + this.headers.getHeaders().get("User-Agent"));

		if(this.request.isComplete()){

			// get database connection via worker assigned to write this log entry package
			Connection c = getWorker().getConnection();

			// first write base request log
			this.request.write(c);

			long rid = this.request.getId();

			// then write request headers log
			this.headers.setId(rid);
			this.headers.write(c);

			// then write request query parameters log
			this.query.setId(rid);
			this.query.write(c);

			// finally commit whole transaction for log entry package, if everything went ok
			c.commit();

			Monitor.log.info("Log entry package " + this.getId() + " successfully written and committed.");	

		} else {
			Monitor.log.info("Log entry package " + this.getId() + " dropped.");
		}
	}
}

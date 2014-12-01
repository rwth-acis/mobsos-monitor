package acis.dbis.rwth.mobsos.monitor;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import acis.dbis.rwth.mobsos.monitor.log.LogEntryPackageManager;
import acis.dbis.rwth.mobsos.monitor.log.nginx.NginxLogEntryPackage;
import acis.dbis.rwth.mobsos.monitor.notify.NotificationManager;

public class Monitor {

	// stream of incoming data (usually `tail -f access.log` in stand-alone mode)
	private InputStream in;

	// properties from file ./etc/conf.properties
	public static Properties conf;
	public static JSONObject oidcProviderConfig;

	// logging framework
	public final static Logger log = LoggerFactory.getLogger(Monitor.class );

	public Monitor(InputStream in) {

		// set input stream
		this.in = in;

		// read configuration
		readConf();
		
		// initialize log entry package manager. If this fails, exit with error.
		initLogEntryPackageManager();
		
		// initialize OpenID Connect interation
		
		initOidcProviderConfig();
		// initialize notification manager (for some reasons takes over a minute to init...)
				//initNotificationManager();
		
	}
	
	private void initLogEntryPackageManager(){
		try {
			LogEntryPackageManager.initInstance(conf.getProperty("jdbcDriverClassName"), 
					conf.getProperty("jdbcUrl"), 
					conf.getProperty("jdbcLogin"), 
					conf.getProperty("jdbcPass"),
					Integer.parseInt(conf.getProperty("jdbcWorkers")),
					Integer.parseInt(conf.getProperty("jdbcKeepAlive")));

		} catch (NumberFormatException | ClassNotFoundException | SQLException e) {
			Monitor.log.error("Could not start log entry package manager! Stopping... ", e);
			System.exit(-1);
		}
	}
	
	/**
	 * Fetches Open ID Connect provider configuration, according to the OpenID Connect discovery specification
	 * (cf. http://openid.net/specs/openid-connect-discovery-1_0.html#ProviderConfig)
	 * @throws ParseException 
	 */
	private void initOidcProviderConfig() {
		
		// send Open ID Provider Config request
		// (cf. http://openid.net/specs/openid-connect-discovery-1_0.html#ProviderConfig)
		String url = Monitor.conf.getProperty("oidcProviderUrl").trim() + "/.well-known/openid-configuration";
		
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
				Monitor.log.warn("fetching OpenID Connect provider config failed: " + method.getStatusLine());
			}

			// Read the response body.
			String responseBody = new String(method.getResponseBody());
			
			Monitor.oidcProviderConfig = (JSONObject) JSONValue.parseWithException(responseBody.toString());
			
			//Monitor.log.debug("OIDC Discovery Endpoint response parsed JSON:");
			//Monitor.log.debug(this.oidcProviderConfig.toJSONString());
			

		} catch (HttpException e) {
			Monitor.log.error("Could not retrieve OpenID Connect provider config!",e);
		} catch (IOException e) {
			Monitor.log.error("Could not retrieve OpenID Connect provider config!",e);
		} catch (ParseException e) {
			Monitor.log.error("Could not retrieve OpenID Connect provider config!",e);
		} finally {
			// Release the connection.
			method.releaseConnection();
		} 
	}

	private void initNotificationManager(){
		try {

			NotificationManager.initInstance(
					conf.getProperty("mailHost"),
					Integer.parseInt(conf.getProperty("mailPort")), 
					conf.getProperty("mailUsername"), 
					conf.getProperty("mailPassword"), 
					conf.getProperty("mailAuth"), 
					conf.getProperty("mailTLSEnable"), 
					conf.getProperty("mailFrom"), 
					conf.getProperty("mailTo"));

		} catch (Exception e1) {
			Monitor.log.warn("couldn't start mail notification manager!", e1);
		}
	}

	private void readConf(){
		conf = new Properties();
		InputStream input = null;

		try {
			input = new FileInputStream("./etc/conf.properties");

			// load a properties file
			conf.load(input);

		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * starts listening for new log messages on the input stream.
	 * 
	 * @throws IOException
	 */
	public void listen() throws IOException{
		BufferedReader bin = new BufferedReader(new InputStreamReader(in));
		
		
		for (CSVRecord record : CSVFormat.DEFAULT.parse(bin)) {
			
			NginxLogEntryPackage lp = new NginxLogEntryPackage(record);
			
			LogEntryPackageManager.getInstance().addLogEntryPackage(lp);
			LogEntryPackageManager.getInstance().queueLogEntryPackage(lp.getId());
			
		}
	}

	public static void main(String[] args){
		try {
			Monitor m = new Monitor(System.in);
			m.listen();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}

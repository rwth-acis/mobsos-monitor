package acis.dbis.rwth.mobsos.monitor;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.Properties;

import org.slf4j.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.dbcp2.BasicDataSource;

import acis.dbis.rwth.mobsos.monitor.log.LogEntryPackageManager;
import acis.dbis.rwth.mobsos.monitor.notify.NotificationManager;

public class Monitor {

	// stream of incoming data (usually `tail -f access.log` in stand-alone mode)
	private InputStream in;
	
	// properties from file ./etc/conf.properties
	private Properties conf;
	
	// pooled connection to MobSOS database
	private static BasicDataSource dataSource;
	
	// logging framework
	public final static Logger log = LoggerFactory.getLogger(Monitor.class );

	public Monitor(InputStream in) {
		
		// set input stream
		this.in = in;
		
		// read configuration
		readConf();
		
		// initialize notification manager (for some reasons takes over a minute to init...)
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
	
	private void setupDataSource() throws ClassNotFoundException {

		// request classloader to load JDBC driver class
		Class.forName(conf.getProperty("jdbcDriverClassName"));

		// prepare and configure data source
		dataSource = new BasicDataSource();
		dataSource.setDefaultAutoCommit(true);
		dataSource.setDriverClassName(conf.getProperty("jdbcDriverClassName"));
		dataSource.setUsername(conf.getProperty("jdbcLogin"));
		dataSource.setPassword(conf.getProperty("jdbcPass"));
		dataSource.setUrl(conf.getProperty("jdbcUrl") + conf.getProperty("jdbcSchema"));
		dataSource.setValidationQuery("select 1");
		dataSource.setDefaultQueryTimeout(1000);
		dataSource.setMaxConnLifetimeMillis(100000);
	}

	public void listen() throws IOException{
		BufferedReader bin = new BufferedReader(new InputStreamReader(in));
		for (CSVRecord record : CSVFormat.DEFAULT.parse(bin)) {
			System.out.println("New record: ");

			for (String field : record) {
				System.out.println("*** " + field);
			}
			
			System.out.println();
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

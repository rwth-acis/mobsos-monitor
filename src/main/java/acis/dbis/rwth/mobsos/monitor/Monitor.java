package acis.dbis.rwth.mobsos.monitor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.slf4j.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import acis.dbis.rwth.mobsos.monitor.notify.NotificationManager;

public class Monitor {

	private InputStream in;
	
	public final static Logger log = LoggerFactory.getLogger(Monitor.class );

	public Monitor(InputStream in) {
		this.in = in;
		
		try {
			
			//NotificationManager.initInstance(mailHost, mailPort, mailUsername, mailPassword, mailAuth, mailTLSEnable, mailFrom, mailTo);
		} catch (Exception e1) {
			Monitor.log.warn("Could not start MobSOS Notification Manager!", e1);
		}
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

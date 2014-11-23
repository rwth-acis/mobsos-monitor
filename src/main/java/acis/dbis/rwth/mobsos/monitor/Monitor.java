package acis.dbis.rwth.mobsos.monitor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

public class Monitor {

	private InputStream in;

	public Monitor(InputStream in) {
		this.in = in;
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

package acis.dbis.rwth.mobsos.monitor.log.nginx;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Iterator;

import acis.dbis.rwth.mobsos.monitor.log.LogEntry;
import acis.dbis.rwth.mobsos.monitor.log.LogEntryPackage;

public class NginxRequestQueryLogEntry extends LogEntry {

	public NginxRequestQueryLogEntry(LogEntryPackage container) {
		super(container);
	}

	private String query_string;

	@Override
	public boolean isComplete() {
		return false;
	}

	@Override
	public boolean write() throws SQLException {
		if(isComplete()){


			// get corresponding prepared statement, then clear and set parameters
			/*
			Connection c = this.getContainer().getWorker().getConnection();
			PreparedStatement stmt = c.prepareStatement("insert into mobsos_log.log_header(id,key,val) values (?,?,?)");

			Iterator<String> it = answerFieldTable.keySet().iterator();
			while(it.hasNext()){

				String qkey = it.next();
				String qval = ""+answerFieldTable.get(qkey);

				stmt.setString(1,sub);
				stmt.setInt(2,surveyId);
				stmt.setString(3, qkey);
				stmt.setString(4, qval);
				stmt.setTimestamp(5, new Timestamp(now.getTime()));
				stmt.addBatch();

			}

			stmt.executeBatch();
			*/
			
			return true;
		} else {
			return false;
		}

	}

	/**
	 * @return the query_string
	 */
	public String getQuery_string() {
		return query_string;
	}

	/**
	 * @param query_string the query_string to set
	 */
	public void setQuery_string(String query_string) {
		this.query_string = query_string;
	}

}

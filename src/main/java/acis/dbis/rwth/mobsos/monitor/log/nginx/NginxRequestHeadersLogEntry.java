package acis.dbis.rwth.mobsos.monitor.log.nginx;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Hashtable;

import acis.dbis.rwth.mobsos.monitor.log.LogEntry;
import acis.dbis.rwth.mobsos.monitor.log.LogEntryPackage;

public class NginxRequestHeadersLogEntry extends LogEntry {


	private Hashtable<String,String> headers;

	public NginxRequestHeadersLogEntry(LogEntryPackage container) {
		super(container);
	}

	@Override
	public boolean isComplete() {
		// consider log entry as complete, if id is set from parent request log entry and if headers table is not empty.
		if(getId()!=null && getHeaders() !=null && getHeaders().size()>0){
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean write(Connection c) throws SQLException {
		PreparedStatement stmt = null;

		try{
			if(isComplete()){
				String dbname = this.getContainer().getWorker().getManager().getDatabaseName();
				stmt = c.prepareStatement("insert into " + dbname +".log_header(id,name,value) values (?,?,?)");

				for(String key: getHeaders().keySet()){
					stmt.clearParameters();
					stmt.setLong(1,getId());
					stmt.setString(2,key);
					stmt.setString(3, getHeaders().get(key));
					stmt.addBatch();
				}

				stmt.executeBatch();
				return true;

			} else {
				return false;
			}
		} catch(SQLException e){
			throw e;
		} finally {
			if(stmt!=null)
				stmt.close();
		}
	}

	/**
	 * @return the headers
	 */
	public Hashtable<String,String> getHeaders() {
		return headers;
	}

	/**
	 * @param headers the headers to set
	 */
	public void setHeaders(Hashtable<String,String> headers) {
		this.headers = headers;
	}

}

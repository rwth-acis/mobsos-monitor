package acis.dbis.rwth.mobsos.monitor.log.nginx;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import javax.xml.bind.DatatypeConverter;

import acis.dbis.rwth.mobsos.monitor.Monitor;
import acis.dbis.rwth.mobsos.monitor.log.LogEntry;
import acis.dbis.rwth.mobsos.monitor.log.LogEntryPackage;

public class NginxRequestLogEntry extends LogEntry {

	private String time, ip, scheme, host, method, uri, status, referer, userAgent, accept, receivedContent, sentContent, requestLength, responseLength, requestTime, clientId, userId;

	public NginxRequestLogEntry(LogEntryPackage container) {
		super(container);
	}

	@Override
	public boolean isComplete() {
		if(userId != null && clientId != null){
			Monitor.log.debug("Authorized request -> Log!");
		}
		return true;
	}

	@Override
	public boolean write(Connection c) throws SQLException {
		if(isComplete()){

			PreparedStatement stmt = null;
			ResultSet rs = null;
			try{
				String dbname = this.getContainer().getWorker().getManager().getDatabaseName();
				//time, ip, scheme, host, method, uri, status, referer, userAgent, accept, content_type, requestLength, responseLength, requestTime;
				stmt = c.prepareStatement("insert into " + dbname + ".log(time,ip,user_id, client_id,scheme,host,method,uri,status,referer,useragent,accept,received_content,sent_content,request_length,response_length,request_time) "
						+ "values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);

				if(uri.endsWith("/")){
					uri = uri.substring(0,uri.length()-1);
				}

				stmt.clearParameters();
				stmt.setTimestamp(1, new Timestamp(DatatypeConverter.parseDateTime(time).getTimeInMillis()));
				stmt.setString(2,ip);
				stmt.setString(3, userId);
				stmt.setString(4, clientId);
				stmt.setString(5,scheme);
				stmt.setString(6,host);
				stmt.setString(7,method);
				stmt.setString(8,uri);
				stmt.setInt(9,Integer.parseInt(status));
				stmt.setString(10, referer);
				stmt.setString(11, userAgent);
				stmt.setString(12, accept);
				stmt.setString(13, receivedContent);
				stmt.setString(14, sentContent);

				stmt.setInt(15,Integer.parseInt(requestLength));
				stmt.setInt(16,Integer.parseInt(responseLength));
				stmt.setFloat(17,Float.parseFloat(requestTime));

				stmt.executeUpdate();

				rs = stmt.getGeneratedKeys();

				if (rs.next()) {
					Long nid = rs.getLong(1);
					this.setId(nid);
					return true;
				} else {
					throw new SQLException("Could not write new request log entry!");
				} 
			} catch (SQLException e){
				e.printStackTrace();
				throw e;
			} finally {
				try { if (rs != null) rs.close(); } catch(Exception e) {Monitor.log.warn("Could not close result set!",e);}
				try { if (stmt != null) stmt.close(); } catch(Exception e) {Monitor.log.warn("Could not close result set!",e);}
			}

		} else {
			return false;
		}
	}

	/**
	 * @return the time
	 */
	public String getTime() {
		return time;
	}

	/**
	 * @param time the time to set
	 */
	public void setTime(String time) {
		this.time = time;
	}

	/**
	 * @return the ip
	 */
	public String getIp() {
		return ip;
	}

	/**
	 * @param ip the ip to set
	 */
	public void setIp(String ip) {
		this.ip = ip;
	}

	/**
	 * @return the scheme
	 */
	public String getScheme() {
		return scheme;
	}

	/**
	 * @param scheme the scheme to set
	 */
	public void setScheme(String scheme) {
		this.scheme = scheme;
	}

	/**
	 * @return the host
	 */
	public String getHost() {
		return host;
	}

	/**
	 * @param host the host to set
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * @return the method
	 */
	public String getMethod() {
		return method;
	}

	/**
	 * @param method the method to set
	 */
	public void setMethod(String method) {
		this.method = method;
	}

	/**
	 * @return the uri
	 */
	public String getUri() {
		return uri;
	}

	/**
	 * @param uri the uri to set
	 */
	public void setUri(String uri) {
		this.uri = uri;
	}

	/**
	 * @return the status
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * @param status the status to set
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * @return the referer
	 */
	public String getReferer() {
		return referer;
	}

	/**
	 * @param referer the referer to set
	 */
	public void setReferer(String referer) {
		this.referer = referer;
	}

	/**
	 * @return the userAgent
	 */
	public String getUserAgent() {
		return userAgent;
	}

	/**
	 * @param userAgent the userAgent to set
	 */
	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	/**
	 * @return the accept
	 */
	public String getAccept() {
		return accept;
	}

	/**
	 * @param accept the accept to set
	 */
	public void setAccept(String accept) {
		this.accept = accept;
	}

	/**
	 * @return the receivedContent
	 */
	public String getReceivedContent() {
		return receivedContent;
	}

	/**
	 * @param receivedContent the receivedContent to set
	 */
	public void setReceivedContent(String receivedContent) {
		this.receivedContent = receivedContent;
	}

	/**
	 * @return the sentContent
	 */
	public String getSentContent() {
		return sentContent;
	}

	/**
	 * @param sentContent the sentContent to set
	 */
	public void setSentContent(String sentContent) {
		this.sentContent = sentContent;
	}

	/**
	 * @return the requestLength
	 */
	public String getRequestLength() {
		return requestLength;
	}

	/**
	 * @param requestLength the requestLength to set
	 */
	public void setRequestLength(String requestLength) {
		this.requestLength = requestLength;
	}

	/**
	 * @return the responseLength
	 */
	public String getResponseLength() {
		return responseLength;
	}

	/**
	 * @param responseLength the responseLength to set
	 */
	public void setResponseLength(String responseLength) {
		this.responseLength = responseLength;
	}

	/**
	 * @return the requestTime
	 */
	public String getRequestTime() {
		return requestTime;
	}

	/**
	 * @param requestTime the requestTime to set
	 */
	public void setRequestTime(String requestTime) {
		this.requestTime = requestTime;
	}

	/**
	 * @return the clientId
	 */
	public String getClientId() {
		return clientId;
	}

	/**
	 * @param clientId the clientId to set
	 */
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	/**
	 * @return the userId
	 */
	public String getUserId() {
		return userId;
	}

	/**
	 * @param userId the userId to set
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}

}

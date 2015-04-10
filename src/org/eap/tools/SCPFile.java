package org.eap.tools;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.SCPClient;

public class SCPFile {
	
	public Connection conn = null;
	public Properties property = null;

	public SCPFile(){
		property = new Properties();
		InputStream in = this.getClass().getResourceAsStream("/jcseg.properties");
		try {
			property.load(in);
			conn = new Connection(property.getProperty("scphost"));
			conn.connect();
			boolean isAuthenticated = conn.authenticateWithPassword(property.getProperty("scpusename"),
					property.getProperty("scppassword"));
			if (isAuthenticated == false)
				throw new IOException("Authentication failed.");
		}
		catch (IOException e){
			e.printStackTrace(System.err);
		}
	}
	
	public static void main(String[] args){
		
	}
	
	public void uploadFile(String fileName){
		try{
			SCPClient scpClient = conn.createSCPClient();
			scpClient.put(fileName, property.getProperty("scpdir"));
			conn.close();
		}catch(Exception e){
			e.printStackTrace(System.err);
		}
	}
	
	public void downloadFile(String fileName, String dir){
		try{
			SCPClient scpClient = conn.createSCPClient();
			scpClient.get(property.getProperty("scpdir") + "/" +fileName, dir);
			conn.close();
		}catch(Exception e){
			e.printStackTrace(System.err);
		}
	}
}

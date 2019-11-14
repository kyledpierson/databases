package cs5530;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Connector
{
	public Connection conn;
	public PreparedStatement stmt;
	
	public Connector()
	{
		try
		{
			String userName = "<username>";
			String password = "<password>";
			String url = "<url>";
			
		    Class.forName ("com.mysql.jdbc.Driver").newInstance ();
        	conn = DriverManager.getConnection (url, userName, password);
			conn.setAutoCommit(false);
        }
		catch(Exception e)
		{
			System.err.println("Unable to open mysql jdbc connection. The error is as follows:");
            System.err.println(e.getMessage());
		}
	}
	
	public void closeConnection()
	{
		try
		{
			conn.close();
		}
		catch(Exception e)
		{
		}
	}
	
 	public void closeStatement()
	{
		try
		{
			stmt.close();
		}
		catch(Exception e)
		{
		}
	}
}

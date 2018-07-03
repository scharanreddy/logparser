package com.ef;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.regex.Pattern;


import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Class to parse a log file and check the IP's with requests exceeding a threshold
 * 
 * @author sricharan
 *
 */

public class Parser
{
	

	public static void main(String[] args) 
	{
		OptionsClass options = new OptionsClass();
		options = getOptions(args);
		String       filepath = options.getfilePath();
		File         file = new File(filepath);
		
		try 
		{
            List<String> contents = FileUtils.readLines(file, "UTF-8");
            
             //Iterate the result to print each line of the file.
            clearDB(); //Clear the table for loading the fresh Log
            System.out.println("Log Contents cleared from DB");
            System.out.println("Start of load of Log File into DB");
            Connection con = getDBConnection();
            PreparedStatement ps = null;
            loadinDB(contents,con);
            closeConnection(con);
            System.out.println("end of load of Log into DB");
            
            if(options.getDuration().equalsIgnoreCase("hourly"))
            {
            	captureIPWithHourlyRequestsExceeded(options.startDate,options.threshold);
            }
            else if(options.getDuration().equalsIgnoreCase("daily"))
            {
            	captureIPWithDailyRequestExceeded(options.startDate,options.threshold);
            }
            
        }
		catch(Exception e)
		{
        	  //TODO: Use Log4J
			  System.out.println(e.getMessage());
		}
		
	}

	/**
	 * Function to capture IP's with Daily requests exceeded
	 * @param startDate the start time to check for web requests
	 * @param threshold the maximum number of requests allowable
	 */
	private static void captureIPWithDailyRequestExceeded(String startDate, String threshold) 
	{
		
		String selectSql = "SELECT DISTINCT IP FROM logger.log_tbl "+ 
		                   " WHERE LOG_TIME >= ? "+
		                   " AND  LOG_TIME  < (? + INTERVAL 24 HOUR) "+
		                   " GROUP BY IP "+
		                   " HAVING COUNT(IP)>? ";

       Connection conn   = getDBConnection();
       PreparedStatement ps = null;
       ResultSet rs =null;
       try
       {
         ps = conn.prepareStatement(selectSql);
         ps.setTimestamp(1, Timestamp.valueOf(StringUtils.replaceOnce(startDate, "."," ")));
         ps.setTimestamp(2, Timestamp.valueOf(StringUtils.replaceOnce(startDate, "."," ")));
         ps.setInt(3, Integer.parseInt(threshold));
         rs = ps.executeQuery();
         System.out.println("the ip's blocked for exceeding daily requests are ");
         while(rs.next())
          {
           System.out.println(rs.getString("IP"));
           insertIntoBlockedTable(rs.getString("IP"),"the IP "+rs.getString("IP")+" has exceeded the maximum daily threshold of " +threshold+ " requests ");
          }

        }
        catch(SQLException e)
        {
        	//TODO: Use log4j or logging API
        	System.out.println(e.getMessage());
        }
        finally
        {
	      closeResultSet(rs);
	      closeConnection(conn);
	      closeStatement(ps);
	    }
		
	}

	private static void insertIntoBlockedTable(String IP, String reason) 
	{
		String sqlInsert = "INSERT INTO logger.blocked_tbl (IP_ADDRESS,BLOCKING_REASON)" + 
				           "VALUES (?,?) ";
		
		Connection conn   = getDBConnection();
        PreparedStatement ps = null;
        
        try 
        {
			ps = conn.prepareStatement(sqlInsert);
			ps.setString(1, IP);
			ps.setString(2, reason);
			ps.executeUpdate();
		}
        catch (SQLException e)
        {
			e.printStackTrace();
		}
        finally
        {
        	closeConnection(conn);
        	closeStatement(ps);
        }
      }

	/**
	 * Function to Capture IP's with Hourly requests exceeded
	 * @param startDate the start time for capturing requests
	 * @param threshold the maximum allowed requests per hour
	 */
	private static void captureIPWithHourlyRequestsExceeded(String startDate,String threshold)
	{
		String selectSql = " SELECT DISTINCT IP FROM logger.log_tbl "+ 
				           " WHERE LOG_TIME >= ? "+
				           " AND   LOG_TIME < (? + INTERVAL 1 HOUR) "+
				           " GROUP BY IP "+
				           " HAVING COUNT(IP)>? ";

        Connection conn   = getDBConnection();
        PreparedStatement ps = null;
        ResultSet rs =null;
        try
        {
	    ps = conn.prepareStatement(selectSql);
	    ps.setTimestamp(1, Timestamp.valueOf(StringUtils.replaceOnce(startDate, "."," ")));
	    ps.setTimestamp(2, Timestamp.valueOf(StringUtils.replaceOnce(startDate, "."," ")));
	    ps.setInt(3, Integer.parseInt(threshold));
	    rs = ps.executeQuery();
	    System.out.println("the ip's blocked for exceeding hourly requests are ");
	     while(rs.next())
	     {
	      System.out.println(rs.getString("IP"));
	      insertIntoBlockedTable(rs.getString("IP"),"the IP "+rs.getString("IP")+" has exceeded the maximum hourly threshold of "+threshold.toString()+" requests ");
	     }
	
         }
        catch(SQLException e)
         {
        	//TODO: Use logging API
        	System.out.println(e.getMessage()); 
         }
        finally
        {
        	closeResultSet(rs);
        	closeConnection(conn);
        	closeStatement(ps);
        }
	}
     
	// Helper function to close resultSet
	private static void closeResultSet(ResultSet rs)
	{
	
		if(rs!=null)
		{
			try
			{
				rs.close();
			}
			catch (SQLException e) 
			{
				//TODO: Use logging API
				System.out.println(e.getMessage());;
			}
		}
		
	}

	/**
	 * Function to return the options object with the command line parameters which were passed
	 * @param   args the command line args which were passed to the main class
	 * @return  optionsClass object with all the required parameters
	 */
	private static OptionsClass getOptions(String[] args)
	{
	    OptionsClass opts = new OptionsClass();
	    String filepath   = args[0].substring(args[0].lastIndexOf('=') + 1, args[0].length());
	    String startDate  = args[1].substring(args[1].lastIndexOf('=')+1, args[1].length());
	    String duration   = args[2].substring(args[2].lastIndexOf('=')+1, args[2].length());
	    String threshold  = args[3].substring(args[3].lastIndexOf('=')+1, args[3].length());
	    opts.setfilePath(filepath);
	    opts.setStartDate(startDate);
	    opts.setDuration(duration);
	    opts.setThreshold(threshold);
 		return opts;
	}

	/**
	 * Function to clear the log db for fresh load
	 */
	private static void clearDB() 
	{
		Connection conn = getDBConnection();
		PreparedStatement ps = null;
		String     delSql  = "DELETE FROM LOGGER.LOG_TBL ";
		try 
		{
			ps = conn.prepareStatement(delSql);
			ps.executeUpdate();
		} 
		catch (SQLException e) 
		{
			//TODO: Put log4j logger
			System.out.println(e.getMessage());
		}
		finally
		{
			closeConnection(conn);
			closeStatement(ps);
		}
	}

	//Helper function to close JDBC Statement
	private static void closeStatement(PreparedStatement ps) 
	{
		if(ps!=null)
		{
			try 
			{
				ps.close();
			} 
			catch (SQLException e)
			{
				//TODO: Put log4j logger
				System.out.println(e.getMessage());
			}
		}
		
	}

	//Helper function to close JDBC Connection
	private static void closeConnection(Connection conn) 
	{
		if(conn!=null)
		{
			try 
			{
				conn.close();
			} 
			catch (SQLException e) 
			{
				//TODO: Put log4j logger
				System.out.println(e.getMessage());
			}
		}
		
	}
    
	/**
	 * Function to load contents of the file into DB
	 * @param contents List of lines from the log file
	 * @param conn the connection object for the Database
	 * @throws SQLException
	 */
	private static void loadinDB(List<String> contents,Connection conn) throws SQLException 
	{
	  String sqlInsert = "INSERT INTO LOGGER.LOG_TBL " + 
		                 "(id, LOG_TIME, IP, PROTOCOL_STRING,HTTP_STATUS_CODE,DEVICE_STRING) VALUES " + 
		                 "(?,?,?,?,?,?) ";
	  PreparedStatement ps = conn.prepareStatement(sqlInsert);
	  //Connection conn = getDBConnection();
	  int i=1;
	  for(String line:contents)
	  {
		 String[]   values = line.split(Pattern.quote("|"));
		 try
		 {
			//PreparedStatement stmt = conn.prepareStatement(sqlInsert);
			ps.setInt(1, i);
			ps.setTimestamp(2, Timestamp.valueOf(values[0]));
			ps.setString(3, values[1]);
			ps.setString(4,values[2]);
			ps.setInt(5,Integer.parseInt(values[3]));
			ps.setString(6, values[4]);
			ps.addBatch();
		    i++;
		     if (i % 1000 == 0 || i == contents.size()) 
		     {
                 ps.executeBatch(); // Execute every 1000 rows
             }
	     }
		 catch (SQLException e) 
		 {
			//TODO: Use log4j or logging API
			System.out.println(e.getMessage());;
		 }
		
	  }
	  
   }

    /**
     * Function to return the connection object
     * @return the connection to the local mysql DB
     */
    private static Connection getDBConnection() 
	{
		Connection con = null;
		
		try 
		{
			Class.forName("com.mysql.cj.jdbc.Driver");  
			con=DriverManager.getConnection("jdbc:mysql://localhost:3306/logger","test","test#1234");
		} 
		catch (SQLException e)
		{
			//TODO: Put log4j logger
			System.out.println(e.getMessage());
		} 
		catch (ClassNotFoundException e)
		{
			//TODO: Put log4j logger
			System.out.println(e.getMessage());
		}  
		return con;
	}
                 


	


}

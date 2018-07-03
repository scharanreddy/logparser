package com.ef.Parser;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;

/**
 * Class to parse a log file
 * @author sricharan
 *
 */
public class parser
{
	

	public static void main(String[] args)
	{
		String filepath = "";
		File file = new File(filepath);
		String connString = "";
		createSchema(connString);
		try 
		{
            List<String> contents = FileUtils.readLines(file, "UTF-8");

            // Iterate the result to print each line of the file.
            for (String line : contents)
            {
                loadinDB(line);
            }
        }catch(Exception e)
		{
        	  e.printStackTrace();
		}
		
	}

	private static void createSchema(String connString)
	{
		String sql = "CREATE TABLE LOG_TBL (" + 
				"   id int(11) NOT NULL, " + 
				"   LOG_TIME TIMESTAMP DEFAULT 0, " + 
				"   IP VARCHAR(20) NOT NULL," + 
				"   PROTOCOL_STRING VARCHAR NOT NULL," +
				"   HTTP_STATUS_CODE int(5) NOT NULL ," + 
				"   DEVICE_STRING VARCHAR(50)"+
				")";
		
	}

	private static void loadinDB(String line) 
	{
		// TODO Auto-generated method stub
		
	}

}

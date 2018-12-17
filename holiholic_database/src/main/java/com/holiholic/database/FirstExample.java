package com.holiholic.database;
//STEP 1. Import required packages
import com.holiholic.database.auth.DatabaseConnection;

import java.sql.*;

public class FirstExample {


    public static void main(String[] args) {
        Statement stmt = null;
        try{

            //STEP 4: Execute a query
            System.out.println("Creating statement...");
            stmt = DatabaseConnection.getConnection().createStatement();
            String sql;
            sql = "INSERT into holiholicdb.Employees values (105, 1555, \"cristi\", \"ghr\");";
            int res = stmt.executeUpdate(sql);

            //STEP 6: Clean-up environment

            stmt.close();
//            DatabaseConnection.getConnection().close();
        }catch(SQLException se){
            //Handle errors for JDBC
            se.printStackTrace();
        }catch(Exception e){
            //Handle errors for Class.forName
            e.printStackTrace();
        }finally{
            //finally block used to close resources
            try{
                if(stmt!=null)
                    stmt.close();
            }catch(SQLException se2){
            }// nothing we can do
//            try{
//                if(DatabaseConnection.getConnection()!=null)
//                    DatabaseConnection.getConnection().close();
//            }catch(SQLException se){
//                se.printStackTrace();
//            }//end finally try
        }//end try
        System.out.println("Goodbye!");

        System.out.println(DatabaseConnection.getConnection());
        try{

            //STEP 4: Execute a query
            System.out.println("Creating statement...");
            stmt = DatabaseConnection.getConnection().createStatement();
            String sql;
            sql = "SELECT id, first, last, age FROM holiholicdb.Employees";
            ResultSet rs = stmt.executeQuery(sql);

            //STEP 5: Extract data from result set
            while(rs.next()){
                //Retrieve by column name
                int id  = rs.getInt("id");
                int age = rs.getInt("age");
                String first = rs.getString("first");
                String last = rs.getString("last");

                //Display values
                System.out.print("ID: " + id);
                System.out.print(", Age: " + age);
                System.out.print(", First: " + first);
                System.out.println(", Last: " + last);
            }
            //STEP 6: Clean-up environment
            rs.close();
            stmt.close();
//            DatabaseConnection.getConnection().close();
        }catch(SQLException se){
            //Handle errors for JDBC
            se.printStackTrace();
        }catch(Exception e){
            //Handle errors for Class.forName
            e.printStackTrace();
        }finally{
            //finally block used to close resources
            try{
                if(stmt!=null)
                    stmt.close();
            }catch(SQLException se2){
            }// nothing we can do
//            try{
//                if(DatabaseConnection.getConnection()!=null)
//                    DatabaseConnection.getConnection().close();
//            }catch(SQLException se){
//                se.printStackTrace();
//            }//end finally try
        }//end try
        System.out.println("Goodbye!");

    }//end main
}//end FirstExample
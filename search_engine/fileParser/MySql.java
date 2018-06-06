import org.apache.commons.lang3.StringEscapeUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.UnknownHostException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class MySql {

    // JDBC �����������ݿ� URL
    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    static final String DB_URL = "jdbc:mysql://localhost:3306/SchoolSearch?characterEncoding=UTF-8&useSSL=false";
	// ���ݿ���û��������룬��Ҫ�����Լ�������
    static final String USER = "root";
    static final String PASS = "";
	//public ResultSet rst = null;
    Connection conn = null;
    Statement stmt = null;
	public PreparedStatement p_insertpdf = null;
	public PreparedStatement p_insertdoc = null;
	public PreparedStatement p_insert8 = null;
    public static String[] tableName = {"pdf", "doc", "html"};
	MySql() 
	{
		getConnection();
		getStatement();
		DropAllTables();
		createTables();
    }
	public void getConnection() {
		try {
            // ע�� JDBC ����
            Class.forName("com.mysql.jdbc.Driver");
			// ������
            System.out.println("�������ݿ�...");
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
        } catch(ClassNotFoundException e){
            System.out.println("�������ݿ�����ʧ��:\n"+e.toString());
            return;
        }
        catch (SQLException e) {
            System.out.println("��ȡ����ʧ��" + e.toString());
            return;
        }
    }

    public void getStatement() 
	{
        try {
            stmt = conn.createStatement();
            p_insertpdf = conn.prepareStatement("INSERT INTO pdf VALUES (?,?,?);");
            p_insertdoc = conn.prepareStatement("INSERT INTO doc VALUES (?,?,?);");
            p_insert8 = conn.prepareStatement("INSERT INTO html VALUES (?,?,?,?,?,?,?,?);");
        }catch (SQLException e) {
            System.out.println("getStatement failed " + e.toString());
        }

    }
	public void createTables()
	{
		try{
			String sql = "";
			stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS SchoolSearch character set utf8mb4;");
			sql = "CREATE TABLE IF NOT EXISTS pdf " +
					"(path VARCHAR(255) not null primary key, " +
					"type VARCHAR(4), " +
					"content TEXT) DEFAULT CHARSET=utf8mb4";
			stmt.executeUpdate(sql);
			sql = "CREATE TABLE IF NOT EXISTS doc " +
					"(path VARCHAR(255) not null primary key, " +
					"type VARCHAR(4), " +
					"content TEXT) DEFAULT CHARSET=utf8mb4";
			stmt.executeUpdate(sql);
			sql = "CREATE TABLE IF NOT EXISTS html " +
					"(path VARCHAR(255) not null primary key, " +
					"type VARCHAR(4), " +
					"title VARCHAR(255), " +
					"h1 VARCHAR(255), " + 
					"h2 VARCHAR(255), " +
					"content TEXT, " +
					"outlinks TEXT, " + 
					"pagerank FLOAT) DEFAULT CHARSET=utf8mb4";
			stmt.executeUpdate(sql);
		}
		catch (SQLException se)
		{
			se.printStackTrace();
		}
	}
	public void DropAllTables()
	{
		
		try {
			String sql = "DROP TABLE pdf;";
			stmt.executeUpdate(sql);
			sql = "DROP TABLE html;";
			stmt.executeUpdate(sql);
			sql = "DROP TABLE doc;";
			stmt.executeUpdate(sql);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
    public ResultSet read(String sqlSelect){
        ResultSet rst = null;
        //System.out.println(sqlSelect);
        try{
            rst = stmt.executeQuery(sqlSelect);
            System.out.println("��ѯ�ɹ�");
        }
        catch(SQLException e){
            System.out.println(e.toString());
        }
        return rst;
    }

    public int update(String sqlUpdate){
        int nRecord = 0;
        try{
            nRecord = stmt.executeUpdate(sqlUpdate);
            //System.out.println("���³ɹ�");
        }
        catch(SQLException e){
            System.out.println(e.toString());
        }
        return nRecord;
    }

    //ɾ����¼[delete from table1 where ��Χ]
    public int delete(String sqlDelete){
        int nRecord=0;
        try{
            nRecord = stmt.executeUpdate(sqlDelete);
            System.out.println("ɾ���ɹ�");
        }
        catch(SQLException e){
            System.out.println(e.toString());
        }
        return nRecord;
    }

    //�ر�
    public void closeAll(){
        try{
			stmt.close();
			conn.close();
        }
        catch(SQLException e){
            System.out.println(e.toString());
            return;
        }
    }




    public static void main(String[] args) throws SQLException {
        MySql ms = new MySql();
       



    }
}
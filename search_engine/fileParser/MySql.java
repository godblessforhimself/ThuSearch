import org.apache.commons.lang3.StringEscapeUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.UnknownHostException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MySql {
    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    static final String DB_URL = "jdbc:mysql://localhost:3306/SchoolSearch?characterEncoding=UTF-8&useSSL=false";
    static final String USER = "root";
    static final String PASS = "";
    Connection conn = null;
    Statement stmt = null;
	public PreparedStatement p_insertpdf = null;
	public PreparedStatement p_insertdoc = null;
	public PreparedStatement p_insert8 = null;
	public PreparedStatement p_selectpdf = null;
	public PreparedStatement p_selectdoc = null;
	public PreparedStatement p_selecthtml = null;
    public static String[] tableName = {"pdf", "doc", "html"};
	MySql() 
	{
		getConnection();
		getStatement();
		System.out.println("Database initalize finish!");
    }
	public void getConnection() {
		try {
            Class.forName("com.mysql.jdbc.Driver");
            System.out.println("连接数据库...");
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
        } catch(ClassNotFoundException e){
            System.out.println("加载数据库驱动失败:\n"+e.toString());
            return;
        }
        catch (SQLException e) {
            System.out.println("获取连接失败" + e.toString());
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
			p_selectpdf = conn.prepareStatement("SELECT * FROM pdf;");
			p_selecthtml = conn.prepareStatement("SELECT * FROM html;",ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
			p_selecthtml.setFetchDirection(ResultSet.FETCH_REVERSE); 
			p_selecthtml.setFetchSize(Integer.MIN_VALUE);
			p_selectdoc = conn.prepareStatement("SELECT * FROM doc;");
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
					"(url VARCHAR(255) not null primary key, " +
					"type VARCHAR(4), " +
					"content MEDIUMTEXT) DEFAULT CHARSET=utf8mb4";
			stmt.executeUpdate(sql);
			sql = "CREATE TABLE IF NOT EXISTS doc " +
					"(url VARCHAR(255) not null primary key, " +
					"type VARCHAR(4), " +
					"content MEDIUMTEXT) DEFAULT CHARSET=utf8mb4";
			stmt.executeUpdate(sql);
			sql = "CREATE TABLE IF NOT EXISTS html " +
					"(url VARCHAR(255) not null primary key, " +
					"type VARCHAR(4), " +
					"title VARCHAR(255), " +
					"h1 VARCHAR(255), " + 
					"h2 VARCHAR(255), " +
					"content MEDIUMTEXT, " +
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
			e.printStackTrace();
		}
	}
    public ResultSet read(String sqlSelect){
        ResultSet rst = null;
        try{
            rst = stmt.executeQuery(sqlSelect);
            System.out.println("查询成功");
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
        }
        catch(SQLException e){
            System.out.println(e.toString());
        }
        return nRecord;
    }
    public Map<String,Double> getPR()
    {
    	Map<String,Double> m = new HashMap<String,Double>();
    	long t0 = System.currentTimeMillis();
    	try{
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream("pagerank.txt"),"utf-8"));
			String line="";
			while ((line = in.readLine()) != null)
			{
				//System.out.println(line);
				//Thread.sleep(10000);
				int p1 = line.indexOf(" ");
				int p2 = line.indexOf(" ", p1 + 1);
				String url = line.substring(p2 + 1);
				Double pagerank = Double.valueOf(line.substring(p1 + 1, p2));
				//System.out.println("url="+url + " pagerank = " +pagerank);
				
				m.put(url, pagerank);
			}
			in.close();
			System.out.println("get pagerank: time " + (System.currentTimeMillis() - t0)/1000 + " seconds");
		}catch (Exception e){
			e.printStackTrace();
		}
    	return m;
    }
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
	public static String processString(String s) 
	{
        s = s.replace( "&", "&amp;" );
		s = s.replace( "<", "&lt;" );
		s = s.replace( ">", "&gt;" );
		s = s.replace( "\"", "&quot;" );
		s = s.replace( "\'", "&apos;" );
		s = s.replaceAll("[\\000]+", "");
		s = s.replaceAll("\\s{2,}","\n");
		StringBuffer out = new StringBuffer(); // Used to hold the output.
		char current; // Used to reference the current character.
		if (s == null || ("".equals(s))) 
			return ""; // vacancy test.
		for (int i = 0; i < s.length(); i++) 
		{
			current = s.charAt(i); // NOTE: No IndexOutOfBoundsException caught here; it should not happen.
			if ((current == 0x9) ||
					(current == 0xA) ||
					(current == 0xD) ||
					((current >= 0x20) && (current <= 0xD7FF)) ||
					((current >= 0xE000) && (current <= 0xFFFD)) ||
					((current >= 0x10000) && (current <= 0x10FFFF)))
				out.append(current);
		}
		return out.toString();
	}
	public BufferedWriter write2file(String filename)
	{
		File file = new File(filename);
		try{
			if (!file.exists())
				file.createNewFile();
			OutputStream fw=new FileOutputStream(file);
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fw,"utf-8"));
			bw.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
			bw.write("<pics>\n" +
					"\t<category name=\""+filename.replace(".xml", "")+"\">\n");
			return bw;
		}catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
	public void end(BufferedWriter bw)
	{
		try {
			bw.write("</category>\n" +
			        "</pics>");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        try {
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void write2xml()
	{
		String f1 = "SchoolSearch_pdf.xml";
		String f2 = "SchoolSearch_doc.xml";
		String f3 = "SchoolSearch_html.xml";
		try{
			long t0 = System.currentTimeMillis();
			int cnt = 0;
			ResultSet rst = p_selectpdf.executeQuery();
			BufferedWriter bw = write2file(f1);
	        while (rst.next()) 
			{
	            String url = processString(rst.getString("url"));
	            String content = processString(rst.getString("content"));
	            bw.write("<pic url=\""+url+"\" type=\"pdf\" content=\""+content+ "\" />\n");
				cnt += 1;
	        }
	        end(bw);
	        bw = write2file(f2);
	        rst = p_selectdoc.executeQuery();
	        while (rst.next()) 
			{
	            String url = processString(rst.getString("url"));
	            String content = processString(rst.getString("content"));
	            bw.write("<pic url=\"" + url + "\" type=\"doc\" content=\"" + content + "\" />\n");
				cnt += 1;
	        }
			end(bw);
			rst = p_selecthtml.executeQuery();
			Map<String,Double> pagerank = getPR();
			t0 = System.currentTimeMillis();
			bw = write2file(f3);
			while (rst.next())
			{
	            String url = rst.getString("url");
	            String content = processString(rst.getString("content"));
				String title = processString(rst.getString("title"));
				String h1 = processString(rst.getString("h1"));
				String h2 = processString(rst.getString("h2"));
				String anchor = processString(rst.getString("outlinks"));
				Double pr = pagerank.get(url);
				url = processString(url);
				if (pr == null)
				{
					System.out.println(url + ": pagerank not found");
					pr = 1E-7;
				}
				bw.write("<pic url=\""+url+"\" type=\"html\" title=\""+title+"\" h1=\""+h1+"\" h2=\""+h2+"\" content=\""+content+ "\" anchor=\"" + anchor + "\" pagerank=\""+String.valueOf(pr)+"\" />\n");
				cnt += 1;
				if (cnt % 10000 == 0)
				{
					System.out.println(cnt+":time " + (System.currentTimeMillis() - t0) / 1000 + " seconds");
				}
	        }
			end(bw);
		}catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	public static Map<Integer,String> loadMap()
	{
		Map<Integer,String> m = new HashMap<Integer,String>();
		try{
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream("mapping.txt"),"utf-8"));
			String line="";
			while ((line = in.readLine()) != null)
			{
				String part[] = line.split(":");
				Integer src = Integer.valueOf(part[0]);
				m.put(src,part[1]);
			}
			in.close();
		}catch (Exception e){
			e.printStackTrace();
		}
		return m;
	}
	public static ArrayList<ArrayList> loadEdge()
	{
		ArrayList<ArrayList> m = new ArrayList<ArrayList>();
		try{
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream("graph.txt"),"utf-8"));
			String line="";
			while ((line = in.readLine()) != null)
			{
				ArrayList<Integer> a = new ArrayList<Integer>();
				String part[] = line.split("->");
				Integer src = Integer.valueOf(part[0]);
				a.add(src);
				if (part.length==1)
					continue;
				String[] dsts = part[1].split(",");
				for (String dst: dsts)
				{
					a.add(Integer.valueOf(dst));
				}
				if (a.size() > 1)
				{
					m.add(a);
					//System.out.println(a.toString());
				}
			}
			in.close();
		}catch (Exception e){
			e.printStackTrace();
		}
		return m;
	}
	public void buildMap_Graph()
	{
		try
		{
			long t0 = System.currentTimeMillis();
			PreparedStatement p = conn.prepareStatement("SELECT url,outlinks FROM html;");
			ResultSet rst = p.executeQuery();
			System.out.println("Query use " + (System.currentTimeMillis() - t0)/1000 + " seconds");
			t0=System.currentTimeMillis();
			
			Map<String,Integer> map = new HashMap<String,Integer>();
			ArrayList<String> indexing = new ArrayList<String>();
			
			File graph = new File("graph.txt");
			if (!graph.exists())
				graph.createNewFile();
			OutputStream fw=new FileOutputStream(graph);
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fw,"utf-8"));
			int count = 0;
			int loop = 0;
			while (rst.next())
			{
				String forGraph = "";
				String url = rst.getString("url");
				Integer mapindex;
				if ((mapindex = map.get(url)) != null)
				{
					forGraph += Integer.toString(mapindex) + "->";
				}
				else
				{
					map.put(url, count);
					indexing.add(url);
					forGraph += Integer.toString(count) + "->";
					count += 1;
				}
				String outlinks = rst.getString("outlinks");
				if (outlinks.equals(""))
					continue;
				String links[] = outlinks.split("\n");
				for (String link: links)
				{
					if (link.equals(""))
						continue;
					int towrite = 0;
					Integer i = map.get(link);
					if (i == null)
					{
						map.put(link, count);
						indexing.add(link);
						towrite = count;
						count += 1;
					}
					else
					{
						towrite = i;
					}
					if (forGraph.endsWith("->"))
					{
						forGraph += Integer.toString(towrite);
					}
					else
					{
						forGraph += "," + Integer.toString(towrite);
					}
				}
				bw.write(forGraph + "\n");
				loop += 1;
				/*if (loop % 5000 == 0)
				{
					System.out.println(loop + ":time passed " + (System.currentTimeMillis() - t0) / 1000 + " seconds");
				}*/
			}
			p.close();
			rst.close();
			bw.close();
			fw.close();
			File mapping = new File("mapping.txt");
			if (!mapping.exists())
				mapping.createNewFile();
			fw=new FileOutputStream(mapping);
			bw = new BufferedWriter(new OutputStreamWriter(fw,"utf-8"));
			for (int i = 0; i < indexing.size(); i ++)
			{
				bw.write(Integer.toString(i) + ":" + indexing.get(i) + "\n");
			}
			bw.close();
			fw.close();
			System.out.println("build graph and map use " + (System.currentTimeMillis() - t0) / 1000 + " seconds");
		}catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	public void calculate_pagerank()
	{
		try{
			double alpha = 0.15;
			int TN = 30;
			long t0 = System.currentTimeMillis();
			Map<Integer,String> mapping = loadMap();
			int N = mapping.size();
			int[] outdegrees = new int[N];
			for (int i = 0; i < N; i ++)
			{
				outdegrees[i] = 0;
			}
			double[] PRs = new double[N];
			double[] I = new double[N];
			double S = 0;
			ArrayList<ArrayList> edges = loadEdge();
			for (int i = 0, n = edges.size(); i < n; i ++)
			{
				ArrayList<Integer> edge = edges.get(i);
				Integer src = edge.get(0);
				int outdegree = edge.size() - 1;
				outdegrees[src] = outdegree;
			}
			for (int i = 0; i < N; i ++)
			{
				PRs[i] = 1 / (double)N;
				I[i] = alpha / (double) N;
				if (outdegrees[i] == 0)
				{
					S += PRs[i];
				}
			}
			System.out.println("time " + (System.currentTimeMillis() - t0) / 1000 + " seconds");
			t0 = System.currentTimeMillis();
			for (int k = 1; k <= TN; k ++)
			{
				for (int i = 0, n = edges.size(); i < n; i ++)
				{
					ArrayList<Integer> edge = edges.get(i);
					Integer src = edge.get(0);
					int outdegree = edge.size() - 1;
					for (int j = 0; j < outdegree; j ++)
					{
						Integer dst = edge.get(j + 1);
						I[dst] += (1 - alpha) * PRs[src] / (double) outdegrees[src];
					}
				}
				S = 0.0;
				for (int i = 0; i < N; i ++)
				{
					PRs[i] = I[i] + (1 - alpha) * S / N;
					I[i] = alpha / (double) N;
					if (outdegrees[i] == 0)
					{
						S += PRs[i];
					}
				}
				//System.out.println("k = " + k + " time " + (System.currentTimeMillis() - t0) / 1000 + " seconds");
			}
			File pagerank = new File("pagerank.txt");
			if (!pagerank.exists())
				pagerank.createNewFile();
			OutputStream fw=new FileOutputStream(pagerank);
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fw,"utf-8"));
			for (int i = 0; i < N; i ++)
			{
				bw.write(Integer.toString(i) + " " + Double.toString(PRs[i]) + " " + mapping.get(i) + "\n");
			}
			bw.close();
			fw.close();
			System.out.println("calculating pagerank use " + (System.currentTimeMillis() - t0) / 1000 + " seconds");
		}catch (Exception e){
			e.printStackTrace();
		}
	}
	public void updatePR2DB()
	{
		try{
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream("pagerank.txt"),"utf-8"));
			String str = "";
			PreparedStatement p = conn.prepareStatement("UPDATE html SET pagerank = ? WHERE url = ?;");
			long t0 = System.currentTimeMillis();
			int cnt = 0;
			while ((str = in.readLine()) != null)
			{
				String[] part = str.split(" ");
				String url = part[2];
				double d = Double.valueOf(part[1]);
				float pagerank = (float)d;
				p.setFloat(1, pagerank);
				p.setString(2, url);
				p.executeUpdate();
				cnt += 1;
				if (cnt % 500 == 0)
				{
					System.out.println(cnt + ":use " + (System.currentTimeMillis() - t0)/1000 + " seconds");
				}
			}
			System.out.println("updatePR2DB use " + (System.currentTimeMillis() - t0)/1000 + " seconds");
			in.close();
		}catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	public static void main(String[] args) throws SQLException {
        MySql ms = new MySql();
		//ms.buildMap_Graph();
        //ms.calculate_pagerank();
		ms.write2xml();
        
	}
}
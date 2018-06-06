import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.POIXMLDocument;
import org.apache.poi.POIXMLTextExtractor;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.xmlbeans.impl.common.IOUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class fileParser {
	public static int PDF = 0;
	public static int WORD = 1;
	public static int HTML = 2;
	private static String[] pdf = {"pdf"};
	private static String[] word = {"doc", "docx"};
	private static String[] html = {"html", "htm"};
	public int pdfcnt = 0;
	public int wordcnt = 0;
	public int htmlcnt = 0;
	public int totalcnt = 0;
	public MySql ms = null;
	public fileParser()
	{
		ms = new MySql();
	}
	public void parse(String path)
	{
		int index = path.lastIndexOf(".") + 1;
		int first = path.lastIndexOf("/") + 1;
		if (index == 0)
		{
			System.out.println("file type error:" + path);
		}
		String suffix = path.substring(index);
		for (String i: pdf)
		{
			if (suffix.equalsIgnoreCase(i))
			{
				runPDFParser(path);
				return;
			}
		}
		for (String i: word)
		{
			if (suffix.equalsIgnoreCase(i))
			{
				runDocParser(path);
				return;
			}
		}
		for (String i: html)
		{
			if (suffix.equalsIgnoreCase(i))
			{
				runHtmlParser(path);
				return;
			}
		} 
		//System.out.println("File Type:" + path);
		return;
	}
	public void runPDFParser(String path)
	{
		try {
			PDDocument document = PDDocument.load(new File(path));
			PDFTextStripper stripper = new PDFTextStripper();
			StringWriter writer = new StringWriter();
            stripper.writeText(document, writer);

            String content = writer.getBuffer().toString();
			writer.close();
			//System.out.println(content);
			document.close();
			if (content != null)
			{
				PreparedStatement p = ms.p_insertpdf;
				
				p.setString(1, path);
				p.setString(2, "pdf");
				p.setString(3, content);
				//System.out.println(p.toString());
				p.executeUpdate();
				
					
				/*String sql = "INSERT INTO pdf " +
						"VALUES (" + wrapContent(path) + "," +
						wrapContent("pdf") + "," +
						wrapContent(content) + ")";
				ms.update(sql);*/
			}
			pdfcnt += 1;
			totalcnt += 1;
		}
		catch (Exception e)
		{
			//e.printStackTrace();
			System.out.println("runPDFParser for file: " +path + " failed!");
		}
	}
	public void runDocParser(String path)
	{
		String content = null;
		try
		{
			if (path.charAt(path.length()-1)=='x' || path.charAt(path.length()-1)=='X')
			{
				XWPFWordExtractor docx = new XWPFWordExtractor(POIXMLDocument.openPackage(path));
				/*OPCPackage opcPackage = POIXMLDocument.openPackage(path);  
		        POIXMLTextExtractor extractor = new XWPFWordExtractor(opcPackage);  */
		        content =  docx.getText(); 
		        docx.close();
			}
			else
			{
				WordExtractor ex = new WordExtractor(new FileInputStream(new File(path)));
				content = ex.getText();
				ex.close();
			}
			if (content != null)
			{
				PreparedStatement p = ms.p_insertdoc;
				
				p.setString(1, path);
				p.setString(2, "doc");
				p.setString(3, content);
				//System.out.print(p.toString());
				p.executeUpdate();
				
				/*String sql = "INSERT INTO doc " +
						"VALUES (" + wrapContent(path) + "," +
						wrapContent("doc") + "," +
						wrapContent(content) + ")";
				ms.update(sql);*/
			}
			wordcnt += 1;
			totalcnt += 1;
		}
		catch (Exception e)
		{
			//e.printStackTrace();
			System.out.println("runDocParser for file: " +path + " failed!");
		}
		
	}
	public String urlAfterMirror(String path)
	{
		int index = path.lastIndexOf("mirror/") + 7;
		return path.substring(index);
	}
	public boolean checkLink(String link)
	{
		if (!link.startsWith("http"))
			return false;
		return true;
	}
	public static String codec(String filename){
		String charset = "GBK";
		boolean st = isUtf(filename);
		if(st == true)
			charset = "UTF-8";
		return charset;
	}
	
	private static boolean isUtf(String filePath){
		try{
			FileInputStream fis=new FileInputStream(filePath);
			byte[] bbuf=new byte[1024];
			int L=-1;
			int status=0;
			int oneByteCount=0;
			int twoByteCount=0;
			int threeByteCount=0;
			int fourByteCount=0;
			int errorCount=0;
			while((L=fis.read(bbuf))!=-1){
				for (int i = 0; i <L; i++) {
					byte b=bbuf[i];
					switch (status) {
						case 0:
							if(b>=0&&b<=(byte)0x7F)
									oneByteCount++;
							else if(b>=(byte)0xC0&&b<=(byte)0xDF)
								status=2;
							else if(b>=(byte)0xE0&&b<=(byte)0XEF)
								status=4;
							else if(b>=(byte)0xF0&&b<=(byte)0xF7)
								status=7;
							else 
								errorCount++;
							break;
						case 1:
							break;
						case 2:
							if(b>=(byte)0x80&&b<=(byte)0xBF){
								twoByteCount++;
								status=0;
							}
							else{
								errorCount+=2;
								status=0;
							}
							break;
						case 3:
							break;
						case 4:
							if(b>=(byte)0x80&&b<=(byte)0xBF)
								status=5;
							else{
								errorCount+=2;
								status=0;
							}
							break;
						case 5:
							if(b>=(byte)0x80&&b<=(byte)0xBF){
								threeByteCount++;
								status=0;
							}
							else{
								errorCount+=3;
								status=0;
							}
							break;
						case 7:
							if(b>=(byte)0x80&&b<=(byte)0xBF){
								status=8;
							}
							else{
								errorCount+=2;
								status=0;
							}
							break;
						case 8:
							if(b>=(byte)0x80&&b<=(byte)0xBF){
								status=9;
							}
							else{
								errorCount+=3;
								status=0;
							}
							break;
						case 9:
							if(b>=(byte)0x80&&b<=(byte)0xBF){
								fourByteCount+=4;
								status=0;
							}
							else{
								errorCount++;
								status=0;
							}
							break;
						default:
							break;
					}
				}
			}
			fis.close();
			if(errorCount==0)
				return true;
			return false;
		}catch(IOException e){
			System.out.println(e);
			System.out.println("error when detect codec:" + filePath);
			return true;
		}
	}
	public void runHtmlParser(String path)
	{
		String input = "";
		String content = "";
		String charset = codec(path);
		String temp ="";
		try
		{
			StringBuilder contentBuilder = new StringBuilder();
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(path), charset));
		    String str;
		    while ((str = in.readLine()) != null) {
		        contentBuilder.append(str.replaceAll("\\s*", "") + "\n");
		    }
		    in.close();
		
			input = contentBuilder.toString();
			Document doc;
			doc = Jsoup.parse(input, urlAfterMirror(path));
			String type = "html";
			String title = (doc.title() == null) ? "" : doc.title();
			content = doc.text();
			Elements links = doc.getElementsByTag("a");
			String outlinks = "";
			HashSet<String> linkmap = new HashSet<String>();
			contentBuilder = new StringBuilder();
			for (Element i: links)
			{
				String linkhref = i.attr("href");
				if (linkmap.contains(linkhref) || linkhref.length() == 0 || !checkLink(linkhref))
				{
					continue;
				}
				linkmap.add(linkhref);
				contentBuilder.append(linkhref.replaceAll("\\s*", "") + "\n");
				
			}
			outlinks = contentBuilder.toString();
			String[] hlist = {"h1", "h2"};
			String h1 = "";
			String h2 = "";
			Elements tags = doc.getElementsByTag("h1");
			if (tags != null)
			{
				for (Element tag: tags)
				{
					h1 += tag.text() + "\n";
				}
			}
			Elements tags2 = doc.getElementsByTag("h2");
			if (tags2 != null)
			{
				for (Element tag: tags2)
				{
					h2 += tag.text() + "\n";
				}
			}
			/*title = coder(title);
			content = coder(content);
			h1 = coder(h1);
			h2 = coder(h2);
			outlinks = coder(outlinks);
			System.out.println("path:"+path);
			System.out.println("title:"+title);
			System.out.println("content:"+content);
			System.out.println("h1:"+h1);
			System.out.println("h2:"+h2);
			System.out.println("outlinks:"+outlinks);*/
			
			PreparedStatement p = ms.p_insert8;
			p.setString(1, path);
			p.setString(2, "html");
			p.setString(3, title);
			p.setString(4, h1);
			p.setString(5, h2);
			p.setString(6, content);
			p.setString(7, outlinks);
			p.setFloat(8, 0.0f);
			p.executeUpdate();
	
			
			/*String sql = "INSERT INTO html " +
						"VALUES (" + wrapContent(path) + "," +
						wrapContent("html") + "," +
						wrapContent(title) + "," +
						wrapContent(h1) + "," +
						wrapContent(h2) + "," +
						wrapContent(content) + ","
						+ wrapContent(outlinks) + ")";
			System.out.println(sql);
			ms.update(sql);*/
			htmlcnt += 1;
			totalcnt += 1;
		}
		catch (Exception e)
		{
			System.out.println(charset + " path:" + path);
			//System.out.println(input.substring(0,300));
			//System.out.println(temp.substring(0,300));
			/*try {
				System.out.println(new String(temp.getBytes("gb2312"),"utf8"));
			} catch (UnsupportedEncodingException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}*/
			//e.printStackTrace();
			//System.out.println("runHtmlParser for file: " +path + " failed!");
		}
		
	}
	public static void main(String[] args)
	{
		//String rootdir = "C:/resource/search_engine/mirror/academic.tsinghua.edu.cn";
		String rootdir = "C:/resource/search_engine/20180603112306/mirror";
		
		final fileParser p = new fileParser();
		//p.ms.DropAllTables();
		try {
			final long t0 = System.currentTimeMillis();
			Files.walkFileTree(Paths.get(rootdir), new SimpleFileVisitor<Path>()
			{
				@Override
			    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			        // TODO Auto-generated method stub
			        String dirname = dir.toString();
			        //System.out.println("visiting dir:" + dirname);
			        return FileVisitResult.CONTINUE;
			    }

			    @Override
			    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			        // TODO Auto-generated method stub
			        String filename = file.toString().replace("\\", "/");
			        if (p.totalcnt % 1000 == 0)
			        {
			        	System.out.println(p.totalcnt +": takes " + (System.currentTimeMillis() - t0) / 1000 + " seconds!");
			        	//t0 = System.currentTimeMillis();
			        }
					//System.out.println("parsing file:" + filename);
			        p.parse(filename);
			        return FileVisitResult.CONTINUE;
			    }
			});
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/*try
		{
			String sql = "select * from html";
			ResultSet rs = p.ms.read(sql);
			while (rs.next())
			{
				//System.out.println("get from database:");
				for (int i = 0; i < 7; i ++)
				{
					//System.out.println("info:"+(rs.getString(i + 1)));
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}*/
	}
}

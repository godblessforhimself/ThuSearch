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
import java.io.OutputStream;
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

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.POIXMLDocument;
import org.apache.poi.POIXMLTextExtractor;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
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
	public boolean testmode = true;
	public MySql ms = null;
	public BufferedWriter log = null;
	public fileParser()
	{
		if (ms == null && !testmode)
		{
			ms = new MySql();
		}
		try {
			log = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File("log.txt")),"utf-8"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run()
			{
				close();
			}
		});
	}
	public void parse(String path)
	{
		int index = path.lastIndexOf(".") + 1;
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
		return;
	}
	public void runPDFParser(String path)
	{
		String content = "";
		String url = path2url(path);
		try {
			PDDocument document = PDDocument.load(new File(path));
			PDFTextStripper stripper = new PDFTextStripper();
			StringWriter writer = new StringWriter();
            stripper.writeText(document, writer);
            content = writer.getBuffer().toString();//.replaceAll("[\\sa-zA-Z]+", "");
			writer.close();
			content = getChinese(content,PDF);
			document.close();
			if (content != null)
			{
				PreparedStatement p = ms.p_insertpdf;
				p.setString(1, url);
				p.setString(2, "pdf");
				p.setString(3, content);
				p.executeUpdate();
			}
			pdfcnt += 1;
			totalcnt += 1;
		}
		catch (Exception e)
		{
			try {
				log.write("PDF EXCEPTION:\n" + path + "\n" + e.toString() + "\n------------\n");
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}
	public void runDocParser(String path)
	{
		String content = null;
		String url = path2url(path);
		try
		{
			if (path.charAt(path.length()-1)=='x' || path.charAt(path.length()-1)=='X')
			{
				XWPFWordExtractor docx = new XWPFWordExtractor(POIXMLDocument.openPackage(path));
		        content =  docx.getText(); 
		        docx.close();
			}
			else
			{
				WordExtractor ex = new WordExtractor(new FileInputStream(new File(path)));
				content = ex.getText();
				ex.close();
			}
			content = getChinese(content,WORD);
			if (content != null)
			{
				PreparedStatement p = ms.p_insertdoc;
				p.setString(1, url);
				p.setString(2, "doc");
				p.setString(3, content);
				p.executeUpdate();
			}
			wordcnt += 1;
			totalcnt += 1;
		}
		catch (Exception e)
		{
			try {
				log.write("WORD EXCEPTION:\n" + path + "\n" + e.toString() + "\n------------\n");
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}
	public String urlAfterMirror(String path)
	{
		int start = path.lastIndexOf("mirror/") + 7;
		int end = path.lastIndexOf("/");
		return path.substring(start, end + 1);
	}
	public String path2url(String path)
	{
		int index = path.lastIndexOf("mirror") + 7;
		return path.substring(index);
	}
	public String checkLink(String link, String base)
	{
		if (link.startsWith("http"))
		{
			return link.replaceAll("http[s]*://", "");
		}
		if (link.contains("javascript") || link.contains(";") || link.equals("") || link.equals("#"))
		{
			return "";
		}
		return (base + link).replaceAll("//", "/");
	}
	public static String codec(String filename){
		String charset = "GBK";
		boolean st = isUtf(filename);
		if(st == true)
			charset = "UTF-8";
		return charset;
	}
	private static boolean isUtf(String filePath)
	{
		try
		{
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
			//System.out.println(e);
			System.out.println("error when detect codec:" + filePath);
			return true;
		}
	}
	public static String getChinese(String str, int kind)
	{
		String src = "";
		String chineseRegex = "";
		if (kind == PDF)
		{
			src = str.replaceAll("\r", "\n").replaceAll("\\s{2,}", "\n");
			chineseRegex = "([\u4e00-\u9fa5£¬¡££¿£¡£º£»¡¶¡·0-9]+)";
		}
		else if (kind == WORD)
		{
			src = str.replaceAll("\r", "\n").replaceAll("\\s{2,}", "\n");
			chineseRegex = "([\u4e00-\u9fa5£¬¡££¿£¡£º£»¡¶¡·0-9\n]+)";
		}
		else if (kind == HTML)
		{
			src = str;
			chineseRegex = "([\u4e00-\u9fa5]+)";
		}
		
		Pattern reg = Pattern.compile(chineseRegex);
		Matcher matcher = reg.matcher(src);
		StringBuffer buffer = new StringBuffer();
		while (matcher.find())
		{
			buffer.append(matcher.group(1));
		}
		return buffer.toString();
	}
	public void runHtmlParser(String path)
	{
		String input = "";
		String content = "";
		String charset = codec(path);
		String url = path2url(path);
		try
		{
			StringBuilder contentBuilder = new StringBuilder();
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(path), charset));
		    String str;
		    while ((str = in.readLine()) != null) {
		        contentBuilder.append(str + "\n");
		    }
		    in.close();
		    input = contentBuilder.toString().replaceAll("<!--.*?-->", "");
			Document doc;
			String baseUri = urlAfterMirror(path);
			doc = Jsoup.parse(input);
			String title = (doc.title() == null) ? "" : doc.title();
			int MAXTITLE = 255;
			if (title.length() > MAXTITLE)
			{
				title=title.substring(0,MAXTITLE);
			}
			content = getChinese(input,HTML);
			Elements links = doc.select("a[href]");
			HashSet<String> linkmap = new HashSet<String>();
			contentBuilder = new StringBuilder();
			for (Element i: links)
			{
				String linkhref = checkLink(i.attr("href"), baseUri).replaceAll("\\s+", "");
				if (linkmap.contains(linkhref) || linkhref.length() == 0)
					continue;
				linkmap.add(linkhref);
				contentBuilder.append(linkhref + "\n");
			}
			String outlinks = contentBuilder.toString();
			String h1 = "";
			String h2 = "";
			Elements tags = doc.getElementsByTag("h1");
			int H1MAX = 255;
			int H2MAX = 255;
			if (tags != null)
			{
				int l = 0;
				int temp = 0;
				for (Element tag: tags)
				{
					temp = tag.text().length() + 1;
					if (l + temp > H1MAX)
						break;
					l += temp;
					h1 += tag.text() + "\n";
				}
			}
			Elements tags2 = doc.getElementsByTag("h2");
			if (tags2 != null)
			{
				int l = 0;
				int temp = 0;
				for (Element tag: tags2)
				{
					temp = tag.text().length() + 1;
					if (l + temp > H2MAX)
						break;
					l += temp;
					h2 += tag.text() + "\n";
				}
			}
			PreparedStatement p = ms.p_insert8;
			if (url==null || title==null || h1 == null || h2 == null || content == null || outlinks == null)
			{
				System.out.println("null");
			}
			p.setString(1, url);
			p.setString(2, "html");
			p.setString(3, title);
			p.setString(4, h1);
			p.setString(5, h2);
			p.setString(6, content);
			p.setString(7, outlinks);
			p.setFloat(8, 0.0f);
			p.executeUpdate();
			htmlcnt += 1;
			totalcnt += 1;
		}
		catch (Exception e)
		{
			try {
				log.write("Html EXCEPTION:\n" + path + "\n" + e.toString() + "\n------------\n");
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}
	public void close()
	{
		if (ms != null)
		ms.closeAll();
		try {
			log.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static void main(String[] args)
	{
		final fileParser p = new fileParser();
		if (p.testmode)
		{
			try {
				String check = "check.txt";
				BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(check)));
				String path = "";
				while ((path = in.readLine()) != null)
				{
					p.parse(path);
				}
				in.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		else
		{
			String testset = "C:/resource/search_engine/20180603112306/mirror/alumni.sem.tsinghua.edu.cn";
			String allset = "C:/resource/search_engine/20180603112306/mirror";
			String rootdir = allset;
			try {
				p.ms.DropAllTables();
				p.ms.createTables();
				final long t0 = System.currentTimeMillis();
				Files.walkFileTree(Paths.get(rootdir), new SimpleFileVisitor<Path>()
				{
					@Override
				    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				        return FileVisitResult.CONTINUE;
				    }

				    @Override
				    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				        String filename = file.toString().replace("\\", "/");
				        if (p.totalcnt % 50 == 0)
				        {
				        	System.out.println(p.totalcnt +": takes " + (System.currentTimeMillis() - t0) / 1000 + " seconds!");
				        }
				        p.parse(filename);
				        return FileVisitResult.CONTINUE;
				    }
				});
				System.out.println("All takes " + (System.currentTimeMillis() - t0) / 1000 + " seconds!");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}finally
			{
				System.out.println("Total "+p.totalcnt+" files("+p.pdfcnt+" pdfs," + p.wordcnt+ " docs," + p.htmlcnt + " htmls)");
			}
		}
		p.close();
	}
}

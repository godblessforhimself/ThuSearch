import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

public class encodingTest {
	public static String[] mode = {"0","1","2","3","4","5","6","7","8","9","A","B","C","D","E","F"};
	public static void print(String s)
	{
		System.out.print(s);
	}
	public static void bytes2num(byte b[])
	{
		for (int i = 0; i < b.length; i ++)
		{
			int c = b[i] & 0xFF;
			int h = c / 16;
			int l = c % 16;
			System.out.print("0x" + mode[h] + mode[l] + " ");
		}
		System.out.println();
	}
	public static void str2bytes(String str)
	{
		bytes2num(str.getBytes());
	}
	public static void main(String args[]) throws Exception
	{
		/*这个文件是gbk编码的！byte[] b = "好".getBytes();
		bytes2num(b);
		b = "好".getBytes("unicode");
		bytes2num(b);
		byte[] c = {(byte) 0xBA,(byte) 0xC3};
		print(new String(c,"gb2312"));*/
		String files[] = {"utf8.txt","gbk.txt"};
		
		BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(files[0]),"utf-8"));
		String con1 = "";
		String con2 = "";
		String con3 = "这个文件是gbk编码的！";
		String str;
		while ((str = in.readLine()) != null)
		{
			con1 += str;
		}
		in.close();
		in = new BufferedReader(new InputStreamReader(new FileInputStream(files[1]),"gbk"));
		while ((str = in.readLine()) != null)
		{
			con2 += str;
		}
		in.close();
		System.out.println(con3.equals(con1));
		str2bytes(con1);
		str2bytes(con2);
		str2bytes(con3);
	}
}

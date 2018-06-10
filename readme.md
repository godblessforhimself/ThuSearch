# 校园搜索引擎

## 资源抓取

### Heritrix3.2.0配置：

#### Seed：使用尽可能多的种子
```
    <prop key="seeds.textSource.value">
# URLS HERE
http://news.tsinghua.edu.cn
http://www.cs.tsinghua.edu.cn/
http://graduate.tsinghua.edu.cn
http://member.artmuseum.tsinghua.edu.cn
http://search.tsinghua.edu.cn
http://ticket.artmuseum.tsinghua.edu.cn
http://tv.tsinghua.edu.cn
http://www.artmuseum.tsinghua.edu.cn
http://www.tsinghua.edu.cn
http://www.tuef.tsinghua.edu.cn
http://yz.tsinghua.edu.cn
http://zhaopin.rsc.tsinghua.edu.cn
http://student.tsinghua.edu.cn
http://www.dangjian.tsinghua.edu.cn
http://academic.tsinghua.edu.cn
http://myhome.tsinghua.edu.cn
http://career.tsinghua.edu.cn
http://pt.tsinghua.edu.cn
http://kyybgxx.cic.tsinghua.edu.cn/kybg/index.jsp
http://guofang.tsinghua.edu.cn
http://info.itc.tsinghua.edu.cn
http://thdag.cic.tsinghua.edu.cn
http://xsg.tsinghua.edu.cn
http://info.tsinghua.edu.cn
    </prop>
```
#### SurtsSource
```
<property name="surtsSource">
        <bean class="org.archive.spring.ConfigString">
         <property name="value">
          <value>
           # example.com
           # http://www.example.edu/path1/
           +http://(cn,edu,tsinghua,
          </value>
         </property> 
        </bean>
       </property>
 </bean>
 ```
 #### MatchesListRegexDecideRule:过滤掉非pdf,doc,html和图书馆的链接
 ```
 <bean class="org.archive.modules.deciderules.MatchesListRegexDecideRule">
          <property name="decision" value="REJECT"/>
      <property name="listLogicalOr" value="true" /> 
      <property name="regexList">
           <list>
                <value>^(?:(?!\.html)(?!\.htm)(?!\.pdf)(?!\.PDF)(?!\.doc)(?!\.docx)(?!\.DOC)(?!\.DOCX)(?!\.htmlx)(?!\.HTML)(?!\.HTM)(?!\.HTMLX).)+</value>
                <value>[\S]*lib.tsinghua.edu.cn[\S]*</value>
           <value>[\S]*166.111.120.[\S]*</value>
           </list>
          </property>
    </bean>
 ```
 #### 将默认的warcWriter改为MirorWritor
 ```
  <bean id="warcWriter" class="org.archive.modules.writer.MirrorWriterProcessor"> </bean>
 ```
 #### 资源抓取遇到的困难
   - 按照作业说明提供的1.x版本heritrix的配置抓取不到文档。
   - heritrix 3.x 配置只能通过修改crawl-beans.cxml文档。在建立heritrix 3.x源码版本，建立自己的抓取项目时遇到了一些依赖和maven配置的问题，暂未解决。
   - heritrix 3.x 抓取文档需要2-3天时间。
 ### 参考 https://github.com/ChenGuangbinTHU/THUSearchEngine/blob/master/WebPreprocess/crawler-beans.cxml
 
 ## 资源预处理
 
 ### 总体流程：抓取的资源在mirror目录中。遍历mirror目录，根据文件后缀(doc,pdf,html) ，调用不同的解析方法。解析出有效文本，进行格式处理后保存到mysql中。
      通过访问处理后的文本，生成三个xml文件，用于indexing步骤。
 #### fileParser.java 进行目录遍历，解析文本，存入mysql的java代码
   - main：Files.walkFileTree进行遍历目录，对于每个文件，调用fileParser p.parse(filename)
   - fileParser.parse(String path): 根据path的后缀，调用runPDFParser(path)\runDocParser(path)\runHtmlParser(path)或直接返回;
   - fileParser.runPDFParser(String path): 使用apache PDFBox解析PDF格式文件，将该pdf对应的url,content,type存入mysql ;
   - fileParser.runDocParser(String path): 根据doc文件的版本，分别调用POI库中不同的解析方法获得word文档的文本内容。将word文档的url,content,type存入mysql。
   - fileParser.runHtmlParser(String path): 使用Jsoup提取html中的title,h1,h2,a href内容。打开文档前，用codec(path)判断编码。提取前，用正则表达式过滤掉html中的注释内容。content：getChinese获取html中的中文部分。将url,title,h1,h2,content,outlinks，type存入mysql。title,h1,h2作了长度限制。
      
#### MySql.java 提供了mysql的接口，main负责读取数据库，pagerank的图和映射文件的生成，pagerank的计算，indexing需要的xml文件的生成。
   - MySql(): 初始化时与SchoolSearch数据库建立连接，然后初始化statement。
   - createTables(): 创建pdf,doc,html三个表
   - DropAllTables(): 删除pdf,doc,html三个表
   - update(String sql):用语句sql更新数据库。
   - buildMap_Graph(): 遍历mysql schoolsearch.html,根据每项的url,outlinks建立mapping.txt和graph.txt文件。
   - calculate_pagerank(): 根据mapping.txt graph.txt，进行pagerank计算，写入pagerank.txt文件。
   - write2xml(): 读取schoolsearch html，pdf，doc三个表，分别生成三个xml文件
    
#### 资源预处理遇到的问题
  - Jsoup存在bug，解析html时：解析了注释的代码；返回正文时，把正文和javascript代码一起返回了。
  - 所以预处理时，先过滤了注释的内容，content不直接通过Jsoup获取，而是手动解析中文部分。
  - 编码问题：一部分网页的编码和<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />中设置的不同。如academic.tsinghua.edu.cn/index.html。实际上编码是gbk
    通过使用fileParser.codec(String filename)->fileParser.isUtf(String filename)先判断编码，再用文件流指定编码读入文件。
  - 抓取的一些pdf\doc文档损坏。暂时不记录这些损坏文件的数据。未解决。
  - 抓取的一些doc文件缺失一些css,无法通过POI解析。暂未解决。
  - 遍历一次mirror/下的文件，约3小时。可以并行提速。
  
 
 
 
 
 

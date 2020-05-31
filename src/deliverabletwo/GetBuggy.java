package deliverabletwo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class GetBuggy {
	
	// Variabili usato solo per evitare di duplicare piu volte la stessa stringa nel codice(considerato smell)
	private static final String JAVA_FILES = ".java";

	private static final String PROJ = "tajo";
	private static final String PROJ_FILES = PROJ+"fileInProject.csv";
	private static final String GIT_COMMITS = PROJ+"gitCommits.csv";
	private static final String VERSIONS_INFO = "VersionInfo.csv";
	private static final String BUGGY_FILES = PROJ+"buggyfiles.csv";
	private static final String BUG_AND_VERSIONS = PROJ+"bugs&versions.csv";
	private static final String BUGGY_METRIC = PROJ+"buggyMetrics.csv";
	private static final String FILES_INFO = PROJ+"filesInfo.csv";
	private static final String FINAL_METRICS = PROJ+"finalMetrics.csv";
	private static final String OAUTH = "C:\\Users\\" + "malav\\Desktop\\isw2\\oauth.txt";
	private static final String BASE_URL = "https://api.github.com/repos/apache/";
	
	
private static String readAll(Reader rd) throws IOException {
	      StringBuilder sb = new StringBuilder();
	      int cp;
	      while ((cp = rd.read()) != -1) {
	         sb.append((char) cp);
	      }
	      return sb.toString();
}

public static String getOAUTHToken() throws IOException {
	String token;
	try(BufferedReader oauthReader = new BufferedReader(new FileReader(OAUTH));){
	 	   token = oauthReader.readLine();
	    }
	return token;
}

public static JSONArray readJsonArrayAuth(String url) throws IOException, JSONException {
    URL url1 = new URL(url);
    URLConnection uc = url1.openConnection();
    uc.setRequestProperty("X-Requested-With", "Curl");
    String username =  "malavasiale";
    String token =  getOAUTHToken();
    String userpass = username + ":" + token;
    byte[] encodedBytes = Base64.getEncoder().encode(userpass.getBytes());
    String basicAuth = "Basic " + new String(encodedBytes);
    uc.setRequestProperty("Authorization", basicAuth);

    InputStreamReader inputStreamReader = new InputStreamReader(uc.getInputStream());
    try(BufferedReader rd = new BufferedReader(inputStreamReader);){
 	   return new JSONArray(readAll(rd));
    } finally {
       inputStreamReader.close();
    }
 }

public static JSONObject readJsonObjectAuth(String url) throws IOException, JSONException {
    URL url1 = new URL(url);
    URLConnection uc = url1.openConnection();
    uc.setRequestProperty("X-Requested-With", "Curl");
    String username =  "malavasiale";
    String token =  getOAUTHToken();
    String userpass = username + ":" + token;
    byte[] encodedBytes = Base64.getEncoder().encode(userpass.getBytes());
    String basicAuth = "Basic " + new String(encodedBytes);
    uc.setRequestProperty("Authorization", basicAuth);

    InputStreamReader inputStreamReader = new InputStreamReader(uc.getInputStream());
    try(BufferedReader rd = new BufferedReader(inputStreamReader);){
 	   return new JSONObject(readAll(rd));
    } finally {
       inputStreamReader.close();
    }
 }

public static JSONArray readJsonArrayFromUrl(String url) throws IOException, JSONException {
  InputStream is = new URL(url).openStream();
  try(BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));) {
     String jsonText = readAll(rd);
     return new JSONArray(jsonText);
   } finally {
     is.close();
   }
}

public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
  InputStream is = new URL(url).openStream();
  try(BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
     String jsonText = readAll(rd);
     return new JSONObject(jsonText);
   } finally {
     is.close();
   }
}

public static Long firstHalfVersions() throws IOException {	
	
	String project = PROJ.toUpperCase();
	try(Stream<String> a = Files.lines(Paths.get(project+VERSIONS_INFO), Charset.defaultCharset());) {
		Long versions = a.count();
		return (versions-1)/2;
	}
}

public static void retrieveGitCommits() throws IOException, InterruptedException {
	Integer i = 1;
	Integer k;
	String msg;
	String sha;
	try(FileWriter csvWriter = new FileWriter(GIT_COMMITS);) {
		csvWriter.append("message;sha\n");
		for(;;i++) {
			String url = BASE_URL+PROJ+"/commits?page="+i.toString()+"&per_page=50";
			Thread.sleep(1000);
			JSONArray json = readJsonArrayAuth(url);
			Integer l = json.length();
			if(l == 0) {
				return;
			}
			for( k=0 ; k<l ; k++ ) {
				msg = json.getJSONObject(k).getJSONObject("commit").getString("message");
				sha = json.getJSONObject(k).getString("sha");
				csvWriter.append(msg.replaceAll("\\n", "-").replaceAll("\\r", "-"));
				csvWriter.append(";");
				csvWriter.append(sha);
				csvWriter.append("\n");
	  		}
		}	
		
	}
	
}

public static String getOV(String created) throws IOException {
	String rowDate;
	Integer ov = 0;
	Integer count = 1; 
	String[] a = created.split("T");

	try(RandomAccessFile ra = new RandomAccessFile(PROJ.toUpperCase()+VERSIONS_INFO,"rw");){
		ra.readLine();
		while((rowDate = ra.readLine()) != null ) {
			String[] z = rowDate.split(",");
		    if(a[0].compareTo(z[3]) < 0) {
		    	ov = count-1;
		    	break;
		    }
		    count++;
		}
	}
	if(ov == 0) {
		ov = 1;
	}
	return ov.toString();
}

public static List<String> retrieveTick() throws JSONException, IOException {
	   Integer j = 0; 
	   Integer i = 0;
	   Integer total = 1;
	   String ov;
	   String fields = "fields";
	   
	   List<String> ids = new ArrayList<>();
	   try(RandomAccessFile ra = new RandomAccessFile(PROJ.toUpperCase()+VERSIONS_INFO,"rw");){
		   do {
			   j = i + 1000;
			   String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"
					   + PROJ.toUpperCase() + "%22AND%22issueType%22=%22Bug%22AND%22resolution%22=%22fixed%22&fields=key,fixVersions,versions,created&startAt="
					   + i.toString() + "&maxResults=" + j.toString();
			   JSONObject json = readJsonFromUrl(url);
			   JSONArray issues = json.getJSONArray("issues");
			   total = json.getInt("total");
			   for (; i < total && i < j; i++) {
				   //Iterate through each bug
				   String key = issues.getJSONObject(i%1000).get("key").toString();
				   String created = issues.getJSONObject(i%1000).getJSONObject(fields).getString("created");
				   JSONArray affectedVersions = issues.getJSONObject(i%1000).getJSONObject(fields).getJSONArray("versions");
				   //get all AV in jira if there are
				   StringBuilder av = getAVJira(affectedVersions,ra);
				   
				   JSONArray fixedVersions = issues.getJSONObject(i%1000).getJSONObject(fields).getJSONArray("fixVersions");
				   //get all FV in jira if there are
				   StringBuilder fv = getFVJira(fixedVersions,ra);
				   
				   //get OV from Jira creation ticket date
				   ov = getOV(created);
				   
				   ids.add(key + av.toString() + fv.toString() + ";" + ov);
			   } 
		   } while (i < total);
	   }
	   return ids;
}

public static StringBuilder getFVJira(JSONArray fixedVersions,RandomAccessFile ra) throws IOException {
	StringBuilder fv = new StringBuilder();
	   fv.append(";");
	   for(int k = 0; k<fixedVersions.length();k++) {
		   String line = "";
		   String versID = "";
		   String name = fixedVersions.getJSONObject(k).get("name").toString();
		   while ((line = ra.readLine()) != null) {

             String[] row = line.split(",");
             if(row[2].equals(name)) {
             	versID = row[0];
             	break;
             }
        }
		   ra.seek(0);
		   if(k == 0 && !versID.equals("")) {
			   fv.append(versID);
		   }
		   else if (!versID.equals("") ) {
			   fv.append("*"+versID);
		   }
	   }
	   if(fixedVersions.length() == 0) {
		   fv.append("none");
	   }
	   return fv;
}

public static StringBuilder getAVJira(JSONArray affectedVersions,RandomAccessFile ra) throws IOException {
	   StringBuilder av = new StringBuilder();
	   av.append(";");
	   for(int k = 0; k<affectedVersions.length();k++) {
			String line = "";
			String versID = "";
			String name = affectedVersions.getJSONObject(k).get("name").toString();				
	        while ((line = ra.readLine()) != null) {
	                String[] row = line.split(",");
	                if(row[2].equals(name)) {
	                	versID = row[0];    	
	                	break;
	                }
	       }
	       ra.seek(0);
		   if(k == 0 && !versID.equals("")) {
			   av.append(versID);
		   }
		   else if(!versID.equals("")) {
			   av.append("*" + versID);
		   }
	   }
	   if(affectedVersions.length() == 0 || av.toString().equals(";")) {
		   av.append("none");
	   }
	   return av;
}

public static void retrieveFiles() throws IOException, InterruptedException {
	String baseurl = BASE_URL+PROJ+"/contents";
	String path = "";
	List<String> checked = new ArrayList<>();
	List<String> directory = new ArrayList<>();
	
	try(FileWriter csvWriter = new FileWriter(PROJ_FILES);){
		do {
			JSONArray files = readJsonArrayAuth(baseurl+path);
			for(int i = 0 ; i<files.length() ; i++) {
				JSONObject file = files.getJSONObject(i);
				String filepath = file.getString("path");
				String type = file.getString("type");
				if(type.equals("file")) {
					checked.add(filepath);
				}
				else {
					directory.add(filepath);
				}
			}
			if(!directory.isEmpty()) {
				path = directory.get(0);
				directory.remove(0);
			}
			else{
				path = "";
			}
			Thread.sleep(1000);
		}while(!path.equals(""));
		
		csvWriter.append("filepath");
		csvWriter.append("\n");
		for(String s : checked) {
			csvWriter.append(s);
			csvWriter.append("\n");
		}
		csvWriter.flush();
	}
	
}

public static List<String> retrieveBuggyFiles(String sha) throws JSONException, IOException{
	List<String> buggyfiles = new ArrayList<>();
	String url = BASE_URL+PROJ+"/commits/" + sha;
	JSONObject commit = readJsonObjectAuth(url);
	JSONArray filesarray = commit.getJSONArray("files");
	for(int i = 0; i < filesarray.length(); i++) {
		JSONObject file = filesarray.getJSONObject(i);
		String filename = file.getString("filename");
		if(StringUtils.countMatches(filename, JAVA_FILES) >= 1) {
			buggyfiles.add(filename);
		}
	}
	return buggyfiles;
}

public static void commitsBuggyClasses() throws IOException {
	String rowBugs;
	String rowCommits;
	String avmin;
	String fvmin;
	Integer match1;
	Integer match2;
	Integer match3;
	Integer meanP = 0;
	Integer countP = 0;	
	
	try(FileWriter csvBuggyFilesWriter = new FileWriter(BUGGY_FILES);
		BufferedReader csvReaderBugs = new BufferedReader(new FileReader(BUG_AND_VERSIONS ));
		RandomAccessFile csvReaderCommits = new RandomAccessFile(GIT_COMMITS,"r");){
		
		while ((rowBugs = csvReaderBugs.readLine()) != null) {
		    String[] dataBugs = rowBugs.split(";");
		   while((rowCommits = csvReaderCommits.readLine()) != null) {
		    	String[] dataCommits = rowCommits.split(";");
		    	match1 = StringUtils.countMatches(dataCommits[0], "[" + dataBugs[0] + "]");
		    	match2 = StringUtils.countMatches(dataCommits[0], dataBugs[0] + ":");
		    	match3 = StringUtils.countMatches(dataCommits[0], dataBugs[0] + " ");
		    	if(match1 >= 1 || match2 >= 1 || match3 >= 1) {
		    		
		    		//Find minimum AV from the list
		    		avmin = findMinVersion(dataBugs[1]);
		    		
		    		//Find minimun FV from the list
		    		fvmin = findMinVersion(dataBugs[2]);
		    		
		    		List<String> buggyfiles = retrieveBuggyFiles(dataCommits[1]);
		    		if(!avmin.equals("none") && !fvmin.equals("none")) {
		    			//Calculate incremental proportion witch tickets that have AV and FV
		    			List<Integer> incremental = proportionIncremental(dataBugs[0],avmin,fvmin,dataBugs[3],meanP,countP);
		    			meanP = incremental.get(0);
		    			countP = incremental.get(1);
		    		}
		    		else {
		    			//Prediction of AV with incremental proportion
		    			avmin = predictAV(fvmin,meanP,dataBugs[3],dataBugs[0]);
		    		}
		    		
		    		//Write in a file all the buggy classes of a ticket
		    		buggyFilesRowWriter(dataBugs[0],avmin,fvmin,dataCommits[1],csvBuggyFilesWriter,buggyfiles);
		    		
		    		csvReaderCommits.seek(csvReaderCommits.length());
		    	}	
		    }
	    	csvReaderCommits.seek(0);
		}
	}
}

public static void buggyFilesRowWriter(String tickID,String avmin,String fvmin,String sha,FileWriter csvBuggyFilesWriter,List<String> buggyfiles) throws IOException {
	StringBuilder bugss = new StringBuilder();
	bugss.append(tickID + ";" + avmin+ ";" + fvmin + ";" + sha);
	for(String elem : buggyfiles) {
		bugss.append(";" + elem);
	}
	csvBuggyFilesWriter.write(bugss.toString()+"\n");
}

public static List<Integer> proportionIncremental(String tickID,String avmin,String fvmin,String ov,Integer meanP,Integer countP) {
	Logger l = Logger.getLogger(GetBuggy.class.getName());
	String logStr;
	Integer p;
	List<Integer> incremental = new ArrayList<>();
	
		if(Integer.parseInt(fvmin) == Integer.parseInt(ov)) {
			p = 0;
			countP++;
		}
		else {
			p = (Integer.parseInt(fvmin) - Integer.parseInt(avmin))/(Integer.parseInt(fvmin) - Integer.parseInt(ov));
			countP++;
		}
		if(p < 0) {
			p = 0;
			countP--;
			logStr ="Jumping : " +  fvmin + " " + avmin;
			l.log(Level.INFO, logStr );
			incremental.add(meanP);
			incremental.add(countP);
			return incremental; //jump who have FV < AV
		}
		meanP = (meanP + p)/(countP);
		logStr = tickID + ", P : " + p.toString()+ ", MeanP : " + meanP.toString();
		l.log(Level.INFO,logStr);
		
		incremental.add(meanP);
		incremental.add(countP);
		return incremental;
}

public static String predictAV(String fvmin,Integer meanP,String ov,String tickID) {
	String avmin = "none";
	String logStr;
	Logger l = Logger.getLogger(GetBuggy.class.getName());
	
	if(!fvmin.equals("none")) {
		Integer predicted = (Integer.parseInt(fvmin) - meanP *(Integer.parseInt(fvmin) - Integer.parseInt(ov)));
		avmin = predicted.toString();
		logStr = tickID + ", Predicted IV :  " + avmin; 
		l.log(Level.INFO,logStr);
	}
	return avmin;
}

public static String findMinVersion(String versionsString) {
	String minVers;
	List<String> listVers = new ArrayList<>();
	
	if(!versionsString.contentEquals("none") && StringUtils.countMatches(versionsString, "*") >= 1) {
		String[] versionsArray = versionsString.split("\\*");
		Collections.addAll(listVers, versionsArray);
		minVers = Collections.min(listVers);
	}
	else if(!versionsString.contentEquals("none")) {
		minVers = versionsString;
	}
	else {
		minVers = "none";
	}
	return minVers;
}


public static void buggyMetric() throws IOException {
	String rowFile;
	String rowBuggyFile;
	Long maxVersion;
	Integer av;
	Integer fv;
	Boolean buggy = false;
	Logger l = Logger.getLogger(GetBuggy.class.getName());
	
	maxVersion = firstHalfVersions();
	
	try(FileWriter csvMetrics = new FileWriter(BUGGY_METRIC);
		BufferedReader csvReaderFiles = new BufferedReader(new FileReader(PROJ_FILES));
		RandomAccessFile csvReaderBuggyFiles = new RandomAccessFile(BUGGY_FILES,"r");){
		
		csvMetrics.write("Version;File;Buggy\n");
		
		while((rowFile = csvReaderFiles.readLine()) != null) {
			l.log(Level.INFO, "Cecking file : {0} ",rowFile);
			if(StringUtils.countMatches(rowFile, JAVA_FILES) >= 1) {
				for(Integer v = 1; v <= maxVersion;v++) {
					while((rowBuggyFile = csvReaderBuggyFiles.readLine()) != null) {
						String[] listOfFiles = rowBuggyFile.split(";");
						if(listOfFiles.length > 4) {
							for(int i = 4; i < listOfFiles.length; i++) {
								if(listOfFiles[i].equals(rowFile)) {
									if(!listOfFiles[1].equals("none") && !listOfFiles[2].equals("none") ) {
										av = Integer.parseInt(listOfFiles[1]);
										fv = Integer.parseInt(listOfFiles[2]);
										if(v >= av && v < fv) {
											buggy = true;
											break;
										}
										else {
											buggy = false;
										}
									}
									else {
										buggy = false;
									}
								}
							}
						}
						if(Boolean.TRUE.equals(buggy)) {
							break;
						}
					}
					if(Boolean.TRUE.equals(buggy)) {
						csvMetrics.write(v.toString()+";"+rowFile+";"+"yes\n");
					}
					else {
						csvMetrics.write(v.toString()+";"+rowFile+";"+"no\n");
					}
					buggy = false;
					csvReaderBuggyFiles.seek(0);
				}
			}
		}
	}
}

public static void filesInfo() throws IOException {
	String row;
	String author;
	Integer added;
	Integer deleted;
	
	Long maxVersion = firstHalfVersions();
	
	try(RandomAccessFile csvReaderBuggyFiles = new RandomAccessFile(BUGGY_FILES,"r");
		FileWriter csvFilesInfo = new FileWriter(FILES_INFO);){
		
		csvFilesInfo.write("Version;Filename;LOC_Added;LOC_deleted;Set_size;Author\n");
		
		while((row = csvReaderBuggyFiles.readLine()) != null) {
			String[] rowSplit = row.split(";");
			if(!rowSplit[2].equals("none") && Integer.parseInt(rowSplit[2]) <= maxVersion) {
				String url = BASE_URL+PROJ+"/commits/" + rowSplit[3];
				JSONObject commit = readJsonObjectAuth(url);
				author = commit.getJSONObject("commit").getJSONObject("author").getString("name");
				JSONArray files = commit.getJSONArray("files");
				for(int i = 0; i < files.length();i++) {
					JSONObject file = files.getJSONObject(i);
					String filename = file.getString("filename");
					if(StringUtils.countMatches(filename, JAVA_FILES) >= 1) {
						added = file.getInt("additions");
						deleted = file.getInt("deletions");
						csvFilesInfo.write(rowSplit[2] + ";" + filename + ";" + added + ";" + deleted + ";" + files.length() + ";" + author + "\n");
					}
				}
			}
		}
	}
}

public static void makeMetricsFile() throws IOException {
	String rowBuggy;
	String filename;
	String version;
	String buggy;
	Integer avglocAdded=0;
	Integer avgChurn=0;
	Integer avgSetsize=0;
	
	try(RandomAccessFile csvReaderFilesInfo = new RandomAccessFile(FILES_INFO,"r");
		RandomAccessFile csvReaderMetrics = new RandomAccessFile(BUGGY_METRIC,"r");
		FileWriter csvMetrics = new FileWriter(FINAL_METRICS);){
		
		csvReaderMetrics.readLine();
		
		csvMetrics.write("Version;Filename;LOC_touched;LOC_added;MAXLOC_added;AVGLOC_added;churn;MAX_churn;AVG_churn;Change_setsize;MAXChange_setsize;AVGChange_setsize;Buggy\n");
		
		while((rowBuggy = csvReaderMetrics.readLine()) != null) {
			String[] a = rowBuggy.split(";");
			version = a[0];
			filename = a[1];
			buggy = a[2];
			
			//Calculate metrics of every file
			List<Integer> metrics = calculateMetrics(version,filename,csvReaderFilesInfo);
			Integer count = metrics.get(7);
			if(count != 0) {
				avglocAdded = metrics.get(1)/count;
				avgChurn = metrics.get(3)/count;
				avgSetsize = metrics.get(5)/count;
			}
			
			csvMetrics.write(version + ";" + filename + ";" + metrics.get(0) + ";" + metrics.get(1) + ";" + metrics.get(2) + ";" + avglocAdded + ";" + metrics.get(3) + ";" + avgChurn + ";" + metrics.get(4) + ";" + metrics.get(5) + ";" + metrics.get(6) + ";" + avgSetsize + ";" + buggy + "\n");
			csvReaderFilesInfo.seek(0);
			avglocAdded = 0;
			avgChurn = 0;
			avgSetsize = 0;
		}
	}
}

public static List<Integer> calculateMetrics(String version,String filename,RandomAccessFile csvReaderFilesInfo) throws IOException{
	String rowInfo;
	Integer locTouched = 0;
	Integer locAdded = 0;
	Integer maxlocAdded = 0;
	Integer churn = 0;
	Integer maxChurn = 0;
	Integer setsize = 0;
	Integer maxSetsize = 0;

	Integer count = 0;
	List<Integer> metrics = new ArrayList<>();
	
	try {
		while((rowInfo = csvReaderFilesInfo.readLine()) != null) {
			String[] b = rowInfo.split(";");
			if(b[0].equals(version) && b[1].equals(filename)) {
				locTouched = locTouched + Integer.parseInt(b[2]) + Integer.parseInt(b[3]);
				locAdded = locAdded + Integer.parseInt(b[2]);
				if(maxlocAdded < Integer.parseInt(b[2])) {
					maxlocAdded = Integer.parseInt(b[2]);
				}
				count++;
				churn = churn + Integer.parseInt(b[2]) - Integer.parseInt(b[3]);
				if(maxChurn < Integer.parseInt(b[2]) - Integer.parseInt(b[3])) {
					maxChurn = Integer.parseInt(b[2]) - Integer.parseInt(b[3]);
				}
				setsize = setsize + Integer.parseInt(b[4]);
				if(maxSetsize < Integer.parseInt(b[4])) {
					maxSetsize = Integer.parseInt(b[4]);
				}
			}
		}
	} catch (Exception e) {
		e.printStackTrace();
	}

	metrics.add(locTouched);
	metrics.add(locAdded);
	metrics.add(maxlocAdded);
	metrics.add(churn);
	metrics.add(maxChurn);
	metrics.add(setsize);
	metrics.add(maxSetsize);
	metrics.add(count);
	
	return metrics;
}

	public static void main(String[] args) throws IOException, InterruptedException {
		List<String> tickets = new ArrayList<>();
		Logger l = Logger.getLogger(GetBuggy.class.getName());
		
		File csvFile = new File(PROJ_FILES);
		if(!csvFile.isFile()) {
			l.log(Level.INFO, "Downloading list of files");
			retrieveFiles();
		}
		
		
		File versionsInfo = new File(PROJ.toUpperCase()+VERSIONS_INFO);
		if(!versionsInfo.isFile()) {
			l.log(Level.INFO, "Downloading versions info of the project");
			GetReleaseInfo.getInfo();
		}
		
		File bugAndVer = new File(BUG_AND_VERSIONS);
		if(!bugAndVer.isFile()) {
			l.log(Level.INFO, "Downloading list of bug and versions");
			tickets = retrieveTick();
			try(FileWriter bugAndVers = new FileWriter(BUG_AND_VERSIONS);){
				bugAndVers.append("TicketID;AV;FV;OV\n");
				for(String s : tickets) {
					bugAndVers.append(s+"\n");
				}
			}
		}
		
		File gitCommits = new File(GIT_COMMITS);
		if(!gitCommits.isFile()) {
			l.log(Level.INFO, "Downloading list of commits from git");
			retrieveGitCommits();
		}
		
		// retrieve buggy file for every commit
		File buggyFiles = new File(BUGGY_FILES);
		if(!buggyFiles.isFile()) {
			l.log(Level.INFO,"Finding buggy files for every ticket");
			commitsBuggyClasses();
		}
		
		File buggyMetricsFile = new File(BUGGY_METRIC);
		if(!buggyMetricsFile.isFile()) {
			l.log(Level.INFO,"Calculating buggy metrics");
			buggyMetric();
		}
		
		File filesInfo = new File(FILES_INFO);
		if(!filesInfo.isFile()) {
			l.log(Level.INFO,"retrieving files info");
			filesInfo();
		}
		
		
		File finalMetrics = new File(FINAL_METRICS);
		if(!finalMetrics.isFile()) {
			l.log(Level.INFO,"Calculating final metrics");
			makeMetricsFile();
		}
		
	}
}

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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;




public class GetBuggy {
	
	// Variabili usato solo per evitare di duplicare piu volte la stessa stringa nel codice(considerato smell)
	private static final String JAVA_FILES = ".java";

	private static final String PROJ_FILES = "fileInProject.csv";
	private static final String GIT_COMMITS = "gitCommits.csv";
	private static final String TAJO_VERSIONS_INFO = "TAJOVersionInfo.csv";
	private static final String BUGGY_FILES = "buggyfiles.csv";
	private static final String BUG_AND_VERSIONS = "bugs&versions.csv";
	private static final String BUGGY_METRIC = "buggyMetrics.csv";
	private static final String FILES_INFO = "filesInfo.csv";
	private static final String FINAL_METRICS = "finalMetrics.csv";
	private static final String OAUTH = "C:\\Users\\" + "malav\\Desktop\\isw2\\oauth.txt";
	
	
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

public static void retrieveGitCommits() throws IOException, InterruptedException {
	Integer i = 1;
	Integer k;
	String msg;
	String sha;
	try(FileWriter csvWriter = new FileWriter(GIT_COMMITS);) {
		csvWriter.append("message;sha\n");
		for(;;i++) {
			String url = "https://api.github.com/repos/apache/tajo/commits?page="+i.toString()+"&per_page=50";
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

	try(RandomAccessFile ra = new RandomAccessFile(TAJO_VERSIONS_INFO,"rw");){
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
	   String projName ="TAJO";
	   Integer j = 0; 
	   Integer i = 0;
	   Integer total = 1;
	   String ov;
	   String fields = "fields";
	   
	   List<String> ids = new ArrayList<>();
	   try(RandomAccessFile ra = new RandomAccessFile(TAJO_VERSIONS_INFO,"rw");){
		   do {
			   j = i + 1000;
			   String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"
					   + projName + "%22AND%22issueType%22=%22Bug%22AND%22resolution%22=%22fixed%22&fields=key,fixVersions,versions,created&startAt="
					   + i.toString() + "&maxResults=" + j.toString();
			   JSONObject json = readJsonFromUrl(url);
			   JSONArray issues = json.getJSONArray("issues");
			   total = json.getInt("total");
			   for (; i < total && i < j; i++) {
				   //Iterate through each bug
				   String key = issues.getJSONObject(i%1000).get("key").toString();
				   String created = issues.getJSONObject(i%1000).getJSONObject(fields).getString("created");
				   JSONArray affectedVersions = issues.getJSONObject(i%1000).getJSONObject(fields).getJSONArray("versions");
				   String av =";";
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
						   av = av + versID;
					   }
					   else if(!versID.equals("")) {
						   av = av +"*"+ versID;
					   }
				   }
				   if(affectedVersions.length() == 0 || av.equals(";")) {
					   av = av + "none";
				   }
				   JSONArray fixedVersions = issues.getJSONObject(i%1000).getJSONObject(fields).getJSONArray("fixVersions");
				   String fv = ";";
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
						   fv = fv + versID;
					   }
					   else if (!versID.equals("") ) {
						   fv = fv +"*"+ versID;
					   }
				   }
				   if(fixedVersions.length() == 0) {
					   fv = fv + "none";
				   }
				   ov = getOV(created);
				   ids.add(key+av+fv + ";" + ov);
			   } 
		   } while (i < total);
	   }
	   return ids;
}

public static void retrieveFiles() throws IOException, InterruptedException {
	String baseurl = "https://api.github.com/repos/apache/tajo/contents";
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
	String url = "https://api.github.com/repos/apache/tajo/commits/" + sha;
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
	String logStr;
	
	Integer match1;
	Integer match2;
	Integer match3;
	Integer p = 0;
	Integer meanP = 0;
	Integer countP = 0;
	List<String> avlist = new ArrayList<>();
	List<String> fvlist = new ArrayList<>();
	Logger l = Logger.getLogger(GetBuggy.class.getName());
	
	
	
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
		    		if(!dataBugs[1].contentEquals("none") && StringUtils.countMatches(dataBugs[1], "*") >= 1) {
		    			String[] avarray = dataBugs[1].split("\\*");
		    			Collections.addAll(avlist, avarray);
		    			avmin = Collections.min(avlist);
		    		}
		    		else if(!dataBugs[1].contentEquals("none")) {
		    			avmin = dataBugs[1];
		    		}
		    		else {
		    			avmin = "none";
		    		}
		    		if(!dataBugs[2].contentEquals("none") && StringUtils.countMatches(dataBugs[2], "*") >= 1) {
		    			String[] fvarray = dataBugs[2].split("\\*");
		    			Collections.addAll(fvlist, fvarray);
		    			fvmin = Collections.min(fvlist);
		    		}
		    		else if(!dataBugs[2].contentEquals("none")){
		    			fvmin = dataBugs[2];
		    		}
		    		else {
		    			fvmin = "none";
		    		}
		    		List<String> buggyfiles = retrieveBuggyFiles(dataCommits[1]);
		    		if(!avmin.equals("none") && !fvmin.equals("none")) {
		    			if(Integer.parseInt(fvmin) == Integer.parseInt(dataBugs[3])) {
		    				p = 0;
			    			countP++;
		    			}
		    			else {
		    				p = (Integer.parseInt(fvmin) - Integer.parseInt(avmin))/(Integer.parseInt(fvmin) - Integer.parseInt(dataBugs[3]));
			    			countP++;
		    			}
		    			if(p < 0) {
		    				p = 0;
		    				countP--;
		    				logStr ="Jumping : " +  fvmin + " " + avmin;
		    				l.log(Level.INFO, logStr );
		    				continue; //jump who have FV < AV
		    			}
		    			meanP = (meanP + p)/(countP);
		    			logStr = dataBugs[0]+ ", P : " + p.toString()+ ", MeanP : " + meanP.toString();
		    			l.log(Level.INFO,logStr);
		    		}
		    		else {
		    			if(!fvmin.equals("none")) {
		    				Integer predicted = (Integer.parseInt(fvmin) - meanP *(Integer.parseInt(fvmin) - Integer.parseInt(dataBugs[3])));
		    				avmin = predicted.toString();
		    				logStr = dataBugs[0]+ ", Predicted IV :  " + avmin; 
		    				l.log(Level.INFO,logStr);
		    			}
		    		}
		    		String bugss = dataBugs[0] + ";" + avmin+ ";" + fvmin + ";" + dataCommits[1];
		    		for(String elem : buggyfiles) {
		    			bugss = bugss + ";" + elem;
		    		}
		    		csvBuggyFilesWriter.write(bugss);
		    		csvBuggyFilesWriter.write("\n");
		    		csvReaderCommits.seek(csvReaderCommits.length());
		    	}	
		    }
	    	csvReaderCommits.seek(0);
		}
	}

}

public static void buggyMetric() throws IOException {
	String rowFile;
	String rowBuggyFile;
	Integer versions = 0;
	Integer maxVersion;
	Integer av;
	Integer fv;
	Boolean buggy = false;
	Logger l = Logger.getLogger(GetBuggy.class.getName());
	
	try(FileWriter csvMetrics = new FileWriter(BUGGY_METRIC);
		BufferedReader csvReaderVersions = new BufferedReader(new FileReader(TAJO_VERSIONS_INFO));
		BufferedReader csvReaderFiles = new BufferedReader(new FileReader(PROJ_FILES));
		RandomAccessFile csvReaderBuggyFiles = new RandomAccessFile(BUGGY_FILES,"r");){
		
		csvMetrics.write("Version;File;Buggy\n");
		
		while((csvReaderVersions.readLine()) != null) {
			versions++;
		}
		maxVersion = (versions-1)/2;
		
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
	
	try(RandomAccessFile csvReaderBuggyFiles = new RandomAccessFile(BUGGY_FILES,"r");
		FileWriter csvFilesInfo = new FileWriter(FILES_INFO);){
		
		csvFilesInfo.write("Version;Filename;LOC_Added;LOC_deleted;Set_size;Author\n");
		
		while((row = csvReaderBuggyFiles.readLine()) != null) {
			String[] rowSplit = row.split(";");
			if(!rowSplit[2].equals("none") && Integer.parseInt(rowSplit[2]) <= 5) {
				//TODO: METTERE VERSIONE NON STATICA
				String url = "https://api.github.com/repos/apache/tajo/commits/" + rowSplit[3];
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
	
	Integer loc_touched = 0;
	Integer loc_added = 0;
	Integer maxloc_added = 0;
	Integer avgloc_added = 0;
	Integer churn = 0;
	Integer mac_churn = 0;
	Integer avg_churn = 0;
	Integer setsize = 0;
	Integer max_setsize = 0;
	Integer avg_setsize = 0;
	Integer count = 0;
	String rowBuggy;
	String rowInfo;
	String filename;
	String version;
	String buggy;
	
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
			while((rowInfo = csvReaderFilesInfo.readLine()) != null) {
				String[] b = rowInfo.split(";");
				if(b[0].equals(version) && b[1].equals(filename)) {
					loc_touched = loc_touched + Integer.parseInt(b[2]) + Integer.parseInt(b[3]);
					loc_added = loc_added + Integer.parseInt(b[2]);
					if(maxloc_added < Integer.parseInt(b[2])) {
						maxloc_added = Integer.parseInt(b[2]);
					}
					count++;
					churn = churn + Integer.parseInt(b[2]) - Integer.parseInt(b[3]);
					if(mac_churn < Integer.parseInt(b[2]) - Integer.parseInt(b[3])) {
						mac_churn = Integer.parseInt(b[2]) - Integer.parseInt(b[3]);
					}
					setsize = setsize + Integer.parseInt(b[4]);
					if(max_setsize < Integer.parseInt(b[4])) {
						max_setsize = Integer.parseInt(b[4]);
					}
				}
			}
			if(count != 0) {
				avgloc_added = loc_added/count;
				avg_churn = churn/count;
				avg_setsize = setsize/count;
			}
			csvMetrics.write(version + ";" + filename + ";" + loc_touched.toString() + ";" + loc_added.toString() + ";" + maxloc_added.toString() + ";" + avgloc_added.toString() + ";" + churn.toString() + ";" + mac_churn.toString() + ";" + avg_churn.toString() + ";" + setsize.toString() + ";" + max_setsize.toString() + ";" + avg_setsize.toString() + ";" + buggy + "\n");
			loc_touched = 0;
			loc_added = 0;
			maxloc_added = 0;
			avgloc_added = 0;
			churn = 0;
			mac_churn = 0;
			avg_churn = 0;
			setsize = 0;
			max_setsize = 0;
			avg_setsize = 0;
			count = 0;
			csvReaderFilesInfo.seek(0);
		}
		csvReaderFilesInfo .close();
		csvReaderMetrics.close();
		csvMetrics.close();
	}
}

	public static void main(String[] args) throws IOException, InterruptedException {
		List<String> tickets = new ArrayList<>();
		
		File csvFile = new File(PROJ_FILES);
		if(!csvFile.isFile()) {
			System.out.println("Downloading list of files");
			retrieveFiles();
		}
		
		
		File versionsInfo = new File(TAJO_VERSIONS_INFO);
		if(!versionsInfo.isFile()) {
			System.out.println("Downloading versions info of the project");
			GetReleaseInfo.getInfo();
		}
		
		File bugAndVer = new File(BUG_AND_VERSIONS);
		if(!bugAndVer.isFile()) {
			System.out.println("Downloading list of bug and versions");
			tickets = retrieveTick();
			try(FileWriter bugAndVers = new FileWriter(BUG_AND_VERSIONS);){
				bugAndVers.append("TicketID;AV;FV;OV\n");
				for(String s : tickets) {
					bugAndVers.append(s+"\n");
				}
				bugAndVers.flush();
				bugAndVers.close();
			}
		}
		
		File gitCommits = new File(GIT_COMMITS);
		if(!gitCommits.isFile()) {
			System.out.println("Downloading list of commits from git");
			retrieveGitCommits();
		}
		
		// retrieve buggy file for every commit
		File buggyFiles = new File(BUGGY_FILES);
		if(!buggyFiles.isFile()) {
			System.out.println("Finding buggy files for every ticket");
			commitsBuggyClasses();
		}
		
		File buggyMetricsFile = new File(BUGGY_METRIC);
		if(!buggyMetricsFile.isFile()) {
			System.out.println("Calculating buggy metrics");
			buggyMetric();
		}
		
		File filesInfo = new File(FILES_INFO);
		if(!filesInfo.isFile()) {
			System.out.println("retrieving files info");
			filesInfo();
		}
		
		
		File finalMetrics = new File(FINAL_METRICS);
		if(!finalMetrics.isFile()) {
			System.out.println("Calculating final metrics");
			makeMetricsFile();
		}
		
	}
}

package deliverabletwo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
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

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;



public class GetBuggy {
	

	private static final String PROJ_FILES = "fileInProject.csv";
	private static final String FINAL_DATA = "finalData.csv";
	
private static String readAll(Reader rd) throws IOException {
	      StringBuilder sb = new StringBuilder();
	      int cp;
	      while ((cp = rd.read()) != -1) {
	         sb.append((char) cp);
	      }
	      return sb.toString();
}

public static JSONArray readJsonArrayAuth(String url) throws IOException, JSONException {
    URL url_1 = new URL(url);
    URLConnection uc = url_1.openConnection();
    uc.setRequestProperty("X-Requested-With", "Curl");
    String username =  "malavasiale";
    String token =  "58fce61bdb38de01eed317fddc07ff0ea248edb4";
    String userpass = username + ":" + token;
    byte[] encodedBytes = Base64.getEncoder().encode(userpass.getBytes());
    String basicAuth = "Basic " + new String(encodedBytes);
    uc.setRequestProperty("Authorization", basicAuth);

    InputStreamReader inputStreamReader = new InputStreamReader(uc.getInputStream());
    try(BufferedReader rd = new BufferedReader(inputStreamReader);){
 	   JSONArray  jsonArray = new JSONArray(readAll(rd));
 	   return jsonArray;
    } finally {
       inputStreamReader.close();
    }
 }

public static JSONObject readJsonObjectAuth(String url) throws IOException, JSONException {
    URL url_1 = new URL(url);
    URLConnection uc = url_1.openConnection();
    uc.setRequestProperty("X-Requested-With", "Curl");
    String username =  "malavasiale";
    String token =  "58fce61bdb38de01eed317fddc07ff0ea248edb4";
    String userpass = username + ":" + token;
    byte[] encodedBytes = Base64.getEncoder().encode(userpass.getBytes());
    String basicAuth = "Basic " + new String(encodedBytes);
    uc.setRequestProperty("Authorization", basicAuth);

    InputStreamReader inputStreamReader = new InputStreamReader(uc.getInputStream());
    try(BufferedReader rd = new BufferedReader(inputStreamReader);){
 	   JSONObject  jsonObject = new JSONObject(readAll(rd));
 	   return jsonObject;
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
	try(FileWriter csvWriter = new FileWriter("gitCommits.csv");) {
		csvWriter.append("message;sha\n");
		for(;;i++) {
			String url = "https://api.github.com/repos/apache/tajo/commits?page="+i.toString()+"&per_page=50";
			Thread.sleep(1100);
			System.out.println("Pagina numero :"+i);
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
	Integer OV = 0;
	Integer count = 1; 
	String[] a = created.split("T");
	RandomAccessFile ra = new RandomAccessFile("TAJOVersionInfo.csv","rw");
	ra.readLine();
	while((rowDate = ra.readLine()) != null ) {
		String[] z = rowDate.split(",");
	    System.out.println(a[0] + " " + z[3] );
	    System.out.println(a[0].compareTo(z[3]));
	    if(a[0].compareTo(z[3]) < 0) {
	    	OV = count-1;
	    	break;
	    }
	    count++;
	}
	if(OV == 0) {
		OV = 1;
	}
	return OV.toString();
}

public static List<String> retrieveTick() throws JSONException, IOException {
	   String projName ="TAJO";
	   Integer j = 0; 
	   Integer i = 0;
	   Integer total = 1;
	   String OV;
	   List<String> ids = new ArrayList<>();
	   //BufferedReader br = new BufferedReader(new FileReader("TAJOVersionInfo.csv"));
	   RandomAccessFile ra = new RandomAccessFile("TAJOVersionInfo.csv","rw");
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
			   String created = issues.getJSONObject(i%1000).getJSONObject("fields").getString("created");
			   JSONArray affectedVersions = issues.getJSONObject(i%1000).getJSONObject("fields").getJSONArray("versions");
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
			   JSONArray fixedVersions = issues.getJSONObject(i%1000).getJSONObject("fields").getJSONArray("fixVersions");
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
			   OV = getOV(created);
			   ids.add(key+av+fv + ";" + OV);
		   } 
	   } while (i < total);
	   ra.close();
	   return ids;
}

public static void retrieveFiles() throws IOException, InterruptedException {
	String baseurl = "https://api.github.com/repos/apache/tajo/contents";
	String path = "";
	int count = 0;
	int total = 0;
	List<String> checked = new ArrayList<>();
	List<String> directory = new ArrayList<>();
	FileWriter csvWriter = new FileWriter(PROJ_FILES);
	
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
		if(directory.size() != 0) {
			path = directory.get(0);
			directory.remove(0);
		}
		else{
			path = "";
		}
		count++;
		total++;
		System.out.println("File :"+checked.toString());
		System.out.println("Dir : "+directory.toString());
		System.out.println(count +" and "+total);
		Thread.sleep(1100);
	}while(!path.equals(""));
	
	csvWriter.append("filepath");
	csvWriter.append("\n");
	for(String s : checked) {
		csvWriter.append(s);
		csvWriter.append("\n");
	}
	csvWriter.flush();
	csvWriter.close();
}

public static List<String> retrieveBuggyFiles(String sha) throws JSONException, IOException{
	List<String> buggyfiles = new ArrayList<>();
	String url = "https://api.github.com/repos/apache/tajo/commits/" + sha;
	JSONObject commit = readJsonObjectAuth(url);
	JSONArray filesarray = commit.getJSONArray("files");
	for(int i = 0; i < filesarray.length(); i++) {
		JSONObject file = filesarray.getJSONObject(i);
		String filename = file.getString("filename");
		if(StringUtils.countMatches(filename, ".java") >= 1) {
			buggyfiles.add(filename);
		}
	}
	return buggyfiles;
}

public static void commitsBuggyClasses() throws IOException {
	String rowBugs,rowCommits, AVmin, FVmin;
	FileWriter csvBuggyFilesWriter = new FileWriter("buggyfiles.csv");
	Integer match1,match2,match3;
	Integer P = 0;
	Integer meanP = 0;
	Integer countP = 0;
	List<String> bugs = new ArrayList<>();
	List<String> AVlist = new ArrayList<>();
	List<String> FVlist = new ArrayList<>();
	BufferedReader csvReaderBugs = new BufferedReader(new FileReader("bugs&versions.csv"));
	RandomAccessFile csvReaderCommits = new RandomAccessFile("gitCommits.csv","r");
	
	while ((rowBugs = csvReaderBugs.readLine()) != null) {
	    String[] dataBugs = rowBugs.split(";");
	    //System.out.println(dataBugs[0]);
	   while((rowCommits = csvReaderCommits.readLine()) != null) {
	    	String[] dataCommits = rowCommits.split(";");
	    	match1 = StringUtils.countMatches(dataCommits[0], "[" + dataBugs[0] + "]");
	    	match2 = StringUtils.countMatches(dataCommits[0], dataBugs[0] + ":");
	    	match3 = StringUtils.countMatches(dataCommits[0], dataBugs[0] + " ");
	    	//System.out.println(dataBugs[0] +" "+ match1.toString() +" " + match2.toString() + " " + match3.toString());
	    	if(match1 >= 1 || match2 >= 1 || match3 >= 1) {
	    		if(!dataBugs[1].contentEquals("none") && StringUtils.countMatches(dataBugs[1], "*") >= 1) {
	    			String[] AVarray = dataBugs[1].split("\\*");
	    			for(String s : AVarray) {
	    				AVlist.add(s);
	    			}
	    			AVmin = Collections.min(AVlist);
	    		}
	    		else if(!dataBugs[1].contentEquals("none")) {
	    			AVmin = dataBugs[1];
	    		}
	    		else {
	    			AVmin = "none";
	    		}
	    		if(!dataBugs[2].contentEquals("none") && StringUtils.countMatches(dataBugs[2], "*") >= 1) {
	    			String[] FVarray = dataBugs[2].split("\\*");
	    			for(String s : FVarray) {
	    				FVlist.add(s);
	    			}
	    			FVmin = Collections.min(FVlist);
	    		}
	    		else if(!dataBugs[2].contentEquals("none")){
	    			FVmin = dataBugs[2];
	    		}
	    		else {
	    			FVmin = "none";
	    		}
	    		List<String> buggyfiles = retrieveBuggyFiles(dataCommits[1]);
	    		if(!AVmin.equals("none") && !FVmin.equals("none")) {
	    			if(Integer.parseInt(FVmin) == Integer.parseInt(dataBugs[3])) {
	    				P = 0;
		    			countP++;
	    			}
	    			else {
	    				P = (Integer.parseInt(FVmin) - Integer.parseInt(AVmin))/(Integer.parseInt(FVmin) - Integer.parseInt(dataBugs[3]));
		    			countP++;
	    			}
	    			if(P < 0) {
	    				P = 0;
	    				countP--;
	    				System.out.println("Jumping " + FVmin + " " + AVmin);
	    				continue; //jump who have FV < AV
	    			}
	    			meanP = (meanP + P)/(countP);
	    			System.out.println(dataBugs[0] + " " + P.toString() + " " + meanP.toString() );
	    		}
	    		else {
	    			if(!FVmin.equals("none")) {
	    				Integer predicted = (Integer.parseInt(FVmin) - meanP *(Integer.parseInt(FVmin) - Integer.parseInt(dataBugs[3])));
	    				AVmin = predicted.toString();
	    				System.out.println(dataBugs[0] + " " + AVmin );
	    			}
	    		}
	    		String bugss = dataBugs[0] + ";" + AVmin+ ";" + FVmin + ";" + dataCommits[1];
	    		for(String elem : buggyfiles) {
	    			bugss = bugss + ";" + elem;
	    		}
	    		csvBuggyFilesWriter.write(bugss);
	    		csvBuggyFilesWriter.write("\n");
	    		//bugs.add(dataBugs[0] + ";" + AVmin+ ";" + FVmin + ";" + dataCommits[1]);
	    		csvReaderCommits.seek(0);
	    		break;
	    	}
	    }
    	csvReaderCommits.seek(0);
    	//System.out.println("done");
	}
	csvReaderCommits.close();
	csvReaderBugs.close();
	csvBuggyFilesWriter.close();
	//System.out.println(bugs.toString());
}

public static void buggyMetric() throws IOException {
	BufferedReader csvReaderVersions = new BufferedReader(new FileReader("TAJOVersionInfo.csv"));
	BufferedReader csvReaderFiles = new BufferedReader(new FileReader("fileInProject.csv"));
	RandomAccessFile csvReaderBuggyFiles = new RandomAccessFile("buggyfiles.csv","r");
	FileWriter csvMetrics = new FileWriter("buggyMetrics.csv");
	String rowVersions , rowFile,rowBuggyFile;
	Integer versions = 0;
	Integer maxVersion,AV,FV;
	Boolean buggy = false;
	
	csvMetrics.write("Version;File;Buggy\n");
	
	while((rowVersions = csvReaderVersions.readLine()) != null) {
		versions++;
	}
	maxVersion = (versions-1)/2;
	
	while((rowFile = csvReaderFiles.readLine()) != null) {
		System.out.println("Cecking file : " +rowFile);
		if(StringUtils.countMatches(rowFile, ".java") >= 1) {
			for(Integer v = 1; v <= maxVersion;v++) {
				while((rowBuggyFile = csvReaderBuggyFiles.readLine()) != null) {
					String[] listOfFiles = rowBuggyFile.split(";");
					if(listOfFiles.length > 4) {
						for(int i = 4; i < listOfFiles.length; i++) {
							if(listOfFiles[i].equals(rowFile)) {
								if(!listOfFiles[1].equals("none") && !listOfFiles[2].equals("none") ) {
									AV = Integer.parseInt(listOfFiles[1]);
									FV = Integer.parseInt(listOfFiles[2]);
									if(v >= AV && v < FV) {
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
					if(buggy) {
						break;
					}
				}
				if(buggy) {
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
	csvReaderVersions.close();
	csvReaderFiles.close();
	csvReaderBuggyFiles.close();
	csvMetrics.close();
}

public static void filesInfo() throws IOException {
	RandomAccessFile csvReaderBuggyFiles = new RandomAccessFile("buggyfiles.csv","r");
	FileWriter csvFilesInfo = new FileWriter("filesInfo.csv");
	String row;
	String author;
	Integer added,deleted;
	
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
				if(StringUtils.countMatches(filename, ".java") >= 1) {
					added = file.getInt("additions");
					deleted = file.getInt("deletions");
					csvFilesInfo.write(rowSplit[2] + ";" + filename + ";" + added + ";" + deleted + ";" + files.length() + ";" + author + "\n");
				}
			}
		}
	}
	csvReaderBuggyFiles.close();
	csvFilesInfo.close();
}

public static void makeMetricsFile() throws IOException {
	RandomAccessFile csvReaderFilesInfo = new RandomAccessFile("filesInfo.csv","r");
	RandomAccessFile csvReaderMetrics = new RandomAccessFile("buggyMetrics.csv","r");
	FileWriter csvMetrics = new FileWriter("finalMetrics.csv");
	Integer LOC_touched = 0;
	Integer LOC_added = 0;
	Integer MAXLOC_added = 0;
	Integer AVGLOC_added = 0;
	Integer churn = 0;
	Integer MAX_churn = 0;
	Integer AVG_churn = 0;
	Integer setsize = 0;
	Integer MAX_setsize = 0;
	Integer AVG_setsize = 0;
	Integer count = 0;
	String rowBuggy,rowInfo,filename,version,buggy;
	
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
				LOC_touched = LOC_touched + Integer.parseInt(b[2]) + Integer.parseInt(b[3]);
				LOC_added = LOC_added + Integer.parseInt(b[2]);
				if(MAXLOC_added < Integer.parseInt(b[2])) {
					MAXLOC_added = Integer.parseInt(b[2]);
				}
				count++;
				churn = churn + Integer.parseInt(b[2]) - Integer.parseInt(b[3]);
				if(MAX_churn < Integer.parseInt(b[2]) - Integer.parseInt(b[3])) {
					MAX_churn = Integer.parseInt(b[2]) - Integer.parseInt(b[3]);
				}
				setsize = setsize + Integer.parseInt(b[4]);
				if(MAX_setsize < Integer.parseInt(b[4])) {
					MAX_setsize = Integer.parseInt(b[4]);
				}
			}
		}
		if(count != 0) {
			AVGLOC_added = LOC_added/count;
			AVG_churn = churn/count;
			AVG_setsize = setsize/count;
		}
		csvMetrics.write(version + ";" + filename + ";" + LOC_touched.toString() + ";" + LOC_added.toString() + ";" + MAXLOC_added.toString() + ";" + AVGLOC_added.toString() + ";" + churn.toString() + ";" + MAX_churn.toString() + ";" + AVG_churn.toString() + ";" + setsize.toString() + ";" + MAX_setsize.toString() + ";" + AVG_setsize.toString() + ";" + buggy + "\n");
		LOC_touched = 0;
		LOC_added = 0;
		MAXLOC_added = 0;
		AVGLOC_added = 0;
		churn = 0;
		MAX_churn = 0;
		AVG_churn = 0;
		setsize = 0;
		MAX_setsize = 0;
		AVG_setsize = 0;
		count = 0;
		csvReaderFilesInfo.seek(0);
	}
	csvReaderFilesInfo .close();
	csvReaderMetrics.close();
	csvMetrics.close();
}

	public static void main(String[] args) throws IOException, InterruptedException {
		List<String> tickets = new ArrayList<>();
		
		File csvFile = new File(PROJ_FILES);
		if(!csvFile.isFile()) {
			System.out.println("Downloading list of files");
			retrieveFiles();
		}
		
		
		File versionsInfo = new File("TAJOVersionInfo.csv");
		if(!versionsInfo.isFile()) {
			System.out.println("Downloading versions info of the project");
			GetReleaseInfo.getInfo();
		}
		
		File bugAndVer = new File("bugs&versions.csv");
		if(!bugAndVer.isFile()) {
			System.out.println("Downloading list of bug and versions");
			tickets = retrieveTick();
			FileWriter bugAndVers = new FileWriter("bugs&versions.csv");
			bugAndVers.append("TicketID;AV;FV;OV\n");
			for(String s : tickets) {
				bugAndVers.append(s+"\n");
			}
			bugAndVers.flush();
			bugAndVers.close();
		}
		
		File gitCommits = new File("gitCommits.csv");
		if(!gitCommits.isFile()) {
			System.out.println("Downloading list of commits from git");
			retrieveGitCommits();
		}
		
		// retrieve buggy file for every commit
		File buggyFiles = new File("buggyfiles.csv");
		if(!buggyFiles.isFile()) {
			System.out.println("Finding buggy files for every ticket");
			commitsBuggyClasses();
		}
		
		File buggyMetricsFile = new File("buggyMetrics.csv");
		if(!buggyMetricsFile.isFile()) {
			System.out.println("Calculating buggy metrics");
			buggyMetric();
		}
		
		File filesInfo = new File("filesInfo.csv");
		if(!filesInfo.isFile()) {
			System.out.println("retrieving files info");
			filesInfo();
		}
		
		
		File finalMetrics = new File("finalMetrics.csv");
		if(!finalMetrics.isFile()) {
			System.out.println("Calculating final metrics");
			makeMetricsFile();
		}

		
	}
}
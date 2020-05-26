package deliverabletwo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;
import weka.core.converters.ConverterUtils.DataSource;

public class WekaTesting {
	
	private static List<String> finalData = new ArrayList<String>();
	
	private static final String PROJ = "tajo";
	private static final String METRICS = PROJ+"finalMetrics.csv";
	private static final String TRAINING_CSV = PROJ+"trainingSet.csv";
	private static final String TESTING_CSV = PROJ+"testingSet.csv";
	private static final String TRAINING_ARFF = PROJ+"trainingSet.arff";
	private static final String TESTING_ARFF = PROJ+"testingSet.arff";
	private static final String WEKA_OUTPUT = PROJ + "wekaOutput.csv";
	
	public static void makeSets(Integer version) throws IOException {
		String row;
		List<Integer> trainingVersions = new ArrayList<>();
		
		try(BufferedReader csvReader = new BufferedReader(new FileReader(METRICS));
			FileWriter csvTraining = new FileWriter(TRAINING_CSV);
			FileWriter csvTesting = new FileWriter(TESTING_CSV);){
			
			for(int i = 1; i <= version;i++) {
				trainingVersions.add(i);
			}
			
			row = csvReader.readLine();
			String a = row.replace(";", ",");
			csvTraining.write(a+"\n");
			csvTesting.write(a+"\n");

			
			while((row = csvReader.readLine()) != null) {
				String[] array = row.split(";");
				String replaced = row.replace(";", ",");
				if(trainingVersions.contains(Integer.parseInt(array[0]))) {
					csvTraining.write(replaced+"\n");
				}
				else if (Integer.parseInt(array[0]) == (version+1)) {
					csvTesting.write(replaced+"\n");
				}
			}
			
		}
		
		// load CSV
	    CSVLoader loader = new CSVLoader();
	    loader.setSource(new File(TRAINING_CSV));
	    Instances data = loader.getDataSet();

	    // save ARFF
	    ArffSaver saver = new ArffSaver();
	    saver.setInstances(data);
	    saver.setFile(new File(TRAINING_ARFF));
	    saver.writeBatch();
	    
	 // load CSV
	    CSVLoader loader2 = new CSVLoader();
	    loader2.setSource(new File(TESTING_CSV));
	    Instances data2 = loader2.getDataSet();

	    // save ARFF
	    ArffSaver saver2 = new ArffSaver();
	    saver2.setInstances(data2);
	    saver2.setFile(new File(TESTING_ARFF));
	    saver2.writeBatch();
		}
	
	public static void evaluate(Integer numbOfTraining) throws Exception {
		DataSource source1 = new DataSource(TRAINING_ARFF);
		Instances training = source1.getDataSet();
		DataSource source2 = new DataSource(TESTING_ARFF);
		Instances testing = source2.getDataSet();

		int numAttr = training.numAttributes();
		training.setClassIndex(numAttr - 1);
		testing.setClassIndex(numAttr - 1);

		IBk classifierIBk = new IBk();
		NaiveBayes classifierNB = new NaiveBayes();
		RandomForest classifierRF = new RandomForest();

		classifierIBk.buildClassifier(training);
		classifierNB.buildClassifier(training);
		classifierRF.buildClassifier(training);

		Evaluation evalIBk = new Evaluation(testing);
		Evaluation evalNB = new Evaluation(testing);
		Evaluation evalRF = new Evaluation(testing);

		evalIBk.evaluateModel(classifierIBk, testing); 
		evalNB.evaluateModel(classifierNB, testing); 
		evalRF.evaluateModel(classifierRF, testing); 
		
		finalData.add("Tajo;" + numbOfTraining + ";" + "IBk;" + evalIBk.precision(1) +";" + evalIBk.recall(1) +  ";" + evalIBk.areaUnderROC(1) + ";" + evalIBk.kappa() + "\n");
		finalData.add("Tajo;" + numbOfTraining + ";" + "NaiveBayes;" + evalNB.precision(1) +";" + evalNB.recall(1) +  ";" + evalNB.areaUnderROC(1) + ";" + evalNB.kappa() + "\n");
		finalData.add("Tajo;" + numbOfTraining + ";" + "RandomForest;" + evalRF.precision(1) +";" + evalRF.recall(1) +  ";" + evalRF.areaUnderROC(1) + ";" + evalRF.kappa() + "\n");
	}
	
	public static void main(String args[]) throws Exception{
		Long maxvers = GetBuggy.firstHalfVersions();
		
		for(int i = 1 ; i < maxvers ; i++) {
			makeSets(i);
			evaluate(i);
		}
		
		try(FileWriter csvEvaluate = new FileWriter(WEKA_OUTPUT);){
			csvEvaluate.write("Dataset;#TrainingRelease;Classifier;Precision;Recall;AUC;Kappa\n");
			
			for(String elem : finalData) {
				csvEvaluate.write(elem);
			}
		}		
	}
}

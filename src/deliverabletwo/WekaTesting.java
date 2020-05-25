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
	private static final Integer maxvers = 5;//TODO mettere versione massima dinamica
	private static List<String> finalData = new ArrayList<String>();
	
	public static void makeSets(Integer version) throws IOException {
		BufferedReader csvReader = new BufferedReader(new FileReader("finalMetrics.csv"));
		FileWriter csvTraining = new FileWriter("TAJOtrainingSet.csv");
		FileWriter csvTesting = new FileWriter("TAJOtestingSet.csv");
		String row;
		List<Integer> trainingVersions = new ArrayList<Integer>();
		
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
		
		csvReader.close();
		csvTraining.close();
		csvTesting.close();
		
		// load CSV
	    CSVLoader loader = new CSVLoader();
	    loader.setSource(new File("TAJOtrainingSet.csv"));
	    Instances data = loader.getDataSet();

	    // save ARFF
	    ArffSaver saver = new ArffSaver();
	    saver.setInstances(data);
	    saver.setFile(new File("TAJOtrainingSet.arff"));
	    //saver.setDestination(new File("TAJOtrainingSet.arff"));
	    saver.writeBatch();
	    
	 // load CSV
	    CSVLoader loader2 = new CSVLoader();
	    loader2.setSource(new File("TAJOtestingSet.csv"));
	    Instances data2 = loader2.getDataSet();

	    // save ARFF
	    ArffSaver saver2 = new ArffSaver();
	    saver2.setInstances(data2);
	    saver2.setFile(new File("TAJOtestingSet.arff"));
	    //saver2.setDestination(new File("TAJOtestingSet.arff"));
	    saver2.writeBatch();
		}
	
	public static void evaluate(Integer numbOfTraining) throws Exception {
		DataSource source1 = new DataSource("TAJOtrainingSet.arff");
		Instances training = source1.getDataSet();
		DataSource source2 = new DataSource("TAJOtestingSet.arff");
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
		
		/*System.out.println("---------Training on "+ numbOfTraining +" Versions ----------\n\n");
		System.out.println("AUC IBk = "+evalIBk.areaUnderROC(1));
		System.out.println("kappa IBk = "+evalIBk.kappa());
		System.out.println("Precision IBk = "+evalIBk.precision(1));
		System.out.println("Recall IBk = " + evalIBk.recall(1));
		System.out.println("\n------\n");
		System.out.println("AUC NB = "+evalNB.areaUnderROC(1));
		System.out.println("kappa NB = "+evalNB.kappa());
		System.out.println("Precision NB = "+evalNB.precision(1));
		System.out.println("Recall NB = " + evalNB.recall(1));
		System.out.println("\n------\n");
		System.out.println("AUC RF = "+evalRF.areaUnderROC(1));
		System.out.println("kappa RF = "+evalRF.kappa());
		System.out.println("Precision RF = "+evalRF.precision(1));
		System.out.println("Recall RF = " + evalRF.recall(1));
		System.out.println("\n------\n");*/
		finalData.add("Tajo;" + numbOfTraining + ";" + "IBk;" + evalIBk.precision(1) +";" + evalIBk.recall(1) +  ";" + evalIBk.areaUnderROC(1) + ";" + evalIBk.kappa() + "\n");
		finalData.add("Tajo;" + numbOfTraining + ";" + "NaiveBayes;" + evalNB.precision(1) +";" + evalNB.recall(1) +  ";" + evalNB.areaUnderROC(1) + ";" + evalNB.kappa() + "\n");
		finalData.add("Tajo;" + numbOfTraining + ";" + "RandomForest;" + evalRF.precision(1) +";" + evalRF.recall(1) +  ";" + evalRF.areaUnderROC(1) + ";" + evalRF.kappa() + "\n");
	}
	
	public static void main(String args[]) throws Exception{
		FileWriter csvEvaluate = new FileWriter("wekaOutput.csv");
		csvEvaluate.write("Dataset;#TrainingRelease;Classifier;Precision;Recall;AUC;Kappa\n");
		for(int i = 1 ; i < maxvers ; i++) {
			makeSets(i);
			evaluate(i);
		}
		
		for(String elem : finalData) {
			csvEvaluate.write(elem);
		}
		csvEvaluate.close();			
	}
}

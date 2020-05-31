package deliverabletwo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import weka.attributeSelection.CfsSubsetEval;
import weka.attributeSelection.GreedyStepwise;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.trees.RandomForest;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.CSVLoader;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;
import weka.filters.supervised.instance.Resample;
import weka.filters.supervised.instance.SMOTE;
import weka.filters.supervised.instance.SpreadSubsample;

public class WekaTesting {
	
	//repeated strings in code
	private static final String NOSAMPL_NOFS = ";No Sampling;No Feature Selection;"; 
	private static final String NOSAMPL_FS = "No Sampling;Feature Selection;";
	private static final String UNDERSAMPL_NOFS = "UnderSampling;No Feature Selection;";
	private static final String UNDERSAMPL_FS = "UnderSampling;Feature Selection;";
	private static final String SMOTE_NOFS = "SMOTE;No Feature Selection;";
	private static final String SMOTE_FS = "SMOTE;Feature Selection;";
	private static final String OVERSAMPLING_NOFS = "OverSampling;No Feature Selection;";
	private static final String OVERSAMPLING_FS = "OverSampling;Feature Selection;";
	private static final String NB = "NaiveBayes";
	private static final String RF = "RandomForest";
	
	private static List<String> finalData = new ArrayList<>();
	
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
			FileWriter csvTesting = new FileWriter(TESTING_CSV);
			BufferedWriter writer = new BufferedWriter(new FileWriter(TRAINING_ARFF));
		    BufferedWriter writer1 = new BufferedWriter(new FileWriter(TESTING_ARFF));	){
			
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
			
			// load CSV
	        CSVLoader loader = new CSVLoader();
	        loader.setSource(new File(TRAINING_CSV));
	        Instances dataSet = loader.getDataSet();

	        // save ARFF
	        
	        writer.write(dataSet.toString());
	        writer.flush();
	        
	        // load CSV
	        CSVLoader loader1 = new CSVLoader();
	        loader1.setSource(new File(TESTING_CSV));
	        Instances dataSet1 = loader1.getDataSet();

	        // save ARFF

	        writer1.write(dataSet1.toString());
	        writer1.flush();
			
		}
		
	}
	
	public static List<Double> countLines(Instances training, Instances testing){
		Double trainingLines= (double) training.size();
		Double testingLines= (double) testing.size();
		Double defectiveInTraining=0.0;
		Double defectiveInTesting=0.0;
		List<Double> lines = new ArrayList<>();
		
		for(int i = 0; i < trainingLines;i++) {
			Instance trainingInstance = training.get(i);
			String buggy = trainingInstance.stringValue(trainingInstance.numAttributes()-1);
			if(buggy.equals("yes")) {
				defectiveInTraining++;
			}
		}
		
		for(int i = 0; i < testingLines;i++) {
			Instance testingInstance = testing.get(i);
			String buggy = testingInstance.stringValue(testingInstance.numAttributes()-1);
			if(buggy.equals("yes")) {
				defectiveInTesting++;
			}
		}
		lines.add(trainingLines);
		lines.add(testingLines);
		lines.add(defectiveInTraining);
		lines.add(defectiveInTesting);
		
		return lines;
	}
	
	public static List<Double> calculatePercentage(Instances training, Instances testing){
		Double trainingLines;
		Double testingLines;
		Double defectiveInTraining;
		Double defectiveInTesting;
		List<Double> percentages = new ArrayList<>();
		
		//Counting lines in training and testing files
		List<Double> lines = countLines(training,testing);
		trainingLines = lines.get(0);
		testingLines = lines.get(1);
		defectiveInTraining = lines.get(2);
		defectiveInTesting = lines.get(3);
		
		//find percentages
		Double dataInTraining = (trainingLines/(trainingLines + testingLines));
		Double defectInTraining = (defectiveInTraining/trainingLines);
		Double defectInTesting = (defectiveInTesting/testingLines);
		percentages.add(dataInTraining);
		percentages.add(defectInTraining);
		percentages.add(defectInTesting);

		return percentages;
		
	}
	
	public static void evaluate(Integer numbOfTraining){
		
		try {
			//evaluation
			DataSource source1 = new DataSource(TRAINING_ARFF);
			Instances training = source1.getDataSet();
			DataSource source2 = new DataSource(TESTING_ARFF);
			Instances testing = source2.getDataSet();
			
			List<Double> perc = calculatePercentage(training,testing);

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
			
			finalData.add(PROJ+";" + numbOfTraining + ";" + perc.get(0) + ";" + perc.get(1) + ";" + perc.get(2) + ";IBk" + NOSAMPL_NOFS + evalIBk.numTruePositives(1) + ";"+ evalIBk.numFalsePositives(1) + ";"+ evalIBk.numTrueNegatives(1)+ ";"+ evalIBk.numFalseNegatives(1)+ ";" + evalIBk.precision(1) +";" + evalIBk.recall(1) +  ";" + evalIBk.areaUnderROC(1) + ";" + evalIBk.kappa() + "\n");
			finalData.add(PROJ+";" + numbOfTraining + ";" + perc.get(0) + ";" + perc.get(1) + ";" + perc.get(2) + ";"+ NB + NOSAMPL_NOFS+ evalNB.numTruePositives(1) + ";"+ evalNB.numFalsePositives(1) + ";"+ evalNB.numTrueNegatives(1)+ ";"+ evalNB.numFalseNegatives(1)+ ";" + evalNB.precision(1) +";" + evalNB.recall(1) +  ";" + evalNB.areaUnderROC(1) + ";" + evalNB.kappa() + "\n");
			finalData.add(PROJ+";" + numbOfTraining + ";" + perc.get(0) + ";" + perc.get(1) + ";" + perc.get(2) + ";"+ RF + NOSAMPL_NOFS + evalRF.numTruePositives(1) + ";"+ evalRF.numFalsePositives(1) + ";"+ evalRF.numTrueNegatives(1)+ ";"+ evalRF.numFalseNegatives(1)+ ";" + evalRF.precision(1) +";" + evalRF.recall(1) +  ";" + evalRF.areaUnderROC(1) + ";" + evalRF.kappa() + "\n");
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static void evaluateFilter(Integer numbOfTraining){
		DataSource source1;
		DataSource source2;
		Instances testingNoFilter;
		Instances noFilterTraining;
		Integer numAttrNoFilter;
		Integer numAttrFiltered;
		Evaluation evalRF;
		Evaluation evalIBk;
		Evaluation evalNB;
		
		try{
			source1 = new DataSource(TRAINING_ARFF);
			source2 = new DataSource(TESTING_ARFF);
			testingNoFilter = source2.getDataSet();
			noFilterTraining = source1.getDataSet();
			
			List<Double> perc = WekaTesting.calculatePercentage(noFilterTraining, testingNoFilter);
			//create AttributeSelection object
			AttributeSelection filter = new AttributeSelection();
			//create evaluator and search algorithm objects
			CfsSubsetEval eval = new CfsSubsetEval();
			GreedyStepwise search = new GreedyStepwise();
			//set the algorithm to search backward
			search.setSearchBackwards(true);
			//set the filter to use the evaluator and search algorithm
			filter.setEvaluator(eval);
			filter.setSearch(search);
			//specify the dataset
			filter.setInputFormat(noFilterTraining);
			//apply
			Instances filteredTraining = Filter.useFilter(noFilterTraining, filter);
			
			numAttrNoFilter = noFilterTraining.numAttributes();
			noFilterTraining.setClassIndex(numAttrNoFilter - 1);
			testingNoFilter.setClassIndex(numAttrNoFilter - 1);
			
			numAttrFiltered = filteredTraining.numAttributes();

			
			IBk classifierIBk = new IBk();
			NaiveBayes classifierNB = new NaiveBayes();
			RandomForest classifierRF = new RandomForest();
			
			
			//evaluation with filtered
			filteredTraining.setClassIndex(numAttrFiltered - 1);
			Instances testingFiltered = Filter.useFilter(testingNoFilter, filter);
			testingFiltered.setClassIndex(numAttrFiltered - 1);
			
			classifierRF.buildClassifier(filteredTraining);
			classifierIBk.buildClassifier(filteredTraining);
			classifierNB.buildClassifier(filteredTraining);
			
			evalRF = new Evaluation(testingNoFilter);
			evalIBk = new Evaluation(testingNoFilter);
			evalNB = new Evaluation(testingNoFilter);
			
		    evalRF.evaluateModel(classifierRF, testingFiltered);
		    evalNB.evaluateModel(classifierNB, testingFiltered);
		    evalIBk.evaluateModel(classifierIBk, testingFiltered);
		    
			finalData.add(PROJ+";" + numbOfTraining + ";" + perc.get(0) + ";" + perc.get(1) + ";" + perc.get(2) + ";" + "IBk;"+ NOSAMPL_FS + evalIBk.numTruePositives(1) + ";"+ evalIBk.numFalsePositives(1) + ";"+ evalIBk.numTrueNegatives(1)+ ";"+ evalIBk.numFalseNegatives(1)+ ";" + evalIBk.precision(1) +";" + evalIBk.recall(1) +  ";" + evalIBk.areaUnderROC(1) + ";" + evalIBk.kappa() + "\n");
			finalData.add(PROJ+";" + numbOfTraining + ";" + perc.get(0) + ";" + perc.get(1) + ";" + perc.get(2) + ";" + NB + ";" +NOSAMPL_FS + evalNB.numTruePositives(1) + ";"+ evalNB.numFalsePositives(1) + ";"+ evalNB.numTrueNegatives(1)+ ";"+ evalNB.numFalseNegatives(1)+ ";" + evalNB.precision(1) +";" + evalNB.recall(1) +  ";" + evalNB.areaUnderROC(1) + ";" + evalNB.kappa() + "\n");
			finalData.add(PROJ+";" + numbOfTraining + ";" + perc.get(0) + ";" + perc.get(1) + ";" + perc.get(2) + ";" + RF + ";" + NOSAMPL_FS + evalRF.numTruePositives(1) + ";"+ evalRF.numFalsePositives(1) + ";"+ evalRF.numTrueNegatives(1)+ ";"+ evalRF.numFalseNegatives(1)+ ";" + evalRF.precision(1) +";" + evalRF.recall(1) +  ";" + evalRF.areaUnderROC(1) + ";" + evalRF.kappa() + "\n");

		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public static List<Double> percentageUnderSampling(Instances training,Instances testing){
		
		Double trainingLinesUS;
		Double testingLinesUS;
		Double defectiveInTrainingUS;
		Double defectiveInTestingUS;
		List<Double> percentagesUS = new ArrayList<>();
		
		//Counting lines in training and testing files
		List<Double> lines = countLines(training,testing);
		testingLinesUS = lines.get(1);
		defectiveInTrainingUS = lines.get(2);
		defectiveInTestingUS = lines.get(3);
		
		//Training Lines will be double of defects in training with USampling
		trainingLinesUS = 2*defectiveInTrainingUS;
		
		//find percentages
		Double dataInTrainingUS = (trainingLinesUS/(trainingLinesUS + testingLinesUS));
		Double defectInTrainingUS = 0.0; 
		if(trainingLinesUS != 0.0) {
			defectInTrainingUS = (defectiveInTrainingUS/trainingLinesUS);
		}
		Double defectInTestingUS = (defectiveInTestingUS/testingLinesUS);
		percentagesUS.add(dataInTrainingUS);
		percentagesUS.add(defectInTrainingUS);
		percentagesUS.add(defectInTestingUS);
		
		return percentagesUS;
	}
	
	public static List<Double> percentageOverSampling(Instances training,Instances testing){
		
		Double trainingLinesOS;
		Double testingLinesOS;
		Double defectiveInTrainingOS;
		Double defectiveInTestingOS;
		List<Double> percentagesOS = new ArrayList<>();
		
		//Counting lines in training and testing files
		List<Double> lines = countLines(training,testing);
		trainingLinesOS = lines.get(0);
		testingLinesOS = lines.get(1);
		defectiveInTrainingOS = lines.get(2);
		defectiveInTestingOS = lines.get(3);
		
		//Training Lines will be double of defects in training with USampling
		Double notDefectiveTrainingOS = trainingLinesOS - defectiveInTrainingOS;
		trainingLinesOS = 2*notDefectiveTrainingOS;
		defectiveInTrainingOS = notDefectiveTrainingOS;
		
		//find percentages
		Double dataInTrainingOS = (trainingLinesOS/(trainingLinesOS + testingLinesOS));
		Double defectInTrainingOS = (defectiveInTrainingOS/trainingLinesOS);
		Double defectInTestingOS = (defectiveInTestingOS/testingLinesOS);
		percentagesOS.add(dataInTrainingOS);
		percentagesOS.add(defectInTrainingOS);
		percentagesOS.add(defectInTestingOS);
		
		return percentagesOS;
	}
	
	public static List<FilteredClassifier> setClassifiersResamplingNoFilter(Instances training,Instances testing){
		List<FilteredClassifier> classifiers = new ArrayList<>();
		
		try {
			int numAttr = training.numAttributes();
			training.setClassIndex(numAttr - 1);
			testing.setClassIndex(numAttr - 1);
			
			Resample resample = new Resample();
			resample.setInputFormat(training);

			RandomForest classifierRF = new RandomForest();
			IBk classifierIBk = new IBk();
			NaiveBayes classifierNB = new NaiveBayes();
			FilteredClassifier fcRF = new FilteredClassifier();
			FilteredClassifier fcIBk = new FilteredClassifier();
			FilteredClassifier fcNB = new FilteredClassifier();
			fcRF.setClassifier(classifierRF);
			fcIBk.setClassifier(classifierIBk);
			fcNB.setClassifier(classifierNB);
			
			classifiers.add(fcRF);
			classifiers.add(fcIBk);
			classifiers.add(fcNB);

		}catch (Exception e) {
			e.printStackTrace();
		}
		
		return classifiers;
		
	}
	
	public static void evaluateUnderSampling(Integer numbOfTraining) {
		try {
			DataSource source1 = new DataSource(TRAINING_ARFF);
			Instances training = source1.getDataSet();
			DataSource source2 = new DataSource(TESTING_ARFF);
			Instances testing = source2.getDataSet();
			
			//Count lines for underSampling
			List<Double> perc = percentageUnderSampling(training,testing);
			
			//Set all 3 FilteredClasifiers for sampling
			List<FilteredClassifier> classifiers = setClassifiersResamplingNoFilter(training,testing);	
			FilteredClassifier fcRF = classifiers.get(0);
			FilteredClassifier fcIBk = classifiers.get(1);
			FilteredClassifier fcNB = classifiers.get(2);
			
			SpreadSubsample  spreadSubsample = new SpreadSubsample();
			String[] opts = new String[]{ "-M", "1.0"};
			spreadSubsample.setOptions(opts);
			fcRF.setFilter(spreadSubsample);
			fcRF.buildClassifier(training);
			fcIBk.setFilter(spreadSubsample);
			fcIBk.buildClassifier(training);
			fcNB.setFilter(spreadSubsample);
			fcNB.buildClassifier(training);
			

			Evaluation evalRF = new Evaluation(testing);	
			evalRF.evaluateModel(fcRF, testing);
			Evaluation evalIBk = new Evaluation(testing);	
			evalIBk.evaluateModel(fcIBk, testing);
			Evaluation evalNB = new Evaluation(testing);	
			evalNB.evaluateModel(fcNB, testing);
			
			//with filters
			//create AttributeSelection object
			AttributeSelection filter = new AttributeSelection();
			//create evaluator and search algorithm objects
			CfsSubsetEval eval = new CfsSubsetEval();
			GreedyStepwise search = new GreedyStepwise();
			//set the algorithm to search backward
			search.setSearchBackwards(true);
			//set the filter to use the evaluator and search algorithm
			filter.setEvaluator(eval);
			filter.setSearch(search);
			//specify the dataset
			filter.setInputFormat(training);
			//apply
			Instances filteredTraining = Filter.useFilter(training, filter);
			
			Resample resampleFS = new Resample();
			resampleFS.setInputFormat(filteredTraining);
			FilteredClassifier fcRFFS = new FilteredClassifier();
			FilteredClassifier fcIBkFS = new FilteredClassifier();
			FilteredClassifier fcNBFS = new FilteredClassifier();
			
			RandomForest classifierRFFS = new RandomForest();
			IBk classifierIBkFS = new IBk();
			NaiveBayes classifierNBFS = new NaiveBayes();
			fcRFFS.setClassifier(classifierRFFS);
			fcIBkFS.setClassifier(classifierIBkFS);
			fcNBFS.setClassifier(classifierNBFS);
				
			SpreadSubsample  spreadSubsampleFS = new SpreadSubsample();
			String[] optsFS = new String[]{ "-M", "1.0"};
			spreadSubsampleFS.setOptions(optsFS);
			fcRFFS.setFilter(spreadSubsampleFS);
			fcRFFS.buildClassifier(filteredTraining);
			fcIBkFS.setFilter(spreadSubsampleFS);
			fcIBkFS.buildClassifier(filteredTraining);
			fcNBFS.setFilter(spreadSubsampleFS);
			fcNBFS.buildClassifier(filteredTraining);
			
			
			Integer numAttrFiltered = filteredTraining.numAttributes();
			
						
			//evaluation with filtered
			filteredTraining.setClassIndex(numAttrFiltered - 1);
			Instances testingFiltered = Filter.useFilter(testing, filter);
			testingFiltered.setClassIndex(numAttrFiltered - 1);
			
			RandomForest classifierRF = new RandomForest();
			IBk classifierIBk = new IBk();
			NaiveBayes classifierNB = new NaiveBayes();
			
			classifierRF.buildClassifier(filteredTraining);
			classifierIBk.buildClassifier(filteredTraining);
			classifierNB.buildClassifier(filteredTraining);
			
			Evaluation evalRFFS = new Evaluation(testing);
			Evaluation evalIBkFS = new Evaluation(testing);
			Evaluation evalNBFS = new Evaluation(testing);
			
		    evalRFFS.evaluateModel(fcRFFS, testingFiltered);
		    evalNBFS.evaluateModel(fcNBFS, testingFiltered);
		    evalIBkFS.evaluateModel(fcIBkFS, testingFiltered);
			
			
			finalData.add(PROJ+";" + numbOfTraining + ";" + perc.get(0) + ";" + perc.get(1) + ";" + perc.get(2) + ";" + "IBk;"+ UNDERSAMPL_NOFS + evalIBk.numTruePositives(1) + ";"+ evalIBk.numFalsePositives(1) + ";"+ evalIBk.numTrueNegatives(1)+ ";"+ evalIBk.numFalseNegatives(1)+ ";" + evalIBk.precision(1) +";" + evalIBk.recall(1) +  ";" + evalIBk.areaUnderROC(1) + ";" + evalIBk.kappa() + "\n");
			finalData.add(PROJ+";" + numbOfTraining + ";" + perc.get(0) + ";" + perc.get(1) + ";" + perc.get(2) + ";" + NB +";" + UNDERSAMPL_NOFS + evalNB.numTruePositives(1) + ";"+ evalNB.numFalsePositives(1) + ";"+ evalNB.numTrueNegatives(1)+ ";"+ evalNB.numFalseNegatives(1)+ ";" + evalNB.precision(1) +";" + evalNB.recall(1) +  ";" + evalNB.areaUnderROC(1) + ";" + evalNB.kappa() + "\n");
			finalData.add(PROJ+";" + numbOfTraining + ";" + perc.get(0) + ";" + perc.get(1) + ";" + perc.get(2) + ";" + RF +";" + UNDERSAMPL_NOFS + evalRF.numTruePositives(1) + ";"+ evalRF.numFalsePositives(1) + ";"+ evalRF.numTrueNegatives(1)+ ";"+ evalRF.numFalseNegatives(1)+ ";" + evalRF.precision(1) +";" + evalRF.recall(1) +  ";" + evalRF.areaUnderROC(1) + ";" + evalRF.kappa() + "\n");
			
			finalData.add(PROJ+";" + numbOfTraining + ";" + perc.get(0) + ";" + perc.get(1) + ";" + perc.get(2) + ";" + "IBk;"+ UNDERSAMPL_FS + evalIBkFS.numTruePositives(1) + ";"+ evalIBkFS.numFalsePositives(1) + ";"+ evalIBkFS.numTrueNegatives(1)+ ";"+ evalIBkFS.numFalseNegatives(1)+ ";" + evalIBkFS.precision(1) +";" + evalIBkFS.recall(1) +  ";" + evalIBkFS.areaUnderROC(1) + ";" + evalIBkFS.kappa() + "\n");
			finalData.add(PROJ+";" + numbOfTraining + ";" + perc.get(0) + ";" + perc.get(1) + ";" + perc.get(2) + ";" + NB +";" + UNDERSAMPL_FS+ evalNBFS.numTruePositives(1) + ";"+ evalNBFS.numFalsePositives(1) + ";"+ evalNBFS.numTrueNegatives(1)+ ";"+ evalNBFS.numFalseNegatives(1)+ ";" + evalNBFS.precision(1) +";" + evalNBFS.recall(1) +  ";" + evalNBFS.areaUnderROC(1) + ";" + evalNBFS.kappa() + "\n");
			finalData.add(PROJ+";" + numbOfTraining + ";" + perc.get(0) + ";" + perc.get(1) + ";" + perc.get(2) + ";" + RF +";" + UNDERSAMPL_FS + evalRFFS.numTruePositives(1) + ";"+ evalRFFS.numFalsePositives(1) + ";"+ evalRFFS.numTrueNegatives(1)+ ";"+ evalRFFS.numFalseNegatives(1)+ ";" + evalRFFS.precision(1) +";" + evalRFFS.recall(1) +  ";" + evalRFFS.areaUnderROC(1) + ";" + evalRFFS.kappa() + "\n");
		}catch(Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public static void evaluateSmote(Integer numbOfTraining) {
		try {
			DataSource source1 = new DataSource(TRAINING_ARFF);
			Instances training = source1.getDataSet();
			DataSource source2 = new DataSource(TESTING_ARFF);
			Instances testing = source2.getDataSet();
			
			List<Double> perc = percentageOverSampling(training,testing);
			
			
			//Set all 3 FilteredClasifiers for sampling
			List<FilteredClassifier> classifiers = setClassifiersResamplingNoFilter(training,testing);	
			FilteredClassifier fcRF = classifiers.get(0);
			FilteredClassifier fcIBk = classifiers.get(1);
			FilteredClassifier fcNB = classifiers.get(2);

		    SMOTE smote = new SMOTE();
			smote.setInputFormat(training);
			fcRF.setFilter(smote);
			fcIBk.setFilter(smote);
			fcNB.setFilter(smote);
			
			
			fcRF.buildClassifier(training);
			fcIBk.buildClassifier(training);
			fcNB.buildClassifier(training);
			

			Evaluation evalRF = new Evaluation(testing);	
			evalRF.evaluateModel(fcRF, testing);
			Evaluation evalIBk = new Evaluation(testing);	
			evalIBk.evaluateModel(fcIBk, testing);
			Evaluation evalNB = new Evaluation(testing);	
			evalNB.evaluateModel(fcNB, testing);
			
			//with filters
			//create AttributeSelection object
			AttributeSelection filter = new AttributeSelection();
			//create evaluator and search algorithm objects
			CfsSubsetEval eval = new CfsSubsetEval();
			GreedyStepwise search = new GreedyStepwise();
			//set the algorithm to search backward
			search.setSearchBackwards(true);
			//set the filter to use the evaluator and search algorithm
			filter.setEvaluator(eval);
			filter.setSearch(search);
			//specify the dataset
			filter.setInputFormat(training);
			//apply
			Instances filteredTraining = Filter.useFilter(training, filter);
			
			Resample resampleFS = new Resample();
			resampleFS.setInputFormat(filteredTraining);
			FilteredClassifier fcRFFS = new FilteredClassifier();
			FilteredClassifier fcIBkFS = new FilteredClassifier();
			FilteredClassifier fcNBFS = new FilteredClassifier();
			
			SMOTE smoteFS = new SMOTE();
			smoteFS.setInputFormat(filteredTraining);
			
			RandomForest classifierRFFS = new RandomForest();
			IBk classifierIBkFS = new IBk();
			NaiveBayes classifierNBFS = new NaiveBayes();
			fcRFFS.setClassifier(classifierRFFS);
			fcIBkFS.setClassifier(classifierIBkFS);
			fcNBFS.setClassifier(classifierNBFS);
				
			fcRFFS.setFilter(smoteFS);
			fcRFFS.buildClassifier(filteredTraining);
			fcIBkFS.setFilter(smoteFS);
			fcIBkFS.buildClassifier(filteredTraining);
			fcNBFS.setFilter(smoteFS);
			fcNBFS.buildClassifier(filteredTraining);
			
			
			Integer numAttrFiltered = filteredTraining.numAttributes();
			
						
			//evaluation with filtered
			filteredTraining.setClassIndex(numAttrFiltered - 1);
			Instances testingFiltered = Filter.useFilter(testing, filter);
			testingFiltered.setClassIndex(numAttrFiltered - 1);
			
			RandomForest classifierRF = new RandomForest();
			IBk classifierIBk = new IBk();
			NaiveBayes classifierNB = new NaiveBayes();
			
			classifierRF.buildClassifier(filteredTraining);
			classifierIBk.buildClassifier(filteredTraining);
			classifierNB.buildClassifier(filteredTraining);
			
			Evaluation evalRFFS = new Evaluation(testing);
			Evaluation evalIBkFS = new Evaluation(testing);
			Evaluation evalNBFS = new Evaluation(testing);
			
		    evalRFFS.evaluateModel(fcRFFS, testingFiltered);
		    evalNBFS.evaluateModel(fcNBFS, testingFiltered);
		    evalIBkFS.evaluateModel(fcIBkFS, testingFiltered);
			
			
			finalData.add(PROJ+";" + numbOfTraining + ";" + perc.get(0) + ";" + perc.get(1) + ";" + perc.get(2) + ";" + "IBk;"+ SMOTE_NOFS + evalIBk.numTruePositives(1) + ";"+ evalIBk.numFalsePositives(1) + ";"+ evalIBk.numTrueNegatives(1)+ ";"+ evalIBk.numFalseNegatives(1)+ ";" + evalIBk.precision(1) +";" + evalIBk.recall(1) +  ";" + evalIBk.areaUnderROC(1) + ";" + evalIBk.kappa() + "\n");
			finalData.add(PROJ+";" + numbOfTraining + ";" + perc.get(0) + ";" + perc.get(1) + ";" + perc.get(2) + ";" + NB +";" + SMOTE_NOFS + evalNB.numTruePositives(1) + ";"+ evalNB.numFalsePositives(1) + ";"+ evalNB.numTrueNegatives(1)+ ";"+ evalNB.numFalseNegatives(1)+ ";" + evalNB.precision(1) +";" + evalNB.recall(1) +  ";" + evalNB.areaUnderROC(1) + ";" + evalNB.kappa() + "\n");
			finalData.add(PROJ+";" + numbOfTraining + ";" + perc.get(0) + ";" + perc.get(1) + ";" + perc.get(2) + ";" + RF +";" + SMOTE_NOFS + evalRF.numTruePositives(1) + ";"+ evalRF.numFalsePositives(1) + ";"+ evalRF.numTrueNegatives(1)+ ";"+ evalRF.numFalseNegatives(1)+ ";" + evalRF.precision(1) +";" + evalRF.recall(1) +  ";" + evalRF.areaUnderROC(1) + ";" + evalRF.kappa() + "\n");
			
			finalData.add(PROJ+";" + numbOfTraining + ";" + perc.get(0) + ";" + perc.get(1) + ";" + perc.get(2) + ";" + "IBk;"+ SMOTE_FS + evalIBkFS.numTruePositives(1) + ";"+ evalIBkFS.numFalsePositives(1) + ";"+ evalIBkFS.numTrueNegatives(1)+ ";"+ evalIBkFS.numFalseNegatives(1)+ ";" + evalIBkFS.precision(1) +";" + evalIBkFS.recall(1) +  ";" + evalIBkFS.areaUnderROC(1) + ";" + evalIBkFS.kappa() + "\n");
			finalData.add(PROJ+";" + numbOfTraining + ";" + perc.get(0) + ";" + perc.get(1) + ";" + perc.get(2) + ";" + NB +";" + SMOTE_FS + evalNBFS.numTruePositives(1) + ";"+ evalNBFS.numFalsePositives(1) + ";"+ evalNBFS.numTrueNegatives(1)+ ";"+ evalNBFS.numFalseNegatives(1)+ ";" + evalNBFS.precision(1) +";" + evalNBFS.recall(1) +  ";" + evalNBFS.areaUnderROC(1) + ";" + evalNBFS.kappa() + "\n");
			finalData.add(PROJ+";" + numbOfTraining + ";" + perc.get(0) + ";" + perc.get(1) + ";" + perc.get(2) + ";" + RF +";" + SMOTE_FS + evalRFFS.numTruePositives(1) + ";"+ evalRFFS.numFalsePositives(1) + ";"+ evalRFFS.numTrueNegatives(1)+ ";"+ evalRFFS.numFalseNegatives(1)+ ";" + evalRFFS.precision(1) +";" + evalRFFS.recall(1) +  ";" + evalRFFS.areaUnderROC(1) + ";" + evalRFFS.kappa() + "\n");
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void evaluateOverSampling(Integer numbOfTraining) {
		try {
			DataSource source1 = new DataSource(TRAINING_ARFF);
			Instances training = source1.getDataSet();
			DataSource source2 = new DataSource(TESTING_ARFF);
			Instances testing = source2.getDataSet();
			
			List<Double> perc = percentageOverSampling(training,testing);
			List<Double> perc1 = calculatePercentage(training,testing);
			Double minorityDoubledClass = (1-perc1.get(1)) * 200.0 ;
			
			
			int numAttr = training.numAttributes();
			training.setClassIndex(numAttr - 1);
			testing.setClassIndex(numAttr - 1);
			
			Resample resample = new Resample();
			resample.setInputFormat(training);
			FilteredClassifier fcRF = new FilteredClassifier();
			FilteredClassifier fcIBk = new FilteredClassifier();
			FilteredClassifier fcNB = new FilteredClassifier();

			RandomForest classifierRF = new RandomForest();
			IBk classifierIBk = new IBk();
			NaiveBayes classifierNB = new NaiveBayes();
			fcRF.setClassifier(classifierRF);
			fcIBk.setClassifier(classifierIBk);
			fcNB.setClassifier(classifierNB);
			
			resample.setOptions(new String[] {"-B","1.0","-Z",minorityDoubledClass.toString()});
			
			fcRF.setFilter(resample);
			fcIBk.setFilter(resample);
			fcNB.setFilter(resample);
			
			fcRF.buildClassifier(training);
			fcIBk.buildClassifier(training);
			fcNB.buildClassifier(training);
			

			Evaluation evalRF = new Evaluation(testing);	
			evalRF.evaluateModel(fcRF, testing);
			Evaluation evalIBk = new Evaluation(testing);	
			evalIBk.evaluateModel(fcIBk, testing);
			Evaluation evalNB = new Evaluation(testing);	
			evalNB.evaluateModel(fcNB, testing);
			
			//with filters
			//create AttributeSelection object
			AttributeSelection filter = new AttributeSelection();
			//create evaluator and search algorithm objects
			CfsSubsetEval eval = new CfsSubsetEval();
			GreedyStepwise search = new GreedyStepwise();
			//set the algorithm to search backward
			search.setSearchBackwards(true);
			//set the filter to use the evaluator and search algorithm
			filter.setEvaluator(eval);
			filter.setSearch(search);
			//specify the dataset
			filter.setInputFormat(training);
			//apply
			Instances filteredTraining = Filter.useFilter(training, filter);
			
			Resample resampleFS = new Resample();
			resampleFS.setInputFormat(filteredTraining);
			FilteredClassifier fcRFFS = new FilteredClassifier();
			FilteredClassifier fcIBkFS = new FilteredClassifier();
			FilteredClassifier fcNBFS = new FilteredClassifier();
			

			resampleFS.setOptions(new String[] {"-B","1.0","-Z",minorityDoubledClass.toString()});
			
			fcRF.setFilter(resampleFS);
			fcIBk.setFilter(resampleFS);
			fcNB.setFilter(resampleFS);
			
			RandomForest classifierRFFS = new RandomForest();
			IBk classifierIBkFS = new IBk();
			NaiveBayes classifierNBFS = new NaiveBayes();
			fcRFFS.setClassifier(classifierRFFS);
			fcIBkFS.setClassifier(classifierIBkFS);
			fcNBFS.setClassifier(classifierNBFS);
				
			fcRFFS.buildClassifier(filteredTraining);
			fcIBkFS.buildClassifier(filteredTraining);
			fcNBFS.buildClassifier(filteredTraining);
			
			
			Integer numAttrFiltered = filteredTraining.numAttributes();
			
						
			//evaluation with filtered
			filteredTraining.setClassIndex(numAttrFiltered - 1);
			Instances testingFiltered = Filter.useFilter(testing, filter);
			testingFiltered.setClassIndex(numAttrFiltered - 1);
			
			classifierRF.buildClassifier(filteredTraining);
			classifierIBk.buildClassifier(filteredTraining);
			classifierNB.buildClassifier(filteredTraining);
			
			Evaluation evalRFFS = new Evaluation(testing);
			Evaluation evalIBkFS = new Evaluation(testing);
			Evaluation evalNBFS = new Evaluation(testing);
			
		    evalRFFS.evaluateModel(fcRFFS, testingFiltered);
		    evalNBFS.evaluateModel(fcNBFS, testingFiltered);
		    evalIBkFS.evaluateModel(fcIBkFS, testingFiltered);
			
			
			finalData.add(PROJ+";" + numbOfTraining + ";" + perc.get(0) + ";" + perc.get(1) + ";" + perc.get(2) + ";" + "IBk;"+ OVERSAMPLING_NOFS + evalIBk.numTruePositives(1) + ";"+ evalIBk.numFalsePositives(1) + ";"+ evalIBk.numTrueNegatives(1)+ ";"+ evalIBk.numFalseNegatives(1)+ ";" + evalIBk.precision(1) +";" + evalIBk.recall(1) +  ";" + evalIBk.areaUnderROC(1) + ";" + evalIBk.kappa() + "\n");
			finalData.add(PROJ+";" + numbOfTraining + ";" + perc.get(0) + ";" + perc.get(1) + ";" + perc.get(2) + ";" + NB + ";" + OVERSAMPLING_NOFS + evalNB.numTruePositives(1) + ";"+ evalNB.numFalsePositives(1) + ";"+ evalNB.numTrueNegatives(1)+ ";"+ evalNB.numFalseNegatives(1)+ ";" + evalNB.precision(1) +";" + evalNB.recall(1) +  ";" + evalNB.areaUnderROC(1) + ";" + evalNB.kappa() + "\n");
			finalData.add(PROJ+";" + numbOfTraining + ";" + perc.get(0) + ";" + perc.get(1) + ";" + perc.get(2) + ";" + RF +";" + OVERSAMPLING_NOFS + evalRF.numTruePositives(1) + ";"+ evalRF.numFalsePositives(1) + ";"+ evalRF.numTrueNegatives(1)+ ";"+ evalRF.numFalseNegatives(1)+ ";" + evalRF.precision(1) +";" + evalRF.recall(1) +  ";" + evalRF.areaUnderROC(1) + ";" + evalRF.kappa() + "\n");
			
			finalData.add(PROJ+";" + numbOfTraining + ";" + perc.get(0) + ";" + perc.get(1) + ";" + perc.get(2) + ";" + "IBk;"+ OVERSAMPLING_FS + evalIBkFS.numTruePositives(1) + ";"+ evalIBkFS.numFalsePositives(1) + ";"+ evalIBkFS.numTrueNegatives(1)+ ";"+ evalIBkFS.numFalseNegatives(1)+ ";" + evalIBkFS.precision(1) +";" + evalIBkFS.recall(1) +  ";" + evalIBkFS.areaUnderROC(1) + ";" + evalIBkFS.kappa() + "\n");
			finalData.add(PROJ+";" + numbOfTraining + ";" + perc.get(0) + ";" + perc.get(1) + ";" + perc.get(2) + ";" + NB +";" + OVERSAMPLING_FS + evalNBFS.numTruePositives(1) + ";"+ evalNBFS.numFalsePositives(1) + ";"+ evalNBFS.numTrueNegatives(1)+ ";"+ evalNBFS.numFalseNegatives(1)+ ";" + evalNBFS.precision(1) +";" + evalNBFS.recall(1) +  ";" + evalNBFS.areaUnderROC(1) + ";" + evalNBFS.kappa() + "\n");
			finalData.add(PROJ+";" + numbOfTraining + ";" + perc.get(0) + ";" + perc.get(1) + ";" + perc.get(2) + ";" + RF +";" + OVERSAMPLING_FS + evalRFFS.numTruePositives(1) + ";"+ evalRFFS.numFalsePositives(1) + ";"+ evalRFFS.numTrueNegatives(1)+ ";"+ evalRFFS.numFalseNegatives(1)+ ";" + evalRFFS.precision(1) +";" + evalRFFS.recall(1) +  ";" + evalRFFS.areaUnderROC(1) + ";" + evalRFFS.kappa() + "\n");
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	
	public static void main(String[] args) throws Exception {
		Long maxvers = GetBuggy.firstHalfVersions();
		
		
		for(int i = 1 ; i < maxvers ; i++) {
			makeSets(i);
			
		    //No FS and NO balancing
			evaluate(i);
			//FS no balancing
			evaluateFilter(i);
			//UnderSampling with/without FS
			evaluateUnderSampling(i);
			//SMOTE with/without FS
			evaluateSmote(i);
			//OverSampling with/without FS
			evaluateOverSampling(i);
		}

		try(FileWriter csvEvaluate = new FileWriter(WEKA_OUTPUT);){
			csvEvaluate.write("Dataset,#TrainingRelease,%training,%Defective in Training,%Defective in Testing,Classifier,Balancing,Feature Selection,TP,FP,TN,FN,Precision,Recall,AUC,Kappa\n");
			
			for(String elem : finalData) {
				csvEvaluate.write(elem.replace(";", ","));
			}
		}
		
	}
}

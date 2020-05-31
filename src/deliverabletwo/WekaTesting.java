package deliverabletwo;

import java.io.BufferedReader;
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
import weka.core.converters.ArffSaver;
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
	
	private static final String PROJ = "bookkeeper";
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
		
		 // load the CSV file (input file)
        CSVLoader loader = new CSVLoader();
        loader.setSource(new File(TRAINING_CSV));
        Instances data = loader.getDataSet();

        // save as an  ARFF (output file)
        ArffSaver saver = new ArffSaver();
        saver.setInstances(data);
        saver.setFile(new File(TRAINING_ARFF));
        saver.writeBatch();
        
        // load the CSV file (input file)
        CSVLoader loader1 = new CSVLoader();
        loader1.setSource(new File(TESTING_CSV));
        Instances data1 = loader1.getDataSet();

        // save as an  ARFF (output file)
        ArffSaver saver1 = new ArffSaver();
        saver1.setInstances(data1);
        saver1.setFile(new File(TESTING_ARFF));
        saver1.writeBatch();
		
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
	
	public static AttributeSelection applyFeatureSelection(Instances noFilterTraining){
		
		try {
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
			return filter;
			
		}catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
		
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
			
			//Counting training and testing lines
			List<Double> perc = calculatePercentage(noFilterTraining, testingNoFilter);
			
			//create AttributeSelection object
			AttributeSelection filter = applyFeatureSelection(noFilterTraining);
			
			//Apply feature selection to training instances
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
			

			Evaluation evalRFUS = new Evaluation(testing);	
			evalRFUS.evaluateModel(fcRF, testing);
			Evaluation evalIBkUS = new Evaluation(testing);	
			evalIBkUS.evaluateModel(fcIBk, testing);
			Evaluation evalNBUS = new Evaluation(testing);	
			evalNBUS.evaluateModel(fcNB, testing);
			
			//generate filter
			AttributeSelection filter = applyFeatureSelection(training);
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
			
			Evaluation evalRFFSUS = new Evaluation(testing);
			Evaluation evalIBkFSUS = new Evaluation(testing);
			Evaluation evalNBFSUS = new Evaluation(testing);
			
		    evalRFFSUS.evaluateModel(fcRFFS, testingFiltered);
		    evalNBFSUS.evaluateModel(fcNBFS, testingFiltered);
		    evalIBkFSUS.evaluateModel(fcIBkFS, testingFiltered);
			
			
			finalData.add(PROJ+";" + numbOfTraining + ";" + perc.get(0) + ";" + perc.get(1) + ";" + perc.get(2) + ";" + "IBk;"+ UNDERSAMPL_NOFS + evalIBkUS.numTruePositives(1) + ";"+ evalIBkUS.numFalsePositives(1) + ";"+ evalIBkUS.numTrueNegatives(1)+ ";"+ evalIBkUS.numFalseNegatives(1)+ ";" + evalIBkUS.precision(1) +";" + evalIBkUS.recall(1) +  ";" + evalIBkUS.areaUnderROC(1) + ";" + evalIBkUS.kappa() + "\n");
			finalData.add(PROJ+";" + numbOfTraining + ";" + perc.get(0) + ";" + perc.get(1) + ";" + perc.get(2) + ";" + NB +";" + UNDERSAMPL_NOFS + evalNBUS.numTruePositives(1) + ";"+ evalNBUS.numFalsePositives(1) + ";"+ evalNBUS.numTrueNegatives(1)+ ";"+ evalNBUS.numFalseNegatives(1)+ ";" + evalNBUS.precision(1) +";" + evalNBUS.recall(1) +  ";" + evalNBUS.areaUnderROC(1) + ";" + evalNBUS.kappa() + "\n");
			finalData.add(PROJ+";" + numbOfTraining + ";" + perc.get(0) + ";" + perc.get(1) + ";" + perc.get(2) + ";" + RF +";" + UNDERSAMPL_NOFS + evalRFUS.numTruePositives(1) + ";"+ evalRFUS.numFalsePositives(1) + ";"+ evalRFUS.numTrueNegatives(1)+ ";"+ evalRFUS.numFalseNegatives(1)+ ";" + evalRFUS.precision(1) +";" + evalRFUS.recall(1) +  ";" + evalRFUS.areaUnderROC(1) + ";" + evalRFUS.kappa() + "\n");
			
			finalData.add(PROJ+";" + numbOfTraining + ";" + perc.get(0) + ";" + perc.get(1) + ";" + perc.get(2) + ";" + "IBk;"+ UNDERSAMPL_FS + evalIBkFSUS.numTruePositives(1) + ";"+ evalIBkFSUS.numFalsePositives(1) + ";"+ evalIBkFSUS.numTrueNegatives(1)+ ";"+ evalIBkFSUS.numFalseNegatives(1)+ ";" + evalIBkFSUS.precision(1) +";" + evalIBkFSUS.recall(1) +  ";" + evalIBkFSUS.areaUnderROC(1) + ";" + evalIBkFSUS.kappa() + "\n");
			finalData.add(PROJ+";" + numbOfTraining + ";" + perc.get(0) + ";" + perc.get(1) + ";" + perc.get(2) + ";" + NB +";" + UNDERSAMPL_FS+ evalNBFSUS.numTruePositives(1) + ";"+ evalNBFSUS.numFalsePositives(1) + ";"+ evalNBFSUS.numTrueNegatives(1)+ ";"+ evalNBFSUS.numFalseNegatives(1)+ ";" + evalNBFSUS.precision(1) +";" + evalNBFSUS.recall(1) +  ";" + evalNBFSUS.areaUnderROC(1) + ";" + evalNBFSUS.kappa() + "\n");
			finalData.add(PROJ+";" + numbOfTraining + ";" + perc.get(0) + ";" + perc.get(1) + ";" + perc.get(2) + ";" + RF +";" + UNDERSAMPL_FS + evalRFFSUS.numTruePositives(1) + ";"+ evalRFFSUS.numFalsePositives(1) + ";"+ evalRFFSUS.numTrueNegatives(1)+ ";"+ evalRFFSUS.numFalseNegatives(1)+ ";" + evalRFFSUS.precision(1) +";" + evalRFFSUS.recall(1) +  ";" + evalRFFSUS.areaUnderROC(1) + ";" + evalRFFSUS.kappa() + "\n");
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
			

			Evaluation evalRFSM = new Evaluation(testing);	
			evalRFSM.evaluateModel(fcRF, testing);
			Evaluation evalIBkSM = new Evaluation(testing);	
			evalIBkSM.evaluateModel(fcIBk, testing);
			Evaluation evalNBSM = new Evaluation(testing);	
			evalNBSM.evaluateModel(fcNB, testing);
			
			
			//with filters
			//generate filter
			AttributeSelection filter = applyFeatureSelection(training);
			
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
			
			Evaluation evalRFFSSM = new Evaluation(testing);
			Evaluation evalIBkFSSM = new Evaluation(testing);
			Evaluation evalNBFSSM = new Evaluation(testing);
			
		    evalRFFSSM.evaluateModel(fcRFFS, testingFiltered);
		    evalNBFSSM.evaluateModel(fcNBFS, testingFiltered);
		    evalIBkFSSM.evaluateModel(fcIBkFS, testingFiltered);
			
			
			finalData.add(PROJ+";" + numbOfTraining + ";" + perc.get(0) + ";" + perc.get(1) + ";" + perc.get(2) + ";" + "IBk;"+ SMOTE_NOFS + evalIBkSM.numTruePositives(1) + ";"+ evalIBkSM.numFalsePositives(1) + ";"+ evalIBkSM.numTrueNegatives(1)+ ";"+ evalIBkSM.numFalseNegatives(1)+ ";" + evalIBkSM.precision(1) +";" + evalIBkSM.recall(1) +  ";" + evalIBkSM.areaUnderROC(1) + ";" + evalIBkSM.kappa() + "\n");
			finalData.add(PROJ+";" + numbOfTraining + ";" + perc.get(0) + ";" + perc.get(1) + ";" + perc.get(2) + ";" + NB +";" + SMOTE_NOFS + evalNBSM.numTruePositives(1) + ";"+ evalNBSM.numFalsePositives(1) + ";"+ evalNBSM.numTrueNegatives(1)+ ";"+ evalNBSM.numFalseNegatives(1)+ ";" + evalNBSM.precision(1) +";" + evalNBSM.recall(1) +  ";" + evalNBSM.areaUnderROC(1) + ";" + evalNBSM.kappa() + "\n");
			finalData.add(PROJ+";" + numbOfTraining + ";" + perc.get(0) + ";" + perc.get(1) + ";" + perc.get(2) + ";" + RF +";" + SMOTE_NOFS + evalRFSM.numTruePositives(1) + ";"+ evalRFSM.numFalsePositives(1) + ";"+ evalRFSM.numTrueNegatives(1)+ ";"+ evalRFSM.numFalseNegatives(1)+ ";" + evalRFSM.precision(1) +";" + evalRFSM.recall(1) +  ";" + evalRFSM.areaUnderROC(1) + ";" + evalRFSM.kappa() + "\n");
			
			finalData.add(PROJ+";" + numbOfTraining + ";" + perc.get(0) + ";" + perc.get(1) + ";" + perc.get(2) + ";" + "IBk;"+ SMOTE_FS + evalIBkFSSM.numTruePositives(1) + ";"+ evalIBkFSSM.numFalsePositives(1) + ";"+ evalIBkFSSM.numTrueNegatives(1)+ ";"+ evalIBkFSSM.numFalseNegatives(1)+ ";" + evalIBkFSSM.precision(1) +";" + evalIBkFSSM.recall(1) +  ";" + evalIBkFSSM.areaUnderROC(1) + ";" + evalIBkFSSM.kappa() + "\n");
			finalData.add(PROJ+";" + numbOfTraining + ";" + perc.get(0) + ";" + perc.get(1) + ";" + perc.get(2) + ";" + NB +";" + SMOTE_FS + evalNBFSSM.numTruePositives(1) + ";"+ evalNBFSSM.numFalsePositives(1) + ";"+ evalNBFSSM.numTrueNegatives(1)+ ";"+ evalNBFSSM.numFalseNegatives(1)+ ";" + evalNBFSSM.precision(1) +";" + evalNBFSSM.recall(1) +  ";" + evalNBFSSM.areaUnderROC(1) + ";" + evalNBFSSM.kappa() + "\n");
			finalData.add(PROJ+";" + numbOfTraining + ";" + perc.get(0) + ";" + perc.get(1) + ";" + perc.get(2) + ";" + RF +";" + SMOTE_FS + evalRFFSSM.numTruePositives(1) + ";"+ evalRFFSSM.numFalsePositives(1) + ";"+ evalRFFSSM.numTrueNegatives(1)+ ";"+ evalRFFSSM.numFalseNegatives(1)+ ";" + evalRFFSSM.precision(1) +";" + evalRFFSSM.recall(1) +  ";" + evalRFFSSM.areaUnderROC(1) + ";" + evalRFFSSM.kappa() + "\n");
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
			

			Evaluation evalRFOS = new Evaluation(testing);	
			evalRFOS.evaluateModel(fcRF, testing);
			Evaluation evalIBkOS = new Evaluation(testing);	
			evalIBkOS.evaluateModel(fcIBk, testing);
			Evaluation evalNBOS = new Evaluation(testing);	
			evalNBOS.evaluateModel(fcNB, testing);
			
			//with filters
			//generate filter
			AttributeSelection filter = applyFeatureSelection(training);
			
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
			
			Evaluation evalRFFSOS = new Evaluation(testing);
			Evaluation evalIBkFSOS = new Evaluation(testing);
			Evaluation evalNBFSOS = new Evaluation(testing);
			
		    evalRFFSOS.evaluateModel(fcRFFS, testingFiltered);
		    evalNBFSOS.evaluateModel(fcNBFS, testingFiltered);
		    evalIBkFSOS.evaluateModel(fcIBkFS, testingFiltered);
			
			
			finalData.add(PROJ+";" + numbOfTraining + ";" + perc.get(0) + ";" + perc.get(1) + ";" + perc.get(2) + ";" + "IBk;"+ OVERSAMPLING_NOFS + evalIBkOS.numTruePositives(1) + ";"+ evalIBkOS.numFalsePositives(1) + ";"+ evalIBkOS.numTrueNegatives(1)+ ";"+ evalIBkOS.numFalseNegatives(1)+ ";" + evalIBkOS.precision(1) +";" + evalIBkOS.recall(1) +  ";" + evalIBkOS.areaUnderROC(1) + ";" + evalIBkOS.kappa() + "\n");
			finalData.add(PROJ+";" + numbOfTraining + ";" + perc.get(0) + ";" + perc.get(1) + ";" + perc.get(2) + ";" + NB + ";" + OVERSAMPLING_NOFS + evalNBOS.numTruePositives(1) + ";"+ evalNBOS.numFalsePositives(1) + ";"+ evalNBOS.numTrueNegatives(1)+ ";"+ evalNBOS.numFalseNegatives(1)+ ";" + evalNBOS.precision(1) +";" + evalNBOS.recall(1) +  ";" + evalNBOS.areaUnderROC(1) + ";" + evalNBOS.kappa() + "\n");
			finalData.add(PROJ+";" + numbOfTraining + ";" + perc.get(0) + ";" + perc.get(1) + ";" + perc.get(2) + ";" + RF +";" + OVERSAMPLING_NOFS + evalRFOS.numTruePositives(1) + ";"+ evalRFOS.numFalsePositives(1) + ";"+ evalRFOS.numTrueNegatives(1)+ ";"+ evalRFOS.numFalseNegatives(1)+ ";" + evalRFOS.precision(1) +";" + evalRFOS.recall(1) +  ";" + evalRFOS.areaUnderROC(1) + ";" + evalRFOS.kappa() + "\n");
			
			finalData.add(PROJ+";" + numbOfTraining + ";" + perc.get(0) + ";" + perc.get(1) + ";" + perc.get(2) + ";" + "IBk;"+ OVERSAMPLING_FS + evalIBkFSOS.numTruePositives(1) + ";"+ evalIBkFSOS.numFalsePositives(1) + ";"+ evalIBkFSOS.numTrueNegatives(1)+ ";"+ evalIBkFSOS.numFalseNegatives(1)+ ";" + evalIBkFSOS.precision(1) +";" + evalIBkFSOS.recall(1) +  ";" + evalIBkFSOS.areaUnderROC(1) + ";" + evalIBkFSOS.kappa() + "\n");
			finalData.add(PROJ+";" + numbOfTraining + ";" + perc.get(0) + ";" + perc.get(1) + ";" + perc.get(2) + ";" + NB +";" + OVERSAMPLING_FS + evalNBFSOS.numTruePositives(1) + ";"+ evalNBFSOS.numFalsePositives(1) + ";"+ evalNBFSOS.numTrueNegatives(1)+ ";"+ evalNBFSOS.numFalseNegatives(1)+ ";" + evalNBFSOS.precision(1) +";" + evalNBFSOS.recall(1) +  ";" + evalNBFSOS.areaUnderROC(1) + ";" + evalNBFSOS.kappa() + "\n");
			finalData.add(PROJ+";" + numbOfTraining + ";" + perc.get(0) + ";" + perc.get(1) + ";" + perc.get(2) + ";" + RF +";" + OVERSAMPLING_FS + evalRFFSOS.numTruePositives(1) + ";"+ evalRFFSOS.numFalsePositives(1) + ";"+ evalRFFSOS.numTrueNegatives(1)+ ";"+ evalRFFSOS.numFalseNegatives(1)+ ";" + evalRFFSOS.precision(1) +";" + evalRFFSOS.recall(1) +  ";" + evalRFFSOS.areaUnderROC(1) + ";" + evalRFFSOS.kappa() + "\n");
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

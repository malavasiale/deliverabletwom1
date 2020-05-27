package deliverabletwo;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import weka.attributeSelection.CfsSubsetEval;
import weka.attributeSelection.GreedyStepwise;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.lazy.IBk;
import weka.classifiers.trees.RandomForest;
import weka.filters.supervised.attribute.AttributeSelection;
import weka.filters.Filter;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

public class WekaTestingFS {
	
	private static List<String> finalData = new ArrayList<String>();
	
	private static final String PROJ = "tajo";
	private static final String WEKA_OUTPUT_FILTER = PROJ+"wekaOutputFiltered.csv";
	private static final String TRAINING_ARFF = PROJ+"trainingSet.arff";
	private static final String TESTING_ARFF = PROJ+"testingSet.arff";
	
	public static void evaluateFilter(Integer numbOfTraining) throws Exception {
		DataSource source1 = new DataSource(TRAINING_ARFF);
		Instances noFilterTraining = source1.getDataSet();
		DataSource source2 = new DataSource(TESTING_ARFF);
		Instances testingNoFilter = source2.getDataSet();
		
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
		
		int numAttrNoFilter = noFilterTraining.numAttributes();
		noFilterTraining.setClassIndex(numAttrNoFilter - 1);
		testingNoFilter.setClassIndex(numAttrNoFilter - 1);
		
		int numAttrFiltered = filteredTraining.numAttributes();

		//System.out.println("Number of trainings : "+ numbOfTraining);
		//System.out.println("No filter attr: "+ numAttrNoFilter);
		//System.out.println("Filtered attr: "+ numAttrFiltered);
		
		IBk classifierIBk = new IBk();
		NaiveBayes classifierNB = new NaiveBayes();
		RandomForest classifierRF = new RandomForest();

		//evaluation with no filtered
		/*Evaluation evalClass = new Evaluation(testingNoFilter);
		classifier.buildClassifier(noFilterTraining);
	    evalClass.evaluateModel(classifier, testingNoFilter); 
		
		System.out.println("AUC no filter = "+evalClass.areaUnderROC(1));
		System.out.println("Kappa no filter = "+evalClass.kappa());
		System.out.println("Precision no filter = "+evalClass.precision(1));
		System.out.println("Recall no filter = "+evalClass.recall(1));*/
		
		
		//evaluation with filtered
		filteredTraining.setClassIndex(numAttrFiltered - 1);
		Instances testingFiltered = Filter.useFilter(testingNoFilter, filter);
		testingFiltered.setClassIndex(numAttrFiltered - 1);
		
		classifierRF.buildClassifier(filteredTraining);
		classifierIBk.buildClassifier(filteredTraining);
		classifierNB.buildClassifier(filteredTraining);
		
		Evaluation evalRF = new Evaluation(testingFiltered);
		Evaluation evalIBk = new Evaluation(testingFiltered);
		Evaluation evalNB = new Evaluation(testingFiltered);
		
	    evalRF.evaluateModel(classifierRF, testingFiltered);
	    evalNB.evaluateModel(classifierNB, testingFiltered);
	    evalIBk.evaluateModel(classifierIBk, testingFiltered);
	    
		finalData.add(PROJ+";" + numbOfTraining + ";" + numAttrNoFilter + ";" + numAttrFiltered + ";" + "IBk;" + evalIBk.precision(1) +";" + evalIBk.recall(1) +  ";" + evalIBk.areaUnderROC(1) + ";" + evalIBk.kappa() + "\n");
		finalData.add(PROJ+";" + numbOfTraining + ";" + numAttrNoFilter + ";" + numAttrFiltered + ";"  + "NaiveBayes;" + evalNB.precision(1) +";" + evalNB.recall(1) +  ";" + evalNB.areaUnderROC(1) + ";" + evalNB.kappa() + "\n");
		finalData.add(PROJ+";" + numbOfTraining + ";" + numAttrNoFilter + ";" + numAttrFiltered + ";"  + "RandomForest;" + evalRF.precision(1) +";" + evalRF.recall(1) +  ";" + evalRF.areaUnderROC(1) + ";" + evalRF.kappa() + "\n");
	}
	
	public static void main(String[] args) throws Exception {
		Long maxvers = GetBuggy.firstHalfVersions();
		
		for(int i = 1 ; i < maxvers ; i++) {
			WekaTesting.makeSets(i);
			evaluateFilter(i);
		}
		
		try(FileWriter csvEvaluate = new FileWriter(WEKA_OUTPUT_FILTER);){
			csvEvaluate.write("Dataset;#TrainingRelease;#AttributeNoFilter;#AttributeFiltered;Classifier;Precision;Recall;AUC;Kappa\n");
			
			for(String elem : finalData) {
				csvEvaluate.write(elem);
			}
		}
	}

}

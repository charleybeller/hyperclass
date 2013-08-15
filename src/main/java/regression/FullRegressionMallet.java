import java.io.*;
import java.util.Random;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.HashMap;

import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.*;
import cc.mallet.types.*;
import cc.mallet.classify.*;

public class FullRegressionMallet {

	public static Classifier trainClassifier(InstanceList trainingInstances) {

		ClassifierTrainer trainer = new MaxEntTrainer(1000);
		return trainer.train(trainingInstances);
	}
    
	public static Classifier loadClassifier(File serializedFile) throws FileNotFoundException, IOException, ClassNotFoundException {

		Classifier classifier;

		ObjectInputStream ois = new ObjectInputStream (new FileInputStream (serializedFile));
		classifier = (Classifier) ois.readObject();
		ois.close();

		return classifier;
	}

	public static void saveClassifier(Classifier classifier, File serializedFile) throws IOException {

		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream (serializedFile));
		oos.writeObject (classifier);
		oos.close();
	}

	public static Trial evaluate(Classifier classifier, InstanceList instances){
		
		Trial trial = new Trial(classifier, instances);

		/*int numClasses = trial.numClasses();
		Object[] keyOrder = trial.getLabels();
	
		float A = trial.getAccuracy();
		float[] P = new float[numClasses];
		float[] R = new float[numClasses];
		float[] F = new float[numClasses];
		trial.updatePrecision(P, keyOrder);	
		trial.updateRecall(R, keyOrder);	
		trial.updateF1(F, keyOrder);	

		System.out.println("Accuracy: " + trial.getAccuracy());
		System.out.println("\t\tP\tR\tF1");
		for(int i = 0; i < keyOrder.length; i ++){
			System.out.format("Class '%s':\t%.03f\t%.03f\t%.03f\n",keyOrder[i], P[i], R[i], F[i]);
		}*/
		
		return trial;
	}

	public static TrialRun testTrainSplit(InstanceList instances, Double l1Weight) {

		int TRAINING = 0;
		int TESTING = 1;
		int VALIDATION = 2;

		InstanceList[] instanceLists = instances.split(new Random(), new double[] {0.9, 0.1, 0.0});

		Classifier classifier = trainClassifier( instanceLists[TRAINING] );
		Trial testTrial = evaluate(classifier, instanceLists[TESTING]);
		return new TrialRun(classifier, testTrial);
	}

	public static void crossValidate(InstanceList instances, int nfold) {
		crossValidate(instances, nfold, 1000, null);
	}

	public static void crossValidate(InstanceList instances, int nfold, int l1Weight) {
		crossValidate(instances, nfold, l1Weight, null);
	}

	public static void crossValidate(InstanceList instances, int nfold, double l1Weight, PrintWriter outfile) {

		ArrayList<TrialRun> trials = new ArrayList<TrialRun>();

		for(int i = 0; i < nfold; i++){
			trials.add(testTrainSplit(instances, l1Weight));
		}

		int numClasses = trials.get(0).numClasses();
		Object[] keyOrder = trials.get(0).getLabels();
	
		float avgA = 0;
		float[] avgP = new float[numClasses];
		float[] avgR = new float[numClasses];
		float[] avgF = new float[numClasses];

		for(TrialRun t : trials){
			t.updatePrecision(avgP, keyOrder);	
			t.updateRecall(avgR, keyOrder);	
			t.updateF1(avgF, keyOrder);	
			avgA += t.trial.getAccuracy();
		}
		
		outfile.print(l1Weight+"\n");
		outfile.printf("AVERAGE ACCURACY:\t%.03f\n", avgA/nfold);
		outfile.printf("AVERAGE\t\tP\tR\tF1\n");
		for(int i = 0; i < keyOrder.length; i ++){
			outfile.printf("Class '%s':\t%.03f\t%.03f\t%.03f\n",keyOrder[i].toString(), avgP[i]/nfold, avgR[i]/nfold, avgF[i]/nfold);
		}

		System.out.println();
		System.out.format("AVERAGE ACCURACY:\t%.03f\n", avgA/nfold);
		System.out.println("AVERAGE\t\tP\tR\tF1");
		for(int i = 0; i < keyOrder.length; i ++){
			System.out.format("Class '%s':\t%.03f\t%.03f\t%.03f\n",keyOrder[i].toString(), avgP[i]/nfold, avgR[i]/nfold, avgF[i]/nfold);
		}
	}
	
	public static void trainAndTest(Classifier classifier, String testFileName, double l1Weight, PrintWriter outfile) throws IOException{

	        InstanceList test = new InstanceList(classifier.getInstancePipe());
	        CsvIterator reader = FullDataReader.getIter(new File(testFileName));
        	test.addThruPipe(reader);

		TrialRun t = new TrialRun(classifier, evaluate(classifier, test));

		int numClasses = t.numClasses();
		Object[] keyOrder = t.getLabels();
	
		float A = 0;
		float[] P = new float[numClasses];
		float[] R = new float[numClasses];
		float[] F = new float[numClasses];

		t.updatePrecision(P, keyOrder);	
		t.updateRecall(R, keyOrder);	
		t.updateF1(F, keyOrder);	
		A += t.trial.getAccuracy();
		
		outfile.printf("ACCURACY:\t%.03f\n", A);
		outfile.printf("\t\tP\tR\tF1\n");
		for(int i = 0; i < keyOrder.length; i ++){
			outfile.printf("Class '%s':\t%.03f\t%.03f\t%.03f\n",keyOrder[i].toString(), P[i], R[i], F[i]);
		}

		System.out.println();
		System.out.format("ACCURACY:\t%.03f\n", A);
		System.out.println("\t\tP\tR\tF1");
		for(int i = 0; i < keyOrder.length; i ++){
			System.out.format("Class '%s':\t%.03f\t%.03f\t%.03f\n",keyOrder[i].toString(), P[i], R[i], F[i]);
		}
	}

	public static void trainAndTest(Classifier classifier, InstanceList test, double l1Weight, PrintWriter outfile) throws IOException{


		TrialRun t = new TrialRun(classifier, evaluate(classifier, test));

		int numClasses = t.numClasses();
		Object[] keyOrder = t.getLabels();
	
		float A = 0;
		float[] P = new float[numClasses];
		float[] R = new float[numClasses];
		float[] F = new float[numClasses];

		t.updatePrecision(P, keyOrder);	
		t.updateRecall(R, keyOrder);	
		t.updateF1(F, keyOrder);	
		A += t.trial.getAccuracy();
		
		outfile.printf("ACCURACY:\t%.03f\n", A);
		outfile.printf("\t\tP\tR\tF1\n");
		for(int i = 0; i < keyOrder.length; i ++){
			outfile.printf("Class '%s':\t%.03f\t%.03f\t%.03f\n",keyOrder[i].toString(), P[i], R[i], F[i]);
		}

		System.out.println();
		System.out.format("ACCURACY:\t%.03f\n", A);
		System.out.println("\t\tP\tR\tF1");
		for(int i = 0; i < keyOrder.length; i ++){
			System.out.format("Class '%s':\t%.03f\t%.03f\t%.03f\n",keyOrder[i].toString(), P[i], R[i], F[i]);
		}
	}

	public static void trainAndTest(InstanceList train, InstanceList test, double l1Weight, PrintWriter outfile) throws IOException{

		Classifier classifier = trainClassifier(train);

		TrialRun t = new TrialRun(classifier, evaluate(classifier, test));

		int numClasses = t.numClasses();
		Object[] keyOrder = t.getLabels();
	
		float A = 0;
		float[] P = new float[numClasses];
		float[] R = new float[numClasses];
		float[] F = new float[numClasses];

		t.updatePrecision(P, keyOrder);	
		t.updateRecall(R, keyOrder);	
		t.updateF1(F, keyOrder);	
		A += t.trial.getAccuracy();
		
		outfile.printf("ACCURACY:\t%.03f\n", A);
		outfile.printf("\t\tP\tR\tF1\n");
		for(int i = 0; i < keyOrder.length; i ++){
			outfile.printf("Class '%s':\t%.03f\t%.03f\t%.03f\n",keyOrder[i].toString(), P[i], R[i], F[i]);
		}

		System.out.println();
		System.out.format("ACCURACY:\t%.03f\n", A);
		System.out.println("\t\tP\tR\tF1");
		for(int i = 0; i < keyOrder.length; i ++){
			System.out.format("Class '%s':\t%.03f\t%.03f\t%.03f\n",keyOrder[i].toString(), P[i], R[i], F[i]);
		}
	}

	public static void trainAndTest(InstanceList train, String testFileName, double l1Weight, PrintWriter outfile, String modelPath) throws IOException{

		Classifier classifier = trainClassifier(train);

		saveClassifier(classifier, new File(modelPath));	

	        InstanceList test = new InstanceList(classifier.getInstancePipe());
	        CsvIterator reader = FullDataReader.getIter(new File(testFileName));
        	test.addThruPipe(reader);
	        test.save(new File(testFileName.replaceAll("raw", "inst")));
		for(Instance i : train){
			System.out.println(i.getData());
		}
		for(Instance i : test){
			System.out.println(i.getData());
		}

		TrialRun t = new TrialRun(classifier, evaluate(classifier, test));

		int numClasses = t.numClasses();
		Object[] keyOrder = t.getLabels();
	
		float A = 0;
		float[] P = new float[numClasses];
		float[] R = new float[numClasses];
		float[] F = new float[numClasses];

		t.updatePrecision(P, keyOrder);	
		t.updateRecall(R, keyOrder);	
		t.updateF1(F, keyOrder);	
		A += t.trial.getAccuracy();
		
		outfile.printf("ACCURACY:\t%.03f\n", A);
		outfile.printf("\t\tP\tR\tF1\n");
		for(int i = 0; i < keyOrder.length; i ++){
			outfile.printf("Class '%s':\t%.03f\t%.03f\t%.03f\n",keyOrder[i].toString(), P[i], R[i], F[i]);
		}

		System.out.println();
		System.out.format("ACCURACY:\t%.03f\n", A);
		System.out.println("\t\tP\tR\tF1");
		for(int i = 0; i < keyOrder.length; i ++){
			System.out.format("Class '%s':\t%.03f\t%.03f\t%.03f\n",keyOrder[i].toString(), P[i], R[i], F[i]);
		}
	}

	public static void gridSearch(InstanceList instances, String fileName) throws IOException{
	
		Double[] params = {0.001, 0.01, 0.1, 1.0, 10.0, 100.0, 1000.0, 10000.0};
		PrintWriter outFile = new PrintWriter(fileName);
		for( double l1 : params){
			crossValidate(instances, 10, l1, outFile);
		}
		outFile.close();
	}

	public static class TrialRun {

		Classifier classifier;
		Trial trial;
		HashMap<String, Integer> labelMap = new HashMap<String, Integer>();

		public TrialRun(Classifier c, Trial t){
			this.classifier = c;
			this.trial = t;
			for(Object name : getLabels()){
				labelMap.put(name.toString(), getIndex(name.toString()));
			}
		}

		public int numClasses(){ return classifier.getLabelAlphabet().size(); }

		public Object[] getLabels() { return classifier.getLabelAlphabet().toArray(); }

		public int getIndex(String name){ 
			for(int i = 0; i < numClasses(); i++){
				if(name.equals(classifier.getLabelAlphabet().lookupLabel(i).toString())){ return i; }
			}
			return -1;
		}

		public void updatePrecision( float[] p, Object[] keyOrder ){ 
			for(Object name : keyOrder){
				int index = labelMap.get(name.toString());
				p[index] += trial.getPrecision(index);
			}
		}
		
		public void updateRecall( float[] r, Object[] keyOrder ){ 
			for(Object name : keyOrder){
				int index = labelMap.get(name.toString());
				r[index] += trial.getRecall(index);
			}
		}
		
		public void updateF1( float[] f, Object[] keyOrder ){ 
			for(Object name : keyOrder){
				int index = labelMap.get(name.toString());
				f[index] += trial.getF1(name);
			}
		}
	}

}

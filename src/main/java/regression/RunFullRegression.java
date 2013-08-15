import java.io.*;

import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.*;
import cc.mallet.types.*;
import cc.mallet.classify.*;

public class RunFullRegression{

	public static void main(String[] args) throws IOException, ClassNotFoundException{

		String path = args[0]; int folds = 10;
		if(args.length == 2){ 
			folds = Integer.parseInt(args[1]); 
		}

        	InstanceList instances = InstanceList.load(new File(path+"/data.inst"));
        	String testFileName = path+"/dev.raw";
        	//String testFileName = path+"/data.full";
		//Classifier c = FullRegressionMalletg.trainClassifier(instances);
		//FullRegressionMalletg.saveClassifier(c, new File(path+"/model"));	
		//FullRegressionMalletg.gridSearch(instances, path+"/tuneCv");
		PrintWriter outFile = new PrintWriter(path+"/results");
		String modelPath = path+"/model";
		//FullRegressionMalletg.crossValidate(instances, folds, 1000, outFile);
		FullRegressionMallet.trainAndTest(instances, testFileName, 1000, outFile, modelPath);
        	//Classifier classifier = RegressionMallet.loadClassifier(new File(modelPath));
		//FullRegressionMallet.trainAndTest(classifier, testFileName, 1000, outFile);
		outFile.close();
		//FullRegressionMalletg.crossValidate(instances, 10, 100);
	}

}

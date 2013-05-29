import java.io.*;

import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.*;
import cc.mallet.types.*;
import cc.mallet.classify.*;

public class RunRegression{

	public static void main(String[] args){

        	InstanceList instances = InstanceList.load(new File("blah"));
		RegressionMallet.testTrainSplit(instances);
		//Classifier clf = RegressionMallet.trainClassifier(instances);
		//RegressionMallet.evaluate(clf, instances);

	}

}

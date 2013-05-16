import java.io.File;
import java.io.IOException;
import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;

import FeatureExtractor.*;

public class Regression {

	public static void main(String[] args) throws IOException{

		Problem problem = new Problem();
		problem.l = 3; // number of training examples
		problem.n = 3; // number of features
		
		double[] labels = {1, -1, 1};

		FeatureExtractor fe = new FeatureExtractor();
		FeatureNode[][] features = fe.extractFeatures(new String[] {"hi", "bye", "yo"});

		problem.x = features;  // feature nodes
		problem.y = labels;    // target values

		SolverType solver = SolverType.L2R_LR; // -s 0
		double C = 1.0;    // cost of constraints violation
		double eps = 0.01; // stopping criteria

		Parameter parameter = new Parameter(solver, C, eps);
		Model model = Linear.train(problem, parameter);
		File modelFile = new File("model");
		model.save(modelFile);

		Linear.crossValidation(problem, parameter, 3, labels);
//		double prediction = Linear.predict(model, instance);
//		System.out.println(prediction);
	}
}

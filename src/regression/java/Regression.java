import java.io.File;
import java.io.IOException;
import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;

public class Regression {

	/**
 	* Train a model on X with labels Y 
 	*/
	public static Model train(DataMatrix dm){ 
		FeatureNode[][] X = dm.getX();
		double[] Y = dm.getY();
		
		Problem problem = new Problem();
		problem.x = X;  // feature nodes
		problem.y = Y;  // target values
		problem.l = X.length; // number of training examples
		problem.n = X[0].length; // number of features

		SolverType solver = SolverType.L2R_LR; 
		double C = 1.0;    // cost of constraints violation
		double eps = 0.01; // stopping criteria

		Parameter parameter = new Parameter(solver, C, eps);
		return Linear.train(problem, parameter);
	}

	/**
 	* Test model on X with known labels Y 
 	*/
	public static double test(Model model, DataMatrix dm){
		FeatureNode[][] X = dm.getX();
		double[] Y = dm.getY();
		
		Problem problem = new Problem();
		problem.x = X;  // feature nodes
		problem.y = Y;  // target values
		problem.l = X.length; // number of training examples
		problem.n = X[0].length; // number of features

		float correct = 0; float total = 0; double prediction = 0;
		for(int i = 0; i < X.length; i++){
			prediction = Linear.predict(model, X[i]);
			System.out.println(dm.get(i));
			if(prediction == Y[i]){ correct++; }
			total++;			
		}
		return correct / total;

	}
	
	public static void main(String[] args) throws IOException{

		DataMatrix dm = new DataMatrix("fake_input_file.txt");
		DataMatrix[] trainAndTest = dm.splitTrainTest(10);

		DataMatrix trainDM = trainAndTest[0]; 
		DataMatrix testDM = trainAndTest[1];
	
		Model model = train(trainDM);

		File modelFile = new File("model");
		model.save(modelFile);
	
		System.out.println(test(model, testDM));
	}
}

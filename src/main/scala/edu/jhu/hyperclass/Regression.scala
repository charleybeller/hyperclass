import java.io.File;
import java.io.IOException;
import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;

object Regression {

	/**
 	* Train a model on X with labels Y 
 	*/
	def train(dm : DataMatrix) = { 
		val X : Array[Array[Feature]] = dm.getX();
		val Y : Array[Double] = dm.getY();
		
		val problem : Problem = new Problem();
		problem.x = X;  // feature nodes
		problem.y = Y;  // target values
		problem.l = X.length; // number of training examples
		problem.n = X(0).length; // number of features

		val solver : SolverType = SolverType.L2R_LR; 
		val C : Double = 1.0;    // cost of constraints violation
		val eps : Double = 0.01; // stopping criteria

		val parameter : Parameter = new Parameter(solver, C, eps);
		Linear.train(problem, parameter);
	}

	/**
 	* Test model on X with known labels Y 
 	*/
	def test(model : Model, dm : DataMatrix) : (Double, Double, Double) = {
		val X : Array[Array[Feature]] = dm.getX();
		val Y : Array[Double] = dm.getY();
		
		var tp : Float = 0
		var fp : Float = 0
		var fn : Float = 0
		var yhat : Double = 0;
		for((x,y) <- X.zip(Y)){
			yhat = Linear.predict(model, x);
			println("Predicted: " + yhat + " True:  " + y)
			if(y == 1 && yhat == 1) tp = tp + 1
			if(y == 0 && yhat == 1) fp = fp + 1
			if(y == 1 && yhat == 0) fn = fn + 1
		}	
		val p = tp / (tp + fp)
		val r = tp / (tp + fn)
		val f = 2 * (p * r) / (p + r)
		(p, r, f)	
	}
	
	def main(args : Array[String]){

		println("Initializing...")
		var dm : DataMatrix = new DataMatrix(new Array[PhrasePair](0));

		val posFile = "output/xy.txt"
		val allFile = "output/joined.txt"

		dm.initializeFromFile(posFile, allFile)
		
		println("Splitting train and test...")
		val (trainDM : DataMatrix, testDM : DataMatrix) = dm splitTrainTest 10;

		println("Training...")
		train(trainDM);
		var model : Model = train(trainDM);
		
		//var modelFile : File = new File("model");
		//model save modelFile;
	
		println("Testing...")
		val (p, r, f) = test(model, testDM)
		println("Precision: " + p + " Recall: " + r + " Fscore: " + f)
	}
}

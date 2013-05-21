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
	def test(model : Model, dm : DataMatrix) : Double = {
		val X : Array[Array[Feature]] = dm.getX();
		val Y : Array[Double] = dm.getY();
		
		var correct : Float = 0; var total : Float = 0; var prediction : Double = 0;
		for((x,y) <- X.zip(Y)){
			prediction = Linear.predict(model, x);
			println(prediction + "  " + y)
			if(prediction == y) correct = correct + 1; 
			total = total + 1;	
		}		
		correct / total;
	}
	
	def main(args : Array[String]){

		println("Initializing...")
		var dm : DataMatrix = new DataMatrix(new Array[PhrasePair](0));
		for(a <- args){
			println(a)
		}

		val posFile = "output/xy.txt"
		val negFile = "output/zz.txt"

		dm.initializeFromFile(posFile)
		
		println("Splitting train and test...")
		val (trainDM : DataMatrix, testDM : DataMatrix) = dm splitTrainTest 10;

		println("Training...")
		train(trainDM);
		var model : Model = train(trainDM);
		
		//var modelFile : File = new File("model");
		//model save modelFile;
	
		println("Testing...")
		println(test(model, testDM));
	}
}

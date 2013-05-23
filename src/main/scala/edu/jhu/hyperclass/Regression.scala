import java.io.File;
import java.io.IOException;
import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;
import scala.collection.mutable.ListBuffer

object Regression extends App {

	/**
 	* Train a model on X with labels Y 
 	*/
	def train(dm : DataMatrix) = { 
		val X : Array[Array[Feature]] = dm.getX();
		val Y : Array[Double] = dm.getY();
	
		println(X.length)
	
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
	def baselineTest(traindm : DataMatrix, testdm : DataMatrix) : (Double, Double, Double, Double) = {
		val X : Array[Array[Feature]] = testdm.getX();
		val Y : Array[Double] = testdm.getY();
	
		val yhat = traindm.getY().groupBy(identity).mapValues(_.size).maxBy(_._2)._1
	
		var tp : Float = 0
		var fp : Float = 0
		var fn : Float = 0
		var t : Float = 0
		for((x,y) <- X.zip(Y)){
			if(y == 1 && yhat == 1) tp = tp + 1
			if(y == 0 && yhat == 1) fp = fp + 1
			if(y == 1 && yhat == 0) fn = fn + 1
			t = t + 1
		}	
		val a = (t - fp - fn) / t
		val p = tp / (tp + fp)
		val r = tp / (tp + fn)
		val f = 2 * (p * r) / (p + r)
		(p, r, f, a)	
	}

	/**
 	* Test model on X with known labels Y 
 	*/
	def test(model : Model, dm : DataMatrix) : (Double, Double, Double, Double) = {
		val X : Array[Array[Feature]] = dm.getX();
		val Y : Array[Double] = dm.getY();
	
		val out = new java.io.FileWriter("log")
	
		var tp : Float = 0
		var fp : Float = 0
		var fn : Float = 0
		var t : Float = 0
		var c : Float = 0
		var yhat : Double = 0;
		for(((x,y),p) <- X.zip(Y).zip(dm.pairs)){
			yhat = Linear.predict(model, x);
			out.write("Predicted: " + yhat + " True:  " + y + " " + p + "\n")
			if(y == 1 && yhat == 1) tp = tp + 1
			if(y == 0 && yhat == 1) fp = fp + 1
			if(y == 1 && yhat == 0) fn = fn + 1
			if(y == yhat) c = c + 1
			t = t + 1
		}	
		out.close
		val a = c / t
		val p = tp / (tp + fp)
		val r = tp / (tp + fn)
		val f = 2 * (p * r) / (p + r)
		(p, r, f, a)	
	}
	

	println("Initializing...")
	var dm : DataMatrix = new DataMatrix(Vector.empty);

	val posFile = "output/xy4.txt"
	val allFile = "output/joined.small"

	dm.initializeFromFile(posFile, allFile)
	
	println("Splitting train and test...")
	val (trainDM : DataMatrix, testDM : DataMatrix) = dm splitTrainTest 10;

	println("Training...")
	train(trainDM);
	var model : Model = train(trainDM);
	
	var modelFile : File = new File("model");
	model save modelFile;
	
	println("Testing...")

	val out = new java.io.FileWriter("results")
	val (p0, r0, f0, t0) = baselineTest(trainDM, testDM)
	println("Majority Guess Baseline - Precision: " + p0 + " Recall: " + r0 + " Fscore: " + f0 )
	out.write("Majority Guess Baseline - Precision: " + p0 + " Recall: " + r0 + " Fscore: " + f0 + " Accuracy: " + t0 + "\n")

	val (p, r, f, t) = test(model, testDM)
	println("Parse Features - Precision: " + p + " Recall: " + r + " Fscore: " + f )
	out.write("Parse Features - Precision: " + p + " Recall: " + r + " Fscore: " + f + " Accuracy: " + t + "\n")

	out.close
}

import java.io.File;
import java.io.IOException;
import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;
import scala.collection.immutable.HashMap

object Regression extends App {

	class ParameterMatrix {

		var params : Map[String, Vector[Double]] = new HashMap()
		params = params.updated("C", new Vector[Double](0, 0, 0))
		params = params.updated("Eps", new Vector[Double](0, 0, 0))

		def add(key : String, param : Double) = params = params.updated(key, params.get(key).get :+ param)
		def get(key : String) = params.get(key).get

	}

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
		val eps : Double = 1.0; // stopping criteria

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
	
		val out = new java.io.FileWriter("log_train")
	
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
	
	def tune(trainDM : DataMatrix, testDM : DataMatrix, params : ParameterMatrix ){
		
		val X : Array[Array[Feature]] = trainDM.getX();
		val Y : Array[Double] = trainDM.getY();
	
		val problem : Problem = new Problem();
		problem.x = X;  // feature nodes
		problem.y = Y;  // target values
		problem.l = X.length; // number of training examples
		problem.n = X(0).length; // number of features

		val solver : SolverType = SolverType.L2R_LR; 

		for(c <- params.get("C")) {
			for(eps <- params.get("Eps")){

				val parameter : Parameter = new Parameter(solver, c, eps);
				val model : Model  = Linear.train(problem, parameter);

				val (p, r, f, t) = test(model, testDM)
				val s = "[ "+c+" , "+eps+" ] Precision: "+p+" Recall: "+r+" Fscore: "+f+" Accuracy: "+t+"\n"
				println(s)
			}
		}
	}

	def crossValidate(dm : DataMatrix, numfolds : Int){
		
		val out = new java.io.FileWriter("results_train")
		var averageP : Double = 0;
		var averageR : Double = 0;
		var averageF : Double = 0;
		var averageT : Double = 0;

		for(fold <- 1 to numfolds){
		
			println("Splitting train and test...")
			val (trainDM : DataMatrix, testDM : DataMatrix) = dm splitTrainTest 10;
		
			println("Training...")
			train(trainDM);
			var model : Model = train(trainDM);

			val (p0, r0, f0, t0) = baselineTest(trainDM, testDM)
			println("Majority Guess Baseline - Precision: "+p0+" Recall: " + r0 + " Fscore: " + f0 + " Accuracy: " + t0 + "\n")
			out.write(fold + " Majority Guess - Precision: "+p0+" Recall: " + r0 + " Fscore: " + f0 + " Accuracy: " + t0 + "\n")

			val (p1, r1, f1, t1) = test(model, trainDM)
			println("Train Error - Precision: " + p1 + " Recall: " + r1 + " Fscore: " + f1 + " Accuracy: " + t1 + "\n")
			out.write(fold + " Train Error - Precision: " + p1 + " Recall: " + r1 + " Fscore: " + f1 + " Accuracy: " + t1 + "\n")

			val (p, r, f, t) = test(model, testDM)
			println("Parse Features - Precision: " + p + " Recall: " + r + " Fscore: " + f + " Accuracy: " + t + "\n")
			out.write(fold + " Features - Precision: " + p + " Recall: " + r + " Fscore: " + f + " Accuracy: " + t + "\n")

			averageP = averageP + p
			averageR = averageR + r
			averageF = averageF + f
			averageT = averageT + t
		}

		averageP = averageP / numfolds
		averageR = averageR / numfolds 
		averageF = averageF / numfolds 
		averageT = averageT / numfolds 
		println("Final Features - Precision: "+averageP+" Recall: "+averageR+" Fscore: "+averageF+" Accuracy: "+averageT + "\n")
		out.write("Final Features - Precision: "+averageP+" Recall: "+averageR+" Fscore: "+averageF+" Accuracy: "+averageT + "\n")
		out.close
	}	

	println("Initializing...")
	var dm : DataMatrix = new DataMatrix(Vector.empty);

	val posFile = "output/xy5.txt"
	val allFile = "output/joined.txt"

	dm.initializeFromFile(posFile, allFile, false, 10)

	crossValidate(dm, 3)

/*	
	val (trainDM : DataMatrix, testDM : DataMatrix) = dm splitTrainTest 10;
	
	var params = new ParameterMatrix()
	params.add("C", 0.001)
	params.add("C", 0.01)
	params.add("C", 0.1)
	params.add("C", 1)
	params.add("C", 10)
	params.add("C", 100)
	params.add("Eps", 0.001)
	params.add("Eps", 0.01)
	params.add("Eps", 0.1)
	params.add("Eps", 1)
	params.add("Eps", 10)
	params.add("Eps", 100)

	tune(trainDM, testDM, params)
*/

}

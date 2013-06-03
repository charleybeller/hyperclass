import io.Source
import breeze.data.Example
import breeze.config.{GenerateHelp, Help, CommandLineParser}
import breeze.linalg.{VectorBuilder, SparseVector}
import breeze.util.HashIndex
import breeze.classify.Classifier
import breeze.classify.LogisticClassifier
import breeze.stats.ContingencyStats
import breeze.optimize.FirstOrderMinimizer.OptParams
import scala.collection.immutable.HashMap
import collection.mutable.ArrayBuffer
import collection.immutable.Vector
import scala.util.Random

object RegressionBreeze {
			
	val featureIndex : HashIndex[String] = new HashIndex()
	
	class PhrasePairExample(_id : String, _label : Int, _features : Vector[String], size : Int) extends Example[Int, SparseVector[Double]] {
		val builder = new VectorBuilder[Double](size + 1)

		if( _features.size == 0){
	//		println("no features")
          		val fi = featureIndex.index("None")
          		builder.add(fi, 1.0)

		}else{	
			for(f <- _features){
          			val fi = featureIndex.index(f)
          			builder.add(fi, 1.0)
			}
		}
		
		val id = _id
		val label = _label
		val features = builder.toSparseVector
	}

	def toBreezeFormat(data : DataMatrix, featureSize : Int) : IndexedSeq[Example[Int, SparseVector[Double]]] = {

		var breezeData: IndexedSeq[PhrasePairExample] = new ArrayBuffer()
		for(p <- data.pairs){
//			if(p.rawFeatures.size > 0){
				breezeData = breezeData :+ new PhrasePairExample(p.toString, p.hypernym.toInt, p.rawFeatures, featureSize)
//			}
		}
		return breezeData
		
	}

	def crossValidate(data : DataMatrix) = {

		val nfold = 3
	
		var avgP = 0.0
		var avgR = 0.0
		var avgF = 0.0
		var avgA = 0.0
		var avgGP = 0.0
		var avgGN = 0.0
	
		for(n <- 0 until nfold){
			val (train, test) = data.splitTrainTest(10)
			val opt = OptParams(maxIterations=60,useStochastic=false,useL1=true)
			println("Training")
			println(data.featureSize + " " + test.pairs.size + " " + train.pairs.size)
      			val btrain = toBreezeFormat(train, data.featureSize)
      			val classifier = new LogisticClassifier.Trainer[Int, SparseVector[Double]](opt).train(btrain)
			var stats = ContingencyStats(classifier, btrain.toSeq)
      			print("Train Error ")
      			//println(stats)
			runTest(classifier, btrain)
      			print("Majority Baseline ")
			runBaselineTest(btrain, toBreezeFormat(test, data.featureSize))
      			print("Test Error ")
			stats = ContingencyStats(classifier, toBreezeFormat(test, data.featureSize).toSeq)
      			//println(stats)
			var (p, r, f, a, gp, gn) = runTest(classifier, toBreezeFormat(test, data.featureSize))
			avgP = avgP + p
			avgR = avgR + r
			avgF = avgF + f
			avgA = avgA + a
			avgGP = avgGP + gp
			avgGN = avgGN + gn
		}
		println("Average - P: "+avgP/nfold + " R: " + avgR/nfold + " F: "+avgF/nfold + " A: "+avgA/nfold + " GP " +avgGP/nfold + " GN " +avgGN/nfold)
    	}
	
	def runTest(model : Classifier[Int, SparseVector[Double]] , exs : IndexedSeq[Example[Int, SparseVector[Double]]]) : (Double, Double, Double, Double, Double, Double) = {
	
		val out = new java.io.FileWriter("log")
	
		var tp : Float = 0
		var fp : Float = 0
		var fn : Float = 0
		var t : Float = 0
		var c : Float = 0
		var yhat : Double = 0;
		var y : Double = 0;
		for(ex <- exs){
			yhat = model.classify(ex.features)
			y = ex.label
			out.write("Predicted: " + yhat + " True:  " + y + " " + ex.id + "\n")
			if(y == 1 && yhat == 1) tp = tp + 1
			if(y == 0 && yhat == 1) fp = fp + 1
			if(y == 1 && yhat == 0) fn = fn + 1
			if(y == yhat) c = c + 1
			t = t + 1
		}	
		out.close
		val a = c / t
		val p = if (tp + fp == 0) 0 else tp / (tp + fp)
		val r = if (tp + fn == 0) 0 else tp / (tp + fn)
		val f = if (p + r == 0) 0 else 2 * (p * r) / (p + r)
		val gp = tp + fp
		val tn = t - tp - fp - fn
		val gn = tn + fn
		println("guessed positive  " + gp+ " guessed negative " + gn)
		println(p + "  " + r + " " + f + " " + a)
		return (p,r,f,a,gp,gn)
	}
	
	def runBaselineTest(trainEx : IndexedSeq[Example[Int, SparseVector[Double]]], testEx : IndexedSeq[Example[Int, SparseVector[Double]]]): (Double, Double, Double, Double) = {

		val Y = for(ex <- trainEx) yield ex.label
		val Yt = for(ex <- testEx) yield ex.label
	
		val yhat = Y.groupBy(identity).mapValues(_.size).maxBy(_._2)._1
	
		var tp : Float = 0
		var fp : Float = 0
		var fn : Float = 0
		var t : Float = 0
		for(y <- Yt){
			if(y == 1 && yhat == 1) tp = tp + 1
			if(y == 0 && yhat == 1) fp = fp + 1
			if(y == 1 && yhat == 0) fn = fn + 1
			t = t + 1
		}	
		val a = (t - fp - fn) / t
		val p = if (tp + fp == 0) 0 else tp / (tp + fp)
		val r = if (tp + fn == 0) 0 else tp / (tp + fn)
		val f = if (p + r == 0) 0 else 2 * (p * r) / (p + r)
		println(p + "  " + r + " " + f + " " + a)
		return (p, r, f, a)	
	}

	
	def main(args: Array[String]) {

		var dm : DataMatrix = new DataMatrix(Vector.empty);

		if(args.size == 0){
			 println("Usage : run RegressionBreeze DATA_FILE POS_FILE [filter] [threshold]")
			System.exit(0)
		}

		val allFile = args(0)
		val posFile = args(1)
		
		val filter = if(args.size > 2) args(2).toBoolean else false 
		val threshold = if(args.size > 3) args(3).toInt else 10

		dm.initializeFromFile(posFile, allFile, filter, threshold)

		crossValidate(dm)

	}
}

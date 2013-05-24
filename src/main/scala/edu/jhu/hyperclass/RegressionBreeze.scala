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
import breeze.config.{GenerateHelp, Help, CommandLineParser}
import io.Source
import breeze.data.Example
import breeze.linalg.{VectorBuilder, SparseVector}
import breeze.util.Index
import breeze.classify.LogisticClassifier
import breeze.stats.ContingencyStats
import breeze.optimize.FirstOrderMinimizer.OptParams
import collection.mutable.ArrayBuffer
import collection.immutable.Vector

object RegressionBreeze {
			
	class PhrasePairExample(_id : String, _label : Int, _features : Vector[String]) extends Example[Int, SparseVector[Double]] {
		val builder = new VectorBuilder[Double](1)
	    	val featureIndex = Index[String]()
		
		for(f <- _features){
          		val fi = featureIndex.index(f)
          		builder.add(fi, 1.0)
		}
		
		val id = _id
		val label = _label
		val features = builder.toSparseVector
	}

	def toBreezeFormat(data : DataMatrix) : IndexedSeq[Example[Int, SparseVector[Double]]] = {

		println("Encoding Features")
		var breezeData: IndexedSeq[PhrasePairExample] = new ArrayBuffer()
		for(p <- data.pairs){
			breezeData = breezeData :+ new PhrasePairExample(p.toString, p.hypernym.toInt, p.rawFeatures)
		}
		return breezeData
		
	}

	def crossValidate(data : DataMatrix) = {

		val (train, test) = data.splitTrainTest(10)
		val opt = OptParams(maxIterations=60,useStochastic=false,useL1=true)
		println("Training")
      		val classifier = new LogisticClassifier.Trainer[Int, SparseVector[Double]](opt).train(toBreezeFormat(train))
		var stats = ContingencyStats(classifier, toBreezeFormat(train).toSeq)
      		println(stats)
		stats = ContingencyStats(classifier, toBreezeFormat(test).toSeq)
      		println(stats)
    	}

	def main(args: Array[String]) {

		var dm : DataMatrix = new DataMatrix(Vector.empty);

		val posFile = "output/xy4.txt"
		val allFile = "output/joined.small"

		dm.initializeFromFile(posFile, allFile)

		crossValidate(dm)

	}
}

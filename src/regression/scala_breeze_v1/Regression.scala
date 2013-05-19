import java.io.File
import io.Source
import breeze.data.Example
import breeze.util.Index
import breeze.linalg._
import breeze.classify.LogisticClassifier
import breeze.stats.ContingencyStats
import breeze.optimize.FirstOrderMinimizer.OptParams
import collection.mutable.ArrayBuffer

object Regression{

	def main(args: Array[String]) {
    
		val dm : DataMatrix = new DataMatrix()
		val dataFile : String = "/home/hltcoe/epavlick/hyperclass/repo/txt_sentoken/pos/cv991_18645.txt"
		//val dataFile : String = "/home/hltcoe/epavlick/hyperclass/repo/src/main/scala/edu/jhu/hyperclass/fake_input_file.txt"
		val extractedData = dm.readDataFromDir(dataFile)

		val folds = extractedData.groupBy(_.id.take(2)).mapValues(_.toSet)
		//val folds = extractedData.groupBy(_.id).mapValues(_.toSet)
		
		println(folds)
	
		val allStats = ArrayBuffer[ContingencyStats[Int]]()
		for( (nameOfFold, test) <- folds) {
			val train = extractedData.filterNot(test)
			val opt = OptParams(maxIterations=60,useStochastic=false,useL1=true)
			val classifier = new LogisticClassifier.Trainer[Int, SparseVector[Double]](opt).train(train)

			val stats = ContingencyStats(classifier, test.toSeq)
			allStats += stats
			println(stats)
		}
		for( (stats,i) <- allStats.zipWithIndex) {
			println("CV Fold " + i)
			println(stats)
		}
	}
}

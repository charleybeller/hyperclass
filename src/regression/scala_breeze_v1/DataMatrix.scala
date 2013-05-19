import java.io.File
import io.Source
import breeze.data.Example
import breeze.util.Index
import breeze.linalg._
import breeze.classify.LogisticClassifier
import breeze.stats.ContingencyStats
import breeze.optimize.FirstOrderMinimizer.OptParams
import collection.mutable.ArrayBuffer
import collection.mutable.ArraySeq
import scala.util.Random

class DataMatrix(){

	val r : Random = new Random()

	val langData = breeze.text.LanguagePack.English
	val stemmer = langData.stemmer.getOrElse(identity[String] _)
	val tokenizer = langData.simpleTokenizer
	
	trait Feature
	case class ParseFeature(w: String) extends Feature { 
		val name = w 
		val value = 1 
	}

	val featureIndex = Index[String]()

	def getLabel(phrasePair : String) : Int = if(r.nextBoolean) 0 else 1 //WordNet labeling here

	def getParseFeatures(w : String) : ArraySeq[ParseFeature] = {
		var xs = new ArraySeq[ParseFeature](1)
		xs :+ ParseFeature(w)
	}
		
    	def extractFeatures(ex: Example[Int, IndexedSeq[String]]) =  {
		ex.map { words =>
        		val builder = new VectorBuilder[Double](Int.MaxValue)
        		for(w <- words) {
				for(pf <- getParseFeatures(w)){
					println(w)
					println(pf.name)
          				builder.add(featureIndex.index(pf.name), pf.value)
				}
        		}
			builder
		}
	}
	
	def readDataFromDir(dirName: String) : IndexedSeq[Example[Int, SparseVector[Double]]] = {
		
		val trainFile = new File(dirName);

		val data: Array[Example[Int, IndexedSeq[String]]] = {
			for( line <- Source.fromFile(dirName).getLines().toArray ) yield {
				val text = tokenizer(line).toIndexedSeq
				Example(label=getLabel(line), features = text, id = line)
			}
		}
		val extractedData: IndexedSeq[Example[Int, SparseVector[Double]]] = data.map(extractFeatures).map{ex =>
      			ex.map{ builder =>
        			builder.length = featureIndex.size
        			builder.toSparseVector
      			}
    		}
		extractedData
	}
}

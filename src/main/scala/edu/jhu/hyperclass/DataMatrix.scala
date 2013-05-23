import scala.io.Source._;
import scala.collection.immutable.HashMap;
import scala.collection.immutable.Vector;

import java.util.Collection;
import java.util.Random;

import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.FeatureNode;

/**
 * Holds the list of word pairs do be used in classifier training/testing
 * */
class DataMatrix(phrasePairs : Vector[PhrasePair]){ 

	var pairs : Vector[PhrasePair] = phrasePairs

	/**
 	* Initialize DataMatrix by reading PhrasePairs from file
 	*/
	def initializeFromFile(posFileName: String, allFileName : String) = {
		println("Reading File")
		for(line <- fromFile(allFileName).getLines){
			val comps : Array[String] = line.split('\t')
			pairs = new PhrasePair(comps(2).split("=")(1), comps(3).split("=")(1)) +: pairs
		}
		println("Getting Labels")
		getLabelsFromFile(posFileName);
		println("Getting Features")
		extractFeaturesFromFile(allFileName);
	}

	/**
 	* Return two DataMatrix items, one for training one for testing
 	* @param testPnct - the percent of data to use for testing, and integer 0 to 100
 	*/

	def splitTrainTest(testPcnt : Int) : (DataMatrix, DataMatrix) = {
		
		var train : Vector[PhrasePair] = new Vector(0, 0, 0)
		var test : Vector[PhrasePair] = new Vector(0, 0, 0)
		var i : Int = 0;
		var r : Random = new Random(); 
		for(p <- pairs){
			i = r.nextInt(100);
			if(i <= testPcnt){ test = p +: test }
			else{ train = p +: train }
		}	
		(new DataMatrix(train), new DataMatrix(test));
	}
	
	/**
 	* Use wordnet to determine if the pair is related by hypernym/hyponym relationship
 	*/
	def getLabelsFromFile(fileName : String) = {
		
		var r : Random = new Random(); 
		var labels : HashMap[PhrasePair, Int] = new HashMap[PhrasePair, Int]();
		for(line <- fromFile(fileName).getLines){
			val comps : Array[String] = line.split('\t')
			val p : PhrasePair = new PhrasePair(comps(2).split("=")(1), comps(3).split("=")(1))
			labels = labels + ((p, 1))
		}
		for(p <- pairs ){ p.setHypernym(if(labels contains p) 1 else 0) }
	}

	/**
 	* Get the coordinate features for a pair of words
 	*/
	def coordinateFeatures(p : PhrasePair) : Vector[Int] = {
	
		var features = Vector.empty
		0 +: features
	}

	/**
 	* Get the parse features for a pair of words
 	*/
	def parseFeatures(p : PhrasePair) : Vector[Int] = {
		var features = Vector.empty
		0 +: features
	}

	/**
 	* Get the parse features each PhrasePair 
 	*/
	def parseFeaturesFromFile(fileName : String) : Vector[Vector[Feature]] = {
		
		//first pass to get all string feature types to initialize FeatureEncoder

		println("Building feature matrix")
		var features : Vector[String] = new Vector(0, 0, 0)
		for(line <- fromFile(fileName).getLines){
			features = line.split('\t')(0) +: features
		}
	
		//second pass, to save all features into feature array 
	
		var fm : FeatureMatrix[PhrasePair, String] = new FeatureMatrix[PhrasePair, String](features);
	
		println("Encoding features")
		for(line <- fromFile(fileName).getLines){
			val comps : Array[String] = line.split('\t')
			val p : PhrasePair = new PhrasePair(comps(2).split("=")(1), comps(3).split("=")(1))
			val parse : String = comps(0)
			fm.store(p, parse) 
		}
	
		return fm.toArray(pairs)
	}

	/**
 	* Extract features for each PhrasePair is pairs
 	*/
	def extractFeaturesFromFile(fileName : String) = {
		
		var idx : Int = 1;
		var wordFeatures = Vector.empty 
		var fs = parseFeaturesFromFile(fileName)
		for((p,f) <- pairs.zip(fs)){
			p.addFeatures(f)
		}
	} 

	/**
 	* Get the feature matrix
 	*/
	def getX() : Array[Array[Feature]] = {
		
		var X : Vector[Array[Feature]] = new Vector(0, 0, 0)

		for(p <- pairs){
			X = p.features.toArray +: X
		}
	
		return X.toArray

	}

	/**
 	* Get the label matrix
 	*/
	def getY() : Array[Double] = {
		
		var Y : Vector[Double] = new Vector(0, 0, 0)
		for(p <- pairs){
			Y = p.hypernym +: Y
		}
		return Y.toArray
	}

	/**
 	* Get phrase pair at position i 
 	*/
	def get(i : Int) : PhrasePair  = pairs(i);	
}

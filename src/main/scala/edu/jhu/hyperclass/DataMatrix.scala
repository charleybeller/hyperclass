import scala.io.Source._;
import scala.collection.immutable.HashMap;

import java.util.Collection;
import java.util.Random;

import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.FeatureNode;

/**
 * Holds the list of word pairs do be used in classifier training/testing
 * */
class DataMatrix(phrasePairs : Array[PhrasePair]){ 

	var pairs : Array[PhrasePair] = phrasePairs

	/**
 	* Initialize DataMatrix by reading PhrasePairs from file
 	*/
	def initializeFromFile(fileName: String) = {
		pairs = new Array(0);
		for(line <- fromFile(fileName).getLines){
			val comps : Array[String] = line.split('\t')
			pairs = pairs :+ new PhrasePair(comps(2).split("=")(1), comps(3).split("=")(1))
		}
		getLabelsFromFile(fileName);
		extractFeaturesFromFile(fileName);
	}

	/**
 	* Return two DataMatrix items, one for training one for testing
 	* @param testPnct - the percent of data to use for testing, and integer 0 to 100
 	*/

	def splitTrainTest(testPcnt : Int) : (DataMatrix, DataMatrix) = {
		
		var train : Array[PhrasePair] = new Array(0);
		var test : Array[PhrasePair] = new Array(0);
		var i : Int = 0;
		var r : Random = new Random(); 
		for(p <- pairs){
			i = r.nextInt(100);
			if(i <= testPcnt){ test = test :+ p; }
			else{ train = train :+ p; }
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
	def coordinateFeatures(p : PhrasePair) : Array[Int] = {
	
		var features : Array[Int] = new Array[Int](0);
		features :+ 0;
	}

	/**
 	* Get the parse features for a pair of words
 	*/
	def parseFeatures(p : PhrasePair) : Array[Int] = {
		var features : Array[Int] = new Array[Int](0);
		features :+ 0;
	}

	/**
 	* Get the parse features each PhrasePair 
 	*/
	def parseFeaturesFromFile(fileName : String) : Array[Array[Feature]] = {
		
		//first pass to get all string feature types to initialize FeatureEncoder

		var features : Array[String] = new Array[String](0);
		for(line <- fromFile(fileName).getLines){
			features = features :+ line.split('\t')(0)
		}
		
		//second pass, to save all features into feature array 
	
		var fm : FeatureMatrix[PhrasePair, String] = new FeatureMatrix[PhrasePair, String](features);
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
		
		for(p <- pairs ){
			var idx : Int = 1;
			var wordFeatures : Array[Feature] = new Array[Feature](0);
			var fs = parseFeaturesFromFile(fileName)
			for((p,f) <- pairs.zip(fs)){
				p.features = f
			}
		}
	} 

	/**
 	* Get the feature matrix
 	*/
	def getX() : Array[Array[Feature]] = {
		
		var X : Array[Array[Feature]] = new Array[Array[Feature]](0);
		for(p <- pairs){
			X = X :+ p.features
		}
		return X 

	}

	/**
 	* Get the label matrix
 	*/
	def getY() : Array[Double] = {
		
		var Y : Array[Double] = new Array(0);
		for(p <- pairs){
			Y = Y :+ p.hypernym; 
		}
		return Y
	}

	/**
 	* Get phrase pair at position i 
 	*/
	def get(i : Int) : PhrasePair  = pairs(i);	
}

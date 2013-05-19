import scala.collection.mutable.ArraySeq
import scala.io.Source._;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.FeatureNode;

/**
 * Holds the list of word pairs do be used in classifier training/testing
 * */
class DataMatrix(phrasePairs : ArraySeq[PhrasePair]){ 

	var pairs : ArraySeq[PhrasePair] = phrasePairs

	/**
 	* Initialize DataMatrix by reading PhrasePairs from file
 	*/
	def initializeFromFile(fileName: String) = {
		pairs = new ArraySeq(0);
		for(line <- fromFile(fileName).getLines){
			println(line) 
		}

//	     	String line;
//		String[] words;
//  		try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)){
//  			while ((line = reader.readLine()) != null) {
//				words = line.split("\t"); 
//				this.pairs.add(new PhrasePair(words[0], words[1]));
//			}
//		}
//		getLabels();
//		extractFeatures();
	}

	/**
 	* Return two DataMatrix items, one for training one for testing
 	* @param testPnct - the percent of data to use for testing, and integer 0 to 100
 	*/

	def splitTrainTest(testPcnt : Int) : (DataMatrix, DataMatrix) = {
		
		var train : ArraySeq[PhrasePair] = new ArraySeq(0);
		var test : ArraySeq[PhrasePair] = new ArraySeq(0);
		var i : Int = 0;
		var r : Random = new Random(); 
		for(p <- pairs){
			i = r.nextInt(100);
			if(i <= testPcnt){ test :+ p; }
			else{ train :+ p; }
		}	
		(new DataMatrix(train), new DataMatrix(test));
	}
	
	/**
 	* Use wordnet to determine if the pair is related by hypernym/hyponym relationship
 	*/
	def getLabels() = {
		
		for(p <- pairs ){ p.hypernym = 1.0; }
	}

	/**
 	* Get the coordinate features for a pair of words
 	*/
	def coordinateFeatures(p : PhrasePair) : ArraySeq[Int] = {
	
		var features : ArraySeq[Int] = new ArraySeq[Int](0);
		features :+ 0;
	}

	/**
 	* Get the parse features for a pair of words
 	*/
	def parseFeatures(p : PhrasePair) : ArraySeq[Int] = {
	
		var features : ArraySeq[Int] = new ArraySeq[Int](0);
		features :+ 0;
	}

	/**
 	* Extract features for each PhrasePair is pairs
 	*/
	def extractFeatures() = {

		for(p <- pairs ){
			var idx : Int = 1;
			var wordFeatures : ArraySeq[FeatureNode] = new ArraySeq[FeatureNode](0);
			for(f <- parseFeatures(p)){
				wordFeatures :+ (new FeatureNode(idx, f));
				idx = idx + 1;
			}
			for(f <- coordinateFeatures(p)){
				wordFeatures :+ (new FeatureNode(idx, f));
				idx = idx + 1;
			}
			p.features = wordFeatures; 
		}
	} 

	/**
 	* Get the feature matrix
 	*/
	def getX() : ArraySeq[Array[FeatureNode]] = {
		
		var X : ArraySeq[Array[FeatureNode]] = new ArraySeq[Array[FeatureNode]](0);
		return X;
//		for(var p <- pairs ){
//			copyToArray( X :+ (p.features.toArray(new FeatureNode[ p.features.size() ]));
//		}
//		return X.toArray(new FeatureNode[ X.size() ][ X.get(0).length ]);

	}

	/**
 	* Get the label matrix
 	*/
	def getY() : ArraySeq[Double] = {
		
		var Y : ArraySeq[Double] = new ArraySeq(pairs.length);
		return Y;
//		for(p <- pairs){
//			Y :+ p.hypernym; 
//		}
	}

	/**
 	* Get phrase pair at position i 
 	*/
	def get(i : Int) : PhrasePair  = pairs(i);	
}

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.FeatureNode;

/**
 * Holds the list of word pairs do be used in classifier training/testing
 * */
public class DataMatrix{

	public ArrayList<PhrasePair> pairs;

	/**
 	* Initialize DataMatrix from a collection of PhrasePairs
 	*/
	public DataMatrix(Collection<PhrasePair> pairs, Boolean extractFeatures){
		this.pairs = new ArrayList<PhrasePair>(pairs);
		if(extractFeatures){
			getLabels();
			extractFeatures();
		}
	}
	
	/**
 	* Initialize DataMatrix by reading PhrasePairs from file
 	*/
	public DataMatrix(String fileName) throws IOException{
		this.pairs = new ArrayList<PhrasePair>();
    		Path path = Paths.get(fileName);
      		String line;
		String[] words;
    		try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)){
      			while ((line = reader.readLine()) != null) {
				words = line.split("\t"); 
				this.pairs.add(new PhrasePair(words[0], words[1]));
			}
		}
		getLabels();
		extractFeatures();
	}

	/**
 	* Return two DataMatrix items, one for training one for testing
 	* @param testPnct - the percent of data to use for testing, and integer 0 to 100
 	*/

	public DataMatrix[] splitTrainTest(int testPnct){
		
		ArrayList<PhrasePair> train = new ArrayList<PhrasePair>();
		ArrayList<PhrasePair> test = new ArrayList<PhrasePair>();
		Random r = new Random(); int i = 0;
		for(PhrasePair p : this.pairs){
			i = r.nextInt(100);
			if(i <= testPnct){ test.add(p); }
			else{ train.add(p); }
		}	
		DataMatrix[] ret = { new DataMatrix(train, false), new DataMatrix(test, false) };
		return ret;
	}
	
	/**
 	* Use wordnet to determine if the pair is related by hypernym/hyponym relationship
 	*/
	public void getLabels(){
		
		for(PhrasePair p : this.pairs ){
			p.setHypernym(1.0); 
		}
	}

	/**
 	* Get the coordinate features for a pair of words
 	*/
	public ArrayList<Integer> coordinateFeatures(PhrasePair p){
	
		ArrayList<Integer> features = new ArrayList<Integer>();
		features.add(0);
		return features;

	}

	/**
 	* Get the parse features for a pair of words
 	*/
	public ArrayList<Integer> parseFeatures(PhrasePair p){
	
		ArrayList<Integer> features = new ArrayList<Integer>();
		features.add(0);
		return features;

	}

	/**
 	* Extract features for each PhrasePair is pairs
 	*/
	public void extractFeatures(){

		for(PhrasePair p : this.pairs ){
			int idx = 1;
			ArrayList<FeatureNode> wordFeatures = new ArrayList<FeatureNode>();
			for(Integer f : parseFeatures(p)){
				wordFeatures.add(new FeatureNode(idx++, f));
			}
			for(Integer f : coordinateFeatures(p)){
				wordFeatures.add(new FeatureNode(idx++, f));
			}
			p.setFeatures(wordFeatures); 
		}
	} 

	/**
 	* Get the feature matrix
 	*/
	public FeatureNode[][] getX(){
		
		ArrayList<FeatureNode[]> X = new ArrayList<FeatureNode[]>();
		for(PhrasePair p : this.pairs ){
			X.add(p.features.toArray(new FeatureNode[ p.features.size() ]));
		}
		return X.toArray(new FeatureNode[ X.size() ][ X.get(0).length ]);

	}

	/**
 	* Get the label matrix
 	*/
	public double[] getY(){
		
		double[] Y = new double[ this.pairs.size() ];
		for(int i = 0; i < this.pairs.size(); i++){ 
			Y[i] = this.pairs.get(i).hypernym; 
		}
		return Y;

	}

	/**
 	* Get phrase pair at position i 
 	*/
	public PhrasePair get(int i){
	
		return this.pairs.get(i);	

	}
}

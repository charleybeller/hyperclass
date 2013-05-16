import java.util.ArrayList;
import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.FeatureNode;

/**
 * Extract Wordnet and Coordinate Term Features for classifier.
 * */
public class FeatureExtractor{

	public ArrayList<FeatureNode[]> matrix;

	public FeatureExtractor(){
		this.matrix = new ArrayList<FeatureNode[]>();
	}

	public ArrayList<Integer> coordinateFeatures(String word){
	
		ArrayList<Integer> features = new ArrayList<Integer>();
		features.add(0);
		return features;

	}

	/**
 	* Use wordnet to determine if the pair is related by hypernym/hyponym relationship
 	*/
	public ArrayList<Integer> wordnetLabel(String word){
	
		ArrayList<Integer> features = new ArrayList<Integer>();
		features.add(0);
		return features;

	}

	/**
 	* Get the parse features for a pair of words
 	*/
	public ArrayList<Integer> parseFeatures(String word){
	
		ArrayList<Integer> features = new ArrayList<Integer>();
		features.add(0);
		return features;

	}

	public FeatureNode[][] extractFeatures(String[] words){

		for(String word : words){
			int idx = 0;
			ArrayList<FeatureNode> wordFeatures = new ArrayList<FeatureNode>();
			for(Integer f : parseFeatures(word)){
				wordFeatures.add(new FeatureNode(idx++, f));
			}
			for(Integer f : coordinateFeatures(word)){
				wordFeatures.add(new FeatureNode(idx++, f));
			}
			this.matrix.add(wordFeatures.toArray(new FeatureNode[ wordFeatures.size() ]));
		}
		return this.matrix.toArray(new FeatureNode[ this.matrix.size() ][ this.matrix.get(0).length ]);
	} 

	public static void main(String[] args){

		FeatureExtractor f = new FeatureExtractor();
		FeatureNode[][] m = f.extractFeatures(new String[] {"hi", "bye", "yo"});
		for(FeatureNode[] l : m){
			for(FeatureNode ll : l){
				System.out.print(ll + " ");
			}
			System.out.println();
		}
		
	}
}

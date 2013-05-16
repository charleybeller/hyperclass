import de.bwaldvogel.liblinear.FeatureNode;

/**
 * Pair of words/phrases to be classified
 * */
public class PhrasePair{

	public String A;
	public String B;
	public Boolean hypernym;
	public ArrayList<FeatureNode> features;

	public PhrasePair(A, B){

		this.A = A;
		this.B = B;
		this.hypernym = false;
		this.features = new ArrayList<FeatureNode>();

	}

	public void setHypernym(Boolean isHypernym){

		this.hypernym = isHypernym;

	}
		
}

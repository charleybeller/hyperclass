import java.util.ArrayList;
import java.util.Collection;
import de.bwaldvogel.liblinear.FeatureNode;

/**
 * Pair of words/phrases to be classified
 * */
public class PhrasePair{

	public String A;
	public String B;
	public double hypernym;
	public ArrayList<FeatureNode> features;

	public PhrasePair(String A, String B){

		this.A = A;
		this.B = B;
		this.hypernym = 0;
		this.features = new ArrayList<FeatureNode>();

	}

	public void setHypernym(double isHypernym){

		this.hypernym = isHypernym;

	}
		
	public void setFeatures(Collection<FeatureNode> features){

		this.features = new ArrayList(features);

	}
		
}

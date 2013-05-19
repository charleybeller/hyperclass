import scala.collection.mutable.ArraySeq
import java.util.ArrayList;
import java.util.Collection;

/**
 * Pair of words/phrases to be classified
 * */
class PhrasePair(AA : String, BB : String){

	var A : String = AA;
	var B : String = BB;
	var hypernym : Double = 0;
	var features : ArraySeq[Int] = new ArraySeq(0);

	override def toString() = "(" + this.A + ", " + this.B + ")";
	
	def setHypernym(isHypernym : Double){

		hypernym = isHypernym;

	}
}

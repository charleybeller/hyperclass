import java.util.ArrayList;
import scala.collection.immutable.Vector
import java.util.Collection;
import de.bwaldvogel.liblinear.Feature;

/**
 * Pair of words/phrases to be classified
 * */
class PhrasePair(AA : String, BB : String){

	var A : String = AA;
	var B : String = BB;
	var hypernym : Double = 0;
	var features : Vector[Feature] = new Vector(0, 0, 0)
	var rawFeatures : Vector[String] = new Vector(0, 0, 0)

	override def toString() = "(" + this.A + ", " + this.B + ")";

	override def equals(obj:Any) = obj.isInstanceOf[PhrasePair] && 
		obj.asInstanceOf[PhrasePair].A == this.A && 
		obj.asInstanceOf[PhrasePair].B == this.B 
	
	override def hashCode() = this.toString().hashCode()
	
	def setHypernym(isHypernym : Double) = { hypernym = isHypernym }
	
	def getFeatures = features
	
	def addFeature(f: Feature) = { features = f +: features }
	
	def addRawFeature(f: String) = { rawFeatures = f +: rawFeatures }
	
	def addFeatures(f: Vector[Feature]) = { features = f ++: features }
}

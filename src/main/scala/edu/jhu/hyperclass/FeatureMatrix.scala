import breeze.linalg.SparseVector
import scala.collection.immutable.HashMap
import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.FeatureNode;


class FeatureMatrix[K, V](features : Array[V]) {


	class FeatureEncoder[V](features : Array[V]) {
	
		var hm : HashMap[V, Int] = new HashMap()
		var size: Int = 0
		
		for(f <- features){
			if(!hm.contains(f)){
				hm = hm + ((f, size))
				size = size + 1
			}
		}

		def index(elem : V) : Int = hm.getOrElse(elem, -1)

	}

	var fmap : HashMap[K, Array[Int]] = new HashMap()
	var fe : FeatureEncoder[V] = new FeatureEncoder(features)
	
	def store(key : K, value : V) = {
	
		if(!fmap.contains(key)){
			fmap = fmap.+((key, new Array[Int](fe.size)))
		}	
		
		var v = fmap.getOrElse(key, new Array[Int](0)) 
		v(fe.index(value)) = 1
		fmap = fmap.updated(key, v)

	}

	def toFeatureArray(intArray : Array[Int]) : Array[Feature] = {

		var idx : Int = 1
		var fArray : Array[Feature] = new Array(0)

		for(i <- intArray){
			fArray = fArray :+ new FeatureNode(idx, i)
                	idx = idx + 1;
		}
		return fArray

	}

	def toArray(keyOrder : Array[K]) : Array[Array[Feature]] = {

		var array : Array[Array[Feature]] = new Array[Array[Feature]](0)

		for(k <- keyOrder){
			array = array :+ toFeatureArray(fmap.getOrElse(k, new Array[Int](0)))

		}
		return array
	}

}

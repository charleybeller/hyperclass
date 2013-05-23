import breeze.linalg.SparseVector
import scala.collection.immutable.Vector
import scala.collection.immutable.List
import scala.collection.immutable.HashMap
import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.FeatureNode;


class FeatureMatrix[K, V](features : Vector[V]) {


	class FeatureEncoder[V](features : Vector[V]) {
	
		var hm : HashMap[V, Int] = new HashMap()
		var size: Int = 0
	
		for(f <- features){
			if(!hm.contains(f)){
				hm = hm.updated(f, size)
				size = size + 1
			}
		}

		println(hm.size)

		def index(elem : V) : Int = {
			hm.get(elem).get
		}

	}

	var fmap : HashMap[K, Vector[Int]] = new HashMap()
	var fe : FeatureEncoder[V] = new FeatureEncoder(features)
	
	def store(key : K, value : V) = {
	
		if(!fmap.contains(key)){
			var vec : Vector[Int] = new Vector(0, 0, 0)
			var i : Int = 0
			while(i < fe.size){
				vec = 0 +: vec
				i = i + 1
			}
			fmap = fmap.updated(key, vec)
		}	
	
		var v : Vector[Int] = fmap.get(key).get
		v = v.updated(fe.index(value), 1)
		fmap = fmap.updated(key, v)
	}

	def toFeatureArray(intListBuffer : Vector[Int]) : Vector[Feature] = {

		var idx : Int = 1
		var fVector : Vector[Feature] = new Vector(0, 0, 0)

		for(i <- intListBuffer){
			fVector = fVector :+ new FeatureNode(idx, i)
                	idx = idx + 1;
		}
		return fVector

	}

	def toArray(keyOrder : Vector[K]) : Vector[Vector[Feature]] = {

		var array: Vector[Vector[Feature]] = new Vector(0, 0, 0)

		for(k <- keyOrder){
			array = toFeatureArray(fmap.get(k).get) +: array
		}

		return array
	}

}

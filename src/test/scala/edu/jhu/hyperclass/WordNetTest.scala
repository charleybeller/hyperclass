
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

import edu.jhu.hyperclass.WordNet

class WordNetTest extends FlatSpec with ShouldMatchers {
  
  "A WordNet" should "be instantiable" in {
    val wn = new WordNet
  }

  it should "be loadable" in {
    lazy val dict = WordNet.dict
  }

  it should "provide the synset of a word" in {
    val word = "dog"
    WordNet.dict
    val synsets = WordNet.getSynsets(word)
    val synsetString = "SYNSET{SID-02084071-N : Words[W-02084071-N-1-dog, W-02084071-N-2-domestic_dog, W-02084071-N-3-Canis_familiaris]}"
    synsets(0).toString should be (synsetString)
  }
}



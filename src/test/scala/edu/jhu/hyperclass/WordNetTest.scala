import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

import edu.jhu.hyperclass.WordNet

class WordNetTest extends FlatSpec with ShouldMatchers {
  
  "WordNet" should "provide the synset of a word" in {
    val word = "dog"
    val synsets = WordNet.getSynsets(word)
    val synsetString = "SYNSET{SID-02084071-N : Words[W-02084071-N-1-dog, W-02084071-N-2-domestic_dog, W-02084071-N-3-Canis_familiaris]}"
    synsets(0).toString should be (synsetString)
  }

  it should "provide the hypernyms of a word" in {
    val word = "dog"
    val hypernyms = WordNet.getHypernyms(word)
    val set = Set("support", "scoundrel", "cuss", "disagreeable_woman", "villain", "domesticated_animal", "sausage", "gent", "stop", "canid", "blighter", "unpleasant_woman", "canine", "fellow", "feller", "domestic_animal", "lad", "bloke", "catch", "chap", "fella")
    hypernyms should be (set)
  }

  it should "tell you if X is a Y" in {
    val word = "dog"
    val word2 = "gent"
    WordNet.isa("dog", "gent") should be (true)
    WordNet.isa("gent", "dog") should be (false)
  }
}



import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

import java.io.File
import edu.mit.jwi.IDictionary
import edu.mit.jwi.Dictionary
import edu.mit.jwi.IRAMDictionary
import edu.mit.jwi.RAMDictionary
import edu.mit.jwi.item.POS
import edu.mit.jwi.item.IWordID
import edu.mit.jwi.data.ILoadPolicy

class WordNetTest extends FlatSpec with ShouldMatchers {
  
  "A dictionary" should "be openable" in {
    val wnDir  = new File("data/wordnet/dict")
    val dict = new Dictionary(wnDir)
    dict.open()

    val idxWord = dict.getIndexWord("dog", POS.NOUN)
    val wordID = idxWord.getWordIDs().get(0)
    val word = dict.getWord(wordID)
    wordID.toString should be ("WID-02084071-N-??-dog")
    word.getLemma should be ("dog")
    val synset = "SYNSET{SID-02084071-N : Words[W-02084071-N-1-dog, W-02084071-N-2-domestic_dog, W-02084071-N-3-Canis_familiaris]}"
    word.getSynset.toString should be (synset)
    val gloss ="a member of the genus Canis (probably descended from the common wolf) that has been domesticated by man since prehistoric times; occurs in many breeds; \"the dog barked all night\""
    word.getSynset.getGloss should be (gloss)
  }

  "An IRAMDictionary" should "get synonyms " in {
    val wnDir = new File("data/wordnet/dict")
    val dict = new RAMDictionary(wnDir, ILoadPolicy.NO_LOAD)
    dict.open()
    dict.load(true)
  }


}

package edu.jhu.hyperclass;

import edu.mit.jwi._  //(Dictionary, IDictionary, RAMDictionary, IRAMDictionary)
import edu.mit.jwi.data._
import edu.mit.jwi.item._  //(POS)

import java.io.File
import java.util.logging.Logger

import scala.collection.JavaConverters.collectionAsScalaIterableConverter

class WordNet
object WordNet {
  val logger = Logger.getLogger(this.getClass.getName())
  val wordnetPath = "data/wordnet/dict"

  lazy val dict = {
    try {
      val wnDir = new File(wordnetPath)
      logger.info("loading wordnet data from " + wnDir.getPath())
      val dict = new RAMDictionary(wnDir, ILoadPolicy.IMMEDIATE_LOAD)
      dict.open()
      dict
    }
    catch { case e:Throwable => throw new RuntimeException(e) }
  }

  def getSynsets(word: String): Array[ISynset] = {
    val idxWords = POS.values().map{pos => Option(dict.getIndexWord(word,pos)) }.flatten
    val wordIDs = idxWords.map{ iw => iw.getWordIDs.asScala }.flatten
    val synsets = wordIDs.map{ wid => dict.getWord(wid).getSynset }
    synsets
  }

}

/*
	private HashSet<String> getSetHelper(String input, Pointer pointer) {
		HashSet<String> set = new HashSet<String>();
		List<ISynset> synsets = getSynsetList(input);
		for (ISynset synset : synsets) {
			List<ISynsetID> synsetIds = synset.getRelatedSynsets(Pointer.HYPERNYM);	
			for (ISynsetID sid : synsetIds) {
				for (IWord w : dict.getSynset(sid).getWords()){
					set.add(formatString(w.getLemma()));
				}
			}
		}
		return set;
	}
  */

/*
	private List<ISynset> getSynsetList(String input){
		List<ISynset> synsets = new ArrayList<ISynset>();
		IIndexWord idxWord;
		List<IWordID> wordIDs;
		for (POS pos : POS.values()){
			idxWord = dict.getIndexWord(input, pos);
			if (idxWord != null) {
				wordIDs = idxWord.getWordIDs();
				IWord word;
				for (IWordID wordID : wordIDs){
					word = dict.getWord(wordID);
					synsets.add(word.getSynset());
				}
			}
		}
		return synsets;
	}

	private String formatString(String input) {
		return input.replace('_',' ');
	}

	private class StringIntPair{
		public String s;
		public int i;
		public StringIntPair(String s, int i){
			this.s = s;
			this.i = i;
		}
	}
}
*/

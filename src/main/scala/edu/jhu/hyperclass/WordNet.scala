package edu.jhu.hyperclass;

import edu.mit.jwi._  //RAMDictionary
import edu.mit.jwi.data.ILoadPolicy
import edu.mit.jwi.item._  //POS,Pointer
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

  def getSynsets(word: String): List[ISynset] = {
    val idxWords = List(Option(dict.getIndexWord(word,POS.NOUN))).flatten
    val wordIDs = idxWords.map{ iw => iw.getWordIDs.asScala }.flatten
    val synsets = wordIDs.map{ wid => dict.getWord(wid).getSynset }
    synsets
  }
  
  def getHypernyms(word: String): Set[String] = {
    val synsets = getSynsets(word)
    synsets match {
      case Nil => Set()
      case x::xs => {
        val synsetID = x.getRelatedSynsets(Pointer.HYPERNYM).asScala
        val words = synsetID.map{ sid => dict.getSynset(sid).getWords().asScala}.flatten
        words.map{ w => w.getLemma }.toSet
      }
    }
  }

  def getAllSenseHypernyms(word: String): Set[String] = {
    val synset = getSynsets(word)
    val synsetID = synset.map{ s => s.getRelatedSynsets(Pointer.HYPERNYM).asScala}.flatten
    val words = synsetID.map{ sid => dict.getSynset(sid).getWords().asScala}.flatten
    words.map{ w => w.getLemma }.toSet
  }

  def isa(x: String, y:String): Boolean = {
    getHypernyms(x)(y)
  }

  def isnota(x: String, y:String): Boolean = {
    !getAllSenseHypernyms(x)(y)
  }
}

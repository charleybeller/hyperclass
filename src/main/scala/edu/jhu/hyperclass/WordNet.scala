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
  val countsPath = "data/wordnet/dict/cntlist.rev"

  /**
   * Code to deal with cntlist.rev
   *   Positive classification only if there is a single 
   *    frequently used sense of the word (the first sense)
   */

  type LexInfoSet = Set[(Int, Int, Int)]
  type CountMap = Map[String, LexInfoSet]

  case class CountlistEntry(lemma: String, ssType: Int, senseNumber: Int, count: Int) 

  def readCountline(countLine: String) = {
    val (senseKey, num, count) = countLine.split(" ") match {
      case Array(s, n, c) => (s, n.toInt, c.toInt)
    }
    val (word, lexSense) = senseKey.split("%") match {
      case Array(w, l) => (w, l)
    }
    val ssType = lexSense.split(":").head.toInt
    new CountlistEntry(word, ssType, num, count)
  }

  def buildCountMap(file: Iterator[String]): CountMap = {
    def loop(file: Iterator[String], cmap: CountMap): CountMap = {
      if (! file.hasNext) cmap
      else {
        val entry = readCountline(file.next)
        val word = entry.lemma
        val info = (entry.ssType, entry.senseNumber, entry.count)
        val cmap1 = cmap.get(word) match {
          case Some(x:LexInfoSet) => cmap + (word -> (x + info))
          case None => cmap + (word -> Set(info))
        }
        loop(file, cmap1)
      }
    }
    loop(file, Map.empty)
  }

  lazy val countmap = {
    try {
      val countfile = scala.io.Source.fromFile(countsPath).getLines
      buildCountMap(countfile)
    }
    catch { case e:Throwable => throw new RuntimeException(e) }
  }

  def singleFrequents(entry:(String, LexInfoSet))(pos: Int):Boolean = {
    val (w, i) = entry
    val (ssType: Int, senseNumber: Int, count: Int) = i.head
    i.size == 1 && ssType == pos && senseNumber == 1
  }

  lazy val singleNouns: Set[String] = countmap.collect{case s if singleFrequents(s)(1) => s._1}.toSet

  def singleSenseNoun(word:String) ={
    singleNouns(word)
  }


  /**
   * Read in WordNet
   */
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
    def loop(synset:ISynset, hypernyms: Set[String]): Set[String] = {
      val next = synset.getRelatedSynsets(Pointer.HYPERNYM).asScala 
      if (next.isEmpty) hypernyms
      else {
        val hyps = dict.getSynset(next.head)
        val words = hyps.getWords().asScala
        val hypernyms1 = hypernyms ++ words.map{w => w.getLemma}.toSet
        loop(hyps, hypernyms1)
      }
    }
    synsets match {
      case Nil => Set()
      case x::xs => loop(x, Set.empty)
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

  def wordNetRelations(x:String, y:String): String = {
    val string = if (singleSenseNoun(x) && singleSenseNoun(y)) {
      lazy val hypernym = tagHypernym(x,y)
      lazy val hyponym = tagHyponym(x,y)
      lazy val synonym = tagSynonym(x,y)
      lazy val antonym = tagAntonym(x,y)
      lazy val alternation = tagAlternation(x,y)
      Array(hypernym, hyponym, synonym, antonym, alternation).mkString("\t")
    }
    else {
      Array("unknown", "unknown", "unknown", "unknown", "unknown").mkString("\t")
    }
    string
  }

  def tagHypernym(x:String, y:String): String = {
    if (isa(x, y)) "hyper"
    else if (isnota(x, y)) "nonhyper"
    else "unknown"
  }

  def tagHyponym(x:String, y:String): String = {
    if (isa(y, x)) "hypo"
    else if (isnota(y, x)) "nonhypo"
    else "unknown"
  }


  def tagSynonym(x:String, y:String): String = "TODO"
  def tagAntonym(x:String, y:String): String = "TODO"
  def tagAlternation(x:String, y:String): String = "TODO"

    
}

package edu.jhu.hyperclass;

import edu.mit.jwi._  //RAMDictionary
import edu.mit.jwi.data.ILoadPolicy
import edu.mit.jwi.item._  //POS,Pointer
import java.io.File
import java.util.logging.Logger
import scala.collection.JavaConverters.collectionAsScalaIterableConverter



class WordNet(wordNetPath: String) {
  val logger = Logger.getLogger(this.getClass.getName())
  val nounPath = wordNetPath + "/index.noun"
  val countsPath = "data/wordnet/dict/cntlist.rev"

  /**
   * Code to deal with cntlist.rev
   *   Positive classification only if there is a single 
   *    frequently used sense of the word (the first sense)
   */
  type LexInfoSet = Set[(Int, Int, Int, String)]
  type CountMap = Map[String, LexInfoSet]

  case class CountlistEntry(lemma: String, ssType: Int, senseNumber: Int, count: Int, senseKey: String) 

  def readCountline(countLine: String) = {
    val (senseKey, num, count) = countLine.split(" ") match {
      case Array(s, n, c) => (s, n.toInt, c.toInt)
    }
    val (word, lexSense) = senseKey.split("%") match {
      case Array(w, l) => (w, l)
    }
    val ssType = lexSense.split(":").head.toInt
    new CountlistEntry(word, ssType, num, count, senseKey)
  }

  def buildCountMap(file: Iterator[String]): CountMap = {
    def loop(file: Iterator[String], cmap: CountMap): CountMap = {
      if (! file.hasNext) cmap
      else {
        val entry = readCountline(file.next)
        val word = entry.lemma
        val info = (entry.ssType, entry.senseNumber, entry.count, entry.senseKey)
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
    val (ssType: Int, senseNumber: Int, count: Int, senseKey: String) = i.head
    i.size == 1 && ssType == pos && senseNumber == 1
  }

  lazy val singleNouns: Set[String] = countmap.collect{case s if singleFrequents(s)(1) => s._1}.toSet

  def singleSenseNoun(word:String) ={
    singleNouns(word)
  }

  /**
   * All nouns in WordNet
   */
  def buildNounSet(file: Iterator[String]): Set[String] = {
    def loop(file: Iterator[String], nset: Set[String]): Set[String] = {
      if (! file.hasNext) nset
      else {
        val noun = file.next.split(" ")(0)
        val nset1 = if (noun.isEmpty) nset else nset + noun
        loop(file, nset1)
      }
    }
    loop(file, Set.empty)
  }
  lazy val inVocab = {
    try {
      val nounfile = scala.io.Source.fromFile(nounPath).getLines
      buildNounSet(nounfile)
    }
    catch { case e:Throwable => throw new RuntimeException(e) }
  }


  /**
   * Read in WordNet
   */
  lazy val dict = {
    try {
      val wnDir = new File(wordNetPath)
      logger.info("loading WordNet data from " + wnDir.getPath())
      val dict = new RAMDictionary(wnDir, ILoadPolicy.IMMEDIATE_LOAD)
      dict.open()
      dict
    }
    catch { case e:Throwable => throw new RuntimeException(e) }
  }

  def getWords(word: String): List[IWord] = {
    val idxWords = List(Option(dict.getIndexWord(word,POS.NOUN))).flatten
    val wordIDs = idxWords.map{ iw => iw.getWordIDs.asScala }.flatten
    wordIDs.map{ wid => dict.getWord(wid) }
  }

  
  def getSynsets(word: String): List[ISynset] = {
    getWords(word).map { w => w.getSynset }
  }

  /**
   * Synonym methods
   */
  def collectSynonyms(synset: ISynset): Set[String] = {
    val words = synset.getWords().asScala
    words.map{w => w.getLemma}.toSet
  }

  def getFirstSenseSynonyms(word: String): Set[String] = {
    getSynsets(word) match {
      case Nil => Set()
      case x::xs => collectSynonyms(x)
    }
  }

  def getAllSenseSynonyms(word: String): Set[String] = {
    def loop(synsets: List[ISynset], synonyms: Set[String]): Set[String] = {
      if (synsets.isEmpty) synonyms
      else {
        val syn1 = synonyms ++ collectSynonyms(synsets.head)
        loop(synsets.tail, syn1)
      }
    }
    val synsets = getSynsets(word)
    loop(synsets, Set.empty)
  }

  def synonymous(x: String, y: String): Boolean = {
    val syns = getFirstSenseSynonyms(x).map(_.toLowerCase)
    syns(y)
  }

  def notSynonymous(x: String, y: String): Boolean = {
    val syns = getAllSenseSynonyms(x).map(_.toLowerCase)
    !syns(y)
  }

  /**
   * Hypernym methods
   */
  def collectHypernyms(synset: ISynset, hypernyms: Set[String], maxHeight: Int): Set[String] = {
    val h = synset.getRelatedSynsets(Pointer.HYPERNYM).asScala 
    val hi = synset.getRelatedSynsets(Pointer.HYPERNYM_INSTANCE).asScala 
    val parents = h ++ hi
    if (parents.isEmpty || maxHeight <= 0) hypernyms
    else {
      parents.map{ p =>
        val hyps = dict.getSynset(p)
        val words = hyps.getWords().asScala
        val hypernyms1 = hypernyms ++ words.map{w => w.getLemma}.toSet
        collectHypernyms(hyps, hypernyms1, maxHeight-1)
      }.reduce(_ union _)
    }
  }

  def getFirstSenseHypernyms(word: String, maxHeight: Int = 1000): Set[String] = {
    getSynsets(word) match {
      case Nil => Set()
      case x::xs => collectHypernyms(x, Set.empty, maxHeight)
    }
  }

  def getAllSenseHypernyms(word: String, maxHeight: Int = 1000): Set[String] = {
    def loop(synsets: List[ISynset], hypernyms: Set[String]): Set[String] = {
      if (synsets.isEmpty) hypernyms
      else {
        val hyp1 = collectHypernyms(synsets.head, hypernyms, maxHeight)
        loop(synsets.tail, hyp1)
      }
    }
    val synsets = getSynsets(word)
    loop(synsets, Set.empty)
  }

  def kindOf(x: String, y:String): Boolean = {
    getFirstSenseHypernyms(x)(y)
  }

  def notKindOf(x: String, y:String): Boolean = {
    !getAllSenseHypernyms(x)(y)
  }

  /**
   * Antonym methods
   */
  def collectAntonyms(word: IWord): Set[String] = {
    val antonyms =  word.getRelatedWords(Pointer.ANTONYM).asScala 
    antonyms.map{a => a.getLemma}.toSet
  }

  def getFirstSenseAntonyms(word: String): Set[String] = {
    getWords(word) match {
      case Nil => Set()
      case x::xs => collectAntonyms(x)
    }
  }

  def getAllSenseAntonyms(word: String): Set[String] = {
    def loop(words: List[IWord], antonyms: Set[String]): Set[String] = {
      if (words.isEmpty) antonyms
      else {
        val ant1 = antonyms ++ collectAntonyms(words.head)
        loop(words.tail, ant1)
      }
    }
    val words = getWords(word)
    loop(words, Set.empty)
  }

  def antonymous(x: String, y: String): Boolean = {
    getFirstSenseAntonyms(x)(y)
  }

  def notAntonymous(x: String, y: String): Boolean = {
    !getAllSenseAntonyms(x)(y)
  }

  /**
   * Alternation methods
   */
  def collectLevelHypernyms(synset: ISynset, maxHeight: Int): Set[String] = {
    val h = synset.getRelatedSynsets(Pointer.HYPERNYM).asScala 
    val hi = synset.getRelatedSynsets(Pointer.HYPERNYM_INSTANCE).asScala 
    val next = h ++ hi
    if (next.isEmpty || maxHeight < 0) Set()
    else if (maxHeight == 0) {
      val hyps = dict.getSynset(next.head)
      val words = hyps.getWords().asScala
      words.map{w => w.getLemma}.toSet
    }
    else {
      collectLevelHypernyms(dict.getSynset(next.head), maxHeight-1)
    }
  }

  def heightAlternation(x: ISynset, y: ISynset, height: Int): Set[String] = {
    val fx = collectLevelHypernyms(x, height)
    val fy = collectLevelHypernyms(y, height)
    fx.intersect(fy)
  }

  def sibling(x: String, y: String, maxHeight: Int = 2): Boolean = {
    def climb(x: ISynset, y: ISynset, currentHeight: Int): Boolean = {
      if (currentHeight >= maxHeight) false
      else { 
        val intersect = heightAlternation(x, y, currentHeight)
        if (intersect.nonEmpty) true
        else climb(x, y, currentHeight+1)
      }
    }
    
    val fx = getSynsets(x) 
    val fy = getSynsets(y) 
    if (fx.isEmpty || fy.isEmpty) false
    else climb(fx.head, fy.head, 0) 
  }

  def alternation(x: String, y: String): Boolean = {
    val fx = getFirstSenseHypernyms(x)
    val fy = getFirstSenseHypernyms(y)
    fx.intersect(fy).nonEmpty
  }
  
  def nonAlternation(x: String, y: String): Boolean = {
    val fx = getAllSenseHypernyms(x)
    val fy = getAllSenseHypernyms(y)
    fx.intersect(fy).isEmpty
  }

  /**
   * Labeling methods
   */
  def wordNetRelations(x:String, y:String): String = {
    val string = if (inVocab(x) && inVocab(y)) {
      val senses = if (singleSenseNoun(x) && singleSenseNoun(y)) "single" else "multiple"
      lazy val hypernym = tagHypernym(x,y)
      lazy val hyponym = tagHyponym(x,y)
      lazy val synonym = tagSynonym(x,y)
      lazy val antonym = tagAntonym(x,y)
      lazy val alternation = tagAlternation(x,y)
      Array(senses, synonym, hypernym, hyponym, antonym, alternation).mkString("\t")
    }
    else {
      Array("OOV", "unknown", "unknown", "unknown", "unknown", "unknown").mkString("\t")
    }
    string
  }

  def tagHypernym(x:String, y:String): String = {
    if (kindOf(x, y)) "hypernym"
    else if (notKindOf(x, y)) "nonhypernym"
    else "possible"
  }

  def tagHyponym(x:String, y:String): String = {
    if (kindOf(y, x)) "hyponym"
    else if (notKindOf(y, x)) "nonhyponym"
    else "possible"
  }

  def tagSynonym(x:String, y:String): String = {
    if (x == y) "identical"
    else if (synonymous(x, y)) "synonym"
    else if (notSynonymous(x, y)) "nonsynonym"
    else "possible"
  }

  def tagAntonym(x:String, y:String): String = {
    if (antonymous(x, y)) "antonym"
    else if (notAntonymous(x, y)) "nonantonym"
    else "possible"
  }

  def tagAlternation(x:String, y:String): String = {
    if (x == y || synonymous(x,y)) "nonalternation"
    else if (sibling(x, y)) "sibling"
    else if (kindOf(x, y) || kindOf(y, x) || antonymous(x, y)) "nonalternation"
    else if (alternation(x, y)) "alternation"
    else if (nonAlternation(x, y)) "nonalternation"
    else "possible"
  }

    
}

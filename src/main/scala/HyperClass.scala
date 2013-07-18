import edu.jhu.hltcoe.sp.data.depparse._
import java.io.BufferedWriter
import scala.collection.JavaConverters.asScalaIteratorConverter
import scala.collection.JavaConverters.collectionAsScalaIterableConverter
import org.apache.commons.io.filefilter.PrefixFileFilter
import org.apache.commons.io.FileUtils.listFiles
import org.apache.log4j._
import edu.jhu.jerboa.util.FileManager
import edu.jhu.agiga.AgigaConstants
import edu.jhu.agiga.AgigaPrefs
import edu.jhu.agiga.StreamingSentenceReader
import edu.stanford.nlp.util.Pair
import edu.stanford.nlp.trees._

object HyperClass {
  /**
   * Traverses agiga documents in directory (args(1)) that match prefix (args(2))
   * writes dependency paths to files in output directory (args(4))
   * Dependency type is determined by args(0), args(3) gives the path to the WordNet files
   */
  def main(args: Array[String]) = {
    val dependencyTypes = Set("basic", "collapsed", "propagated", "all")
    def usage = {
      println("""|Usage: HyperClass [dependency-type] [agiga-dir] [agiga-prefix] [wn-dir] ([output-dir])
                 |
                 |       legal dependency types: basic, collapsed, propagated, all""".stripMargin)
    }
    if ((args.length < 4 || args.length > 5) || !dependencyTypes(args(0))) usage
    else {
      val cAppender = new ConsoleAppender(new PatternLayout("%d{HH:mm:ss,SSS} [%t] %c %x -%m%n"))
      BasicConfigurator.configure(cAppender)

      val log = Logger.getRootLogger
      log.setLevel(Level.INFO)

      val agiga = new java.io.File(args(1))
      val prefix = new PrefixFileFilter(args(2))
      val wordnet = new edu.jhu.hyperclass.WordNet(args(3))
      val outdir = args.length match {
        case 5 => new java.io.File(args.last)
        case 4 => new java.io.File(args.last + ".output")
      }
      outdir.mkdirs

      
      val prefs = new AgigaPrefs

      val form = args(0) match {
        case "collapsed" => AgigaConstants.DependencyForm.COL_DEPS
        case "propagated" => AgigaConstants.DependencyForm.COL_CCPROC_DEPS
        case _ => AgigaConstants.DependencyForm.BASIC_DEPS
      }
        
      println(form)

      listFiles(agiga, prefix, null).asScala foreach { item =>
        val arg = item.toString 
        val file = arg.split("/").last
        val outFile = file.split("""\.""")(0) + ".out"
        val rulesWriter = FileManager.getWriter(outdir + "/" + outFile)
        log.info("Parsing XML file "+arg)

        val reader = new StreamingSentenceReader(arg, prefs)

        reader.asScala.foreach { sent  => 
          if (args(0) == "all") {
            val rule = new AllDependencyRule(sent)
            rule.extractDIRTdependencies(rulesWriter, false, wordnet)
          }
          else {
            val rule = new DirtRuleFromAgiga(sent, form)
            rule.extractDIRTdependencies(rulesWriter, false, wordnet)
          }
          rulesWriter.flush
        }

        log.info("Number of sentences: " + reader.getNumSents())
        rulesWriter.close()  
      }
    }
  }
}

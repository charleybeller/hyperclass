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
   * Traverses agiga documents in directory (args(0)) that match prefix (args(1))
   * writes dependency paths to files in output directory (args(2))
   */
  def main(args: Array[String]) = {
    val dependencyTypes = Set("basic", "collapsed", "propagated")
    def usage = {
      println("""|Usage: HyperClass [dependency-type] [agiga-dir] [agiga-prefix] ([output-dir])
                 |
                 |       legal dependency types: basic, collapsed, propagated""".stripMargin)
    }
    if ((args.length < 3 || args.length > 4) || !dependencyTypes(args(0))) usage
    else {
      val cAppender = new ConsoleAppender(new PatternLayout("%d{HH:mm:ss,SSS} [%t] %c %x -%m%n"))
      BasicConfigurator.configure(cAppender)

      val log = Logger.getRootLogger
      log.setLevel(Level.INFO)

      val agiga = new java.io.File(args(1))
      val prefix = new PrefixFileFilter(args(2))
      val outdir = args.length match {
        case 4 => new java.io.File(args.last)
        case 3 => new java.io.File(args.last + ".output")
      }
      outdir.mkdirs
      
      val prefs = new AgigaPrefs
      //val form = AgigaConstants.DependencyForm.BASIC_DEPS
      val form = args(0) match {
        case "basic" => AgigaConstants.DependencyForm.BASIC_DEPS
        case "collapsed" => AgigaConstants.DependencyForm.COL_DEPS
        case "propagated" => AgigaConstants.DependencyForm.COL_CCPROC_DEPS
      }
        
      println(form)
      prefs.setForConnlStyleDeps(form)

      listFiles(agiga, prefix, null).asScala foreach { item =>
        val arg = item.toString 
        val file = arg.split("/").last
        val outFile = file.split("""\.""")(0) + ".out"
        val rulesWriter = FileManager.getWriter(outdir + "/" + outFile)
        log.info("Parsing XML file "+arg)

        val reader = new StreamingSentenceReader(arg, prefs)

        reader.asScala.foreach { sent  => 
          val rule = new DirtRuleFromAgiga(sent, form)
          rule.extractDIRTdependencies(rulesWriter, false)
        }

        log.info("Number of sentences: " + reader.getNumSents())
        rulesWriter.close()  
      }
    }
  }
}

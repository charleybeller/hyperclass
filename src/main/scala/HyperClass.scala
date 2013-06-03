import edu.jhu.hltcoe.sp.data.depparse._
import java.io.BufferedWriter
import scala.collection.JavaConverters.asScalaIteratorConverter
import scala.collection.JavaConverters.collectionAsScalaIterableConverter
import org.apache.commons.io.filefilter.PrefixFileFilter
import org.apache.commons.io.FileUtils.listFiles
import org.apache.log4j._
import edu.jhu.jerboa.util.FileManager
import edu.jhu.agiga._
import edu.stanford.nlp.util.Pair
import edu.stanford.nlp.trees._

object HyperClass {
  /**
   * Traverses agiga documents in directory (args(0)) that match prefix (args(1))
   * writes dependency paths to files in output directory (args(2))
   */
  def main(args: Array[String]) = {
    if (args.length < 2 || args.length > 3) println("Usage: HyperClass [agiga-directory] [agiga-prefix] ([output-file])")
    else {
      val cAppender = new ConsoleAppender(new PatternLayout("%d{HH:mm:ss,SSS} [%t] %c %x -%m%n"))
      BasicConfigurator.configure(cAppender)

      val log = Logger.getRootLogger
      log.setLevel(Level.INFO)

      val agiga = new java.io.File(args(0))
      val prefix = new PrefixFileFilter(args(1))
      val outdir = args.length match {
        case 3 => new java.io.File(args.last)
        case 2 => new java.io.File(args.last + ".output")
      }
      outdir.mkdirs
      
      listFiles(agiga, prefix, null).asScala foreach { item =>
        val arg = item.toString 
        val file = arg.split("/").last
        val outFile = file.split("""\.""")(0) + ".out"
        val rulesWriter = FileManager.getWriter(args(2) + "/" +outFile)
        log.info("Parsing XML file "+arg)

        val prefs = new AgigaPrefs
        val form = AgigaConstants.DependencyForm.BASIC_DEPS
        prefs.setForConnlStyleDeps(form)
        val reader = new StreamingSentenceReader(arg, prefs)

        reader.asScala foreach {sent => 
          val rule = new DirtRuleFromAgiga(sent.getStanfordWordLemmaTags(), sent.getStanfordTreeGraphNodes(form), sent.getStanfordTypedDependencies(form))
          rule.extractDIRTdependencies(rulesWriter, false)
        }

        log.info("Number of sentences: " + reader.getNumSents())
        rulesWriter.close()  
      }
    }
  }
}

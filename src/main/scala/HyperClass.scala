import edu.jhu.hltcoe.sp.data.depparse._
import java.io.BufferedWriter
import scala.collection.JavaConverters.asScalaIteratorConverter
import org.apache.log4j._
import edu.jhu.jerboa.util.FileManager
import edu.jhu.agiga._
import edu.stanford.nlp.util.Pair
import edu.stanford.nlp.trees._

object HyperClass {
  /**
   * Traverses agiga document (args(0)) writes dependency paths to file (args(1))
   */
  def main(args: Array[String]) = {
    if (args.length != 2) println("Usage: HyperClass [agiga-file] [output-file]")
    else {
      val cAppender = new ConsoleAppender(new PatternLayout("%d{HH:mm:ss,SSS} [%t] %c %x -%m%n"))
      BasicConfigurator.configure(cAppender)

      val log = Logger.getRootLogger
      log.setLevel(Level.INFO)

      val rulesWriter = FileManager.getWriter(args(1))

      log.info("Parsing XML file "+args(0))

      val prefs = new AgigaPrefs
      val form = AgigaConstants.DependencyForm.BASIC_DEPS
      prefs.setForConnlStyleDeps(form)
      val reader = new StreamingSentenceReader(args(0), prefs)

      reader.asScala foreach {sent => 
        val rule = new DirtRuleFromAgiga(sent.getStanfordWordLemmaTags(), sent.getStanfordTreeGraphNodes(form), sent.getStanfordTypedDependencies(form))
        rule.extractDIRTdependencies(rulesWriter, false)
      }

      log.info("Number of sentences: " + reader.getNumSents())
      rulesWriter.close()  
    }
  }
}

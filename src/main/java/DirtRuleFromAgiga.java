/**
 * 
 */
package edu.jhu.hltcoe.sp.data.depparse;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import edu.jhu.agiga.AgigaPrefs;
import edu.jhu.agiga.AgigaSentence;
import edu.jhu.agiga.StreamingSentenceReader;
import edu.jhu.agiga.AgigaConstants.DependencyForm;
import edu.jhu.jerboa.util.FileManager;
import edu.stanford.nlp.ling.WordLemmaTag;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeGraphNode;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.Pair;

import edu.jhu.hyperclass.WordNet;

/**
 * This class parses AGIGA .xml.gz files and output DIRT-like rules
 * @author Xuchen Yao
 *
 */
public class DirtRuleFromAgiga {
  
  private static Logger log = Logger.getLogger(DirtRuleFromAgiga.class);
  
  private List<WordLemmaTag> labels;
  private List<TreeGraphNode> tree;
  private List<TypedDependency> dependencies;
  protected TreeGraphNode root = null;
  protected HashMap<Pair<TreeGraphNode, TreeGraphNode>, TypedDependency> nodes2dep = null;
  
  public DirtRuleFromAgiga (List<WordLemmaTag> labels, List<TreeGraphNode> tree, List<TypedDependency> dependencies){
    this.labels = labels;
    this.tree = tree;
    this.dependencies = dependencies;
    // 0th of tree is ROOT, while 0th of labels is the first word in the sentence
    this.root = this.tree.get(0);
    //System.out.println(this.labels.get(0).lemma());
    //this.tree.get(1).label().setLemma(this.labels.get(0).lemma());
    for (int i=0; i<labels.size(); i++) {
      this.tree.get(i+1).label().setLemma(labels.get(i).lemma());
    }
  }
  
  public int getSize() {
    return this.tree.size();
  }

  public List<WordLemmaTag> getLabels() {
    return labels;
  }

  public List<TreeGraphNode> getTree() {
    return tree;
  }

  public List<TypedDependency> getDependencies() {
    return dependencies;
  }
  
  public TreeGraphNode getRoot() {
    if (this.root != null) return this.root;

    for (int i=0; i<this.tree.size(); i++) {
      this.root = this.tree.get(i);
      while (this.root.parent() != null) {
        this.root = (TreeGraphNode) this.root.parent();
      }
      // important: this version of dependencies have a lot of
      // "islands", i.e., nodes without parents or children.
      // especially the quote symbols. ('Mr. Smith lost ...', he said)
      if (this.root.children().length != 0) {
        break;
      }
    }
    return this.root;
  }

  public HashMap<Pair<TreeGraphNode, TreeGraphNode>, TypedDependency> nodes2dep() {
    if (this.nodes2dep != null) return this.nodes2dep;
    this.nodes2dep = new HashMap<Pair<TreeGraphNode, TreeGraphNode>, TypedDependency>();
    for (TypedDependency dep:this.dependencies) {
      this.nodes2dep.put(new Pair<TreeGraphNode, TreeGraphNode>(dep.gov(), dep.dep()), dep);
    }
    return this.nodes2dep;
  }
  
  public void extractDIRTdependencies(BufferedWriter rulesWriter, boolean print_to_stdout) throws IOException {
    if (this.root == null) this.getRoot();

    HashMap<Pair<TreeGraphNode, TreeGraphNode>, TypedDependency> nodes2dep = this.nodes2dep();
    for (int i=0; i<this.labels.size()-1; i++) {
      WordLemmaTag iLabel = this.labels.get(i);
      // slot fillers have to be nouns
      if (!iLabel.tag().startsWith("NN")) continue;

      for (int j=i+1; j<this.labels.size(); j++) {
        WordLemmaTag jLabel = this.labels.get(j);
        // slot fillers have to be nouns
        if (!jLabel.tag().startsWith("NN")) continue;
        TreeGraphNode iNode = this.tree.get(i+1);
        TreeGraphNode jNode = this.tree.get(j+1);
        List<Tree> paths = this.root.pathNodeToNode(iNode, jNode);
        // if path.size()==2, we have rules like this:
        // ??/NNP<-nn<-NNP/??
        if (paths == null || paths.size() < 3 || paths.size() > 5) continue;
        // [poured, Smith, died, earlier, attack, aged, 55]
        String x = "", y = "";
        TreeGraphNode xNode = (TreeGraphNode) paths.get(0);
        TreeGraphNode yNode = (TreeGraphNode) paths.get(paths.size() - 1);
        x = xNode.label().lemma();
        y = yNode.label().lemma();
        String hypernymRelation = "";
        if (WordNet.isa(x,y))
          hypernymRelation = "xy";
        else if (WordNet.isa(y,x))
          hypernymRelation = "yx";
        else if (WordNet.isnota(x,y) && WordNet.isnota(y,x))
          hypernymRelation = "zz";
        else hypernymRelation = "unknown";
        StringBuilder dirtRule = this.buildRule(paths);
        dirtRule.append("\t"+hypernymRelation+"\tX="+x+"\tY="+y+"\tphrases=");
        for (int k=i; k<=j; k++) {
          dirtRule.append(this.labels.get(k).word()+" ");
        }
        String rule = dirtRule.toString();
        if (print_to_stdout)
          System.out.println(rule);
        rulesWriter.write(rule+"\n");
      }
    }
  }

  protected StringBuilder buildRule(List<Tree> paths) {
    StringBuilder dirtRule = new StringBuilder(); 
    TreeGraphNode xNode = (TreeGraphNode) paths.get(0);
    dirtRule.append(getReducedTag(xNode.label().tag()));
    dirtRule.append(":");
    TreeGraphNode yNode;
    this.nodes2dep();
    for (int k=0; k<paths.size()-1; k++) {
      xNode = (TreeGraphNode) paths.get(k);
      yNode = (TreeGraphNode) paths.get(k+1);
      TypedDependency dep = nodes2dep.get(new Pair<TreeGraphNode, TreeGraphNode>(xNode, yNode));
      String direction = "";
      if (dep == null) {
        dep = nodes2dep.get(new Pair<TreeGraphNode, TreeGraphNode>(yNode, xNode));
         if (dep == null) {
          System.err.println("Wrong dependeicies between " + xNode + " and " + yNode);
          System.err.println("check your code!");
        }
        direction = "<-";
      } else {
        direction = "->";
      }

      if (k != 0) {
        dirtRule.append(direction);
        dirtRule.append(getReducedTag(xNode.label().tag()));
        dirtRule.append(":");
      }
      dirtRule.append(dep.reln().toString());
      dirtRule.append(":");
      dirtRule.append(getReducedTag(yNode.label().tag()));
      
      if (k != paths.size() - 2) {
        dirtRule.append(direction);
        dirtRule.append(yNode.label().lemma());
      }
    }
    return dirtRule;
  }
  
  public static String getReducedTag(String tag) {
    if (tag.toLowerCase().startsWith("nn"))
      return "n";
    else if (tag.toLowerCase().startsWith("vb"))
      return "v";
    else
      return tag;
  }

  /**
   * @param args
   */
  public static void main(String[] args) throws Exception {
    ConsoleAppender cAppender = new ConsoleAppender(new PatternLayout("%d{HH:mm:ss,SSS} [%t] %p %c %x - %m%n"));
    BasicConfigurator.configure(cAppender);
    // Must be Level.TRACE for debug logging
    Logger.getRootLogger().setLevel(Level.INFO);

    BufferedWriter rulesWriter = FileManager.getWriter(args[1]);

    log.info("Parsing XML file "+args[0]);
    
    AgigaPrefs prefs = new AgigaPrefs();
    DependencyForm form = DependencyForm.BASIC_DEPS;
        prefs.setForConnlStyleDeps(form);
        StreamingSentenceReader reader = new StreamingSentenceReader(args[0], prefs);

        for (AgigaSentence sent : reader) {
          DirtRuleFromAgiga rule = new DirtRuleFromAgiga(sent.getStanfordWordLemmaTags(), sent.getStanfordTreeGraphNodes(form), sent.getStanfordTypedDependencies(form));
          rule.extractDIRTdependencies(rulesWriter, false);
        }
        log.info("Number of sentences: " + reader.getNumSents());

    rulesWriter.close();
  }

}

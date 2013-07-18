/**
 * 
 */
package edu.jhu.hltcoe.sp.data.depparse;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Deque;
import java.util.ArrayDeque;

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
 * @author Charley Beller
 *	added functionality to label rules with WordNet relations,
 *	deal with collapsed, possibly cyclic, dependencies
 *
 */
public class DirtRuleFromAgiga {
	
	private static Logger log = Logger.getLogger(DirtRuleFromAgiga.class);
	
	private List<WordLemmaTag> labels;
	private List<TreeGraphNode> tree;
	private List<TypedDependency> dependencies;
	protected Boolean collapsed = false;
	protected Boolean cyclic;
	protected TreeGraphNode root = null;
	protected HashMap<Pair<TreeGraphNode, TreeGraphNode>, TypedDependency> nodes2dep = null;
	protected HashMap<TreeGraphNode, Set<TreeGraphNode>> arcsMap = null;
	
	public DirtRuleFromAgiga (AgigaSentence sentence, DependencyForm form){
		this.labels = sentence.getStanfordWordLemmaTags();
		this.tree = sentence.getStanfordTreeGraphNodes(form);
		this.dependencies = sentence.getStanfordTypedDependencies(form);
		this.root = this.tree.get(0);
		this.collapsed = form != DependencyForm.BASIC_DEPS;
		for (int i=0; i<labels.size(); i++) {
			this.tree.get(i+1).label().setLemma(labels.get(i).lemma());
		}
	}

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

	public Boolean isCollapsed() {
		return collapsed;
	}


	@SuppressWarnings("unchecked")
	public Boolean isCyclic() {
		if (this.cyclic != null) return this.cyclic;
		if (this.isCollapsed()) {
			Set<Pair<TreeGraphNode, TreeGraphNode>> keys = nodes2dep().keySet();
			for (Pair<TreeGraphNode, TreeGraphNode> pair : keys) {
				if (keys.contains(new Pair(pair.second, pair.first))) {
					this.cyclic = true;
					return this.cyclic;
				}
			}
		}
		this.cyclic = false;
		return this.cyclic;
	}

	public List<Tree> cyclicPathNodeToNode(Tree start, Tree goal) {
		for (int depth=0; depth<arcsMap().size(); depth++) {
			List<Tree> path = depthLimitedSearch(start, goal, depth);
			if (path != null) return path;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private List<Tree> depthLimitedSearch(Tree start, Tree goal, int maxDepth) {
		//check that start and goal are valid
		if (arcsMap().containsKey(start) && arcsMap().containsKey(goal)) {
			int depth = 0;
			List<Tree> path = new java.util.ArrayList();
			Set<Tree> visited = new java.util.HashSet();
			Deque<Tree> stack = new java.util.ArrayDeque();
			stack.push(start);
			while (stack.peek() != null && depth < maxDepth) {
				depth += 1;
				Tree node = stack.pop();
				Boolean deadEnd = true;
				path.add(node);
				visited.add(node);
				//System.out.println("stack: " + stack);
				//System.out.println("path: " + path);
				//System.out.println("visited: " + visited);
				//System.out.println("depth: " + depth);
				if (node == goal) return path; //success!
				else {
					for (Tree t : arcsMap().get(node)) {
						if (! visited.contains(t)) {
							stack.push(t);
							deadEnd = false;
						}
					}
					//System.out.println("stack: " + stack);
					//System.out.println("path: " + path);
					//System.out.println("visited: " + visited);
					//System.out.println("deadEnd: " + deadEnd);
					//backtrack to last non-dead-end node
					while (deadEnd) {
						path.remove(path.size() - 1);
						if (path.isEmpty()) return null;
						Tree previous = path.get(path.size() - 1);
						for (Tree t : arcsMap().get(previous)) {
							if (! visited.contains(t)) deadEnd = false;
						}
					}
					//System.out.println("stack: " + stack);
					//System.out.println("path: " + path);
					//System.out.println("visited: " + visited);
				}
				//System.out.println("_________________________");
			}
		}
		return null;
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

	public HashMap<TreeGraphNode, Set<TreeGraphNode>> arcsMap() {
		if (this.arcsMap != null) return this.arcsMap;
		this.arcsMap = new HashMap<TreeGraphNode, Set<TreeGraphNode>>();
		for (Pair<TreeGraphNode,TreeGraphNode> key : nodes2dep().keySet()) {
			addEntry(key);
		}
		return this.arcsMap;
	}
	
	private void addEntry(Pair<TreeGraphNode,TreeGraphNode> entry){
		if (this.arcsMap.get(entry.first) == null)
			this.arcsMap.put(entry.first, new HashSet<TreeGraphNode>());
		this.arcsMap.get(entry.first).add(entry.second);
		if (this.arcsMap.get(entry.second) == null)
			this.arcsMap.put(entry.second, new HashSet<TreeGraphNode>());
		this.arcsMap.get(entry.second).add(entry.first);
	}

	public HashMap<Pair<TreeGraphNode, TreeGraphNode>, TypedDependency> nodes2dep() {
		if (this.nodes2dep != null) return this.nodes2dep;
		this.nodes2dep = new HashMap<Pair<TreeGraphNode, TreeGraphNode>, TypedDependency>();
		for (TypedDependency dep:this.dependencies) {
			this.nodes2dep.put(new Pair<TreeGraphNode, TreeGraphNode>(dep.gov(), dep.dep()), dep);
		}
		return this.nodes2dep;
	}
	
	public void extractDIRTdependencies(BufferedWriter rulesWriter, boolean print_to_stdout, WordNet wordnet) throws IOException {
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
				List<Tree> paths;	 
				if (this.isCollapsed())
					paths = this.cyclicPathNodeToNode(iNode, jNode);
				else 
					paths = this.root.pathNodeToNode(iNode, jNode);
				// if path.size()==2, we have rules like this:
				// ??/NNP<-nn<-NNP/??
				if (paths == null || paths.size() < 3 || paths.size() > 4) continue;
				// [poured, Smith, died, earlier, attack, aged, 55]
				String x = "", y = "";
				TreeGraphNode xNode = (TreeGraphNode) paths.get(0);
				TreeGraphNode yNode = (TreeGraphNode) paths.get(paths.size() - 1);
				x = xNode.label().value();
				y = yNode.label().value();
        String xLemma = xNode.label().lemma();
        String yLemma = yNode.label().lemma();
				//label WordNet relations from wordnet
				String wordNetRelations = wordnet.wordNetRelations(xLemma,yLemma);
				//
				StringBuilder dirtRule = this.buildRule(paths);
				dirtRule.append("\t"+wordNetRelations+"\tX="+x+"|"+xLemma+
            "\tY="+y+"|"+yLemma+"\tphrases=");
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

	private StringBuilder buildRule(List<Tree> paths) {
		StringBuilder dirtRule = new StringBuilder(); 
		TreeGraphNode xNode = (TreeGraphNode) paths.get(0);
		for (Tree child : xNode.getChildrenAsList()) {
      if ("such".equals(child.nodeString()))
        dirtRule.append("such<-amod:");
    }
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
				if (dep == null) 
					continue;
				direction = "<-";
			}
			else {
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
    Tree last = paths.get(paths.size() - 1);
    for (Tree child : last.getChildrenAsList()) {
      if ("other".equals(child.nodeString()))
        dirtRule.append(":amod->other");
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

//
//	/**
//	 * @param args
//	 */
//	public static void main(String[] args) throws Exception {
//		ConsoleAppender cAppender = new ConsoleAppender(new PatternLayout("%d{HH:mm:ss,SSS} [%t] %p %c %x - %m%n"));
//		BasicConfigurator.configure(cAppender);
//		// Must be Level.TRACE for debug logging
//		Logger.getRootLogger().setLevel(Level.INFO);
//
//		BufferedWriter rulesWriter = FileManager.getWriter(args[1]);
//
//		log.info("Parsing XML file "+args[0]);
//		
//		AgigaPrefs prefs = new AgigaPrefs();
//		DependencyForm form = DependencyForm.BASIC_DEPS;
//				prefs.setForConnlStyleDeps(form);
//				StreamingSentenceReader reader = new StreamingSentenceReader(args[0], prefs);
//
//				for (AgigaSentence sent : reader) {
//					DirtRuleFromAgiga rule = new DirtRuleFromAgiga(sent.getStanfordWordLemmaTags(), sent.getStanfordTreeGraphNodes(form), sent.getStanfordTypedDependencies(form));
//					rule.extractDIRTdependencies(rulesWriter, false);
//				}
//				log.info("Number of sentences: " + reader.getNumSents());
//
//		rulesWriter.close();
//	}

}

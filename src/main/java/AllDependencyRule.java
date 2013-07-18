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
 *	prints three dependency paths: basic, collapsed, propagated
 *
 */
public class AllDependencyRule {

	private static Logger log = Logger.getLogger(DirtRuleFromAgiga.class);

	private List<WordLemmaTag> labels;
	private List<TreeGraphNode> tree;
	private List<TypedDependency> basic_deps;
	private List<TypedDependency> collapsed_deps;
	private List<TypedDependency> propagated_deps;
	protected TreeGraphNode root = null;
	protected HashMap<Pair<TreeGraphNode, TreeGraphNode>, TypedDependency> nodes2dep_basic = null;
	protected HashMap<Pair<TreeGraphNode, TreeGraphNode>, TypedDependency> nodes2dep_collapsed = null;
	protected HashMap<Pair<TreeGraphNode, TreeGraphNode>, TypedDependency> nodes2dep_propagated = null;
	protected HashMap<TreeGraphNode, Set<TreeGraphNode>> arcsMap_collapsed = null;
	protected HashMap<TreeGraphNode, Set<TreeGraphNode>> arcsMap_propagated = null;


				
	public AllDependencyRule(AgigaSentence sentence) {
		this.labels = sentence.getStanfordWordLemmaTags();
		this.tree = sentence.getStanfordTreeGraphNodes(DependencyForm.BASIC_DEPS);
		this.basic_deps = sentence.getStanfordTypedDependencies(DependencyForm.BASIC_DEPS);
		this.collapsed_deps = sentence.getStanfordTypedDependencies(DependencyForm.COL_DEPS);
		this.propagated_deps = sentence.getStanfordTypedDependencies(DependencyForm.COL_CCPROC_DEPS);
		this.root = this.tree.get(0);
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

	public List<Tree> cyclicPathNodeToNode(Tree start, Tree goal, HashMap<TreeGraphNode, Set<TreeGraphNode>> arcsMap) {
		for (int depth=0; depth<arcsMap.size(); depth++) {
			List<Tree> path = depthLimitedSearch(start, goal, depth, arcsMap);
			if (path != null) return path;
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	private List<Tree> depthLimitedSearch(Tree start, Tree goal, int maxDepth, HashMap<TreeGraphNode, Set<TreeGraphNode>> arcsMap) {
		//check that start and goal are valid
		if (arcsMap.containsKey(start) && arcsMap.containsKey(goal)) {
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
					for (Tree t : arcsMap.get(node)) {
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
						for (Tree t : arcsMap.get(previous)) {
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

	public HashMap<TreeGraphNode, Set<TreeGraphNode>> arcsMap_collapsed() {
		if (this.arcsMap_collapsed != null) return this.arcsMap_collapsed;
		this.arcsMap_collapsed = new HashMap<TreeGraphNode, Set<TreeGraphNode>>();
		for (Pair<TreeGraphNode,TreeGraphNode> key : nodes2dep_collapsed().keySet()) {
			addEntry(key, this.arcsMap_collapsed);
		}
		return this.arcsMap_collapsed;
	}

	public HashMap<TreeGraphNode, Set<TreeGraphNode>> arcsMap_propagated() {
		if (this.arcsMap_propagated != null) return this.arcsMap_propagated;
		this.arcsMap_propagated = new HashMap<TreeGraphNode, Set<TreeGraphNode>>();
		for (Pair<TreeGraphNode,TreeGraphNode> key : nodes2dep_propagated().keySet()) {
			addEntry(key, this.arcsMap_propagated);
		}
		return this.arcsMap_propagated;
	}

	private void addEntry(Pair<TreeGraphNode,TreeGraphNode> entry, HashMap<TreeGraphNode, Set<TreeGraphNode>> arcsMap){
		if (arcsMap.get(entry.first) == null)
			arcsMap.put(entry.first, new HashSet<TreeGraphNode>());
		arcsMap.get(entry.first).add(entry.second);
		if (arcsMap.get(entry.second) == null)
			arcsMap.put(entry.second, new HashSet<TreeGraphNode>());
		arcsMap.get(entry.second).add(entry.first);
	}

	public HashMap<Pair<TreeGraphNode, TreeGraphNode>, TypedDependency> nodes2dep_basic() {
		if (this.nodes2dep_basic != null) return this.nodes2dep_basic;
		this.nodes2dep_basic = new HashMap<Pair<TreeGraphNode, TreeGraphNode>, TypedDependency>();
		for (TypedDependency dep:this.basic_deps) {
			this.nodes2dep_basic.put(new Pair<TreeGraphNode, TreeGraphNode>(dep.gov(), dep.dep()), dep);
		}
		return this.nodes2dep_basic;
	}
	
	public HashMap<Pair<TreeGraphNode, TreeGraphNode>, TypedDependency> nodes2dep_collapsed() {
		if (this.nodes2dep_collapsed != null) return this.nodes2dep_collapsed;
		this.nodes2dep_collapsed = new HashMap<Pair<TreeGraphNode, TreeGraphNode>, TypedDependency>();
		for (TypedDependency dep:this.collapsed_deps) {
			this.nodes2dep_collapsed.put(new Pair<TreeGraphNode, TreeGraphNode>(dep.gov(), dep.dep()), dep);
		}
		return this.nodes2dep_collapsed;
	}
	
	public HashMap<Pair<TreeGraphNode, TreeGraphNode>, TypedDependency> nodes2dep_propagated() {
		if (this.nodes2dep_propagated != null) return this.nodes2dep_propagated;
		this.nodes2dep_propagated = new HashMap<Pair<TreeGraphNode, TreeGraphNode>, TypedDependency>();
		for (TypedDependency dep:this.propagated_deps) {
			this.nodes2dep_propagated.put(new Pair<TreeGraphNode, TreeGraphNode>(dep.gov(), dep.dep()), dep);
		}
		return this.nodes2dep_propagated;
	}
	
	public void extractDIRTdependencies(BufferedWriter rulesWriter, boolean print_to_stdout, WordNet wordnet) throws IOException {
		if (this.root == null) this.getRoot();

		HashMap<Pair<TreeGraphNode, TreeGraphNode>, TypedDependency> nodes2dep_basic = this.nodes2dep_basic();
		HashMap<Pair<TreeGraphNode, TreeGraphNode>, TypedDependency> nodes2dep_collapsed = this.nodes2dep_collapsed();
		HashMap<Pair<TreeGraphNode, TreeGraphNode>, TypedDependency> nodes2dep_propagated = this.nodes2dep_propagated();
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
				List<Tree> paths_basic = this.root.pathNodeToNode(iNode, jNode);
				List<Tree> paths_collapsed = this.cyclicPathNodeToNode(iNode, jNode, arcsMap_collapsed());
				List<Tree> paths_propagated = this.cyclicPathNodeToNode(iNode, jNode, arcsMap_propagated());
				// if path.size()==2, we have rules like this:
				// ??/NNP<-nn<-NNP/??
				if (paths_basic == null || paths_basic.size() < 2) continue;
        if (paths_collapsed == null || paths_collapsed.size() < 2) continue;
        if (paths_propagated == null || paths_propagated.size() < 2 || paths_propagated.size() > 4) continue;
				// [poured, Smith, died, earlier, attack, aged, 55]
				String x = "", y = "";
				TreeGraphNode xNode = (TreeGraphNode) paths_propagated.get(0);
				TreeGraphNode yNode = (TreeGraphNode) paths_propagated.get(paths_propagated.size() - 1);
				x = xNode.label().value();
				y = yNode.label().value();
        String xLemma = xNode.label().lemma();
        String yLemma = yNode.label().lemma();
				//label WordNet relations from wordnet
				String wordNetRelations = wordnet.wordNetRelations(xLemma,yLemma);
				//
				StringBuilder dirtRule = this.buildRule(paths_basic, nodes2dep_basic());
				dirtRule.append("\t");
				dirtRule.append(this.buildRule(paths_collapsed, nodes2dep_collapsed()));
				dirtRule.append("\t");
				dirtRule.append(this.buildRule(paths_propagated, nodes2dep_propagated()));
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

	private StringBuilder buildRule(List<Tree> paths, HashMap<Pair<TreeGraphNode, TreeGraphNode>, TypedDependency> nodes2dep) {
		StringBuilder dirtRule = new StringBuilder(); 
    assert(paths.size()>=2);
		TreeGraphNode xNode = (TreeGraphNode) paths.get(0);
    for (Tree child : xNode.getChildrenAsList()) {
      if ("such".equals(child.nodeString()))
        dirtRule.append("such<-amod:");
    }
		dirtRule.append(getReducedTag(xNode.label().tag()));
		dirtRule.append(":");
		TreeGraphNode yNode;
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


	/**
	 * @param args
	 */
  /*
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
	}*/

}


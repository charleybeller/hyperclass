import java.io.*;
import java.util.Random;
import java.util.Iterator;

import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.*;
import cc.mallet.types.*;
import cc.mallet.classify.*;

public class RegressionMallet {

	public static Classifier trainClassifier(InstanceList trainingInstances) {

	// Here we use a maximum entropy (ie polytomous logistic regression)                               
	//  classifier. Mallet includes a wide variety of classification                                   
	//  algorithms, see the JavaDoc API for details.                                                   

		ClassifierTrainer trainer = new MaxEntTrainer();
		return trainer.train(trainingInstances);
	}
    
	public static Classifier loadClassifier(File serializedFile) throws FileNotFoundException, IOException, ClassNotFoundException {

	// The standard way to save classifiers and Mallet data                                            
	//  for repeated use is through Java serialization.                                                
	// Here we load a serialized classifier from a file.                                               

		Classifier classifier;

		ObjectInputStream ois = new ObjectInputStream (new FileInputStream (serializedFile));
		classifier = (Classifier) ois.readObject();
		ois.close();

		return classifier;
	}

	public static void saveClassifier(Classifier classifier, File serializedFile) throws IOException {

	// The standard method for saving classifiers in                                                   
	//  Mallet is through Java serialization. Here we                                                  
	//  write the classifier object to the specified file.                                             

		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream (serializedFile));
		oos.writeObject (classifier);
		oos.close();
	}

	public static void printLabelings(Classifier classifier, File file) throws IOException {

		// Create a new iterator that will read raw instance data from                                     
		//  the lines of a file.                                                                           
		// Lines should be formatted as:                                                                   
		//                                                                                                 
		//   [name] [label] [data ... ]                                                                    
		//                                                                                                 
		//  in this case, "label" is ignored.                                                              

		CsvIterator reader = new CsvIterator(new FileReader(file), "(\\w+)\\s+(\\w+)\\s+(.*)", 3, 2, 1);  // (data, label, name) field indices               

		// Create an iterator that will pass each instance through                                         
		//  the same pipe that was used to create the training data                                        
		//  for the classifier.                                                                            
		Iterator<Instance> instances =
		classifier.getInstancePipe().newIteratorFrom(reader);
	
		// Classifier.classify() returns a Classification object                                           
		//  that includes the instance, the classifier, and the                                            
		//  classification results (the labeling). Here we only                                            
		//  care about the Labeling.                                                                       
		while (instances.hasNext()) {
			Labeling labeling = classifier.classify(instances.next()).getLabeling();

			// print the labels with their weights in descending order (ie best first)                     
	
			for (int rank = 0; rank < labeling.numLocations(); rank++){
				System.out.print(labeling.getLabelAtRank(rank) + ":" + labeling.getValueAtRank(rank) + " ");
			}
			System.out.println();

		}
	}

	public static void evaluate(Classifier classifier, InstanceList instances){
		
		Trial trial = new Trial(classifier, instances);

		// The Trial class implements many standard evaluation                                             
		//  metrics. See the JavaDoc API for more details.                                                 

		System.out.println("Accuracy: " + trial.getAccuracy());

		// precision, recall, and F1 are calcuated for a specific                                          
		//  class, which can be identified by an object (usually                                           
		//  a String) or the integer ID of the class                                                       

		System.out.println("F1 for class 'xy': " + trial.getF1("xy"));

		System.out.println("Precision for class '" + classifier.getLabelAlphabet().lookupLabel(1) + "': " + trial.getPrecision(1));
		System.out.println("Recall for class '" + classifier.getLabelAlphabet().lookupLabel(1) + "': " + trial.getRecall(1));
		
		System.out.println("F1 for class 'zz': " + trial.getF1("zz"));
		System.out.println("Precision for class '" + classifier.getLabelAlphabet().lookupLabel(0) + "': " + trial.getPrecision(0));
		System.out.println("Recall for class '" + classifier.getLabelAlphabet().lookupLabel(0) + "': " + trial.getRecall(0));
	}

	public static Trial testTrainSplit(InstanceList instances) {

		int TRAINING = 0;
		int TESTING = 1;
		int VALIDATION = 2;

		// Split the input list into training (90%) and testing (10%) lists.                               
		// The division takes place by creating a copy of the list,                                        
		//  randomly shuffling the copy, and then allocating                                               
		//  instances to each sub-list based on the provided proportions.                                  

		InstanceList[] instanceLists = instances.split(new Random(), new double[] {0.9, 0.1, 0.0});

		// The third position is for the "validation" set,                                                 
		//  which is a set of instances not used directly                                                  
		//  for training, but available for determining                                                    
		//  when to stop training and for estimating optimal                                               
		//  settings of nuisance parameters.                                                               
		// Most Mallet ClassifierTrainers can not currently take advantage                                 
		//  of validation sets.                                                                            

		Classifier classifier = trainClassifier( instanceLists[TRAINING] );
		evaluate(classifier, instanceLists[TRAINING]);
		evaluate(classifier, instanceLists[TESTING]);
		return new Trial(classifier, instanceLists[TESTING]);
	}

}

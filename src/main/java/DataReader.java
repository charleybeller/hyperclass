import java.io.*;
import java.util.*;
import java.util.regex.*;

import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.*;
import cc.mallet.types.*;

public class DataReader {

	Pipe pipe;
	CsvIterator iter;

	public DataReader(File file) throws IOException {
		pipe = buildPipe();
		iter = getIter(file);
	}

	public CsvIterator getIter(File file) throws IOException {

        	Pattern tokenPattern = Pattern.compile("(\\S+)\\t+(\\S+)\\t+(.*)");
		return new CsvIterator(new FileReader(file), tokenPattern, 1, 2, 3);

	}

	public InstanceList getInstances(){
		InstanceList instances = new InstanceList(pipe);
    		instances.addThruPipe(iter);
		return instances;
	}

	public Pipe buildPipe() {
		ArrayList<Pipe> pipeList = new ArrayList<Pipe>();

	 	Pattern tokenPattern = Pattern.compile("\\S+");

        	// Tokenize raw strings
                pipeList.add(new CharSequence2TokenSequence(tokenPattern));

		// Rather than storing tokens as strings, convert 
		//  them to integers by looking them up in an alphabet.
		pipeList.add(new TokenSequence2FeatureSequence());

		// Do the same thing for the "target" field: 
		//  convert a class label string to a Label object,
		//  which has an index in a Label alphabet.
		pipeList.add(new Target2Label());

		// Now convert the sequence of features to a sparse vector,
		//  mapping feature IDs to counts.
		pipeList.add(new FeatureSequence2FeatureVector());

		return new SerialPipes(pipeList);
	}

    public InstanceList readDirectory(File directory) {
        return readDirectories(new File[] {directory});
    }

    public InstanceList readDirectories(File[] directories) {
        
        // Construct a file iterator, starting with the 
        //  specified directories, and recursing through subdirectories.
        // The second argument specifies a FileFilter to use to select
        //  files within a directory.
        // The third argument is a Pattern that is applied to the 
        //   filename to produce a class label. In this case, I've 
        //   asked it to use the last directory name in the path.
        FileIterator iterator = new FileIterator(directories, new TxtFilter(), FileIterator.LAST_DIRECTORY);

        // Construct a new instance list, passing it the pipe
        //  we want to use to process instances.
        InstanceList instances = new InstanceList(pipe);

        // Now process each instance provided by the iterator.
        instances.addThruPipe(iterator);

        return instances;
    }

    public static void main (String[] args) throws IOException {

        DataReader importer = new DataReader(new File(args[0]));
        InstanceList instances = importer.getInstances();
        instances.save(new File(args[1]));

    }

    /** This class illustrates how to build a simple file filter */
    class TxtFilter implements FileFilter {

        /** Test whether the string representation of the file 
         *   ends with the correct extension. Note that {@ref FileIterator}
         *   will only call this filter if the file is not a directory,
         *   so we do not need to test that it is a file.
         */
        public boolean accept(File file) {
            return file.toString().endsWith(".txt");
        }
    }

}

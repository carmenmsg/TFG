package main;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import ca.pfv.spmf.algorithms.sequentialpatterns.BIDE_and_prefixspan.AlgoBIDEPlus2;
import ca.pfv.spmf.algorithms.sequentialpatterns.BIDE_and_prefixspan.SequentialPatterns;
import ca.pfv.spmf.input.sequence_database_list_integers.SequenceDatabase2;

/**
 * Example of how to use the BIDE+ algorithm, from the source code.
 * 
 * @author Philippe Fournier-Viger
 */
public class MainTestBIDEPlus_saveToMemory {

	public static void main(String [] arg) throws IOException{    
		// Load a sequence database
		SequenceDatabase2 sequenceDatabase2 = new SequenceDatabase2(); 
		sequenceDatabase2.loadFile(fileToPath("contextPrefixSpan-conClases.txt"));
		//sequenceDatabase2.loadFile(fileToPath("ejemplo-3clases.txt"));
		//sequenceDatabase2.print();
		
		int minsup2 = 2; // we use a minsup of 2 sequences
		
		int k = 5;
		
		// Create an instance of the algorithm
		AlgoBIDEPlus2 algo  = new AlgoBIDEPlus2();
		
        // if you set the following parameter to true, the sequence ids of the sequences where
        // each pattern appears will be shown in the result
        boolean showSequenceIdentifiers = false;
		
		// execute the algorithm
//		SequentialPatterns patterns = algo.runAlgorithm(sequenceDatabase2, null, minsup2, k);
//		algo.printStatistics(sequenceDatabase2.size());
//		patterns.printFrequentPatterns(sequenceDatabase2.size(),showSequenceIdentifiers);
        Map<String,List<SequentialPatterns>> mapaPatrones = algo.runAlgorithm(sequenceDatabase2, null, minsup2, k);
        
        algo.printStatistics(sequenceDatabase2.size());
        
        for(String clase: mapaPatrones.keySet()) {
        	System.out.println("c: " + clase);
	        Iterator<SequentialPatterns> iterator = mapaPatrones.get(clase).iterator();
	        while (iterator.hasNext()) {
	        	SequentialPatterns auxPatterns = iterator.next();
	        	auxPatterns.printFrequentPatterns(sequenceDatabase2.size(),showSequenceIdentifiers);
	        }
        }        
	}
	
	public static String fileToPath(String filename) throws UnsupportedEncodingException{
		URL url = MainTestBIDEPlus_saveToMemory.class.getResource(filename);
		 return java.net.URLDecoder.decode(url.getPath(),"UTF-8");
	}
}
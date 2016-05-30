package main;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
//import java.util.List;

//librerias algoritmo 2
import ca.pfv.spmf.algorithms.sequentialpatterns.BIDE_and_prefixspan.AlgoBIDEPlus2;
import ca.pfv.spmf.input.sequence_database_list_integers.SequenceDatabase2;

/*
 * Example of how to use the BIDE+ algorithm, from the source code.
 */
public class MainTestBIDEPlus_saveToFile {

	public static void main(String [] arg) throws IOException{ 
		System.out.println("Se va a ejecutar BIDE2");
		
		/**
		 * Algoritmo 2
		 */
		//Datos Algoritmo 2 Bide Discriminativo
		SequenceDatabase2 sequenceDatabase2 = new SequenceDatabase2();			// Load a sequence database
		sequenceDatabase2.loadFile(fileToPath("contextPrefixSpan-conClases.txt"));
		
		sequenceDatabase2.print();
		
		int minsup2 = 2; // we use a minsup of 2 sequences (50 % of the database size)
		
		//Algoritmo 2 Bide Discriminativo
		AlgoBIDEPlus2 algo2  = new AlgoBIDEPlus2();
		
        // if you set the following parameter to true, the sequence ids of the sequences where
        // each pattern appears will be shown in the result
        algo2.setShowSequenceIdentifiers(false);
		
		// execute the algorithm
        System.out.println(sequenceDatabase2.size());
        algo2.runAlgorithm(sequenceDatabase2, ".//output.txt", minsup2);  
		algo2.printStatistics(sequenceDatabase2.size());

		System.out.println("Salio!");
	}
	
	public static String fileToPath(String filename) throws UnsupportedEncodingException{
		URL url = MainTestBIDEPlus_saveToFile.class.getResource(filename);
		 return java.net.URLDecoder.decode(url.getPath(),"UTF-8");
	}
}
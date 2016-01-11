package main;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
//import java.util.List;

////librerias algoritmo 1
//import ca.pfv.spmf.algorithms.sequentialpatterns.BIDE_and_prefixspan.AlgoBIDEPlus;
//import ca.pfv.spmf.input.sequence_database_list_integers.SequenceDatabase;


//librerias algoritmo 2
import ca.pfv.spmf.algorithms.sequentialpatterns.BIDE_and_prefixspan.AlgoBIDEPlus2;
import ca.pfv.spmf.input.sequence_database_list_integers.SequenceDatabase2;

/*
 * Example of how to use the BIDE+ algorithm, from the source code.
 */
public class MainTestBIDEPlus_saveToFile {

	public static void main(String [] arg) throws IOException{    

//		/**
//		 * Algoritmo 1
//		 */
//		
//		//Datos
//		SequenceDatabase sequenceDatabase = new SequenceDatabase(); 		// Load a sequence database
//		sequenceDatabase.loadFile(fileToPath("contextPrefixSpan.txt"));
//		
//		sequenceDatabase.print();
//		
//		int minsup = 2; // we use a minsup of 2 sequences (50 % of the database size)
//		
//		//Algoritmo 1 Bide
//		AlgoBIDEPlus algo  = new AlgoBIDEPlus(); 
//		
//        // if you set the following parameter to true, the sequence ids of the sequences where
//        // each pattern appears will be shown in the result
//        algo.setShowSequenceIdentifiers(false);
//		
//		// execute the algorithm
//		algo.runAlgorithm(sequenceDatabase, ".//output.txt", minsup);    
//		algo.printStatistics(sequenceDatabase.size());
		
		
		/**
		 * Algoritmo 2
		 */
		
		//Datos Algoritmo 2 Bide Discriminativo
		SequenceDatabase2 sequenceDatabase = new SequenceDatabase2();			// Load a sequence database
		sequenceDatabase.loadFile(fileToPath("contextPrefixSpan-conClases.txt"));
		
		sequenceDatabase.print();
		
		int minsup = 2; // we use a minsup of 2 sequences (50 % of the database size)
		
		//Algoritmo 2 Bide Discriminativo
		AlgoBIDEPlus2 algo  = new AlgoBIDEPlus2();
		
		
        // if you set the following parameter to true, the sequence ids of the sequences where
        // each pattern appears will be shown in the result
        algo.setShowSequenceIdentifiers(false);
		
		// execute the algorithm
        algo.runAlgorithm(sequenceDatabase, ".//output.txt", minsup);  
		algo.printStatistics(sequenceDatabase.size());
		
	}
	
	public static String fileToPath(String filename) throws UnsupportedEncodingException{
		URL url = MainTestBIDEPlus_saveToFile.class.getResource(filename);
		 return java.net.URLDecoder.decode(url.getPath(),"UTF-8");
	}
}
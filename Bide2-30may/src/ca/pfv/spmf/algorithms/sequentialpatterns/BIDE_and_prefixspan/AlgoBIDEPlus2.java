package ca.pfv.spmf.algorithms.sequentialpatterns.BIDE_and_prefixspan;


import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
//import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import ca.pfv.spmf.input.sequence_database_list_integers.Sequence;
//import ca.pfv.spmf.input.sequence_database_list_integers.SequenceDatabase;
import ca.pfv.spmf.patterns.itemset_list_integers_without_support.Itemset;
import ca.pfv.spmf.tools.MemoryLogger;
import ca.pfv.spmf.input.sequence_database_list_integers.SequenceDatabase2;

import java.util.LinkedHashMap;

/**
 * This is a modified version of the BIDE algorithm so that it discovers
 * the set of maximal sequential patterns instead of the set of all
 * closed sequential patterns.
 * 
 * Copyright (c) 2008-2013 Philippe Fournier-Viger
 * 
 * This file is part of the SPMF DATA MINING SOFTWARE
 * (http://www.philippe-fournier-viger.com/spmf).
 *
 * SPMF is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SPMF is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SPMF.  If not, see <http://www.gnu.org/licenses/>.
 */

public class AlgoBIDEPlus2 {

	// for statistics
	long startTime;
	long endTime;
	
	// the number of patterns found
	int patternCount = 0;
		
	// absolute minimum support
	private int minsuppAbsolute;
	
	// object to write the file
	BufferedWriter writer = null;
	
	// For BIDE+, we have to keep a pointer to the original database
	private Map<String,List<PseudoSequenceBIDE>> initialDatabase = null;
	
	// The sequential patterns that are found 
	// (if the user want to keep them into memory)
	private SequentialPatterns patterns = null;
	
	/** if true, sequence identifiers of each pattern will be shown*/
	boolean showSequenceIdentifiers = false;
	
	/**--------------------------------------------------------------------yop
	 * vector of minimal thresholds for discriminative score of a pattern 
	 * for each class  
	 **/
	Map<String, Integer> dtc = new HashMap<String,Integer>();
	
	/**--------------------------------------------------------------------yop
	 * yop
	 * vector de los soportes de las clases Sc(P)
	 */
	ArrayList<Integer> soporte = new ArrayList<Integer>();
	
	
	/**
	 * Default constructor
	 */
	public AlgoBIDEPlus2(){}

	/**
	 * Run the algorithm
	 * @param database a sequence database
	 * @param outputPath an output file path
	 * @param minsup a minimum support as an integer representing a number of sequences
	 * @return return the result, if saved into memory, otherwise null
	 * @throws IOException  exception if error while writing the file
	 */
	public SequentialPatterns runAlgorithm(SequenceDatabase2 database, String outputPath, int minsup) throws IOException {

		// save minsup
		this.minsuppAbsolute = minsup;
		// reset the counter for the number of patterns found
		patternCount = 0; 
		// reset the stats about memory usage
		MemoryLogger.getInstance().reset();
		// save the start time
		startTime = System.currentTimeMillis();
		// start the algorithm
		bide(database, outputPath);
		// save the end time
		endTime = System.currentTimeMillis();

		// close the output file if the result was saved to a file
		if(writer != null){
			writer.close();
		}
		return patterns;
	}
	
	/**
	 * This is the main method for the BIDE+ algorithm.
	 * @param database a sequence database
	 * @throws IOException exception if some error occurs while writing the output file.
	 */
	private void bide(SequenceDatabase2 database, String outputFilePath) throws IOException{

		// if the user want to keep the result into memory
		if(outputFilePath == null){
			writer = null;
			patterns = new SequentialPatterns("FREQUENT SEQUENTIAL PATTERNS");
		}else{ // if the user want to save the result to a file
			patterns = null;
			writer = new BufferedWriter(new FileWriter(outputFilePath)); 
		}
		
		// The algorithm first scan the database to find all frequent items 
		// The algorithm note the sequences in which these items appear.
		// This is stored in a map:  Key: item  Value : IDs of sequences containing the item
		System.out.println("ELEFANTE");
		Map<Integer, Map<String, Set<Integer>>> mapSequenceID = findSequencesContainingItems(database);	//-------------------REVISAR
		System.out.println("ELEFANTEE");
		

		/**--------------------------------------------------------------------yop
		 * lista que guarda las referencias de los indices generales de las secuencias y 
		 * los indices en base a la clase  
		 **/
		List<Indice> enlace = correspondenciaEntreIndices(database);
				
		// WE CONVERT THE DATABASE TO A PSEUDO-DATABASE, AND REMOVE
		// THE ITEMS OF SIZE 1 THAT ARE NOT FREQUENT, SO THAT THE ALGORITHM 
		// WILL NOT CONSIDER THEM ANYMORE.

		//yop
		//rellenar dtc con valores las clases y valores 0
		for(String c : database.getDiferentesClases()) {
				dtc.put(c, 0);
		}
		//comprobación de que dtc está bien rellenado.
		for(String c : database.getDiferentesClases()) {
			System.out.println("Clase: " + c + " - " + dtc.get(c));
		}
		
		
		// we create a database
		initialDatabase = new HashMap<String,List<PseudoSequenceBIDE>>(database.size());
		listRellenarClasesCualquierMap(initialDatabase, database.getDiferentesClases());
		
		
		//para cada clase
		for(String c: database.getDiferentesClases()) {
			// for each sequence of the original database
			for(Sequence sequence : database.getSequences(c)){
				// we make a copy of the sequence while removing infrequent items
				Sequence optimizedSequence = sequence.cloneSequenceMinusItems(mapSequenceID, minsuppAbsolute);
				if(optimizedSequence.size() != 0){
					// if this sequence has size >0, we add it to the new database
					initialDatabase.get(c).add(new PseudoSequenceBIDE(optimizedSequence, 0, 0));
				}else {
					initialDatabase.get(c).add(null);
				}
			}
		}
				
		// for each sequence of the original database
		// For each frequent item
		for(Entry<Integer, Map<String, Set<Integer>>> sequenceIDItem : mapSequenceID.entrySet()) {
			// if the item is frequent
			Map<String, Set<Integer>> sidset = sequenceIDItem.getValue();
			if(sidset.values().size() >= minsuppAbsolute) {		//yop esto de sidset.values().size() sería un getSupport()
				//build the projected database with this item
				Integer item = sequenceIDItem.getKey();
				Map<String,List<PseudoSequenceBIDE>> projectedContext = buildProjectedContextSingleItem(item, initialDatabase, false, sidset);
				
				// Create the prefix with this item
				SequentialPattern prefix = new SequentialPattern();  
				prefix.addItemset(new Itemset(item));
				// set the sequence IDS of this prefix
				prefix.setSequenceIDs(sidset);
	
				//yop
//					for(String c: database.getDiferentesClases()){
//						if (dUB(prefix, database, c) < dtc.get(c))
//							return;
//					}
				
				// variable to store the largest support of patterns - variable para guardar el mayor soporte de los patrones
				// that will be found starting with this prefix
				if(projectedContext.size() >= minsuppAbsolute) {
					int successorSupport = 0;
					
					if(!checkBackScanPruning(prefix, sidset, enlace)) {
						successorSupport =  recursion(prefix, projectedContext, enlace); // recursion;
					}
					
					// Finally, because this prefix has support > minsup
					// and passed the backscan pruning,
					// we check if it has no sucessor with support >= minsup
					// (a forward extension)
					// IF no forward extension
					if(successorSupport !=sidset.values().size()){    // ######### MODIFICATION #### 	-yop esto de sidset.values().size() sería un getSupport()
						// IF there is also no backward extension
						if(!checkBackwardExtension(prefix, sidset, enlace)){ 
							// the pattern is closed and we save it
							savePattern(prefix);  
						}
					}
				}else {
					if(!checkBackwardExtension(prefix, sidset, enlace)){ 
						// the pattern is closed and we save it
						savePattern(prefix);  
					}
				}
			}
		}
		// check the memory usage for statistics
		MemoryLogger.getInstance().checkMemory();
	}
	
	/**
	 * Method to check if a prefix has a backward-extension (see Bide+ article for full details).
	 * This method do it a little bit differently than the BIDE+ article since
	 * we iterate with i on elements of the prefix instead of iterating with 
	 * a i on the itemsets of the prefix. But the idea is the same!
	 * @param prefix the current prefix
	 * @param sidset IDs of sequences containing the item
	 * @parma clase indica la clase a la que pertenece el item
	 * @return boolean true, if there is a backward extension
	 */
	private boolean checkBackwardExtension(SequentialPattern prefix, Map<String,Set<Integer>> sidset, List<Indice> enlace) {	
//		System.out.println("======" + prefix);

		int totalOccurenceCount = prefix.getItemOccurencesTotalCount();
		// For the ith item of the prefix
		for(int i=0; i< totalOccurenceCount; i++){
			
			Set<Integer> alreadyVisitedSID = new HashSet<Integer>();
			
			// SOME CODE USED BY "findAllFrequentPairsForBackwardExtensionCheck"
			Integer itemI = prefix.getIthItem(i);  // iPeriod 
			Integer itemIm1 = null;  // iPeriod -1
			if(i > 0){ 
				itemIm1 = prefix.getIthItem(i -1);	
			}
			// END NEW

			// Create a Map of pairs to count the support of items (represented by a pair) 
			// in the ith semi-maximum periods
			Map<PairBIDE, PairBIDE> mapPaires = new HashMap<PairBIDE, PairBIDE>();
			
			// (1) For each i, we build the list of maximum periods
			// for each sequence in the original database
			int highestSupportUntilNow = -1;
			
			for(String clase : sidset.keySet()) {
				// 1703 pat -  9391 ms
				for(int sequenceID : sidset.get(clase)){
					// OPTIMIZATION  PART 1==  DON'T CHECK THE BACK EXTENSION IF THERE IS NOT ENOUGH SEQUENCE LEFT TO FIND AN EXTENSION
					// THIS CAN IMPROVE THE PERFORMANCE BY UP TO 30% on FIFA
					int remainingSeqID = (sidset.get(clase).size() - alreadyVisitedSID.size()); //YOP sidset.size() corresponde con sidset.get(clase).size() --abajo
					if(highestSupportUntilNow != -1 && highestSupportUntilNow + remainingSeqID < sidset.get(clase).size()) {
						break;
					}
	
					alreadyVisitedSID.add(sequenceID);
					// END OF OPTIMIZATION PART 1 (IT CONTINUES A FEW LINES BELOW...)
					
					sequenceID = enlace.get(sequenceID).getIndiceTipo();
					PseudoSequenceBIDE sequence = initialDatabase.get(clase).get(sequenceID);
					
					PseudoSequenceBIDE period = sequence.getIthMaximumPeriodOfAPrefix(prefix.getItemsets(), i);
			
					// if the period is not null 
					if(period !=null){
	
						boolean hasBackwardExtension = findAllFrequentPairsForBackwardExtensionCheck(alreadyVisitedSID.size(), prefix, period, i, mapPaires, itemI, itemIm1, clase); //YOP TIENE SENTIDO PASARLE LA CLASE AQUÍ??
	
						if(hasBackwardExtension) {
							return true;
						}
						//===== OPTIMIZATION PART 2
						if((sidset.get(clase).size() - alreadyVisitedSID.size()) < minsuppAbsolute){	//YOP sidset.size() corresponde con sidset.get(clase).size()
							for(PairBIDE pair : mapPaires.values()) {
								int supportOfPair = pair.getSequenceIDs().size();
								if(supportOfPair > highestSupportUntilNow) {
									highestSupportUntilNow = supportOfPair; // +1 because it may be raised for this sequence...
								}
							}
						}
						//===== END OF OPTIMIZATION PART 2
					}
				}
			}
		}
		return false; // no backward extension, we return false
	} 
	
	/**
	 * This is the "backscan-pruning" strategy described in the BIDE+
	 * paper to avoid extending some prefixs that are guaranteed to not
	 * generate a closed pattern (see the BIDE+ paper for details).
	 * @param prefix the current prefix
	 * @param sidset IDs of sequences containing the item
	 * @parma clase indica la clase a la que pertenece el item
	 * @return boolean true if we should not extend the prefix
	 */
	private boolean checkBackScanPruning(SequentialPattern prefix, Map<String, Set<Integer>> sidset, List<Indice> enlace) {	
//		
		// See the BIDE+ paper for details about this method.
		// For the number of item occurences that can be generated with this prefix:
		System.out.println("GetItemOcurrencesTotalCount" + prefix.getItemOccurencesTotalCount());
		for(int i=0; i< prefix.getItemOccurencesTotalCount(); i++){
			
			Set<Integer> alreadyVisitedSID = new HashSet<Integer>();

			// Create a Map of pairs to count the support of items (represented by a pair) 
			// in the ith semi-maximum periods
			Map<PairBIDE, PairBIDE> mapPaires = new HashMap<PairBIDE, PairBIDE>();
			
			// SOME CODE USED BY "findAllFrequentPairsForBackwardExtensionCheck"
			Integer itemI = prefix.getIthItem(i);  // iPeriod 
			Integer itemIm1 = null;  // iPeriod -1
			if(i > 0){ 
				itemIm1 = prefix.getIthItem(i -1);	
			}
			// END NEW
			
			//para cada clase
			for(String clase : sidset.keySet()) {
				// (1) For each i, we build the list of maximum periods
				// for each sequence in the original database
				for(int sequenceID : sidset.get(clase)){
					alreadyVisitedSID.add(sequenceID);
					sequenceID = enlace.get(sequenceID).getIndiceTipo();
					System.out.println("Prefix" + prefix);
					System.out.println("ESTO-ID" + initialDatabase.get(clase).get(sequenceID));
					PseudoSequenceBIDE sequence = initialDatabase.get(clase).get(sequenceID);	//YOP Ejemplo de no poder usar ids unicos de clases 
					PseudoSequenceBIDE period = sequence.getIthSemiMaximumPeriodOfAPrefix(prefix.getItemsets(), i);
								
					if(period !=null){
	//					// we add it to the list of maximum periods
						boolean hasExtension = findAllFrequentPairsForBackwardExtensionCheck(alreadyVisitedSID.size(), prefix, period, i, mapPaires, itemI, itemIm1, clase); //YOP TIENE SENTIDO PASARLE LA CLASE??
						if(hasExtension) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}
	
	/**
	 * Method to update the support count of item in a maximum period
	 * @param prefix the current prefix
	 * @param mapPaires 
	 * @param maximum periods  a maximum period
	 * @param seqProcessedCount contador de sequences procesadas
	 * @param iPeriod
	 * @param itemI
	 * @param itemIm1
	 * @return a set of pairs indicating the support of items (note that a pair distinguish
	 *         between items in a postfix, prefix...).
	 */
	protected boolean findAllFrequentPairsForBackwardExtensionCheck(int seqProcessedCount, SequentialPattern prefix, PseudoSequenceBIDE maximumPeriod, int iPeriod, 
			Map<PairBIDE, PairBIDE> mapPaires, Integer itemI, Integer itemIm1, String clase) {

		int supportToMatch = prefix.getSupport(); // TODO Change call to prefix.getSequenceIDs().size() to prefix."getSupport()"
		int maxPeriodSize = maximumPeriod.size();
		
			// for each itemset in that period
		for(int i=0; i< maxPeriodSize; i++){
			int sizeOfItemsetAtI = maximumPeriod.getSizeOfItemsetAt(i);
			
			// NEW
			boolean sawI = false;  // sawI after current position
			boolean sawIm1 = false; // sawI-1 before current position
			// END NEW
			
			// NEW march 20 2010 : check if I is after current position in current itemset
			for(int j=0; j < sizeOfItemsetAtI; j++){
				Integer item = maximumPeriod.getItemAtInItemsetAt(j, i);
				if(item.equals(itemI)){
					sawI = true; 
				}else if (item > itemI){
					break;
				}
			}
			// END NEW
			
			for(int j=0; j < sizeOfItemsetAtI; j++){
				Integer item = maximumPeriod.getItemAtInItemsetAt(j, i);
	
				if(itemIm1 != null && item == itemIm1){
					sawIm1 = true;
				}
				
				boolean isPrefix = maximumPeriod.isCutAtRight(i);
				boolean isPostfix = maximumPeriod.isPostfix(i);
				// END NEW
																							//YOP Esto hay que cambiarlo...?
				PairBIDE paire = new PairBIDE(isPrefix, isPostfix, item);  
				if(seqProcessedCount >= minsuppAbsolute) {
					// $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
					// normal case  
					if(addPair(mapPaires, maximumPeriod.getId(), paire, supportToMatch, clase)) {
						return true;
					}
					
					// NEW: special cases
					if(sawIm1){
						PairBIDE paire2 = new PairBIDE(isPrefix, !isPostfix, item);  
						if(addPair(mapPaires,  maximumPeriod.getId(), paire2,supportToMatch, clase)) {
							return true;
						}
					}

					if(sawI ){  
						PairBIDE paire2 = new PairBIDE(!isPrefix, isPostfix, item);  
						if(addPair(mapPaires,  maximumPeriod.getId(), paire2, supportToMatch, clase)) {
							return true;
						}
					}
					// END NEW
				}else { // $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
					// normal case
					addPairWithoutCheck(mapPaires, maximumPeriod.getId(), paire, clase);
					
//					// NEW: special cases
					if(sawIm1){
						PairBIDE paire2 = new PairBIDE(isPrefix, !isPostfix, item);  
						addPairWithoutCheck(mapPaires,  maximumPeriod.getId(), paire2, clase);
					}

					if(sawI ){  
						PairBIDE paire2 = new PairBIDE(!isPrefix, isPostfix, item);  
						addPairWithoutCheck(mapPaires,  maximumPeriod.getId(), paire2, clase);
					}
					// END NEW
				}
			}
		}
		return false; 
	}

	/**
	 * Add a pair to the map of pairs and add a sequence ID to it. 
	 * If the pair is already in the map, the id is added to the old pair.
	 * @param mapPaires the map of pairs
	 * @param seqID a sequence id
	 * @param pair a pair
	 */
	private void addPairWithoutCheck(Map<PairBIDE, PairBIDE> mapPaires, Integer seqID, PairBIDE pair, String clase) {	//YOP Será así?
//		long start = System.currentTimeMillis();
		// check if the pair is already in the map
		PairBIDE oldPaire = mapPaires.get(pair);
		// if not
		if(oldPaire == null){
			// we add the new pair "paire" to the map
			mapPaires.put(pair, pair);
			if(!pair.getSequenceIDs().containsKey(clase)) {
				Set<Integer> satan = new HashSet<Integer>();
				satan.add(seqID);
				pair.getSequenceIDs().put(clase, satan);
			}
			else {
				pair.getSequenceIDs().get(clase).add(seqID);
			}
			//pair.getSequenceIDs().get(clase).add(seqID); //YOP  Será así?
		}else{
			// otherwise we use the old one
			oldPaire.getSequenceIDs().get(clase).add(seqID);	//YOP  Será así?
		}
		return;
	} 
	
	private boolean addPair(Map<PairBIDE, PairBIDE> mapPaires, Integer seqID, PairBIDE pair, int supportToMatch, String clase) {	//YOP  Será así?
//		long start = System.currentTimeMillis();
		// check if the pair is already in the map
		PairBIDE oldPaire = mapPaires.get(pair);
		// if not
		if(oldPaire == null){
			// we add the new pair "paire" to the map
			mapPaires.put(pair, pair);
		}else{
			// otherwise we use the old one
			pair = oldPaire;
		}
		
		// we add the sequence ID  to the pair
		if(!pair.getSequenceIDs().containsKey(clase)) {
			Set<Integer> satan = new HashSet<Integer>();
			satan.add(seqID);
			pair.getSequenceIDs().put(clase, satan);
		}
		else {
			pair.getSequenceIDs().get(clase).add(seqID);
		}
		//pair.getSequenceIDs().get(clase).add(seqID);		//YOP Será así?
		
		if(pair.getSequenceIDs().size() == supportToMatch) {
			return true;
		}
		
//		debugAddPairTime += System.currentTimeMillis() - start;
		return false;
	} 
	
	/**
	 * For each item, calculate the sequence id of sequences containing that item
	 * @param database the current sequence database
	 * @return Map of items to sequence IDs that contains each item
	 */
	private Map<Integer, Map<String, Set<Integer>>> findSequencesContainingItems(SequenceDatabase2 database) {
		// We use a map to store the sequence IDs where an item appear
		// Key : item   Value :  a set of sequence IDs
		//Map<Integer, Set<Integer>> mapSequenceID = new HashMap<Integer, Set<Integer>>(); // pour conserver les ID des sequences: <Id Item, Set id de sequences>
		Map<Integer, Map<String, Set<Integer>>> mapSequenceID = new HashMap<Integer, Map<String, Set<Integer>>>();
		
		//for each sequence
		//for(Sequence sequence : database.getSequences()) {
		for(Sequence sequence : database.getSequences()) {	//para cada secuencia
			// for each itemset in that sequence
			for(List<Integer> itemset : sequence.getItemsets()){
				// for each item
				for(Integer item : itemset){
					// get the set of sequence ids for that item
					//para cada clase
					for(String c: database.getClases()) {
						Set<Integer> sequenceIDs = null;
						if(mapSequenceID.get(item) != null) {
							sequenceIDs = mapSequenceID.get(item).get(c);
						} else {
							mapSequenceID.put(item, new HashMap<String,Set<Integer>>());
						}
						
						if(sequenceIDs == null) {
							// if null create a new set
							sequenceIDs = new HashSet<Integer>();
							if(!mapSequenceID.get(item).containsKey(c)) {
								mapSequenceID.get(item).put(c, sequenceIDs);
							}
						}
						// add the current sequence id to this set
						if(database.getIDClase(c).contains(sequence.getId())) {	
							sequenceIDs.add(sequence.getId());
						}
					}
				}
			}
		}
		
		for(Integer i : mapSequenceID.keySet()){
			System.out.println("The item es: " + i);
			for(String c : mapSequenceID.get(i).keySet()) {
				System.out.println("          De la clase: " + c + "   con estas Sequencias: " + mapSequenceID.get(i).get(c));
			}
		}		
		return mapSequenceID;
	}
	
	//buildProjectedContextSingleItem
	
	/**
	 * Create a projected database by pseudo-projection
	 * @param item The item to use to make the pseudo-projection
	 * @param context The current database.
	 * @param inSuffix This boolean indicates if the item "item" is part of a suffix or not.
	 * @param sidset IDs of sequences containing the item
	 * @parma clase indica la clase a la que pertenece el item
	 * @return the projected database.
	 */
	private Map<String,List<PseudoSequenceBIDE>> buildProjectedContextSingleItem(Integer item, Map<String,List<PseudoSequenceBIDE>> database, boolean inSuffix, Map<String, Set<Integer>> sidset) {
		// The projected pseudo-database
		//sequenceDatabase = new ArrayList<PseudoSequenceBIDE>();
		Map<String,List<PseudoSequenceBIDE>> sequenceDatabase = new LinkedHashMap<String,List<PseudoSequenceBIDE>>();
		setRellenarClasesCualquierMap(sequenceDatabase, database.keySet());
		
		//para cada clase
		for(String clase : sidset.keySet()) {
			System.out.println("-" + clase);
			
			//for each sequence 
			for(int sid = 0; sid < sidset.get(clase).size(); sid++) { 
				System.out.println("--" + sid);
				System.out.println(">>>"+ database.get(clase).get(sid));
				PseudoSequenceBIDE sequence = database.get(clase).get(sid);
				// for each itemset of the sequence
				for(int i =0; i< sequence.size(); i++) {  	
					int sizeOfItemsetAti = sequence.getSizeOfItemsetAt(i);
					// find the position of the item used for the projection in this itemset if it appears
	 				int index = sequence.indexOf(sizeOfItemsetAti, i, item);
					// if it does appear and it is in a postfix/suffix if the item is in a postfix/suffix
					if(index != -1 && sequence.isPostfix(i) == inSuffix){
						// if this is not the last item of the itemset
						if(index != sizeOfItemsetAti-1){ 
							// create a new pseudo sequence
							//sequenceDatabase.add(new PseudoSequenceBIDE(sequence, i, index+1));
							sequenceDatabase.get(clase).add(new PseudoSequenceBIDE(sequence, i, index+1));
						}else if (i != sequence.size()-1){// if this is not the last itemset of the sequence			 
							// create a new pseudo sequence
							// if the size of this pseudo sequence is greater than 0
							// add it to the projected database.
							//sequenceDatabase.add(new PseudoSequenceBIDE(sequence, i+1, 0)); 
							sequenceDatabase.get(clase).add(new PseudoSequenceBIDE(sequence, i+1, 0));
						}	
					}
				}
			}
		}
		return sequenceDatabase; // return the projected database
	}
	
	/**
	 * Create a projected database by pseudo-projection
	 * @param item The item to use to make the pseudo-projection
	 * @param context The current database.
	 * @param inSuffix This boolean indicates if the item "item" is part of a suffix or not.
	 * @param sidset IDs of sequences containing the item ahora es un map String,sidset
	 * @return the projected database.
	 */
	private Map<String, List<PseudoSequenceBIDE>> buildProjectedDatabase(Integer item, Map<String, List<PseudoSequenceBIDE>> database, boolean inSuffix, Map<String,Set<Integer>> sidset) {
		// The projected pseudo-database
		Map<String, List<PseudoSequenceBIDE>> sequenceDatabase = new  HashMap<String, List<PseudoSequenceBIDE>>();
		setRellenarClasesCualquierMap(sequenceDatabase, database.keySet());
		
		//para cada clase
		for(String clase : database.keySet()) {
			// for each sequence 
			for(PseudoSequenceBIDE sequence : database.get(clase)) { 			//retocado
				
				//si esta condición se cumple no ejecuta el for de abajo, sino que pasa a la siguiente iteración del for de arriba
				if(sidset.get(clase).contains(sequence.getId()) == false){		
					continue;
				}
				
				// for each item of the sequence
				for(int i =0; i< sequence.size(); i++) {
					
					int sizeOfItemsetAti = sequence.getSizeOfItemsetAt(i);
					
					// check if the itemset contains the item that we use for the projection
					int index = sequence.indexOf(sizeOfItemsetAti, i , item);
					// if it does not, and the current item is part of a suffix if inSuffix is true
					//   and vice-versa
					if(index != -1 && sequence.isPostfix(i) == inSuffix){
						if(index != sizeOfItemsetAti - 1){ // if this is not the last item of the itemset
							// create a new pseudo sequence
								// add it to the projected database.
								sequenceDatabase.get(clase).add(new PseudoSequenceBIDE(sequence, i, index+1));
						}else if ((i != sequence.size()-1)){// if this is not the last itemset of the sequence			 
							// create a new pseudo sequence
							// add it to the projected database.
							sequenceDatabase.get(clase).add(new PseudoSequenceBIDE( sequence, i+1, 0));
						}	
					}
				}
			}
		}
		return sequenceDatabase; // return the projected database
	}
	
	/**
	 * Method to recursively grow a given sequential pattern.
	 * @param prefix  the current sequential pattern that we want to try to grow
	 * @param database the current projected sequence database
	 * @throws IOException exception if there is an error writing to the output file
	 */
	private int recursion(SequentialPattern prefix, Map<String,List<PseudoSequenceBIDE>> contexte, List<Indice> enlace) throws IOException {	
		// find frequent items of size 1 in the current projected database.
		Set<PairBIDE> pairs = findAllFrequentPairs(contexte);		//YOP SOCORRO!! 	//YOP Esto hay que cambiarlo
		
		// we will keep tract of the maximum support of patterns
		// that can be found with this prefix, to check
		// for forward extension when this method returns.
		int maxSupport = 0;
		
		// For each pair found (a pair is an item with a boolean indicating if it
		// appears in an itemset that is cut (a postfix) or not, and the sequence IDs
		// where it appears in the projected database).
		for(PairBIDE pair : pairs){
			// if the item is frequent.
			if(pair.getCount() >= minsuppAbsolute){
				// create the new postfix by appending this item to the prefix
				SequentialPattern newPrefix;
				// if the item is part of a postfix
				if(pair.isPostfix()){ 
					// we append it to the last itemset of the prefix
					newPrefix = appendItemToPrefixOfSequence(prefix, pair.getItem()); // is =<is, (deltaT,i)>
				}else{ // else, we append it as a new itemset to the sequence
					newPrefix = appendItemToSequence(prefix, pair.getItem());
				}
				// build the projected database with this item
//				long start = System.currentTimeMillis();
				Map<String, List<PseudoSequenceBIDE>> projectedContext = buildProjectedDatabase(pair.getItem(), contexte, pair.isPostfix(), pair.getSequenceIDs());
//				debugProjectDBTime += System.currentTimeMillis() - start;
				
				// create new prefix
				newPrefix.setSequenceIDs(pair.getSequenceIDs()); 

				// variable to keep track of the maximum support of extension 
				// with this item and this prefix
				if(projectedContext.size() >= minsuppAbsolute) {
					
					int maxSupportOfSuccessors = 0;
					
					if(!checkBackScanPruning(newPrefix, pair.getSequenceIDs(), enlace)) {
						 maxSupportOfSuccessors = recursion(newPrefix, projectedContext, enlace); // recursion
					}
					
					// check the forward extension for the prefix
					// if no forward extension
					if(newPrefix.getSupport() != maxSupportOfSuccessors){  // TODO Change call to prefix.getSequenceIDs().size() to prefix."getSupport()"
						//  if there is no backward extension
						if(!checkBackwardExtension(newPrefix, pair.getSequenceIDs(), enlace)){
							// save the pattern
							savePattern(newPrefix);
						}
					}
				}else {
					if(!checkBackwardExtension(newPrefix, pair.getSequenceIDs(), enlace)){
						// save the pattern
						savePattern(newPrefix);
					}
				}
				// record the largest support of patterns found starting
				// with this prefix until now
				if(newPrefix.getAbsoluteSupport() > maxSupport){
					maxSupport = newPrefix.getAbsoluteSupport();
				}
			}
		}
		return maxSupport; // return the maximum support generated by extension of the prefix
	}
	
	/**
	 * Method to find all frequent items in a projected sequence database
	 * @param sequences  map of sequences
	 * @return A list of pairs, where a pair is an item with (1) booleans indicating if it
	 *         is in an itemset that is "cut" at left or right (prefix or postfix)
	 *         and (2) the sequence IDs where it occurs.
	 *         Ahora devuelve un map clase, set<PairBIDE>
	 */
	protected Set<PairBIDE> findAllFrequentPairs(Map<String,List<PseudoSequenceBIDE>> sequences) {
		// We use a Map the store the pairs.
		Map<PairBIDE, PairBIDE> mapPairs = new HashMap<PairBIDE, PairBIDE>();
		//yop para las clases
		for(String clase : sequences.keySet()) {
			// for each sequence
			for(PseudoSequenceBIDE sequence : sequences.get(clase)){
				// for each itemset
				for(int i=0; i< sequence.size(); i++){
					// for each item
					for(int j=0; j < sequence.getSizeOfItemsetAt(i); j++){
						Integer item = sequence.getItemAtInItemsetAt(j, i);
						// create the pair corresponding to this item
						PairBIDE pair = new PairBIDE(sequence.isCutAtRight(i), sequence.isPostfix(i), item);  
						// register this sequenceID for that pair.
						addPairWithoutCheck(mapPairs, sequence.getId(), pair, clase);		//YOP Será así??
					}
				}
			}
		}
		// check the memory usage
		MemoryLogger.getInstance().checkMemory();
		return mapPairs.keySet();
	}

	/**
	 *  This method creates a copy of the sequence and add a given item 
	 *  as a new itemset to the sequence. 
	 *  It sets the support of the sequence as the support of the item.
	 * @param prefix  the sequence
	 * @param item the item
	 * @return the new sequence
	 */
	private SequentialPattern appendItemToSequence(SequentialPattern prefix, Integer item) {
		SequentialPattern newPrefix = prefix.cloneSequence();  // isSuffix
		newPrefix.addItemset(new Itemset(item));  
		return newPrefix;
	}
	
	/**
	 *  This method creates a copy of the sequence and add a given item 
	 *  to the last itemset of the sequence. 
	 *  It sets the support of the sequence as the support of the item.
	 * @param prefix  the sequence
	 * @param item the item
	 * @return the new sequence
	 */
	private SequentialPattern appendItemToPrefixOfSequence(SequentialPattern prefix, Integer item) {
		SequentialPattern newPrefix = prefix.cloneSequence();
		// add to the last itemset
		Itemset itemset = newPrefix.get(newPrefix.size()-1);  
		itemset.addItem(item); 
		return newPrefix;
	}
	
	/**
	 * This method saves a sequential pattern to the output file or
	 * in memory, depending on if the user provided an output file path or not
	 * when he launched the algorithm
	 * @param prefix the pattern to be saved.
	 * @throws IOException exception if error while writing the output file.
	 */
	private void savePattern(SequentialPattern prefix) throws IOException {
		// increase the number of pattern found for statistics purposes
		patternCount++; 
	
		// if the result should be saved to a file
		if(writer != null){
		
			StringBuilder r = new StringBuilder("");
			for(Itemset itemset : prefix.getItemsets()){
	//			r.append('(');
				for(Integer item : itemset.getItems()){
					String string = item.toString();
					r.append(string);
					r.append(' ');
				}
				r.append("-1 ");
			}
			
			r.append(" #SUP: ");
			r.append(prefix.getSupport()); // TODO Change call to prefix.getSequenceIDs().size() to prefix."getSupport()"
			if(showSequenceIdentifiers) {
				for (String clase: prefix.getSequenceIDs().keySet()) {	//YOP revisar si va aquí!
					r.append("#Clase: ");
					r.append(clase);
					r.append(" - ");
		        	r.append(" #SID: ");
		        	for (Integer sid: prefix.getSequenceIDs().get(clase)) { // TODO Check if it is necessary to print also each class of the sequenceIDs Sets
		        		r.append(sid);
		        		r.append(" ");
		        	}
				}
			}
			writer.write(r.toString());
			writer.newLine();
		}// otherwise the result is kept into memory
		else{
			patterns.addSequence(prefix, prefix.size());
		}

	}
	
	/**
	 * Print statistics about the algorithm execution to System.out.
	 * @param size  the size of the database
	 */
	public void printStatistics(int size) {
		StringBuilder r = new StringBuilder(200);
		r.append("=============  Algorithm BIDE2 - STATISTICS =============\n Total time ~ ");
		r.append(endTime - startTime);
		r.append(" ms\n");
		r.append(" Closed sequential pattern count : ");
		r.append(patternCount);
		r.append('\n');
		r.append(" Max memory (mb):");
		r.append(MemoryLogger.getInstance().getMaxMemory());
		r.append('\n');
		r.append("===================================================\n");
		System.out.println(r.toString());
	}

	/**
	 * Set that the sequence identifiers should be shown (true) or not (false) for each
	 * pattern found
	 * @param showSequenceIdentifiers true or false
	 */
	public void setShowSequenceIdentifiers(boolean showSequenceIdentifiers) {
		this.showSequenceIdentifiers = showSequenceIdentifiers;
	}
	
	/** -------------------------------------------------yop
	 * Rellena la lista de indice con los valores de los indices por clases
	 **/
	private List<Indice> correspondenciaEntreIndices(SequenceDatabase2 database){
		List<Indice> lista = new ArrayList<Indice>();
		Indice ind;
		for(String c: database.getClases()) {
			if(lista.isEmpty()) {
				ind = new Indice(c, 0); 
				lista.add(ind);
			}
			else {
				int aux = lista.size();
				aux--;
				boolean salir = false;
				while((aux > -1) && (salir != true)) {
					if(lista.get(aux).getTipo().contentEquals(c)) {
						ind = new Indice(c,lista.get(aux).getIndiceTipo()+1);
						System.out.println("EN ALGUN MOMENTO ENTRA AQUÍ!!!");
						lista.add(ind);
						salir = true;
					}
					else {
						if(aux == 0) {
							ind = new Indice(c,0);
							lista.add(ind);
							salir = true;
						}
						else {
							aux--;
						}
					}
				}	
			}
		}
		return lista;
	}
	
	/** -------------------------------------------------yop */
	private void listRellenarClasesCualquierMap(Map<String,List<PseudoSequenceBIDE>> mapRellenar, List<String> listaClases) {
		for(String c : listaClases) {
			mapRellenar.put(c, new LinkedList<PseudoSequenceBIDE>());
		}
	}
	
	/** -------------------------------------------------yop */
	private void setRellenarClasesCualquierMap(Map<String,List<PseudoSequenceBIDE>> mapRellenar, Set<String> listaClases) {
		for(String c : listaClases) {
			mapRellenar.put(c, new LinkedList<PseudoSequenceBIDE>());
		}
	}
	
	/** -------------------------------------------------yop */
//	private int dUB(SequentialPattern prefix, SequenceDatabase2 database, String c){
//		int discriminative = 1;
//		discriminative = gananciaInformacion(database);
//		System.out.println("dUB--Esto?? Es s(P)??--------->" + prefix.getSequenceIDs().size());
//		
//		if(prefix.getSequenceIDs() != null){
//			System.out.print("dUB----#SID: ");
//			for(Integer id : prefix.getSequenceIDs()){
//				System.out.print(id);
//				System.out.print(' ');
//				System.out.println(prefix.getAbsoluteSupport());
//			}
//			System.out.println("");
//		}
//		prefix.getAbsoluteSupport();
//		//discriminative = Math.max(gananciaInformacion(,0),gananciaInformacion(0,));
//		return discriminative;
//	}
//	
//	/** -------------------------------------------------yop */
//	private int d(SequentialPatterns patterns, String c) {
//		int puntuacion = 0;
//		//puntuacion = Math.max();
//		return puntuacion;
//	}
//	
//	/** -------------------------------------------------yop */	
//	private int gananciaInformacion(SequenceDatabase2 database) {
//		int aux = 0;
//		int n = database.size();
//		System.out.println(" n: " + n);
//		return aux;
//	}
//	
//	/** -------------------------------------------------yop */
//	private double entropia(double p){
//		double valor = 0;
//		if (p > 0) 
//			valor = p * (Math.log(p) / Math.log(2));	/** --> es necesario hacer un cambio de base */
//		return valor; 
//	}
	
}
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
import java.util.Iterator;
import java.util.TreeSet;

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
	 * minimal threshold for discriminative score of a pattern 
	 * for each class  
	 **/
	private double dt = 0;
	
	/**--------------------------------------------------------------------yop
	 * cantidad de clases por tipo 
	 **/
	private Map<String, Integer> elementosC = new HashMap<String,Integer>();
	
	/**--------------------------------------------------------------------yop
	 * lista de clases diferentes
	 **/
	private List<String> diferentesClases = new ArrayList<String>();
	
	/**--------------------------------------------------------------------yop
	 * Lista ordenada que guarda los patrones antes de ser definitivos
	 **/
	//private List<SequentialPattern> F = new ArrayList<SequentialPattern>();
	private TreeSet<SequentialPattern> F = new TreeSet<SequentialPattern>();
	
	/**--------------------------------------------------------------------yop
	 * número de patrones a guardar...
	 **/
	private int kpatrones;
	
	/**--------------------------------------------------------------------yop
	 * tamanio de la database
	 **/
	private int N;
	
	private double primero;
	
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
	public SequentialPatterns runAlgorithm(SequenceDatabase2 database, String outputPath, int minsup, int k ) throws IOException {

		// save minsup
		this.minsuppAbsolute = minsup;
		// reset the counter for the number of patterns found
		patternCount = 0; 
		// reset the stats about memory usage
		MemoryLogger.getInstance().reset();
		// save the start time
		startTime = System.currentTimeMillis();
		//yop
		this.kpatrones = k;
		// start the algorithm
		bide(database, outputPath);
		// save the end time
		endTime = System.currentTimeMillis();
		
		//System.out.println("******************************************Hola F");

		// Displaying the Tree set data
		Iterator<SequentialPattern> iterator = F.iterator();
		while (iterator.hasNext()) {
			SequentialPattern aux = iterator.next();
			patterns.addSequence(aux, aux.size());
		}

		//System.out.println("******************************************Adiós F");
		
		// close the output file if the result was saved to a file
		if(writer != null){
			writer.close();
		}
		return patterns;
	}
	
	/**
	 * This is the main method for the BIDE+ algorithm.
	 * @param database a sequence database
	 * @param outputFilePath fichero para almacenar el resultado
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
		Map<Integer, Map.Entry<Map<String, Set<Integer>>,Integer>> mapSequenceID = findSequencesContainingItems(database);
		

		/**--------------------------------------------------------------------yop
		 * lista que guarda las referencias de los indices generales de las secuencias y 
		 * los indices en base a la clase  
		 **/
		List<Indice> generalAEspecifico = correspondenciaEntreIndices(database);
		
		//yop dar valor a N
		N = database.size();
		
		// WE CONVERT THE DATABASE TO A PSEUDO-DATABASE, AND REMOVE
		// THE ITEMS OF SIZE 1 THAT ARE NOT FREQUENT, SO THAT THE ALGORITHM 
		// WILL NOT CONSIDER THEM ANYMORE.
		
		//yop
		for(String c : database.getClases()) {
			if(!elementosC.containsKey(c)) {
				elementosC.put(c,1);
			}
			else {
				int v = elementosC.get(c); 
				elementosC.replace(c,v+1);
			}
		}

		//Rellenamos diferentesClases
		diferentesClases = database.getDiferentesClases();
		
		//yop
		primero = 0.0;
		for(String clase : diferentesClases) {
			System.out.println("(elementosC) Esto es los que se va a usar en |c|: |" + clase + "| : " + elementosC.get(clase) );
			primero += entropia((double)elementosC.get(clase)/(double)N);	//YOP primero para dub, d
		}
		
		// we create a database
		initialDatabase = new HashMap<String,List<PseudoSequenceBIDE>>(N);
		listRellenarClasesCualquierMap(initialDatabase, diferentesClases);
		
		//para cada clase
		for(String c: diferentesClases) {
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
		for(Entry<Integer, Map.Entry<Map<String, Set<Integer>>,Integer>> sequenceIDItem : mapSequenceID.entrySet()) {
			// if the item is frequent
			Map<String, Set<Integer>> sidset = sequenceIDItem.getValue().getKey();
			
			if(sequenceIDItem.getValue().getValue() >= minsuppAbsolute) {		//yop sequenceIDItem.getValue().getValue() sería un getSupport()
				//build the projected database with this item
				Integer item = sequenceIDItem.getKey();
				Map.Entry<Map<String,List<PseudoSequenceBIDE>>,Integer> projectedContext = buildProjectedContextSingleItem(item, initialDatabase, false, sidset, generalAEspecifico, sequenceIDItem.getValue().getValue());
				
				// Create the prefix with this item
				SequentialPattern prefix = new SequentialPattern();  
				prefix.addItemset(new Itemset(item));
				// set the sequence IDS of this prefix
				prefix.setSequenceIDs(sidset);
	
				//yop
				//YOP Paso1
				if (dUB(prefix) < dt)	// Base Original??
					continue;

				
				// variable to store the largest support of patterns - variable para guardar el mayor soporte de los patrones
				// that will be found starting with this prefix
				if(projectedContext.getValue() >= minsuppAbsolute) {
					int successorSupport = 0;
					
					if(!checkBackScanPruning(prefix, sidset, generalAEspecifico)) {
						successorSupport =  recursion(prefix, projectedContext, generalAEspecifico); // recursion;
					}
					
					// Finally, because this prefix has support > minsup
					// and passed the backscan pruning,
					// we check if it has no sucessor with support >= minsup
					// (a forward extension)
					// IF no forward extension
					
					prefix.setIG(d(prefix));
					
					//if(d(prefix) >= dt) {
					if(prefix.getIG() >= dt) {	//YOP Paso2 Base Original??
						if(successorSupport != sequenceIDItem.getValue().getValue()){    // ######### MODIFICATION #### 	-yop sequenceIDItem.getValue().getValue() seria el getSupport()
							// IF there is also no backward extension
							if(!checkBackwardExtension(prefix, sidset, generalAEspecifico, sequenceIDItem.getValue().getValue())){ 
								// the pattern is closed and we save it
								//savePattern(prefix);
								F.add(prefix);
								if (F.size() > kpatrones) {		//YOP PASO REPE
								//	System.out.println("Esto borrará el más peque...: " + F.first()+ " IG:" + F.first().getIG());
									F.remove(F.first());
									dt = F.first().getIG();
								}
							}
						}
					}
				}else {
					if(!checkBackwardExtension(prefix, sidset, generalAEspecifico, sequenceIDItem.getValue().getValue())){ 
						// the pattern is closed and we save it
						prefix.setIG(d(prefix));
						//savePattern(prefix);
						F.add(prefix);
						if (F.size() > kpatrones) {		//YOP PASO REPE
						//	System.out.println("Esto borrará el más peque...: " + F.first()+ " IG:" + F.first().getIG());
							F.remove(F.first());
							dt = F.first().getIG();
						}
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
	 * @param generalAEspecifico lista que pasa de indices generales a especificos de la clase
	 * @param sup support del sidset
	 * @return boolean true, if there is a backward extension
	 */
	private boolean checkBackwardExtension(SequentialPattern prefix, Map<String,Set<Integer>> sidset, List<Indice> generalAEspecifico, Integer sup) {	
//		System.out.println("======" + prefix);

		int totalOccurenceCount = prefix.getItemOccurencesTotalCount();
		// For the ith item of the prefix
		for(int i=0; i< totalOccurenceCount; i++){
			
			Set<Integer> alreadyVisitedSID = new HashSet<Integer>();
			
			// SOME CODE USED BY "findAllFrequentPairsForBackwardExtensionCheck"
			Integer itemI = prefix.getIthItem(i);  // iPeriod 
			Integer itemIm1 = null;  // iPeriod -1
			if(i > 0) { 
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
					int remainingSeqID = (sup - alreadyVisitedSID.size()); //YOP sidset.size() sidset.get(clase).size()
					if(highestSupportUntilNow != -1 && highestSupportUntilNow + remainingSeqID < sup) {
						break;
					}
	
					alreadyVisitedSID.add(sequenceID);
					// END OF OPTIMIZATION PART 1 (IT CONTINUES A FEW LINES BELOW...)
					
					sequenceID = generalAEspecifico.get(sequenceID).getIndiceTipo();
					PseudoSequenceBIDE sequence = initialDatabase.get(clase).get(sequenceID);
					
					PseudoSequenceBIDE period = sequence.getIthMaximumPeriodOfAPrefix(prefix.getItemsets(), i);
			
					// if the period is not null 
					if(period !=null){
	
						boolean hasBackwardExtension = findAllFrequentPairsForBackwardExtensionCheck(alreadyVisitedSID.size(), prefix, period, i, mapPaires, itemI, itemIm1, clase);
	
						if(hasBackwardExtension) {
							return true;
						}
						//===== OPTIMIZATION PART 2
						if((sup - alreadyVisitedSID.size()) < minsuppAbsolute){
							for(PairBIDE pair : mapPaires.values()) {
								int supportOfPair = pair.getSupport();	
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
	 * @param generalAEspecifico lista que pasa de indices generales a especificos de la clase
	 * @return boolean true if we should not extend the prefix
	 */
	private boolean checkBackScanPruning(SequentialPattern prefix, Map<String, Set<Integer>> sidset, List<Indice> generalAEspecifico) {	
//		
		// See the BIDE+ paper for details about this method.
		// For the number of item occurences that can be generated with this prefix:
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
					sequenceID = generalAEspecifico.get(sequenceID).getIndiceTipo();
					PseudoSequenceBIDE sequence = initialDatabase.get(clase).get(sequenceID);	//YOP Ejemplo de no poder usar ids unicos de clases 
					PseudoSequenceBIDE period = sequence.getIthSemiMaximumPeriodOfAPrefix(prefix.getItemsets(), i);
								
					if(period !=null){
	//					// we add it to the list of maximum periods
						boolean hasExtension = findAllFrequentPairsForBackwardExtensionCheck(alreadyVisitedSID.size(), prefix, period, i, mapPaires, itemI, itemIm1, clase);
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
	 * @param seqProcessedCount contador de sequences procesadas
	 * @param prefix the current prefix
	 * @param maximum periods  a maximum period
	 * @param iPeriod
	 * @param mapPaires 
	 * @param itemI
	 * @param itemIm1
	 * @param clase clase a la que pertenece el prefix
	 * @return a set of pairs indicating the support of items (note that a pair distinguish
	 *         between items in a postfix, prefix...).
	 */
	protected boolean findAllFrequentPairsForBackwardExtensionCheck(int seqProcessedCount, SequentialPattern prefix, PseudoSequenceBIDE maximumPeriod, int iPeriod, 
			Map<PairBIDE, PairBIDE> mapPaires, Integer itemI, Integer itemIm1, String clase) {

		int supportToMatch = prefix.getAbsoluteSupport(); // Changed call to prefix.getSequenceIDs().size() to prefix."getSupport()"
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
	 * @param clase
	 */
	private void addPairWithoutCheck(Map<PairBIDE, PairBIDE> mapPaires, Integer seqID, PairBIDE pair, String clase) {
//		long start = System.currentTimeMillis();
		// check if the pair is already in the map
		PairBIDE oldPaire = mapPaires.get(pair);
		// if not
		if(oldPaire == null){
			// we add the new pair "paire" to the map
			mapPaires.put(pair, pair);
			if(!pair.containsClase(clase)) {
				pair.setSequenceIDs(clase, seqID);
			}
			else {
				//pair.getSequenceIDs().get(clase).add(seqID);
				pair.addUnaSequenceIDs(clase,seqID);
			}
		}else{
			// otherwise we use the old one
			oldPaire.addUnaSequenceIDs(clase, seqID);
			//oldPaire.getSequenceIDs().get(clase).add(seqID);
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
		if(!pair.containsClase(clase)) {
			pair.setSequenceIDs(clase, seqID);
		}
		else {
			pair.addUnaSequenceIDs(clase, seqID);
		}
		if(pair.getSupport() == supportToMatch)  {
		//if(pair.getSequenceIDs().size() == supportToMatch) {
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
	private Map<Integer, Map.Entry<Map<String, Set<Integer>>, Integer>> findSequencesContainingItems(SequenceDatabase2 database) {
		// We use a map to store the sequence IDs where an item appear
		// Key : item   Value :  a set of sequence IDs
		//Map<Integer, Set<Integer>> mapSequenceID = new HashMap<Integer, Set<Integer>>(); // pour conserver les ID des sequences: <Id Item, Set id de sequences>
		Map<Integer, Map.Entry<Map<String, Set<Integer>>, Integer>> mapSequenceID = new HashMap<Integer, Map.Entry<Map<String, Set<Integer>>, Integer>>();
		
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
					//for(String c: diferentesClases) {
						Set<Integer> sequenceIDs = null;
						if(mapSequenceID.get(item) != null) {
							if(mapSequenceID.get(item).getKey() != null) {
								sequenceIDs = mapSequenceID.get(item).getKey().get(c);
							}
						} else {
							Map<String, Set<Integer>> sid = new HashMap<String, Set<Integer>>();
							mapSequenceID.put(item, new MyEntry<Map<String, Set<Integer>>, Integer>(sid,0));
						}
						
						if(sequenceIDs == null) {
							// if null create a new set
							sequenceIDs = new HashSet<Integer>();
							if(!mapSequenceID.get(item).getKey().containsKey(c)) {
								mapSequenceID.get(item).getKey().put(c, sequenceIDs);
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
		
		//rellenar supports! no me gusta!!
		int sup = 0;
		for(Integer i : mapSequenceID.keySet()) { 
			for(String c : mapSequenceID.get(i).getKey().keySet() ) {
				sup += mapSequenceID.get(i).getKey().get(c).size();
			}
			mapSequenceID.get(i).setValue(sup);
			sup = 0;
		}

		return mapSequenceID;
	}
	
	//buildProjectedContextSingleItem
	
	/**
	 * Create a projected database by pseudo-projection
	 * @param item The item to use to make the pseudo-projection
	 * @param database The current database.
	 * @param inSuffix This boolean indicates if the item "item" is part of a suffix or not.
	 * @param sidset IDs of sequences containing the item
	 * @parma clase indica la clase a la que pertenece el item
	 * @return the projected database.
	 */
	private Map.Entry<Map<String,List<PseudoSequenceBIDE>>,Integer> buildProjectedContextSingleItem(Integer item, Map<String,List<PseudoSequenceBIDE>> database, boolean inSuffix, Map<String, Set<Integer>> sidset, List<Indice> generalAEspecifico, Integer sup) {
		// The projected pseudo-database
		//sequenceDatabase = new ArrayList<PseudoSequenceBIDE>();
		Map.Entry<Map<String,List<PseudoSequenceBIDE>>,Integer> sequenceDatabase = new MyEntry<Map<String, List<PseudoSequenceBIDE>>, Integer>(new HashMap<String,List<PseudoSequenceBIDE>>(),0);
		setRellenarClasesCualquierMap(sequenceDatabase.getKey(), database.keySet());
		
		int auxSid = 0;
		
		//para cada clase
		for(String clase : sidset.keySet()) {
			//for each sequence 
			for(int sid : sidset.get(clase)) { //YOP			
				auxSid = generalAEspecifico.get(sid).getIndiceTipo(); 
				PseudoSequenceBIDE sequence = database.get(clase).get(auxSid);
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
							sequenceDatabase.getKey().get(clase).add(new PseudoSequenceBIDE(sequence, i, index+1));
						}else if (i != sequence.size()-1){// if this is not the last itemset of the sequence			 
							// create a new pseudo sequence
							// if the size of this pseudo sequence is greater than 0
							// add it to the projected database.
							//sequenceDatabase.add(new PseudoSequenceBIDE(sequence, i+1, 0)); 
							sequenceDatabase.getKey().get(clase).add(new PseudoSequenceBIDE(sequence, i+1, 0));
						}	
					}
				}
			}
		}
		
		//actualizar supports! no me gusta!!
		int auxSup = 0;
		for(String c : sequenceDatabase.getKey().keySet() ) {
			auxSup += sequenceDatabase.getKey().get(c).size();
		}
		sequenceDatabase.setValue(auxSup);
		sup = 0;		
		
		return sequenceDatabase; // return the projected database
	}
	
	/**
	 * Create a projected database by pseudo-projection
	 * @param item The item to use to make the pseudo-projection
	 * @param database The current database.
	 * @param inSuffix This boolean indicates if the item "item" is part of a suffix or not.
	 * @param sidset IDs of sequences containing the item ahora es un map String,sidset
	 * @return the projected database.
	 */
	private Map.Entry<Map<String, List<PseudoSequenceBIDE>>,Integer> buildProjectedDatabase(Integer item, Map.Entry<Map<String, List<PseudoSequenceBIDE>>,Integer> database, boolean inSuffix, Map<String,Set<Integer>> sidset) {
		// The projected pseudo-database
		Map.Entry<Map<String, List<PseudoSequenceBIDE>>,Integer> sequenceDatabase = new MyEntry<Map<String, List<PseudoSequenceBIDE>>, Integer>(new HashMap<String,List<PseudoSequenceBIDE>>(),0);
		setRellenarClasesCualquierMap(sequenceDatabase.getKey(), database.getKey().keySet());
		
		//para cada clase
		for(String clase : database.getKey().keySet()) {
			// for each sequence 
			for(PseudoSequenceBIDE sequence : database.getKey().get(clase)) { 			//retocado
				
				//si esta condición se cumple no ejecuta el for de abajo, sino que pasa a la siguiente iteración del for de arriba
				if (sidset.get(clase) != null) {
					if(sidset.get(clase).contains(sequence.getId()) == false){		
						continue;
					}	
				} else
					continue;
				
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
							sequenceDatabase.getKey().get(clase).add(new PseudoSequenceBIDE(sequence, i, index+1));
						}else if ((i != sequence.size()-1)){// if this is not the last itemset of the sequence			 
							// create a new pseudo sequence
							// add it to the projected database.
							sequenceDatabase.getKey().get(clase).add(new PseudoSequenceBIDE( sequence, i+1, 0));
						}	
					}
				}
			}
		}
		
		//rellenar supports! no me gusta!!
		int sup = 0;
		for(String c : sequenceDatabase.getKey().keySet() ) {
			sup += sequenceDatabase.getKey().get(c).size();
		}
		sequenceDatabase.setValue(sup);
		return sequenceDatabase; // return the projected database
	}
	
	/**
	 * Method to recursively grow a given sequential pattern.
	 * @param prefix  the current sequential pattern that we want to try to grow
	 * @param contexte the current projected sequence database
	 * @throws IOException exception if there is an error writing to the output file
	 * @return maxSupport
	 */
	private int recursion(SequentialPattern prefix, Map.Entry<Map<String, List<PseudoSequenceBIDE>>,Integer> contexte, List<Indice> generalAEspecifico) throws IOException {	
		// find frequent items of size 1 in the current projected database.
		Set<PairBIDE> pairs = findAllFrequentPairs(contexte);
		
		// we will keep tract of the maximum support of patterns
		// that can be found with this prefix, to check
		// for forward extension when this method returns.
		int maxSupport = 0;
		
		// For each pair found (a pair is an item with a boolean indicating if it
		// appears in an itemset that is cut (a postfix) or not, and the sequence IDs
		// where it appears in the projected database).
		for(PairBIDE pair : pairs){
			// if the item is frequent.
			if(pair.getSupport() >= minsuppAbsolute){
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
				Map.Entry<Map<String, List<PseudoSequenceBIDE>>,Integer> projectedContext = buildProjectedDatabase(pair.getItem(), contexte, pair.isPostfix(), pair.getSequenceIDs());
//				debugProjectDBTime += System.currentTimeMillis() - start;
				
				// create new prefix
				newPrefix.setSequenceIDs(pair.getSequenceIDs());
				
				//yop
				//YOP Paso1
				if (dUB(newPrefix) < dt)	// Base Original??
					continue;	//YOP RECURSION DEBE DEVOLVER VALOR!!

				
				// variable to keep track of the maximum support of extension 
				// with this item and this prefix
				if(projectedContext.getValue() >= minsuppAbsolute) {
					int maxSupportOfSuccessors = 0;
					
					if(!checkBackScanPruning(newPrefix, pair.getSequenceIDs(), generalAEspecifico)) {
						 maxSupportOfSuccessors = recursion(newPrefix, projectedContext, generalAEspecifico); // recursion
					}
					
					newPrefix.setIG(d(newPrefix));

					//if(d(newPrefix) >= dt) {
					if(newPrefix.getIG() >= dt) {	//YOP Paso2 Base Original??
						// check the forward extension for the prefix
						// if no forward extension
						if(newPrefix.getAbsoluteSupport() != maxSupportOfSuccessors){  // Changed call to prefix.getSequenceIDs().size() to prefix."getSupport()"
							//  if there is no backward extension
							if(!checkBackwardExtension(newPrefix, pair.getSequenceIDs(), generalAEspecifico, contexte.getValue())){
								// save the pattern
								//savePattern(newPrefix);
								F.add(newPrefix);
								if (F.size() > kpatrones) {		//YOP PASO REPE
								//	System.out.println("Esto borrará el más peque...: " + F.first()+ " IG:" + F.first().getIG());
									F.remove(F.first());
									dt = F.first().getIG();
								}
							}
						}
					}
				}else {
					if(!checkBackwardExtension(newPrefix, pair.getSequenceIDs(), generalAEspecifico, contexte.getValue())){
						// save the pattern
						newPrefix.setIG(d(newPrefix));
						//savePattern(newPrefix);
						F.add(newPrefix);
						if (F.size() > kpatrones) {		//YOP PASO REPE
						//	System.out.println("Esto borrará el más peque...: " + F.first()+ " IG:" + F.first().getIG());
							F.remove(F.first());
							dt = F.first().getIG();
						}
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
	protected Set<PairBIDE> findAllFrequentPairs(Map.Entry<Map<String,List<PseudoSequenceBIDE>>,Integer> sequences) {
		// We use a Map the store the pairs.
		Map<PairBIDE, PairBIDE> mapPairs = new HashMap<PairBIDE, PairBIDE>();
		//yop para las clases
		for(String clase : sequences.getKey().keySet()) {
			// for each sequence
			for(PseudoSequenceBIDE sequence : sequences.getKey().get(clase)){
				// for each itemset
				for(int i=0; i< sequence.size(); i++){
					// for each item
					for(int j=0; j < sequence.getSizeOfItemsetAt(i); j++){
						Integer item = sequence.getItemAtInItemsetAt(j, i);
						// create the pair corresponding to this item
						PairBIDE pair = new PairBIDE(sequence.isCutAtRight(i), sequence.isPostfix(i), item);  
						// register this sequenceID for that pair.
						addPairWithoutCheck(mapPairs, sequence.getId(), pair, clase);
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
		//System.out.println("--" + patternCount); //YOP CONTADOR DE PATRONES
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
			r.append(prefix.getAbsoluteSupport()); // Changed call to prefix.getSequenceIDs().size() to prefix."getSupport()"
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
	 * @param database la base de datos original
	 * @return lista con la correspondencia entre los índices
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
	
	/** -------------------------------------------------yop
	 * Rellena el map con las clases, partiendo de una lista
	 * @param mapRellenar el map que se quiere rellenar con las clases
	 * @param listaClases la lista de clases para rellenar
	 **/
	private void listRellenarClasesCualquierMap(Map<String,List<PseudoSequenceBIDE>> mapRellenar, List<String> listaClases) {
		for(String c : listaClases) {
			mapRellenar.put(c, new LinkedList<PseudoSequenceBIDE>());
		}
	}
	
	/** -------------------------------------------------yop
	 * Rellena el map con las clases, partiendo de un set
	 * @param mapRellenar el map que se quiere rellenar con las clases
	 * @param listaClases la lista de clases para rellenar
	 **/
	private void setRellenarClasesCualquierMap(Map<String,List<PseudoSequenceBIDE>> mapRellenar, Set<String> listaClases) {
		for(String c : listaClases) {
			mapRellenar.put(c, new LinkedList<PseudoSequenceBIDE>());
		}
	}
	
	/** -------------------------------------------------yop
	 *  Halla el dub (umbral superior del information gain) de un prefix
	 * @param prefix del que se quiere conocer el dub
	 * @return valor de dub
	 */
	private double dUB(SequentialPattern prefix){
		double discriminative = 0;
		double aux = 0.0;
		double segundo = 0.0;
		double tercero = 0.0;
		
		for(String clase: diferentesClases){
			//System.out.println("primero: " + primero); //YOP Hecho en la inicializacion de la clase
			for(String c : diferentesClases) {
				if(prefix.getSequenceIDs().get(c) != null){
					//Sc(P):S(P)
					segundo += entropia((double)prefix.getSequenceIDs().get(c).size()/(double)prefix.getAbsoluteSupport());	//si el if es falso segundo sería + 0
				}
				if(clase.equals(c)) {  
					if(N-prefix.getAbsoluteSupport() != 0)
						if(prefix.getSequenceIDs().get(c)==null)
							//(|c|-Sc(P)):(N-S(P))
							tercero += entropia((double)elementosC.get(c))/(double)(N-prefix.getAbsoluteSupport());
						else
							tercero += entropia((double)(elementosC.get(c)-prefix.getSequenceIDs().get(c).size())/(double)(N-prefix.getAbsoluteSupport()));
					else tercero += 0;
				}
				else {
					//(|c|:Sc(P)):(N:S(P))
					if(N-prefix.getAbsoluteSupport() != 0)
						if(prefix.getSequenceIDs().get(c) == null)
							tercero += entropia((double)elementosC.get(c))/(double)(N-prefix.getAbsoluteSupport());
						else
							tercero += entropia((double)(elementosC.get(c)-prefix.getSequenceIDs().get(c).size())/(double)(N-prefix.getAbsoluteSupport()));
					else tercero += 0;
				}
			}
			//primero - (S(P):N)*segundo -((N-S(P)):N)*tercero ----donde primero es siempre igual, segundo vale 0 pq el S(P)= Sc(P) y el tercero hay que hayarlo 
			aux = primero -((double)prefix.getAbsoluteSupport()/N) * segundo - ((double)(N-prefix.getAbsoluteSupport())/(double)N) * tercero;
			
			
			if(discriminative == 0)
				discriminative = aux;
			else 
				discriminative = Math.max(discriminative,aux);
			segundo = tercero = 0.0;
		}

		return discriminative*(-1);
	}
	
	/** -------------------------------------------------yop
	 * Halla el information gain
	 * @param prefix al que se le quiere hallar el information gain
	 * @return valor de ig
	 */	
	private double d(SequentialPattern prefix) {
		double aux = 0;
		
		double segundo = 0;
		double tercero = 0;

		for(String c : diferentesClases) {
			//|c|:N
			//primero = entropia(elementosC.get(c)/N);	//YOP hacerlo una vez en la inicializacion de la clase
			
			if(prefix.getSequenceIDs().get(c) != null){
				//Sc(P):S(P)
				segundo += entropia((double)prefix.getSequenceIDs().get(c).size()/(double)prefix.getAbsoluteSupport());	//si el if es falso segundo sería + 0
			}

			if(N-prefix.getAbsoluteSupport() != 0)
				if(prefix.getSequenceIDs().get(c) != null)
					//(|c|:Sc(P)):(N:S(P))	
					tercero += entropia((double)(elementosC.get(c)-prefix.getSequenceIDs().get(c).size())/(double)(N-prefix.getAbsoluteSupport()));
				else 
					tercero += entropia((double)(elementosC.get(c))/(double)(N-prefix.getAbsoluteSupport()));
		}
		//primero - (S(P):N)*segundo -((N-S(P)):N)*tercero
		aux = primero - ((double)prefix.getAbsoluteSupport()/N) * segundo - ((double)(N-prefix.getAbsoluteSupport())/N) * tercero;
		
		return aux*(-1);
	}
	
	/** -------------------------------------------------yop
	 * Halla la entropía de un valor
	 * @param p valor para hallar la entropía
	 * @return valor de la entropía
	 */
	private double entropia(double p){
		if(p == 0) { 
			return 0;
		}else {
			double valor = 0;
			if (p > 0) 
				valor = p * (Math.log(p) / Math.log(2));	/** --> es necesario hacer un cambio de base */
			return valor;
		}
	}
	
}
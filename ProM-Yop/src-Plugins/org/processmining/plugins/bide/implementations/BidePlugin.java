package org.processmining.plugins.bide.implementations;

import java.io.IOException;

import org.processmining.bide.algorithms.sequentialpatterns.BIDE_and_prefixspan.AlgoBIDEPlus2;
import org.processmining.bide.algorithms.sequentialpatterns.BIDE_and_prefixspan.SequentialPatterns;
import org.processmining.bide.input.sequence_database_list_integers.SequenceDatabase2;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;

//Esta anotacion indica que esta clase es un plugin
@Plugin(name = "Algoritmo BIDE-D",
        parameterLabels = { "Bide Configuration" },
        returnLabels = {  },
        returnTypes = { /*SequentialPatterns.class*/ }) //GspPetrinet.class
/**
 * @author Carmen
 */
public class BidePlugin {
  
  /*
   * The main idea of the plug-in is to expose two variants: one does the actual work and 
   * one populates the configurations.  
   * The 1st variant do the actual work, takes all parameters and the configuration,
   * and the other takes all parameters except the configuration. 
   * 
   * If a plug-in returns multiple parameters, they should be returned in an 
   * Object[14] array
   */
  @UITopiaVariant(affiliation = "Universidad de la Laguna",
                  author = "Carmen Santos García",
                  email = "alu0100537420@ull.edu.es",
                  uiLabel = UITopiaVariant.USEPLUGIN)
  //Indica que este metodo es una variante del plugin
  @PluginVariant(requiredParameterLabels = {0})
  public static String build(final UIPluginContext context,
                                   final SequenceDatabase2 sequenceDatabase) throws IOException{
    
    int minsup = 2;
    int k = 5;
    AlgoBIDEPlus2 algo = new AlgoBIDEPlus2();
    
    SequentialPatterns patterns = algo.runAlgorithm(sequenceDatabase, null, minsup, k);
//    System.out.println("Llego al final tuonta!");
//    System.out.println("Patterns:");
    //patterns.printFrequentPatterns(sequenceDatabase.size(),false);
    return "hola";
  }
}

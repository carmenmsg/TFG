package ca.pfv.spmf.algorithms.sequentialpatterns.BIDE_and_prefixspan;

/*
 * TODA LA CLASE ESTA HECHA POR MI (yop)
 * 
 * Esta clase se utilizará para saber la referencia entre el índice general en las secuencias
 * y el índice dentro de la clase.
 * 
 */
public class Indice {
	
	
	//guarda el valor de la clase del indice
	private String tipo;
	
	//guarda el indice de referencia dentro de la clase
	private int indiceTipo;
	
	/**
	* Defaults constructor
	 */
	public Indice(){}
	
	/**
	 * Constructor
	 */
	public Indice(String t, int i){
		tipo = t;
		System.out.println("La i vale: " + i);
		indiceTipo = i;
		System.out.println("La indiceTipo vale: " + indiceTipo);
	}
	
	/**
	 * Devuelve el tipo (clase)
	 * @return la clase (tipo)
	 */
	public String getTipo () {
		return tipo;
	}
	
	/**
	 * Devuelve el indice del tipo (clase)
	 * @return valor del indice en la clase  
	 */
	public int getIndiceTipo () {
		System.out.println("Sera este?? " + indiceTipo);
		return indiceTipo;
	}
	
	/**
	 * Imprime Indice completo
	 */
	public void imprimir() {
		System.out.println("Indice en el tipo "+ tipo + ": " + indiceTipo);
	}
	
}


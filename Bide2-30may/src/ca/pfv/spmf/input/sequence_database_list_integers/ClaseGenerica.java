package ca.pfv.spmf.input.sequence_database_list_integers;

public class ClaseGenerica<T> {
	T obj;
	
	public ClaseGenerica(T o) {
		obj = o;
	}
	
	public void classType() {
		System.out.println("El tipo de T es " + obj.getClass().getName());
	}
}

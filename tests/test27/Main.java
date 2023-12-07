public class Main {
    public static Main global; 
	public static Main f;
	public static void main(String[] args) {
		Main A = new Main(); //<internal 0>
        Main B = new Main(); //<internal 8>
        global = A;
		foo(A);
		foo(B);

	}

	public static void foo(Main p1){
        // Case 1
        //global = p1;
        // Case 2
        p1 = new Main();
        // Case 3 (Nothing)

	}
	
}

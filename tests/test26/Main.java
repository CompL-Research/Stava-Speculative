public class Main {
    public static Main global; 
	public static Main f;
	public static void main(String[] args) {
		Main A = new Main();
        Main B = new Main();
		A.f = new Main();
        foo(A);
        B = A.f;

	}

	public static void foo(Main p1){
        // Case 1
        //global = p1.f;
        // Case 2
        //p1 = new Main();
        // Case 3 (Nothing)

	}
	
}

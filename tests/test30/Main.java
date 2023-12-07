// Check handling of inlining case

public class Main {
	public static Node A;
	
	public static void main(String[] args) {
		Node B = new Node(); // <internal,0>
        Node C = new Node(); // <internal,8>
		B.n = new Node();    // <internal,17>
		C.n = new Node();    // <internal,28>
		foo(B.n);
        bar(B.n);
        func(C.n);
	}

	public static void foo(Node p1){
        Node D = new Node();
		bar(D);
		A = p1;
//		p1.n = D;
	}

	public static void bar(Node p2) {
		Node F = new Node(); // <internal,0>
		p2 = F;
	}

    public static void func(Node p3){
        //Node G = new Node();
        bar(p3);
    }
	
}

// Check handling of inlining case

public class Main {
	public static Node A;

	public static void main(String[] args) {
		Node B = new Node(); // <internal,0>
        Node C = new Node(); // <internal,8>
		foo(B);
		A = C;
        bar(C);
		bar(B);
        func(B);
	}

	public static void foo(Node p1){
        Node D = new Node();
		p1.n = D;
		bar(D);
	}

	public static void bar(Node p2) {
		Node F = new Node(); // <internal,0>
		p2.n = F;
	}

    public static void func(Node p3){
        Node G = new Node(); //<internal,0>
		Node H = new Node(); //<internal,8>
        p3.n = G;
		G = fb(H);
		bar(G);
		foo(H.n);  // <H.n> --> <external,31>
    }

	public static Node fb(Node p4) {
		Node I = new Node();
		p4.n = I;
		return I;
	}
	
}

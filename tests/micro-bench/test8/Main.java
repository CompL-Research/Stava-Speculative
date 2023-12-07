// Micro benchmark for checking the fields access (Checking for return case).

public class Main {
	public static Node A;
	public static void main(String[] args) {
		Node B = new Node(); // <internal,0>
		Node D = new Node(); // <internal,8>
		Node C = bar(B);
		foo(D);
	}
	// All three dependency Object <internal,0> ha all three dependency
	public static void foo(Node p1){
		Node E = new Node();
		Node D = new Node();
		Node F = new Node();
		p1.n = D;
		fb(D);
	}

	public static Node bar(Node p2) {
		Node G = new Node();
        Node I = new Node();
		G.n = new Node();
		foobar(G.n);
		return G;
	}
	public static void foobar(Node p3) {
		A = p3;
	}
	public static void fb(Node p4) { Node X = new Node(); }
}



// Micro benchmark for checking the fields access.

public class Main {
	public static Node A;
	public static void main(String[] args) {
		Node B = new Node(); // <internal,0>
		Node D = new Node(); // <internal,8>
		Node C = bar(B);
		Node E = foo(D);
	}

	public static Node foo(Node p1){
		Node D = new Node();
		D.n = new Node();
		fb(D.n);
		return D;
	}

	public static Node bar(Node p2) {
		Node G = new Node();
        G.n = new Node();
		foobar(G.n);
		return G;
	}

	public static void foobar(Node p3) {
		A = p3;
	}

	public static void fb(Node p4) {
		Node X = new Node();
	}
}


// Micro benchmark for checking the fields access (Checking for return case).

public class Main {
	public static Node A;
	public static void main(String[] args) {
		Node B = new Node(); // <internal,0>
		Node D = new Node(); // <internal,8>
		Node C = bar(B);
		foo(D);
	}

	public static void foo(Node p1){
		Node D = new Node();
		p1.n = D;
		Node F = fb2(D);
		F.fb(D.n);
	}

	public static Node bar(Node p2) {
		Node G = new Node();
        //Node I = new Node();
		G.n = new Node();
		foobar(G.n);
		return G;
	}

	public static void foobar(Node p3) {
		A = p3;
	}

	public static Node fb2(Node E) {
		if(E instanceof A)
			return new A();
		else
			return new Node();
	}
}


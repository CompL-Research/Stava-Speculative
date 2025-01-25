// Cyclic field dependency
public class Main {
	public static Node A;
	public static void main(String[] args) {
		Node B = new Node(); // <internal,0>
		Node C = new Node(); // <internal,8>
		Node D = new Node(); // <internal,16>
		Node E = new Node(); // <internal,24>
		E.n = B;
		D.n = E;
		C.n = D;
		B.n = C;
		bar(B);
		//foobar(B);
	}

	public static void bar(Node p2) {
		A = p2.n; //<external,4>
	}

	public static void foobar(Node p2) {
		// ...
	}
}


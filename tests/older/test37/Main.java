// Check handling of inlining case for two differnt context where at one place one object escapes other doesn't and
// at the other context both does not esacpe.

public class Main {
	public static Node A;

	public static void main(String[] args) {
		Node B = new Node(); // <internal,0>
        Node C = new Node(); // <internal,8>
		Node D = new Node(); // <internal,16>
		foo(B);
		bar(C, D);   // CONTEXT 1 (bar should get inlined with both <internal,0> and <internal,8>)
	}

	public static void foo(Node p1) {
		Node G = new Node(); // <internal,0>
		A = p1;			// ESCAPES
		bar(p1, G);  // CONTEXT 2  (bar should get inlined with just <internal,8>) ----> p1 escapes
	}

	public static void bar(Node p2, Node p3) {
		Node E = new Node(); // <internal,0>
		Node F = new Node(); // <internal,0>
		p2.n = E;
		p3.n = F;
	}
}

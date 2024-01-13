public class Main {
	public static Node A;
	public static void main(String[] args) {
		Node B = new Node(); // <internal,0>
		Node C = new Node(); // <interna l,8>
		Node D = new Node(); // <internal,16>
		Node E = new Node(); // <internal,24>
		C = bar(B);
		//foobar(B);
	}

	public static Node bar(Node p2) {
		return p2;
	}

	public static void foobar(Node p3) {
		fb(p3);
		
	}

		Node C = new Node(); // <internal,8>
		Node D = new Node(); // <internal,16>
		Node E = new Node(); // <internal,24>
		foo(B);
		foo2(B,C);
	}
	 public static void foo (Node p0) {
		Node G = new Node();
		foo2(p0,G);
	 }

	 public static void foo2(Node p3, Node p4) {
		bar(p3, p4);
	 }

	public static void bar (Node p1, Node p2) {
		//Case 1: Nothing Escapes (BOth Local)
//		Node o1 = new Node();
//		Node o2 = new Node();
//		o1.n = o2;

		// Case 2:Local Object Escapes (Stored in parameter)
//		Node o3 = new Node();
//		p1.n = o3;

		// Case 3: Nothing Escapes (Parameter Stored in local)
//		Node o4= new Node();
//		o4.n = p1;

		// Case 4: Both parameter
		p1.n = p2;
	}

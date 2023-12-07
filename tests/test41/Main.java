// Micro benchmark for checking all the fields access cases.
//Case 1: Parameter case
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
		foobar(B);
	}

	public static void bar(Node p2) {
		// ...
	}

	public static void foobar(Node p2) {
		// ...
	}
}

// Case 2: Arg case
//public class Main {
//	public static Node A;
//	public static void main(String[] args) {
//		Node B = new Node(); // <internal,0>
//		bar(B);
//		Node C = B.n;
//	}
//
//	public static void bar(Node p1) {
//		p1.n = new Node();
//		p1.n.n = new Node();
//		A = p1;
//	}
//}

// Case 3: Ret case
//public class Main {
//	public static Node A;
//	public static void main(String[] args) {
//		Node B = new Node(); // <internal,0>
//		Node C = bar(B);
//	}
//
//	public static Node bar(Node p1) {
//		Node D = new Node();
//		D.n = new Node();
//		D.n.n = new Node();
//		return D;
//	}
//}

// Case 4: load case
//public class Main {
//	public static Node A;
//	public static void main(String[] args) {
//		Node B = new Node(); // <internal,0>
//		Node C = new Node(); // <internal,8>
//		C = bar(B);
//		Node D = C.n;
//		Node E = D.n;
//	}
//
//	public static Node bar(Node p1) {
//		Node F = p1.n;
//		Node G = F.n;
//		return F;
//	}
//}

////Case 5: Store case
//public class Main {
//	public static Node A;
//	public static void main(String[] args) {
//		Node B = new Node(); // <internal,0>
//		Node C = new Node(); // <internal,8>
//		//B.n = new Node();
//		C = bar(B);
//		B.n = new Node();
//		B.n.n = new Node();
//	}
//
//	public static Node bar(Node p1) {
//		Node F = new Node();
//		return F;
//	}
//}
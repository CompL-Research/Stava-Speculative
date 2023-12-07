public class Main {
	public static Node A;
	public static void main(String[] args) {
		Node B = new Node(); // <internal,0>
		Node C = new Node();
		foo(B);
		B.n = C;
	}

	public static void foo(Node p1){
      A = p1.n;
	}
}


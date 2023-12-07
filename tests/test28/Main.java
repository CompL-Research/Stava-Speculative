public class Main {
	public static Node A;
	public static void main(String[] args) {
		Node B = new Node(); // <internal,0>
		Node C = new Node();
		foo(B);
		C = B.n;
	}

	public static void foo(Node p1){
		Node D= new Node();
		A = D;
		p1.n = D;
	}
}


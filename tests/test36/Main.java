// Check handling of inlining case

public class Main {
	public static Node A;

	public static void main(String[] args) {
		Node B = new Node(); // <internal,0>
        Node C = new Node(); // <internal,8>
		A = B;
		foo(B);
		foo(C);
	}

	public static void foo(Node p1) { bar(p1); }
	public static void bar(Node p2) { foobar(p2); }
	public static void foobar(Node p3) { foo(p3); }
}

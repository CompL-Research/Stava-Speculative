
public class Main {
	public static Node A;
	public static void main(String[] args) {
		Node B = new Node(); // <internal,0>
		Node D = new Node(); // <internal,8>
		foo(D);
	}
	// All three dependency Object <internal,0> ha all three dependency
	public static void foo(Node p1){
		Node E = new Node();
		Node D = new Node();
		Node F = new Node();
		p1.n = D;
		fb(D);
		foobar(D);
	}
	public static void fb(Node p4) { Node X = new Node(); }
	public static void foobar(Node p5) { A = p5; }
}

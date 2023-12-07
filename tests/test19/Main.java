class Node {
    Node n;
}

class C1 {
    Node n;
    Node f;
    public static C1 global;
    public Node test() {
		C1 A = new C1();
		Node B = new Node();
		Node C = new Node();
		Node D = new Node();
        // Case 0:
        A.func(B, C);
        bar(B);
		return new Node();
    }
    void bar(Node p1) {
        //Case 1:
        func(p1, new Node());
        //Case 2:
        //func(new Node(), p1);

    }
	public void func(Node p1, Node p2) {
        // p1.n = p2;
        this.n = p2;
        this.f = new Node();
	}
}

public class Main {
	public static void main(String[] args) {
        C1 c = new C1();
		c.test();

	}
}

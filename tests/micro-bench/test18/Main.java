
public class Main {
	public static Node A;
	public static void main(String[] args) {
		Node B = new Node(); // <internal,0>
		foo(B);
	}

	public static void foo(Node p1){
      Node C = new Node();  //o1
      A = C; //optional
      p1.n = C;    //S1
      bar(p1);
	}

	public static void bar(Node p2) {
      Node D = new Node(); //o2
      Node E = p2.n;   //o3 (dummy)
      E.n = D;
	}
}


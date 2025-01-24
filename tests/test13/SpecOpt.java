class Node {
    Node f1;
}

public class SpecOpt{
    public static void main(String[] args) {
        SpecOpt x = new SpecOpt();
        Node y = new Node();
        x.foo(y);
    }

    void foo(Node p1) {
        Node z = new Node();
        this.bar(p1, z);
    }
    void bar(Node p2, Node p3) {
        p2.f1 = p3;
    }
}

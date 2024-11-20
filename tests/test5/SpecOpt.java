// class SpecOpt
class SpecOpt {
    SpecOpt f1;
    public static void main(String args[]) {
        for(int i = 0; i <100000; i++) {
            SpecOpt o1;
            if (i%5 == 0) {
                o1 = new SpecOpt();
            } else if(i%3 == 0) {
                o1 = new Child();
            } else {
                o1 = new SecondChild();
            }
            SpecOpt o2 = new SpecOpt();
            o2.foo(o1); // Callsite 1 with only type as SpecOpt.
        }
    }
    // Method foo
    void foo(SpecOpt p1) {
        SpecOpt o2;
        for(int j = 0; j<1000; j++) {
            if(p1 instanceof Child) {
                o2 = new Child();
            } else if (p1 instanceof SecondChild){
                o2 = new SecondChild();
            } else {
                o2 = new SpecOpt();
            }
            SpecOpt temp = o2.bar(new SpecOpt());// Callsite 2 with two types: SpecOpt, Child and SecondChild.
            SpecOpt o3 = new SpecOpt().foobar(new SpecOpt());
            temp = o2.bar(new SpecOpt());
        }
    }
    // Method bar
    SpecOpt bar(SpecOpt p1) {
        SpecOpt o4 = new SpecOpt();
        p1.f1 = o4;
        return o4;
    }
    // Method foobar
    SpecOpt foobar(SpecOpt p2) {
        SpecOpt o5 = new SpecOpt();
        p2.f1 = new SpecOpt();
        return p2;
    }
}
// class Child
class Child extends SpecOpt {
    void foo(SpecOpt p2) {
        SpecOpt o1 = new SpecOpt();
        o1.bar(new SpecOpt());
    }
    SpecOpt bar(SpecOpt p3) {
        SpecOpt o2 = new SpecOpt();
        p3.f1 = o2;
        return o2;
    }
}
// class SecondChild
class SecondChild extends SpecOpt {
    public static SpecOpt global;
    void foo(SpecOpt p2) {
        SpecOpt o1 = new SpecOpt(); // Object
        o1.bar(new SpecOpt());
    }
    SpecOpt bar(SpecOpt p3) {
        SpecOpt o2 = new SpecOpt();
        global = p3;
        o2.f1 = new SpecOpt();
        return o2;
    }
}
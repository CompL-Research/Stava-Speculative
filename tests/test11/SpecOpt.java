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
            o1.foo(o2); 
        }
    }
    // Method foo
    void foo(SpecOpt p1) {
        SpecOpt o3;
        for(int j = 0; j<1000; j++) {
            o3 = new SpecOpt();
            SpecOpt o4 = new SpecOpt();
            SpecOpt o6 = o3.foobar(o4);
            o6.f1 = new SpecOpt();
        }
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
        SpecOpt o6 = new SpecOpt();
        o6.foobar(new SpecOpt());
    }
}
// class SecondChild
class SecondChild extends SpecOpt {
    public static SpecOpt global;
    void foo(SpecOpt p2) {
        SpecOpt o7 = new SpecOpt(); // Object
        o7.foobar(new SpecOpt());
    }
}

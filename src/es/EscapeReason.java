package es;

/*
 * Enumeration to specify the reason for an object to escape.
 */

public enum EscapeReason {

    /*
     * escape_argument: It shows the object is escaping because of its argument dependency. It depends on the formal parameter.
     * For eg: foo (A p1) { a = new A(); p1.f = a;} // a will have a caller,arg dependency.
     */
    escape_argument,

    /*
     * escape_parameter: It shows the object is escaping because its parameter dependency. It is passed as argument to another method.
     * For eg: foo (A p1) { foo(new A()); } // the new object  will have a parameter dependency.
     */
    escape_parameter,

    /*
     * escape_return: It shows the object is escaping because its return dependency. It is returned to another method.
     * For eg: foo (A p1) { a = new A(); return a;} // a will have a return dependency.
     */
    escape_return,

    /*
     * escape_global: It shows the object is escaping because its global dependency. It is stored to a static field.
     * For eg: foo (A p1) { a = new A(); global = a;} // a will have a global dependency.
     */
    escape_global,

    /*
     * escape_merge: It shows the object is escaping because it merged the values coming from two different places. Formal parameters for a polymorphic callsite.
     * For eg: foo1() { //o1 escapes; a.bar(o1); }  foo2(){ a.bar(o2); }  bar(p1) { }  // p1 will escape due to merge.
     */
    escape_merge

}

package es;

import ptg.ObjectNode;
import soot.SootMethod;

public class CallSite {
    public SootMethod methodName;
    public int BCI;

    public CallSite(SootMethod m, Integer BCI) {
        this.methodName = m;
        this.BCI = BCI;
    }

    @Override
    public int hashCode() {
        return 17* methodName.hashCode() + BCI;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof CallSite) {
            CallSite o = (CallSite) other;
            return this.methodName.equals(o.methodName) && this.BCI == o.BCI;
        }
        return false;
    }

    @Override
    public String toString() {
        return "<" +methodName.toString() + "," + BCI + ">";
    }
}

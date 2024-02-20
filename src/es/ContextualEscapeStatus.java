package es;

import soot.SootMethod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class ContextualEscapeStatus {
    public HashMap<CallSite, EscapeState> cescapestat;

    @Override
    public String toString() {
        return "<" +cescapestat.keySet().toString() + "," + cescapestat.values().toString() + ">";
    }

    public ContextualEscapeStatus() {

    }

    public ContextualEscapeStatus(CallSite c , EscapeState es) {
        cescapestat = new HashMap<>();
        cescapestat.put(c,es);
    }

//    public ContextualEscapeStatus(SootMethod m , EscapeStatus es) {
//        cescapestat = new HashMap<>();
//        cescapestat.put(m,es);
//    }

    public boolean doesEscape(CallSite c) {
        if(this.cescapestat.containsKey(c)) {
            if(this.cescapestat.get(c) instanceof Escape)
                return true;
        }
        return false;
    }
    public boolean doesnotEscape(CallSite c) {
        if(this.cescapestat.containsKey(c)) {
            if(this.cescapestat.get(c) instanceof NoEscape)
                return true;
        }
        return false;
    }

    public boolean isCallerOnly() {
        boolean _ret = true;
        Iterator<EscapeState> it = cescapestat.values().iterator();
        while (it.hasNext()) {
            EscapeState e = it.next();
            if (e instanceof Escape || e instanceof NoEscape || (e instanceof ConditionalValue && (((ConditionalValue) e).getMethod() != null))) {
                _ret = false;
                break;
            }
        }
        return _ret;
    }

    public void setEscape() {

    }
}
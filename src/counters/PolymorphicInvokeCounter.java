package counters;
import es.CallSite;
import ptg.ObjectNode;

import java.util.HashMap;
import java.util.List;

public class PolymorphicInvokeCounter {
    HashMap<CallSite, List<ObjectNode>> polymorphicInvokes = new HashMap<>();

}
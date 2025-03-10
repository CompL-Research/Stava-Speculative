package branch;

import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

public class ListStringKey {
    public final List<Integer> list;
    public final String str;

    public ListStringKey(List<Integer> list, String str) {
        this.list = new ArrayList<>(list); // Ensure immutability
        this.str = str;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ListStringKey)) return false;
        ListStringKey other = (ListStringKey) o;
        return list.equals(other.list) && str.equals(other.str);
    }

    @Override
    public int hashCode() {
        return Objects.hash(list, str);
    }
}
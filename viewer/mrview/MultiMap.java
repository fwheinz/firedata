package mrview;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Florian Heinz <fh@sysv.de>
 */
public class MultiMap<K, V> {

    private Map<K, HashSet<V>> map = new HashMap();

    public void put(K key, V value) {
        check();
        HashSet<V> set = map.get(key);
        if (set == null) {
            set = new HashSet();
            map.put(key, set);
        }
        set.add(value);
        check();
    }

    public HashSet<V> get(K key) {
        return map.get(key);
    }

    public Set<K> getKeys() {
        return map.keySet();
    }

    public boolean empty() {
        return map.isEmpty();
    }

    public boolean remove(Seg s) {
        check();
        Set<V> s1 = map.get(s.s);
        Set<V> s2 = map.get(s.e);
        boolean found = false;
        if (s1 != null) {
            found = s1.remove(s);
            if (s1.isEmpty()) {
                map.remove(s.s);
            }
        }
        check();

        if (s2 != null) {
            found = found || s2.remove(s);
            if (s2.isEmpty()) {
                map.remove(s.e);
            }
        }
        check();

        return found;
    }

    public V getSomeValue() {
        if (map.isEmpty())
            return null;
        Set<V> set = map.values().iterator().next();
        if (set.isEmpty()) {
            System.out.println("ERROR: got an empty set! ");
            return null;
        }
        return map.values().iterator().next().iterator().next();
    }
    
    public void check() {
        for (Set<V> s : map.values()) {
            if (s.isEmpty()) {
                throw new RuntimeException("Empty check failed!!"+System.identityHashCode(s));
            }
        }
    }
}

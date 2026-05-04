package graph_algorithms;

import java.util.*;


/**
 * @param <V>   {@inheritDoc}
 * @param <Key> {@inheritDoc}
 */
public class BinaryMinHeapImpl<Key extends Comparable<Key>, V> implements BinaryMinHeap<Key, V> {
    /**
     * {@inheritDoc}
     */


    private int num = 0;
    ArrayList<Entry<Key,V>> stuff = new ArrayList<>();
    HashMap<V, Integer> vals = new HashMap<>();

    @Override
    public int size() {
        return num;
    }

    @Override
    public boolean isEmpty() {
        return num == 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsValue(V value) {
        return vals.containsKey(value);
    }

    private void minHeapify(int index) {
        int left = 2 * index + 1;
        int right = (2 * index) + 2;
        int largest;
        if (left < stuff.size() &&
                stuff.get(left).key.compareTo(stuff.get(index).key) < 0) {
            largest = left;
        } else {
            largest = index;
        }
        if (right < stuff.size() &&
                stuff.get(right).key.compareTo(stuff.get(largest).key) < 0) {
            largest = right;
        }
        if (largest != index) {
            Entry<Key, V> hold = stuff.get(largest);
            V v1 = stuff.get(index).value;
            V v2 = stuff.get(largest).value;
            vals.put(v1, largest); //check
            vals.put(v2, index); //check
            stuff.set(largest, stuff.get(index));
            stuff.set(index, hold);
            minHeapify(largest);
        }
    }

    /**
     * {@inheritDoc}
     */


    @Override
    public void add(Key key, V value) {
        if (key == null) {
            throw new IllegalArgumentException("Key is Null");
        }

        if (containsValue(value)) {
            throw new IllegalArgumentException("Value Already in Heap");
        }

        num = num + 1;
        Entry<Key,V> create = new Entry(key, value);
        vals.put(value,num - 1);
        stuff.add(create);
        moveUp(num - 1);
    }

    private void moveUp(int index) {
        int parent = (index - 1) / 2;
        while (index > 0 && stuff.get(parent).key.compareTo(stuff.get(index).key) > 0) {
            Entry<Key,V> hold = stuff.get(index);
            V p1 = stuff.get(parent).value;
            V p2 = stuff.get(index).value;
            stuff.set(index, stuff.get(parent));
            stuff.set(parent, hold);
            vals.put(p1,index);
            vals.put(p2,parent);
            index = parent;
            parent = (parent - 1) / 2;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void decreaseKey(V value, Key newKey) {
        if (!containsValue(value)) {
            throw new NoSuchElementException("Value not in Heap");
        }

        int ind = vals.get(value);
        Key old = stuff.get(ind).key;

        if (newKey == null || newKey.compareTo(old) > 0) {
            throw new IllegalArgumentException("Bad Key Selection");
        }

        Entry<Key,V> change = new Entry(newKey, value);
        stuff.set(ind,change);
        int parent = (ind - 1) / 2;
        while (ind > 0 && stuff.get(parent).key.compareTo(stuff.get(ind).key) > 0) {
            Entry<Key,V> hold = stuff.get(ind);
            V p1 = stuff.get(parent).value;
            V p2 = stuff.get(ind).value;
            stuff.set(ind, stuff.get(parent));
            stuff.set(parent, hold);
            vals.put(p1,ind);
            vals.put(p2,parent);
            ind = parent;
            parent = (parent - 1) / 2;
        }


    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Entry<Key, V> peek() {
        if (num < 1) {
            throw new NoSuchElementException("Heap is Empty");
        }
        return stuff.get(0);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Entry<Key, V> extractMin() {
        if (num < 1) {
            throw new NoSuchElementException("Heap is Empty");
        }
        Entry<Key,V> min = stuff.get(0);

        V step = min.value;
        V move = stuff.get(num - 1).value;

        stuff.set(0, stuff.get(num - 1));
        stuff.remove(num - 1);
        vals.remove(step);
        num = num - 1;

        if (num > 0) {
            vals.put(move,0);
            minHeapify(0);
        }
        return min;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<V> values() {
        Set<V> out = new HashSet<V>();
        for (int i = 0; i < num; i++) {
            V val = stuff.get(i).value;
            out.add(val);
        }
        return out;
    }
}
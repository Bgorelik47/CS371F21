package framework;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

// All reducers also have concurrent access to a kv store which maps
// a key to all the value associated to the key, for instace
// if reducer_i has fetched ("foo", 1) ("bar",1) ("foo",1)
// ("foo",1) and ("bar",1) from partition_i,
// then after injecting them, reducer_i should have updated the KV
// store to contain ("foo", {1,1,1}) and ("bar", {1,1}). You can use a
// concurrent hashmap/tree to implement the concurrent KV store


/**Modeled after provided single-threaded SimpleHashMap (https://replit.com/join/ezbeltcxsd-junyuan2**/

public class ConcurrentKVStore<K, V> {
    int capacity;
    int size;
    double loadfactor;
    Entry<K,V>[] table;
    ReentrantLock[] tableLocks;
    //Lock miscAttributeLock;

    protected Logger LOGGER = Logger.getLogger(MyMapReduce.class.getName());
    ReentrantLock rehashLock = new ReentrantLock();
    Condition rehashDone = rehashLock.newCondition();
    Condition rehashCV = rehashLock.newCondition();
    boolean rehash = false;

    public ConcurrentKVStore() {
        this(16);
    }


    /*
    public ConcurrentKVStore() {
        this(4);
    }
     */
    public ConcurrentKVStore(int capacity) {
        this(capacity, 0.75);
    }

    public ConcurrentKVStore(int capacity, double loadfactor) {
        this.capacity = capacity;
        this.size = 0;
        this.loadfactor = loadfactor;
        this.table = (Entry<K,V>[]) new Entry[capacity];
        tableLocks = new ReentrantLock[capacity];

        for(int i = 0; i < capacity; i++){
            this.tableLocks[i] = new ReentrantLock();
        }

        //this.miscAttributeLock = new ReentrantLock();
    }
    /*
        public String show() {
            StringBuilder sb = new StringBuilder();
            int i = 0;
            for(Entry<K,V> e : table) {
                sb.append(Integer.toString(i));
                while(e != null) {
                    sb.append("\t");
                    sb.append(e.toString());
                    e = e.next;
                }
                sb.append("\n");
                i++;
            }
            return sb.toString();
        }
    */
    /*
    public V get(Object arg0) {

        K key = (K)arg0;

        int i = index(key);
        ReentrantLock lock = tableLocks[i];

        if(rehashLock.isHeldByCurrentThread() && rehash == true) {
            lock.lock();
        }
        else if(rehash == true) {
            try {
                //rehashDone.await();
                //rehashLock.wait();
                //rehashLock.lock();
                rehashLock.lock();
                rehashLock.unlock();
            } catch (Exception e) {
                e.printStackTrace();

                //rehashLock.unlock();
            }

            i = index(key);
            lock = tableLocks[i];
            lock.lock();
        } else {
            i = index(key);
            lock = tableLocks[i];
            lock.lock();
        }

        Entry<K,V> e = find(index(key), key);
        lock.unlock();
        if(e != null) return e.getValue();
        return null;
    }

     */

    public boolean isEmpty() {
        return size() == 0;
    }

    private int index(K key) {
        int ret = -1;
        ret = Math.abs(key.hashCode()) % capacity;
        return ret;

    }

    private Entry<K,V> find(int index, K key) {
        tableLocks[index].lock();//turn on lock for tableLocks[index]
        Entry<K, V> e = table[index];
        try {
            while (e != null) {
                if (e.getKey().equals(key)) break;
                e = e.next;
            }
        } catch (Exception a) {
            a.printStackTrace();
        }
        finally {
            tableLocks[index].unlock();//turn off lock for tableLocks[index]
            return e;
        }
    }

    public V put(K arg0, V arg1) {

        int i = index(arg0);
        V ret = null;
        ReentrantLock lock = tableLocks[i];

        if(rehashLock.isHeldByCurrentThread() && rehash == true) {
            lock.lock();
        }
        else if(rehash == true) {
            try {
                //rehashDone.await();
                //rehashLock.wait();
                //rehashLock.lock();
                rehashLock.lock();
                rehashLock.unlock();
            } catch (Exception e) {
                e.printStackTrace();

                //rehashLock.unlock();
            }

            i = index(arg0);
            lock = tableLocks[i];
            lock.lock();
        } else {
            i = index(arg0);
            lock = tableLocks[i];
            lock.lock();
        }
        try {
            if (table[i] == null) {
                table[i] = new Entry<K, V>(arg0, arg1);
            } else {
                Entry<K, V> e = find(i, arg0);
                if (e != null) {
                    Entry<K, V> a = new Entry<K, V>(arg0, arg1);
                    a.next = e.next;
                    e.next = a;
                    //V old_val = e.getValue();
                    //e.setValue(arg1);
                    //tableLocks[i].unlock();
                    ret = arg1;
                } else {
                    e = table[i];
                    while (e.next != null) {
                        e = e.next;
                    }
                    e.next = new Entry<K, V>(arg0, arg1);
                    ret = arg1;
                }
            }
        } catch (Exception c) {
            c.printStackTrace();
        } finally {
            lock.unlock();//turn off locks for tableLocks[i]
            //miscAttributeLock.lock();//turn on locks for main table attributes, this is unlocked
            // in rehash or in else statement
            rehashLock.lock();
            size++;
            if (size > capacity * loadfactor && rehash == false) {
                rehashLock.unlock();
                //miscAttributeLock.unlock();
                rehash();
            } else {
                rehashLock.unlock();
                //miscAttributeLock.unlock();
            }

            return arg1;
        }
    }

    private void rehash() {
        //turn on locks for main table attributes
        //lock it down
        rehashLock.lock();
        LOGGER.log(Level.INFO, Thread.currentThread().getName() + " start rehash");
        rehash = true;
        boolean allLocked = true;
        for(int i = 0; i < capacity; i++) {
            tableLocks[i].lock();
        }
        int oldCapacity = capacity;
        int oldSize = size;
        int newCapacity = capacity * 2;
        int newSize = 0;
        Entry<K,V>[] oldTable = table;
        ReentrantLock[] oldLocks = tableLocks;
        Entry<K,V>[] newTable = (Entry<K,V>[]) new Entry[newCapacity];
        ReentrantLock[] newLocks = new ReentrantLock[newCapacity];
        for(int i = 0; i < newCapacity; i++){
            newLocks[i] = new ReentrantLock();
            newLocks[i].lock();
        }
        //miscAttributeLock.lock();
        table = newTable;
        tableLocks = newLocks;
        capacity = newCapacity;
        size = newSize;
        //miscAttributeLock.unlock();

        for(int i = 0; i < oldCapacity; i++) {
            Entry<K, V> e = oldTable[i];
            Entry<K, V> mov;
            while(e != null) {
                put(e.getKey(), e.getValue());
                e = e.next;
            }
        }
        for(int i = capacity - 1; i >= 0; i--) {
            tableLocks[i].unlock();
        }

        /*
        for(int i = oldCapacity - 1; i >= 0; i--) {
            oldLocks[i].unlock();
        }
                oldLocks = null;
        oldTable = null;
         */
        rehash = false;
        //rehashDone.signalAll();
        LOGGER.log(Level.INFO, Thread.currentThread().getName() + " rehash complete");
        rehashLock.unlock();



       /*

        for(int i = 0; i < oldCapacity; i++){
            tableLocks[i].lock();
        }
        miscAttributeLock.lock();
        capacity *= 2;
        size = 0;

        Entry<K,V>[] old_table = table;
        table = (Entry<K,V>[]) new Entry[capacity];
        Lock[] oldLocks = tableLocks;
        tableLocks = new ReentrantLock[capacity];
        for(int i = 0; i < capacity; i++){
            tableLocks[i] = new ReentrantLock();
        }
        miscAttributeLock.unlock();
        for(int i = (oldCapacity - 1); i >= 0; i--){
            oldLocks[i].unlock();
        }

        //unlock it down

        for(int i = 0; i < oldCapacity; i++) {
            Entry<K, V> e = old_table[i];
            while(e != null) {
                put(e.getKey(), e.getValue());

            }
        }
        for(Entry<K,V> e : old_table) {
            while(e != null) {
                put(e.getKey(), e.getValue());
                e = e.next;
            }
        }

        */
    }

    public V remove(Object arg0) {

        K key = (K)arg0;
        int i = index(key);
        ReentrantLock lock = tableLocks[i];

        if(rehashLock.isHeldByCurrentThread() && rehash == true) {
            lock.lock();
        }
        else if(rehash == true) {
            try {
                //rehashDone.await();
                //rehashLock.wait();
                //rehashLock.lock();
                rehashLock.lock();
                rehashLock.unlock();
            } catch (Exception e) {
                e.printStackTrace();

                //rehashLock.unlock();
            }

            i = index(key);
            lock = tableLocks[i];
            lock.lock();
        } else {
            i = index(key);
            lock = tableLocks[i];
            lock.lock();
        }
        if(table[i] == null) {
            lock.unlock();
            return null;
        }
        else if(table[i].getKey().equals(key)) {
            Entry<K,V> e = table[i];
            table[i] = e.next;
            e.next = null;
            //miscAttributeLock.lock();
            rehashLock.lock();
            size--;
            rehashLock.unlock();
            //miscAttributeLock.unlock();
            lock.unlock();
            return e.getValue();
        } else {
            Entry<K,V> e_prev = table[i];
            Entry<K,V> e_curr = table[i].next;
            while(e_curr != null) {
                if(e_curr.getKey().equals(key)) {
                    e_prev.next = e_curr.next;
                    e_curr.next = null;
                    //miscAttributeLock.lock();
                    rehashLock.lock();
                    size--;
                    rehashLock.unlock();
                    //miscAttributeLock.unlock();
                    tableLocks[i].unlock();
                    return e_curr.getValue();
                }
                e_prev = e_curr;
                e_curr = e_curr.next;
            }
            lock.unlock();
            return null;
        }
    }

    public int size() {

        return this.size;
    }

    private static class Entry<K, V> {

        private K key;
        private V val;
        private Entry<K,V> next;



        public Entry(K key, V val) {
            this.key = key;
            this.val = val;
            this.next = null;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return val;
        }

        public V setValue(V value) {
            this.val = value;
            return val;
        }

        public String toString() {
            return key + "=" + val;
        }
    }
}

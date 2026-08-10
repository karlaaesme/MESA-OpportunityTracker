import java.util.Arrays;
//hash table from class
public class Entry {
    String key;
    Opportunity value;
    // replaced "DELETED" marker with flag
    boolean isDeleted = false;

    public Entry(String key, Opportunity value){
        this.key = key;
        this.value = value;
        this.isDeleted = false;
    }
}
class HashTableEntry{
    private Entry[] table;
    private double size;
    private double loadFactor;

    public HashTableEntry(int capacity){
        table = new Entry[capacity];
        size = 0;
    }

    private int hash(String key){
        if (key == null){
            return 0;
        }
        int hashCode = key.hashCode();
        return Math.abs(hashCode % table.length);
    }
    private void resize(int newSize){
        Entry[] oldTable = Arrays.copyOf(table, table.length);
        table = new Entry[newSize];
        for (Entry e : oldTable){
            if (e != null) {
                put(e.key, e.value);
            }
        }
        loadFactor = size / (double) table.length;
    }
    public void put(String key, Opportunity value){
        if(loadFactor > 0.7){
            int newSize = table.length * 2;
            resize(newSize);
        }

        int index = hash(key);
        int startIndex = index;
        while(table[index] != null){
            if(table[index].key.equals(key)){
                table[index].value = value;
                return;
            }
            index = (index + 1) % table.length; //linear probing

            if (index == startIndex){
                System.err.println("No empty slot");
                return;
            }
        }
        table[index] = new Entry(key, value);
        size++;
        loadFactor = size / (double) table.length;
    }
    public Opportunity get(String key) {
        //get hash index based key
        int index = hash(key);
        int startIndex = index;

        while (table[index] != null){
            if(table[index].isDeleted == false && table[index].key.equals(key)){
                return table[index].value;
            }
            index = (index + 1) % table.length;
            if (index == startIndex){
                return null;
            }
        }
        return null;
    }
    public void remove(String key) {
        int index = hash(key);
        int startIndex = index;

        while (table[index] != null) {
            if (table[index].key.equals(key)) {
                table[index].isDeleted = true;
                size--;
                loadFactor = size / (double) table.length;
                return;
            }
            index = (index + 1) % table.length;
            if (index == startIndex) {
                break;
            }
        }
    }
}
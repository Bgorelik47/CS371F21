package framework;
import utils.*;

class PartitionTable {

    Partition[] partitions;
    private int size;

    PartitionTable(int numMappers) {
        partitions = new Partition[numMappers];
        size = numMappers;
        for(int i = 0; i < numMappers; i++) {
            partitions[i] = new Partition();
        }
    }
    int size() {
        return size;
    }


    class Partition<KVPair> extends BoundedBuffer {
        //TODO: your codde here

        //Partition() {super(50);}
        Partition() {
            super(100);

        }

        //Notes:
        // (1) each partition works like a bounded buffer between
        // mappers and a reducer. (you can assume size = 10 or 50)
        // (2) if reducer_i wants to fetch a KV pair it can
        // only fetches from partition_i, but mapper_i can drop messages
        // into different partitions.

        public void deposit(Object key, Object value) throws InterruptedException {
            super.deposit(new MyMapReduce.KVPair(key, value));
        }

        @Override
        public KVPair fetch() throws InterruptedException {
            return (KVPair)super.fetch();
        }
    }

}

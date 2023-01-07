package framework;
import java.io.*;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WordCountTest {
    Logger LOGGER = Logger.getLogger(MyMapReduce.class.getName());
    private int[] stats ;
    private class WordCount extends MapperReducerClientAPI {
        private MapReduce myMapReduce;
        public WordCount(MapReduce myMapReduce) {
            this.myMapReduce = myMapReduce;
        }

        public void Map(Object inputSource) {
            String fileName = (String) inputSource;
            try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
                String token;
                int i = 0;
                while ((token = br.readLine()) != null) {

                    if(i % 250000 == 0 && i >= 100) {
                        LOGGER.log(Level.INFO,
                                Thread.currentThread().getName() + " emit line " + i);
                    }
                    i++;


                    myMapReduce.MREmit(token, "1"); //puts KV in buffer via partition table
                }

                br.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void Reduce(Object key, int partition_number) {
            int count = 0;
            while ((myMapReduce.MRGetNext(key, partition_number)) != null) {
                count++;
                stats[partition_number] ++;
            }
            myMapReduce.MRPostProcess((String) key, count, partition_number);
        }
    }
    @Test
    public void test1_small() throws IOException {
        WordCount wordCountInstance = new WordCount(new MyMapReduce());
        this.stats = new int[2];
        int ret = wordCountInstance.myMapReduce.MRRun
                ("C:\\Users\\betsy\\Documents\\GitHub\\CS371F21\\project3\\src\\framework\\small", wordCountInstance, 2, 2);
        assertEquals(stats[0], 4);
        assertEquals(stats[1], 5);
        //assertEquals(0, ret);
        //Code review questions:
        //q1: How many partitions does this test set up?
        //Answer: This test sets up two partitions, as that is the amount of Reducers threads that are specified.

        //q2: Based on the Partitioner function in MapperReducerClientAPI, what should be stored in partition 0
        //and what should be stored in partition 1? (hint: you can write a little main() test func in MapperReducerClientAPI)
        //Answer: Partition 0 should store: {bar, woof, fox, boss} and Partition 1 should store: {foo, foo, foo, road, road}

        //q3: What are the key value pairs stored in the concurrent KV stores by reducers when the reduce() is called for the first time? Please use the format of 
        //(key,{val_1,val_2,..val_n}).
        //Answer:
            //(bar, {1})
            //(woof, {1})
            //(fox, {1})
            //(boss, {1})
            //(foo, {1, 1, 1})
            //(road, {1, 1})

        //q4: For key "foo", how many times does MRGetNext get invoked?
        //Answer: 4 times. The first three times it returns 1, and the last time it returns NULL.
    }

    @Test
    public  void test2_large_single() throws IOException {
        WordCount wordCountInstance = new WordCount(new MyMapReduce());
        this.stats = new int[1];
        int ret = wordCountInstance.myMapReduce.MRRun
                ("res/cybersla", wordCountInstance, 1, 1);
        assertEquals(0, ret);
    }

    @Test
    public  void test2_large_multiple() throws IOException {
        WordCount wordCountInstance = new WordCount(new MyMapReduce());
        this.stats = new int[8];
        int ret = wordCountInstance.myMapReduce.MRRun
                ("res/cybersla", wordCountInstance, 8, 8);
        assertEquals(0, ret);
    }

    @Test
    public void test1_medium() throws IOException {
        WordCount wordCountInstance = new WordCount(new MyMapReduce());
        this.stats = new int[8];
        int ret = wordCountInstance.myMapReduce.MRRun
                ("res/medium", wordCountInstance, 8, 8);
        //assertEquals(stats[0], 4);
        //assertEquals(stats[1], 5);
        //assertEquals(0, ret);
    }


}

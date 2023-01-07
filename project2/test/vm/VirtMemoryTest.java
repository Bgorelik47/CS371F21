package vm;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.hamcrest.MatcherAssert;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
@FixMethodOrder(MethodSorters.NAME_ASCENDING)

public class VirtMemoryTest {
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    @Before
    public void setUpStreams() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }
    @Test
    public void test1_OutOfRange() {
        Memory m = new VirtMemory();
        m.startup();
        m.write(0xFFFFFF, Byte.parseByte("-1"));
        MatcherAssert.assertThat(0, not(errContent.toString().length()));
        byte x = m.read(0xFFFFFF);
        MatcherAssert.assertThat(0, not(errContent.toString().length()));
        //Code review q1: what is the max legit address for m.write()??
        //Answer: Since default virtual address space is 64KB, max legit address is 64KB-1byte,
        // which is 0xFFFF
        m.shutdown();
    }
    @After
    public void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }
    @Test
    public void test2_SingleWrite() {
        Memory m = new VirtMemory();
        m.startup();
        m.write(0x8000, Byte.parseByte("-1")); //write it to somewhere way beyond 16K
        m.shutdown();
        //now the disk should have persisted your write, reboot
        m = new VirtMemory();
        m.startup();
        byte data = m.read(0x8000);
        m.shutdown();
        assertEquals(Byte.parseByte("-1"), data);
    }
    @Test
    public void test3_WriteBackToSameBlock() {
        //every 32 writes triggers a write-back to disk.
        Memory m = new VirtMemory();
        m.startup();
        for(int i=0; i<32; i++) {
            m.write(i, Byte.parseByte("-1"));
        }
        m.shutdown();
        int writeCount = m.getPhyMemory().writeCountDisk();
        int readCount = m.getPhyMemory().readCountDisk();
        assertEquals(1, writeCount);
        //Code review q2: why is there only 1 disk write?
        //Answer: because we only write to disk if a page has to be evicted or there have been
        // more than 32 writes since the last write-back. In this case, we have exactly 32
        // writes, which isn't enough to trigger a batched-write-back, so the only write occurs
        // during the shutdown() function, when it calls sync_to_disk().
        assertEquals(1, readCount);
        //Code review q3: why is there only 1 disk read?
        //Answer: there is only 1 disk read because the block is only loaded into memory once, in
        // the first write. The rest of the writes are to the same block, which is already in
        // memory, so no more disk reads are necessary.
    }
    @Test
    public void test4_WriteBackToMultiBlocks() {
        //every 32 writes triggers a write-back to disk.
        Memory m = new VirtMemory();
        m.startup();
        for(int i=0; i<32; i++) {
            m.write(i*64, Byte.parseByte("-1"));
        }

        m.shutdown();
        int writeCount = m.getPhyMemory().writeCountDisk();
        int readCount = m.getPhyMemory().readCountDisk();
        assertEquals(32, writeCount);
        //Code review q4: why are there 32 disk writes?
        //Answer: there are 32 disk writes because 32 blocks have been changed, so during
        // shutdown, those 32 blocks need to be written back to the disk.
        assertEquals(32, readCount);
        //Code review q5: why are there 32 disk read?
        //Answer: there are 32 disk reads because each of the write calls writes to a different
        // block, and so 32 blocks total must be read into memory from the disk.
    }
    //the following are more realistic workloads
    static final int TEST_SIZE = 64*1024;// 64K, test on max address space!
    static byte fce(int adr) {
        return (byte) ((adr * 5 + 6) % 256 - 128);
    }
    static byte fce2(int adr) {
        return (byte) ((adr * 7 + 5) % 256 - 128);
    }
    @Test
    public void test5_End2EndForward() {
        Memory m = new VirtMemory();
        m.startup();
        boolean result = true;
        for (int i = 0; i < TEST_SIZE; i++)
            m.write(i, fce(i));
        for (int i = 0; i < TEST_SIZE; i++)
            if (m.read(i) != fce(i))
                result = false;
        assertEquals(true, result);
        m.shutdown();
        assertEquals(2048, m.getPhyMemory().writeCountDisk());
        //Code review q6: why are there 2048 disk writes?
        //Answer: each block has 64 bytes. We automatically write-back after every 32 writes.
        // That means for each block we write-back 2 times. There are 1024 blocks, so 2048 total
        // disk writes in the first for-loop. The second for-loop doesn't cause any disk writes
        // since it only reads data, it doesn't change anything, so nothing needs to be
        // written-back.

        assertEquals(2048, m.getPhyMemory().readCountDisk());
        //Code review q7: why are there 2048 disk reads?
        //Answer: there are 2048 disk reads because each block is read once in the first for-loop
        // and once in the second for-loop. There are 1024 blocks so 2048 total disk reads.


    }
    @Test
    public void test6_End2EndBackward() {
        Memory m = new VirtMemory();
        m.startup();
        boolean result = true;
        for (int i = 0; i < TEST_SIZE; i++)
            m.write(i, fce(i));
        for (int i = TEST_SIZE-1; i >= 0; i--)
            if (m.read(i) != fce(i))
                result = false;
        assertEquals(true, result);
        m.shutdown();
        assertEquals(2048, m.getPhyMemory().writeCountDisk());
        //Code review q8: why are there 2048 disk writes?
        //Answer: each block has 64 bytes. We automatically write-back after every 32 writes.
        // That means for each block we write-back 2 times. There are 1024 blocks, so 2048 total
        // disk writes in the first for-loop. The second for-loop doesn't cause any disk writes
        // since it only reads data, it doesn't change anything, so nothing needs to be
        // written-back.
        assertEquals(1792, m.getPhyMemory().readCountDisk());
        //Code review q9: why are there 1792 disk reads? Why is it different from test5?
        //Answer: there are 1792 disk reads because, when the second for-loop begins, the first 256
        // blocks it wants to read (which are at the very end of the virtual memory) are already in
        // memory, since they were loaded in at the end of the first for-loop. After it reads
        // those 256 blocks that are already in memory, then it must load the other 1792 blocks
        // from the disk to read them, resulting in 1792 disk reads.

    }
    @Test
    public void test7_End2EndMix() {
        Memory m = new VirtMemory();
        m.startup();
        boolean result = true;
        for (int i = TEST_SIZE-1; i >= 0; i--)
            m.write(i, fce(i));
        for (int posun = 0; posun < TEST_SIZE; posun += 100) {
            for (int i = 0; i < TEST_SIZE; i++) {
                int adr = (i+posun)%TEST_SIZE;
                if (m.read(adr) != fce(adr))
                    result = false;
            }
        }
        int posun_zapis=55;
        for (int i = 0; i < TEST_SIZE; i++) {
            int adr = (i+posun_zapis)%TEST_SIZE;
            m.write(adr, fce2(adr));
        }
        for (int posun = 20; posun < TEST_SIZE; posun += 100) {
            for (int i = 0; i < TEST_SIZE; i++) {
                int adr = (i+posun)%TEST_SIZE;
                if (m.read(adr) != fce2(adr))
                    result = false;
            }
        }
        assertEquals(true, result);
        m.shutdown();
    }
}

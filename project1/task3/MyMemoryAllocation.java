import java.util.Iterator;
public class MyMemoryAllocation extends MemoryAllocation {

    //Access Modifier Justification:
    // We are using protected so that the list and allocator classes can access the blocks, but,
    // assuming that whoever/whatever is instantiating the allocator and using the allocator
    // methods is not within this default package, then the user's only access to the memory
    // model (lists and blocks) will be mediated by MyMemoryAllocation.  As long as
    // MyMemoryAllocationTest is not within this default package, then it cannot access blocks or
    // lists directly.
    //Overall, we want the test/user to have access only to the methods specified in
    // MemoryAllocation abstract class. We do not want the test/user to have access to any other
    // methods or fields. This is why the methods specified in MemoryAllocation are public, and
    // nearly everything else (methods and variables) are protected.

   protected String operatingMode;
   protected UsedList usedList;
   protected FreeList freeList;
   protected int maxMemSize;
   protected Iterator<Block> usedListIterator;
   protected Iterator<Block> freeListIterator;


    
    public MyMemoryAllocation(int mem_size, String algorithm) {
        super(mem_size, algorithm); 

        freeList = new FreeList(mem_size);
        usedList = new UsedList(mem_size);
        operatingMode = algorithm;

        maxMemSize = mem_size;
        if(algorithm.equals("NF")) {
            freeListIterator = freeList.iterator(true);
           
        }
        
    }
    
    public int alloc(int size) {
        if(size <= 0 || size >= maxMemSize) {
            System.err.println("invalid alloc");
            return 0;
        }
        
        if(this.operatingMode.equals("BF")) {
            freeListIterator = freeList.iterator();
            Block current;
            int[] maxBlock = freeList.getMaxBlock();
            if(maxBlock[1] < size) {
                System.err.println("large enough block not available");
                return 0;
            }
            while(freeListIterator.hasNext()) {
                current = freeListIterator.next();
                int curSize = current.getSize();
                int curOffset = current.getOffset();

                if(curSize < size) {
                    continue;
                }

                if(curSize - size == maxBlock[1] - size) {
                    if(curOffset < maxBlock[0]) {
                        maxBlock[0] = curOffset;
                        maxBlock[1] = curSize;
                    }
                }
                else if(curSize - size < maxBlock[1] - size) {
                    maxBlock[0] = curOffset;
                    maxBlock[1] = curSize;
                }
            }
            usedList.insert(maxBlock[0], size);
           
            freeList.shrinkOrDelete(maxBlock[0], size);
           
            return maxBlock[0];//return address of allocated block


        }
        if(this.operatingMode.equals("FF")){

            freeListIterator = freeList.iterator();
            Block current;

            while(freeListIterator.hasNext()){
                current = freeListIterator.next();
               
                if(current.getSize() >= size){
                    int blockOffset = current.getOffset();
                    usedList.insert(current.getOffset(), size);
                    freeList.shrinkOrDelete(current.getOffset(), size);

                    return blockOffset;//return address of allocated block
                }
            }
           
            System.err.println("error in FF");
            return 0;
        }
        if(this.operatingMode.equals("NF")) {
            Block nfCurrent;
            int blockCount = freeList.getBlockCount();
            int numBlocksChecked = 0;
            boolean done = false;

            if(blockCount == 1) {
                nfCurrent = freeListIterator.next();
               
                int blockOffset = nfCurrent.getOffset();
                usedList.insert(nfCurrent.getOffset(), size);
                freeList.shrinkOrDelete(nfCurrent.getOffset(), size);
               
                return blockOffset;//return address of allocated block
            }
            if(freeListIterator.hasNext()) {
                do {
                    nfCurrent = freeListIterator.next();
                    numBlocksChecked++;
                    if (nfCurrent.getSize() >= size) {
                        int blockOffset = nfCurrent.getOffset();
                        usedList.insert(nfCurrent.getOffset(), size);
                        freeList.shrinkOrDelete(nfCurrent.getOffset(), size);

                        return blockOffset;//return address of allocated block
                    }
                    
                    if(blockCount == numBlocksChecked) {
                        done = true;
                    }
                } while (!done);

            }

        }
        System.err.println("fail");
        return 0; //allocation failed
    }

    public void free(int offset) {
        if(offset <= 0 || offset >= maxMemSize) {
            System.err.println("invalid offset");
            return;
        }
        usedListIterator= usedList.iterator();
        Block usedCurrent;
        
        while(usedListIterator.hasNext()){
           usedCurrent = usedListIterator.next();
          
           if(usedCurrent.getOffset() == offset){
               freeList.insert(offset, usedCurrent.getSize());
               usedList.delete(offset);
              
               return;

           }
        }
        System.err.println("invalid offset");

    }
    public int size() {
        return freeList.getTotalSize();
    }
    public int max_size() {
        return freeList.getMaxBlock()[1];
    }

    public void print() {
        System.out.print("free: ");
        freeList.print();
        System.out.print("used: ");
        usedList.print();

    }
}

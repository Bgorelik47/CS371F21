public class UsedList extends BlockList {
    protected UsedList(int sizeOfMemory) {
        head = null;
        tail = null;
        blockCount = 0;
        memSize = sizeOfMemory;
    }
}

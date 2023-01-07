public class Block {
   
    private int offset;
    private int size;
    private Block left;
    private Block right;

    protected Block(int offset, int size) {
        this.offset = offset;
        this.size = size;
        left = null;
        right = null;
    }

    protected Block(int offset, int size, Block left, Block right) {
        this.offset = offset;
        this.size = size;
        this.left = left;
        this.right = right;
    }
    protected int getSize() {
        return size;
    }
    protected void setSize(int size) {
        this.size = size;
    }
    protected int getOffset() {
        return offset;
    }
    protected void setOffset(int offset) {
        this.offset = offset;
    }


    //The left boundary (first occupied memory slot) of a block is always its offset
    //The right boundary of a block (the final memory slot it occupies) will be (size - 1) blocks after the offset
   
    protected int getRightBoundary(){
        return this.offset + this.size - 1;
    }

    protected Block getLeft() {
        return left;
    }
    protected void setLeft(Block left) { this.left = left; }
    
    protected Block getRight() {
        return right;
    }
    protected void setRight(Block right) {
        this.right = right;
    } 

    //Returns -1 if this block is a lower offset than the parameter block
    //Returns 0 if the blocks have the same offset
    //Returns 1 if this block has a greater offset than the passed block
    protected int compareTo(Block b) {
        
        if(this.offset < b.getOffset()){
            return -1;
        }
        else if (this.offset > b.getOffset()){
            return 1;
        }
        else{
            return 0;
        }
        
    }
    public String toString() {
        return "[" + offset + "," + size + "]";
    }
}

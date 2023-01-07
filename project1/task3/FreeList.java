public class FreeList extends BlockList {
    protected FreeList(int initialSize) {
        head = new Block(1, initialSize - 1);
        tail = head;
        blockCount = 1;
        memSize = initialSize;
    }

    private boolean merge(Block a, Block b) {
        Block l;
        Block r;
        if(a.getOffset() < b.getOffset()) { 
            l = a;
            r = b;
        } else {
            l = b;
            r = a;
        }
        try { 
            l.setSize(l.getSize() + r.getSize());
            l.setRight(r.getRight());
            if(r.getRight() != null) {
                r.getRight().setLeft(l);
            }
            r.setLeft(null);
            r.setRight(null);
            if(tail == r) {
                tail = l;
            }
            blockCount--;

        } catch (Exception e) {
            System.err.println("ERROR: merge failed"); 
            return false;
        }
        return true;
    }


    private boolean shrinkBy(Block b, int shrinkByVal) {
        try { 
            Block a = searchByOffset(b.getOffset());
            a.setOffset(a.getOffset()+shrinkByVal);
            a.setSize(a.getSize()-shrinkByVal);
        } catch (Exception e) {
            System.err.println("ERROR: shrink failed"); 
            return false;
        }
        return true;
    }

    @Override
    protected int insert(int offset, int size) {
        Block b = super.insertBlock(offset, size);
        if(b != null) {
            if(calculateAdjacency(b, b.getRight())) {
                merge(b, b.getRight());
            }
            if(calculateAdjacency(b, b.getLeft())) {
                merge(b, b.getLeft());
            }
            return offset;
        }
        else return 0;
    }

    @Override
    protected boolean delete(int offset) {
        Block b = super.deleteBlock(searchByOffset(offset));
        if(b != null) {
            if(calculateAdjacency(b.getLeft(), b.getRight())) {
                merge(b.getLeft(), b.getRight());
            }
            return true;
        }
        else return false;
    }


    protected boolean shrinkOrDelete(int offset, int size) {
        Block b = searchByOffset(offset);
        if(b.getSize() == size) {
            b = super.deleteBlock(b);
            if(b == null) {
                return false;
            }
            else {
                return true;
            }
        }
        else {
            return shrinkBy(b, size);
        }
    }



}

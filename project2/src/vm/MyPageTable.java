package vm;
import java.util.Iterator;

public class MyPageTable {

    private static int tableSize;
    private static int HASH_SIZE;
    private PageTableEntry[] vpnBuckets;
    private PageTableEntry[] pfnBuckets;
    private int numPTEs;
    private final double DEFAULT_LOAD_FACTOR = 0.75;

    public MyPageTable(int physMemSize) {
        tableSize = physMemSize / 64;
        HASH_SIZE = physMemSize / 64;
        vpnBuckets = new PageTableEntry[tableSize];
        pfnBuckets = new PageTableEntry[tableSize];
        numPTEs = 0;

    }


    private int hashCode(int pn) {
        int hash = (pn / HASH_SIZE);
        return hash;
    }


    protected PageTableEntry getPTEbyPFN(int pfn) {
        int index = hashCode(pfn) % tableSize;
        PageTableEntry finger = pfnBuckets[index];
        try {
            while (finger != null) {
                if (finger.pfn == pfn) {
                    return finger;
                }
                finger = finger.pfnNext;
            }
            if (finger == null) {
                throw new NoPFNException();
            }
        } catch (NoPFNException e) {
            System.err.print("there's nothing in this frame");
        }
        return null;
    }

    protected PageTableEntry getPTEbyVPN(int vpn) throws PageFaultException { //returns pfn for vpn
        try {
            int index = hashCode(vpn) % tableSize;
            PageTableEntry finger = vpnBuckets[index];
            while (finger != null) {
                if (finger.vpn == vpn) {
                    return finger;
                }
                finger = finger.vpnNext;
            }
            if (finger == null) {
                throw new PageFaultException();
            }
        } catch (PageFaultException e) {
            throw e;
        }
        return null;
    }

    private PageTableEntry addPTE(PageTableEntry pte, int tableSize) {
        int vpnKey = hashCode(pte.vpn) % tableSize;
        int pfnKey = hashCode(pte.pfn) % tableSize;
        PageTableEntry finger = vpnBuckets[vpnKey];
        pte.vpnNext = finger;
        vpnBuckets[vpnKey] = pte;
        finger = pfnBuckets[pfnKey];
        pte.pfnNext = finger;
        pfnBuckets[pfnKey] = pte;
        numPTEs++;

        return pte;
    }
    protected PageTableEntry addNewPTE(int vpn, int pfn) {
        PageTableEntry pte = new PageTableEntry(vpn, pfn);
        addPTE(pte, tableSize);
        
        //after new element is added load factor is recalculated
        double loadFactor = (1.0 * numPTEs) / tableSize;
        //if load factor > 0.75 we need to rehash
        if(loadFactor > DEFAULT_LOAD_FACTOR ){
            rehash();
        }
        return pte;
    }

    private void rehash(){
        //We'd need to rehash the vpn bucket list AND the pfn bucket list
            //Current buckets are made into temps
            PageTableEntry oldVpn[] = vpnBuckets;
            PageTableEntry oldPfn[] = pfnBuckets;

            //Old lists are made to be twice the size
            this.vpnBuckets = new PageTableEntry[2 * tableSize];
            this.pfnBuckets = new PageTableEntry[2 * tableSize];
             //numPTEs = 0;
             tableSize *= 2;

            //loop through original vpn bucket list and insert it into the new list
            for(int i = 0; i < oldVpn.length; i++){
                //head of chain at index
                PageTableEntry head = oldVpn[i];
                PageTableEntry next;
                if(head == null) {
                    continue;
                }
                else {
                    while( head != null) {
                        removePTE(head, oldVpn, oldPfn, tableSize);
                        addPTE(head, tableSize * 2);
                        head = oldVpn[i];
                    }

                }
            }

    }

    protected void removePTE(PageTableEntry pte) {
        removePTE(pte, vpnBuckets, pfnBuckets, tableSize);
    }

    private void removePTE(PageTableEntry pte, PageTableEntry[] vpnBuckets,
                      PageTableEntry[] pfnBuckets, int tableSize) {
        int vpnKey = hashCode(pte.vpn) % tableSize;
        int pfnKey = hashCode(pte.pfn) % tableSize;
        //vpn
        PageTableEntry finger = vpnBuckets[vpnKey];
        if (finger == pte) {
            vpnBuckets[vpnKey] = pte.vpnNext;
            pte.vpnNext = null;
        }
        else {
            while (finger.vpnNext != pte) {
                finger = finger.vpnNext;
            }
            finger.vpnNext = pte.vpnNext;
        }
        pte.vpnNext = null;
        //pfn
        finger = pfnBuckets[pfnKey];
        if (finger == pte) {
            pfnBuckets[pfnKey] = pte.pfnNext;
            pte.pfnNext = null;
        }
        else {
            while (finger.pfnNext != pte) {
                finger = finger.pfnNext;
            }
            finger.pfnNext = pte.pfnNext;
        }
        pte.pfnNext = null;
        numPTEs--;
    }

    protected static class PageTableEntry { //Protected so VirtMemory can reference PTEs directly
        //so sync_to_disk() can check and change if the pte is dirty and store the pte if
        // necessary without having to re-iterate through the table to find the pte by pfn (since
        // it already found the pte earlier in the function). We are not super concerned with
        // VirtMemory having access to PTEs since clients calling VirtMemory can only use what's
        // public in VirtMemory, so they won't have access to PTEs (assuming the client is not
        // a member of the vm package and can only import vm).
        int vpn;
        int pfn;
        boolean dirty;
        PageTableEntry vpnNext;
        PageTableEntry pfnNext;

        PageTableEntry(int vpn, int pfn) {
            this.vpn = vpn;
            this.pfn = pfn;
            dirty = false;
        }

    }

    protected PageTableIterator iterator() {
        return new PageTableIterator();
    }


    private class PageTableIterator implements Iterator<Integer> {
        PageTableEntry current;
        int numSeen;
        int numLeft;
        int currBucket;
        //initializes pointer to head of PageTable for iteration
        private PageTableIterator() {
            numSeen = 0;
            numLeft = numPTEs;
            currBucket = 0;
            for ( ; currBucket < pfnBuckets.length; currBucket++) {
                if (pfnBuckets[currBucket] == null) {
                    continue;
                }
                current = pfnBuckets[currBucket];
                break;
            }

        }
        private PageTableEntry getNext() {
            if(current.pfnNext == null) {
                currBucket++;
                for ( ; currBucket < pfnBuckets.length; currBucket++) {
                    if (pfnBuckets[currBucket] == null) {
                        continue;
                    }
                    PageTableEntry next = pfnBuckets[currBucket];
                    return next;
                }
            }
            return current.pfnNext;

        }

        //returns false if next element doesn't exist
        public boolean hasNext() {
            if(numSeen == numPTEs) {
                return false;
            }
            else return true;
        }
        //Returns current block and updates pointer
        public Integer next() {
            PageTableEntry temp = current;
            current = getNext();
            numSeen++;
            return temp.pfn;
        }
    }


}

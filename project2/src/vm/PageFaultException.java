package vm;
public class PageFaultException extends Exception{

    public PageFaultException() {
        super("page fault exception");
    }
}

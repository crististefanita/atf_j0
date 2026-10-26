package atf.reporting.console;
import atf.reporting.contract.AtfReportStatus;
import java.util.concurrent.atomic.AtomicBoolean;
public final class ReportStepScope implements AutoCloseable {
    private final Runnable start; private final java.util.function.Consumer<AtfReportStatus> complete; private final AtomicBoolean closed=new AtomicBoolean(); private AtfReportStatus status=AtfReportStatus.INFO;
    public ReportStepScope(Runnable start,java.util.function.Consumer<AtfReportStatus> complete){this.start=start;this.complete=complete;start.run();}
    public void complete(AtfReportStatus status){this.status=status;if(closed.compareAndSet(false,true))complete.accept(status);}
    public void close(){complete(status);}
}

package is.landsbokasafn.crawler.processors;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.crawler.framework.CrawlController;
import org.archive.modules.CrawlURI;
import org.archive.modules.ProcessResult;
import org.archive.modules.Processor;
import org.archive.util.ArchiveUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

public class LowDiskPauseProcessor extends Processor implements InitializingBean {
    Map<String, Long> lastValues = new HashMap<>();
    
    /**
     * Logger.
     */
    private static final Logger logger =
        Logger.getLogger(LowDiskPauseProcessor.class.getName());


    protected CrawlController controller;
    public CrawlController getCrawlController() {
        return this.controller;
    }
    @Autowired
    public void setCrawlController(CrawlController controller) {
        this.controller = controller;
    }
    
    /**
     * List of filessystem mounts whose 'available' space should be monitored
     * via 'df' (if available).
     */
    List<String> monitorMounts = new ArrayList<>();
    public List<String> getMonitorMounts() {
        return this.monitorMounts;
    }
    public void setMonitorMounts(List<String> monitorMounts) {
        this.monitorMounts = monitorMounts;
    }

    /**
     * When available space on any monitored mounts falls below this threshold,
     * the crawl will be paused.
     */
    long pauseThresholdKb = 500*1024L; // 500MB 
    public long getPauseThresholdKb() {
        return this.pauseThresholdKb;
    }
    public void setPauseThresholdKb(long pauseThresholdKb) {
        this.pauseThresholdKb = pauseThresholdKb;
    }
    
    /**
     * Available space via 'df' is rechecked after every increment of this much
     * content (uncompressed) is observed.
     */
    long recheckThresholdKb = 200*1024L; // 200MB 
    public long getRecheckThresholdKb() {
        return this.recheckThresholdKb;
    }
    public void setRecheckThresholdKb(long recheckThresholdKb) {
        this.recheckThresholdKb = recheckThresholdKb;
    }
    
    protected int contentSinceCheck = 0;
    
	@Override
	public void afterPropertiesSet() throws Exception {
		// Get first reading immediately
		checkAvailableSpace();
	}
    
    @Override
    protected boolean shouldProcess(CrawlURI curi) {
        return true;
    }

    @Override
    protected void innerProcess(CrawlURI uri) {
        throw new AssertionError();
    }
    
    /**
     * Notes a CrawlURI's content size in its running tally. If the 
     * recheck increment of content has passed through since the last
     * available-space check, checks available space and pauses the 
     * crawl if any monitored mounts are below the configured threshold. 
     * 
     * @param curi CrawlURI to process.
     */
    @Override
    protected ProcessResult innerProcessResult(CrawlURI curi) {
        synchronized (this) {
            contentSinceCheck += curi.getContentSize();
            if (contentSinceCheck/1024 > getRecheckThresholdKb()) {
                ProcessResult r = checkAvailableSpace();
                contentSinceCheck = 0;
                return r;
            } 

            return ProcessResult.PROCEED;
        }
    }


    /**
     * Probe via File.getUsableSpace to see if monitored mounts have fallen
     * below the pause available threshold. If so, request a 
     * crawl pause. 
     */
    private ProcessResult checkAvailableSpace() {
        List<String> monitoredMounts = getMonitorMounts();
        for (String folderPath : monitoredMounts) {
        	File folder = new File(folderPath);
        
            long availBytes = folder.getUsableSpace();
            lastValues.put(folderPath, availBytes);
            long thresholdKilobytes = getPauseThresholdKb();
            if (availBytes/1024 < thresholdKilobytes ) {
                logger.log(Level.SEVERE, "Low Disk Pause - " + 
                		ArchiveUtils.formatBytesForDisplay(availBytes) + 
                		" available on " + folderPath + " (below threshold " + 
                        ArchiveUtils.formatBytesForDisplay(thresholdKilobytes*1024) + ")");
                controller.requestCrawlPause();
                return ProcessResult.PROCEED;
            }
        }
        return ProcessResult.PROCEED;
    }
    
    
	@Override
	public String report() {
        List<String> monitoredMounts = getMonitorMounts();
        StringBuilder report = new StringBuilder();
        report.append(super.report());
        report.append("  Pause threshold: " + getPauseThresholdKb() + " KiB ("); 
        report.append(ArchiveUtils.formatBytesForDisplay(getPauseThresholdKb()*1024) +")\n");
        report.append("  Recheck intervals: " + getRecheckThresholdKb() + " KiB (");
        report.append(ArchiveUtils.formatBytesForDisplay(getRecheckThresholdKb()*1024) +")\n");
        report.append("  Last seen usable space:\n");
        for (String folderPath : monitoredMounts) {
        	report.append("    " + folderPath + " > " + lastValues.get(folderPath) + " KiB (");
            report.append(ArchiveUtils.formatBytesForDisplay(lastValues.get(folderPath)) +")\n");
        }
        report.append("  Will check again after " + (recheckThresholdKb-(contentSinceCheck/1024)) + " KiB (");
        report.append(ArchiveUtils.formatBytesForDisplay((recheckThresholdKb*1024)-contentSinceCheck) +")\n");
		return report.toString();
	}
	
    
    
}

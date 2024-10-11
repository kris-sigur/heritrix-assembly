package is.landsbokasafn.crawler.reporting;

import java.util.Date;

import org.archive.crawler.event.StatSnapshotEvent;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.reporting.CrawlStatSnapshot;
import org.archive.crawler.reporting.StatisticsTracker;
import org.archive.util.ArchiveUtils;
import org.archive.util.PaddingStringBuffer;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Lookup;

/**
 * <p>Variant of the default StatisticsTracker.<p>
 * 
 * <p>Resolves 3 issues related to <em>progress-statistics.log</em>:</p>
 * 
 * <ol>
 * <li>docs/s and KB/sec columns were too small. Now provides more room. Also breaks up their averages into their
 *     own columns</li>
 * <li>Adds info about elapsed runtime (i.e. time the crawler has spent either RUNNING or waiting to either
 *     PAUSE or END.</li> 
 * <li>No longer logs regular progress lines to the file while PAUSED, or before finished preparing the crawl</li>
 *</ol>
 */
public class LbsStatisticsTracker extends StatisticsTracker {

    @Override
    protected void logNote(String note) {
        this.controller.logProgressStatistics(new PaddingStringBuffer()
                .append(ArchiveUtils.getLog14Date(new Date()))
                .raAppend(34, formatElapsedMilliseconds(getCrawlElapsedTime()))
                .append(" ")
                .append(note)
                .toString());
    }

    @Override
    public String progressStatisticsLegend() {
        return "           timestamp       runtime  discovered      queued   downloaded     doc/s    (avg)"
                + "     KB/s   (avg) dl-failures   busy-thread   mem-use-KB  heap-size-KB   congestion"
                + "   max-depth   avg-depth";
    }

    @Override
    protected synchronized void progressStatisticsEvent() {
        CrawlStatSnapshot snapshot = getSnapshot();
        
        if (this.controller != null 
                && this.controller.getState() != CrawlController.State.NASCENT
                && this.controller.getState() != CrawlController.State.FINISHED
                && this.controller.getState() != CrawlController.State.PAUSED
                && this.controller.getState() != CrawlController.State.PREPARING) {
            this.controller.logProgressStatistics(getProgressStatisticsLine(snapshot));
        }
        snapshots.addFirst(snapshot);
        while(snapshots.size()>getKeepSnapshotsCount()) {
            snapshots.removeLast();
        }
        
        // publish app event 
        appCtx.publishEvent(new StatSnapshotEvent(this,snapshot));
        
        // temporary workaround for 
        // [ 996161 ] Fix DNSJava issues (memory) -- replace with JNDI-DNS?
        // http://sourceforge.net/support/tracker.php?aid=996161
        Lookup.getDefaultCache(DClass.IN).clearCache();
    }
    
    /**
     * Build progress-statistics report line
     * 
     */
    public String getProgressStatisticsLine(CrawlStatSnapshot snapshot) {
        return new PaddingStringBuffer()
            .append(ArchiveUtils.getLog14Date(snapshot.timestamp))
            .raAppend(34, formatElapsedMilliseconds(snapshot.elapsedMilliseconds))
            .raAppend(46, snapshot.discoveredUriCount)
            .raAppend(58, snapshot.queuedUriCount)
            .raAppend(71, snapshot.downloadedUriCount)
            .raAppend(81, ArchiveUtils.doubleToString(snapshot.currentDocsPerSecond, 2))
            .raAppend(90, "(" + ArchiveUtils.doubleToString(snapshot.docsPerSecond, 2) + ")")
            .raAppend(99, snapshot.currentKiBPerSec)
            .raAppend(107, "(" + snapshot.totalKiBPerSec + ")")
            .raAppend(119, snapshot.downloadFailures)
            .raAppend(133, snapshot.busyThreads)
            .raAppend(146, (Runtime.getRuntime().totalMemory() -
                Runtime.getRuntime().freeMemory()) / 1024)
            .raAppend(160, Runtime.getRuntime().totalMemory() / 1024)
            .raAppend(173, ArchiveUtils.doubleToString(snapshot.congestionRatio, 2))
            .raAppend(185, snapshot.deepestUri)
            .raAppend(197, snapshot.averageDepth)
            .toString();
    }

    protected static String formatElapsedMilliseconds(long duration) {
        if(duration < 1000) {
            return "00s";
        }
        StringBuilder sb = new StringBuilder();
        if(duration<0) {
            sb.append("-");
        }
        long absTime = Math.abs(duration);
        long[] thresholds = {24*60*60*1000, 60*60*1000, 60*1000, 1000};
        String[] thresholdFormats = {"%01d", "%02d", "%02d", "%02d"};
        String[] units = {"d","h","m","s"};
        
        for(int i = 0; i < thresholds.length; i++) {
            if(absTime >= thresholds[i]) {
                sb.append(String.format(thresholdFormats[i], absTime / thresholds[i]));
                sb.append(units[i]);
                absTime = absTime % thresholds[i];
            }
        }
        return sb.toString();
    }
}

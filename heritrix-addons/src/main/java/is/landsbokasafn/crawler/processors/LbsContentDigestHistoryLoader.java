package is.landsbokasafn.crawler.processors;

import static org.archive.modules.recrawl.RecrawlAttributeConstants.A_ORIGINAL_DATE;
import static org.archive.modules.recrawl.RecrawlAttributeConstants.A_ORIGINAL_URL;
import static org.archive.modules.recrawl.RecrawlAttributeConstants.A_WARC_RECORD_ID;
import static is.landsbokasafn.deduplicator.DeDuplicatorConstants.REVISIT_ANNOTATION_MARKER;
import static is.landsbokasafn.deduplicator.DeDuplicatorConstants.EXTRA_REVISIT_DATE;
import static is.landsbokasafn.deduplicator.DeDuplicatorConstants.EXTRA_REVISIT_PROFILE;
import static is.landsbokasafn.deduplicator.DeDuplicatorConstants.EXTRA_REVISIT_URI;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.archive.modules.CrawlURI;
import org.archive.modules.recrawl.ContentDigestHistoryLoader;
import org.archive.modules.revisit.IdenticalPayloadDigestRevisit;
import org.archive.util.ArchiveUtils;

/**
 * Variant on {@link ContentDigestHistoryLoader}. Overrides {@link #innerProcess(CrawlURI)} in order to ensure 
 * that the annotation and extra info written to the crawl log are consistent with what our primary deduplication
 * (via the DeDuplicator) is writing.
 *
 */
public class LbsContentDigestHistoryLoader extends ContentDigestHistoryLoader {
	
	private AtomicInteger uriHandled = new AtomicInteger(0);
	private AtomicLong bytesHandled = new AtomicLong(0);
	private AtomicInteger uriDuplicate = new AtomicInteger(0);
	private AtomicLong bytesDuplicate = new AtomicLong(0);

	@Override
	protected void innerProcess(CrawlURI curi) throws InterruptedException {
		uriHandled.incrementAndGet();
		bytesHandled.addAndGet(curi.getContentSize());
        contentDigestHistory.load(curi);

        if (!curi.getContentDigestHistory().isEmpty()) {
        	IdenticalPayloadDigestRevisit revisit = 
        			new IdenticalPayloadDigestRevisit(curi.getContentDigestSchemeString());
			revisit.setRefersToDate((String)curi.getContentDigestHistory().get(A_ORIGINAL_DATE));
			revisit.setRefersToTargetURI((String)curi.getContentDigestHistory().get(A_ORIGINAL_URL));
			String warcRecordId= (String)curi.getContentDigestHistory().get(A_WARC_RECORD_ID);
			if (warcRecordId!=null) {
				revisit.setRefersToRecordID(warcRecordId);
			}
			curi.setRevisitProfile(revisit);

			curi.getAnnotations().add("CrawlTimeDedup");
			
			uriDuplicate.incrementAndGet();
			bytesDuplicate.addAndGet(curi.getContentSize());
			
			// BELOW HERE IS EFFECTIVELY COPIED FROM DEDUPLICATIOR
			// Add annotation to crawl.log 
            curi.getAnnotations().add(REVISIT_ANNOTATION_MARKER);
            
            // Write extra logging information (needs to be enabled in CrawlerLoggerModule)
            curi.addExtraInfo(EXTRA_REVISIT_PROFILE, revisit.getProfileName());
            curi.addExtraInfo(EXTRA_REVISIT_URI, revisit.getRefersToTargetURI());
            curi.addExtraInfo(EXTRA_REVISIT_DATE, revisit.getRefersToDate());
        }	
    }

	public String report() {
        StringBuilder ret = new StringBuilder();
        ret.append("Processor: ");
        ret.append(LbsContentDigestHistoryLoader.class.getCanonicalName());
        ret.append("\n");
        ret.append("  Function:          Set revisit profile on records deemed duplicate by hash comparison\n");
        ret.append("  Total handled:     " + uriHandled + "\n");
        ret.append("  Duplicates found:  " + uriDuplicate + " " + 
        		getPercentage(uriDuplicate.get(),uriHandled.get()) + "\n");
        ret.append("  Bytes total:       " + bytesHandled + " (" + 
        		ArchiveUtils.formatBytesForDisplay(bytesHandled.get()) + ")\n");
        ret.append("  Bytes duplicate:   " + bytesDuplicate + " (" + 
        		ArchiveUtils.formatBytesForDisplay(bytesDuplicate.get()) + ") " + 
        		getPercentage(bytesDuplicate.get(), bytesHandled.get()) + "\n");
        ret.append("\n");
        return ret.toString();
	}

	protected static String getPercentage(double portion, double total){
		NumberFormat percentFormat = NumberFormat.getPercentInstance(Locale.ENGLISH);
		percentFormat.setMaximumFractionDigits(1);
		return percentFormat.format(portion/total);
	}

}

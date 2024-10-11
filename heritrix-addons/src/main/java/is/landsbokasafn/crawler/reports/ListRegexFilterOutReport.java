package is.landsbokasafn.crawler.reports;

import is.landsbokasafn.crawler.deciderules.MatchesListRegexDecideRule;

import java.io.PrintWriter;

import org.archive.crawler.reporting.Report;
import org.archive.crawler.reporting.StatisticsTracker;

public class ListRegexFilterOutReport extends Report {

	MatchesListRegexDecideRule listRegexFilterOut = null;
	public MatchesListRegexDecideRule getListRegexFilterOut() {
		return listRegexFilterOut;
	}
	public void setListRegexFilterOut(MatchesListRegexDecideRule listRegexFilterOut) {
		this.listRegexFilterOut = listRegexFilterOut;
	}

	@Override
	public void write(PrintWriter writer, StatisticsTracker stats) {
		writer.write(listRegexFilterOut.getReport());
	}

	@Override
	public String getFilename() {
		return "listRegexFilterOut-report.txt";
	}

}

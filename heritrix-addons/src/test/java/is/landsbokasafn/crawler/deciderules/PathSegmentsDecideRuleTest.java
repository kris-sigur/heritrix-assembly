package is.landsbokasafn.crawler.deciderules;

import org.archive.modules.deciderules.DecideResult;

import junit.framework.TestCase;

public class PathSegmentsDecideRuleTest extends TestCase {

    public void testGetSegments(){
        PathSegmentsDecideRule psdr = new PathSegmentsDecideRule();
        String url = "http://example.com/path1/path2/index.html";
        String[] expected = {"path1","path2","index.html"};
        String[] actual = psdr.getSegments(url);
        assertEquals(expected.length, actual.length);
        for (int i=0 ; i<expected.length ; i++) {
            assertEquals(expected[i], actual[i]);
        }
    }
    
    public void testGetSegmentsOnUrlsWithRepeatingSlashes(){
        PathSegmentsDecideRule psdr = new PathSegmentsDecideRule();
        String url = "http://example.com/path1///path2/index.html";
        String[] expected = {"path1","path2","index.html"};
        String[] actual = psdr.getSegments(url);
        assertEquals(expected.length, actual.length);
        for (int i=0 ; i<expected.length ; i++) {
            assertEquals(expected[i], actual[i]);
        }
    }
    
    public void testGetSegmentsOnUrlsWithParameters(){
        PathSegmentsDecideRule psdr = new PathSegmentsDecideRule();
        String url = "http://example.com/path1/path2/index.php?stuff=cool/stuff";
        String[] expected = {"path1","path2","index.php?stuff=cool/stuff"};
        String[] actual = psdr.getSegments(url);
        assertEquals(expected.length, actual.length);
        for (int i=0 ; i<expected.length ; i++) {
            assertEquals(expected[i], actual[i]);
        }
    }

    public void testGetSegmentsOnUrlsWithParametersBeginningASegment(){
        PathSegmentsDecideRule psdr = new PathSegmentsDecideRule();
        String url = "http://example.com/path1/path2/?stuff=cool/stuff";
        String[] expected = {"path1","path2","?stuff=cool/stuff"};
        String[] actual = psdr.getSegments(url);
        assertEquals(expected.length, actual.length);
        for (int i=0 ; i<expected.length ; i++) {
            assertEquals(expected[i], actual[i]);
        }
    }

    public void testGetSegmentsOnUrlsEndingWithDoubleSlashes(){
        PathSegmentsDecideRule psdr = new PathSegmentsDecideRule();
        String url = "http://example.com//";
        String[] expected = {""};
        String[] actual = psdr.getSegments(url);
        assertEquals(expected.length, actual.length);
        for (int i=0 ; i<expected.length ; i++) {
            assertEquals(expected[i], actual[i]);
        }
    }


    
    public void testEvaluatePass() {
        PathSegmentsDecideRule psdr = new PathSegmentsDecideRule();
        String[] segments = {"path1","path2","index.html"};
        assertEquals(DecideResult.NONE, psdr.evaluate(3, 2, segments));
    }
    
    public void testEvaluteFailMaxConSeg() {
        PathSegmentsDecideRule psdr = new PathSegmentsDecideRule();
        String[] segments = {"path1","path1","path1","index.html"};
        assertEquals(DecideResult.REJECT, psdr.evaluate(3, 2, segments));
    }
    
    public void testEvalutateFailMaxSeg() {
        PathSegmentsDecideRule psdr = new PathSegmentsDecideRule();
        String[] segments = {"path1","path2","path1","path1","path2","path1","index.html"};
        assertEquals(DecideResult.REJECT, psdr.evaluate(3, 2, segments));
    }
}

package org.wltea.analyzer.lucene;

/**
 * Created by IntelliJ IDEA.
 * User: huxing(xing.hu@okhqb.com)
 * Date: 13-5-14
 * Time: 下午2:50
 */
public class IKQueryAnalyzer extends IKAnalyzer {

    public IKQueryAnalyzer() {
        this(true);
    }

    public IKQueryAnalyzer(boolean useSmart) {
        super(useSmart);
    }
}

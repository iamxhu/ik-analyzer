package org.wltea.analyzer.solr;

import org.apache.lucene.analysis.util.TokenizerFactory;
import org.wltea.analyzer.lucene.IKTokenizer;

import java.io.Reader;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: huxing(xing.hu@okhqb.com)
 * Date: 13-8-2
 * Time: 下午5:17
 */
public class IKTokenizerFactory extends TokenizerFactory {

    //是否使用smart模式，即去除其它被包括的分词
    private boolean useSmart;

    @Override
    public IKTokenizer create(Reader input) {
        return new IKTokenizer(input,useSmart);
    }

    @Override
    public void init(Map<String, String> args) {
        super.init(args);
        assureMatchVersion();
        useSmart = getBoolean("useSmart", true);
    }


}

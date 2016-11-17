package lda;

import java.io.IOException;
import java.util.Map;

public class Main
{
	public static void main(String[] args) throws IOException
	{

	    Corpus corpus = Corpus.load("data/mini");

	    LdaGibbsSampler ldaGibbsSampler = new LdaGibbsSampler(corpus.getDocument(), corpus.getVocabularySize());

	    ldaGibbsSampler.gibbs(10);

	    double[][] phi = ldaGibbsSampler.getPhi();
	    Map<String, Double>[] topicMap = LdaUtil.translate(phi, corpus.getVocabulary(), 10);
	    LdaUtil.explain(topicMap);
	}
}

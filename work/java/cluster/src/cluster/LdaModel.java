package cluster;

import java.util.Map;

public class LdaModel
{

	int[][] doc;// word index array
	int V, K, M;// vocabulary size, topic number, document number
	int[][] z;// topic label array
	float alpha; // doc-topic dirichlet prior parameter
	float beta; // topic-word dirichlet prior parameter
	int[][] nmk;// given document m, count times of topic k. M*K
	int[][] nkt;// given topic k, count times of term t. K*V
	int[] nmkSum;// Sum for each row in nmk
	int[] nktSum;// Sum for each row in nkt
	double[][] phi;// Parameters for topic-word distribution K*V
	double[][] theta;// Parameters for doc-topic distribution M*K
	int iterations;// Times of iterations
	int saveStep;// The number of iterations between two saving
	int beginSaveIters;// Begin save model at this iteration
	Map<String, Integer> wordIndexMap;
	Documents docSet;

	public LdaModel(LdaGibbsSampling.modelparameters modelparam)
	{
		// TODO Auto-generated constructor stub
		alpha = modelparam.alpha;
		beta = modelparam.beta;
		iterations = modelparam.iteration;
		K = modelparam.topicNum;
		saveStep = modelparam.saveStep;
		beginSaveIters = modelparam.beginSaveIters;
	}

	public void initializeModel(Documents docSet)
	{
		this.docSet = docSet;
		// TODO Auto-generated method stub
		M = docSet.docs.size();
		V = docSet.termToIndexMap.size();
		nmk = new int[M][K];
		nkt = new int[K][V];
		nmkSum = new int[M];
		nktSum = new int[K];
		phi = new double[K][V];
		theta = new double[M][K];
		this.wordIndexMap = new HashMap<String, Integer>();

		// initialize documents index array
		doc = new int[M][];
		for (int m = 0; m < M; m++)
		{
			// Notice the limit of memory
			int N = docSet.docs.get(m).docWords.length;
			doc[m] = new int[N];
			for (int n = 0; n < N; n++)
			{
				doc[m][n] = docSet.docs.get(m).docWords[n];
			}
		}

		// initialize topic lable z for each word
		z = new int[M][];
		for (int m = 0; m < M; m++)
		{
			int N = docSet.docs.get(m).docWords.length;
			z[m] = new int[N];
			for (int n = 0; n < N; n++)
			{
				// 随机初始化！
				int initTopic = (int) (Math.random() * K);// From 0 to K - 1
				z[m][n] = initTopic;
				// number of words in doc m assigned to topic initTopic add 1
				nmk[m][initTopic]++;
				// number of terms doc[m][n] assigned to topic initTopic add 1
				nkt[initTopic][doc[m][n]]++;
				// total number of words assigned to topic initTopic add 1
				nktSum[initTopic]++;
			}
			// total number of words in document m is N
			nmkSum[m] = N;
		}
	}

	public void inferenceModel(Documents docSet) throws IOException
	{
		// TODO Auto-generated method stub
		if (iterations < saveStep + beginSaveIters)
		{
			System.err.println("Error: the number of iterations should be larger than " + (saveStep + beginSaveIters));
			System.exit(0);
		}
		for (int i = 0; i < iterations; i++)
		{
			System.out.println("Iteration " + i);
			if ((i >= beginSaveIters) && (((i - beginSaveIters) % saveStep) == 0))
			{
				// Saving the model
				System.out.println("Saving model at iteration " + i + " ... ");
				// Firstly update parameters
				updateEstimatedParameters();
				// Secondly print model variables
				saveIteratedModel(i, docSet);
			}

			// Use Gibbs Sampling to update z[][]
			for (int m = 0; m < M; m++)
			{
				int N = docSet.docs.get(m).docWords.length;
				for (int n = 0; n < N; n++)
				{
					// Sample from p(z_i|z_-i, w)
					int newTopic = sampleTopicZ(m, n);
					z[m][n] = newTopic;
				}
			}
		}
	}

	private void updateEstimatedParameters()
	{
		// TODO Auto-generated method stub
		for (int k = 0; k < K; k++)
		{
			for (int t = 0; t < V; t++)
			{
				phi[k][t] = (nkt[k][t] + beta) / (nktSum[k] + V * beta);
			}
		}

		for (int m = 0; m < M; m++)
		{
			for (int k = 0; k < K; k++)
			{
				theta[m][k] = (nmk[m][k] + alpha) / (nmkSum[m] + K * alpha);
			}
		}
	}

	private int sampleTopicZ(int m, int n)
	{
		// TODO Auto-generated method stub
		// Sample from p(z_i|z_-i, w) using Gibbs upde rule

		// Remove topic label for w_{m,n}
		int oldTopic = z[m][n];
		nmk[m][oldTopic]--;
		nkt[oldTopic][doc[m][n]]--;
		nmkSum[m]--;
		nktSum[oldTopic]--;

		// Compute p(z_i = k|z_-i, w)
		double[] p = new double[K];
		for (int k = 0; k < K; k++)
		{
			p[k] = (nkt[k][doc[m][n]] + beta) / (nktSum[k] + V * beta) * (nmk[m][k] + alpha) / (nmkSum[m] + K * alpha);
		}

		// Sample a new topic label for w_{m, n} like roulette
		// Compute cumulated probability for p
		for (int k = 1; k < K; k++)
		{
			p[k] += p[k - 1];
		}
		double u = Math.random() * p[K - 1]; // p[] is unnormalised
		int newTopic;
		for (newTopic = 0; newTopic < K; newTopic++)
		{
			if (u < p[newTopic])
			{
				break;
			}
		}

		// Add new topic label for w_{m, n}
		nmk[m][newTopic]++;
		nkt[newTopic][doc[m][n]]++;
		nmkSum[m]++;
		nktSum[newTopic]++;
		return newTopic;
	}

	/**
	 * 对给定的待预测的文本，将其分词结果的单词与训练集的单词的索引对应上
	 * 
	 * @param predictWordSet
	 * @return
	 */
	public Map<String, String> matchTermIndex(Set<Word> predictWordSet)
	{
		/**
		 * key:word的内容 value：文档index-单词index，如“1-2”
		 */
		Map<String, String> wordIndexMap = new HashMap<String, String>();
		for (Word word : predictWordSet)
		{
			String content = word.getContent();
			String indexStr = getTermIndex(content);
			wordIndexMap.put(content, indexStr);
		}
		return wordIndexMap;
	}

	/**
	 * 对于给定单词，找到该单词在训练集中对应的文档和单词索引
	 * 
	 * @param content
	 * @return
	 */
	public String getTermIndex(String content)
	{
		for (Integer m : docSet.getDocWordsList().keySet())
		{
			LinkedList<String> list = docSet.getDocWordsList().get(m);
			for (int i = 0; i < list.size(); i++)
			{
				if (list.get(i).equals(content))
					return m + "-" + i;
			}
		}
		return "none";
	}

	/**
	 * 在训练完LDA模型后，根据给定的主题索引set，得到每个主题的topNum单词列表集合
	 * 
	 * @param topicIndexSet
	 * @param topNum
	 * @return
	 */
	public Set<Word> getWordByTopics(Set<Integer> topicIndexSet, int topNum)
	{
		Set<Word> wordSet = new HashSet<Word>();
		for (Integer indexT : topicIndexSet)
		{
			List<Integer> tWordsIndexArray = new ArrayList<Integer>();
			for (int j = 0; j < V; j++)
				tWordsIndexArray.add(new Integer(j));
			Collections.sort(tWordsIndexArray, new LdaModel.TwordsComparable(phi[indexT]));
			for (int t = 0; t < topNum; t++)
			{
				String content = docSet.indexToTermMap.get(tWordsIndexArray.get(t));
				Word word = new Word(content);
				if (SegmentWordsResult.getStopWordsSet().contains(content) || ProcessKeyWords.remove(word)
						|| ProcessKeyWords.isMeaninglessWord(content))
					continue;
				wordSet.add(word);
			}
		}
		return wordSet;
	}

	public Set<Word> getWordByTopic(Integer topicIndex)
	{
		Set<Word> wordSet = new HashSet<Word>();
		List<Integer> tWordsIndexArray = new ArrayList<Integer>();
		for (int j = 0; j < V; j++)
		{
			tWordsIndexArray.add(new Integer(j));
		}
		Collections.sort(tWordsIndexArray, new LdaModel.TwordsComparable(phi[topicIndex]));
		for (int t = 0; t < V; t++)
		{
			String content = docSet.indexToTermMap.get(tWordsIndexArray.get(t));
			Word word = new Word(content);
			word.setWeight(phi[topicIndex][tWordsIndexArray.get(t)]);
			if (SegmentWordsResult.getStopWordsSet().contains(content) || ProcessKeyWords.remove(word)
					|| ProcessKeyWords.isMeaninglessWord(content))
				continue;
			if (phi[topicIndex][tWordsIndexArray.get(t)] <= 0.0)
				continue;
			wordSet.add(word);
		}
		return wordSet;
	}

	public int predictNewSampleTopic(Document doc)
	{
		double topicProb[] = new double[K];
		Map<String, String> wordIndexMap = matchTermIndex(doc.getWordMap().keySet());
		int predict_v = doc.getWordCount();
		int[][] predict_nkt;// given topic k, count times of term t. K*V
		double[][] predict_phi;// Parameters for topic-word distribution K*V
		int[] predict_z;// topic label array
		int[] predict_nk;// 该文档覆盖的主题索引，值为该文档覆盖指定主题的次数

		predict_nkt = new int[K][predict_v];
		predict_phi = new double[K][predict_v];
		predict_z = new int[predict_v];
		predict_nk = new int[K];
		for (int index = 0; index < predict_v; index++)
		{
			String content = doc.getWordsList().get(index);
			String indexStr = wordIndexMap.get(content);
			if (indexStr.indexOf("-") == -1)
				continue;
			int m = Integer.valueOf(indexStr.substring(0, indexStr.indexOf("-")));
			int n = Integer.valueOf(indexStr.substring(indexStr.indexOf("-") + 1));
			// Sample from p(z_i|z_-i, w)
			int newTopic = predictSampleTopicZ(m, n);
			predict_z[index] = newTopic;
			predict_nkt[newTopic][index]++;
			predict_nk[newTopic]++;
		}
		for (int k = 0; k < K; k++)
		{
			topicProb[k] = (predict_nk[k] + alpha) / (predict_v + K * alpha);
		}
		return getTopic(topicProb);

	}

	public int getTopic(double[] topicProp)
	{
		int maxIndex = 0;
		double maxProp = topicProp[0];
		Set<String> words = new HashSet<String>();
		for (int k = 1; k < K; k++)
		{
			if (maxProp < topicProp[k])
			{
				maxProp = topicProp[k];
				maxIndex = k;
			}
		}
		return maxIndex;
	}

	public int predictSampleTopicZ(int m, int n)
	{
		// TODO Auto-generated method stub
		// Sample from p(z_i|z_-i, w) using Gibbs upde rule

		// Compute p(z_i = k|z_-i, w)
		double[] p = new double[K];
		for (int k = 0; k < K; k++)
		{
			p[k] = (nkt[k][doc[m][n]] + beta) / (nktSum[k] + V * beta) * (nmk[m][k] + alpha) / (nmkSum[m] + K * alpha);
		}

		// Sample a new topic label for w_{m, n} like roulette
		// Compute cumulated probability for p
		for (int k = 1; k < K; k++)
		{
			p[k] += p[k - 1];
		}
		double u = Math.random() * p[K - 1]; // p[] is unnormalised
		int newTopic;
		for (newTopic = 0; newTopic < K; newTopic++)
		{
			if (u < p[newTopic])
			{
				break;
			}
		}

		// Add new topic label for w_{m, n}
		return newTopic;
	}

	public void saveIteratedModel(int iters, Documents docSet) throws IOException
	{
		// TODO Auto-generated method stub
		// lda.params lda.phi lda.theta lda.tassign lda.twords
		// lda.params
		String resultPath = "ldaResult/";
		String modelName = "lda_" + iters;
		ArrayList<String> lines = new ArrayList<String>();
		lines.add("alpha = " + alpha);
		lines.add("beta = " + beta);
		lines.add("topicNum = " + K);
		lines.add("docNum = " + M);
		lines.add("termNum = " + V);
		lines.add("iterations = " + iterations);
		lines.add("saveStep = " + saveStep);
		lines.add("beginSaveIters = " + beginSaveIters);
		FileUtil.writeLines(resultPath + modelName + ".params", lines);

		// lda.phi K*V
		BufferedWriter writer = new BufferedWriter(new FileWriter(resultPath + modelName + ".phi"));
		for (int i = 0; i < K; i++)
		{
			for (int j = 0; j < V; j++)
			{
				writer.write(phi[i][j] + "\t");
			}
			writer.write("\n");
		}
		writer.close();

		// lda.theta M*K
		writer = new BufferedWriter(new FileWriter(resultPath + modelName + ".theta"));
		for (int i = 0; i < M; i++)
		{
			for (int j = 0; j < K; j++)
			{
				writer.write(theta[i][j] + "\t");
			}
			writer.write("\n");
		}
		writer.close();

		// lda.tassign
		writer = new BufferedWriter(new FileWriter(resultPath + modelName + ".tassign"));
		for (int m = 0; m < M; m++)
		{
			for (int n = 0; n < doc[m].length; n++)
			{
				writer.write(doc[m][n] + ":" + z[m][n] + "\t");
			}
			writer.write("\n");
		}
		writer.close();
		List<Word> appendwords = new ArrayList<Word>();
		// lda.twords phi[][] K*V
		writer = new BufferedWriter(new FileWriter(resultPath + modelName + ".twords"));
		int topNum = 10; // Find the top 20 topic words in each topic
		for (int i = 0; i < K; i++)
		{
			List<Integer> tWordsIndexArray = new ArrayList<Integer>();
			for (int j = 0; j < V; j++)
			{
				tWordsIndexArray.add(new Integer(j));
			}
			Collections.sort(tWordsIndexArray, new LdaModel.TwordsComparable(phi[i]));
			writer.write("topic " + i + "\t:\t");
			for (int t = 0; t < topNum; t++)
			{
				writer.write(docSet.indexToTermMap.get(tWordsIndexArray.get(t)) + " " + phi[i][tWordsIndexArray.get(t)]
						+ "\t");
				Word word = new Word(docSet.indexToTermMap.get(tWordsIndexArray.get(t)));
				word.setWeight(phi[i][tWordsIndexArray.get(t)]);
				appendwords.add(word);
			}
			writer.write("\n");
		}
		writer.close();
		// lda.words
		writer = new BufferedWriter(new FileWriter(resultPath + modelName + ".words"));
		for (Word word : appendwords)
		{
			if (word.getContent().trim().equals(""))
				continue;
			writer.write(word.getContent() + "\t" + word.getWeight() + "\n");
		}
		writer.close();
	}

	public class TwordsComparable implements Comparator<Integer>
	{

		public double[] sortProb; // Store probability of each word in topic k

		public TwordsComparable(double[] sortProb)
		{
			this.sortProb = sortProb;
		}

		@Override
		public int compare(Integer o1, Integer o2)
		{
			// TODO Auto-generated method stub
			// Sort topic word index according to the probability of each word
			// in topic k
			if (sortProb[o1] > sortProb[o2])
				return -1;
			else if (sortProb[o1] < sortProb[o2])
				return 1;
			else
				return 0;
		}
	}

	public static void main(String[] args)
	{

	}

}
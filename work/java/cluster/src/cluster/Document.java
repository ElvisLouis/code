package cluster;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.ling.Word;

public class Document
{
	private static NLPIRUtil npr = new NLPIRUtil();
	private static Set<String> stopWordsSet = SegmentWordsResult.getStopWordsSet();
	private String docName;
	int[] docWords;
	private int wordCount;
	private Map<Word, Integer> wordMap;
	private LinkedList<String> wordsList;// 为了和docWords的索引对应，即单词内容对应索引值

	public int getWordCount()
	{
		return wordCount;
	}

	public void setWordCount(int wordCount)
	{
		this.wordCount = wordCount;
	}

	public Map<Word, Integer> getWordMap()
	{
		return wordMap;
	}

	public void setWordMap(Map<Word, Integer> wordMap)
	{
		this.wordMap = wordMap;
	}

	public LinkedList<String> getWordsList()
	{
		return wordsList;
	}

	public void setWordsList(LinkedList<String> wordsList)
	{
		this.wordsList = wordsList;
	}

	public Document(String docContent)
	{
		this.wordMap = new HashMap<Word, Integer>();
		this.wordsList = new LinkedList<String>();
		String splitResult = npr.NLPIR_ParagraphProcess(ProcessMessage.dealWithSentence(docContent), 0);
		String[] wordsArray = splitResult.split(" ");
		this.docWords = new int[wordsArray.length];
		int index = 0;
		// Transfer word to index
		for (String str : wordsArray)
		{
			String content = ProcessMessage.dealSpecialString(str);
			Word word = new Word(content);
			if (ProcessKeyWords.remove(word) || stopWordsSet.contains(content))
				continue;
			else if (content.length() <= 1 || RegexMatch.specialMatch(content))
				continue;
			this.wordCount++;
			if (!wordMap.containsKey(content))
			{
				int newIndex = wordMap.size();
				wordMap.put(word, 1);
				docWords[index++] = newIndex;
			}
			else
			{
				wordMap.put(word, wordMap.get(word) + 1);
				docWords[index++] = wordMap.get(content);
			}
			this.wordsList.add(content);
		}

	}

	public Document(String filePath, Map<String, Integer> termToIndexMap, ArrayList<String> indexToTermMap,
			Map<String, Integer> termCountMap)
	{
		this(FileUtil.readContent(filePath));
		this.docName = filePath;
		this.wordMap = new HashMap<Word, Integer>();
		this.wordsList = new LinkedList<String>();
		// Read file and initialize word index array
		String docContent = FileUtil.readContent(docName);

		String splitResult = npr.NLPIR_ParagraphProcess(docContent, 0);
		String[] wordsArray = splitResult.split(" ");
		this.docWords = new int[wordsArray.length];
		int index = 0;
		// Transfer word to index
		for (String str : wordsArray)
		{
			String content = ProcessMessage.dealSpecialString(str);
			Word word = new Word(content);
			if (ProcessKeyWords.remove(word) || stopWordsSet.contains(content))
				continue;
			else if (ProcessKeyWords.isMeaninglessWord(content))
				continue;
			this.wordCount++;
			if (!termToIndexMap.containsKey(content))
			{
				int newIndex = termToIndexMap.size();
				termToIndexMap.put(str, newIndex);
				indexToTermMap.add(str);
				termCountMap.put(str, new Integer(1));
				docWords[index++] = newIndex;
			}
			else
			{
				termCountMap.put(content, termCountMap.get(content) + 1);
				docWords[index++] = termToIndexMap.get(content);
			}
			this.wordsList.add(content);
			if (wordMap.containsKey(word))
				wordMap.put(word, wordMap.get(word) + 1);
			else
				wordMap.put(word, 1);
		}

	}

	public boolean isNoiseWord(String string)
	{
		// TODO Auto-generated method stub
		string = string.toLowerCase().trim();
		Pattern MY_PATTERN = Pattern.compile(".*[a-zA-Z]+.*");
		Matcher m = MY_PATTERN.matcher(string);
		// filter @xxx and URL
		if (string.matches(".*www\\..*") || string.matches(".*\\.com.*") || string.matches(".*http:.*"))
			return true;
		else
			return false;
	}

}
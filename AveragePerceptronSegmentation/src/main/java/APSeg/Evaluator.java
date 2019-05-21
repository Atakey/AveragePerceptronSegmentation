package APSeg;

import org.apache.commons.lang.StringUtils;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.HashSet;


public class Evaluator
{
	/**
	 * 样本个数
	 */
	int exampleNum = 0;
	/**
	 * 语料库分词数量
	 */
	private int standardNum = 0;
	/**
	 * 算法分词数量
	 */
	private int resultNum = 0;
	/**
	 * 算法正确分词数量
	 */
	private int correctNum = 0;
	/**
	 * 保存错误样本数据
	 */
	private String filePath;
	private long startTime = System.currentTimeMillis();
	private long nowTime = startTime;

	private HashSet<String> words = new HashSet<>();

	public Evaluator(){}

	public Evaluator(String filePath)
	{
		this.filePath = filePath;
	}

	private HashSet<String> GenerateWordsSet(String[] words)
	{
		int offset = 0;
		HashSet<String> wordsSet = new HashSet<>();
		for(String word:words)
		{
			wordsSet.add(offset + word);
			offset += word.length();
		}
		return wordsSet;
	}

	public void Call(String[] oriSeg, String[] resSeg)
	{
		exampleNum++;
		HashSet<String> wordSet_ans = GenerateWordsSet(oriSeg);
		HashSet<String> wordSet_res = GenerateWordsSet(resSeg);
		standardNum += wordSet_ans.size();
		resultNum += wordSet_res.size();
		for(String wordPair : wordSet_res)
		{
			if(wordSet_ans.contains(wordPair))
			{
				correctNum ++;
				wordSet_ans.remove(wordPair);
			}
		}
//		if(filePath != null && filePath.length() > 0 && wordSet_ans.size() > 0)
		if(filePath != null && filePath.length() > 0 && wordSet_ans.size() > 0 && JudgeErrorExample(oriSeg, resSeg))
			SuperAddWriter(filePath,
					Arrays.toString(oriSeg) +
							System.lineSeparator() +
							Arrays.toString(resSeg) +
							System.lineSeparator() +
							System.lineSeparator());
	}

	private boolean JudgeErrorExample(String[] oriSeg, String[] resSeg)
	{
		int i=0,j=0;
		int avgFlag = 1;
		String word = "";
		int size1 = oriSeg.length;
		int size2 = resSeg.length;
		int iLen = oriSeg[i++].length(), jLen = resSeg[j++].length();
		while(i < size1-1 && j < size2-1)
		{
			if(avgFlag % 6 == 0)
				return true;
			if(iLen == jLen)
			{
				iLen = 0;jLen = 0;
				words.add(word);
				word = ("|" + oriSeg[i]);
				iLen += oriSeg[i++].length();
				jLen += resSeg[j++].length();
				avgFlag = 1;
			}
			else if (iLen < jLen)
			{
				word += ("|" + oriSeg[i]);
				iLen += oriSeg[i++].length();
				avgFlag = avgFlag << 1;
			}
			else
			{
				jLen += resSeg[j++].length();
				avgFlag *=3;
			}
		}
		return false;
	}

	/**
	 * 错误样本追加写入
	 * @param filePath
	 * @param sentence
	 */
	private void SuperAddWriter(String filePath, String sentence)
	{
		try
			(
				FileOutputStream fileOutputStream = new FileOutputStream(filePath, true);
				OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
				BufferedWriter out = new BufferedWriter(outputStreamWriter);
			){
			out.write(sentence + System.lineSeparator());
		}
		catch (Exception e) {
			e.printStackTrace();
			System.err.println("文本写入失败！~");
		}
	}

	public void CallTime(int step)
	{
		long preTime = System.currentTimeMillis();
		System.out.println("当前处理了 " +  step + " 个样本，距上次已过去 " + (preTime - nowTime) + " ms");
		nowTime = preTime;
	}

	public void Report()
	{
		double precision = resultNum > 0 ? 1.0d * correctNum / resultNum : 0;
		double recall = standardNum > 0 ? 1.0d * correctNum / standardNum : 0;
		double f1 = (precision + recall) > 0 ? 2 * precision * recall / (precision + recall) : 0;
		System.out.println(String.format("历时: %.2f秒 样本个数: %d  原始词数: %d 结果词数: %d  正确词数: %d",
				(System.currentTimeMillis() - startTime) *1.0f / 1000, exampleNum, standardNum, resultNum, correctNum));
		System.out.println(String.format("F值: %.4f  P值: %.4f  R值: %.4f", f1, precision, recall));

		if(filePath != null && filePath.length() > 0 )
			SuperAddWriter(filePath, StringUtils.join(words,"\n"));
		words.clear();
		words = null;
	}
}

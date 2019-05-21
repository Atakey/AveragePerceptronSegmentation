package APSeg;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class AveragePerceptronSeg
{
    private static APModel model;
    private static Logger logger = Logger.getLogger(AveragePerceptronSeg.class);

	public void init(String path)
    {
        model = APModel.getInstanse();
        if(model != null)
            model.init(path);
    }
	

	public List<SegToken> process(String inputStr)
	{
        List<SegToken> segTokens = new ArrayList<>();
		if(inputStr == null || inputStr.length() == 0)
			return segTokens;
		String labels = model.featureStructure.Decode(inputStr);
        StringBuilder cache = new StringBuilder();

        int startOffset = 0;
		for(int i = 0; i < inputStr.length(); i++)
		{
			cache.append(inputStr.charAt(i));
            char label = labels.charAt(i);
            if(label == '2' || label == '3')
			{
				segTokens.add(new SegToken(new String(cache), startOffset, i));
				cache.delete(0, cache.length());
				startOffset = i + 1;
			}
		}
		if(cache.length() > 0)
		{
			segTokens.add(new SegToken(new String(cache), startOffset, inputStr.length()));
			cache.delete(0, cache.length());
		}
        // 用户字典的后处理
		segTokens = FinalSegTokenSeg(segTokens);
		return segTokens;
	}

    /**
     * 用户字典的最后一步分词操作
     * @return
     */
    private List<SegToken> FinalSegTokenSeg(List<SegToken> tokens)
    {
        int size = tokens.size();
        if(size < 2)
            return tokens;
        List<SegToken> _words = new ArrayList<>();
        StringBuilder cache = new StringBuilder();
        UserDictTrieTree.TrieNode tree = model.userDictTrieTree.root;
        int i = 0;
        while(i < size)
        {
            SegToken segToken = tokens.get(i);
            String word = segToken.word;
            int startOffset = segToken.startOffset;
            cache.append(word);
            i++;
            UserDictTrieTree.TrieNode _tree = model.userDictTrieTree.SearchNode(tree, word);
            if(null != _tree)
            {
                tree = _tree;
                for (int j = i; j < size; j++)
                {
                    String jword = tokens.get(j).word;
                    _tree = model.userDictTrieTree.SearchNode(tree, jword);
                    if(null == _tree)
                    {
                        i = j;
                        break;
                    }
                    else if(_tree.HasWord())
                    {
                        cache.append(jword);
                        tree = _tree;
                    }
                    else
                    {
                        cache.append(jword);
                        i = ++j;
                        break;
                    }
                }
            }
            _words.add(new SegToken(new String(cache), startOffset, cache.length()));
            cache.setLength(0);
            tree = model.userDictTrieTree.root;
        }
        return _words;
    }

	public List<String> sentenceProcess(String strSentence)
	{
        ArrayList<String> words = new ArrayList<>();
        if(strSentence == null || strSentence.length() == 0)
            return words;
        String labels = model.featureStructure.Decode(strSentence);
        StringBuilder cache = new StringBuilder();
        for(int i = 0; i < strSentence.length(); i++)
        {
            cache.append(strSentence.charAt(i));
            char label = labels.charAt(i);
            if(label == '2' || label == '3')
            {
                words.add(new String(cache));
                cache.delete(0, cache.length());
            }
        }
        if(cache.length() > 0)
            words.add(cache.toString());
		return FinalStringSeg(words);
	}

    /**
     * 用户字典的最后一步分词操作
     * @return
     */
	private List<String> FinalStringSeg(List<String> words)
    {
        int size = words.size();
        if(size < 2)
            return words;
        List<String> _words = new ArrayList<>();
        StringBuilder cache = new StringBuilder();
        UserDictTrieTree.TrieNode tree = model.userDictTrieTree.root;
        int i = 0;
        while(i < size)
        {
            String word = words.get(i);
            cache.append(word);
            i++;
            UserDictTrieTree.TrieNode _tree = model.userDictTrieTree.SearchNode(tree, word);
            if(null != _tree)
            {
                tree = _tree;
                for (int j = i; j < size; j++)
                {
                    String jword = words.get(j);
                    _tree = model.userDictTrieTree.SearchNode(tree, jword);
                    if(null == _tree)
                    {
                        i = j;
                        break;
                    }
                    else if(_tree.HasWord())
                    {
                        cache.append(jword);
                        tree = _tree;
                    }
                    else
                    {
                        cache.append(jword);
                        i = ++j;
                        break;
                    }
                }
            }
            _words.add(new String(cache));
            cache.setLength(0);
            tree = model.userDictTrieTree.root;
        }
        return _words;
    }

    /**
     * @param segSentence 例如："南京 市长 江大桥"，一个或多个空格分开
     * @param times 学习次数, 建议不大于10次，默认一次
     */
    public void Learn(String segSentence, int times)
    {
        int learn_Times = 0;
        String[] segSentences = segSentence.split("\\s+");
        String oriSentence = StringUtils.join(segSentences, "");
        while(learn_Times < times)
        {
            if(JudgeTheSame(sentenceProcess(oriSentence), segSentences))
            {
                logger.info("学习提前退出~ ，当前学习" + learn_Times + "次");
                break;
            }
            learn_Times++;
            model.Learn(segSentence);
        }
    }

    public void addUserWord(String word)
    {
        model.addUserWord(word);
    }

    private boolean JudgeTheSame(List<String> strings1, String[] strings2)
    {
        int size = strings1.size();
        if(size != strings2.length)
            return false;
        for (int i = 0; i < size; i++)
        {
            if(strings1.get(i).equals(strings2[i]))
                continue;
            return false;
        }
        return true;
    }

    public static void main(String[] args)
    {
        String path = ".\\data\\model\\";
        AveragePerceptronSeg averagePerceptronSeg = new AveragePerceptronSeg();
        averagePerceptronSeg.init(path);
        String sentece = "由感知机衍生出来的一个重要算法是平均感知机——一种序列标注算法。同MIRA类似，平均感知机存在...";
        long start;
        int times = 10000;

        start = System.currentTimeMillis();
        for (int i = 0; i < times; i++)
            averagePerceptronSeg.process(sentece);
        System.out.println(System.currentTimeMillis() - start);

        for(String segToken :averagePerceptronSeg.sentenceProcess(sentece))
            System.out.print(segToken + " ");
        System.out.println();

        System.out.println();
        //文件分词测试
        String testFile = ".\\data\\dev.dev";
        try
        (
            FileReader fileReader = new FileReader(new File(testFile));
            BufferedReader sb = new BufferedReader(fileReader);
        ){
            String line;
            start = System.currentTimeMillis();
            while ((line = sb.readLine()) != null)
            {
                String[] oriString = line.trim().split("\\s+");
                if(oriString.length < 2) continue;
                averagePerceptronSeg.sentenceProcess(StringUtils.join(oriString));
            }
            System.out.println("文件分词耗时：" + (System.currentTimeMillis() - start) + " ms");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
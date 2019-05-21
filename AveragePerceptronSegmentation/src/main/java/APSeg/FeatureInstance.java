package APSeg;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Huanghl on 2019/4/9.
 */
public class FeatureInstance
{
    private int len;
    private char[] oriSentenceChars;
    private char[] newSentenceChars;
    private int[] typeChar;


    public FeatureInstance(String oriSentence)
    {
        this.oriSentenceChars = oriSentence.toCharArray();
        this.len = oriSentence.length();
        this.typeChar = GenerateType(oriSentenceChars);
        this.newSentenceChars = GenerateWord(oriSentenceChars, typeChar);
    }

    /**
     * 对字简单的处理，如数字统一映射为1，英文统一映射为a,标点统一映射为。等等
     */
    private char[] GenerateWord(char[] oriSentenceChars, int[] typeChar)
    {
        char[] newSentenceChars = new char[len];
        for (int i = 0; i < len; i++)
            newSentenceChars[i] = SegUtils.GetWord(oriSentenceChars[i], typeChar[i]);
        return newSentenceChars;
    }

    /**
     * 获取字对应的类型，如数字类型，英文字母类型
     * @param oriSentenceChars
     * @return
     */
    private int[] GenerateType(char[]oriSentenceChars)
    {
        int[] typeChar = new int[len];
        for (int i = 0; i < len; i++)
            typeChar[i] = SegUtils.GetWordType(oriSentenceChars[i]);
        return typeChar;
    }

    private List<StringBuilder> GenerateFeature(int index)
    {
//        int typeLeft1,typeMid,typeRight1;
        char left1,mid,right1;

//          typeLeft2 = index > 1 ? typeChar[index-2] : 0;
//        typeLeft1 = index > 0 ? typeChar[index-1] : 0;
//        typeMid   =             typeChar[index]; //当前字符类型
//        typeRight1 = index < len-1 ? typeChar[index + 1] : 0;
//          typeRight2 = index < len-2 ? typeChar[index+2] : 0;

//          left2 = index > 1 ? newSentenceChars[index-2] : SegUtils.BeginEnd;
        left1 = index > 0 ? newSentenceChars[index-1] : SegUtils.BeginEnd;
        mid   =             newSentenceChars[index];//当前字符
        right1 = index < len-1 ? newSentenceChars[index + 1] : SegUtils.BeginEnd;
//          right2 = index < len-2 ? newSentenceChars[index+2] : SegUtils.BeginEnd;
        //特征列表
        List<StringBuilder> featureList = new ArrayList<>();
        appendFeature(featureList, mid);
        appendFeature(featureList, left1, mid);
        appendFeature(featureList, mid, right1);
//            appendFeature(featureList, left1);
//            appendFeature(featureList, right1);
//            appendFeature(featureList, typeMid);
//            appendFeature(featureList, left1, typeMid);
//            appendFeature(featureList, typeMid, right1);

        //作用较小
//            appendFeature(featureList, typeLeft1, typeMid);
//            appendFeature(featureList, typeMid, typeRight1);
//            以下2个特征对FP值有较小影响,没有对模型大小造成压力
//            appendFeature(featureList, left2);
//            appendFeature(featureList, right2);
        //轻微负相关
//            appendFeature(featureList, typeLeft1, mid);
//            appendFeature(featureList, mid, typeRight1);
        //轻微负相关
//            appendFeature(featureList, left2, left1);
//            appendFeature(featureList, right1, right2);
        //轻微正相关，对模型大小影响很大，不考虑该特征
//            appendFeature(featureList, left2, mid);
//            appendFeature(featureList, mid, right2);
        //严重负相关
//            appendFeature(featureList, typeLeft1);
//            appendFeature(featureList, typeRight1);
        return featureList;
    }

    private void appendFeature(List<StringBuilder> featureList, Object... objects)
    {
        StringBuilder sb = new StringBuilder();
        for(Object o:objects)
            sb.append(o);
        featureList.add(sb);
    }

    /**
     * 获取该句子所有特征
     * @return
     */
    public List<StringBuilder>[] GetAllFeatures()
    {
        List<StringBuilder>[] featureBuilders = new ArrayList[len];
        for (int i = 0; i < len; i++)
            featureBuilders[i] = GenerateFeature(i);
        return featureBuilders;
    }

    /**
     *
     * @param state 字符对应BEMS→0123
     * @param featureBuffers 特征
     * @param booleans 特征筛选，是否被选择
     * @return
     */
    public static List<String> GenerateKey(int state, List<StringBuilder> featureBuffers, boolean[] booleans)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(state);
        int i = 0;
        List<String> features = new ArrayList<>();
        if(null == booleans)
        {
            for (StringBuilder featureBuffer : featureBuffers)
            {
            sb.append(featureBuffer);
            features.add(new String(sb));
            sb.setLength(1);
            i++;
            }
        }
        else
        {
            for (StringBuilder featureBuffer : featureBuffers)
            {
                if(booleans[i])
                {
                    sb.append(featureBuffer);
                    features.add(new String(sb));
                    sb.setLength(1);
                }
                i++;
            }
        }
        return features;
    }

    public static List<String> GenerateKey(int state, List<StringBuilder> featureBuffers)
    {
        return GenerateKey(state, featureBuffers,null);
    }
}

package APSeg;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;

/**
 * train test 入口
 * Created by Huanghl on 2019/4/9.
 */
public class AveragePerceptronAlgorithm
{
    FeatureStructure featureStructure;
    private Logger logger = Logger.getLogger(APModel.class);

    public AveragePerceptronAlgorithm()
    {
        this(FeatureStructure.Pena.no_regu);
    }

    public AveragePerceptronAlgorithm(FeatureStructure.Pena pena)
    {
        featureStructure = new FeatureStructure(pena);
    }

    public void Train(String trainFile, String devFile, int iteration)
    {
        Random random = new Random();// 随机shuffle作用，对样本进行随机丢失
        for (int i = 0; i < iteration; i++)
        {
            System.out.println("第" + (i+1) + "次迭代");
            Evaluator evaluator = new Evaluator();
            try
            (
                FileReader fileReader = new FileReader(new File(trainFile));
                BufferedReader sb = new BufferedReader(fileReader);
            ) {
                String line;
                while ((line = sb.readLine()) != null)
                {
                    if(random.nextFloat() > 0.7) // 起到随机shuffle作用 3/10的样本丢失率
                        continue;
                    String[] oriString = line.trim().split("\\s+");
                    if(oriString.length < 1) continue;

                    String x = StringUtils.join(oriString,"");
                    String y = GenerateLabel(oriString);
                    FeatureInstance featureInstances = new FeatureInstance(x);
                    List<StringBuilder>[] featureBuffers = featureInstances.GetAllFeatures();
                    String z = featureStructure.Decode(x);
                    String[] resString = DumpExample(x, z);

                    evaluator.Call(oriString, resString);
                    featureStructure.step ++;
                    if(featureStructure.step % 50000 == 0)
                    {
                        evaluator.CallTime(featureStructure.step);
                    }
                    int j = 0;
                    for (; j < z.length() - 1; j++)
                    {
                        int yj = y.charAt(j) - 48;
                        int zj = z.charAt(j) - 48;
                        featureStructure.UpdateWeights(yj, featureBuffers[j], 1);
                        featureStructure.UpdateWeights(zj, featureBuffers[j], -1);
                        featureStructure.UpdateTransformProbility(yj, y.charAt(j+1)-48,1);
                        featureStructure.UpdateTransformProbility(zj, z.charAt(j+1)-48,-1);
                    }
                    int yj = y.charAt(j) - 48;
                    int zj = z.charAt(j) - 48;
                    featureStructure.UpdateWeights(yj, featureBuffers[j], 1);
                    featureStructure.UpdateWeights(zj, featureBuffers[j], -1);
                }
                evaluator.Report();
                featureStructure.UpdateAll();
                if((i+1) % 3 == 0)
                {
                featureStructure.Average();
                System.out.println(Arrays.toString(Predict(
                        "由感知机衍生出来的一个重要算法是平均感知机——一种序列标注算法。同MIRA类似，平均感知机存在...")));
                Test(devFile);
                Test(devFile, 0.80f);
//                String path = ".\\data\\model\\model_it-";
//                Save(path + (i+1) +"_00.ap",0f);
                featureStructure.Unaverage();
                System.gc();
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
                continue;
            }
        }
        Test(devFile);
        featureStructure.Average();
        //内存友好
        featureStructure.back_up.clear();
        featureStructure.averageFeatureMap.clear();
        featureStructure.timeStamp.clear();
        System.gc();
        System.out.println("========Train-------------End===========");
    }

    public void Save(String modelFile, final float ratio) throws IOException
    {
        System.out.println("======压缩比率 " + ratio + " 保存模型中...");
        if (ratio < 0 || ratio >= 1)
        {
            throw new IllegalArgumentException("the Rompression ratio should more than 0 and less than 1");
        }
        DataOutputStream wr = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(modelFile)));
        float threshold = GetThreshold(ratio);
        TransformProbilityWriter(wr, featureStructure.GetTransformProbility());
        FeatureWriter(wr, threshold);
        wr.flush();
        wr.close();
    }

    /**
     * 根据权重大小进行模型压缩
     * @param threshold
     * @return
     */
    public Map<String, Float> FeatureCompression(float threshold)
    {
        Map<String, Float> _featureFreqMap = new FeatureMap<>();
        float value;
        for(Map.Entry<String,Float> entry:featureStructure.featureFreqMap.entrySet())
        {
            value = entry.getValue();
            if(Math.abs(value) > threshold)
                _featureFreqMap.put(entry.getKey(), value);
        }
        return _featureFreqMap;
    }

    /**
     * 获取权重阈值
     * @param ratio 模型压缩比例
     * @return
     */
    private float GetThreshold(float ratio)
    {
        float threshold = -0.001f;
        if (ratio > 0)
        {
            int size = featureStructure.GetTotalSize();
            int len = (int) (size * ratio);

            ArrayList<Float> freq = new ArrayList<>();
            for (Float d:featureStructure.featureFreqMap.values())
            {
                freq.add(d);
            }
            Collections.sort(freq);
            int i = 0, j = size-1;
            float i_d = threshold;
            float j_d = threshold;
            while(len > -1)
            {
                i_d = Math.abs(freq.get(i));
                j_d = Math.abs(freq.get(j));
                if(i_d < j_d)
                {
                    i++;
                    len--;
                }
                else if(i_d > j_d)
                {
                    j--;
                    len--;
                }
                else
                {
                    i++; j--;
                    len -= 2;
                }
            }
            threshold = Math.min(i_d,j_d);
            freq.clear();
            freq = null;
        }
        return threshold;
    }

    /**
     * 写入转移矩阵
     * @throws IOException
     */
    private void TransformProbilityWriter(DataOutputStream wr, int[][] transformProbility) throws IOException
    {
        wr.writeUTF("#TransformProbility");
        int size = transformProbility.length;
        wr.writeInt(size);
        for (int i = 0; i < size; i++)
        {
            for (int j = 0; j < size; j++)
                wr.writeInt(transformProbility[i][j]);
        }
    }

    /**
     * 写入模型特征
     * @throws IOException
     */
    private void FeatureWriter(DataOutputStream wr, float threshold) throws IOException
    {
        wr.writeUTF("#Feature");
        String feature;
        float value;
        for(Map.Entry<String, Float> entry : featureStructure.featureFreqMap.entrySet())
        {
            value = entry.getValue();
            if(Math.abs(value) <= threshold)
                continue;
            feature = entry.getKey();
            wr.writeUTF(feature);
            wr.writeFloat(SegUtils.FormatValue(value,2));
        }
    }

    public FeatureStructure LoadModel(String modelFile) throws IOException
    {
        long start = System.currentTimeMillis();
        DataInputStream br = new DataInputStream(new BufferedInputStream(new FileInputStream(modelFile)));
        String line;
        while ((line = br.readUTF()) != null && !line.equals("#TransformProbility"))
            continue;
        if (!line.equals("#TransformProbility"))
        {
            throw new IOException("read model error when reading TransformProbility from model file");
        }
        //转移矩阵
        int labelSize = br.readInt();
        int[][] transformProbility = new int[labelSize][labelSize];
        for (int i = 0; i < labelSize; i++)
        {
            for (int j = 0; j < labelSize; j++)
                transformProbility[i][j] = br.readInt();
        }
        //特征map
        br.readUTF();
        float value;
        String feature;
        int FeatureSize = 0;
        Map<String, Float> featureFreqMap = new FeatureMap<>(1024*1024);
        while (br.available() != 0)
        {
            feature = br.readUTF();
            value = br.readFloat();
            featureFreqMap.put(feature, value);
            FeatureSize ++;
        }
        logger.info("特征大小 " + FeatureSize);
        System.out.println("特征大小 " + FeatureSize);
        featureStructure = new FeatureStructure(transformProbility, featureFreqMap);

        logger.info("加载耗时 " + (System.currentTimeMillis() - start) + "ms");
        return featureStructure;
    }

    public String[] Predict(String sentence)
    {
        return Predict(sentence, featureStructure);
    }

    public String[] Predict(String sentence, FeatureStructure featureStructure)
    {
        return Predict(sentence, featureStructure, null);
    }

    public String[] Predict(String sentence, FeatureStructure featureStructure, boolean[] booleans)
    {
        if(sentence == null || sentence.length() == 0)
            return null;
        String z = featureStructure.Decode(sentence, booleans);
        return DumpExample(sentence, z);
    }

    public boolean Learn(String segSentence)
    {
        return Learn(segSentence, featureStructure);
    }

    public boolean Learn(String segSentence, FeatureStructure featureStructure)
    {
        if(segSentence == null || segSentence.length() == 0)
            return false;
        String[] oriString = segSentence.trim().split("\\s+");
        // Note: 单独一个词的学习效果并不好，需要依赖上下文才能获得好的效果
        if(oriString.length < 2)
            return false;
        String x = StringUtils.join(oriString,"");
        String y = GenerateLabel(oriString);
        FeatureInstance featureInstances = new FeatureInstance(x);
        List<StringBuilder>[] featureBuffers = featureInstances.GetAllFeatures();
        String z = featureStructure.Decode(x);
        for (int j = 0; j < z.length(); j++)
        {
            int zj = z.charAt(j) - 48;
            int yj = y.charAt(j) - 48;
//            if(zj != yj) //尝试跳过相等的状态会使得学习结果变差，原因未深入，训练时跳过更明显
            {
                featureStructure.UpdateWeights(yj, featureBuffers[j], 1);
                featureStructure.UpdateWeights(zj, featureBuffers[j], -1);
            }
        }
        return true;
    }

    /**
     * 全量测试，全部特征参与测试
     * @param testFile
     */
    public void Test(String testFile)
    {
        Test(testFile, 0 , null);
    }

    public void Test(String testFile, boolean[] booleans)
    {
        Test(testFile, 0 , booleans);
    }

    public void Test(String testFile, float ratio)
    {
        Test(testFile, null, ratio, null, featureStructure);
    }

    public void Test(String testFile, String wrongExampleFile, float ratio)
    {
        Test(testFile, wrongExampleFile, ratio, null, featureStructure);
    }

    public void Test(String testFile, float ratio, boolean[] booleans)
    {
        Test(testFile, null, ratio, booleans, featureStructure);
    }

    public void Test(String testFile, String wrongExampleFile, float ratio, boolean[] booleans, FeatureStructure featureStructure)
    {
        Evaluator evaluator = new Evaluator(wrongExampleFile);
        FeatureStructure _featureStructure = new FeatureStructure(
                featureStructure.GetTransformProbility(), FeatureCompression(GetThreshold(ratio)));
        try
        (
            FileReader fileReader = new FileReader(new File(testFile));
            BufferedReader sb = new BufferedReader(fileReader);
        ){
            String line;
            while ((line = sb.readLine()) != null)
            {
                String[] oriString = line.trim().split("\\s+");
                if(oriString.length < 2) continue;
                String[] resString = Predict(StringUtils.join(oriString), _featureStructure, booleans);
                evaluator.Call(oriString, resString);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        _featureStructure.featureFreqMap.clear();
        _featureStructure = null;
        System.gc();
        System.out.println("======Test===== 特征权重裁剪压缩比值 " + ratio + " 特征选择：" + Arrays.toString(booleans));
        evaluator.Report();
    }

    public void SpeedTest(String sentence, int times)
    {
        SpeedTest(sentence, times, 0);
    }

    /**
     *
     * @param sentence 待分词句子
     * @param times     循环分词次数
     * @param ratio     模型特征裁剪比例
     */
    public void SpeedTest(String sentence, int times, float ratio)
    {
        FeatureStructure _featureStructure = new FeatureStructure(
                featureStructure.GetTransformProbility(), FeatureCompression(GetThreshold(ratio)));
        long start = System.currentTimeMillis();
        for (int i = 0; i < times; i++) {
            Predict(sentence, _featureStructure);
        }
        System.out.println(String.format("裁剪 " + ratio + " 循环 %d次，耗时： %d ms",
                times, (System.currentTimeMillis() - start)));
        System.out.println(Arrays.toString(Predict(sentence, _featureStructure)));
        _featureStructure.featureFreqMap.clear();
        _featureStructure = null;
    }

    /**
     * 生成标签序列字符串。逃逸分析，栈方法
     * @param words
     * @return
     */
    public String GenerateLabel(String[] words)
    {
        StringBuilder ysb = new StringBuilder();
        for(String word : words)
        {
            if(word.length() < 1)
                continue;
            else if(word.length() == 1)
            {
                ysb.append(3);
            }
            else
            {
                ysb.append(0);
                for(int i = 0 ;i < word.length() - 2; i++)
                    ysb.append(1);
                ysb.append(2);
            }
        }
        return new String(ysb);
    }

    /**
     * 根据原始句子和标签反向生成分词结果
     * @param x
     * @param y
     * @return
     */
    String[] DumpExample(String x, String y)
    {
        StringBuilder words = new StringBuilder();
        StringBuilder cache = new StringBuilder();
        for(int i = 0; i < x.length(); i++)
        {
            cache.append(x.charAt(i));
            if(y.charAt(i) == '2' || y.charAt(i) == '3')
            {
                words.append(cache).append(' ');
                cache.setLength(0);
            }
        }
        if(cache.length() > 0)
            words.append(cache);
        return new String(words).trim().split(" ");
    }

    /**
     * 建议选择全量特征才有效果，暴力搜索特征，不过特征之间并不是相互独立的，所以结果仅供参考
     * @param testFile 不要过大，建议 1 ~ 5 M
     * @param FeatureSize 总特征个数，目前为8个，Feature类注释掉了12个特征，全量20个
     * @param ignore 忽略前面ignore个特征
     */
    public void FeatureFilter(String testFile, int FeatureSize, int ignore)
    {
        for(boolean[] booleans:SegUtils.Generate(FeatureSize - ignore))
        {
            boolean[] _booleans = new boolean[booleans.length + ignore];
            for (int i = 0; i < ignore; i++)
                _booleans[i] = true;
            System.arraycopy(booleans,0, _booleans, ignore, booleans.length);
            Test(testFile, _booleans);
        }
    }

    public static void main(String[] args) throws IOException
    {
//        NOTE:　jdk8 训练速度会快些 VM 参数（不是必须）
//        -Xms3072m -Xmx3072m  -Xmn1536m -XX:SurvivorRatio=8
        System.out.println(new Date(System.currentTimeMillis()).toString());
        String devFile = ".\\data\\dev.dev";
        String testFile = ".\\data\\test.test";
        String trainFile = ".\\data\\train.train";
        String modelFile = ".\\data\\model\\model.ap";
        int iterate = 80;
        float ratio = 0;//压缩比例，建议在0.7 - 0.9, 0.8 左右最佳
        AveragePerceptronAlgorithm averagePerceptronAlgorithm = new AveragePerceptronAlgorithm();

        averagePerceptronAlgorithm.Train(trainFile, devFile, iterate);

        System.out.println("Test ++++++++++++++++++++++BEGIN");
        averagePerceptronAlgorithm.Test(testFile);
        averagePerceptronAlgorithm.Test(testFile, 0.80f);
        averagePerceptronAlgorithm.Test(testFile, 0.99f);
        System.gc();
        System.out.println("Test ++++++++++++++++++++++END");
        System.out.println("SpeedTest++++++++++++++++++++++BEGIN");
        String testStr = "由感知机衍生出来的一个重要算法是平均感知机——一种序列标注算法。同MIRA类似，平均感知机存在......";
        averagePerceptronAlgorithm.SpeedTest(testStr, 10000);
        averagePerceptronAlgorithm.SpeedTest(testStr, 10000, 0.75f);
        averagePerceptronAlgorithm.SpeedTest(testStr, 10000, 0.80f);
        averagePerceptronAlgorithm.SpeedTest(testStr, 10000, 0.85f);
        averagePerceptronAlgorithm.SpeedTest(testStr, 10000, 0.90f);
        System.out.println("SpeedTest ++++++++++++++++++++++END");

        System.out.println("SAVE MODEL ++++++++++++++++++++++");
        averagePerceptronAlgorithm.Save(modelFile, ratio);
        averagePerceptronAlgorithm.Save(".\\data\\model\\model70.ap", 0.75f);
        averagePerceptronAlgorithm.Save(".\\data\\model\\model80.ap", 0.80f);
        averagePerceptronAlgorithm.Save(".\\data\\model\\model95.ap", 0.85f);
        System.out.println(new Date(System.currentTimeMillis()).toString());
        //进行特征筛选
//        averagePerceptronAlgorithm.FeatureFilter(testFile, 8, 6);
    }
}

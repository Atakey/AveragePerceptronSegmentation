package APSeg;

import java.util.*;


/**
 * Created by Huanghl on 2019/4/10.
 */
public class FeatureStructure
{
    int step = 0;
    private final float lambda = 5e-7f;
    private final float P = 0.999999f;
    private final float LOG_P = (float) Math.log(P);
    /**
     * 不采用正则
     */
    private Pena pena = Pena.no_regu;
    Map<String, Float> featureFreqMap;
    private Random random = new Random();
    private boolean segFlag;

    private int[][] transformProbility;
    //以下为训练过程临时数据结构
    Map<String, Float> back_up;
    Map<String, Integer> timeStamp;
    Map<String, Float> averageFeatureMap;

    public FeatureStructure(Pena pena)
    {
        this.pena = pena;
        segFlag = false;
        timeStamp = new FeatureMap<>();
        transformProbility = new int[4][4];
        featureFreqMap = new FeatureMap<>();
        averageFeatureMap = new FeatureMap<>();
    }

    public FeatureStructure(int[][] transformProbility, Map<String, Float> featureFreqMap)
    {
        segFlag = true;
        timeStamp = new FeatureMap<>();
        averageFeatureMap = new FeatureMap<>();
        this.transformProbility = transformProbility;
        this.featureFreqMap = new FeatureMap<>(featureFreqMap);
    }

    /**
     * 采用的正则方式
     */
    enum Pena
    {
        no_regu,
        l1_regu,//速度及效果都比 no_regu 差一点点
        l2_regu;//速度及效果都比 no_regu 差一点点
    }

    public int GetTotalSize()
    {
        return featureFreqMap.size();
    }

    public int[][] GetTransformProbility() {
        return transformProbility;
    }

    void UpdateTransformProbility(int from, int to, int delta)
    {
        if(delta == 1)
            transformProbility[from][to] ++;
        else
            transformProbility[from][to] --;
    }

    void UpdateAll()
    {
        for (String key : featureFreqMap.keySet())
            UpdateFeature(key);
    }

    private Float UpdateFeature(String key)
    {
        return UpdateFeature(key, featureFreqMap.get(key));
    }

    private Float UpdateFeature(String key, Float value)
    {
        Integer updateTime = timeStamp.get(key);
        // test、predict 模式需要
        if(null == updateTime)
            return value;

        Float newValue;
        Float penaValue = averageFeatureMap.get(key);
//        penaValue = null == penaValue ? 0 : penaValue;
        int dStep = step - updateTime;
        switch (pena)
        {
            case l1_regu:
                float dValue = dStep * lambda;
                newValue = Math.max(0, Math.abs(value) - dValue) * (value > 0 ? 1 : -1);
                if (newValue.equals(0))
                    averageFeatureMap.put(key, penaValue + value * value / lambda / 2);
                else
                    averageFeatureMap.put(key, penaValue + (value + newValue) * dStep / 2);
                featureFreqMap.put(key, newValue);
                break;

            case l2_regu:
                newValue =  value * (float)Math.exp(dStep * LOG_P);
                averageFeatureMap.put(key, penaValue + value * (float)(1 - Math.exp(dStep * LOG_P)) / (1 - P));
                featureFreqMap.put(key, newValue);
                break;

            default:
                newValue = value;
//                Float _value = averageFeatureMap.get(key);
//                averageFeatureMap.put(key, null == _value ? value * dStep : _value + value * dStep);
                averageFeatureMap.put(key, penaValue + value * dStep);
        }
        timeStamp.put(key, step);
        return newValue;
    }

    /**
     * 平均处理
     */
    void Average()
    {
        back_up = new FeatureMap<>(featureFreqMap);
        for(String key:averageFeatureMap.keySet())
            featureFreqMap.put(key, averageFeatureMap.get(key)/ step);
    }

    /**
     * 取消平均处理
     */
    void Unaverage()
    {
        featureFreqMap = new FeatureMap<>(back_up);
        back_up.clear();
        back_up = null;
    }

    void UpdateWeights(int state, List<StringBuilder> featureBuffer, int delta)
    {
        for(String feature:FeatureInstance.GenerateKey(state, featureBuffer))
        {
             //未登录词的特征占位
//             UnknownFeature(feature, delta);
            Float value = featureFreqMap.get(feature);
            if(null == value)
            {
                value = 0f;
//                featureFreqMap.put(feature, value);
                averageFeatureMap.put(feature, value);
                timeStamp.put(feature, step);
            }
            else
                value = UpdateFeature(feature, value);
            featureFreqMap.put(feature, value + delta);
        }
    }

    //未登录词的特征处理, 处理与否主要看模型的泛化能力是否有提升
    private float UnknownFeature(String feature, int delta)
    {
        String key = new String(new StringBuffer(feature.substring(0, 1)).append(SegUtils.UNKNOWN));
        Float value = featureFreqMap.get(key);
        if(null == value)
        {
            featureFreqMap.put(key, Float.valueOf(delta));
            return 0f;
        }
        else
            return  featureFreqMap.put(key, value + delta);
    }

    float GetValue(String key)
    {
        Float value = featureFreqMap.get(key);
            return null == value ? 0f : segFlag ? value : UpdateFeature(key, value);
//        return null == value ? UnknownFeature(key, 0):UpdateFeature(key, value);
    }

    private float[][] Emissions(String x, int xlen, int slen, boolean[] booleans)
    {
        float[][] emissions = new float[xlen][slen];
        FeatureInstance featureInstance = new FeatureInstance(x);
        List<StringBuilder>[] featureBuffers = featureInstance.GetAllFeatures();
        for (int i = 0; i < xlen; i++)
        {
            for (int j = 0; j < slen; j++)
            {
                float sum = 0;
                List<String> features = FeatureInstance.GenerateKey(j, featureBuffers[i], booleans);
                for (String feature:features)
                {
                    //增加模型的鲁棒性，未登录词的另一种处理方式，假设未登录词的比例为0.01
                    if(segFlag || random.nextFloat() > 0.01)
                        sum += GetValue(feature);
                }
                emissions[i][j] = sum;
            }
        }
        return emissions;
    }

    /**
     * 滚动数组版viterbi
     * @param x
     * @return
     */
    String Decode(String x)
    {
        return Decode(x, null);
    }

    /**
     * 特征选择时调用依赖的viterbi分词Decode，滚动数组版viterbi
     * @param x 未分词句子
     * @param booleans 选择的特征
     * @return
     */
    String Decode(String x, boolean[] booleans)
    {
        char[] xChar = x.toCharArray();
        int xlen = xChar.length;
        int slen = transformProbility[0].length;
        float[][] emissions = Emissions(x, xlen, slen, booleans);
        //采用滚动数组以加速decode以及减少内存占用
        float [][] cost = new float[2][slen];
        int [][] routes = new int[xlen][slen];
        System.arraycopy(emissions[0],0, cost[0],0, slen);
        int index_i = 0;
        for(int i = 1 ; i < xlen; i++)
        {
            index_i = i & 1;
            int index_i_1 = 1 - index_i;
            for(int j = 0 ; j < 4 ; j++)
            {
                int state = 3; // 默认 S 状态
                float emission = emissions[i][j];
                float prob = -Float.MAX_VALUE;
                for(int k = 0; k < 4; k++)
                {
//                    if(transformProbility[k][j] < 0) continue;//NOTE 此步优化跳过几乎没有提升，训练时采用此步使得收敛巨慢或无法收敛
                    float _prob = cost[index_i_1][k] + transformProbility[k][j] + emission;
                    if(prob < _prob)
                    {
                        prob = _prob;
                        state = k;
                    }
                }
                cost[index_i][j] = prob;
                routes[i][j] = state;
            }
        }

        StringBuilder res = new StringBuilder();
        int maxIndex = getMaxValue(cost[index_i]);//获取最后一行最小值所在的索引
        for(int y = xlen - 1; y > -1 ; y --)
        {
            res.append(maxIndex);
            maxIndex = routes[y][maxIndex];
        }
        return new String(res.reverse());
    }

    private int getMaxValue(float[] floatList)
    {
        int index = 3;//默认Single状态
        float value = 0f;
        for(int i = 0; i < floatList.length; i ++)
        {
            if(floatList[i] > value)
            {
                value = floatList[i];
                index = i;
            }
        }
        return index;
    }
}
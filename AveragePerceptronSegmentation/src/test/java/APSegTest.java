import APSeg.AveragePerceptronAlgorithm;
import com.madhukaraphatak.sizeof.SizeEstimator;
import org.apache.commons.lang.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.Date;

/**
 * Created by Huanghl on 2019/4/9.
 */
public class APSegTest
{
    private AveragePerceptronAlgorithm averagePerceptronAlgorithm = new AveragePerceptronAlgorithm();

    /**
     * 分词入口测试
     * @param sentence 待分词的句子
     * @return
     */
    protected String[] SegTest(String sentence)
    {
        return averagePerceptronAlgorithm.Predict(sentence);
    }

    /**
     * 文件分词速度测试
     * @param testFile
     * @throws Exception
     */
    protected void FileSpeedTest(String testFile) throws Exception
    {
        try
        (
            FileReader fileReader = new FileReader(new File(testFile));
            BufferedReader sb = new BufferedReader(fileReader);
        ){
            String line;
            long start = System.currentTimeMillis();
            while ((line = sb.readLine()) != null)
            {
                String[] oriString = line.trim().split("\\s+");
                if(oriString.length < 2) continue;
                averagePerceptronAlgorithm.Predict(StringUtils.join(oriString));
            }
            System.out.println("文件分词耗时：" + (System.currentTimeMillis() - start) + " ms");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * 在线学习入口
     * @param segSentence 如"南京市 长江 大桥 。"
     * @param times 学习次数
     */
    protected void Learn(String segSentence, int times)
    {
        int learn_Times = 0;
        while(learn_Times < times)
        {
            if(JudgeTheSame(SegTest(StringUtils.join(segSentence.split("\\s+"), "")),segSentence.split("\\s+")))
            {
                System.out.println("学习提前退出~ " + learn_Times);
                break;
            }
            learn_Times++;
            averagePerceptronAlgorithm.Learn(segSentence);
        }
    }
    private boolean JudgeTheSame(String[] strings1, String[] strings2)
    {
        int size = strings1.length;
        if(size != strings2.length)
            return false;
        for (int i = 0; i < size; i++) {
            if(strings1[i].equals(strings2[i]))
                continue;
            return false;
        }
        return true;
    }

    protected void SpeedTest(String sentence, int times)
    {
        long start = System.currentTimeMillis();
        for (int i = 0; i < times; i++) {
            averagePerceptronAlgorithm.Predict(sentence);
        }
        System.out.println("循环" + times + "次， 总耗时 " + (System.currentTimeMillis() - start) + " ms");
    }

    /**
     *
     * @param testFile 已分词的待测试文件
     * @param wrongExamplesFile 错误样本的写入的文件，参数为null代表不写入,以供后续分析
     */
    protected void AccTest(String testFile, String wrongExamplesFile, float ratio)
    {
        averagePerceptronAlgorithm.Test(testFile, wrongExamplesFile, ratio);
    }

    /**
     * 建议选择全量特征才有效果，暴力搜索特征，
     * @param testFile 不要过大，建议 1 ~ 5 M
     */
    protected void FeatureFilter(String testFile, int FeatureSize, int ignore)
    {
        averagePerceptronAlgorithm.FeatureFilter(testFile, FeatureSize, ignore);
    }

    public static void main(String[] args) throws Exception
    {
        APSegTest test = new APSegTest();
        System.out.println(new Date(System.currentTimeMillis()).toString());
        String testFile = ".\\data\\dev.dev";
        String modelFile = ".\\data\\model\\model81.ap";
        String sentence = "1、2019年12月28日开始各部门负责人要将TOC、ABC工具在部门内落地，人力部门分享绩效汇报PPT；" ;
//        +
//                "你讲的这个故事好好笑，让我哈哈笑。" +
//                "这件衣服看来看去都没有什么特别的。" +
//                "来呀，看一看，瞧一瞧，走过路过不要错过。" +
//                "你一次次地说要过来看我，结果呢？" +
//                "这件事看看吧，结果不好说。" +
//                "一次一次地说，不要急。" +
//                "花花绿绿的世界让我赏心悦目。" +
//                "小邑目前的功能还在完善中" +
//                "两化融合政策尚未实施。" +
//                "李大钊讲的这个笑话一点儿也不好笑。";
        test.averagePerceptronAlgorithm.LoadModel(modelFile);
        System.out.println(Arrays.toString(test.SegTest(sentence)));
        System.out.printf("类型：%s，占用内存：%.2f MB\n", test.averagePerceptronAlgorithm.getClass().getSimpleName(),
                SizeEstimator.estimate(test.averagePerceptronAlgorithm) / 1024D / 1024D);
        int times = 1_0000;
        test.SpeedTest(sentence, times);
//        test.FileSpeedTest(testFile);
//        test.AccTest(testFile,null,0);
//        test.AccTest(testFile,null,0.81f);
//        test.averagePerceptronAlgorithm.Save(".\\data\\model\\model82.ap",0.82f);
//        test.AccTest(testFile,null, 0.82f);

//        String oriSentence = "二零一九年12月28日开始各部门负责人要将TOC、ABC工具在部门内落地。你看一看隔壁家的小吴，读书多认真，成绩顶呱呱。";
//        System.out.println(Arrays.toString(test.SegTest(oriSentence)));
//
//        String segSentence = "二零一九年 12月28日 开始 各 部门 负责人 要 将 TOC 、 ABC 工具 在 部门 内 落地 。 你 看一看 隔壁家 的 小吴 ， 读书 多 认真 ， 成绩 顶呱呱 。";
//        test.Learn(segSentence, 1);
//        System.out.println();
//
//        System.out.println(Arrays.toString(test.SegTest(sentence)));
//        //进行特征筛选
//        test.FeatureFilter(testFile, 3, 0);
    }
}

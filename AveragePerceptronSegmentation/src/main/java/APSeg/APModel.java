package APSeg;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by Huanghl on 2019/4/9.
 */
public class APModel
{
    private static volatile APModel model;

    FeatureStructure featureStructure;
    final UserDictTrieTree userDictTrieTree = new UserDictTrieTree();
    private final Set<String> loadedPath = new HashSet<>();

    private AveragePerceptronAlgorithm averagePerceptronAlgorithm = new AveragePerceptronAlgorithm();

    private static String USER_DICT = ".dict";//以dict为结尾的所有用户字典文件均读取
    private final String modelFile = "model81.ap"; //裁剪比例81
    public static String resPath = "./data/model/";

    private static Logger logger = Logger.getLogger(APModel.class);

    private APModel(){this.loadDict();}

    private void loadDict()
    {
        try
        {
            this.featureStructure = averagePerceptronAlgorithm.LoadModel(resPath + modelFile);
        }
        catch (Exception e)
        {
            logger.error("fail to init APmodel, cause by " + e);
        }
    }

    public static APModel getInstanse()
    {
        if (model == null)
        {
            synchronized (APModel.class)
            {
                if (model == null)
                {
                    model = new APModel();
                    return model;
                }
            }
        }
        return model;
    }

    public void init(String absPath)
    {
        resPath = absPath;
        logger.info("initialize user dictionary:" + absPath);
        synchronized (APModel.class)
        {
            if (loadedPath.contains(absPath))
                return;
            DirectoryStream<Path> stream;
            try
            {
                Path confPath = Paths.get(absPath);
                stream = Files.newDirectoryStream(confPath, String.format(Locale.getDefault(), "*%s", USER_DICT));
                for (Path path: stream){
                    logger.error(String.format(Locale.getDefault(), "loading dict %s", path.toString()));
                    model.loadUserDict(path);
                }
                loadedPath.add(absPath);
            } catch (IOException e)
            {
                logger.error(String.format(Locale.getDefault(), "%s: load user dict failure!", absPath));
            }
        }
    }

    public void loadUserDict(Path userDict)
    {
        loadUserDict(userDict, StandardCharsets.UTF_8);
    }

    private void loadUserDict(Path userDict, Charset charset)
    {
        try
        (
            BufferedReader br = Files.newBufferedReader(userDict, charset);
        ){
            long s = System.currentTimeMillis();
            int count = 0;
            while (br.ready())
            {
                String line = br.readLine();
                String[] tokens = line.split("[\t ]+");
                if (tokens.length < 2)
                    continue;
                String word = tokens[0];
                userDictTrieTree.Insert(word);
            }
            logger.info(String.format(Locale.getDefault(),
                    "user dict %s load finished, tot words:%d, time elapsed:%dms",
                    userDict.toString(), count, System.currentTimeMillis() - s));
            br.close();
        }
        catch (IOException e)
        {
            logger.error(String.format(Locale.getDefault(), "%s: load user dict failure!", userDict.toString()));
        }
    }

    void addUserWord(String word)
    {
        userDictTrieTree.Insert(word);
    }

    /**
     *
     * @param segSentence 例如："南京 市长 江大桥"，一个或多个空格分开
     * @return 是否学习成功
     */
    boolean Learn(String segSentence)
    {
        return averagePerceptronAlgorithm.Learn(segSentence, featureStructure);
    }
}

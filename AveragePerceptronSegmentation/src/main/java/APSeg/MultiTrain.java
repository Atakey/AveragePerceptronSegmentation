package com.etom.etb.wordsplit.APSeg;

import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by Administrator on 2019/5/30.
 */
public class MultiTrain
{
    private Logger logger = Logger.getLogger(MultiTrain.class);
    private int threadNum = 1;
    private String trainFile;
    private String devFile;
    private int iterate;

    private final String tempFile = "tempTrain-";
    private final String modelFile = "model.ap";


    public MultiTrain(String trainFile, String devFile, int iterate, int threadNum)
    {
        this.trainFile = trainFile;
        this.devFile = devFile;
        this.iterate = iterate;
        this.threadNum = threadNum;
    }

    private List<String> readFile(String trainFile)
    {
        List<String> examples = new ArrayList<>();
        try
        (
            FileReader fileReader = new FileReader(new File(trainFile));
            BufferedReader sb = new BufferedReader(fileReader);
        ) {
            String line;
            while ((line = sb.readLine()) != null)
                examples.add(line.trim());
        }
        catch (Exception e) {
            logger.error(e);
        }
        return examples;
    }

    private void splitFile()
    {
        List<String> examples = readFile(trainFile);
        int size = examples.size();
        int perSize = size / threadNum;

        for (int i = 0; i < threadNum; i++)
        {
            File file = new File( tempFile + i + ".txt");
            try
            (
                FileWriter fileWriter = new FileWriter(file);
            ){
                for (int j = 0; j < perSize; j++)
                {
                    if(j + i*perSize < size)
                        fileWriter.write(examples.get(j + i*perSize) + System.lineSeparator());
                }
            }
            catch (Exception e){}
        }
    }

    public FeatureStructure combineFeatureStructure(FeatureStructure ... featureStructures)
    {
        if(featureStructures.length == 1) return featureStructures[0];
        Map<String, Float> finalMap = new FeatureMap<>();
        int[][] transP = new int[4][4];
        Set<String> keySet = new HashSet<>();
        for(FeatureStructure featureStructure:featureStructures)
        {
            keySet.addAll(featureStructure.featureFreqMap.keySet());
        }
        Float value;
        for(String key:keySet)
        {
            float sum = 0f;
            int count = 0;
            for(FeatureStructure featureStructure:featureStructures)
            {
                value = featureStructure.featureFreqMap.get(key);
                if(value != null)
                {
                    sum += value;
                    count++;
                }
            }
            finalMap.put(key, sum/count);
        }
        //转移矩阵
        for(FeatureStructure featureStructure:featureStructures)
        {
            int[][] _transP = featureStructure.GetTransformProbility();
            for (int i = 0; i < _transP.length; i++)
            {
                for (int j = 0; j < _transP.length; j++)
                {
                    transP[i][j] += _transP[i][j];
                }
            }
        }

        for (int i = 0; i < transP.length; i++)
        {
            for (int j = 0; j < transP.length; j++)
            {
                transP[i][j] = transP[i][j] / featureStructures.length;
            }
        }

        return new FeatureStructure(transP, finalMap);
    }

    private FeatureStructure train()
    {
        splitFile();
        List<TrainThread> threads = new ArrayList<>();
        for (int i = 0; i < threadNum; i++)
        {
            threads.add(new TrainThread(tempFile + i + ".txt", devFile, iterate));
        }
        for (Thread thread : threads) {
            thread.start();
        }
        FeatureStructure[] featureStructures = new FeatureStructure[threadNum];
        for (int i = 0; i < threadNum; i++)
        {
            try {
                threads.get(i).join();
                featureStructures[i] = threads.get(i).call();
            }
            catch (Exception e)
            {logger.error("callBack errors");}
        }
        return combineFeatureStructure(featureStructures);
    }

    public void save(FeatureStructure featureStructure, String modelFile, final float ratio)
    {
        AveragePerceptronAlgorithm ap = new AveragePerceptronAlgorithm();
        ap.featureStructure = featureStructure;
        try {
            ap.Save(modelFile, ratio);
        }
        catch (Exception e){}
    }

    class TrainThread extends Thread implements Callable
    {
        AveragePerceptronAlgorithm ap;
        private String trainFile;
        private String devFile;
        private int iterate;

        public TrainThread(String trainFile, String devFile, int iterate)
        {
            super();
            ap = new AveragePerceptronAlgorithm();
            this.trainFile = trainFile;
            this.devFile = devFile;
            this.iterate = iterate;
        }

        public void run()
        {
            ap.Train(trainFile, devFile, iterate);
        }

        @Override
        public FeatureStructure call() throws Exception
        {
            File file = new File(trainFile);
            file.delete();
            return ap.featureStructure;
        }
    }

    public static void main(String[] args)
    {
        int iterate = 24;
        int threadNum = 2;
        String devFile = ".\\AP分词\\dev.dev";
        String testFile = ".\\AP分词\\test.test";
        String trainFile = ".\\AP分词\\train.train";
        String modelFile = ".\\AP分词\\model\\model1.ap";

        MultiTrain multiTrain = new MultiTrain(trainFile, devFile, iterate, threadNum);
        FeatureStructure featureStructure = multiTrain.train();
        AveragePerceptronAlgorithm averagePerceptronAlgorithm = new AveragePerceptronAlgorithm();
        averagePerceptronAlgorithm.featureStructure = featureStructure;
        averagePerceptronAlgorithm.Test(testFile);
        System.out.println("====================================");
        AveragePerceptronAlgorithm ap = new AveragePerceptronAlgorithm();
        ap.Train(trainFile, devFile, iterate);
        ap.Test(testFile);
    }
}

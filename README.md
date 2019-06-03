# AveragePerceptronSegmentation
Chinese Segmentation Use Average Perceptron Algorithm. 参考了python版https://github.com/zhangkaixu/minitools/blob/master/cws.py 的实现。

数据来源混合了98年、14年人民日报语料、MSR以及通用平衡语料4个语料库，由于语料库存在着差异，合适的特征选择训练集最多可达97%的准确率，考虑到过多的特征带来分词速度下降的影响，只选择了3个特征，训练集及测试集达到约94%的准确率。速度及性能表现均不错。

提供完整的训练测试方法、分词方法，以及用户字典。

训练方法
1）参考AveragePerceptronAlgorithm 中的 main函数
    该类仅支持单线程训练，在i5-6500,-Xms3072m -Xmx3072m  -Xmn1536m -XX:SurvivorRatio=8 VM参数下，133M的训练文件(实际约93M，因为训练过程每个迭代随机丢弃30%的样本)约3分钟一次迭代。
2）参考 MultiTrain 中的 main函数
    多线程训练入口，参看main函数，训练时需要根据训练文件的大小分配合适足够的内存。
    
分词入口
AveragePerceptronSeg averagePerceptronSeg = new AveragePerceptronSeg();
        averagePerceptronSeg.init(path);
        averagePerceptronSeg.process(sentece);
        //averagePerceptronSeg.sentenceProcess(sentece);

如果对你有帮助，请给个star （:

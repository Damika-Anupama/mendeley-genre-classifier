package org.mendeley;

import org.apache.spark.sql.*;
import org.apache.spark.ml.Pipeline;
import org.apache.spark.ml.PipelineModel;
import org.apache.spark.ml.PipelineStage;
import org.apache.spark.ml.feature.Tokenizer;
import org.apache.spark.ml.feature.StopWordsRemover;
import org.apache.spark.ml.feature.Word2Vec;
import org.mendeley.transformer.*;
import org.mendeley.map.Column;
import org.mendeley.map.StringIndexer;
import org.apache.spark.ml.classification.LogisticRegression;
import org.apache.spark.ml.classification.MultilayerPerceptronClassifier;
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator;
import org.apache.spark.ml.classification.RandomForestClassifier;
import org.apache.spark.ml.feature.HashingTF;

public class Main {
    private static final String RELEASE_DATE_COLUMN = "release_date";

    public static void main(String[] args) {
        SparkSession spark = SparkSession.builder()
                .appName("MendeleyGenreClassifier")
                .master("local[*]")
                .config("spark.serializer", "org.apache.spark.serializer.JavaSerializer")
                .config("spark.kryo.unsafe", "false")
                .getOrCreate();

        Dataset<Row> data = spark.read().format("csv")
                .option("header", "true")
                .load("data/original_song_dataset.csv");
        data = data.select("artist_name", "track_name", RELEASE_DATE_COLUMN, "genre", "lyrics");
        data = data.withColumn(RELEASE_DATE_COLUMN, functions.col(RELEASE_DATE_COLUMN).cast("int"));
        data = data.withColumnRenamed("lyrics", Column.VALUE.getName());
        data = StringIndexer.apply(data);

        // prepare lyrics using NLP pipeline. Result: lyrics_vec (vector column)

        Cleanser cleanser = new Cleanser();

        Numerator numerator = new Numerator();

        Tokenizer tokenizer = new Tokenizer()
                .setInputCol(Column.CLEAN.getName())
                .setOutputCol(Column.WORDS.getName());

        StopWordsRemover stopWordsRemover = new StopWordsRemover()
                .setInputCol(Column.WORDS.getName())
                .setOutputCol(Column.FILTERED_WORDS.getName());

        Exploder exploder = new Exploder();

        Stemmer stemmer = new Stemmer();

        Uniter uniter = new Uniter();
        Verser verser = new Verser();

        HashingTF hashingTF = new HashingTF()
                .setInputCol(Column.VERSE.getName())
                .setOutputCol("features")
                .setNumFeatures(5000);
    

        Pipeline pipeline = new Pipeline().setStages(new PipelineStage[]{
                cleanser,
                numerator,
                tokenizer,
                stopWordsRemover,
                exploder,
                stemmer,
                uniter,
                verser,
                hashingTF,
        });

        // Fit the pipeline (NLP + word2vec)
        PipelineModel model = pipeline.fit(data);

        // Transform dataset to get features
        Dataset<Row> processed = model.transform(data).select("features", "label");

        // print
        processed.show();

        // Split into train/test sets
        Dataset<Row>[] splits = processed.randomSplit(new double[]{0.8, 0.2}, 1234);
        Dataset<Row> trainData = splits[0];
        Dataset<Row> testData = splits[1];

        MultilayerPerceptronClassifier mlp = new MultilayerPerceptronClassifier()
                .setFeaturesCol("features")
                .setLabelCol("label")
                .setLayers(new int[]{5000, 100, 50, 7}) // Replace `featuresDim` with your feature vector size
                .setBlockSize(128)
                .setMaxIter(100)
                .setSeed(1234);
        
        Dataset<Row> predictions = mlp.fit(trainData).transform(testData);

        // Evaluate
        MulticlassClassificationEvaluator evaluator = new MulticlassClassificationEvaluator()
                .setLabelCol("label")
                .setPredictionCol("prediction")
                .setMetricName("accuracy");

        double accuracy = evaluator.evaluate(predictions);
        System.out.println("Test Accuracy = " + accuracy);
    }
}

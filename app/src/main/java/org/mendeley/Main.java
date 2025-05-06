package org.mendeley;

import org.apache.spark.sql.*;
import org.apache.spark.sql.functions;

public class Main {
    public static void main(String[] args) {
        SparkSession spark = SparkSession.builder().appName("MendeleyGenreClassifier").master("local[*]").getOrCreate();

        // Load dataset
        Dataset<Row> data = spark.read().format("csv").option("header", "true").load("data/songdata.csv");

        // Preprocess: Drop nulls, cast year to integer
        // data = data.dropna().withColumn("release_year", functions.col("release_year").cast("int"));

        data.show();
    }
}

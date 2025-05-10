package org.mendeley.map;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.functions;
import org.apache.spark.sql.api.java.UDF1;
import org.apache.spark.sql.types.DataTypes;

import java.util.HashMap;
import java.util.Map;

public class StringIndexer {

    public static Dataset<Row> apply(Dataset<Row> df) {
        // Define a mapping from genre strings to numerical indices
        Map<String, Integer> genreMap = new HashMap<>();
        genreMap.put("pop", 0);
        genreMap.put("country", 1);
        genreMap.put("blues", 2);
        genreMap.put("jazz", 3);
        genreMap.put("reggae", 4);
        genreMap.put("rock", 5);
        genreMap.put("hip hop", 6);
        genreMap.put("folk", 7);

        // Define a UDF to map genre strings to indices
        UDF1<String, Integer> genreToIndex = new UDF1<String, Integer>() {
            @Override
            public Integer call(String genre) {
                if (genre == null) return -1;
                String normalized = genre.trim().toLowerCase();
                return genreMap.getOrDefault(normalized, -1);
            }
        };

        // Register the UDF
        df.sparkSession().udf().register("genreToIndex", genreToIndex, DataTypes.IntegerType);

        // Apply the UDF to create a new column 'label'
        df = df.withColumn("label", functions.callUDF("genreToIndex", functions.col("genre")));
        return df;
    }
}

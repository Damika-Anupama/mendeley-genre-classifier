package org.mendeley.map;

import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.tartarus.snowball.SnowballStemmer;
import java.util.Arrays;

public class StemmingFunction implements MapFunction<Row, Row> {

    private SnowballStemmer initializeStemmer() {
        try {
            Class stemClass = Class.forName("org.tartarus.snowball.ext.englishStemmer");

            return (SnowballStemmer) stemClass.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public Row call(Row input) throws Exception {
        String word = input.getAs("filteredWord");
    
        SnowballStemmer stemmer = initializeStemmer();
        stemmer.setCurrent(word);
        stemmer.stem();
        String stemmed = stemmer.getCurrent();
    
        // Handle optional fields robustly
        String id = null;
        Integer rowNumber = null;
        Double label = null;
    
        if (Arrays.asList(input.schema().fieldNames()).contains("id")) {
            Object rawId = input.getAs("id");
            if (rawId != null) id = rawId.toString();
        }
    
        if (Arrays.asList(input.schema().fieldNames()).contains("rowNumber")) {
            Object rawRow = input.getAs("rowNumber");
            if (rawRow instanceof Number) {
                rowNumber = ((Number) rawRow).intValue();
            }
        }
    
        if (Arrays.asList(input.schema().fieldNames()).contains("label")) {
            Object rawLabel = input.getAs("label");
            if (rawLabel instanceof Number) {
                label = ((Number) rawLabel).doubleValue();
            }
        }
    
        return RowFactory.create(id, rowNumber, label, stemmed);
    }
    
}

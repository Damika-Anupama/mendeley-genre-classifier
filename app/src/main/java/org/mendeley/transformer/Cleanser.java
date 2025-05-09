package org.mendeley.transformer;

import java.io.IOException;
import java.util.UUID;
import org.apache.spark.ml.Transformer;
import org.apache.spark.ml.param.ParamMap;
import org.apache.spark.ml.util.*;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import static org.apache.spark.sql.functions.*;
import org.apache.spark.sql.types.StructType;
import org.mendeley.map.Column;
import org.apache.spark.sql.types.StructField;

public class Cleanser extends Transformer implements MLWritable {

    private String uid;

    public Cleanser(String uid) {
        this.uid = uid;
    }

    public Cleanser() {
        this.uid = "Cleanser" + "_" + UUID.randomUUID().toString();
    }

    @Override
    public Dataset<Row> transform(Dataset<?> sentences) {
        // Remove all punctuation symbols.
        sentences = sentences.withColumn(Column.CLEAN.getName(), regexp_replace(trim(column(Column.VALUE.getName())), "[^\\w\\s]", ""));
        sentences = sentences.drop(Column.VALUE.getName());

        // Remove double spaces.
        return sentences.withColumn(Column.CLEAN.getName(), regexp_replace(column(Column.CLEAN.getName()), "\\s{2,}", " "));
    }

    @Override
    public StructType transformSchema(StructType schema) {
        return new StructType(new StructField[]{
                Column.LABEL.getStructType(),
                Column.CLEAN.getStructType()
        });
    }

    @Override
    public Transformer copy(ParamMap extra) {
        return super.defaultCopy(extra);
    }

    @Override
    public String uid() {
        return this.uid;
    }

    @Override
    public MLWriter write() {
        return new DefaultParamsWriter(this);
    }

    public static MLReader<Cleanser> read() {
        return new DefaultParamsReader<>();
    }

    @Override
    public void save(String path) throws IOException {
        write().save(path);
    }
}


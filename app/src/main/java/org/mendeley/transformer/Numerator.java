package org.mendeley.transformer;

import static org.mendeley.map.Column.ID;
import static org.mendeley.map.Column.ROW_NUMBER;
import java.io.IOException;
import java.util.UUID;
import org.apache.spark.ml.Transformer;
import org.apache.spark.ml.param.ParamMap;
import org.apache.spark.ml.util.*;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.expressions.Window;
import org.apache.spark.sql.functions;
import org.apache.spark.sql.types.StructType;

public class Numerator extends Transformer implements MLWritable {

    private String uid;

    public Numerator(String uid) {
        this.uid = uid;
    }

    public Numerator() {
        this.uid = "Numerator" + "_" + UUID.randomUUID().toString();
    }

    @Override
    public Dataset<Row> transform(Dataset<?> sentences) {
        if (!java.util.Arrays.asList(sentences.schema().fieldNames()).contains(ID.getName())) {
            sentences = sentences.withColumn(ID.getName(), functions.monotonically_increasing_id());
        }
    
        return sentences.withColumn(ROW_NUMBER.getName(),
                functions.row_number().over(Window.orderBy(ID.getName())));
    }
    

    @Override
    public StructType transformSchema(StructType schema) {
        return schema.add(ROW_NUMBER.getStructType());
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

    @Override
    public void save(String path) throws IOException {
        write().save(path);
    }

    public static MLReader<Numerator> read() {
        return new DefaultParamsReader<>();
    }

}

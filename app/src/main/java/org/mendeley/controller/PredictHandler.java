package org.mendeley.controller;

import org.apache.spark.ml.PipelineModel;
import org.apache.spark.sql.*;
import org.apache.spark.ml.linalg.Vector;
import org.apache.spark.sql.types.*;
import java.io.*;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

public class PredictHandler extends HttpServlet {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final SparkSession spark = SparkSession.builder()
            .appName("MendeleyGenrePredictor")
            .master("local[*]")
            .getOrCreate();
    private static final PipelineModel model;
    static {
        String modelPath = System.getProperty("model.path", "model"); // Default to "model" if not provided
        System.out.println("Loading model from: " + new File(modelPath).getAbsolutePath());
        model = PipelineModel.load(modelPath) ;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        BufferedReader reader = req.getReader();
        StringBuilder json = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            json.append(line);
        }

        Map<String, String> inputMap = mapper.readValue(json.toString(), Map.class);
        String lyrics = inputMap.get("lyrics");

        List<Row> rows = Collections.singletonList(RowFactory.create("user_input", lyrics, -1.0));
        StructType schema = new StructType(new StructField[]{
                new StructField("id", DataTypes.StringType, false, Metadata.empty()),
                new StructField("value", DataTypes.StringType, false, Metadata.empty()),
                new StructField("label", DataTypes.DoubleType, false, Metadata.empty())
        });

        Dataset<Row> df = spark.createDataFrame(rows, schema);
        Dataset<Row> transformed = model.transform(df);

        ObjectNode result = mapper.createObjectNode();
        double[] probabilities = ((Vector) transformed.first().getAs("probability")).toArray();

        String[] genreLabels = new String[]{"pop", "country", "blues", "jazz", "reggae", "rock", "hip hop", "folk"};
        for (int i = 0; i < genreLabels.length; i++) {
            result.put(genreLabels[i], probabilities[i]);
        }

        resp.setContentType("application/json");
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write(result.toString());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "GET method not supported.");
    }    
}

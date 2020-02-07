package com.bill.BackHaul;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.apache.commons.lang3.ArrayUtils;
import com.opencsv.CSVWriter;
import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        String TrainingSetDir = System.getProperty("user.home") + "/Desktop/Training_Set/";
        String CsvFilePath = System.getProperty("user.home") + "/Training_set.csv";
        File TrainingSet = new File(TrainingSetDir); //Training set directory
        String[] files = TrainingSet.list(); // file becomes a list with every csv file
        CSVWriter csvWriter = null;
        if (TrainingSet.exists()) {
            try {
                /**start reading the csv files with foreach loop, construct the training set and save it to newly created csvwriter file*/
                csvWriter = new CSVWriter(new FileWriter(CsvFilePath));
                for (String csv : files) {
                    csvWriter.writeNext(readAllDataAtOnce(TrainingSetDir + csv, CsvFilePath, csv));
                }
                /**close Training_Set.csv in order to be saved*/
                csvWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                /**first send the Training Set by selecting option 1*/
                Server server = new Server(6090, 1);
                server = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**Connection and interaction with mysql will be in one Thread*/
        Thread SqlInteract = new Thread(new DataLogsThread());
        SqlInteract.start();
    }

    public static String[] readAllDataAtOnce(String file, String fileName, String File) {
        int rowCount = 0;
        int colCount = 0;
        double tempEntropy = 0.0;
        List<String[]> allData = null;
        ArrayList<Double> dataFrame = new ArrayList<Double>();
        String[] featureVector = null;
        try {
            // Create an object of file reader
            // class with CSV file as a parameter.
            FileReader filereader = new FileReader(file);
            // create csvReader object and skip first Line
            CSVReader csvReader = new CSVReaderBuilder(filereader).build();
            allData = csvReader.readAll(); // read all data from file
            List<String> array = new LinkedList<String>();


            array.add(File + "\t");//first cell will be the experiment's name

            /**for each row in the csv we skip the first because it is the names
            * and we parse the relavant cell Double value*/
            for (int i = 0; i < 14; i++) {
                int rowIndex = 0;
                for (String[] row : allData) {
                    if (rowIndex == 0){
                        rowIndex++;
                        continue;
                    }
                    dataFrame.add(Double.parseDouble(row[i]));
                }
                /**we need this conversion because Double is a class. We need to convert from Double to double*/
                Double[] tempdataFrameDouble = dataFrame.toArray(new Double[dataFrame.size()]);
                double[] dataFrameDouble = ArrayUtils.toPrimitive(tempdataFrameDouble);
                tempEntropy = calculateEntropy(dataFrameDouble);
                array.add(Double.toString(tempEntropy) + "\t");// we calculate entropy and add it to the temp list
            }
            /** we convert the list to an array and we have the feature vector*/
            featureVector = array.toArray(new String[array.size()]);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return featureVector;
    }

    public static double calculateEntropy(double[] dataVector)
    {
        int LOG_BASE = 10;
        ProbabilityState state = new ProbabilityState(dataVector);

        double entropy = 0.0;
        for (Double prob : state.probMap.values())
        {
            if (prob > 0)
            {
                entropy -= prob * Math.log(prob);
            }
        }
        entropy /= Math.log(LOG_BASE);

        return entropy;
    }
}

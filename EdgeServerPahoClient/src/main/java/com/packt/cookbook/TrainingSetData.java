package com.packt.cookbook;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

public class TrainingSetData {

    public List<String[]> dataframe;

    public TrainingSetData(String FilePath){
        FileReader filereader = null;
        try {
            filereader = new FileReader(FilePath);

        /** create csvReader object and skip first Line*/
        CSVReader csvReader = new CSVReaderBuilder(filereader).build();
        dataframe = csvReader.readAll();//read entire Training_set.csv and store in datamember "dataFrame"
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException i){
            i.printStackTrace();
        }
    }

    public void print(){
        for (String[] row : dataframe){
            System.out.println();
            for (String cell : row)
                System.out.println(cell);
        }
    }
}

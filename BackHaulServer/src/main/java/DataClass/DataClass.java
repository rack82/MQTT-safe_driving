package DataClass;

import com.bill.BackHaul.ProbabilityState;
import org.apache.commons.lang3.ArrayUtils;
import org.json.JSONArray;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/** Data class will hold the data received from edge server
 * We need a copy of this class here in order for the ObjectStreams to properly receive the object*/

    public class DataClass implements Serializable{
        private String DeviceName;
        private int criticality;
        private String command;
        private String experimentName;
        private double[] buffer;
        private String[] featureVector;
        public double[] featureVectorDouble;
        private float x, y, z;
        private double Longtitude, Latidude;

        public DataClass(String name, String expName, float x, float y, float z, double Longtitude, double Latitude, JSONArray buffer) {
            DeviceName = name;
            experimentName = expName;
            criticality = 1;
            command = null;
            this.x = x;
            this.y = y;
            this.z = z;
            this.Latidude = Latitude;
            this.Longtitude = Longtitude;
            featureVectorDouble = readAllDoubleData(buffer).clone();
        }

        public void setCommand(String str){
            command = str;
        }

        public void setCriticality(int val){
            criticality = val;
        }

        public String getCommand(){
            return command;
        }

        public String getDeviceName(){
            return DeviceName;
        }

        public int getCriticality(){
            return criticality;
        }

        public double getLongtitude(){
            return Longtitude;
        }

        public double getLatidude(){
            return Latidude;
        }

        public void print() {

            System.out.println("name " + DeviceName + " x = " + x + " y = " + y + " z = " + z + " Latitude = " + Latidude + " Longtitude = " + Longtitude );
            for (int i = 0; i < featureVectorDouble.length; i++)
                System.out.println("featureVectorDouble " + featureVectorDouble[i]);
        }

        public String[] readAllData(JSONArray buffer) {

            double tempEntropy = 0.0;
            ArrayList<String[]> allData = new ArrayList<String[]>();
            ArrayList<Double> dataFrame = new ArrayList<Double>();
            String[] featureVector = null;


            String[] array = new String[buffer.length()];
            for (int i = 0; i < buffer.length(); i++) {
                array[i] = buffer.optString(i);
                String[] temp = array[i].split(",");
                allData.add(temp);
            }
            List<String> listarray = new LinkedList<String>();
            //List<double> listarray = new LinkedList<double>();
            try {

                /**for each row in the csv we skip the first because it is the names
                 * and we parse the relavant cell Double value*/


                for (int i = 0; i < 14; i++) {
                    for (String[] row : allData) {
                        Pattern p = Pattern.compile("\\d+\\.\\d+");
                        Matcher m = p.matcher(row[i]);
                        String token = null;
                        while (m.find()) {
                            token = m.group(0); //group 0 is always the entire match

                        }
                        if(token != null)
                            dataFrame.add(Double.parseDouble(token));
                    }
                    /**we need this conversion because Double is a class. We need to convert from Double to double*/
                    Double[] tempdataFrameDouble = dataFrame.toArray(new Double[dataFrame.size()]);
                    double[] dataFrameDouble = ArrayUtils.toPrimitive(tempdataFrameDouble);
                    tempEntropy = calculateEntropy(dataFrameDouble);
                    listarray.add(Double.toString(tempEntropy));// we calculate entropy and add it to the temp list
                }
                /** we convert the list to an array and we have the feature vector*/
                featureVector = listarray.toArray(new String[listarray.size()]);

            }catch(Exception e) {
                e.printStackTrace();
            }
            return featureVector;
        }

        public double[] readAllDoubleData(JSONArray buffer) {
            double tempEntropy = 0.0;
            ArrayList<String[]> allData = new ArrayList<String[]>();
            ArrayList<Double> dataFrame = new ArrayList<Double>();
            double[] featureVector = null;


            String[] array = new String[buffer.length()];
            for (int i = 0; i < buffer.length(); i++) {
                array[i] = buffer.optString(i);
                String[] temp = array[i].split(",");
                allData.add(temp);
            }
            List<Double> listarray = new LinkedList<Double>();
            try {

                /**for each row in the csv we skip the first because it is the names
                 * and we parse the relavant cell Double value*/


                for (int i = 0; i < 14; i++) {
                    for (String[] row : allData) {
                        Pattern p = Pattern.compile("\\d+\\.\\d+");
                        Matcher m = p.matcher(row[i]);
                        String token = null;
                        while (m.find()) {
                            token = m.group(0); //group 0 is always the entire match

                        }
                        if(token != null)
                            dataFrame.add(Double.parseDouble(token));
                    }
                    /**we need this conversion because Double is a class. We need to convert from Double to double*/
                    Double[] tempdataFrameDouble = dataFrame.toArray(new Double[dataFrame.size()]);
                    double[] dataFrameDouble = ArrayUtils.toPrimitive(tempdataFrameDouble);
                    tempEntropy = calculateEntropy(dataFrameDouble);
                    listarray.add(tempEntropy);// we calculate entropy and add it to the temp list
                }
                /** we convert the list to an array and we have the feature vector*/
                Double[] tempVector = listarray.toArray(new Double[listarray.size()]);
                featureVector = ArrayUtils.toPrimitive(tempVector);

            }catch(Exception e) {
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



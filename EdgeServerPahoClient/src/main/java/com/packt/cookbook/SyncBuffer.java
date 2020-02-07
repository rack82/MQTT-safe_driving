package com.packt.cookbook;

import DataClass.DataClass;

import java.util.*;

public class SyncBuffer {

    private ArrayList<DataClass> objQueue;
    public boolean delFlag = false;

    /**Sync buffer holds a list with the objects containing android data and commands */
    public SyncBuffer(){
        objQueue = new ArrayList<DataClass>();
    }

    public void add(DataClass object){
        objQueue.add(object);
    }

    /**Only one thread at a time can access the contents of the data object*/
    public synchronized DataClass read(){
        if (objQueue.size() > 0)
            return objQueue.get(objQueue.size() - 1);
        else
            return null;
    }

    public void delete(){
        if (objQueue.size() > 0) {
            objQueue.clear();
        }
    }
}

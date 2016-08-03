/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.spaceteam.utils;

import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Local
 */
public class Utils {
    public static List<Integer> boxList(int[] data)
    {
        return boxList(data,data.length);
    }
    
    public static List<Integer> boxList(int[] data, int length)
    {
        if(length > data.length)
            return null;
        Integer[] boxed = new Integer[length];
        for(int i = 0; i < length; i++)
            boxed[i] = data[i];
        return Arrays.asList(boxed);
    }
    
    public static int[] unboxList(List<Integer> data)
    {
        int[] unbox = new int[data.size()];
        int i = 0;
        
        while(i < data.size())
        {
            unbox[i] = data.get(i);
            i++;
        }
        
        return unbox;
    }
}

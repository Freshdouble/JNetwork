/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.spaceteam.utils;

import at.spaceteam.utils.Utils;
import java.util.List;

/**
 *
 * @author Local
 */
public class Message {
    
    final public int[] buffer;
    final public int size;
    
    public Message(int[] buffer)
    {
        this(buffer,buffer.length);
    }
    
    public Message(int[] buffer, int size)
    {
        this.buffer = new int[size];
        System.arraycopy(buffer, 0, this.buffer, 0, size);
        this.size = size;
    }
    
    public Message(List<Integer> list)
    {
        this(Utils.unboxList(list));
    }
    
    public List<Integer> asList()
    {
        return Utils.boxList(buffer);
    }
}

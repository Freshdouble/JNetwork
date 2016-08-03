/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.spaceteam.network.driver;

import at.spaceteam.utils.Message;
import at.spaceteam.utils.Utils;
import java.util.List;

/**
 *
 * @author Local
 */
public class TlpMessage extends Message {
    
    private int resendCounter;
    private int timeoutCounter;

    public TlpMessage(int[] buffer, int resendCounter, int timeoutCounter) {
        this(buffer,buffer.length,resendCounter,timeoutCounter);
    }

    public TlpMessage(int[] buffer, int size, int resendCounter, int timeoutCounter) {
        super(buffer, size);
        this.resendCounter = resendCounter;
        this.timeoutCounter = timeoutCounter;
    }

    public TlpMessage(List<Integer> list, int resendCounter, int timeoutCounter) {
        this(Utils.unboxList(list),resendCounter,timeoutCounter);
    }
    
    public int getResendCounter()
    {
        return this.resendCounter;
    }
    
    public int getTimeoutCounter()
    {
        return this.timeoutCounter;
    }
    
    int reloadResendCounter(int counter)
    {
        return this.resendCounter = counter;
    }
    
    int reloadTimeoutCounter(int counter)
    {
        return this.timeoutCounter = counter;
    }
    
    int decrementResendCounter()
    {
        return this.resendCounter--;
    }
    
    int decrementTimeoutCounter()
    {
        return this.timeoutCounter--;
    }
}

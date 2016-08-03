/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.spaceteam.network.interfaces;

import at.spaceteam.utils.Message;
import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Random;
import java.util.TooManyListenersException;

/**
 *
 * @author Local
 */
public class Serial implements SerialPortEventListener {
    
    private CommPort commPort;
    private InputStream in;
    private OutputStream out;
    
    private final ArrayList<SerialListener> listener;
    
    public Serial() {
        listener = new ArrayList<>();
    }
    
    public Serial(String portName, int baud, int Databits, int Stopbits, int Parity) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        this();
        
        connect(portName, baud, Databits, Stopbits, Parity);
    }
    
    public void addListener(SerialListener listener)
    {
        this.listener.add(listener);
    }
    
    public void removeListener(SerialListener listener)
    {
        this.listener.remove(listener);
    }
    
    public void connect(String portName, int baud, int Databits, int Stopbits, int Parity) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portName);
        if (portIdentifier.isCurrentlyOwned()) {
            throw new IOException("Port is currently used");
        } else {
            commPort = portIdentifier.open("Spaceteam FMS Connection", 2000);
            if (commPort instanceof SerialPort) {
                SerialPort serialPort = (SerialPort) commPort;
                serialPort.setSerialPortParams(baud, Databits, Stopbits, Parity);
                serialPort.notifyOnDataAvailable(true);
                try {
                    serialPort.addEventListener(this);
                } catch (TooManyListenersException ex) {
                    
                }
                
                in = serialPort.getInputStream();
                out = serialPort.getOutputStream();
                
            } else {
                throw new IOException("Error: Only serial ports are supported.");
            }
        }
    }
    
    public void close() {
        try {
            in.close();
            out.close();
        } catch (IOException ex) {
            in = null;
            out = null;
        }
        
        commPort.close();
        commPort = null;
    }
    
    public static ArrayList<String> listPorts() {
        ArrayList<String> list = new ArrayList<>();
        java.util.Enumeration<CommPortIdentifier> portEnum = CommPortIdentifier.getPortIdentifiers();
        while (portEnum.hasMoreElements()) {
            CommPortIdentifier portIdentifier = portEnum.nextElement();
            list.add(portIdentifier.getName() + " - " + getPortTypeName(portIdentifier.getPortType()));
        }
        return list;
    }
    
    public static String getPortTypeName(int portType) {
        switch (portType) {
            case CommPortIdentifier.PORT_I2C:
                return "I2C";
            case CommPortIdentifier.PORT_PARALLEL:
                return "Parallel";
            case CommPortIdentifier.PORT_RAW:
                return "Raw";
            case CommPortIdentifier.PORT_RS485:
                return "RS485";
            case CommPortIdentifier.PORT_SERIAL:
                return "Serial";
            default:
                return "unknown type";
        }
    }

    public InputStream getIn() {
        return in;
    }

    public OutputStream getOut() {
        return out;
    }
    
    @Override
    public void serialEvent(SerialPortEvent spe) {
        switch (spe.getEventType()) {
            case SerialPortEvent.DATA_AVAILABLE:
                try {
                    Random rand = new Random();
                    int availableData = in.available();
                    int i = 0;
                    int[] data = new int[availableData];
                    while (availableData > 0) {
                        int rawData = in.read();
                        /*
                        if(rand.nextInt(10000) <= 0 )
                            rawData ^= rand.nextInt(255);*/
                        data[i] = rawData & 0xFF;
                        i++;
                        availableData--;
                    }
                    Message m = new Message(data);
                    listener.stream().forEach((SerialListener l) -> {
                        l.recieveBytes(m);
                    });
                } catch (IOException ex) {
                    
                }
        }
    }
}

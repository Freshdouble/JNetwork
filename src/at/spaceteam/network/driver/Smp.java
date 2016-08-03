/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.spaceteam.network.driver;

import at.spaceteam.utils.Message;
import at.spaceteam.utils.Utils;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Local
 */
public class Smp
{

    final int FRAMESTART;
    final int CRC_Polynom = 0xA001;
    final int MAX_PAYLOAD;
    final int MAX_FRAMESIZE;

    private int bytesToRecieve;
    private ArrayList<Integer> buffer;
    private IPacketReciever packetReciever;
    private IPacketSender packetSender;
    private int crcHighByte;
    private int crc;

    private boolean recieving;
    private boolean recievedDelimeter;
    private byte status;

    public Smp(int Framestart, int Max_Payload)
    {
        packetReciever = null;
        packetSender = null;
        buffer = new ArrayList<>();
        bytesToRecieve = 0;
        crc = 0;
        crcHighByte = 0;
        recievedDelimeter = false;

        FRAMESTART = Framestart;
        MAX_PAYLOAD = Max_Payload;
        MAX_FRAMESIZE = Max_Payload + 4;
    }

    public Smp(int Framestart, int Max_Payload, IPacketReciever packetReciever, IPacketSender packetSender)
    {
        this(Framestart, Max_Payload);
        setCallbackHandler(packetReciever, packetSender);
    }

    public final void setCallbackHandler(IPacketReciever packetReciever, IPacketSender packetSender)
    {
        this.packetReciever = packetReciever;
        this.packetSender = packetSender;
    }

    protected int crc16(int crc, int c)
    {
        short i;
        int _crc = crc;
        for (i = 0; i < 8; i++)
        {
            if (((_crc ^ c) & 1) != 0)
            {
                _crc = (_crc >> 1) ^ CRC_Polynom;
            }
            else
            {
                _crc >>= 1;
            }
            c >>= 1;
        }
        return (_crc);
    }

    public int Send(int[] buffer)
    {
        int i;
        int offset = 0;
        int crc = 0;

        if (buffer.length > MAX_PAYLOAD)
        {
            return 0;
        }

        int message1[] = new int[2 * (buffer.length + 2)];
        int message2[] = new int[2 * (buffer.length + 2) + 5];

        for (i = 0; i < buffer.length; i++)
        {

            if (buffer[i] == FRAMESTART)
            {
                message1[i + offset] = FRAMESTART;
                offset++;
            }

            message1[i + offset] = buffer[i];

            crc = crc16(crc, buffer[i]);
        }

        message1[i + offset] = crc >> 8; //CRC high byte
        if (message1[i + offset] == FRAMESTART)
        {
            offset++;
            message1[i + offset] = FRAMESTART;
        }
        message1[i + offset + 1] = crc & 0xFF; //CRC low byte
        if (message1[i + offset + 1] == FRAMESTART)
        {
            offset++;
            message1[i + offset + 1] = FRAMESTART;
        }
        int packageSize = buffer.length + 2;
        int completeFramesize = packageSize + offset;
        offset = 0;
        message2[0] = FRAMESTART;
        message2[1] = packageSize & 0xFF;
        if (message2[1] == FRAMESTART)
        {
            message2[2] = FRAMESTART;
            offset = 1;
        }
        message2[2 + offset] = packageSize >> 8;
        if (message2[2 + offset] == FRAMESTART)
        {
            message2[3 + offset] = FRAMESTART;
            offset = 2;
        }

        for (i = 3 + offset; i < completeFramesize + 3 + offset; i++)
        {
            message2[i] = message1[i - (3 + offset)];
        }

        if (packetSender != null)
        {
            return packetSender.sendPacket(new Message(message2, completeFramesize + 3 + offset), this);
        }
        else
        {
            return 0;
        }
    }

    public int RecieveInBytes(List<Integer> data)
    {
        return RecieveInBytes(Utils.unboxList(data));
    }

    public int RecieveInBytes(int[] data)
    {
        int i;
        int error;
        for (i = 0; i < data.length; i++)
        {
            error = RecieveInByte(data[i]);
            if (error != 0)
            {
                return error;
            }
        }
        error = 0;
        return error;
    }

    private int pRevcieveInByte(int data)
    {
        switch (status) //State machine
        {
            case 0: //Idle State Waiting for Framestart
                break;
            case 1:
                recieving = true;
                if (bytesToRecieve == 0)
                {
                    bytesToRecieve = data;
                }
                else
                {
                    bytesToRecieve |= data << 8;
                    status = 2;
                    buffer.clear();
                    crc = 0;
                }
                break;
            case 2:
                if (bytesToRecieve == 2) //Only CRC to recieve
                {
                    status = 3;
                }
                else
                {
                    bytesToRecieve--;
                    if (!buffer.add(data))
                    {
                        //Bufferoverflow
                        status = 0;
                        recieving = false;
                        return -1;
                    }
                    crc = crc16(crc, data);
                    break;
                }
            case 3:
                if (bytesToRecieve == 0)
                {
                    status = 0;
                    recieving = false;
                    if (crc == ((crcHighByte << 8) | data)) //Read the crc and compare
                    {
                        //Data ready
                        if (packetReciever != null)
                        {
                            return packetReciever.recievePacket(new Message(buffer), this);
                        }
                        else
                        {
                            return -2;
                        }
                    }
                    else //crc doesnt match.
                     if (packetReciever != null && packetReciever instanceof IPacketReciever_Error)
                        {
                            return ((IPacketReciever_Error) packetReciever).RoguePacketRecieved(new Message(buffer), this);
                        }
                }
                else
                {
                    crcHighByte = data; //Save first byte of crc
                    bytesToRecieve = 0;
                }
                break;

            default: //Invalid State
                status = 0;
                bytesToRecieve = 0;
                recieving = false;
                break;
        }
        return 0;
    }

    public int RecieveInByte(int data)
    {
        if (data == FRAMESTART)
        {
            if (recievedDelimeter)
            {
                recievedDelimeter = false;
                return pRevcieveInByte(data);
            }
            else
            {
                recievedDelimeter = true;
            }
        }
        else
        {
            if (recievedDelimeter)
            {
                status = 1;
                bytesToRecieve = 0;
                recievedDelimeter = false;
                if (recieving)
                {
                    if (packetReciever != null && packetReciever instanceof IPacketReciever_Error)
                    {
                        ((IPacketReciever_Error) packetReciever).RoguePacketRecieved(new Message(buffer), this);
                    }
                }
            }
            else
            {
                if(!recieving)
                {
                    status = 1;
                    bytesToRecieve = 0;
                    recievedDelimeter = false;
                }
            }
            return pRevcieveInByte(data);
        }
        return 0;
    }

    int GetBytesToRecieve()
    {
        if (recieving)
        {
            return bytesToRecieve;
        }
        else
        {
            return 0;
        }
    }

    boolean IsRecieving()
    {
        return recieving;
    }
}

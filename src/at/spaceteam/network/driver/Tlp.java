/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.spaceteam.network.driver;

import at.spaceteam.utils.Message;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author Local
 */
public class Tlp {

    private final int TLP_MESSAGE_SIZE;
    private final int WINDOW_SIZE;
    private final int RESEND_COUNTER;
    private final int ACK_COUNTER;
    private final int TIMEOUT;
    private final boolean immediate_Ack;

    private IPacketReciever packetReciever;
    private IPacketSender packetSender;
    private int last_recieved_sequence;
    private int last_transmitted_sequence;
    private int ack_counter;
    private boolean send_ack;

    private Timer timer;

    private LinkedList<TlpMessage> messagefifo;

    private class Flags {

        public boolean Ack_Ack;

        public Flags() {
            Ack_Ack = false;
        }

        public Flags(int bits) {
            this.Ack_Ack = (bits & 0x01) != 0;
        }

        public int getByte() {
            int bits = 0;
            bits |= (Ack_Ack) ? 1 : 0;
            return bits;
        }
    }

    private Flags f;

    public Tlp(int messageSize, int Window_Size, int Resend_Counter, int Ack_Counter, int Timeout,
            int Tick_Intervall, boolean immediate_Ack) {
        TLP_MESSAGE_SIZE = messageSize;
        WINDOW_SIZE = Window_Size;
        RESEND_COUNTER = Resend_Counter;
        ACK_COUNTER = Ack_Counter;
        TIMEOUT = Timeout;
        this.immediate_Ack = immediate_Ack;
        
        send_ack = true;
        ack_counter = 0;
        last_recieved_sequence = 0;
        last_transmitted_sequence = 0;

        f = new Flags(0x00);
        messagefifo = new LinkedList<>();

        final Object ownInstance = this;

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (ack_counter == 0) {
                    tlp_send_ack();
                } else {
                    ack_counter--;
                }

                for (ListIterator<TlpMessage> it = messagefifo.listIterator(); it.hasNext();) {
                    TlpMessage message = it.next();

                    if (message.getResendCounter() == 0) {
                        if (packetSender.sendPacket(message, ownInstance) != 0) {
                            message.reloadResendCounter(RESEND_COUNTER);
                        }
                    } else {
                        message.decrementResendCounter();
                    }

                    if (message.getTimeoutCounter() == 0) {
                        if (packetReciever instanceof IPacketReciever_Timeout) {
                            ((IPacketReciever_Timeout) packetReciever).Timeout(message, ownInstance);
                        }
                        message.reloadTimeoutCounter(TIMEOUT);
                    } else {
                        message.decrementTimeoutCounter();
                    }
                }

            }
        }, 1, Tick_Intervall);
    }

    public Tlp(int messageSize, int Window_Size, int Resend_Counter, int Ack_Counter, int Timeout,
            int Tick_Intervall, boolean immediate_Ack, IPacketReciever reciever, IPacketSender sender) {
        this(messageSize, Window_Size, Resend_Counter, Ack_Counter, Timeout, Tick_Intervall, immediate_Ack);
        addCallback(reciever, sender);
    }

    public final void addCallback(IPacketReciever reciever, IPacketSender sender) {
        packetReciever = reciever;
        packetSender = sender;
    }

    private void tlp_send_ack_ack() {
        int[] message = new int[3];
        Flags flag = new Flags();
        flag.Ack_Ack = true;
        if ((last_recieved_sequence != 0) && send_ack) {
            if (last_transmitted_sequence != 0) {
                message[0] = last_transmitted_sequence; //Pick a valid sequence number
            } else {
                message[0] = last_transmitted_sequence + 1;
            }
            message[1] = last_transmitted_sequence;
            message[2] = flag.getByte(); //flags
            if (packetSender.sendPacket(new Message(message, 3), this) != 0) // Dont save this message, just transmit it
            {
                ack_counter = ACK_COUNTER;
            }
        } else {
            ack_counter = ACK_COUNTER;
        }
    }

    private void tlp_recv_ack(int number) {
        if (number != 0) //If ACK is 0 it isn't valid ACK Field
        {
            for (ListIterator<TlpMessage> it = messagefifo.listIterator(); it.hasNext();) {
                TlpMessage message = it.next();

                if (message.buffer[0] == number) {
                    it.remove();
                    break;
                }
            }
            if (number == last_transmitted_sequence) {
                tlp_send_ack_ack();
            }
        }
    }

    private void tlp_send_ack() {
        int[] message = new int[3];
        if ((last_recieved_sequence != 0) && send_ack) {
            if (last_transmitted_sequence != 0) {
                message[0] = last_transmitted_sequence; //Pick a valid sequence number
            } else {
                message[0] = last_transmitted_sequence + 1;
            }
            message[1] = last_recieved_sequence;
            message[2] = 0x00; //flags
            if (packetSender.sendPacket(new Message(message, 3), this) != 0) // Dont save this message, just transmit it
            {
                ack_counter = ACK_COUNTER;
            }
        } else {
            ack_counter = ACK_COUNTER;
        }
    }

    public int tlp_recieve(int[] data) {
        if (data.length < 2) {
            return 0;
        }

        long upper;
        long lower;
        long current = data[0] + 255;
        Flags flags = new Flags(data[2]);

        if (last_recieved_sequence == 255) {
            upper = 1 + 255;
            lower = 255 - (WINDOW_SIZE - 1);
        } else {
            upper = last_recieved_sequence + 1 + 255;
            if (last_recieved_sequence + WINDOW_SIZE <= 255) {
                lower = last_recieved_sequence + 255 - WINDOW_SIZE;
            } else {
                lower = last_recieved_sequence + 255 - (WINDOW_SIZE - 1);
            }
        }

        if (upper >= current && lower <= current) {
            if (flags.Ack_Ack) {
                if (last_recieved_sequence == data[1]) {
                    send_ack = false;
                }
            } else {
                tlp_recv_ack(data[1]);
                if (data.length != 3 && current == upper) // If size == 3 recieved a simple ack Message
                {
                    last_recieved_sequence = data[0]; //Update recieve Order and Ack Byte

                    if (immediate_Ack) {
                        tlp_send_ack();
                    }

                    return packetReciever.recievePacket(new Message(Arrays.copyOfRange(data, 3, data.length)), this);
                }
            }
            return 1;
        }
        return 0;
    }

    public int tlp_send(int[] data) {
        int[] message = new int[data.length + 3];
        int increment_sequence;
        int i;

        if (data.length > TLP_MESSAGE_SIZE) {
            return 0;
        }

        increment_sequence = last_transmitted_sequence + 1;
        if (increment_sequence == 0) {
            increment_sequence = 1;
        }

        message[0] = increment_sequence;
        message[1] = last_recieved_sequence;
        message[2] = 0x00; //Flags

        for (i = 0; i < data.length; i++) {
            message[i + 3] = data[i];
        }

        TlpMessage m = new TlpMessage(message, RESEND_COUNTER, TIMEOUT);

        if (messagefifo.add(m)) {
            if (packetSender.sendPacket(m, this) != 0) {
                ack_counter = ACK_COUNTER;
                last_transmitted_sequence = increment_sequence;
                return data.length;
            }
        }

        return 0;
    }

    public void tlp_flush() {
        TlpMessage message = messagefifo.peekLast();
        if (message != null) {
            last_transmitted_sequence = message.buffer[0];
            messagefifo.clear();
        }
    }

}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.spaceteam.network.driver;

import at.spaceteam.utils.Message;

/**
 *
 * @author Local
 */
public interface IPacketReciever {
    int recievePacket(Message buffer, Object Sender);
}

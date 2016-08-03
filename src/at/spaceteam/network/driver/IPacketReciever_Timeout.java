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
public interface IPacketReciever_Timeout extends IPacketReciever {
    public void Timeout(Message message, Object sender);
}

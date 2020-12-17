package org.altoro.core.net.messagehandler;

import org.altoro.core.exception.P2pException;
import org.altoro.core.net.message.TronMessage;
import org.altoro.core.net.peer.PeerConnection;

public interface TronMsgHandler {

  void processMessage(PeerConnection peer, TronMessage msg) throws P2pException;

}

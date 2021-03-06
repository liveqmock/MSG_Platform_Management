package com.neusoft.util.rpc.client;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;

import com.neusoft.util.rpc.message.MessageService;

/**
 * The class responsible for reading and deserializing incoming messages.
 * Should be run in its own thread.
 * 
 * @author Joel Meyer
 */
public class MessageReceiver extends ConnectionRequiredRunnable {
  private final MessageService.Processor processor;
  private final TProtocol protocol;
  private boolean running = true;
  
  public MessageReceiver(
      TProtocol protocol,
      MessageService.Iface messageService,
      ConnectionStatusMonitor connectionMonitor) {
    super(connectionMonitor, "Message Receiver");
    this.protocol = protocol;
    this.processor = new MessageService.Processor(messageService);
  }
  
  @Override
  public void run() {
    connectWait();
    while (running) {
      try {
        while (processor.process(protocol, protocol) == true) { }
      } catch (TException e) {
        disconnected();
      }
    }
  }

public boolean isRunning() {
	return running;
}

public void setRunning(boolean running) {
	this.running = running;
}
}

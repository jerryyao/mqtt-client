package org.stayfool.client.testsuite.response;

/**
 * Created by szl on 2016/12/8.
 */
public class SendCommandRsp {

    private String command_id;

    public SendCommandRsp() {
    }

    public SendCommandRsp(String command_id) {
        this.command_id = command_id;
    }

    public String getCommand_id() {
        return command_id;
    }

    public void setCommand_id(String command_id) {
        this.command_id = command_id;
    }
}

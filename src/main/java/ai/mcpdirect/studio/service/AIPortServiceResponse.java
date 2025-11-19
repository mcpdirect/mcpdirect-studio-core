package ai.mcpdirect.studio.service;

import appnet.hstp.SimpleServiceResponseMessage;

public class AIPortServiceResponse<T> extends SimpleServiceResponseMessage<T> {
    public void code(int code){
        this.code = code;
        if(code==0) status = "success";
    }
}

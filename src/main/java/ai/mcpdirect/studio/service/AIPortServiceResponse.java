package ai.mcpdirect.studio.service;

import appnet.hstp.SimpleServiceResponseMessage;

public class AIPortServiceResponse<T> extends SimpleServiceResponseMessage<T> {
    public static final int ACCOUNT_NOT_EXIST = 1001000;
    public static final int ACCOUNT_EXISTED = 1001001;

    public static final int PASSWORD_INCORRECT = 1001002;

    public static final int SIGN_IN_FAILED = 1001003;
    public static final int ACCOUNT_INCORRECT = 1001004;
    public static final int OTP_EXPIRED = 1001005;
    public static final int OTP_FAILED = 1001006;

    public static final int TEAM_NOT_EXIST = 1002000;

    public static final int TOOL_MAKER_NOT_EXISTS = 1003000;
    public static final int TOOL_MAKER_OCCUPIED = 1003001;
    public void code(int code){
        this.code = code;
        if(code==0) status = "success";
    }
}

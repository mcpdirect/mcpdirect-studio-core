package ai.mcpdirect.backend.dao.entity.account;

//import appnet.hstp.labs.util.ID;

public class AIPortAccount {
    public static final int ACCOUNT_NOT_EXIST = 0x10000;
    public static final int ACCOUNT_EXISTED = 0x10001;

    public static final int PASSWORD_INCORRECT = 0x10100;

    public static final int SIGN_IN_FAILED = 0x10200;
    public static final int PASSWORD = 0;
    public static final int ACCESS_KEY = 1;
    public static final int ECC_SIGNATURE = 2;

    public static final int GOOGLE_ID_TOKEN = 10000;

    public long id;
    public String account;
    public int status;

    public AIPortAccount() {
    }

    public AIPortAccount(long id) {
        this.id = id;
    }

    public AIPortAccount(String account, int status) {
        this(0,account, status);
    }

    public AIPortAccount(long id, String account, int status) {
        this.id = id;
        this.account = account;
        this.status = status;
    }
}

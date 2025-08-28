package ai.mcpdirect.backend.dao.entity.account;

public class AIPortOtp {
    public long id;
    public long expirationDate;
    public String account;
    public String otp;

    public static AIPortOtp createOtp(long id){
        AIPortOtp aiOtp = new AIPortOtp();
        aiOtp.id = id;
        String s = Integer.toString(aiOtp.hashCode());
        aiOtp.otp = s.substring(s.length()-6);
        return aiOtp;
    }
}
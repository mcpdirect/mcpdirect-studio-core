package ai.mcpdirect.backend.dao.entity.aitool;

public class AIPortAppVersion {
    public int appId;

    public static final int PLATFORM_MACOS = 100;
    public static final int PLATFORM_IOS = 101;
    public static final int PLATFORM_IPADOS = 102;
    public static final int PLATFORM_WINDOWS = 200;
    public static final int PLATFORM_LINUX = 300;
    public static final int PLATFORM_ANDROID = 400;
    public int platform;

    public static final int ARCH_X86 = 100;
    public static final int ARCH_X86_64 = 101;
    public static final int ARCH_ARM = 200;
    public static final int ARCH_ARM64 = 201;
    public int architecture;

    public String version;
    public int versionCode;

    public static final short STATUS_DEPRECATED = -1;
    public static final short STATUS_PREVIEW = 0;
    public static final short STATUS_RELEASE = 1;
    public int status;

    public boolean mandatory;
    public String releaseNotes;
    public String url;
    public long created;
}
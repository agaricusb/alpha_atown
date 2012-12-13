package ee.lutsu.alpha.mc.mytown;

import java.util.Properties;

public class Version
{
    private static String major;
    private static String minor;
    private static String rev;
    private static String mcversion;

    static void init(Properties var0)
    {
        if (var0 != null)
        {
            major = var0.getProperty("MyTown.build.major.number");
            minor = var0.getProperty("MyTown.build.minor.number");
            rev = var0.getProperty("MyTown.build.revision.number");
            mcversion = var0.getProperty("MyTown.build.mcversion");
        }
    }

    public static final String version()
    {
        return String.format("%s.%s.%s", new Object[] {major, minor, rev});
    }
}

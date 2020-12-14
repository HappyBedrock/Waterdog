package net.md_5.bungee;

import java.security.Security;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.command.ConsoleCommandSender;

public class BungeeCordLauncher
{

    public static void main(String[] args) throws Exception
    {
        Security.setProperty( "networkaddress.cache.ttl", "30" );
        Security.setProperty( "networkaddress.cache.negative.ttl", "10" );

        OptionParser parser = new OptionParser();
        parser.allowsUnrecognizedOptions();
        parser.acceptsAll( Arrays.asList( "help" ), "Show the help" );
        parser.acceptsAll( Arrays.asList( "v", "version" ), "Print version and exit" );
        parser.acceptsAll( Arrays.asList( "noconsole" ), "Disable console input" );

        OptionSet options = parser.parse( args );

        if ( options.has( "help" ) )
        {
            parser.printHelpOn( System.out );
            return;
        }
        if ( options.has( "version" ) )
        {
            System.out.println( BungeeCord.class.getPackage().getImplementationVersion() );
            return;
        }

        if ( BungeeCord.class.getPackage().getSpecificationVersion() != null && System.getProperty( "IReallyKnowWhatIAmDoingISwear" ) == null )
        {
            Date buildDate = new SimpleDateFormat( "yyyyMMdd" ).parse( BungeeCord.class.getPackage().getSpecificationVersion() );

            Calendar deadline = Calendar.getInstance();
            deadline.add( Calendar.WEEK_OF_YEAR, -8 );
            if ( buildDate.before( deadline.getTime() ) )
            {
                System.err.println( "*** Hey! This build is potentially outdated :( ***" );
                System.err.println( "*** Please check for a new build from https://ci.codemc.org/job/yesdog/job/Waterdog/ ***" );
                System.err.println( "*** Should this build be outdated, you will get NO support for it. ***" );
                System.err.println( "*** Server will start in 10 seconds ***" );
                Thread.sleep( TimeUnit.SECONDS.toMillis( 10 ) );
            }
        }

        BungeeCord bungee = new BungeeCord();
        ProxyServer.setInstance( bungee );
        // Waterdog start
        bungee.getLogger().info( ChatColor.AQUA + "Starting "+VersionInfo.SOFTWARE+" proxy software!");
        bungee.getLogger().info( ChatColor.DARK_AQUA + "Software Version: "+VersionInfo.VERSION);
        bungee.getLogger().info( ChatColor.DARK_AQUA + "Build Version: "+VersionInfo.JENKINS_BUILD_ID);
        bungee.getLogger().info( ChatColor.DARK_AQUA + "Raw Version: "+bungee.getVersion());
        bungee.getLogger().info( ChatColor.DARK_AQUA + "Development Build: "+VersionInfo.IS_DEVELOPMENT_BUILD);
        bungee.getLogger().info( ChatColor.DARK_AQUA + "Software Authors: "+VersionInfo.AUTHORS);
        // Waterdog end
        bungee.start();

        if ( !options.has( "noconsole" ) )
        {
            // Waterfall start - Use TerminalConsoleAppender
            new io.github.waterfallmc.waterfall.console.WaterfallConsole().start();
            /*
            String line;
            while ( bungee.isRunning && ( line = bungee.getConsoleReader().readLine( ">" ) ) != null )
            {
                if ( !bungee.getPluginManager().dispatchCommand( ConsoleCommandSender.getInstance(), line ) )
                {
                    bungee.getConsole().sendMessage( new ComponentBuilder( "Command not found" ).color( ChatColor.RED ).create() );
                }
            }
            */
            // Waterfall end
        }
    }
}

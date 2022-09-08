package bot;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import io.github.cdimascio.dotenv.Dotenv;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.audio.AudioConnection;
import org.javacord.api.audio.AudioSource;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.channel.VoiceChannel;
import org.javacord.api.entity.server.Server;
import org.javacord.api.interaction.*;
import org.w3c.dom.Text;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
//    public static AudioConnection audioConnection1;
//    public static AudioPlayer player;
//    public static AudioPlayerManager playerManager;

    public static Map<Server, AudioConnection> audioConnectionMap = new HashMap();
    public static Map<Server, TextChannel> textChannelMap = new HashMap();

    //    public static Map<Server, AudioPlayerManager> playerManagerMap;
//    public static Map<Server, AudioPlayer> playerMap;
    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.load();
        String TOKEN = dotenv.get("DISCORD_TOKEN");
        DiscordApi api = new DiscordApiBuilder()
                .setToken(TOKEN)
                .login().join();
        System.out.println("discord bot built.");
        System.out.println(String.format("Logged in as: %s", api.getYourself().getName()));
        SlashCommand commandJoin = SlashCommand.with("join", "VCに接続.")
                .createGlobal(api).join();
        SlashCommand commandStop = SlashCommand.with("stop", "音楽を止める.")
                .createGlobal(api).join();
        SlashCommand commandPlay = SlashCommand.with("play", "再生.",
                Arrays.asList(
                        SlashCommandOption.createWithOptions(SlashCommandOptionType.STRING, "URL", "URL")
                )).createGlobal(api).join();
        SlashCommand commandLeave = SlashCommand.with("leave", "VCから切断.")
                .createGlobal(api).join();

        api.addSlashCommandCreateListener(event -> {
            SlashCommandInteraction slashCommandInteraction = event.getSlashCommandInteraction();
            if (slashCommandInteraction.getUser().isBot()) {
                return;
            }
            if (slashCommandInteraction.getCommandName().equalsIgnoreCase("stop")) {
                slashCommandInteraction.createImmediateResponder().setContent("Wait...").respond();
//                AudioPlayerManager playerManager = playerManagerMap.get(slashCommandInteraction.getServer().get());
//                playerManager.shutdown();
                AudioConnection audioConnection = audioConnectionMap.get(slashCommandInteraction.getServer().get());
                audioConnection.removeAudioSource();
//                audioConnectionMap.remove(slashCommandInteraction.getServer().get());
            }
            if (slashCommandInteraction.getCommandName().equalsIgnoreCase("play")) {
//                System.out.println(slashCommandInteraction.getOptionStringValueByIndex(0));
                slashCommandInteraction.createImmediateResponder().setContent("Playing...").respond();
                String url = slashCommandInteraction.getOptionStringValueByIndex(0).get();
                TextChannel textChannel = slashCommandInteraction.getChannel().get();

//                System.out.println(url);
//                AudioPlayer player = playerMap.get(slashCommandInteraction.getServer().get());
//                System.out.println(audioConnectionMap.get(slashCommandInteraction.getServer().get()));
                Server server = slashCommandInteraction.getServer().get();
                AudioConnection audioConnection = audioConnectionMap.get(server);
//                AudioPlayerManager playerManager = playerManagerMap.get(slashCommandInteraction.getServer().get());
                AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
                playerManager.registerSourceManager(new YoutubeAudioSourceManager());
                AudioPlayer player = playerManager.createPlayer();
                AudioSource source = new LavaplayerAudioSource(api, player);
                audioConnection.setAudioSource(source);
                playerManager.loadItem(url, new AudioLoadResultHandler() {
                    @Override
                    public void trackLoaded(AudioTrack track) {
                        player.playTrack(track);
                        player.setVolume(1);
                    }

                    @Override
                    public void playlistLoaded(AudioPlaylist playlist) {
                        for (AudioTrack track : playlist.getTracks()) {
                            player.playTrack(track);
                            player.setVolume(1);
                        }
                    }

                    @Override
                    public void noMatches() {

                    }

                    @Override
                    public void loadFailed(FriendlyException exception) {

                    }
                });

            }
            if (slashCommandInteraction.getCommandName().equalsIgnoreCase("join")) {
                slashCommandInteraction.createImmediateResponder()
                        .setContent("Connecting...")
                        .respond();
                TextChannel textChannel = slashCommandInteraction.getChannel().get();
                Server server = slashCommandInteraction.getServer().get();
                textChannelMap.put(server, textChannel);

                ServerVoiceChannel voiceChannel;
                try {
                    voiceChannel = slashCommandInteraction.getUser().getConnectedVoiceChannel(server).get();
                } catch (Exception e) {
                    slashCommandInteraction.createImmediateResponder().setContent("どこのVCにも参加していません！").respond();
                    throw new RuntimeException(e);
                }
                System.out.println(String.format("in addSlashCommandCreateListener: Connected to: %s", server.getName()));
                voiceChannel.connect().thenAccept(audioConnection -> {
                    textChannel.sendMessage("Connected.").join();
                    textChannel.sendMessage("Created by Yuki.").join();
                    audioConnectionMap.put(server, audioConnection);
                }).exceptionally(throwable -> {
                    throw new RuntimeException(throwable);
                });

            }

            if (slashCommandInteraction.getCommandName().equalsIgnoreCase("leave")) {
                slashCommandInteraction.createImmediateResponder().setContent("Disconnecting...").respond();
                AudioConnection audioConnection = audioConnectionMap.get(slashCommandInteraction.getServer().get());
                audioConnection.close();
                audioConnectionMap.remove(slashCommandInteraction.getServer().get());
            }
        });
        api.addMessageCreateListener(event -> {
            String msg = event.getMessageContent();
            if (msg.equalsIgnoreCase(".debug")) {
                System.out.println(".debug hit.");
                System.out.println(String.format("Logged in as: %s", api.getYourself().getName()));
                System.out.println(String.format("audioConnectionMap: %s", audioConnectionMap));

                for (Server x : api.getServers()) {

                    System.out.println(String.format("Now connected to: %s", x));
                }
            }
        });
        api.addServerVoiceChannelMemberLeaveListener((event) -> {
            if (!event.getUser().isBot()) {
                AudioConnection audioConnection = audioConnectionMap.get(event.getServer());
                if (event.getChannel().getConnectedUsers().stream().count() == 1){
                    audioConnection.close();
                    audioConnectionMap.remove(event.getServer());
                }
            }
        });

    }
}

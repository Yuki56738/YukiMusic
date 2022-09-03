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

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static AudioConnection audioConnection1;
    public static AudioPlayer player;
    public static AudioPlayerManager playerManager;
    public static void main(String[] args){
        Dotenv dotenv = Dotenv.load();
        String TOKEN = dotenv.get("DISCORD_TOKEN");
        DiscordApi api = new DiscordApiBuilder()
                .setToken(TOKEN)
                .login().join();
        SlashCommand commandJoin = SlashCommand.with("join", "VCに接続.")
                .createGlobal(api).join();
        SlashCommand commandStop = SlashCommand.with("stop", "音楽を止める.")
                        .createGlobal(api).join();
        SlashCommand commandPlay = SlashCommand.with("play", "再生.",
        Arrays.asList(
                SlashCommandOption.createWithOptions(SlashCommandOptionType.STRING, "URL", "URL")
        )).createGlobal(api).join();

        api.addSlashCommandCreateListener(event -> {
            SlashCommandInteraction slashCommandInteraction = event.getSlashCommandInteraction();
            if (slashCommandInteraction.getUser().isBot()){
                return;
            }
            if(slashCommandInteraction.getCommandName().equalsIgnoreCase("play")){
//                System.out.println(slashCommandInteraction.getOptionStringValueByIndex(0));
                String url = slashCommandInteraction.getOptionStringValueByIndex(0).get();
//                System.out.println(url);
                AudioSource source = new LavaplayerAudioSource(api, player);
                audioConnection1.setAudioSource(source);
                playerManager.loadItem(url, new AudioLoadResultHandler() {
                    @Override
                    public void trackLoaded(AudioTrack track) {
                        player.playTrack(track);
                    }

                    @Override
                    public void playlistLoaded(AudioPlaylist playlist) {
                        for (AudioTrack track : playlist.getTracks()){
                            player.playTrack(track);
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
            if (slashCommandInteraction.getCommandName().equalsIgnoreCase("join")){
                slashCommandInteraction.createImmediateResponder()
                        .setContent("Connecting...")
                        .respond();
                TextChannel textChannel = slashCommandInteraction.getChannel().get();
                Server server = slashCommandInteraction.getServer().get();
                ServerVoiceChannel voiceChannel = slashCommandInteraction.getUser().getConnectedVoiceChannel(server).get();
                voiceChannel.connect().thenAccept(audioConnection -> {
                    textChannel.sendMessage("Connected.").join();
                    playerManager = new DefaultAudioPlayerManager();
                    playerManager.registerSourceManager(new YoutubeAudioSourceManager());
                    player = playerManager.createPlayer();

                    audioConnection1 = audioConnection;
                    AudioSource source = new LavaplayerAudioSource(api, player);
                    audioConnection.setAudioSource(source);

//                    playerManager.loadItem("https://www.youtube.com/watch?v=8pGRdRhjX3o", new AudioLoadResultHandler() {
//                        @Override
//                        public void trackLoaded(AudioTrack track) {
//                            player.playTrack(track);
//                        }
//
//                        @Override
//                        public void playlistLoaded(AudioPlaylist playlist) {
//                            for (AudioTrack track : playlist.getTracks()){
//                                player.playTrack(track);
//                            }
//                        }
//
//                        @Override
//                        public void noMatches() {
//
//                        }
//
//                        @Override
//                        public void loadFailed(FriendlyException exception) {
//
//                        }
//                    });

                }).exceptionally(throwable -> {
                    throw new RuntimeException(throwable);
                });

            }

        });
    }
}

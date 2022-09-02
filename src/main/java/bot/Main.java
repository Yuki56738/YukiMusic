package bot;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import io.github.cdimascio.dotenv.Dotenv;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.channel.VoiceChannel;
import org.javacord.api.entity.server.Server;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandInteraction;

public class Main {
    public static void main(String[] args){
        Dotenv dotenv = Dotenv.load();
        String TOKEN = dotenv.get("DISCORD_TOKEN");
        DiscordApi api = new DiscordApiBuilder()
                .setToken(TOKEN)
                .login().join();
        SlashCommand commandJoin = SlashCommand.with("join", "VCに接続.")
                .createForServer(api.getServerById("813401986299854859").get())
                .join();
        api.addSlashCommandCreateListener(event -> {
            SlashCommandInteraction slashCommandInteraction = event.getSlashCommandInteraction();
            if (slashCommandInteraction.getUser().isBot()){
                return;
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
                    AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
                    playerManager.registerSourceManager(new YoutubeAudioSourceManager());
                    AudioPlayer player = playerManager.createPlayer();

                }).exceptionally(throwable -> {
                    throw new RuntimeException(throwable);
                });

            }

        });
    }
}

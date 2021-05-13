package io.wolff.discordbot;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import discord4j.rest.util.Permission;

// TODO: note this implementation does not support multiple servers
public class Bot {
	
	public final Path commandsPath;
	
	public Bot(String commandsPath) {
		this.commandsPath = Paths.get(commandsPath);
		load();
	}
	
	private Map<String, Message> commands = new HashMap<>();
	
	public void handleMessageCreateEvent(MessageCreateEvent event) {
		String content = event.getMessage().getContent();
		if(content==null || !content.startsWith("/")) {
			return; // not a command
		}
		String[] args = content.split(" ");
		if(args[0].equals("/set")) {
			if(!memberHasPermissions(event.getMember().orElse(null))) {
				event.getMessage().getChannel().block().createMessage(message -> {
					message.setContent("You do not have permission to use this command.");
				}).block();
				return;
			}
			if(setCommand(args)) {
				event.getMessage().getChannel().block().createMessage(message -> {
					message.setContent("Created new command "+args[1]);
				}).block();
			} else {
				event.getMessage().getChannel().block().createMessage(message -> {
					message.setContent("Usage: /set <command name> <command text> [embedded image url]");
				}).block();
			}
			return;
		}
		if(args[0].equals("/unset")) {
			if(!memberHasPermissions(event.getMember().orElse(null))) {
				event.getMessage().getChannel().block().createMessage(message -> {
					message.setContent("You do not have permission to use this command.");
				}).block();
				return;
			}
			if(removeCommand(args)) {
				event.getMessage().getChannel().block().createMessage(message -> {
					message.setContent("That command was removed.");
				}).block();
			} else {
				event.getMessage().getChannel().block().createMessage(message -> {
					message.setContent("That command doesn't exist.");
				}).block();
			}
			return;
		}
		if(args[0].equals("/list")) {
			event.getMessage().getChannel().block().createMessage(message -> {
				message.setContent("I know how to: "+commands.keySet().stream().reduce("", (s1, s2) -> s1+" "+s2));
			}).block();
			return;
		}
		if(args[0].equals("/user")) {
			if(args.length==0) {
				event.getMessage().getChannel().block().createMessage(message -> {
					message.setContent("Usage: /user [username]");
				}).block();
				return;
			}
			String username = content.substring("/user ".length());
			// discord 'ids' do not correlate to usernames. must search through guild's members
			Member match = event.getGuild().block().requestMembers()
					.filter(p -> username.equals(p.getNickname().orElse(p.getUsername()))).blockFirst();
			if(match==null) {
				event.getMessage().getChannel().block().createMessage(message -> {
					message.setContent("Could not find user "+username);
				}).block();
				return;
			}
			event.getMessage().getChannel().block().createMessage(m -> {
				m.setEmbed(e -> {
					e.setImage(match.getAvatarUrl());
					e.setTitle(username);
				});
			}).block();
			return;
		}
		// now in user-entered command territory, surround in try-catch
		try {
			String command = args[0];
			Message m = commands.get(command);
			if(m==null) {
				// someone else might know how to do this command, so do nothing
				return;
			}
			event.getMessage().getChannel().block().createMessage(message -> {
				message.setContent(m.content);
				if(m.embedUrl!=null) {
					message.setEmbed(embed -> {
						embed.setImage(m.embedUrl);
					});
				}
			}).block();
		} catch (RuntimeException e) {
			// swallow runtime exceptions and tell the user something happened
			event.getMessage().getChannel().block().createMessage(message -> {
				message.setContent("There was an error running that command.");
				message.setEmbed(embed -> {
					embed.setTitle("Dev info");
					StringWriter s = new StringWriter();
					PrintWriter pw = new PrintWriter(s);
					e.printStackTrace(pw);
					embed.setDescription(s.toString());
				});
			}).block();
		}
	}

	public static final String PERM_NAME = "JacksonBottock_set";
	private boolean memberHasPermissions(Member member) {
		if(member==null) {
			return false;
		}
		return member.getBasePermissions().map(permSet -> permSet.asEnumSet().contains(Permission.ADMINISTRATOR)).block() ||
				member.getRoles().any(role -> PERM_NAME.equals(role.getName())).block();
	}

	private boolean removeCommand(String[] args) {
		if(args.length!=2) {
			return false;
		}
		String name = args[1];
		Message m = commands.remove("/"+name);
		save();
		return m!=null;
		
	}
	
	public static final List<String> PREDEF_COMMANDS = Arrays.asList("set", "unset", "list");

	private boolean setCommand(String[] args) {
		if(args.length < 3) {
			return false;
		}
		String name = args[1];
		if(PREDEF_COMMANDS.contains(name)) {
			return false;
		}
		String url;
		String content;
		try {
			new URL(args[args.length-1]);
			url = args[args.length-1];
			content = Arrays.stream(args, 2, args.length-1).reduce("", (s1, s2) -> s1+" "+s2);
		} catch (MalformedURLException e) {
			url = null;
			content = Arrays.stream(args, 2, args.length).reduce("", (s1, s2) -> s1+" "+s2);
		}
		Message m = new Message();
		m.embedUrl = url;
		m.content = content;
		commands.put("/"+name, m);
		save();
		return true;
	}
	
	private void save() {
		try {
			Files.write(commandsPath, new Gson().toJson(this.commands).getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void load() {
		if(Files.exists(commandsPath)) {
			try {
				this.commands = new Gson().fromJson(new String(Files.readAllBytes(commandsPath)), new TypeToken<Map<String, Message>>() {}.getType());
			} catch (JsonSyntaxException | IOException e) {
				e.printStackTrace();
			}
		}
	}

}

class Message {
	String content;
	String embedUrl;
}

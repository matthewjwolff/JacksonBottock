package io.wolff.discordbot;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import discord4j.core.event.domain.message.MessageCreateEvent;

//TODO: serialization
// TODO: note this implementation does not support multiple servers
public class Bot {
	
	public static final Bot INST = new Bot();
	
	private Bot() {
		load();
	}
	
	private Map<String, Message> commands = new HashMap<>();
	
	public void handleMessageCreateEvent(MessageCreateEvent event) {
		String content = event.getMessage().getContent().orElse(null);
		if(content==null || !content.startsWith("/")) {
			return; // not a command
		}
		String[] args = content.split(" ");
		if(args[0].equals("/set")) {
			// TODO: check admin privileges
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
			// TODO: check admin privileges
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
		// now in user-entered command territory, surround in try-catch
		try {
			String command = args[0];
			Message m = commands.get(command);
			if(m==null) {
				event.getMessage().getChannel().block().createMessage(message -> {
					message.setContent("I don't know how to respond to that command.");
				}).block();
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

	private boolean removeCommand(String[] args) {
		if(args.length!=2) {
			return false;
		}
		String name = args[1];
		Message m = commands.remove("/"+name);
		save();
		return m!=null;
		
	}

	private boolean setCommand(String[] args) {
		if(args.length < 3) {
			return false;
		}
		String name = args[1];
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
		// TODO: implement saving user messages
	}
	
	private void load() {
		// TODO: implement loading user messages
	}

}

class Message {
	String content;
	String embedUrl;
}

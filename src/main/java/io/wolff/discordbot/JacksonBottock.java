// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
// 
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package io.wolff.discordbot;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;

public class JacksonBottock {
	
	public static void main(String[] args) {
		String accessToken = System.getenv("JACKSON_ACCESS_TOKEN");
		String commandPath = System.getenv("JACKSON_COMMAND_PATH");
		
		for(int i=0; i<args.length-1; i++) {
			if("-t".equals(args[i])) {
				i++;
				accessToken = args[i];
			} else if("-d".equals(args[i])) {
				i++;
				commandPath = args[i];
			}
		}
		
		if(accessToken == null || commandPath==null) {
			throw new IllegalArgumentException("Invalid usage. Execute with -t your_token_here -d config_path, or use environment variables JACKSON_ACCESS_TOKEN and JACKSON_COMMAND_PATH");
		}
		DiscordClient client = DiscordClientBuilder.create(accessToken).build();
		client.gateway().setEnabledIntents(IntentSet.of(Intent.GUILD_MESSAGES, Intent.GUILD_MEMBERS));
		GatewayDiscordClient gateway = client.login().block();
		Bot bot = new Bot(commandPath);
		gateway.on(MessageCreateEvent.class).subscribe(bot::handleMessageCreateEvent);
		gateway.onDisconnect().block();
	}

}

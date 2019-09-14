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

import java.util.Optional;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.message.MessageCreateEvent;

public class Main {
	
	public static final String YEE = "http://vignette4.wikia.nocookie.net/youtubepoop/images/c/c3/Tumblr_nb7jgq9kcR1slfxluo1_1280.jpg";

	public static void main(String[] args) {
		if(args.length!=1) {
			throw new IllegalArgumentException("Invalid usage. Please provide token only.");
		}
		DiscordClient client = new DiscordClientBuilder(args[0]).build();
		client.getEventDispatcher().on(MessageCreateEvent.class).subscribe(event -> {
			if(event.getMessage().getContent().equals(Optional.of("/yee"))) {
				event.getMessage().getChannel().block().createMessage(message -> {
					message.setContent("Yee");
					message.setEmbed(embed -> {
						embed.setImage(YEE);
					});
				}).block();
			}
		});
		
		client.login().block();
	}

}

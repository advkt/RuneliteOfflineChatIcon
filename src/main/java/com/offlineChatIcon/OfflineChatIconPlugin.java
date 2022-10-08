package com.offlineChatIcon;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.events.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.account.AccountPlugin;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Objects;

@Slf4j
@PluginDescriptor(
	name = "Offline Chat Icon"
)
public class OfflineChatIconPlugin extends Plugin
{
	@Inject
	private Client client;

	private int offlineIconLocation = -1;
	private String iconImg;
	@Inject
	private ClientThread clientThread;

	@Override
	protected void startUp() throws Exception
	{
		clientThread.invoke(() ->
		{
			if (client.getModIcons() == null)
			{
				return false;
			}
			loadIcon();
			return true;
		});
	}

	@Subscribe
	public void onClanMemberJoined(ClanMemberJoined clanMemberJoined)
	{
		final ClanChannelMember member = clanMemberJoined.getClanMember();
		rebuildChat(member.getName(), false);
	}

	@Subscribe
	public void onClanMemberLeft(ClanMemberLeft clanMemberLeft)
	{
		final ClanChannelMember member = clanMemberLeft.getClanMember();
		rebuildChat(member.getName(), true);
	}

	private void loadIcon()
	{

		final IndexedSprite[] modIcons = client.getModIcons();

		if (offlineIconLocation != -1 || modIcons == null)
		{
			return;
		}

		BufferedImage image = ImageUtil.loadImageResource(getClass(), "/logout_icon.png");
		IndexedSprite indexedSprite = ImageUtil.getImageIndexedSprite(image, client);

		offlineIconLocation = modIcons.length;

		final IndexedSprite[] newModIcons = Arrays.copyOf(modIcons, modIcons.length + 1);
		newModIcons[newModIcons.length - 1] = indexedSprite;

		client.setModIcons(newModIcons);

		iconImg = "<img=" + offlineIconLocation + ">";
	}

	private void rebuildChat(String rsn, Boolean addIcon){
		boolean needRefreshing = false;
		IterableHashTable<MessageNode> messages = client.getMessages();
		for (MessageNode message: messages) {
			//I really only think i need to run standardized on the message.getName(), but want to make sure
			String cleanRsnFromMessage = Text.standardize(Text.removeTags(message.getName()));
			String standardizedRsnFromEvent = Text.standardize(rsn);
			ChatMessageType messageType = message.getType();
			if(cleanRsnFromMessage.equals(standardizedRsnFromEvent)){
				if(messageType == ChatMessageType.CLAN_CHAT || messageType == ChatMessageType.CLAN_GUEST_CHAT) {
					if(addIcon){
						message.setName(iconImg + message.getName());
					}else {
						message.setName(message.getName().replace(iconImg, ""));
					}

					needRefreshing = true;
				}
			}
		}
		if(needRefreshing){
			client.refreshChat();
		}
	}

}

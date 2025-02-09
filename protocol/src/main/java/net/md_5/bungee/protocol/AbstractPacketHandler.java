package net.md_5.bungee.protocol;

import net.md_5.bungee.protocol.packet.*;

public abstract class AbstractPacketHandler {

    public void handle(LegacyPing ping) throws Exception {
    }

    public void handle(TabCompleteResponse tabResponse) throws Exception {
    }

    public void handle(PingPacket ping) throws Exception {
    }

    public void handle(StatusRequest statusRequest) throws Exception {
    }

    public void handle(StatusResponse statusResponse) throws Exception {
    }

    public void handle(Handshake handshake) throws Exception {
    }

    public void handle(KeepAlive keepAlive) throws Exception {
    }

    public void handle(Login login) throws Exception {
    }

    public void handle(Chat chat) throws Exception {
    }

    public void handle(ClientChat chat) throws Exception {
    }

    public void handle(SystemChat chat) throws Exception {
    }

    public void handle(ClientCommand command) throws Exception {
    }

    public void handle(UnsignedClientCommand command) throws Exception {
    }

    public void handle(Respawn respawn) throws Exception {
    }

    public void handle(LoginRequest loginRequest) throws Exception {
    }

    public void handle(ClientSettings settings) throws Exception {
    }

    public void handle(ClientStatus clientStatus) throws Exception {
    }

    public void handle(PlayerListItem playerListItem) throws Exception {
    }

    public void handle(PlayerListItemRemove playerListItem) throws Exception {
    }

    public void handle(PlayerListItemUpdate playerListItem) throws Exception {
    }

    public void handle(PlayerListHeaderFooter playerListHeaderFooter) throws Exception {
    }

    public void handle(TabCompleteRequest tabComplete) throws Exception {
    }

    public void handle(ScoreboardObjective scoreboardObjective) throws Exception {
    }

    public void handle(ScoreboardScore scoreboardScore) throws Exception {
    }

    public void handle(ScoreboardScoreReset scoreboardScoreReset) throws Exception {
    }

    public void handle(EncryptionRequest encryptionRequest) throws Exception {
    }

    public void handle(ScoreboardDisplay displayScoreboard) throws Exception {
    }

    public void handle(Team team) throws Exception {
    }

    public void handle(Title title) throws Exception {
    }

    public void handle(Subtitle title) throws Exception {
    }

    public void handle(TitleTimes title) throws Exception {
    }

    public void handle(ClearTitles title) throws Exception {
    }

    public void handle(PluginMessage pluginMessage) throws Exception {
    }

    public void handle(Kick kick) throws Exception {
    }

    public void handle(EncryptionResponse encryptionResponse) throws Exception {
    }

    public void handle(LoginSuccess loginSuccess) throws Exception {
    }

    public void handle(LegacyHandshake legacyHandshake) throws Exception {
    }

    public void handle(SetCompression setCompression) throws Exception {
    }

    public void handle(BossBar bossBar) throws Exception {
    }

    public void handle(LoginPayloadRequest request) throws Exception {
    }

    public void handle(LoginPayloadResponse response) throws Exception {
    }

    public void handle(EntityStatus status) throws Exception {
    }

    public void handle(Commands commands) throws Exception {
    }

    public void handle(ViewDistance viewDistance) throws Exception {
    }

    public void handle(GameState gameState) throws Exception {
    }

    public void handle(ServerData serverData) throws Exception {
    }

    public void handle(LoginAcknowledged loginAcknowledged) throws Exception {
    }

    public void handle(StartConfiguration startConfiguration) throws Exception {
    }

    public void handle(FinishConfiguration finishConfiguration) throws Exception {
    }

    public void handle(Transfer transfer) throws Exception {
    }

    public void handle(StoreCookie storeCookie) throws Exception {
    }

    public void handle(CookieRequest cookieRequest) throws Exception {
    }

    public void handle(CookieResponse cookieResponse) throws Exception {
    }

    public void handle(DisconnectReportDetails disconnectReportDetails) throws Exception {
    }

    public void handle(ServerLinks serverLinks) throws Exception {
    }

    // Waterfall start
    public void handle(net.md_5.bungee.protocol.packet.EntityEffect entityEffect) throws Exception {
    }

    public void handle(net.md_5.bungee.protocol.packet.EntityRemoveEffect removeEffect) throws Exception {
    }

    public void handle(net.md_5.bungee.protocol.packet.ClientChatAcknowledgement clientChatAcknowledgement) {
    }
    // Waterfall end
}
